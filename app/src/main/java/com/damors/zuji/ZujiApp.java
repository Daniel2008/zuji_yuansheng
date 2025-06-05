package com.damors.zuji;

import android.app.Application;
import android.util.Log;
import java.io.File;

import com.damors.zuji.manager.UserManager;
import com.damors.zuji.network.NetworkStateMonitor;
import com.damors.zuji.utils.MapCacheManager;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.location.AMapLocationClient;

/**
 * 应用程序类，负责初始化全局组件
 */
public class ZujiApp extends Application {
    private static final String TAG = "ZujiApp";
    
    private static ZujiApp instance;
    private NetworkStateMonitor networkStateMonitor;
    
    /**
     * 获取应用实例
     */
    public static ZujiApp getInstance() {
        return instance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // 初始化高德地图配置
        initAMapConfig();
        
        // 初始化地图缓存管理器
        MapCacheManager.init(this);
        
        // 初始化用户管理器
        UserManager.init(this);
        
        // 初始化网络状态监听器
        initNetworkStateMonitor();
        
        Log.d(TAG, "应用初始化完成");
    }
    
    /**
     * 初始化高德地图配置
     */
    private void initAMapConfig() {
        try {
            // 设置隐私政策同意状态（必须在使用地图功能前调用）
            MapsInitializer.updatePrivacyShow(this, true, true);
            MapsInitializer.updatePrivacyAgree(this, true);
            
            // 初始化定位SDK的隐私政策
            AMapLocationClient.updatePrivacyShow(this, true, true);
            AMapLocationClient.updatePrivacyAgree(this, true);
            
            // 设置是否已经包含高德隐私政策并弹窗展示显示用户查看，如果未包含或者没有弹窗展示，请设置为false
            MapsInitializer.updatePrivacyShow(this, true, true);
            // 设置是否已经取得用户同意，如果未取得用户同意，请设置为false
            MapsInitializer.updatePrivacyAgree(this, true);
            
            Log.d(TAG, "高德地图SDK初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "高德地图SDK初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化网络状态监听器
     */
    private void initNetworkStateMonitor() {
        networkStateMonitor = new NetworkStateMonitor(this);
        
        // 添加网络状态变化监听器，用于记录网络状态变化
        networkStateMonitor.addNetworkStateListener(new NetworkStateMonitor.NetworkStateListener() {
            @Override
            public void onNetworkStateChanged(boolean isAvailable) {
                Log.d(TAG, "网络状态变化: " + (isAvailable ? "可用" : "不可用"));
            }
        });
        
        // 立即检查网络状态
        networkStateMonitor.checkNetworkAvailability();
    }
    
    /**
     * 获取网络状态监听器
     */
    public NetworkStateMonitor getNetworkStateMonitor() {
        return networkStateMonitor;
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // 释放网络状态监听器资源
        if (networkStateMonitor != null) {
            networkStateMonitor.release();
            networkStateMonitor = null;
        }
        
        Log.d(TAG, "应用终止");
    }
}
