package com.damors.zuji;

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
// 移除AlertDialog导入，因为不再使用头像选择对话框
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.UserInfoResponse;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.utils.ImageUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 用户资料编辑页面
 * 支持编辑用户头像和用户名
 */
public class EditProfileActivity extends AppCompatActivity {
    
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
    private RetrofitApiService apiService;
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
        apiService = RetrofitApiService.getInstance(this);
    }
    
    private void setupClickListeners() {
        // 返回按钮
        buttonBack.setOnClickListener(v -> finish());
        
        // 头像点击事件 - 直接跳转到相册选择
        imageViewAvatar.setOnClickListener(v -> checkStoragePermissionAndSelectFromGallery());
        
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
     * 检查存储权限并从相册选择
     */
    private void checkStoragePermissionAndSelectFromGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            selectFromGallery();
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectFromGallery();
                } else {
                    Toast.makeText(this, "需要存储权限才能选择照片", Toast.LENGTH_SHORT).show();
                }
                break;
        }
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
        
        // 构建完整的用户信息JSON
        String userInfoJson = buildUserInfoJson(username, token);
        if (TextUtils.isEmpty(userInfoJson)) {
            Toast.makeText(this, "用户信息构建失败", Toast.LENGTH_SHORT).show();
            resetSaveButton();
            return;
        }
        
        // 将userInfoJson转换为RequestBody
        okhttp3.RequestBody userInfoRequestBody = okhttp3.RequestBody.create(
            okhttp3.MediaType.parse("application/json; charset=utf-8"), 
            userInfoJson
        );
        
        // 调用实际的更新用户资料API
        apiService.saveUserInfo(userInfoRequestBody, 
            // 成功回调
            new RetrofitApiService.SuccessCallback<BaseResponse<String>>() {
                @Override
                public void onSuccess(BaseResponse<String> response) {
                    if (response.getCode() == 200) {
                        Log.d(TAG, "用户信息保存成功: " + response);
                        
                        // 解析服务器返回的用户数据，特别是新的头像信息
                        JsonObject serverUserData = null;
                        try {
                            String responseData = response.getData();
                            Log.d(TAG, "服务器响应数据: " + responseData);
                            
                            // 先尝试解析response.getData()，看是否包含用户数据
                            if (!TextUtils.isEmpty(responseData)) {
                                try {
                                    // 如果responseData是JSON格式
                                    if (responseData.trim().startsWith("{")) {
                                        serverUserData = JsonParser.parseString(responseData).getAsJsonObject();
                                        Log.d(TAG, "从响应中解析到用户数据: " + serverUserData.toString());
                                        } else {
                                        // 如果responseData只是简单字符串，说明需要重新获取用户信息
                                        Log.d(TAG, "响应为简单字符串，需要重新获取用户信息: " + responseData);
                                    }
                                } catch (Exception parseEx) {
                                    Log.w(TAG, "解析响应JSON失败: " + parseEx.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "处理服务器响应失败: " + e.getMessage());
                        }
                        
                        final JsonObject finalServerUserData = serverUserData;
                        
                        // 如果没有从响应中获取到用户数据，需要重新获取用户信息
                         if (finalServerUserData == null) {
                             final String username = editTextUsername.getText().toString().trim();
                             Log.d(TAG, "响应中没有用户数据，重新获取用户信息");
                     // 获取当前用户的token
                     String currentToken = UserManager.getInstance().getToken();
                     if (currentToken != null) {
                         // 重新获取用户信息以获取最新的头像URL
                         apiService.getUserInfo(
                             new RetrofitApiService.SuccessCallback<UserInfoResponse>() {
                                 @Override
                                 public void onSuccess(UserInfoResponse response) {
                                runOnUiThread(() -> {
                                    if (response.getCode() == 200 && response.getData() != null) {
                                        // 使用最新的用户信息更新本地数据
                                        JsonObject userJsonObj = response.getData().getUser();
                                        JsonObject latestUserData = userJsonObj;
                                        
                                        updateLocalUserData(username, latestUserData);
                                        
                                        // 更新当前页面头像显示
                                        String newAvatar = null;
                                        if (userJsonObj.has("avatar") && !userJsonObj.get("avatar").isJsonNull()) {
                                            newAvatar = userJsonObj.get("avatar").getAsString();
                                        }
                                        if (!TextUtils.isEmpty(newAvatar)) {
                                            String newAvatarUrl = ApiConfig.getImageBaseUrl() + newAvatar;
                                            Glide.with(EditProfileActivity.this)
                                                .load(newAvatarUrl)
                                                .placeholder(R.drawable.ic_default_avatar)
                                                .error(R.drawable.ic_default_avatar)
                                                .circleCrop()
                                                .into(imageViewAvatar);
                                            Log.d(TAG, "重新获取用户信息后更新头像: " + newAvatarUrl);
                                        }
                                    } else {
                                        // 如果获取用户信息失败，只更新用户名
                                        updateLocalUserData(username, null);
                                    }
                                    
                                    Toast.makeText(EditProfileActivity.this, "资料更新成功", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                });
                            }
                        },
                        new RetrofitApiService.ErrorCallback() {
                            @Override
                            public void onError(String errorMessage) {
                                Log.w(TAG, "重新获取用户信息失败: " + errorMessage);
                                runOnUiThread(() -> {
                                    // 即使获取用户信息失败，也要更新本地用户名
                                    updateLocalUserData(username, null);
                                    Toast.makeText(EditProfileActivity.this, "资料更新成功", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                });
                            }
                        }
                    );
                     } else {
                         Log.w(TAG, "无法获取用户token，跳过重新获取用户信息");
                         runOnUiThread(() -> {
                             // 即使无法重新获取用户信息，也要更新本地用户名
                             updateLocalUserData(username, null);
                             Toast.makeText(EditProfileActivity.this, "资料更新成功", Toast.LENGTH_SHORT).show();
                             setResult(RESULT_OK);
                             finish();
                         });
                     }
                } else {
                    // 有服务器返回的用户数据，直接使用
                    runOnUiThread(() -> {
                        // 更新本地用户数据，传入服务器返回的数据
                        updateLocalUserData(username, finalServerUserData);
                        
                        // 如果有新头像，立即更新当前页面显示
                        if (finalServerUserData.has("avatar") 
                            && !finalServerUserData.get("avatar").isJsonNull()) {
                            String newAvatar = finalServerUserData.get("avatar").getAsString();
                            if (!TextUtils.isEmpty(newAvatar)) {
                                String newAvatarUrl = ApiConfig.getImageBaseUrl() + newAvatar;
                                Glide.with(EditProfileActivity.this)
                                    .load(newAvatarUrl)
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .error(R.drawable.ic_default_avatar)
                                    .circleCrop()
                                    .into(imageViewAvatar);
                                Log.d(TAG, "使用响应数据更新头像: " + newAvatarUrl);
                            }
                        }
                        
                        Toast.makeText(EditProfileActivity.this, "资料更新成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
                    } else {
                        String msg = response.getMsg() != null ? response.getMsg() : "更新失败";
                        Log.e(TAG, "用户信息保存失败: " + msg);
                        runOnUiThread(() -> {
                            Toast.makeText(EditProfileActivity.this, "更新失败: " + msg, Toast.LENGTH_SHORT).show();
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
     * 构建完整的用户信息JSON
     * 
     * @param username 用户名
     * @param token 用户token
     * @return 用户信息JSON字符串
     */
    private String buildUserInfoJson(String username, String token) {
        try {
            // 获取当前用户数据作为基础
            String currentUserJson = userManager.getCurrentUserJson();
            JsonObject userObj = new JsonObject();
            
            if (!TextUtils.isEmpty(currentUserJson)) {
                // 解析现有用户数据
                JsonObject currentUserObj = JsonParser.parseString(currentUserJson).getAsJsonObject();
                
                // 只复制基本的字符串字段，避免复杂对象导致的序列化问题
                String[] basicFields = {"userId", "userName", "nickName", "email", "phonenumber", "sex", "avatar", "status"};
                
                for (String field : basicFields) {
                    if (currentUserObj.has(field) && !currentUserObj.get(field).isJsonNull()) {
                        JsonElement element = currentUserObj.get(field);
                        // 只处理基本类型，跳过复杂对象
                        if (element.isJsonPrimitive()) {
                            userObj.add(field, element);
                        }
                    }
                }
            }
            
            // 更新必要字段
            userObj.addProperty("nickName", username);
            
            // 确保userId字段存在
            if (!userObj.has("userId") || userObj.get("userId").isJsonNull()) {
                userObj.addProperty("userId", "");
            }
            
            return new Gson().toJson(userObj);
        } catch (Exception e) {
            Log.e(TAG, "构建用户信息JSON失败", e);
            return null;
        }
    }
    
    /**
     * 更新本地用户数据
     * 
     * @param username 新的用户名
     * @param serverUserData 服务器返回的用户数据（可为null）
     */
    private void updateLocalUserData(String username, JsonObject serverUserData) {
        try {
            // 获取当前用户数据
            String userJson = userManager.getCurrentUserJson();
            if (!TextUtils.isEmpty(userJson)) {
                JsonObject userObj = JsonParser.parseString(userJson).getAsJsonObject();
                
                // 更新用户名
                userObj.addProperty("nickName", username);
                
                // 如果服务器返回了用户数据，使用服务器的数据更新
                if (serverUserData != null) {
                    // 更新头像URL（如果服务器返回了新的头像URL）
                    if (serverUserData.has("avatar") && !serverUserData.get("avatar").isJsonNull()) {
                        String newAvatarUrl = serverUserData.get("avatar").getAsString();
                        userObj.addProperty("avatar", newAvatarUrl);
                        Log.d(TAG, "更新头像URL: " + newAvatarUrl);
                    }
                    
                    // 更新其他可能的用户信息字段
                    if (serverUserData.has("userName") && !serverUserData.get("userName").isJsonNull()) {
                        userObj.addProperty("userName", serverUserData.get("userName").getAsString());
                    }
                }
                
                // 保存更新后的用户数据
                String updatedUserJson = new Gson().toJson(userObj);
                userManager.saveUserAndToken(updatedUserJson, userManager.getToken());
                
                Log.d(TAG, "本地用户数据更新成功");
            }
        } catch (Exception e) {
            Log.e(TAG, "更新本地用户数据失败", e);
        }
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