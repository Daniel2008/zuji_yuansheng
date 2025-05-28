package com.damors.zuji;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import android.util.Log;
import org.osmdroid.config.Configuration;

public class LocationPickerActivity extends AppCompatActivity {
    private static final String TAG = "LocationPickerActivity";
    private MapView mapView;
    private GeoPoint selectedPosition;
    
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
        
        // 确保OSMDroid配置已初始化
        Log.d(TAG, "OSMDroid用户代理: " + Configuration.getInstance().getUserAgentValue());
        Log.d(TAG, "OSMDroid缓存目录: " + Configuration.getInstance().getOsmdroidBasePath());
        
        // 使用高德地图瓦片源，与主地图保持一致
        try {
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
             mapView.setBuiltInZoomControls(true);
             
             Log.d(TAG, "地图瓦片源设置完成: " + tileSource.name());
             Log.d(TAG, "地图缩放级别范围: " + tileSource.getMinimumZoomLevel() + "-" + tileSource.getMaximumZoomLevel());
            
        } catch (Exception e) {
             Log.e(TAG, "地图瓦片源设置失败，使用默认源: " + e.getMessage());
             // 如果高德地图源失败，回退到默认源
             try {
                  mapView.setTileSource(TileSourceFactory.MAPNIK);
                  mapView.setMultiTouchControls(true);
                  mapView.setBuiltInZoomControls(true);
                  Log.d(TAG, "已切换到默认地图源: MAPNIK");
             } catch (Exception fallbackException) {
                 Log.e(TAG, "默认地图源也设置失败: " + fallbackException.getMessage());
                 Toast.makeText(this, "地图加载失败，请检查网络连接", Toast.LENGTH_LONG).show();
             }
         }
        
        // 设置初始位置为北京
        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(39.9042, 116.4074);
        mapController.setCenter(startPoint);
        
        Log.d(TAG, "地图初始位置设置完成: " + startPoint.getLatitude() + ", " + startPoint.getLongitude());
        Log.d(TAG, "地图初始缩放级别: 15");
        
        mapView.getOverlays().add(new Marker(mapView));
        
        // 添加地图点击事件处理
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                selectedPosition = p;
                mapView.getOverlays().clear();
                Marker marker = new Marker(mapView);
                marker.setPosition(p);
                mapView.getOverlays().add(marker);
                mapView.invalidate();
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        mapView.getOverlays().add(0, mapEventsOverlay);
        
        Button btnConfirm = findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            if (selectedPosition != null) {
                Intent result = new Intent();
                result.putExtra("latitude", selectedPosition.getLatitude());
                result.putExtra("longitude", selectedPosition.getLongitude());
                result.putExtra("location", "自定义位置");
                setResult(RESULT_OK, result);
                finish();
            } else {
                Toast.makeText(this, "请选择位置", Toast.LENGTH_SHORT).show();
            }
        });
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