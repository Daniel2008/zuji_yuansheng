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
import android.util.Log;

public class LocationPickerActivity extends AppCompatActivity {
    private static final String TAG = "LocationPickerActivity";
    private MapView mapView;
    private AMap aMap;
    private LatLng selectedPosition;
    private LinearLayout llCoordinateInfo;
    private TextView tvCoordinateInfo;
    private Marker selectedMarker;
    
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
                
                // 清除之前的标记
                if (selectedMarker != null) {
                    selectedMarker.remove();
                }
                
                // 添加新标记
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title("选中位置")
                        .snippet("纬度: " + String.format("%.6f", latLng.latitude) + 
                                "\n经度: " + String.format("%.6f", latLng.longitude));
                
                selectedMarker = aMap.addMarker(markerOptions);
                
                // 更新经纬度显示
                updateCoordinateDisplay(latLng);
                
                Log.d(TAG, "选中位置: " + latLng.latitude + ", " + latLng.longitude);
            }
        });
        
        Button btnConfirm = findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            if (selectedPosition != null) {
                Intent result = new Intent();
                result.putExtra("latitude", selectedPosition.latitude);
                result.putExtra("longitude", selectedPosition.longitude);
                result.putExtra("location", "自定义位置");
                
                Log.d(TAG, "确认选择位置: 纬度=" + selectedPosition.latitude + 
                          ", 经度=" + selectedPosition.longitude);
                
                setResult(RESULT_OK, result);
                finish();
            } else {
                Toast.makeText(this, "请先在地图上点击选择位置", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "用户未选择位置就尝试确认");
            }
        });
        
        // 初始化时显示提示信息
        if (tvCoordinateInfo != null) {
            tvCoordinateInfo.setText("点击地图上的任意位置来选择坐标");
        }
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
    }
    
    /**
     * 更新经纬度显示
     * @param position 选中的地理位置
     */
    private void updateCoordinateDisplay(LatLng position) {
        if (position != null && tvCoordinateInfo != null && llCoordinateInfo != null) {
            // 格式化经纬度显示，保留6位小数
            String latitudeStr = String.format("%.6f", position.latitude);
            String longitudeStr = String.format("%.6f", position.longitude);
            
            // 设置显示文本
            String coordinateText = "纬度: " + latitudeStr + "\n经度: " + longitudeStr;
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
}