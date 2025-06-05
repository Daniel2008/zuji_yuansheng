package com.example.footprint;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.damors.zuji.R;
import com.damors.zuji.utils.AMapHelper;

public class AMapLocationPickerActivity extends AppCompatActivity implements AMapLocationListener {
    private static final String TAG = "AMapLocationPicker";
    
    private MapView mapView;
    private AMap aMap;
    private Button confirmButton;
    private LinearLayout coordinateLayout;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    
    private LatLng selectedLocation;
    private Marker selectedMarker;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amap_location_picker);
        
        // 初始化高德地图SDK
        AMapHelper.initialize(this);
        
        initViews(savedInstanceState);
        initMap();
        initLocation();
        setupClickListeners();
    }
    
    private void initViews(Bundle savedInstanceState) {
        mapView = findViewById(R.id.amap_view);
        confirmButton = findViewById(R.id.btn_confirm_location);
        coordinateLayout = findViewById(R.id.ll_coordinate_info);
        latitudeTextView = findViewById(R.id.tv_latitude);
        longitudeTextView = findViewById(R.id.tv_longitude);
        
        // 创建地图
        mapView.onCreate(savedInstanceState);
        
        // 初始化坐标显示
        latitudeTextView.setText("纬度: 点击地图选择位置");
        longitudeTextView.setText("经度: 点击地图选择位置");
        coordinateLayout.setVisibility(View.GONE);
    }
    
    private void initMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        
        // 设置地图类型为标准地图
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
        
        // 设置缩放控件
        aMap.getUiSettings().setZoomControlsEnabled(true);
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setScaleControlsEnabled(true);
        
        // 设置地图点击监听
        aMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                selectLocation(latLng);
            }
        });
        
        // 设置默认位置（北京）
        LatLng defaultLocation = new LatLng(39.906901, 116.397972);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
    }
    
    private void initLocation() {
        try {
            locationClient = new AMapLocationClient(getApplicationContext());
            locationOption = new AMapLocationClientOption();
            
            // 设置定位模式为高精度模式
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            // 设置定位间隔
            locationOption.setInterval(2000);
            // 设置是否返回地址信息
            locationOption.setNeedAddress(true);
            // 设置是否允许模拟位置
            locationOption.setMockEnable(false);
            
            locationClient.setLocationOption(locationOption);
            locationClient.setLocationListener(this);
            
        } catch (Exception e) {
            Log.e(TAG, "初始化定位失败: " + e.getMessage());
            Toast.makeText(this, "定位初始化失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupClickListeners() {
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedLocation != null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("latitude", selectedLocation.latitude);
                    resultIntent.putExtra("longitude", selectedLocation.longitude);
                    
                    Log.d(TAG, "返回选择的位置: " + selectedLocation.latitude + ", " + selectedLocation.longitude);
                    
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(AMapLocationPickerActivity.this, "请先选择一个位置", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // 添加获取当前位置按钮（可选）
        findViewById(R.id.btn_current_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
            }
        });
    }
    
    private void selectLocation(LatLng latLng) {
        selectedLocation = latLng;
        
        // 清除之前的标记
        if (selectedMarker != null) {
            selectedMarker.remove();
        }
        
        // 添加新标记
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title("选择的位置")
                .snippet("纬度: " + String.format("%.6f", latLng.latitude) + 
                         ", 经度: " + String.format("%.6f", latLng.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        
        selectedMarker = aMap.addMarker(markerOptions);
        selectedMarker.showInfoWindow();
        
        // 更新坐标显示
        updateCoordinateDisplay(latLng);
        
        // 移动相机到选择的位置
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        
        Log.d(TAG, "选择位置: " + latLng.latitude + ", " + latLng.longitude);
    }
    
    private void updateCoordinateDisplay(LatLng latLng) {
        if (latLng != null) {
            String latitudeStr = AMapHelper.formatCoordinate(latLng.latitude, true);
            String longitudeStr = AMapHelper.formatCoordinate(latLng.longitude, false);
            
            latitudeTextView.setText("纬度: " + latitudeStr);
            longitudeTextView.setText("经度: " + longitudeStr);
            coordinateLayout.setVisibility(View.VISIBLE);
        }
    }
    
    private void hideCoordinateDisplay() {
        coordinateLayout.setVisibility(View.GONE);
    }
    
    private void getCurrentLocation() {
        if (locationClient != null) {
            locationClient.startLocation();
            Toast.makeText(this, "正在获取当前位置...", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                // 定位成功
                double latitude = aMapLocation.getLatitude();
                double longitude = aMapLocation.getLongitude();
                
                LatLng currentLocation = new LatLng(latitude, longitude);
                selectLocation(currentLocation);
                
                String address = aMapLocation.getAddress();
                if (address != null && !address.isEmpty()) {
                    Toast.makeText(this, "当前位置: " + address, Toast.LENGTH_LONG).show();
                }
                
                Log.d(TAG, "定位成功: " + latitude + ", " + longitude + ", 地址: " + address);
                
            } else {
                // 定位失败
                String errorText = "定位失败: " + aMapLocation.getErrorCode() + ", " + aMapLocation.getErrorInfo();
                Log.e(TAG, errorText);
                Toast.makeText(this, "定位失败，请检查网络和定位权限", Toast.LENGTH_SHORT).show();
            }
        }
        
        // 停止定位
        if (locationClient != null) {
            locationClient.stopLocation();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
        if (locationClient != null) {
            locationClient.onDestroy();
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}