package com.damors.zuji;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class LocationPickerActivity extends AppCompatActivity {
    private MapView mapView;
    private GeoPoint selectedPosition;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);
        
        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        
        // 设置初始位置为北京
        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(39.9042, 116.4074);
        mapController.setCenter(startPoint);
        
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
}