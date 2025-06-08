package com.damors.zuji;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.WindowManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.core.LatLonPoint;


import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.viewmodel.FootprintViewModel;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.PublishTrandsInfoPO;

import com.damors.zuji.network.HutoolApiService;

import com.damors.zuji.adapter.ImageGridAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AddFootprintActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener {

    private static final int REQUEST_PICK_IMAGES = 1;
    private static final int REQUEST_SELECT_LOCATION = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1003;

    private EditText etContent;
    private GeocodeSearch geocodeSearch;
    private RecyclerView rvImages;
    private TextView tvLocation;
    private Button btnPublish;
    private RadioGroup rgMsgType;
    private RadioButton rbPublic;
    private RadioButton rbPrivate;

    private List<Uri> selectedImages = new ArrayList<>();
    private ImageGridAdapter imageAdapter;
    private String selectedLocation;
    private double latitude;
    private double longitude;
    private int msgType = 1; // 默认为公开类型
    
    // 网络服务实例
    private HutoolApiService apiService;
    
    // 位置管理器
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_add_footprint);

        // 初始化网络服务
        apiService = HutoolApiService.getInstance(this);
        
        // 初始化位置管理器
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        // 获取从Intent传递的位置信息
        getLocationFromIntent();
        
        // 如果没有从Intent获取到位置信息，尝试获取当前位置
        if (latitude == 0.0 && longitude == 0.0) {
            requestCurrentLocation();
        }
        
        initViews();
        setupAdapters();
        setupClickListeners();
    }
    
    /**
     * 从Intent中获取位置信息
     * 主要用于从地图页面传递过来的经纬度信息
     */
    private void getLocationFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            // 获取经纬度信息
            latitude = intent.getDoubleExtra("latitude", 0.0);
            longitude = intent.getDoubleExtra("longitude", 0.0);
            
            // 获取其他位置相关信息
            double altitude = intent.getDoubleExtra("altitude", 0.0);
            float accuracy = intent.getFloatExtra("accuracy", 0.0f);

            // 获取地址信息
            String address = intent.getStringExtra("address");

            // 如果有有效的经纬度信息，设置默认位置文本
            if (latitude != 0.0 && longitude != 0.0) {
                selectedLocation =address;
                Log.d("AddFootprintActivity", "从Intent获取位置信息: " + selectedLocation + 
                      ", 海拔: " + altitude + "m, 精度: " + accuracy + "m");
            } else {
                Log.d("AddFootprintActivity", "Intent中没有有效的位置信息");
            }
        }
     }
     
     /**
      * 请求获取当前位置
      */
     private void requestCurrentLocation() {
         // 检查位置权限
         if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                 != PackageManager.PERMISSION_GRANTED) {
             Log.d("AddFootprintActivity", "没有位置权限，请求权限");
             ActivityCompat.requestPermissions(this,
                     new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                     LOCATION_PERMISSION_REQUEST_CODE);
             return;
         }
         
         try {
             Location location = getBestLastKnownLocation();
             if (location != null) {
                 // 转换为火星坐标系（如果需要）
                 latitude = location.getLatitude();
                 longitude = location.getLongitude();
                 
                 // 临时显示经纬度
                 selectedLocation = String.format("正在获取位置信息... (%.6f, %.6f)", latitude, longitude);
                 updateLocationUI();
                 
                 // 使用高德地图SDK进行逆地理编码
                 performReverseGeocode(latitude, longitude);
                 
                 Log.d("AddFootprintActivity", "获取到当前位置坐标: " + latitude + ", " + longitude);
             } else {
                 Log.w("AddFootprintActivity", "无法获取当前位置");
             }
         } catch (SecurityException e) {
             Log.e("AddFootprintActivity", "获取位置时权限错误: " + e.getMessage());
         }
     }
     
     /**
      * 获取最佳的已知位置
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
             if (timeDiff > -60000 || gpsLocation.getAccuracy() < networkLocation.getAccuracy()) {
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
     
     @Override
     public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults);
         if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
             if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 Log.d("AddFootprintActivity", "位置权限已授予，重新尝试获取位置");
                 requestCurrentLocation();
             } else {
                 Log.w("AddFootprintActivity", "位置权限被拒绝");
                 Toast.makeText(this, "位置权限被拒绝，无法自动获取位置信息", Toast.LENGTH_SHORT).show();
             }
         }
     }
 
     private void initViews() {
        etContent = findViewById(R.id.et_content);
        rvImages = findViewById(R.id.rv_images);
        tvLocation = findViewById(R.id.tv_location);
        btnPublish = findViewById(R.id.btn_publish);
        rgMsgType = findViewById(R.id.rg_msg_type);
        rbPublic = findViewById(R.id.rb_public);
        rbPrivate = findViewById(R.id.rb_private);

        // 如果从Intent获取到了位置信息，显示在UI上
        if (selectedLocation != null && !selectedLocation.isEmpty()) {
            tvLocation.setText(selectedLocation);
            Log.d("AddFootprintActivity", "设置位置显示: " + selectedLocation);
        }

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
    }

    private void setupAdapters() {
        imageAdapter = new ImageGridAdapter(this, selectedImages, new ImageGridAdapter.OnImageActionListener() {
            @Override
            public void onAddImageClick() {
                pickImages();
            }

            @Override
            public void onImageRemove(int position) {
                selectedImages.remove(position);
                imageAdapter.notifyDataSetChanged();
            }
        });

        rvImages.setLayoutManager(new GridLayoutManager(this, 3));
        rvImages.setAdapter(imageAdapter);
    }

    private void setupClickListeners() {
        // 位置选择
        findViewById(R.id.layout_location).setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationPickerActivity.class);
            startActivityForResult(intent, REQUEST_SELECT_LOCATION);
        });

        // 消息类型选择
        rgMsgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_public) {
                msgType = 1; // 公开
            } else if (checkedId == R.id.rb_private) {
                msgType = 2; // 个人可见
            }
        });

        // 发布按钮
        btnPublish.setOnClickListener(v -> {
            String content = etContent.getText().toString();
            if (TextUtils.isEmpty(content) && selectedImages.isEmpty()) {
                Toast.makeText(this, "请填写内容或添加图片", Toast.LENGTH_SHORT).show();
                return;
            }

            // 发布足迹到服务器
            publishFootprint(content, selectedLocation);
        });
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_PICK_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGES && data != null) {
                if (data.getClipData() != null) {
                    // 多选图片
                    int count = Math.min(data.getClipData().getItemCount(), 9 - selectedImages.size());
                    for (int i = 0; i < count; i++) {
                        selectedImages.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else if (data.getData() != null && selectedImages.size() < 9) {
                    // 单选图片
                    selectedImages.add(data.getData());
                }
                imageAdapter.notifyDataSetChanged();
            } else if (requestCode == REQUEST_SELECT_LOCATION) {
                selectedLocation = data.getStringExtra("location");
                latitude = data.getDoubleExtra("latitude", 0);
                longitude = data.getDoubleExtra("longitude", 0);
                tvLocation.setText(selectedLocation);
            }
        }
    }

    /**
     * 发布足迹动态到服务器
     * @param content 足迹内容
     * @param location 位置信息
     */
    private void publishFootprint(String content, String location) {
        // 检查用户登录状态
        UserManager userManager = UserManager.getInstance();
        if (!userManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = userManager.getUserField("userId");
        if (userId == null) {
            Toast.makeText(this, "获取用户信息失败", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查位置信息
        if (latitude == 0.0 && longitude == 0.0) {
            Log.w("AddFootprintActivity", "警告: 没有有效的位置信息，经纬度为(0.0, 0.0)");
            // 可以选择是否允许发布没有位置信息的足迹
            // Toast.makeText(this, "请先获取位置信息", Toast.LENGTH_SHORT).show();
            // return;
        } else {
            Log.d("AddFootprintActivity", "位置信息有效: 纬度=" + latitude + ", 经度=" + longitude);
        }
        
        // 显示加载状态
        btnPublish.setEnabled(false);
        btnPublish.setText("发布中...");
        
        // 创建发布参数对象
        PublishTrandsInfoPO publishInfo = new PublishTrandsInfoPO();
        publishInfo.setUserId(userId); // 当前用户ID
        publishInfo.setContent(content); // 足迹内容
        publishInfo.setLocationInfo(location); // 位置信息
        publishInfo.setType(msgType+""); // 类型 (1: 公开, 2: 个人可见)
        publishInfo.setTag(""); // 标签，可以根据需要设置
        publishInfo.setLng(longitude); // 经度
        publishInfo.setLat(latitude); // 纬度
        publishInfo.setMsgType(msgType); // 消息类型 (1: 公开, 2: 个人可见)
        
        // 处理图片文件对象
        List<File> imageFiles = new ArrayList<>();
        for (Uri imageUri : selectedImages) {
            try {
                // 对于content://类型的URI，需要将图片复制到应用私有目录
                if ("content".equals(imageUri.getScheme())) {
                    // 创建临时文件
                    File tempFile = createTempImageFile(imageUri);
                    if (tempFile != null && tempFile.exists()) {
                        imageFiles.add(tempFile);
                    }
                } else if ("file".equals(imageUri.getScheme())) {
                    // 对于file://类型的URI，直接使用路径
                    String path = imageUri.getPath();
                    if (path != null) {
                        File imageFile = new File(path);
                        if (imageFile.exists()) {
                            imageFiles.add(imageFile);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("AddFootprint", "处理图片URI失败: " + imageUri, e);
            }
        }
        publishInfo.setImages(imageFiles);
        
        // 调用网络接口发布足迹
        apiService.publishFootprint(publishInfo,
            new HutoolApiService.SuccessCallback<String>() {
                @Override
                public void onSuccess(String response) {
                    // 发布成功
                    // 注释：已移除本地数据库保存功能
                    // saveToLocalDatabase(content, location);
                    
                    // 重置UI状态
                    runOnUiThread(() -> {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("发布");
                        Toast.makeText(AddFootprintActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
            },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    // 发布失败，重置UI状态
                    runOnUiThread(() -> {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("发布");
                        Toast.makeText(AddFootprintActivity.this, 
                            "发布失败: " + errorMessage, Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }
    
    /**
     * 更新位置显示UI
     */
    private void updateLocationUI() {
        if (tvLocation != null && selectedLocation != null) {
            tvLocation.setText(selectedLocation);
        }
    }
    
    /**
     * 执行逆地理编码
     * @param lat 纬度
     * @param lng 经度
     */
    private void performReverseGeocode(double lat, double lng) {
        try {
            // 初始化GeocodeSearch
            if (geocodeSearch == null) {
                geocodeSearch = new GeocodeSearch(this);
                geocodeSearch.setOnGeocodeSearchListener(this);
            }
            
            // 创建逆地理编码查询条件
            LatLonPoint latLonPoint = new LatLonPoint(lat, lng);
            // 查询范围500米，使用高德坐标系
            RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 500f, GeocodeSearch.AMAP);
            
            // 异步查询
            geocodeSearch.getFromLocationAsyn(query);
            
        } catch (Exception e) {
            Log.e("AddFootprintActivity", "逆地理编码初始化失败: " + e.getMessage());
            // 失败时显示经纬度
            selectedLocation = String.format("当前位置 (%.6f, %.6f)", lat, lng);
            updateLocationUI();
        }
    }
    
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
        if (rCode == 1000) { // 查询成功
            if (regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null) {
                RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
                String formatAddress = regeocodeAddress.getFormatAddress();
                
                if (formatAddress != null && !formatAddress.isEmpty()) {
                    selectedLocation = formatAddress;
                    Log.d("AddFootprintActivity", "获取到地址描述: " + formatAddress);
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
                    
                    selectedLocation = addressBuilder.length() > 0 ? 
                        addressBuilder.toString() : 
                        String.format("当前位置 (%.6f, %.6f)", latitude, longitude);
                    
                    Log.d("AddFootprintActivity", "组合地址描述: " + selectedLocation);
                }
            } else {
                selectedLocation = String.format("当前位置 (%.6f, %.6f)", latitude, longitude);
                Log.w("AddFootprintActivity", "逆地理编码结果为空");
            }
        } else {
            selectedLocation = String.format("当前位置 (%.6f, %.6f)", latitude, longitude);
            Log.w("AddFootprintActivity", "逆地理编码失败，错误码: " + rCode);
        }
        
        // 更新UI
        updateLocationUI();
    }
    
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {
        // 地理编码回调，这里不需要处理
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (geocodeSearch != null) {
            geocodeSearch.setOnGeocodeSearchListener(null);
        }
    }
    
    /**
     * 保存足迹到本地数据库
     * 注释：此功能已被移除，不再支持本地存储
     * @param content 足迹内容
     * @param location 位置信息
     */
    /*
    private void saveToLocalDatabase(String content, String location) {
        // 创建足迹对象
        FootprintEntity footprint = new FootprintEntity();
        footprint.setDescription(content);
        footprint.setLocationName(location);
        footprint.setLatitude(latitude);
        footprint.setLongitude(longitude);
        footprint.setTimestamp(System.currentTimeMillis());
        footprint.setImageUris(selectedImages.toString());
        
        // 保存到数据库
        FootprintViewModel viewModel = new ViewModelProvider(this).get(FootprintViewModel.class);
        viewModel.insert(footprint);
    }
    */
    
    /**
     * 从URI获取文件路径
     * @param uri 图片URI
     * @return 文件路径，如果获取失败返回null
     */
    private String getPathFromUri(Uri uri) {
        try {
            // 这里简化处理，实际项目中可能需要更复杂的URI到路径转换
            // 对于content://类型的URI，可能需要使用ContentResolver来获取真实路径
            if ("file".equals(uri.getScheme())) {
                return uri.getPath();
            } else {
                // 对于content://类型的URI，返回URI字符串
                // 在实际应用中，可能需要将图片复制到应用私有目录
                return uri.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 为content://类型的URI创建临时文件
     * @param uri 图片URI
     * @return 临时文件，如果创建失败返回null
     */
    private File createTempImageFile(Uri uri) {
        try {
            // 获取文件扩展名
            String fileName = "temp_image_" + System.currentTimeMillis();
            String extension = getFileExtensionFromUri(uri);
            if (extension != null) {
                fileName += "." + extension;
            } else {
                fileName += ".jpg"; // 默认扩展名
            }

            // 在应用私有目录创建临时文件
            File tempDir = new File(getCacheDir(), "temp_images");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            File tempFile = new File(tempDir, fileName);

            // 复制URI内容到临时文件
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                
                if (inputStream != null) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    return tempFile;
                }
            }
        } catch (Exception e) {
            Log.e("AddFootprint", "创建临时图片文件失败: " + uri, e);
        }
        return null;
    }

    /**
     * 从URI获取文件扩展名
     * @param uri 图片URI
     * @return 文件扩展名，如果获取失败返回null
     */
    private String getFileExtensionFromUri(Uri uri) {
        try {
            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null) {
                if (mimeType.equals("image/jpeg")) {
                    return "jpg";
                } else if (mimeType.equals("image/png")) {
                    return "png";
                } else if (mimeType.equals("image/gif")) {
                    return "gif";
                } else if (mimeType.equals("image/webp")) {
                    return "webp";
                }
            }
        } catch (Exception e) {
            Log.e("AddFootprint", "获取文件扩展名失败: " + uri, e);
        }
        return null;
    }
}