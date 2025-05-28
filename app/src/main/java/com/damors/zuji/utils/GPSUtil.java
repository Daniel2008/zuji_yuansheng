package com.damors.zuji.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;

public class GPSUtil {
    public static double pi = 3.1415926535897932384626;
    public static double a = 6378245.0;
    public static double ee = 0.00669342162296594323;

    // 定义位置回调接口
    public interface LocationCallback {
        void onLocationResult(Location location);
    }

    private static final String TAG = "GPSUtil";
    private static final int LOCATION_TIMEOUT = 30000; // 30秒超时
    
    // 获取当前位置
    public static void getCurrentLocation(Context context, LocationCallback callback) {
        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        android.util.Log.d(TAG, "开始获取位置信息");
        
        // 检查位置权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e(TAG, "没有位置权限");
            callback.onLocationResult(null);
            return;
        }
        
        // 检查位置服务是否启用
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        android.util.Log.d(TAG, "GPS状态: " + isGpsEnabled + ", 网络定位状态: " + isNetworkEnabled);
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            android.util.Log.e(TAG, "位置服务未启用");
            // 提示用户开启位置服务
            showLocationSettingsDialog(context);
            callback.onLocationResult(null);
            return;
        }
        
        // 尝试获取最后已知位置
        try {
            Location lastKnownLocation = null;
            if (isGpsEnabled) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (lastKnownLocation == null && isNetworkEnabled) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (lastKnownLocation != null && System.currentTimeMillis() - lastKnownLocation.getTime() < 5 * 60 * 1000) {
                // 如果最后已知位置不超过5分钟，直接使用
                android.util.Log.d(TAG, "使用最后已知位置: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
                callback.onLocationResult(lastKnownLocation);
                return;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "获取最后已知位置异常: " + e.getMessage());
        }
        
        // 创建位置监听器
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                android.util.Log.d(TAG, "位置已更新: " + location.getLatitude() + ", " + location.getLongitude());
                callback.onLocationResult(location);
                // 移除位置更新监听器
                locationManager.removeUpdates(this);
                // 取消超时处理
                handler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                android.util.Log.d(TAG, "位置提供者状态变化: " + provider + ", 状态: " + status);
            }

            @Override
            public void onProviderEnabled(String provider) {
                android.util.Log.d(TAG, "位置提供者已启用: " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                android.util.Log.e(TAG, "位置提供者已禁用: " + provider);
                // 如果是GPS被禁用，尝试使用网络定位
                if (provider.equals(LocationManager.GPS_PROVIDER) && isNetworkEnabled) {
                    android.util.Log.d(TAG, "尝试使用网络定位");
                    return;
                }
                callback.onLocationResult(null);
                locationManager.removeUpdates(this);
                handler.removeCallbacksAndMessages(null);
            }
        };
        
        // 超时处理已移到方法开始处
        handler.postDelayed(() -> {
            android.util.Log.e(TAG, "获取位置超时");
            locationManager.removeUpdates(locationListener);
            callback.onLocationResult(null);
        }, LOCATION_TIMEOUT);
        
        // 请求位置更新，优先使用GPS，如果GPS不可用则使用网络定位
        try {
            if (isGpsEnabled) {
                android.util.Log.d(TAG, "请求GPS位置更新");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, android.os.Looper.getMainLooper());
            } else if (isNetworkEnabled) {
                android.util.Log.d(TAG, "请求网络位置更新");
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener, android.os.Looper.getMainLooper());
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "请求位置更新异常: " + e.getMessage());
            handler.removeCallbacksAndMessages(null);
            callback.onLocationResult(null);
        }
    }
    
    // 显示位置设置对话框
    private static void showLocationSettingsDialog(Context context) {
        try {
            new android.app.AlertDialog.Builder(context)
                .setTitle("位置服务未启用")
                .setMessage("请开启位置服务以获取当前位置")
                .setPositiveButton("设置", (dialog, which) -> {
                    // 打开位置设置页面
                    context.startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .setNegativeButton("取消", null)
                .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "显示位置设置对话框异常: " + e.getMessage());
        }
    }

    public static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    public static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }

    public static boolean outOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347) return true;
        if (lat < 0.8293 || lat > 55.8271) return true;
        return false;
    }

    public static double[] gps84_To_Gcj02(double lat, double lon) {
        if (outOfChina(lat, lon)) {
            return new double[]{lat, lon};
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLat, mgLon};
    }
}