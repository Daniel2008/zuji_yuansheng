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

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;

import java.util.HashMap;
import java.util.Map;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;

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

import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.utils.LoadingDialog;

import com.damors.zuji.adapter.ImageGridAdapter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AddFootprintActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener {

    private static final int REQUEST_PICK_IMAGES = 1;
    private static final int REQUEST_SELECT_LOCATION = 2;
    private static final int REQUEST_CAMERA = 3;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1003;
    private static final int REQUEST_CAMERA_PERMISSION = 1004;

    private EditText etContent;
    private GeocodeSearch geocodeSearch;
    private RecyclerView rvImages;
    private TextView tvLocation;
    private Button btnPublish;
    private RadioGroup rgMsgType;
    private RadioButton rbPublic;
    private RadioButton rbPrivate;
    private ChipGroup chipGroupTags;
    private Chip chipTagThoughts;
    private Chip chipTagAttraction;
    private Chip chipTagFood;

    private List<Uri> selectedImages = new ArrayList<>();
    private ImageGridAdapter imageAdapter;
    private String selectedLocation;
    private double latitude;
    private double longitude;
    private String city;
    private int msgType = 1; // 默认为公开类型
    private Uri photoUri; // 拍照后的图片URI
    
    // 网络服务实例
    private RetrofitApiService apiService;
    private LoadingDialog loadingDialog;
    
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
        apiService = RetrofitApiService.getInstance(this);
        // 初始化加载对话框
        loadingDialog = new LoadingDialog(this);
        
        // 初始化位置管理器
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        // 初始化城市信息
        city = "未知城市";
        
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
            // 获取城市信息
            city = intent.getStringExtra("city");

            // 如果有有效的经纬度信息，设置默认位置文本
            if (latitude != 0.0 && longitude != 0.0) {
                selectedLocation = address;
                Log.d("AddFootprintActivity", "从Intent获取位置信息: " + selectedLocation + 
                      ", 海拔: " + altitude + "m, 精度: " + accuracy + "m, 城市: " + city);
                
                // 如果没有获取到城市信息，进行逆地理编码
                if (city == null || city.isEmpty()) {
                    Log.d("AddFootprintActivity", "Intent中没有城市信息，进行逆地理编码获取");
                    performReverseGeocode(latitude, longitude);
                }
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
                 
                 // 初始化城市信息为未知
                 if (city == null || city.isEmpty()) {
                     city = "未知城市";
                 }
                 
                 // 临时显示经纬度
                 selectedLocation = String.format("正在获取位置信息... (%.6f, %.6f)", latitude, longitude);
                 updateLocationUI();
                 
                 // 使用高德地图SDK进行逆地理编码
                 performReverseGeocode(latitude, longitude);
                 
                 Log.d("AddFootprintActivity", "获取到当前位置坐标: " + latitude + ", " + longitude);
             } else {
                 // 设置默认城市信息
                 city = "未知城市";
                 Log.w("AddFootprintActivity", "无法获取当前位置，设置城市为未知城市");
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
         } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
             if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 Log.d("AddFootprintActivity", "相机权限已授予，打开相机");
                 openCamera();
             } else {
                 Log.w("AddFootprintActivity", "相机权限被拒绝");
                 Toast.makeText(this, "相机权限被拒绝，无法使用拍照功能", Toast.LENGTH_SHORT).show();
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
        
        // 初始化标签选择组件
        chipGroupTags = findViewById(R.id.chip_group_tags);
        chipTagThoughts = findViewById(R.id.chip_tag_thoughts);
        chipTagAttraction = findViewById(R.id.chip_tag_attraction);
        chipTagFood = findViewById(R.id.chip_tag_food);

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
                // 检查是否已经选择了9张图片
                if (selectedImages.size() >= 9) {
                    Toast.makeText(AddFootprintActivity.this, "最多只能选择9张图片", Toast.LENGTH_SHORT).show();
                    return;
                }
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

    /**
     * 显示图片选择对话框
     * 用户可以选择从相机拍照或从相册选择图片
     */
    private void pickImages() {
        // 创建选择对话框
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("选择图片");
        builder.setItems(new String[]{"拍照", "从相册选择"}, (dialog, which) -> {
            if (which == 0) {
                // 拍照
                openCamera();
            } else {
                // 从相册选择
                openGallery();
            }
        });
        builder.show();
    }
    
    /**
     * 打开相机拍照
     */
    private void openCamera() {
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // 创建临时文件保存拍照结果
            try {
                java.io.File photoFile = createImageFile();
                if (photoFile != null) {
                    photoUri = androidx.core.content.FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider", photoFile);
                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(intent, REQUEST_CAMERA);
                }
            } catch (java.io.IOException ex) {
                Log.e("AddFootprintActivity", "创建图片文件失败", ex);
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "没有可用的相机应用", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开相册选择图片
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_PICK_IMAGES);
    }
    
    /**
     * 创建图片文件
     * @return 创建的图片文件
     * @throws java.io.IOException 文件创建异常
     */
    private java.io.File createImageFile() throws java.io.IOException {
        // 创建图片文件名
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        java.io.File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return java.io.File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGES && data != null) {
                if (data.getClipData() != null) {
                    // 多选图片
                    int totalSelected = data.getClipData().getItemCount();
                    int availableSlots = 9 - selectedImages.size();
                    int count = Math.min(totalSelected, availableSlots);
                    
                    for (int i = 0; i < count; i++) {
                        selectedImages.add(data.getClipData().getItemAt(i).getUri());
                    }
                    
                    // 如果用户选择的图片数量超过了可用槽位，给出提示
                    if (totalSelected > availableSlots && availableSlots > 0) {
                        Toast.makeText(this, String.format("最多只能选择9张图片，已为您选择前%d张", count), Toast.LENGTH_SHORT).show();
                    } else if (availableSlots == 0) {
                        Toast.makeText(this, "已达到最大图片数量限制（9张）", Toast.LENGTH_SHORT).show();
                    }
                } else if (data.getData() != null) {
                    // 单选图片
                    if (selectedImages.size() < 9) {
                        selectedImages.add(data.getData());
                    } else {
                        Toast.makeText(this, "最多只能选择9张图片", Toast.LENGTH_SHORT).show();
                    }
                }
                imageAdapter.notifyDataSetChanged();
            } else if (requestCode == REQUEST_CAMERA) {
                // 处理相机拍照结果
                if (photoUri != null) {
                    if (selectedImages.size() < 9) {
                        selectedImages.add(photoUri);
                        imageAdapter.notifyDataSetChanged();
                        Log.d("AddFootprintActivity", "添加拍照图片: " + photoUri.toString());
                    } else {
                        Toast.makeText(this, "最多只能选择9张图片", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (requestCode == REQUEST_SELECT_LOCATION) {
                selectedLocation = data.getStringExtra("location");
                latitude = data.getDoubleExtra("latitude", 0);
                longitude = data.getDoubleExtra("longitude", 0);
                city = data.getStringExtra("city");
                
                // 如果没有获取到城市信息，进行逆地理编码
                if (city == null || city.isEmpty() || "未知城市".equals(city)) {
                    Log.d("AddFootprintActivity", "从位置选择器未获取到有效城市信息，进行逆地理编码");
                    performReverseGeocode(latitude, longitude);
                } else {
                    Log.d("AddFootprintActivity", "从位置选择器获取到城市信息: " + city);
                }
                
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
        
        // 准备发布参数
        
        // 创建发布参数对象
        PublishTrandsInfoPO publishInfo = new PublishTrandsInfoPO();
        publishInfo.setUserId(userId); // 当前用户ID
        publishInfo.setCity(city);
        publishInfo.setContent(content); // 足迹内容
        publishInfo.setLocationInfo(location); // 位置信息
        publishInfo.setType(msgType+""); // 类型 (1: 公开, 2: 个人可见)
        publishInfo.setTag(getSelectedTag()); // 获取选中的标签
        publishInfo.setLng(longitude); // 经度
        publishInfo.setLat(latitude); // 纬度
        publishInfo.setMsgType(msgType); // 消息类型 (1: 公开, 2: 个人可见)
        
        // 输出调试信息
        Log.d("AddFootprintActivity", "准备发布足迹 - 城市: " + city + ", 位置: " + location + ", 经纬度: (" + latitude + ", " + longitude + ")");
        
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
        
        // 显示加载对话框并禁用发布按钮
        loadingDialog.show("正在发布足迹...");
        btnPublish.setEnabled(false);
        btnPublish.setText("发布中...");
        
        // 将PublishTrandsInfoPO转换为Map<String, RequestBody>格式
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        requestBodyMap.put("userId", RequestBody.create(MediaType.parse("text/plain"), publishInfo.getUserId()));
        requestBodyMap.put("city", RequestBody.create(MediaType.parse("text/plain"), publishInfo.getCity() != null ? publishInfo.getCity() : ""));
        requestBodyMap.put("content", RequestBody.create(MediaType.parse("text/plain"), publishInfo.getContent()));
        requestBodyMap.put("locationInfo", RequestBody.create(MediaType.parse("text/plain"), publishInfo.getLocationInfo()));
        requestBodyMap.put("type", RequestBody.create(MediaType.parse("text/plain"), publishInfo.getType()));
        requestBodyMap.put("tag", RequestBody.create(MediaType.parse("text/plain"), publishInfo.getTag() != null ? publishInfo.getTag() : ""));
        requestBodyMap.put("lng", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(publishInfo.getLng())));
        requestBodyMap.put("lat", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(publishInfo.getLat())));
        requestBodyMap.put("msgType", RequestBody.create(MediaType.parse("text/plain"), String.valueOf(publishInfo.getMsgType())));
        
        // 准备图片文件列表
        List<MultipartBody.Part> imageParts = new ArrayList<>();
        if (publishInfo.getImages() != null) {
            for (int i = 0; i < publishInfo.getImages().size(); i++) {
                File imageFile = publishInfo.getImages().get(i);
                if (imageFile != null && imageFile.exists()) {
                    RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
                    MultipartBody.Part imagePart = MultipartBody.Part.createFormData("images", imageFile.getName(), requestFile);
                    imageParts.add(imagePart);
                }
            }
        }
        
        // 调用网络接口发布足迹
        apiService.publishFootprint(requestBodyMap, imageParts,
            new RetrofitApiService.SuccessCallback<BaseResponse<JSONObject>>() {
                @Override
                public void onSuccess(BaseResponse<JSONObject> response) {
                    // 隐藏加载对话框并恢复按钮状态
                    loadingDialog.dismiss();
                    btnPublish.setEnabled(true);
                    btnPublish.setText("发布足迹");
                    
                    if (response.getCode() == 200) {
                        // 发布成功
                        runOnUiThread(() -> {
                            Toast.makeText(AddFootprintActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    } else {
                        // 发布失败
                        runOnUiThread(() -> {
                            String msg = response.getMsg() != null ? response.getMsg() : "发布失败";
                            Toast.makeText(AddFootprintActivity.this, msg, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            },
            new RetrofitApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    // 隐藏加载对话框并恢复按钮状态
                    loadingDialog.dismiss();
                    btnPublish.setEnabled(true);
                    btnPublish.setText("发布足迹");
                    
                    // 发布失败
                    runOnUiThread(() -> {
                        Toast.makeText(AddFootprintActivity.this, 
                            "发布失败: " + errorMessage, Toast.LENGTH_LONG).show();
                    });
                }
            });
    }
    
    /**
     * 获取选中的标签
     * @return 选中的标签文本，如果没有选中则返回空字符串
     */
    private String getSelectedTag() {
        int selectedChipId = chipGroupTags.getCheckedChipId();
        if (selectedChipId == -1) {
            // 没有选中任何标签
            return "";
        }
        
        Chip selectedChip = findViewById(selectedChipId);
        if (selectedChip != null) {
            return selectedChip.getText().toString();
        }
        
        return "";
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
                
                // 提取城市信息
                if (regeocodeAddress.getCity() != null && !regeocodeAddress.getCity().isEmpty()) {
                    city = regeocodeAddress.getCity();
                    Log.d("AddFootprintActivity", "获取到城市信息: " + city);
                } else if (regeocodeAddress.getProvince() != null && !regeocodeAddress.getProvince().isEmpty()) {
                    // 如果没有城市信息，使用省份信息
                    city = regeocodeAddress.getProvince();
                    Log.d("AddFootprintActivity", "使用省份作为城市信息: " + city);
                } else {
                    city = "未知城市";
                    Log.w("AddFootprintActivity", "无法获取城市信息，设置为未知城市");
                }
                
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
                city = "未知城市";
                Log.w("AddFootprintActivity", "逆地理编码结果为空，设置城市为未知城市");
            }
        } else {
            selectedLocation = String.format("当前位置 (%.6f, %.6f)", latitude, longitude);
            city = "未知城市";
            Log.w("AddFootprintActivity", "逆地理编码失败，错误码: " + rCode + "，设置城市为未知城市");
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