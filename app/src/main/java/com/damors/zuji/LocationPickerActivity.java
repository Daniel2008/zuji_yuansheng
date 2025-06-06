package com.damors.zuji;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.core.LatLonPoint;
import android.util.Log;

public class LocationPickerActivity extends AppCompatActivity implements AMapLocationListener, GeocodeSearch.OnGeocodeSearchListener {
    private static final String TAG = "LocationPickerActivity";
    private MapView mapView;
    private AMap aMap;
    private LatLng selectedPosition;
    private LinearLayout llCoordinateInfo;
    private TextView tvCoordinateInfo;
    private Marker selectedMarker;
    
    // 高德定位相关
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    private AMapLocation currentLocation;
    private Marker currentLocationMarker;
    private String selectedAddress = ""; // 选中位置的地址信息
    private GeocodeSearch geocodeSearch;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_location_picker);
        
        // 检查网络连接
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "网络连接不可用，地图可能无法正常加载", Toast.LENGTH_LONG).show();
            Log.w(TAG, "网络连接不可用");
        }
        
        mapView = findViewById(R.id.map);
        llCoordinateInfo = findViewById(R.id.ll_coordinate_info);
        tvCoordinateInfo = findViewById(R.id.tv_coordinate_info);
        
        // 初始化高德地图
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();
        
        // 初始化高德定位
        initializeLocation();
        
        // 设置地图类型
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
        
        // 设置地图UI控件
        aMap.getUiSettings().setZoomControlsEnabled(true);
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setScaleControlsEnabled(true);
        
        // 设置初始位置为北京
        LatLng startPoint = new LatLng(39.9042, 116.4074);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 15));
        
        Log.d(TAG, "高德地图初始位置设置完成: " + startPoint.latitude + ", " + startPoint.longitude);
        Log.d(TAG, "高德地图初始缩放级别: 15");
        
        // 添加高德地图点击事件处理
        aMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                selectedPosition = latLng;
                selectedAddress = "获取地址中..."; // 临时显示
                
                // 清除之前的标记
                if (selectedMarker != null) {
                    selectedMarker.remove();
                }
                
                // 添加新标记
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title("选中位置")
                        .snippet("纬度: " + String.format("%.6f", latLng.latitude) + 
                                "\n经度: " + String.format("%.6f", latLng.longitude))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                
                selectedMarker = aMap.addMarker(markerOptions);
                
                // 先更新经纬度显示
                updateCoordinateDisplay(latLng, selectedAddress);
                
                // 执行逆地理编码获取真实地址
                performReverseGeocode(latLng.latitude, latLng.longitude);
                
                Log.d(TAG, "选中位置: " + latLng.latitude + ", " + latLng.longitude);
            }
        });
        
        Button btnConfirm = findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            if (selectedPosition != null) {
                Intent result = new Intent();
                result.putExtra("latitude", selectedPosition.latitude);
                result.putExtra("longitude", selectedPosition.longitude);
                result.putExtra("location", selectedAddress);
                result.putExtra("address", selectedAddress);
                
                Log.d(TAG, "确认选择位置: 纬度=" + selectedPosition.latitude + 
                          ", 经度=" + selectedPosition.longitude + ", 地址=" + selectedAddress);
                
                setResult(RESULT_OK, result);
                finish();
            } else {
                Toast.makeText(this, "请先在地图上点击选择位置或获取当前位置", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "用户未选择位置就尝试确认");
            }
        });
        
        // 初始化时显示提示信息
        if (tvCoordinateInfo != null) {
            tvCoordinateInfo.setText("点击地图选择位置，或等待自动获取当前位置");
        }
        
        // 自动获取当前位置
        getCurrentLocation();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
        // 清理定位资源
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
            locationClient = null;
            Log.d(TAG, "高德定位资源清理完成");
        }
        
        if (geocodeSearch != null) {
            geocodeSearch.setOnGeocodeSearchListener(null);
            geocodeSearch = null;
            Log.d(TAG, "逆地理编码资源清理完成");
        }
        Log.d(TAG, "LocationPickerActivity资源清理完成");
    }
    
    /**
     * 更新经纬度和地址显示
     * @param position 选中的地理位置
     * @param address 地址信息
     */
    private void updateCoordinateDisplay(LatLng position, String address) {
        if (position != null && tvCoordinateInfo != null && llCoordinateInfo != null) {
            // 格式化经纬度显示，保留6位小数
            String latitudeStr = String.format("%.6f", position.latitude);
            String longitudeStr = String.format("%.6f", position.longitude);
            
            // 设置显示文本，包含地址信息
            String coordinateText = "纬度: " + latitudeStr + "\n经度: " + longitudeStr;
            if (address != null && !address.isEmpty()) {
                coordinateText += "\n地址: " + address;
            }
            tvCoordinateInfo.setText(coordinateText);
            
            // 显示坐标信息区域
            llCoordinateInfo.setVisibility(View.VISIBLE);
            
            Log.d(TAG, "更新坐标显示: " + coordinateText.replace("\n", ", "));
        }
    }
    
    /**
     * 隐藏经纬度显示
     */
    private void hideCoordinateDisplay() {
        if (llCoordinateInfo != null) {
            llCoordinateInfo.setVisibility(View.GONE);
            Log.d(TAG, "隐藏坐标显示");
        }
    }
    
    /**
     * 检查网络连接是否可用
     * @return true如果网络可用，false否则
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "检查网络状态失败: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 初始化高德定位
     */
    private void initializeLocation() {
        try {
            // 初始化定位客户端
            locationClient = new AMapLocationClient(this);
            
            // 配置定位参数
            locationOption = new AMapLocationClientOption();
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            locationOption.setOnceLocation(true); // 单次定位
            locationOption.setOnceLocationLatest(true); // 获取最近3s内精度最高的一次定位结果
            locationOption.setNeedAddress(true); // 需要地址信息
            locationOption.setMockEnable(false); // 禁止模拟定位
            locationOption.setLocationCacheEnable(false); // 禁用缓存
            
            // 设置定位参数
            locationClient.setLocationOption(locationOption);
            
            // 设置定位监听
            locationClient.setLocationListener(new AMapLocationListener() {
                @Override
                public void onLocationChanged(AMapLocation aMapLocation) {
                    if (aMapLocation != null) {
                        if (aMapLocation.getErrorCode() == 0) {
                            // 定位成功
                            currentLocation = aMapLocation;
                            onLocationSuccess(aMapLocation);
                            Log.d(TAG, "高德定位成功: " + aMapLocation.getLatitude() + ", " + aMapLocation.getLongitude());
                        } else {
                            // 定位失败
                            Log.e(TAG, "高德定位失败: " + aMapLocation.getErrorCode() + ", " + aMapLocation.getErrorInfo());
                            Toast.makeText(LocationPickerActivity.this, "定位失败: " + aMapLocation.getErrorInfo(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            
            Log.d(TAG, "高德定位初始化完成");
            
            // 初始化逆地理编码
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
            Log.d(TAG, "逆地理编码初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "初始化高德定位失败: " + e.getMessage(), e);
            Toast.makeText(this, "定位功能初始化失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 获取当前位置
     */
    private void getCurrentLocation() {
        if (locationClient != null) {
            Toast.makeText(this, "正在获取当前位置...", Toast.LENGTH_SHORT).show();
            locationClient.startLocation();
            Log.d(TAG, "开始获取当前位置");
        } else {
            Toast.makeText(this, "定位功能未初始化", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "定位客户端未初始化");
        }
    }
    
    /**
     * 定位成功回调
     * @param aMapLocation 定位结果
     */
    private void onLocationSuccess(AMapLocation aMapLocation) {
        try {
            LatLng currentLatLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
            String address = aMapLocation.getAddress();
            if (address == null || address.isEmpty()) {
                address = "当前位置";
            }
            
            // 设置为选中位置
            selectedPosition = currentLatLng;
            selectedAddress = address;
            
            // 清除之前的标记
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
            }
            
            // 添加当前位置标记
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(currentLatLng)
                    .title("当前位置")
                    .snippet("纬度: " + String.format("%.6f", currentLatLng.latitude) + 
                            "\n经度: " + String.format("%.6f", currentLatLng.longitude) +
                            "\n地址: " + address)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            
            currentLocationMarker = aMap.addMarker(markerOptions);
            selectedMarker = currentLocationMarker;
            
            // 移动地图到当前位置
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
            
            // 更新坐标和地址显示
            updateCoordinateDisplay(currentLatLng, address);
            
            Toast.makeText(this, "当前位置获取成功", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "当前位置设置完成: " + currentLatLng.latitude + ", " + currentLatLng.longitude + ", 地址: " + address);
            
        } catch (Exception e) {
            Log.e(TAG, "处理定位结果失败: " + e.getMessage(), e);
            Toast.makeText(this, "处理定位结果失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 执行逆地理编码
     * @param lat 纬度
     * @param lng 经度
     */
    private void performReverseGeocode(double lat, double lng) {
        try {
            if (geocodeSearch == null) {
                Log.w(TAG, "GeocodeSearch未初始化，无法执行逆地理编码");
                selectedAddress = String.format("位置 (%.6f, %.6f)", lat, lng);
                updateCoordinateDisplay(selectedPosition, selectedAddress);
                return;
            }
            
            // 创建逆地理编码查询条件
            LatLonPoint latLonPoint = new LatLonPoint(lat, lng);
            // 查询范围500米，使用高德坐标系
            RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 500f, GeocodeSearch.AMAP);
            
            // 异步查询
            geocodeSearch.getFromLocationAsyn(query);
            Log.d(TAG, "开始执行逆地理编码: " + lat + ", " + lng);
            
        } catch (Exception e) {
            Log.e(TAG, "逆地理编码执行失败: " + e.getMessage());
            // 失败时显示经纬度
            selectedAddress = String.format("位置 (%.6f, %.6f)", lat, lng);
            updateCoordinateDisplay(selectedPosition, selectedAddress);
        }
    }
    
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
        if (rCode == 1000) { // 查询成功
            if (regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null) {
                RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
                String formatAddress = regeocodeAddress.getFormatAddress();
                
                if (formatAddress != null && !formatAddress.isEmpty()) {
                    selectedAddress = formatAddress;
                    Log.d(TAG, "获取到地址描述: " + formatAddress);
                } else {
                    // 如果格式化地址为空，尝试组合地址信息
                    StringBuilder addressBuilder = new StringBuilder();
                    if (regeocodeAddress.getProvince() != null) {
                        addressBuilder.append(regeocodeAddress.getProvince());
                    }
                    if (regeocodeAddress.getCity() != null) {
                        addressBuilder.append(regeocodeAddress.getCity());
                    }
                    if (regeocodeAddress.getDistrict() != null) {
                        addressBuilder.append(regeocodeAddress.getDistrict());
                    }
                    if (regeocodeAddress.getTownship() != null) {
                        addressBuilder.append(regeocodeAddress.getTownship());
                    }
                    
                    selectedAddress = addressBuilder.length() > 0 ? 
                        addressBuilder.toString() : 
                        String.format("位置 (%.6f, %.6f)", selectedPosition.latitude, selectedPosition.longitude);
                    
                    Log.d(TAG, "组合地址描述: " + selectedAddress);
                }
            } else {
                selectedAddress = String.format("位置 (%.6f, %.6f)", selectedPosition.latitude, selectedPosition.longitude);
                Log.w(TAG, "逆地理编码结果为空");
            }
        } else {
            selectedAddress = String.format("位置 (%.6f, %.6f)", selectedPosition.latitude, selectedPosition.longitude);
            Log.w(TAG, "逆地理编码失败，错误码: " + rCode);
        }
        
        // 更新UI显示
        if (selectedPosition != null) {
            updateCoordinateDisplay(selectedPosition, selectedAddress);
        }
    }
    
    @Override
     public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {
         // 地理编码回调，这里不需要处理
     }
     
     @Override
     public void onLocationChanged(AMapLocation aMapLocation) {
         // AMapLocationListener接口实现
         // 这里的逻辑已经在initializeLocation方法中的匿名监听器中处理了
         // 保留此方法以满足接口要求
     }
 }