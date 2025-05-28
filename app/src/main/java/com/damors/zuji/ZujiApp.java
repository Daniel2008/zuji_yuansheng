package com.damors.zuji;

import android.app.Application;
import android.util.Log;
import java.io.File;

import com.damors.zuji.manager.UserManager;
import com.damors.zuji.network.NetworkStateMonitor;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;

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
        
        // 初始化OSMDroid地图缓存配置
        initOSMDroidCache();
        
        // 初始化用户管理器
        UserManager.init(this);
        
        // 初始化网络状态监听器
        initNetworkStateMonitor();
        
        Log.d(TAG, "应用初始化完成");
    }
    
    /**
     * 初始化OSMDroid地图缓存配置
     * 设置地图瓦片缓存目录和缓存参数，提升地图加载性能
     */
    private void initOSMDroidCache() {
        try {
            // 设置用户代理
            Configuration.getInstance().setUserAgentValue(getPackageName());
            
            // 设置地图瓦片缓存目录
            File osmCacheDir = new File(getCacheDir(), "osmdroid");
            if (!osmCacheDir.exists()) {
                osmCacheDir.mkdirs();
            }
            Configuration.getInstance().setOsmdroidBasePath(osmCacheDir);
            
            // 设置瓦片缓存目录
            File tileCache = new File(osmCacheDir, "tiles");
            if (!tileCache.exists()) {
                tileCache.mkdirs();
            }
            Configuration.getInstance().setOsmdroidTileCache(tileCache);
            
            // 设置缓存参数
            Configuration.getInstance().setCacheMapTileCount((short) 12); // 内存中缓存的瓦片数量
            Configuration.getInstance().setTileFileSystemCacheMaxBytes(50L * 1024 * 1024); // 磁盘缓存最大50MB
            Configuration.getInstance().setTileFileSystemCacheTrimBytes(40L * 1024 * 1024); // 缓存清理阈值40MB
            
            // 设置瓦片下载线程数
            Configuration.getInstance().setTileDownloadThreads((short) 4);
            
            // 设置瓦片文件系统缓存的最大年龄（7天）
            Configuration.getInstance().setExpirationExtendedDuration(7L * 24 * 60 * 60 * 1000);
            
            Log.d(TAG, "OSMDroid地图缓存配置初始化完成");
            Log.d(TAG, "缓存目录: " + osmCacheDir.getAbsolutePath());
            Log.d(TAG, "瓦片缓存目录: " + tileCache.getAbsolutePath());
            
        } catch (Exception e) {
            Log.e(TAG, "OSMDroid缓存配置初始化失败", e);
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
