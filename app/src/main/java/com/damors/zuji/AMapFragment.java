package com.damors.zuji;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.response.FootprintMessageResponse;
import com.damors.zuji.network.HutoolApiService;
import com.damors.zuji.utils.AMapHelper;
import com.damors.zuji.viewmodel.FootprintViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 高德地图Fragment
 * 替换原有的OSMDroid地图实现，提供更稳定和丰富的地图功能
 * 
 * 主要功能：
 * 1. 地图显示和交互
 * 2. GPS定位和位置更新
 * 3. 足迹标记和轨迹显示
 * 4. 足迹动态数据加载
 * 5. 添加足迹功能
 */
public class AMapFragment extends Fragment implements AMapLocationListener {
    private static final String TAG = "AMapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long LOCATION_INTERVAL = 5000; // 定位间隔5秒
    
    // UI组件
    private MapView mapView;
    private AMap aMap;
    private FloatingActionButton addFootprintButton;
    
    // 定位相关
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    private AMapLocation lastLocation;
    private Marker currentLocationMarker;
    
    // 数据相关
    private FootprintViewModel viewModel;
    private HutoolApiService apiService;
    
    // 状态管理
    private final AtomicBoolean isLocationUpdating = new AtomicBoolean(false);
    private final AtomicBoolean isLoadingFootprints = new AtomicBoolean(false);
    private final AtomicBoolean isLoadingMessages = new AtomicBoolean(false);
    private boolean isMapInitialized = false;
    
    // 线程处理
    private Handler mainHandler;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_amap, container, false);
        
        try {
            // 初始化主线程Handler
            mainHandler = new Handler(Looper.getMainLooper());
            
            // 确保高德地图SDK已初始化
            if (!AMapHelper.isInitialized()) {
                AMapHelper.initialize(getContext());
            }
            
            // 初始化UI组件
            initViews(view);
            
            // 初始化地图
            initMap(savedInstanceState);
            
            // 初始化数据
            initData();
            
            Log.d(TAG, "AMapFragment创建成功");
        } catch (Exception e) {
            Log.e(TAG, "AMapFragment创建失败: " + e.getMessage(), e);
            showError("地图初始化失败，请重试");
        }
        
        return view;
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews(View view) {
        mapView = view.findViewById(R.id.amap_view);
        addFootprintButton = view.findViewById(R.id.fab_add_footprint);
        
        // 设置添加足迹按钮点击事件
        addFootprintButton.setOnClickListener(v -> {
            if (lastLocation != null) {
                openAddFootprintActivity();
            } else {
                showError("正在获取位置信息，请稍后再试");
            }
        });
    }
    
    /**
     * 初始化地图
     */
    private void initMap(Bundle savedInstanceState) {
        try {
            mapView.onCreate(savedInstanceState);
            
            if (aMap == null) {
                aMap = mapView.getMap();
            }
            
            // 设置地图类型
            aMap.setMapType(AMap.MAP_TYPE_NORMAL);
            
            // 设置定位样式
            MyLocationStyle myLocationStyle = new MyLocationStyle();
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
            myLocationStyle.interval(LOCATION_INTERVAL);
            myLocationStyle.strokeColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));
            myLocationStyle.radiusFillColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light));
            aMap.setMyLocationStyle(myLocationStyle);
            
            // 启用定位图层
            aMap.setMyLocationEnabled(true);
            
            // 设置UI控件
            aMap.getUiSettings().setZoomControlsEnabled(true);
            aMap.getUiSettings().setCompassEnabled(true);
            aMap.getUiSettings().setScaleControlsEnabled(true);
            aMap.getUiSettings().setRotateGesturesEnabled(true);
            aMap.getUiSettings().setTiltGesturesEnabled(true);
            
            // 设置缩放级别范围
            aMap.setMaxZoomLevel(20);
            aMap.setMinZoomLevel(3);
            
            isMapInitialized = true;
            Log.d(TAG, "高德地图初始化成功");
            
            // 初始化定位
            initLocation();
            
        } catch (Exception e) {
            Log.e(TAG, "地图初始化失败: " + e.getMessage(), e);
            showError("地图初始化失败");
        }
    }
    
    /**
     * 初始化定位
     */
    private void initLocation() {
        try {
            // 检查定位权限
            if (!checkLocationPermission()) {
                requestLocationPermission();
                return;
            }
            
            // 创建定位客户端
            locationClient = new AMapLocationClient(getContext());
            
            // 设置定位参数
            locationOption = new AMapLocationClientOption();
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            locationOption.setInterval(LOCATION_INTERVAL);
            locationOption.setNeedAddress(true);
            locationOption.setOnceLocation(false);
            locationOption.setWifiActiveScan(true);
            locationOption.setMockEnable(false);
            locationOption.setHttpTimeOut(20000);
            
            locationClient.setLocationOption(locationOption);
            locationClient.setLocationListener(this);
            
            // 开始定位
            startLocation();
            
            Log.d(TAG, "定位服务初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "定位服务初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化数据
     */
    private void initData() {
        try {
            // 初始化ViewModel
            viewModel = new ViewModelProvider(this).get(FootprintViewModel.class);
            
            // 初始化API服务
            apiService = HutoolApiService.getInstance(getContext());
            
            // 观察足迹数据变化
            viewModel.getAllFootprints().observe(getViewLifecycleOwner(), this::displayFootprintsOnMap);
            
            // 加载足迹数据
            loadFootprints();
            
            // 加载足迹动态
            loadFootprintMessages();
            
            Log.d(TAG, "数据初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "数据初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查定位权限
     */
    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 请求定位权限
     */
    private void requestLocationPermission() {
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, LOCATION_PERMISSION_REQUEST_CODE);
    }
    
    /**
     * 开始定位
     */
    private void startLocation() {
        if (locationClient != null && !isLocationUpdating.get()) {
            isLocationUpdating.set(true);
            locationClient.startLocation();
            Log.d(TAG, "开始定位");
        }
    }
    
    /**
     * 停止定位
     */
    private void stopLocation() {
        if (locationClient != null && isLocationUpdating.get()) {
            locationClient.stopLocation();
            isLocationUpdating.set(false);
            Log.d(TAG, "停止定位");
        }
    }
    
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
            // 定位成功
            lastLocation = aMapLocation;
            LatLng latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
            
            // 首次定位时移动地图到当前位置
            if (currentLocationMarker == null) {
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            }
            
            Log.d(TAG, "定位成功: " + aMapLocation.getLatitude() + ", " + aMapLocation.getLongitude() + 
                    ", 精度: " + aMapLocation.getAccuracy() + "米");
        } else {
            String errorInfo = aMapLocation != null ? aMapLocation.getErrorInfo() : "未知错误";
            Log.e(TAG, "定位失败: " + errorInfo);
            showError("定位失败: " + errorInfo);
        }
    }
    
    /**
     * 加载足迹数据
     */
    private void loadFootprints() {
        if (isLoadingFootprints.get()) {
            return;
        }
        
        isLoadingFootprints.set(true);
        // 这里可以添加加载指示器
        
        // 足迹数据通过ViewModel的LiveData自动更新
        Log.d(TAG, "开始加载足迹数据");
    }
    
    /**
     * 加载足迹动态
     */
    private void loadFootprintMessages() {
        if (isLoadingMessages.get()) {
            return;
        }
        
        isLoadingMessages.set(true);
        
        // 使用API服务加载足迹动态
        apiService.getFootprintMessages(
            1, // 页码
            10, // 每页大小
            // 成功回调
            new HutoolApiService.SuccessCallback<FootprintMessageResponse.Data>() {
                @Override
                public void onSuccess(FootprintMessageResponse.Data data) {
                    mainHandler.post(() -> {
                        isLoadingMessages.set(false);
                        if (data != null && data.getRecords() != null) {
                            handleFootprintMessages(data.getRecords());
                        }
                    });
                }
            },
            // 错误回调
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String error) {
                    mainHandler.post(() -> {
                        isLoadingMessages.set(false);
                        Log.e(TAG, "加载足迹动态失败: " + error);
                    });
                }
            }
        );
    }
    
    /**
     * 处理足迹动态数据
     */
    private void handleFootprintMessages(List<FootprintMessage> messages) {
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment未附加到Activity，跳过处理足迹动态数据");
            return;
        }
        
        if (messages == null || messages.isEmpty()) {
            Log.d(TAG, "足迹动态列表为空");
            return;
        }
        
        Log.d(TAG, "开始处理足迹动态数据，共 " + messages.size() + " 条记录");
        
        // 在地图上显示足迹动态标记
        for (FootprintMessage message : messages) {
            if (message.getLat() != 0 && message.getLng() != 0) {
                addFootprintMessageMarker(message);
            }
        }
    }
    
    /**
     * 添加足迹动态标记
     */
    private void addFootprintMessageMarker(FootprintMessage message) {
        try {
            LatLng position = new LatLng(message.getLat(), message.getLng());
            
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title(message.getTextContent())
                    .snippet("发布时间: " + message.getCreateBy());
            
            Marker marker = aMap.addMarker(markerOptions);
            marker.setObject(message); // 关联数据对象
            
        } catch (Exception e) {
            Log.e(TAG, "添加足迹动态标记失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 在地图上显示足迹
     */
    private void displayFootprintsOnMap(List<FootprintEntity> footprints) {
        if (!isMapInitialized || aMap == null || footprints == null) {
            return;
        }
        
        try {
            // 清除现有的足迹标记和轨迹
            aMap.clear();
            
            if (footprints.isEmpty()) {
                Log.d(TAG, "没有足迹数据需要显示");
                return;
            }
            
            // 创建轨迹线
            List<LatLng> points = new ArrayList<>();
            
            for (FootprintEntity footprint : footprints) {
                LatLng position = new LatLng(footprint.getLatitude(), footprint.getLongitude());
                points.add(position);
                
                // 添加足迹标记
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title(footprint.getLocationName())
                        .snippet(footprint.getDescription());
                
                Marker marker = aMap.addMarker(markerOptions);
                marker.setObject(footprint); // 关联足迹数据
            }
            
            // 绘制轨迹线
            if (points.size() > 1) {
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(points)
                        .width(5)
                        .color(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));
                
                Polyline polyline = aMap.addPolyline(polylineOptions);
            }
            
            // 调整地图视野以显示所有足迹
            if (!points.isEmpty()) {
                // 这里可以计算边界并调整地图视野
                Log.d(TAG, "足迹轨迹显示完成，共 " + footprints.size() + " 个足迹");
            }
            
            isLoadingFootprints.set(false);
            
        } catch (Exception e) {
            Log.e(TAG, "显示足迹失败: " + e.getMessage(), e);
            isLoadingFootprints.set(false);
        }
    }
    
    /**
     * 打开添加足迹Activity
     */
    private void openAddFootprintActivity() {
        try {
            Intent intent = new Intent(getContext(), AddFootprintActivity.class);
            if (lastLocation != null) {
                intent.putExtra("latitude", lastLocation.getLatitude());
                intent.putExtra("longitude", lastLocation.getLongitude());
                intent.putExtra("address", lastLocation.getAddress());
            }
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "打开添加足迹Activity失败: " + e.getMessage(), e);
            showError("无法打开添加足迹页面");
        }
    }
    
    /**
     * 显示错误信息
     */
    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限获取成功，初始化定位
                initLocation();
            } else {
                showError("需要位置权限才能使用地图功能");
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        startLocation();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        stopLocation();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 停止定位
        stopLocation();
        
        // 销毁定位客户端
        if (locationClient != null) {
            locationClient.onDestroy();
            locationClient = null;
        }
        
        // 销毁地图
        if (mapView != null) {
            mapView.onDestroy();
        }
        
        Log.d(TAG, "AMapFragment已销毁");
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }
}