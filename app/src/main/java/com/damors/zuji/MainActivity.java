package com.damors.zuji;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

// 高德地图相关导入 (暂时注释)
import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.MapsInitializer;
import com.damors.zuji.utils.AMapHelper;

/**
 * 主活动类，作为应用的入口点
 * 管理底部导航和不同的功能Fragment
 */
public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener, LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "MainActivity";
    private static final long MIN_TIME_BETWEEN_UPDATES = 5000; // 5秒
    private static final float MIN_DISTANCE_CHANGE = 10; // 10米
    
    private BottomNavigationView bottomNavigationView;
    private LocationManager locationManager;

    // 主要的Fragment
    private MapFragment mapFragment;
    private HistoryFragment historyFragment;
    private ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 设置主题
        setTheme(R.style.Theme_Zuji_yuansheng);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 初始化高德地图SDK (暂时注释)
        initAMapSDK();
        
        // 初始化视图
        initViews();
        
        // 初始化位置管理器
        initLocationManager();
        
        // 检查并请求位置权限
        checkLocationPermission();
        
        // 默认显示地图Fragment
        if (savedInstanceState == null) {
            mapFragment = new MapFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mapFragment)
                    .commit();
        }
    }

    /**
     * 初始化高德地图SDK
     */
    private void initAMapSDK() {
        try {
            // 初始化高德地图SDK
            AMapHelper.initialize(this);
            
            Log.d(TAG, "高德地图SDK初始化成功");
            
            // 设置隐私政策同意状态
            AMapLocationClient.updatePrivacyShow(this, true, true);
            AMapLocationClient.updatePrivacyAgree(this, true);
            
        } catch (Exception e) {
            Log.e(TAG, "高德地图SDK初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);
        // 默认选中地图tab
        bottomNavigationView.setSelectedItemId(R.id.nav_map);
    }

    /**
     * 检查位置权限，如果没有则请求
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // 如果已经有权限，开始获取位置更新
            startLocationUpdates();
        }
    }
    
    /**
     * 初始化位置管理器
     */
    private void initLocationManager() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d(TAG, "位置管理器初始化完成");
    }
    
    /**
     * 开始获取位置更新
     */
    private void startLocationUpdates() {
        // 检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        // 检查GPS是否可用
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 检查网络定位是否可用
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        Log.d(TAG, "GPS状态: " + (isGPSEnabled ? "可用" : "不可用"));
        Log.d(TAG, "网络定位状态: " + (isNetworkEnabled ? "可用" : "不可用"));
        
        try {
            if (isGPSEnabled) {
                // 使用GPS获取位置
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_CHANGE,
                        this);
                Log.d(TAG, "已注册GPS位置更新监听器");
                
                // 获取最后已知位置
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    onLocationChanged(lastKnownLocation);
                }
            } else if (isNetworkEnabled) {
                // 使用网络获取位置
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_CHANGE,
                        this);
                Log.d(TAG, "已注册网络位置更新监听器");
                
                // 获取最后已知位置
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnownLocation != null) {
                    onLocationChanged(lastKnownLocation);
                }
            } else {
                Toast.makeText(this, "请开启GPS或网络定位服务", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "位置更新请求失败: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予
                if (mapFragment != null) {
                    mapFragment.enableMyLocation();
                }
                // 开始获取位置更新
                startLocationUpdates();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要位置权限来记录足迹", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_map) {
            if (mapFragment == null) {
                mapFragment = new MapFragment();
            }
            selectedFragment = mapFragment;
        } else if (itemId == R.id.nav_footprint) {
            if (historyFragment == null) {
                historyFragment = new HistoryFragment();
            }
            selectedFragment = historyFragment;
            // 设置选中状态
            item.setChecked(true);
        } else if (itemId == R.id.nav_profile) {
            if (profileFragment == null) {
                profileFragment = new ProfileFragment();
            }
            selectedFragment = profileFragment;
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;
        }
        return false;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 在活动恢复时重新注册位置更新
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 在活动暂停时停止位置更新，节省电量
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            Log.d(TAG, "已停止位置更新");
        }
    }
    
    /**
     * 位置变化回调
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {
        // 获取位置信息
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        float accuracy = location.getAccuracy();
        long time = location.getTime();
        
        // 输出位置日志
        Log.d(TAG, "位置更新 - 纬度: " + latitude + ", 经度: " + longitude);
        Log.d(TAG, "精确度: " + accuracy + "米, 时间: " + time);
        
        // 如果需要，可以在这里将位置信息传递给其他组件
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
        Toast.makeText(this, "请开启" + (provider.equals(LocationManager.GPS_PROVIDER) ? "GPS" : "网络定位"), Toast.LENGTH_SHORT).show();
    }
}