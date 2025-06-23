package com.damors.zuji.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
// 移除MediaStore导入，因为不再使用相机拍照功能
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.damors.zuji.R;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.UserInfoModel;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.utils.GsonUtil;
import com.damors.zuji.utils.ImageUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 用户资料编辑页面
 * 支持编辑用户头像和用户名
 */
public class EditProfileActivity extends BaseActivity {
    
    private static final String TAG = "EditProfileActivity";
    // 移除相机相关常量，只保留相册选择功能
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_GALLERY = 201;
    
    private CircleImageView imageViewAvatar;
    private EditText editTextUsername;
    private Button buttonSave;
    private ImageView buttonBack;
    private androidx.appcompat.widget.Toolbar toolbar;
    
    private UserManager userManager;
    private RetrofitApiService retrofitApiService;
    private String currentAvatarUrl;
    private String currentUsername;
    private File tempImageFile;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏状态栏
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        setContentView(R.layout.activity_edit_profile);
        
        initViews();
        initData();
        setupClickListeners();
        loadCurrentUserData();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // 不设置默认的返回按钮，使用自定义的返回按钮
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("编辑资料");
        }
        
        imageViewAvatar = findViewById(R.id.image_view_avatar);
        editTextUsername = findViewById(R.id.edit_text_username);
        buttonSave = findViewById(R.id.button_save);
        buttonBack = findViewById(R.id.button_back);
    }
    
    private void initData() {
        userManager = UserManager.getInstance();
        retrofitApiService = RetrofitApiService.getInstance(this);
    }
    
    private void setupClickListeners() {
        // 返回按钮
        buttonBack.setOnClickListener(v -> finish());
        
        // 头像点击事件 - 直接跳转到相册选择
        imageViewAvatar.setOnClickListener(v -> selectFromGallery());
        
        // 保存按钮
        buttonSave.setOnClickListener(v -> saveProfile());
        
        // 移除Toolbar的导航按钮监听器，只使用自定义返回按钮
        // toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    /**
     * 加载当前用户数据
     */
    private void loadCurrentUserData() {
        if (userManager == null || !userManager.isLoggedIn()) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        String userJson = userManager.getCurrentUserJson();
        if (TextUtils.isEmpty(userJson)) {
            Toast.makeText(this, "获取用户信息失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        try {
            JsonObject userObj = JsonParser.parseString(userJson).getAsJsonObject();
            
            // 加载头像
            String avatar = getUserFieldSafely(userObj, "avatar");
            if (!TextUtils.isEmpty(avatar)) {
                // 拼接完整的头像URL
                currentAvatarUrl = ApiConfig.getImageBaseUrl() + avatar;
                Glide.with(this)
                    .load(currentAvatarUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(imageViewAvatar);
            }
            
            // 加载用户名
            String nickname = getUserFieldSafely(userObj, "nickName");
            if (!TextUtils.isEmpty(nickname)) {
                currentUsername = nickname;
                editTextUsername.setText(nickname);
            } else {
                String username = getUserFieldSafely(userObj, "userName");
                if (!TextUtils.isEmpty(username)) {
                    currentUsername = username;
                    editTextUsername.setText(username);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "解析用户数据失败", e);
            Toast.makeText(this, "加载用户数据失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 安全获取用户字段
     */
    private String getUserFieldSafely(JsonObject userObj, String fieldName) {
        if (userObj.has(fieldName) && !userObj.get(fieldName).isJsonNull()) {
            return userObj.get(fieldName).getAsString();
        }
        return null;
    }
    


    /**
     * 从相册选择
     */
    private void selectFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }
    


    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_GALLERY:
                    if (data != null && data.getData() != null) {
                        handleImageResult(data.getData());
                    }
                    break;
            }
        }
    }
    
    /**
     * 处理图片选择结果
     */
    private void handleImageResult(Uri imageUri) {
        // 在后台线程处理图片
        new Thread(() -> {
            try {
                // 压缩图片
                Bitmap compressedBitmap = ImageUtils.compressImage(this, imageUri, 800, 800, 80);
                if (compressedBitmap != null) {
                    // 保存压缩后的图片到临时文件
                    tempImageFile = ImageUtils.saveBitmapToTempFile(this, compressedBitmap);
                    
                    // 在主线程更新UI
                    runOnUiThread(() -> {
                        // 显示压缩后的图片
                        imageViewAvatar.setImageBitmap(compressedBitmap);
                        Log.d(TAG, "图片处理成功，UI已更新");
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "处理图片失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    /**
     * 保存用户资料
     */
    private void saveProfile() {
        String newUsername = editTextUsername.getText().toString().trim();
        
        // 验证用户名
        if (TextUtils.isEmpty(newUsername)) {
            Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (newUsername.length() < 2 || newUsername.length() > 20) {
            Toast.makeText(this, "用户名长度应在2-20个字符之间", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否有变更
        boolean hasAvatarChanged = tempImageFile != null && tempImageFile.exists();
        boolean hasUsernameChanged = !newUsername.equals(currentUsername);
        
        if (!hasAvatarChanged && !hasUsernameChanged) {
            Toast.makeText(this, "没有任何变更", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示保存进度
        buttonSave.setEnabled(false);
        buttonSave.setText("保存中...");
        
        // 调用API保存
        updateProfile(newUsername, tempImageFile);
    }
    
    /**
     * 更新用户资料
     */
    private void updateProfile(String username, File avatarFile) {
        String token = userManager.getToken();
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            resetSaveButton();
            return;
        }
        
        // 如果有头像文件，先上传头像
        if (avatarFile != null && avatarFile.exists()) {
            Log.d(TAG, "开始上传头像: " + avatarFile.getAbsolutePath());
            uploadUserInfoAndAvatar(username, avatarFile);
        } else {
            // 没有头像变更，直接保存用户信息
            Log.d(TAG, "没有头像变更，直接保存用户信息");
            saveUserInfoOnly(username);
        }
    }
    
    /**
     * 先上传头像，成功后再保存用户信息
     */
    private void uploadUserInfoAndAvatar(String username, File avatarFile) {
        Log.d(TAG, "开始上传头像: " + avatarFile.getAbsolutePath());
        
        // 调用头像上传接口
        retrofitApiService.uploadAvatar(avatarFile,
            new RetrofitApiService.SuccessCallback<BaseResponse<Map<String,Object>>>() {
                @Override
                public void onSuccess(BaseResponse<Map<String,Object>> response) {
                    Log.d(TAG, "头像上传成功回调被调用");
                    Log.d(TAG, "响应对象: " + (response != null ? response.toString() : "null"));
                    Map<String,Object> data = response.getData();
                    UserInfoModel userInfoModel = userManager.getUserInfo();
                    userInfoModel.setAvatar(data.get("fileName").toString());
                    userManager.saveUserInfo(userInfoModel);
                    runOnUiThread(() -> {
                        // 头像上传成功后，再保存用户信息
                        saveUserInfoOnly(username);
                    });
                }
            },
            new RetrofitApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "头像上传失败: " + errorMessage);
                    runOnUiThread(() -> {
                        Toast.makeText(EditProfileActivity.this, "头像上传失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                        resetSaveButton();
                    });
                }
            }
        );
    }
    
    /**
     * 仅保存用户信息（不包含头像上传）
     */
    private void saveUserInfoOnly(String username) {
        Log.d(TAG, "开始保存用户信息: " + username);
        

        
        // 构建用户信息JSON
        UserInfoModel userinfo = userManager.getUserInfo();
        userinfo.setNickName(username);
        
        // 创建RequestBody
        okhttp3.RequestBody userInfoRequestBody = okhttp3.RequestBody.create(
            okhttp3.MediaType.parse("application/json; charset=utf-8"), 
            GsonUtil.GsonString(userinfo)
        );
        
        // 调用实际的更新用户资料API
        retrofitApiService.saveUserInfo(userInfoRequestBody, 
            // 成功回调
            new RetrofitApiService.SuccessCallback<BaseResponse<UserInfoModel>>() {
                @Override
                public void onSuccess(BaseResponse<UserInfoModel> response) {
                    if (response.getCode() == 200) {
                        Log.d(TAG, "用户信息保存成功: " + response);
                        UserInfoModel userInfoModel = response.getData();
                        // 更新本地用户数据
                        userManager.saveUserInfo(userInfoModel);

                        if (!TextUtils.isEmpty(userInfoModel.getAvatar())) {
                            String newAvatarUrl = ApiConfig.getImageBaseUrl() + userInfoModel.getAvatar();
                            Glide.with(EditProfileActivity.this)
                                    .load(newAvatarUrl)
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .error(R.drawable.ic_default_avatar)
                                    .circleCrop()
                                    .into(imageViewAvatar);
                            Log.d(TAG, "重新获取用户信息后更新头像: " + newAvatarUrl);
                        }
                        Toast.makeText(EditProfileActivity.this, "资料更新成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();


                     } else {
                        Log.e(TAG, "用户信息保存失败: " + response);
                        runOnUiThread(() -> {
                            Toast.makeText(EditProfileActivity.this, "更新失败: " + response, Toast.LENGTH_SHORT).show();
                            resetSaveButton();
                        });
                    }
                }
            },
            // 错误回调
            new RetrofitApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "用户信息保存失败: " + errorMessage);
                    runOnUiThread(() -> {
                        Toast.makeText(EditProfileActivity.this, "更新失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                        resetSaveButton();
                    });
                }
            }
        );
    }
    
    /**
     * 重置保存按钮状态
     */
    private void resetSaveButton() {
        buttonSave.setEnabled(true);
        buttonSave.setText("保存");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理临时文件
        if (tempImageFile != null && tempImageFile.exists()) {
            tempImageFile.delete();
        }
    }
}