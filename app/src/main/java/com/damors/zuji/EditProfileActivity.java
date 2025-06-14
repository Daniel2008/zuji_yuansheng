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
import com.damors.zuji.network.HutoolApiService;
import com.damors.zuji.utils.ImageUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;

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
    private HutoolApiService apiService;
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
        apiService = HutoolApiService.getInstance(this);
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
            currentAvatarUrl = getUserFieldSafely(userObj, "avatar");
            if (!TextUtils.isEmpty(currentAvatarUrl)) {
                Glide.with(this)
                    .load(currentAvatarUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
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
        try {
            // 压缩图片
            Bitmap compressedBitmap = ImageUtils.compressImage(this, imageUri, 800, 800, 80);
            if (compressedBitmap != null) {
                // 显示压缩后的图片
                imageViewAvatar.setImageBitmap(compressedBitmap);
                
                // 保存压缩后的图片到临时文件
                tempImageFile = ImageUtils.saveBitmapToTempFile(this, compressedBitmap);
                
                Log.d(TAG, "图片处理成功");
            } else {
                Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "处理图片失败", e);
            Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show();
        }
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
        
        // 调用实际的更新用户资料API
        apiService.saveUserInfo(token, username, avatarFile, 
            // 成功回调
            response -> {
                Log.d(TAG, "用户信息保存成功: " + response);
                
                try {
                    // 解析响应数据
                    JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();
                    
                    if (responseObj.has("code") && responseObj.get("code").getAsInt() == 200) {
                        // 更新成功，处理返回的用户数据
                        if (responseObj.has("data") && !responseObj.get("data").isJsonNull()) {
                            JsonObject userData = responseObj.getAsJsonObject("data");
                            updateLocalUserData(username, userData);
                        } else {
                            // 没有返回用户数据，只更新本地昵称
                            updateLocalUserData(username, null);
                        }
                        
                        runOnUiThread(() -> {
                            Toast.makeText(this, "资料更新成功", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    } else {
                        // 服务器返回错误
                        String errorMsg = responseObj.has("msg") ? 
                            responseObj.get("msg").getAsString() : "更新失败";
                        runOnUiThread(() -> {
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                            resetSaveButton();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析响应数据失败", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "数据解析失败", Toast.LENGTH_SHORT).show();
                        resetSaveButton();
                    });
                }
            },
            // 错误回调
            errorMessage -> {
                Log.e(TAG, "用户信息保存失败: " + errorMessage);
                runOnUiThread(() -> {
                    Toast.makeText(this, "更新失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                    resetSaveButton();
                });
            },
            // 加载状态回调
            new HutoolApiService.LoadingCallback() {
                @Override
                public void onLoadingStart() {
                    // 已经在saveProfile方法中设置了按钮状态
                }
                
                @Override
                public void onLoadingEnd() {
                    // 在成功或失败回调中处理按钮状态重置
                }
            }
        );
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