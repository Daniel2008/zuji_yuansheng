package com.damors.zuji;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.CameraPosition;
import com.damors.zuji.adapter.GridImageAdapter;
import com.damors.zuji.model.GuluFile;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.utils.ImageUtils;
import com.damors.zuji.CommentListActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;
import androidx.appcompat.app.AlertDialog;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.damors.zuji.data.FootprintEntity;

import com.damors.zuji.viewmodel.FootprintViewModel;
import com.damors.zuji.network.HutoolApiService;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.response.FootprintMessageResponse;
// Glide图片加载库导入
import com.bumptech.glide.Glide;
// 高德地图相关导入
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

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
public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001; // 位置权限请求码
    private static final long MIN_TIME_BETWEEN_UPDATES = 10000; // 增加到3秒，减少频繁更新
    private static final float MIN_DISTANCE_CHANGE = 5; // 5米，提高精度
    private static final int MAX_RETRY_COUNT = 3; // 最大重试次数
    private static final long LOCATION_TIMEOUT = 30000; // 位置获取超时时间30秒
    private static final long RETRY_DELAY_MS = 1000;
    
    // 缓存配置
    private static final int MAX_MARKER_CACHE_SIZE = 100;
    
    // UI组件
    private MapView mapView;
    private FloatingActionButton addFootprintButton;
    
    // 高德地图相关
    private AMap aMap;
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    private boolean isDragging = false;

    // 核心服务
    private FootprintViewModel viewModel;
    private HutoolApiService apiService;
    
    // 位置相关
    private AMapLocation lastAMapLocation;
    private Marker currentLocationMarker;
    
    // 状态管理 - 优化版本
    private final AtomicBoolean isLocationUpdating = new AtomicBoolean(false);
    private final AtomicBoolean isLoadingFootprints = new AtomicBoolean(false);
    private final AtomicBoolean isLoadingMessages = new AtomicBoolean(false);
    private final AtomicBoolean isMapInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isFragmentDestroyed = new AtomicBoolean(false);
    private boolean pendingAddFootprint = false; // 标记是否有待处理的添加足迹请求
    
    // 线程处理
    private Handler mainHandler;
    private volatile int retryCount = 0;
    
    // 缓存管理 - 优化内存使用
    private final java.util.concurrent.ConcurrentHashMap<String, Marker> markerCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.List<FootprintMessage> cachedMessages = new java.util.ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        
        try {
            // 重置销毁状态
            isFragmentDestroyed.set(false);
            
            // 初始化主线程Handler
            mainHandler = new Handler(Looper.getMainLooper());
            
            // 初始化核心组件
            initializeComponents(view);
            
            // 初始化地图视图生命周期
            if (mapView != null) {
                mapView.onCreate(savedInstanceState);
            }
            
            // 异步初始化地图，避免阻塞UI
            mainHandler.post(this::initializeMapAsync);
            
            Log.d(TAG, "地图Fragment初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "地图Fragment初始化失败: " + e.getMessage(), e);
            showToast("地图初始化失败，请重试");
        }
        
        return view;
    }
    
    /**
     * 异步初始化地图
     */
    private void initializeMapAsync() {
        try {
            initializeMap();
            setupUIListeners();
            initializeLocationService();
        } catch (Exception e) {
            Log.e(TAG, "异步地图初始化失败: " + e.getMessage(), e);
            showToast("地图初始化失败");
        }
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
        

        
        Log.d(TAG, "核心组件初始化完成");
    }
    
    /**
     * 初始化高德地图配置 - 优化版本
     */
    private void initializeMap() {
        if (mapView == null) {
            throw new IllegalStateException("MapView未正确初始化");
        }
        
        if (isFragmentDestroyed.get()) {
            Log.w(TAG, "Fragment已销毁，跳过地图初始化");
            return;
        }
        
        try {
            // 获取高德地图实例
            aMap = mapView.getMap();
            
            configureMapSettings();
            setupMapListeners();
            
            // 标记地图已初始化
            isMapInitialized.set(true);
            retryCount = 0; // 重置重试计数器
            
            Log.d(TAG, "高德地图初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "高德地图初始化失败: " + e.getMessage(), e);
            throw new RuntimeException("高德地图初始化失败", e);
        }
    }
    
    /**
     * 配置地图设置和样式
     */
    private void configureMapSettings() {
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setScaleControlsEnabled(true);
        aMap.getUiSettings().setMyLocationButtonEnabled(true); //显示默认的定位按钮
        aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
        aMap.setMyLocationEnabled(true);
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        myLocationStyle.interval(2000);
        aMap.setMyLocationStyle(myLocationStyle);
        isDragging = true;
    }
    
    /**
     * 设置地图事件监听器
     */
    private void setupMapListeners() {
        // 设置标记点击监听器
        aMap.setOnMarkerClickListener(marker -> {
            Object footprintObj = marker.getObject();
            if (footprintObj instanceof FootprintMessage) {
                showFootprintMessageInfoCard((FootprintMessage) footprintObj);
                return true;
            }
            return false;
        });

        aMap.setOnMapTouchListener(new AMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent motionEvent) {
                isDragging = false;
            }
        });

        // 设置地图加载完成监听器
        aMap.setOnMapLoadedListener(() -> {
            Log.d(TAG, "地图加载完成");
            retryCount = 0; // 重置重试计数器
        });
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
     * 初始化位置服务 - 优化版本
     */
    private void initializeLocationService() {
        // 检查Fragment状态
        if (isFragmentDestroyed.get()) {
            Log.w(TAG, "Fragment已销毁，跳过位置服务初始化");
            return;
        }
        
        // 检查是否已经初始化过位置服务，避免重复初始化
        if (isLocationUpdating.get()) {
            Log.d(TAG, "位置服务已初始化，跳过重复初始化");
            return;
        }
        
        // 延迟加载数据，避免阻塞UI
        if (mainHandler != null) {
            mainHandler.post(() -> {
                if (!isFragmentDestroyed.get()) {
                    enableMyLocation();
                    loadFootprintMessages();
                }
            });
        }
    }
    
    /**
     * 统一的Toast显示方法
     */
    private void showToast(String message) {
        if (isAdded() && getContext() != null && mainHandler != null) {
            mainHandler.post(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }
    
    /**
     * 更新点赞状态UI
     * @param ivLike 点赞图标
     * @param tvLikeCount 点赞数量文本
     * @param isLiked 是否已点赞
     * @param likeCount 点赞数量
     */
    private void updateLikeStatus(ImageView ivLike, TextView tvLikeCount, boolean isLiked, int likeCount) {
        if (getContext() == null) return;
        
        if (isLiked) {
            ivLike.setImageResource(R.drawable.ic_like_filled);
            ivLike.setColorFilter(getContext().getResources().getColor(R.color.action_icon_active_color));
            tvLikeCount.setTextColor(getContext().getResources().getColor(R.color.action_text_active_color));
        } else {
            ivLike.setImageResource(R.drawable.ic_like_outline);
            ivLike.setColorFilter(getContext().getResources().getColor(R.color.action_icon_color));
            tvLikeCount.setTextColor(getContext().getResources().getColor(R.color.action_text_color));
        }
        tvLikeCount.setText(String.valueOf(likeCount));
    }





    /**
     * 开始高德地图位置更新 - 优化版本
     */
    private void startLocationUpdates() {
        // 检查Fragment状态
        if (isFragmentDestroyed.get()) {
            Log.w(TAG, "Fragment已销毁，跳过位置更新");
            return;
        }
        
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
        
        try {
            // 初始化高德定位客户端
            if (locationClient == null) {
                initializeLocationClient();
            }
            
            isLocationUpdating.set(true);
            locationClient.startLocation();
            Log.d(TAG, "已启动高德地图位置更新");
            
        } catch (Exception e) {
            Log.e(TAG, "高德定位启动失败: " + e.getMessage(), e);
            isLocationUpdating.set(false);
            showToast("定位服务启动失败");
        }
    }
    
    /**
     * 初始化定位客户端
     */
    private void initializeLocationClient() {
        try {
            locationClient = new AMapLocationClient(requireContext());
            
            // 设置定位参数
            locationOption = new AMapLocationClientOption();
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            locationOption.setInterval(MIN_TIME_BETWEEN_UPDATES);
            locationOption.setNeedAddress(true);
            locationOption.setOnceLocation(false);
            locationOption.setWifiScan(true);
            locationOption.setMockEnable(false);
            
            locationClient.setLocationOption(locationOption);
            
            // 设置定位监听
            locationClient.setLocationListener(this::handleLocationResult);
            
            Log.d(TAG, "定位客户端初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "初始化定位客户端失败: " + e.getMessage(), e);
            locationClient = null;
            locationOption = null;
            showToast("定位服务初始化失败");
        }
    }
    
    /**
     * 处理定位结果 - 优化版本
     */
    private void handleLocationResult(AMapLocation aMapLocation) {
        if (aMapLocation == null || isFragmentDestroyed.get()) {
            return;
        }
        
        if (aMapLocation.getErrorCode() == 0) {
            // 定位成功
            lastAMapLocation = aMapLocation;
            Log.d(TAG, "高德定位成功: " + aMapLocation.getLatitude() + ", " + aMapLocation.getLongitude() + 
                  ", 城市: " + aMapLocation.getCity() + ", 地址: " + aMapLocation.getAddress());
            if(isDragging){
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()), 17));
            }

        } else {
            // 定位失败
            Log.e(TAG, "高德定位失败: " + aMapLocation.getErrorCode() + ", " + aMapLocation.getErrorInfo());
            handleLocationError(aMapLocation.getErrorInfo());
        }
    }
    
    /**
     * 处理定位错误
     */
    private void handleLocationError(String errorInfo) {
        String userMessage;
        if (errorInfo.contains("网络")) {
            userMessage = "网络连接异常，请检查网络设置";
        } else if (errorInfo.contains("权限")) {
            userMessage = "缺少定位权限，请在设置中开启";
        } else {
            userMessage = "定位失败，请稍后重试";
        }
        showToast(userMessage);
    }

    /**
     * 停止高德地图位置更新 - 优化版本
     */
    private void stopLocationUpdates() {
        if (!isLocationUpdating.get()) {
            Log.d(TAG, "位置更新未在进行中，无需停止");
            return;
        }
        
        try {
            if (locationClient != null) {
                locationClient.stopLocation();
                Log.d(TAG, "已停止高德地图位置更新");
            }
        } catch (Exception e) {
            Log.e(TAG, "停止高德地图位置更新时发生错误: " + e.getMessage(), e);
        } finally {
            isLocationUpdating.set(false);
        }
    }

    /**
     * 启用我的位置功能 - 优化版本
     */
    public void enableMyLocation() {
        if (isFragmentDestroyed.get()) {
            Log.w(TAG, "Fragment已销毁，跳过位置功能启用");
            return;
        }
        
        if (!isMapInitialized.get()) {
            // 增加重试次数限制，避免无限递归调用
            if (retryCount < MAX_RETRY_COUNT) {
                retryCount++;
                Log.w(TAG, "地图未初始化，延迟启用位置功能 (重试次数: " + retryCount + "/" + MAX_RETRY_COUNT + ")");
                if (mainHandler != null) {
                    mainHandler.postDelayed(this::enableMyLocation, RETRY_DELAY_MS);
                }
            } else {
                Log.e(TAG, "地图初始化超时，停止重试启用位置功能");
                showToast("地图初始化失败，请重启应用");
            }
            return;
        }
        
        // 检查权限
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "缺少位置权限，请求权限");
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        try {
            // 启用地图的我的位置功能
            if (aMap != null) {
                aMap.setMyLocationEnabled(true);
            }
            
            // 重置重试计数器
            retryCount = 0;
            
            // 启动高德定位
            startLocationUpdates();
            
        } catch (SecurityException e) {
            Log.e(TAG, "位置权限异常: " + e.getMessage());
            showToast("位置权限被拒绝");
        } catch (Exception e) {
            Log.e(TAG, "启用位置功能失败: " + e.getMessage(), e);
            showToast("位置服务异常");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        try {
            if (mapView != null) {
                mapView.onResume();
            }
            
            // 只有在地图已初始化且位置更新未运行时才启动位置更新
            if (isMapInitialized.get() && !isLocationUpdating.get() && !isFragmentDestroyed.get()) {
                Log.d(TAG, "Fragment恢复，启动位置更新");
                startLocationUpdates();
            } else {
                Log.d(TAG, "Fragment恢复，但跳过位置更新 - 地图初始化: " + isMapInitialized.get() + 
                      ", 位置更新中: " + isLocationUpdating.get() + ", Fragment销毁: " + isFragmentDestroyed.get());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Fragment恢复时出错: " + e.getMessage(), e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        
        try {
            if (mapView != null) {
                mapView.onPause();
            }
            
            // 暂停时停止位置更新以节省电量
            stopLocationUpdates();
            
        } catch (Exception e) {
            Log.e(TAG, "Fragment暂停时出错: " + e.getMessage(), e);
        }
    }
    


    private void addCurrentLocationFootprint() {
        try {
            // 检查位置权限
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "需要位置权限来添加足迹", Toast.LENGTH_SHORT).show();
                }
                // 标记有待处理的添加足迹请求
                pendingAddFootprint = true;
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }
            
            if (lastAMapLocation != null) {
                // 检查位置精度
                if (lastAMapLocation.getAccuracy() > 50) {
                    Log.w(TAG, "当前位置精度较低: " + lastAMapLocation.getAccuracy() + "m");
                    showToast("位置精度较低，建议等待GPS信号更好时再添加");
                }
                
                // 直接使用高德定位的坐标，无需转换
                Intent intent = new Intent(getActivity(), AddFootprintActivity.class);
                intent.putExtra("latitude", lastAMapLocation.getLatitude());
                intent.putExtra("longitude", lastAMapLocation.getLongitude());
                intent.putExtra("altitude", lastAMapLocation.getAltitude());
                intent.putExtra("accuracy", lastAMapLocation.getAccuracy());
                intent.putExtra("city",lastAMapLocation.getCity());
                intent.putExtra("address", lastAMapLocation.getAddress());
                startActivity(intent);
                
                Log.d(TAG, "准备添加足迹 - 纬度: " + lastAMapLocation.getLatitude() + ", 经度: " + lastAMapLocation.getLongitude());
            } else {
                handleNoLocationAvailable();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "位置权限错误: " + e.getMessage());
            showToast("位置权限被拒绝，请检查应用权限设置");
        } catch (Exception e) {
            Log.e(TAG, "添加足迹时出错: " + e.getMessage(), e);
            showToast("添加足迹时出错: " + e.getMessage());
        }
    }
    

    
    /**
     * 处理无位置可用的情况
     */
    private void handleNoLocationAvailable() {
        if (!isLocationUpdating.get()) {
            showToast("正在获取位置信息，请稍候...");
            startLocationUpdates();
            
            // 延迟重试
            mainHandler.postDelayed(() -> {
                if (lastAMapLocation != null) {
                    addCurrentLocationFootprint();
                } else {
                    showToast("无法获取位置信息，请检查定位设置");
                }
            }, 5000);
        } else {
            showToast("无法获取位置信息，请检查定位设置");
        }
    }
    
    /**
     * 获取足迹动态列表（优化版本）
     * 改进了加载逻辑、错误处理和状态管理
     */
    private void loadFootprintMessages() {
        if (isLoadingMessages.get()) {
            Log.d(TAG, "正在加载地图mark数据，跳过重复请求");
            return;
        }
        
        if (!isMapInitialized.get()) {
            Log.w(TAG, "地图未初始化，延迟加载地图mark数据");
            mainHandler.postDelayed(this::loadFootprintMessages, 1000);
            return;
        }
        
        if (getContext() == null) {
            Log.w(TAG, "Context为空，无法加载地图mark数据");
            return;
        }
        
        isLoadingMessages.set(true);
        Log.d(TAG, "开始获取地图mark数据");
        
        try {
            // 调用API获取地图页mark数据（使用getMsgListAll接口）
            apiService.getMsgListAll(
                1, // 页码
                50, // 每页大小
                    response -> {
                        try {
                            Log.d(TAG, "获取地图mark数据成功，记录数: " + response.getRecords().size());

                            // 在主线程更新UI
                            if (mainHandler != null) {
                                mainHandler.post(() -> {
                                    try {
                                        handleFootprintMessages(response.getRecords());
                                        Log.d(TAG, "地图mark数据显示完成");
                                    } catch (Exception e) {
                                        Log.e(TAG, "显示地图mark数据失败: " + e.getMessage(), e);
                                        showToast("显示地图mark数据失败");
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "处理足迹动态响应失败: " + e.getMessage(), e);
                            showToast("处理数据失败");
                        } finally {
                            isLoadingMessages.set(false);
                        }
                    },
                    errorMessage -> {
                        Log.e(TAG, "获取地图mark数据失败: " + errorMessage);

                        String displayMessage = errorMessage.contains("timeout") ? "网络连接超时，请重试" :
                                               errorMessage.contains("network") ? "网络连接失败，请检查网络设置" :
                                               "获取地图mark数据失败: " + errorMessage;
                        showToast(displayMessage);
                        isLoadingMessages.set(false);
                    }
            );
            
        } catch (Exception e) {
            Log.e(TAG, "启动足迹动态加载失败: " + e.getMessage(), e);
            showToast("启动数据加载失败");
            isLoadingMessages.set(false);
        }
    }
    
    /**
     * 处理足迹动态数据 - 优化版本
     * 在地图上显示足迹动态位置标记
     * @param messages 足迹动态列表
     */
    private void handleFootprintMessages(List<FootprintMessage> messages) {
        // 检查Fragment状态
        if (!isAdded() || getContext() == null || isFragmentDestroyed.get()) {
            Log.w(TAG, "Fragment未附加到Activity或已销毁，跳过处理足迹动态数据");
            return;
        }
        
        if (messages == null || messages.isEmpty()) {
            Log.d(TAG, "足迹动态列表为空");
            return;
        }
        
        Log.d(TAG, "开始处理足迹动态数据，共 " + messages.size() + " 条记录");
        
        // 批量处理标记
        processBatchMarkers(messages);
        
        Log.d(TAG, "足迹动态处理完成");
    }
    
    /**
     * 批量处理标记
     */
    private void processBatchMarkers(List<FootprintMessage> messages) {
        // 清除现有标记
        if (aMap != null) {
            aMap.clear();
        }
        markerCache.clear();
        
        List<MarkerOptions> markerOptionsList = new ArrayList<>();
        
        for (FootprintMessage message : messages) {
            if (message.getLat() != 0 && message.getLng() != 0) {
                LatLng position = new LatLng(message.getLat(), message.getLng());
                
                // 创建足迹动态标记，使用旗帜图标
                MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title(message.getCreateBy() + " - " + message.getTag())
                    .snippet(message.getTextContent())
                    .icon(BitmapDescriptorFactory.fromBitmap(ImageUtils.getBitmap(getContext(),R.drawable.ic_footprint_flag)));
                
                markerOptionsList.add(markerOptions);
            }
        }
        
        // 批量添加标记
        addMarkersToMap(markerOptionsList, messages);
    }
    
    /**
     * 批量添加标记到地图
     */
    private void addMarkersToMap(List<MarkerOptions> markerOptionsList, List<FootprintMessage> messages) {
        if (markerOptionsList.isEmpty() || aMap == null || messages == null) {
            return;
        }
        
        // 分批添加标记以避免UI阻塞
        final int BATCH_SIZE = 20;
        for (int i = 0; i < markerOptionsList.size(); i += BATCH_SIZE) {
            final int start = i;
            final int end = Math.min(i + BATCH_SIZE, markerOptionsList.size());
            
            mainHandler.post(() -> {
                if (!isFragmentDestroyed.get() && aMap != null) {
                    for (int j = start; j < end; j++) {
                        if (j < messages.size()) {
                            MarkerOptions options = markerOptionsList.get(j);
                            FootprintMessage message = messages.get(j);
                            
                            Marker marker = aMap.addMarker(options);
                            if (marker != null) {
                                // 直接将消息对象存储到标记中
                                marker.setObject(message);
                                
                                // 缓存标记对象用于快速访问
                                String key = options.getPosition().latitude + "," + options.getPosition().longitude;
                                markerCache.put(key, marker);
                                
                                Log.d(TAG, "添加足迹动态标记: " + options.getTitle());
                            }
                        }
                    }
                }
            });
        }
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "MapFragment onDestroy - 开始清理资源");
        
        // 首先设置销毁标志，防止其他操作继续执行
        isFragmentDestroyed.set(true);
        
        try {
            stopLocationUpdates();
            cleanupResources();
            
            Log.d(TAG, "MapFragment资源清理完成");
            
        } catch (Exception e) {
            Log.e(TAG, "销毁MapFragment时发生错误: " + e.getMessage(), e);
        } finally {
            super.onDestroy();
        }
    }
    
    /**
     * 清理所有资源
     */
    private void cleanupResources() {
        try {
            // 清理定位客户端
            if (locationClient != null) {
                locationClient.onDestroy();
                locationClient = null;
            }
            locationOption = null;
            lastAMapLocation = null;
            
            // 清理Handler
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
                mainHandler = null;
            }
            
            // 重置状态
            isLocationUpdating.set(false);
            isLoadingFootprints.set(false);
            isLoadingMessages.set(false);
            isMapInitialized.set(false);
            retryCount = 0;
            
            // 清理地图资源
            if (aMap != null) {
                aMap.clear();
                aMap = null;
            }
            if (mapView != null) {
                mapView.onDestroy();
                mapView = null;
            }
            currentLocationMarker = null;
            
            // 清理缓存
            if (markerCache != null) markerCache.clear();
            if (cachedMessages != null) cachedMessages.clear();
        } catch (Exception e) {
            Log.e(TAG, "清理资源时发生错误: " + e.getMessage(), e);
        }
    }

    
    /**
     * 显示足迹消息信息卡片
     * @param message 足迹消息对象
     */
    private void showFootprintMessageInfoCard(FootprintMessage message) {
        if (getContext() == null || !isAdded()) {
            return;
        }
        
        // 创建AlertDialog显示足迹消息信息
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        
        // 创建时间轴样式的自定义布局
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_footprint_message_info, null);
        
        // 获取布局中的控件
        ImageView avatarView = dialogView.findViewById(R.id.image_view_avatar); // 用户头像
        TextView creatorView = dialogView.findViewById(R.id.text_view_creator);
        TextView dateView = dialogView.findViewById(R.id.text_view_date);
        TextView tagView = dialogView.findViewById(R.id.text_view_tag);
        TextView contentView = dialogView.findViewById(R.id.text_view_content);
        TextView locationView = dialogView.findViewById(R.id.text_view_location);
        TextView coordinatesView = dialogView.findViewById(R.id.text_view_coordinates);
        
        // 获取图片相关控件
        FrameLayout frameLayoutImages = dialogView.findViewById(R.id.frame_layout_images);
        ImageView imageViewSingle = dialogView.findViewById(R.id.image_view_single);
        View gridImageLayout = dialogView.findViewById(R.id.grid_image_layout);
        TextView textViewImageCount = dialogView.findViewById(R.id.text_view_image_count);
        
        // 获取操作栏控件
        LinearLayout layoutLike = dialogView.findViewById(R.id.layout_like);
        LinearLayout layoutComment = dialogView.findViewById(R.id.layout_comment);
        ImageView ivLike = dialogView.findViewById(R.id.iv_like);
        TextView tvLikeCount = dialogView.findViewById(R.id.tv_like_count);
        TextView tvCommentCount = dialogView.findViewById(R.id.tv_comment_count);
        
        // 设置基本数据
        // 设置用户头像
        setUserAvatarInDialog(avatarView, message);
        
        // 设置用户昵称（createBy字段）
        creatorView.setText(message.getCreateBy() != null ? message.getCreateBy() : "匿名用户");
        dateView.setText(message.getCreateTime() != null ? message.getCreateTime() : "未知时间");
        tagView.setText(message.getTag() != null ? message.getTag() : "动态");
        contentView.setText(message.getTextContent() != null ? message.getTextContent() : "暂无内容");
        locationView.setText(message.getLocaltionTitle() != null ? message.getLocaltionTitle() : "暂无位置信息"); // 可以根据需要进行地理编码获取具体地址
        coordinatesView.setText(String.format("坐标: %.6f, %.6f", message.getLat(), message.getLng()));
        
        // 设置点赞和评论数据
        Log.d(TAG, "地图信息卡绑定数据 - 消息ID: " + message.getId() + ", 点赞状态: " + message.getHasLiked() + ", 点赞数量: " + message.getLikeCount() + ", 评论数量: " + message.getCommentCount());
        updateLikeStatus(ivLike, tvLikeCount, message.getHasLiked(), message.getLikeCount());
        tvCommentCount.setText(String.valueOf(message.getCommentCount()));
        
        // 设置点赞点击事件
        layoutLike.setOnClickListener(v -> {
            boolean newLikeStatus = !message.getHasLiked();
            int newLikeCount = message.getLikeCount() + (newLikeStatus ? 1 : -1);
            
            // 先更新UI，提供即时反馈
            updateLikeStatus(ivLike, tvLikeCount, newLikeStatus, newLikeCount);
            message.setHasLiked(newLikeStatus);
            message.setLikeCount(newLikeCount);
            
            // 调用API更新点赞状态
            apiService.toggleLike(message.getId(),
                new HutoolApiService.SuccessCallback<String>() {
                    @Override
                    public void onSuccess(String response) {
                        // API调用成功，显示提示信息
                        String toastMessage = newLikeStatus ? "已点赞" : "已取消点赞";
                        Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "点赞状态更新成功: " + response);
                    }
                },
                new HutoolApiService.ErrorCallback() {
                    @Override
                    public void onError(String error) {
                        // API调用失败，回滚UI状态
                        boolean originalStatus = !newLikeStatus;
                        int originalCount = message.getLikeCount() + (originalStatus ? 1 : -1);
                        updateLikeStatus(ivLike, tvLikeCount, originalStatus, originalCount);
                        message.setHasLiked(originalStatus);
                        message.setLikeCount(originalCount);
                        
                        Toast.makeText(requireContext(), "点赞操作失败，请重试", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "点赞状态更新失败: " + error);
                    }
                }
            );
        });
        
        // 设置评论点击事件
        layoutComment.setOnClickListener(v -> {
            // 跳转到评论详情页面
            CommentListActivity.start(requireContext(), message.getId(), 
                message.getTextContent() != null ? message.getTextContent() : "足迹详情");
        });
        
        // 渲染图片内容
        renderImageContent(message, frameLayoutImages, imageViewSingle, gridImageLayout, textViewImageCount);
        
        // 创建并显示dialog
        AlertDialog dialog = builder.setView(dialogView)
               .setPositiveButton("确定", null)
               .create();
        
        // 显示dialog
        dialog.show();
        
        // 设置dialog窗口的圆角背景
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }
    }
    
    /**
     * 渲染图片内容
     * @param message 足迹消息
     * @param frameLayoutImages 图片容器
     * @param imageViewSingle 单张图片视图
     * @param gridImageLayout 网格图片布局
     * @param textViewImageCount 图片数量文本
     */
    private void renderImageContent(FootprintMessage message, FrameLayout frameLayoutImages,
                                    ImageView imageViewSingle, View gridImageLayout, TextView textViewImageCount) {
        
        // 首先隐藏所有图片相关控件
        frameLayoutImages.setVisibility(View.GONE);
        imageViewSingle.setVisibility(View.GONE);
        if (gridImageLayout != null) {
            gridImageLayout.setVisibility(View.GONE);
        }
        textViewImageCount.setVisibility(View.GONE);
        
        // 检查是否有图片文件
        if (message.getGuluFiles() != null && !message.getGuluFiles().isEmpty()) {
            // 筛选出图片文件
            java.util.List<GuluFile> imageFiles = new java.util.ArrayList<>();
            
            for (GuluFile file : message.getGuluFiles()) {
                if (isImageFile(file.getFileType())) {
                    imageFiles.add(file);
                }
            }
            
            android.util.Log.d("MapFragment", "找到图片文件数量: " + imageFiles.size());
            
            if (!imageFiles.isEmpty()) {
                frameLayoutImages.setVisibility(View.VISIBLE);
                
                if (imageFiles.size() == 1) {
                    // 显示单张图片
                    imageViewSingle.setVisibility(View.VISIBLE);
                    loadImageIntoView(imageViewSingle, imageFiles.get(0));
                    
                    // 添加点击事件查看大图
                    imageViewSingle.setOnClickListener(v -> {
                        openImagePreview(imageFiles, 0);
                    });
                    
                } else {
                    // 多张图片使用网格布局
                    if (gridImageLayout != null) {
                        gridImageLayout.setVisibility(View.VISIBLE);
                        setupGridImageLayout(gridImageLayout, imageFiles);
                    }
                    
                    // 显示图片数量
                    textViewImageCount.setVisibility(View.VISIBLE);
                    textViewImageCount.setText(String.format("共%d张", imageFiles.size()));
                }
            }
        }
    }
    
    /**
     * 设置网格图片布局
     */
    private void setupGridImageLayout(View gridImageLayout, java.util.List<GuluFile> imageFiles) {
        ImageView singleImage = gridImageLayout.findViewById(R.id.single_image);
        LinearLayout twoImagesLayout = gridImageLayout.findViewById(R.id.two_images_layout);
        LinearLayout threeImagesLayout = gridImageLayout.findViewById(R.id.three_images_layout);
        androidx.recyclerview.widget.RecyclerView gridRecyclerView = gridImageLayout.findViewById(R.id.grid_recycler_view);
        
        // 隐藏所有布局
        if (singleImage != null) singleImage.setVisibility(View.GONE);
        if (twoImagesLayout != null) twoImagesLayout.setVisibility(View.GONE);
        if (threeImagesLayout != null) threeImagesLayout.setVisibility(View.GONE);
        if (gridRecyclerView != null) gridRecyclerView.setVisibility(View.GONE);
        
        int imageCount = imageFiles.size();
        
        if (imageCount == 2 && twoImagesLayout != null) {
            twoImagesLayout.setVisibility(View.VISIBLE);
            ImageView image1 = twoImagesLayout.findViewById(R.id.image_1_of_2);
            ImageView image2 = twoImagesLayout.findViewById(R.id.image_2_of_2);
            
            if (image1 != null && image2 != null) {
                loadImageIntoView(image1, imageFiles.get(0));
                loadImageIntoView(image2, imageFiles.get(1));
                image1.setOnClickListener(v -> openImagePreview(imageFiles, 0));
                image2.setOnClickListener(v -> openImagePreview(imageFiles, 1));
            }
        } else if (imageCount == 3 && threeImagesLayout != null) {
            threeImagesLayout.setVisibility(View.VISIBLE);
            ImageView image1 = threeImagesLayout.findViewById(R.id.image_1_of_3);
            ImageView image2 = threeImagesLayout.findViewById(R.id.image_2_of_3);
            ImageView image3 = threeImagesLayout.findViewById(R.id.image_3_of_3);
            
            if (image1 != null && image2 != null && image3 != null) {
                loadImageIntoView(image1, imageFiles.get(0));
                loadImageIntoView(image2, imageFiles.get(1));
                loadImageIntoView(image3, imageFiles.get(2));
                image1.setOnClickListener(v -> openImagePreview(imageFiles, 0));
                image2.setOnClickListener(v -> openImagePreview(imageFiles, 1));
                image3.setOnClickListener(v -> openImagePreview(imageFiles, 2));
            }
        } else if (imageCount >= 4 && gridRecyclerView != null) {
            gridRecyclerView.setVisibility(View.VISIBLE);
            androidx.recyclerview.widget.GridLayoutManager gridLayoutManager = 
                new androidx.recyclerview.widget.GridLayoutManager(getContext(), 3);
            gridRecyclerView.setLayoutManager(gridLayoutManager);
            
            GridImageAdapter adapter = new GridImageAdapter(getContext(), imageFiles);
            adapter.setOnImageClickListener((position, files) -> openImagePreview(files, position));
            gridRecyclerView.setAdapter(adapter);
        }
    }
    
    /**
     * 加载图片到ImageView
     */
    private void loadImageIntoView(ImageView imageView, GuluFile imageFile) {
        String imageUrl = getFullImageUrl(imageFile.getFilePath());
        
        com.bumptech.glide.Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_placeholder_image)
            .error(R.drawable.ic_error_image)
            .centerCrop()
            .into(imageView);
    }
    
    /**
     * 获取完整的图片URL
     */
    private String getFullImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "";
        }
        
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }
        
        String imageBaseUrl = ApiConfig.getImageBaseUrl();
        if (!imagePath.startsWith("/")) {
            imagePath = "/" + imagePath;
        }
        return imageBaseUrl + imagePath;
    }
    
    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String fileType) {
        if (fileType == null) return false;
        String type = fileType.toLowerCase();
        return type.equals("jpg") || type.equals("jpeg") || type.equals("png") || 
               type.equals("gif") || type.equals("bmp") || type.equals("webp") || 
               type.equals("image/jpeg");
    }
    
    /**
     * 打开图片预览
     */
    private void openImagePreview(java.util.List<GuluFile> imageFiles, int currentIndex) {
        if (getContext() == null || imageFiles == null || imageFiles.isEmpty()) {
            return;
        }
        
        try {
            Intent intent = new Intent(getContext(), ImagePreviewActivity.class);
            
            java.util.ArrayList<String> imageUrls = new java.util.ArrayList<>();
            for (GuluFile file : imageFiles) {
                imageUrls.add(getFullImageUrl(file.getFilePath()));
            }
            
            intent.putStringArrayListExtra("image_urls", imageUrls);
            intent.putExtra("current_index", currentIndex);
            startActivity(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "打开图片预览失败", e);
            showToast("无法预览图片");
        }
    }
    
    /**
     * 显示评论输入对话框
     * @param message 足迹动态消息
     */
    private void showCommentDialog(FootprintMessage message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("发表评论");
        
        // 创建输入框
        EditText editText = new EditText(requireContext());
        editText.setHint("请输入评论内容...");
        editText.setMaxLines(5);
        editText.setVerticalScrollBarEnabled(true);
        
        // 设置输入框的布局参数
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 20);
        editText.setLayoutParams(params);
        
        builder.setView(editText);
        
        builder.setPositiveButton("发表", (dialog, which) -> {
            String content = editText.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "评论内容不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 调用评论接口
            addComment(message.getId(), content);
        });
        
        builder.setNegativeButton("取消", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // 自动弹出键盘
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    /**
     * 添加评论
     * @param msgId 消息ID
     * @param content 评论内容
     */
    private void addComment(Integer msgId, String content) {
        apiService.addComment(msgId, content,
            new HutoolApiService.SuccessCallback<String>() {
                @Override
                public void onSuccess(String response) {
                    // 评论成功
                    Toast.makeText(requireContext(), "评论发表成功", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "评论发表成功: " + response);
                    
                    // 这里可以刷新评论列表或更新UI
                    // TODO: 刷新当前页面的评论数据
                }
            },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String error) {
                    // 评论失败
                    Toast.makeText(requireContext(), "评论发表失败，请重试", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "评论发表失败: " + error);
                }
            }
        );
    }
    
    /**
     * 在对话框中设置用户头像
     * @param avatarView 头像ImageView控件
     * @param message 足迹消息对象
     */
    private void setUserAvatarInDialog(ImageView avatarView, FootprintMessage message) {
        if (avatarView != null) {
            String userAvatar = message.getUserAvatar();
            if (userAvatar != null && !userAvatar.isEmpty()) {
                // 构建完整的头像URL
                String avatarUrl = getFullImageUrl(userAvatar);
                
                // 使用Glide加载头像，设置圆形裁剪
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_default_avatar) // 加载中显示的默认头像
                    .error(R.drawable.ic_default_avatar) // 加载失败显示的默认头像
                    .circleCrop() // 圆形裁剪
                    .into(avatarView);
            } else {
                // 没有头像时显示默认头像
                avatarView.setImageResource(R.drawable.ic_default_avatar);
            }
        }
    }
}