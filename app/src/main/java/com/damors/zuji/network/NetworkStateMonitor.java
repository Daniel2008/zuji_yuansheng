package com.damors.zuji.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 网络状态监听器，用于监听网络状态变化并通知应用的其他部分
 */
public class NetworkStateMonitor {
    private static final String TAG = "NetworkStateMonitor";
    private static final String TEST_URL = "https://www.baidu.com"; // 用于测试网络连接的URL
    private static final int CONNECTION_TIMEOUT_MS = 3000; // 连接超时时间
    private static final long NETWORK_CHECK_DEBOUNCE_MS = 2000; // 网络检测防抖时间（2秒）

    private Context context;
    private ConnectivityManager connectivityManager;
    private List<NetworkStateListener> listeners = new ArrayList<>();
    private boolean isNetworkAvailable = false;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private long lastNetworkCheckTime = 0; // 最后一次网络检测时间

    // 网络状态变化监听器接口
    public interface NetworkStateListener {
        void onNetworkStateChanged(boolean isAvailable);
    }

    public NetworkStateMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // 初始化时检查网络状态
        checkNetworkAvailability();

        // 注册网络状态变化监听器
        registerNetworkCallback();
    }

    /**
     * 注册网络状态变化监听器
     */
    private void registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0及以上使用NetworkCallback
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Log.d(TAG, "网络连接可用");
                    checkNetworkAvailability();
                }

                @Override
                public void onLost(Network network) {
                    Log.d(TAG, "网络连接断开");
                    updateNetworkState(false);
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                    boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    boolean hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    Log.d(TAG, "网络能力变化: 互联网=" + hasInternet + ", 已验证=" + hasValidated);
                    if (hasInternet && hasValidated) {
                        checkNetworkAvailability();
                    }
                }
            };

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        } else {
            // Android 7.0以下使用BroadcastReceiver
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

            BroadcastReceiver networkReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "收到网络状态变化广播");
                    checkNetworkAvailability();
                }
            };

            context.registerReceiver(networkReceiver, intentFilter);
        }
    }

    /**
     * 检查网络是否可用
     */
    public void checkNetworkAvailability() {
        long currentTime = System.currentTimeMillis();
        
        // 防抖机制：如果距离上次检测时间不足2秒，则跳过本次检测
        if (currentTime - lastNetworkCheckTime < NETWORK_CHECK_DEBOUNCE_MS) {
            Log.d(TAG, "网络检测防抖，跳过本次检测");
            return;
        }
        
        lastNetworkCheckTime = currentTime;
        
        // 首先检查网络连接是否存在
        boolean isConnected = isNetworkConnected();

        if (isConnected) {
            // 如果网络连接存在，异步测试网络是否真正可用
            executorService.execute(() -> {
                boolean canReachInternet = canReachInternet();
                Log.d(TAG, "网络连接测试结果: " + canReachInternet);
                updateNetworkState(canReachInternet);
            });
        } else {
            // 如果网络连接不存在，直接更新状态
            updateNetworkState(false);
        }
    }

    /**
     * 检查网络连接是否存在
     */
    private boolean isNetworkConnected() {
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    /**
     * 测试是否能够访问互联网
     */
    private boolean canReachInternet() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(TEST_URL).openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(CONNECTION_TIMEOUT_MS);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException e) {
            Log.e(TAG, "网络连接测试失败", e);
            return false;
        }
    }

    /**
     * 更新网络状态并通知监听器
     */
    private void updateNetworkState(final boolean isAvailable) {
        // 检查状态是否变化
        if (this.isNetworkAvailable != isAvailable) {
            this.isNetworkAvailable = isAvailable;

            // 在主线程通知监听器
            android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
            mainHandler.post(() -> {
                Log.d(TAG, "网络状态变化: " + (isAvailable ? "可用" : "不可用"));
                for (NetworkStateListener listener : listeners) {
                    listener.onNetworkStateChanged(isAvailable);
                }
            });
        }
    }

    /**
     * 添加网络状态监听器
     */
    public void addNetworkStateListener(NetworkStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            // 不立即通知当前状态，避免在初始化时触发不必要的网络状态变化事件
            // 只有在真正的网络状态变化时才通知监听器
        }
    }

    /**
     * 移除网络状态监听器
     */
    public void removeNetworkStateListener(NetworkStateListener listener) {
        listeners.remove(listener);
    }

    /**
     * 获取当前网络状态
     */
    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    /**
     * 释放资源
     */
    public void release() {
        listeners.clear();
        executorService.shutdown();
    }
}
