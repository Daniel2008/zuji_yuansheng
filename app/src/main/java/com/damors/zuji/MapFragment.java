package com.damors.zuji;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.utils.GPSUtil;
import com.damors.zuji.viewmodel.FootprintViewModel;
import com.damors.zuji.network.HutoolApiService;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.response.FootprintMessageResponse;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 地图Fragment，显示用户位置和足迹
 * 优化版本：改进了性能、内存管理和用户体验
 * 
 * 主要优化点：
 * 1. 添加了状态管理，避免重复操作
 * 2. 改进了错误处理和重试机制
 * 3. 优化了内存使用，防止内存泄漏
 * 4. 增强了位置更新逻辑
 * 5. 添加了加载状态指示
 */
public class MapFragment extends Fragment implements LocationListener {

    private static final String TAG = "MapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001; // 位置权限请求码
    private static final long MIN_TIME_BETWEEN_UPDATES = 5000; // 5秒，提高响应性
    private static final float MIN_DISTANCE_CHANGE = 5; // 5米，提高精度
    private static final int MAX_RETRY_COUNT = 3; // 最大重试次数
    private static final long LOCATION_TIMEOUT = 30000; // 位置获取超时时间30秒
    
    // UI组件
    private MapView mapView;
    private FloatingActionButton addFootprintButton;
    
    // 核心服务
    private LocationManager locationManager;
    private FootprintViewModel viewModel;
    private HutoolApiService apiService;
    
    // 位置相关
    private Location lastLocation;
    private Marker currentLocationMarker;
    
    // 状态管理
    private final AtomicBoolean isLocationUpdating = new AtomicBoolean(false);
    private final AtomicBoolean isLoadingFootprints = new AtomicBoolean(false);
    private final AtomicBoolean isLoadingMessages = new AtomicBoolean(false);
    private boolean isMapInitialized = false;
    
    // 线程处理
    private Handler mainHandler;
    private int retryCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        
        try {
            // 初始化主线程Handler
            mainHandler = new Handler(Looper.getMainLooper());
            
            // 初始化核心组件
            initializeComponents(view);
            
            // 初始化地图
            initializeMap();
            
            // 设置UI事件监听器
            setupUIListeners();
            
            // 初始化位置服务
            initializeLocationService();
            
            Log.d(TAG, "地图Fragment初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "地图Fragment初始化失败: " + e.getMessage(), e);
            showErrorToast("地图初始化失败，请重试");
        }
        
        return view;
    }
    
    /**
     * 初始化核心组件
     */
    private void initializeComponents(View view) {
        // 初始化ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(FootprintViewModel.class);
        
        // 初始化API服务
        apiService = HutoolApiService.getInstance(requireContext());
        
        // 初始化UI组件
        mapView = view.findViewById(R.id.map);
        addFootprintButton = view.findViewById(R.id.btn_add_footprint);
        
        // 初始化位置管理器
        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        
        Log.d(TAG, "核心组件初始化完成");
    }
    
    /**
     * 初始化地图配置
     */
    private void initializeMap() {
        if (mapView == null) {
            throw new IllegalStateException("MapView未正确初始化");
        }
        
        try {
            // OSMDroid配置已在Application中初始化，这里直接设置瓦片源
            final ITileSource tileSource = new XYTileSource("AutoNavi-Vector",
                    5, 18, 256, ".png", new String[]{
                    "https://wprd01.is.autonavi.com/appmaptile?",
                    "https://wprd02.is.autonavi.com/appmaptile?",
                    "https://wprd03.is.autonavi.com/appmaptile?",
                    "https://wprd04.is.autonavi.com/appmaptile?",
            }) {
                @Override
                public String getTileURLString(long pMapTileIndex) {
                    return getBaseUrl() + "x=" + MapTileIndex.getX(pMapTileIndex) + 
                           "&y=" + MapTileIndex.getY(pMapTileIndex) + 
                           "&z=" + MapTileIndex.getZoom(pMapTileIndex) + 
                           "&lang=zh_cn&size=1&scl=1&style=7&ltype=7";
                }
            };
            
            mapView.setTileSource(tileSource);
            mapView.setMultiTouchControls(true);
            mapView.getController().setZoom(17.0);
            
            isMapInitialized = true;
            Log.d(TAG, "地图初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "地图初始化失败: " + e.getMessage(), e);
            throw new RuntimeException("地图初始化失败", e);
        }
    }
    
    /**
     * 设置UI事件监听器
     */
    private void setupUIListeners() {
        if (addFootprintButton != null) {
            addFootprintButton.setOnClickListener(v -> addCurrentLocationFootprint());
        }
    }
    
    /**
     * 初始化位置服务
     */
    private void initializeLocationService() {
        if (locationManager == null) {
            throw new IllegalStateException("LocationManager未正确初始化");
        }
        
        // 延迟加载数据，避免阻塞UI
        mainHandler.post(() -> {
            enableMyLocation();
            loadFootprints();
            loadFootprintMessages();
        });
    }
    
    /**
     * 显示错误提示
     */
    private void showErrorToast(String message) {
        if (isAdded() && getContext() != null && mainHandler != null) {
            mainHandler.post(() -> {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    /**
     * 显示信息提示
     */
    private void showInfoToast(String message) {
        if (isAdded() && getContext() != null && mainHandler != null) {
            mainHandler.post(() -> {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }





    /**
     * 开始位置更新（优化版本）
     * 添加了状态检查、重试机制和更好的错误处理
     */
    private void startLocationUpdates() {
        // 检查是否已在更新中，避免重复请求
        if (isLocationUpdating.get()) {
            Log.d(TAG, "位置更新已在进行中，跳过重复请求");
            return;
        }
        
        // 检查权限
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "缺少位置权限，无法开始位置更新");
            return;
        }
        
        if (locationManager == null) {
            Log.e(TAG, "LocationManager未初始化");
            return;
        }
        
        try {
            // 检查定位服务可用性
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
            if (!isGPSEnabled && !isNetworkEnabled) {
                showErrorToast("请开启GPS或网络定位服务");
                Log.w(TAG, "所有定位服务都不可用");
                return;
            }
            
            isLocationUpdating.set(true);
            
            // 优先使用GPS，备用网络定位
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_CHANGE,
                        this);
                Log.d(TAG, "已启动GPS位置更新");
            }
            
            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_CHANGE,
                        this);
                Log.d(TAG, "已启动网络位置更新");
            }
            
            // 设置超时检查
            mainHandler.postDelayed(this::checkLocationTimeout, LOCATION_TIMEOUT);
            
        } catch (SecurityException e) {
            Log.e(TAG, "位置权限异常: " + e.getMessage());
            isLocationUpdating.set(false);
            showErrorToast("位置权限被拒绝");
        } catch (Exception e) {
            Log.e(TAG, "位置更新请求失败: " + e.getMessage(), e);
            isLocationUpdating.set(false);
            handleLocationUpdateError();
        }
    }
    
    /**
     * 检查位置获取超时
     */
    private void checkLocationTimeout() {
        if (isLocationUpdating.get() && lastLocation == null) {
            Log.w(TAG, "位置获取超时");
            if (retryCount < MAX_RETRY_COUNT) {
                retryCount++;
                Log.d(TAG, "重试位置更新，第 " + retryCount + " 次");
                stopLocationUpdates();
                mainHandler.postDelayed(this::startLocationUpdates, 2000); // 2秒后重试
            } else {
                showErrorToast("无法获取位置信息，请检查定位设置");
                isLocationUpdating.set(false);
            }
        }
    }
    
    /**
     * 处理位置更新错误
     */
    private void handleLocationUpdateError() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "位置更新失败，准备重试第 " + retryCount + " 次");
            mainHandler.postDelayed(this::startLocationUpdates, 3000); // 3秒后重试
        } else {
            showErrorToast("位置服务异常，请重启应用或检查设备设置");
        }
    }

    /**
     * 停止位置更新（优化版本）
     */
    private void stopLocationUpdates() {
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(this);
                isLocationUpdating.set(false);
                Log.d(TAG, "已停止位置更新");
            }
            
            // 清除超时检查
            if (mainHandler != null) {
                mainHandler.removeCallbacks(this::checkLocationTimeout);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "停止位置更新时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 更新地图位置（优化版本）
     * 添加了坐标转换和错误处理
     */
    private void updateMapLocation(Location location) {
        if (!isMapInitialized || mapView == null || location == null) {
            Log.w(TAG, "地图未初始化或位置信息无效，跳过位置更新");
            return;
        }
        
        try {
            // 转换为火星坐标系
            double[] locationPoint = GPSUtil.gps84_To_Gcj02(location.getLatitude(), location.getLongitude());
            GeoPoint currentLocation = new GeoPoint(locationPoint[0], locationPoint[1]);
            
            // 平滑移动到新位置
            mapView.getController().animateTo(currentLocation);
            
            Log.d(TAG, "地图位置已更新: " + locationPoint[0] + ", " + locationPoint[1]);
            
        } catch (Exception e) {
            Log.e(TAG, "更新地图位置失败: " + e.getMessage(), e);
        }
    }



    /**
     * 启用我的位置功能（优化版本）
     * 改进了位置获取逻辑和错误处理
     */
    public void enableMyLocation() {
        if (!isMapInitialized) {
            Log.w(TAG, "地图未初始化，延迟启用位置功能");
            mainHandler.postDelayed(this::enableMyLocation, 1000);
            return;
        }
        
        // 检查权限
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "缺少位置权限，无法启用位置功能");
            return;
        }
        
        if (locationManager == null) {
            Log.e(TAG, "LocationManager未初始化");
            return;
        }
        
        try {
            Location bestLocation = getBestLastKnownLocation();
            
            if (bestLocation != null) {
                updateCurrentLocationMarker(bestLocation);
                updateMapLocation(bestLocation);
                lastLocation = bestLocation;
                retryCount = 0; // 重置重试计数
                Log.d(TAG, "位置功能启用成功");
            } else {
                Log.d(TAG, "无可用的历史位置，启动位置更新");
                startLocationUpdates();
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "位置权限异常: " + e.getMessage());
            showErrorToast("位置权限被拒绝");
        } catch (Exception e) {
            Log.e(TAG, "启用位置功能失败: " + e.getMessage(), e);
            showErrorToast("位置服务异常");
        }
    }
    
    /**
     * 获取最佳的历史位置信息
     */
    private Location getBestLastKnownLocation() throws SecurityException {
        Location gpsLocation = null;
        Location networkLocation = null;
        
        // 尝试获取GPS位置
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        
        // 尝试获取网络位置
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        
        // 选择最佳位置（优先GPS，考虑时间新旧）
        if (gpsLocation != null && networkLocation != null) {
            // 如果GPS位置较新或精度更高，优先使用GPS
            long timeDiff = gpsLocation.getTime() - networkLocation.getTime();
            if (timeDiff > -60000 || gpsLocation.getAccuracy() < networkLocation.getAccuracy()) { // 1分钟内或精度更高
                return gpsLocation;
            } else {
                return networkLocation;
            }
        } else if (gpsLocation != null) {
            return gpsLocation;
        } else {
            return networkLocation;
        }
    }
    
    /**
     * 更新当前位置标记
     */
    private void updateCurrentLocationMarker(Location location) {
        if (mapView == null || location == null) {
            return;
        }
        
        try {
            // 转换为火星坐标系
            double[] locationPoint = GPSUtil.gps84_To_Gcj02(location.getLatitude(), location.getLongitude());
            GeoPoint currentLocation = new GeoPoint(locationPoint[0], locationPoint[1]);
            
            if (currentLocationMarker == null) {
                // 创建新的位置标记
                currentLocationMarker = new Marker(mapView);
                currentLocationMarker.setPosition(currentLocation);
                currentLocationMarker.setTitle("我的位置");
                currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                currentLocationMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_location_marker));
                mapView.getOverlays().add(currentLocationMarker);
                Log.d(TAG, "已创建当前位置标记");
            } else {
                // 更新现有标记位置
                currentLocationMarker.setPosition(currentLocation);
                Log.d(TAG, "已更新当前位置标记");
            }
            
            // 刷新地图
            mapView.invalidate();
            
        } catch (Exception e) {
            Log.e(TAG, "更新位置标记失败: " + e.getMessage(), e);
        }
    }

    /**
     * 加载并显示所有足迹点（优化版本）
     * 改进了加载逻辑和状态管理
     */
    private void loadFootprints() {
        if (isLoadingFootprints.get()) {
            Log.d(TAG, "正在加载足迹点，跳过重复请求");
            return;
        }
        
        if (!isMapInitialized) {
            Log.w(TAG, "地图未初始化，延迟加载足迹点");
            mainHandler.postDelayed(this::loadFootprints, 1000);
            return;
        }
        
        isLoadingFootprints.set(true);
        Log.d(TAG, "开始加载足迹点");
        
        try {
            viewModel.getAllFootprints().observe(getViewLifecycleOwner(), footprints -> {
                try {
                    if (mapView != null && footprints != null && !footprints.isEmpty()) {
                        displayFootprintsOnMap(footprints);
                        Log.d(TAG, "足迹点加载完成，共 " + footprints.size() + " 个点");
                    } else {
                        Log.d(TAG, "无足迹点数据");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "显示足迹点失败: " + e.getMessage(), e);
                    showErrorToast("显示足迹点失败");
                } finally {
                    isLoadingFootprints.set(false);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "加载足迹点失败: " + e.getMessage(), e);
            showErrorToast("加载足迹点失败");
            isLoadingFootprints.set(false);
        }
    }

    /**
     * 在地图上显示足迹点
     */
    private void displayFootprintsOnMap(List<FootprintEntity> footprints) {
        // 清除所有覆盖层，但保留当前位置标记
        Marker tempCurrentLocationMarker = currentLocationMarker;
        mapView.getOverlays().clear();
        currentLocationMarker = tempCurrentLocationMarker; // 恢复当前位置标记引用
        
        // 创建连线
        Polyline polyline = new Polyline();
        polyline.setColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));
        polyline.setWidth(5f);
        List<GeoPoint> points = new ArrayList<>();
        
        for (FootprintEntity footprint : footprints) {
            GeoPoint position = new GeoPoint(footprint.getLatitude(), footprint.getLongitude());
            
            // 添加标记
            Marker marker = new Marker(mapView);
            marker.setPosition(position);
            marker.setTitle(footprint.getDescription());
            marker.setSnippet("时间: " + new Date(footprint.getTimestamp()).toString());
            mapView.getOverlays().add(marker);
            
            // 添加到连线
            points.add(position);
        }
        
        // 绘制足迹路线
        polyline.setPoints(points);
        mapView.getOverlays().add(polyline);
        
        // 如果当前位置标记存在，重新添加到覆盖层以确保它在最上层
        if (currentLocationMarker != null) {
            mapView.getOverlays().add(currentLocationMarker);
            Log.d(TAG, "在显示足迹后重新添加当前位置标记");
        }
        
        // 移动相机到最后一个足迹点
        if (!footprints.isEmpty()) {
            FootprintEntity lastFootprint = footprints.get(footprints.size() - 1);
            GeoPoint lastPosition = new GeoPoint(lastFootprint.getLatitude(), lastFootprint.getLongitude());
            mapView.getController().animateTo(lastPosition);
            mapView.getController().setZoom(15.0);
        }
        
        // 刷新地图
        mapView.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
    
    /**
     * 位置变化回调
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (location == null) {
            Log.w(TAG, "接收到空位置信息");
            return;
        }
        
        try {
            Log.d(TAG, String.format("位置更新: %.6f, %.6f (精度: %.1fm)", 
                location.getLatitude(), location.getLongitude(), location.getAccuracy()));
            
            // 检查位置精度，过滤低精度位置
            if (location.getAccuracy() > 100) { // 精度大于100米的位置可能不准确
                Log.w(TAG, "位置精度较低，跳过更新: " + location.getAccuracy() + "m");
                return;
            }
            
            // 检查位置变化距离，避免频繁更新
            if (lastLocation != null) {
                float distance = location.distanceTo(lastLocation);
                if (distance < MIN_DISTANCE_CHANGE && 
                    (System.currentTimeMillis() - lastLocation.getTime()) < MIN_TIME_BETWEEN_UPDATES) {
                    Log.d(TAG, "位置变化距离过小，跳过更新: " + distance + "m");
                    return;
                }
            }
            
            // 清除超时检查
            mainHandler.removeCallbacks(this::checkLocationTimeout);
            
            // 更新地图位置和标记
            updateMapLocation(location);
            updateCurrentLocationMarker(location);
            
            // 保存最后位置
            lastLocation = location;
            retryCount = 0; // 重置重试计数
            
            Log.d(TAG, "位置更新成功");
            
        } catch (Exception e) {
            Log.e(TAG, "处理位置更新失败: " + e.getMessage(), e);
            handleLocationUpdateError();
        }
    }
    
    /**
     * 位置提供者状态变化回调
     */
    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.d(TAG, "位置提供者已启用: " + provider);
        
        // 如果GPS启用且当前没有在更新位置，尝试重新启动位置更新
        if (LocationManager.GPS_PROVIDER.equals(provider) && !isLocationUpdating.get()) {
            Log.d(TAG, "GPS已启用，尝试重新启动位置更新");
            mainHandler.postDelayed(() -> {
                if (!isLocationUpdating.get()) {
                    startLocationUpdates();
                }
            }, 1000);
        }
        
        showInfoToast("位置服务已启用: " + getProviderDisplayName(provider));
    }
    
    /**
     * 位置提供者状态变化回调
     */
    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.w(TAG, "位置提供者已禁用: " + provider);
        
        // 如果GPS被禁用，显示提示信息
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            showErrorToast("GPS已关闭，位置精度可能降低");
            
            // 如果网络定位也不可用，停止位置更新
            if (locationManager != null && 
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.w(TAG, "所有位置提供者都不可用，停止位置更新");
                stopLocationUpdates();
                showErrorToast("位置服务不可用，请检查设置");
            }
        }
    }
    
    /**
     * 获取位置提供者的显示名称
     */
    private String getProviderDisplayName(String provider) {
        switch (provider) {
            case LocationManager.GPS_PROVIDER:
                return "GPS";
            case LocationManager.NETWORK_PROVIDER:
                return "网络定位";
            case LocationManager.PASSIVE_PROVIDER:
                return "被动定位";
            default:
                return provider;
        }
    }
    
    /**
     * 在当前位置添加足迹点（优化版本）
     * 改进了位置获取和错误处理逻辑
     */
    private void addCurrentLocationFootprint() {
        try {
            // 检查位置权限
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "需要位置权限来添加足迹", Toast.LENGTH_SHORT).show();
                }
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }
            
            Location location = getCurrentBestLocation();
            
            if (location != null) {
                // 检查位置精度
                if (location.getAccuracy() > 50) {
                    Log.w(TAG, "当前位置精度较低: " + location.getAccuracy() + "m");
                    showInfoToast("位置精度较低，建议等待GPS信号更好时再添加");
                }
                
                // 转换为火星坐标系
                double[] locationPoint = GPSUtil.gps84_To_Gcj02(location.getLatitude(), location.getLongitude());
                
                // 跳转到足迹详情页面
                Intent intent = new Intent(getActivity(), AddFootprintActivity.class);
                intent.putExtra("latitude", locationPoint[0]);
                intent.putExtra("longitude", locationPoint[1]);
                intent.putExtra("altitude", location.getAltitude());
                intent.putExtra("accuracy", location.getAccuracy());
                startActivity(intent);
                
                Log.d(TAG, "准备添加足迹 - 纬度: " + locationPoint[0] + ", 经度: " + locationPoint[1]);
            } else {
                handleNoLocationAvailable();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "位置权限错误: " + e.getMessage());
            showErrorToast("位置权限被拒绝，请检查应用权限设置");
        } catch (Exception e) {
            Log.e(TAG, "添加足迹时出错: " + e.getMessage(), e);
            showErrorToast("添加足迹时出错: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前最佳位置
     */
    private Location getCurrentBestLocation() throws SecurityException {
        // 优先使用最近的位置信息
        if (lastLocation != null) {
            long timeDiff = System.currentTimeMillis() - lastLocation.getTime();
            if (timeDiff < 300000) { // 5分钟内的位置认为是有效的
                Log.d(TAG, "使用最近的位置信息: " + timeDiff + "ms前");
                return lastLocation;
            }
        }
        
        // 获取历史最佳位置
        return getBestLastKnownLocation();
    }
    
    /**
     * 处理无位置可用的情况
     */
    private void handleNoLocationAvailable() {
        // 检查位置服务是否可用
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        if (!gpsEnabled && !networkEnabled) {
            showErrorToast("位置服务未开启，请在设置中开启GPS或网络定位");
        } else if (!isLocationUpdating.get()) {
            showInfoToast("正在获取位置信息，请稍候...");
            startLocationUpdates();
            
            // 延迟重试
            mainHandler.postDelayed(() -> {
                if (lastLocation != null) {
                    addCurrentLocationFootprint();
                } else {
                    showErrorToast("无法获取位置信息，请检查GPS设置或移动到信号更好的地方");
                }
            }, 5000);
        } else {
            showErrorToast("无法获取位置信息，请检查GPS设置或移动到信号更好的地方");
        }
    }
    
    /**
     * 获取足迹动态列表（优化版本）
     * 改进了加载逻辑、错误处理和状态管理
     */
    private void loadFootprintMessages() {
        if (isLoadingMessages.get()) {
            Log.d(TAG, "正在加载足迹动态，跳过重复请求");
            return;
        }
        
        if (!isMapInitialized) {
            Log.w(TAG, "地图未初始化，延迟加载足迹动态");
            mainHandler.postDelayed(this::loadFootprintMessages, 1000);
            return;
        }
        
        if (getContext() == null) {
            Log.w(TAG, "Context为空，无法加载足迹动态");
            return;
        }
        
        isLoadingMessages.set(true);
        Log.d(TAG, "开始获取足迹动态列表");
        
        try {
            // 调用API获取足迹动态列表
            apiService.getFootprintMessages(
                1, // 页码
                10, // 每页大小
                    response -> {
                        try {
                            Log.d(TAG, "获取足迹动态列表成功，记录数: " + response.getRecords().size());

                            // 在主线程更新UI
                            if (mainHandler != null) {
                                mainHandler.post(() -> {
                                    try {
                                        handleFootprintMessages(response.getRecords());
                                        Log.d(TAG, "足迹动态显示完成");
                                    } catch (Exception e) {
                                        Log.e(TAG, "显示足迹动态失败: " + e.getMessage(), e);
                                        showErrorToast("显示足迹动态失败");
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "处理足迹动态响应失败: " + e.getMessage(), e);
                            showErrorToast("处理数据失败");
                        } finally {
                            isLoadingMessages.set(false);
                        }
                    },
                    errorMessage -> {
                        Log.e(TAG, "获取足迹动态列表失败: " + errorMessage);

                        String displayMessage;
                        if (errorMessage.contains("timeout")) {
                            displayMessage = "网络连接超时，请重试";
                        } else if (errorMessage.contains("network")) {
                            displayMessage = "网络连接失败，请检查网络设置";
                        } else {
                            displayMessage = "获取足迹动态失败: " + errorMessage;
                        }

                        showErrorToast(displayMessage);
                        isLoadingMessages.set(false);
                    }
            );
            
        } catch (Exception e) {
            Log.e(TAG, "启动足迹动态加载失败: " + e.getMessage(), e);
            showErrorToast("启动数据加载失败");
            isLoadingMessages.set(false);
        }
    }
    
    /**
     * 处理足迹动态数据
     * 在地图上显示足迹动态位置标记
     * @param messages 足迹动态列表
     */
    private void handleFootprintMessages(List<FootprintMessage> messages) {
        // 检查Fragment是否仍然附加到Activity，避免IllegalStateException
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
            // 创建足迹动态标记
            Marker messageMarker = new Marker(mapView);
            GeoPoint position = new GeoPoint(message.getLat(), message.getLng());
            messageMarker.setPosition(position);
            messageMarker.setTitle(message.getCreateBy() + " - " + message.getTag());
            messageMarker.setSnippet(message.getTextContent());
            messageMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            // 设置不同的图标以区分足迹动态和普通足迹
            messageMarker.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_message_marker));
            
            // 添加到地图
            mapView.getOverlays().add(messageMarker);
            
            Log.d(TAG, "添加足迹动态标记: " + message.getCreateBy() + " - " + message.getTextContent());
        }
        
        // 刷新地图
        mapView.invalidate();
    }
    
    /**
     * 刷新足迹动态列表
     * 可以在需要时调用此方法重新加载数据
     */
    public void refreshFootprintMessages() {
        loadFootprintMessages();
    }
    
    /**
     * 刷新地图（优化版本）
     * 改进了刷新逻辑和状态管理
     */
    public void refreshMap() {
        if (!isMapInitialized || mapView == null) {
            Log.w(TAG, "地图未初始化，无法刷新");
            return;
        }
        
        try {
            Log.d(TAG, "开始刷新地图");
            
            // 防止重复刷新
            if (isLoadingFootprints.get() || isLoadingMessages.get()) {
                Log.d(TAG, "正在加载数据，跳过重复刷新");
                return;
            }
            
            // 在后台线程执行数据加载
            mainHandler.post(() -> {
                // 重新加载足迹点
                loadFootprints();
                // 重新加载足迹动态
                loadFootprintMessages();
                
                // 延迟刷新地图显示，确保数据加载完成
                mainHandler.postDelayed(() -> {
                    if (mapView != null) {
                        mapView.invalidate();
                        Log.d(TAG, "地图刷新完成");
                    }
                }, 500);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "刷新地图失败: " + e.getMessage(), e);
            showErrorToast("地图刷新失败");
        }
    }
    
    @Override
    public void onDestroy() {
        try {
            Log.d(TAG, "开始销毁MapFragment");
            
            // 停止位置更新
            stopLocationUpdates();
            
            // 清除所有Handler回调
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
                mainHandler = null;
            }
            
            // 重置状态标志
            isLocationUpdating.set(false);
            isLoadingFootprints.set(false);
            isLoadingMessages.set(false);
            isMapInitialized = false;
            
            // 清理地图资源
            if (mapView != null) {
                try {
                    mapView.getOverlays().clear();
                    mapView.onDetach();
                } catch (Exception e) {
                    Log.w(TAG, "清理地图资源时出现异常: " + e.getMessage());
                }
            }
            
            // 清理标记引用
            currentLocationMarker = null;
            lastLocation = null;
            locationManager = null;
            
            Log.d(TAG, "MapFragment资源清理完成");
            
        } catch (Exception e) {
            Log.e(TAG, "销毁MapFragment时出现异常: " + e.getMessage(), e);
        } finally {
            super.onDestroy();
        }
    }
}