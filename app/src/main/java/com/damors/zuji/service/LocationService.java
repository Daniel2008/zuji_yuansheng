package com.damors.zuji.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.damors.zuji.MainActivity;
import com.damors.zuji.R;
import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.data.FootprintRepository;

import java.util.Date;

/**
 * 位置服务类
 * 负责在后台持续追踪用户位置并记录足迹
 */
public class LocationService extends Service implements LocationListener {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long UPDATE_INTERVAL = 10000; // 10秒
    private static final long FASTEST_INTERVAL = 5000; // 5秒
    private static final float MIN_DISTANCE_CHANGE = 10.0f; // 10米

    private LocationManager locationManager;
    private FootprintRepository repository;
    private final IBinder binder = new LocalBinder();
    private boolean isTracking = false;

    /**
     * 本地Binder类，用于客户端绑定服务
     */
    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化位置管理器
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        // 初始化数据仓库
        repository = new FootprintRepository(getApplication());
        
        Log.d(TAG, "位置服务已创建");
    }



    /**
     * 保存足迹点到数据库
     */
    private void saveFootprint(Location location) {
        if (location != null) {
            FootprintEntity footprint = new FootprintEntity();
            footprint.setLatitude(location.getLatitude());
            footprint.setLongitude(location.getLongitude());
            footprint.setTimestamp(new Date().getTime());
            footprint.setAltitude(location.getAltitude());
            footprint.setAccuracy(location.getAccuracy());
            
            // 异步保存到数据库
            repository.insert(footprint);
            
            Log.d(TAG, "已保存足迹点 - 纬度: " + location.getLatitude() + ", 经度: " + location.getLongitude());
        }
    }

    /**
     * 开始位置追踪
     */
    public void startTracking() {
        if (!isTracking) {
            startForeground(NOTIFICATION_ID, createNotification());
            startLocationUpdates();
            isTracking = true;
        }
    }

    /**
     * 停止位置追踪
     */
    public void stopTracking() {
        if (isTracking) {
            stopLocationUpdates();
            stopForeground(true);
            isTracking = false;
        }
    }

    /**
     * 开始接收位置更新
     */
    private void startLocationUpdates() {
        try {
            // 检查GPS是否可用
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // 检查网络定位是否可用
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
            Log.d(TAG, "GPS状态: " + (isGPSEnabled ? "可用" : "不可用"));
            Log.d(TAG, "网络定位状态: " + (isNetworkEnabled ? "可用" : "不可用"));
            
            if (isGPSEnabled) {
                // 使用GPS获取位置
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        UPDATE_INTERVAL,
                        MIN_DISTANCE_CHANGE,
                        this);
                Log.d(TAG, "已注册GPS位置更新监听器");
                
                // 获取最后已知位置
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    saveFootprint(lastKnownLocation);
                }
            } else if (isNetworkEnabled) {
                // 使用网络获取位置
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        UPDATE_INTERVAL,
                        MIN_DISTANCE_CHANGE,
                        this);
                Log.d(TAG, "已注册网络位置更新监听器");
                
                // 获取最后已知位置
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnownLocation != null) {
                    saveFootprint(lastKnownLocation);
                }
            } else {
                Log.d(TAG, "没有可用的位置提供者");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "位置更新请求失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 停止接收位置更新
     */
    private void stopLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            Log.d(TAG, "已停止位置更新");
        }
    }

    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        createNotificationChannel();
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("足迹记录")
                .setContentText("正在记录您的位置")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
    }

    /**
     * 创建通知渠道（Android 8.0及以上需要）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "位置服务",
                    NotificationManager.IMPORTANCE_LOW);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 如果服务被系统杀死后重新创建，保持启动状态但不重新交付Intent
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopTracking();
        super.onDestroy();
    }
    
    /**
     * 位置变化回调
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (isTracking) {
            saveFootprint(location);
        }
    }
    
    /**
     * 位置提供者状态变化回调
     */
    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.d(TAG, "位置提供者已启用: " + provider);
    }
    
    /**
     * 位置提供者状态变化回调
     */
    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.d(TAG, "位置提供者已禁用: " + provider);
    }

    /**
     * 获取当前追踪状态
     */
    public boolean isTracking() {
        return isTracking;
    }
}