package com.damors.zuji.utils;

import android.content.Context;
import android.util.Log;

import com.amap.api.maps.MapsInitializer;
import com.amap.api.location.AMapLocationClient;

/**
 * 高德地图工具类
 * 负责初始化高德地图SDK和相关配置
 * 
 * 主要功能：
 * 1. 初始化高德地图SDK
 * 2. 配置隐私政策
 * 3. 提供地图相关的工具方法
 */
public class AMapHelper {
    private static final String TAG = "AMapHelper";
    private static boolean isInitialized = false;
    
    /**
     * 初始化高德地图SDK
     * 必须在使用地图功能前调用
     * 
     * @param context 应用上下文
     */
    public static void initialize(Context context) {
        if (isInitialized) {
            Log.d(TAG, "高德地图SDK已经初始化");
            return;
        }
        
        try {
            // 设置隐私政策同意状态
            // 注意：在实际应用中，应该在用户同意隐私政策后再调用
            MapsInitializer.updatePrivacyShow(context, true, true);
            MapsInitializer.updatePrivacyAgree(context, true);
            
            // 初始化定位SDK的隐私政策
            AMapLocationClient.updatePrivacyShow(context, true, true);
            AMapLocationClient.updatePrivacyAgree(context, true);
            
            isInitialized = true;
            Log.d(TAG, "高德地图SDK初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "高德地图SDK初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查高德地图SDK是否已初始化
     * 
     * @return true表示已初始化，false表示未初始化
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * 设置隐私政策同意状态
     * 在用户同意隐私政策后调用
     * 
     * @param context 应用上下文
     * @param isAgree 是否同意隐私政策
     */
    public static void setPrivacyAgree(Context context, boolean isAgree) {
        try {
            MapsInitializer.updatePrivacyAgree(context, isAgree);
            AMapLocationClient.updatePrivacyAgree(context, isAgree);
            Log.d(TAG, "隐私政策同意状态已更新: " + isAgree);
        } catch (Exception e) {
            Log.e(TAG, "更新隐私政策同意状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 坐标转换：GPS坐标转高德坐标
     * 高德地图使用GCJ-02坐标系
     * 
     * @param latitude GPS纬度
     * @param longitude GPS经度
     * @return 转换后的坐标数组 [纬度, 经度]
     */
    public static double[] gpsToAMap(double latitude, double longitude) {
        // 这里可以使用GPSUtil中的转换方法
        return GPSUtil.gps84_To_Gcj02(latitude, longitude);
    }
    
    /**
     * 格式化坐标显示
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @return 格式化后的坐标字符串
     */
    public static String formatCoordinate(double latitude, double longitude) {
        return String.format("纬度: %.6f\n经度: %.6f", latitude, longitude);
    }
    
    /**
     * 格式化单个坐标值
     * 
     * @param coordinate 坐标值（纬度或经度）
     * @param isLatitude 是否为纬度（true为纬度，false为经度）
     * @return 格式化后的坐标字符串
     */
    public static String formatCoordinate(double coordinate, boolean isLatitude) {
        String type = isLatitude ? "纬度" : "经度";
        return String.format("%s: %.6f", type, coordinate);
    }
    
    /**
     * 计算两点之间的距离（米）
     * 
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（米）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371000; // 地球半径（米）
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }
}