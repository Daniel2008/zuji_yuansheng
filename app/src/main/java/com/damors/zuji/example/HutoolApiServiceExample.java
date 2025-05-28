package com.damors.zuji.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.damors.zuji.R;
import com.damors.zuji.network.HutoolApiService;
import com.damors.zuji.model.response.LoginResponse;
import com.damors.zuji.model.response.FootprintMessageResponse;

/**
 * HutoolApiService 使用示例Activity
 * 展示如何在实际项目中使用新的网络请求服务
 * 
 * 这个示例展示了：
 * 1. 如何进行短信登录
 * 2. 如何获取足迹动态列表
 * 3. 如何处理成功和失败回调
 * 4. 如何显示加载状态
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
public class HutoolApiServiceExample extends Activity {
    
    private static final String TAG = "HutoolApiExample";
    
    // UI组件
    private EditText etPhone;
    private EditText etCode;
    private EditText etDeviceId;
    private Button btnLogin;
    private Button btnGetFootprints;
    private ProgressBar progressBar;
    
    // 网络服务
    private HutoolApiService apiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 注意：这里假设有对应的布局文件，实际使用时需要创建
        // setContentView(R.layout.activity_hutool_api_example);
        
        // 初始化网络服务
        initApiService();
        
        // 初始化UI组件
        initViews();
        
        // 设置点击事件
        setupClickListeners();
    }

    /**
     * 初始化网络服务
     */
    private void initApiService() {
        apiService = HutoolApiService.getInstance(this);
        Log.d(TAG, "HutoolApiService 初始化完成");
    }

    /**
     * 初始化UI组件
     * 注意：实际使用时需要创建对应的布局文件
     */
    private void initViews() {
        // 这里是示例代码，实际使用时需要创建对应的布局文件
        /*
        etPhone = findViewById(R.id.et_phone);
        etCode = findViewById(R.id.et_code);
        etDeviceId = findViewById(R.id.et_device_id);
        btnLogin = findViewById(R.id.btn_login);
        btnGetFootprints = findViewById(R.id.btn_get_footprints);
        progressBar = findViewById(R.id.progress_bar);
        
        // 设置默认值
        etDeviceId.setText(android.provider.Settings.Secure.getString(
            getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));
        */
        
        Log.d(TAG, "UI组件初始化完成");
    }

    /**
     * 设置点击事件监听器
     */
    private void setupClickListeners() {
        // 登录按钮点击事件
        /*
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });
        
        // 获取足迹动态按钮点击事件
        btnGetFootprints.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFootprintMessages();
            }
        });
        */
        
        Log.d(TAG, "点击事件设置完成");
    }

    /**
     * 执行登录操作
     * 展示如何使用HutoolApiService进行短信登录
     */
    private void performLogin() {
        // 获取输入参数
        String phone = "13800138000"; // etPhone.getText().toString().trim();
        String code = "123456"; // etCode.getText().toString().trim();
        String deviceId = "example_device_id"; // etDeviceId.getText().toString().trim();
        
        // 参数验证
        if (!validateLoginParams(phone, code, deviceId)) {
            return;
        }
        
        // 显示加载状态
        showLoading(true);
        
        Log.d(TAG, "开始执行登录操作");
        
        // 调用登录接口
        apiService.smsLogin(
            phone,
            code,
            deviceId,
            new HutoolApiService.SuccessCallback<LoginResponse.Data>() {
                @Override
                public void onSuccess(LoginResponse.Data data) {
                    // 隐藏加载状态
                    showLoading(false);
                    
                    // 处理登录成功
                    handleLoginSuccess(data);
                }
            },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    // 隐藏加载状态
                    showLoading(false);
                    
                    // 处理登录失败
                    handleLoginError(errorMessage);
                }
            }
        );
    }

    /**
     * 验证登录参数
     * 
     * @param phone 手机号
     * @param code 验证码
     * @param deviceId 设备ID
     * @return 参数是否有效
     */
    private boolean validateLoginParams(String phone, String code, String deviceId) {
        if (phone == null || phone.trim().isEmpty()) {
            showToast("请输入手机号");
            return false;
        }
        
        if (code == null || code.trim().isEmpty()) {
            showToast("请输入验证码");
            return false;
        }
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            showToast("设备ID不能为空");
            return false;
        }
        
        // 手机号格式验证
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            showToast("请输入正确的手机号");
            return false;
        }
        
        // 验证码长度验证
        if (code.length() != 6) {
            showToast("验证码应为6位数字");
            return false;
        }
        
        return true;
    }

    /**
     * 处理登录成功
     * 
     * @param data 登录响应数据
     */
    private void handleLoginSuccess(LoginResponse.Data data) {
        Log.d(TAG, "登录成功");
        
        if (data != null) {
            Log.d(TAG, "用户信息: " + data.toString());
            
            // 保存用户信息到本地
            // UserManager.getInstance().saveUserInfo(data);
            
            showToast("登录成功");
            
            // 跳转到主界面
            // Intent intent = new Intent(this, MainActivity.class);
            // startActivity(intent);
            // finish();
        } else {
            Log.w(TAG, "登录成功但数据为空");
            showToast("登录成功，但用户数据异常");
        }
    }

    /**
     * 处理登录失败
     * 
     * @param errorMessage 错误消息
     */
    private void handleLoginError(String errorMessage) {
        Log.e(TAG, "登录失败: " + errorMessage);
        
        // 显示错误消息
        showToast("登录失败: " + errorMessage);
        
        // 根据错误类型进行不同处理
        if (errorMessage.contains("网络")) {
            // 网络错误，提示用户检查网络
            showToast("网络连接异常，请检查网络设置");
        } else if (errorMessage.contains("验证码")) {
            // 验证码错误，清空验证码输入框
            // etCode.setText("");
            // etCode.requestFocus();
        } else if (errorMessage.contains("手机号")) {
            // 手机号错误，清空手机号输入框
            // etPhone.setText("");
            // etPhone.requestFocus();
        }
    }

    /**
     * 获取足迹动态列表
     * 展示如何使用HutoolApiService获取列表数据
     */
    private void getFootprintMessages() {
        Log.d(TAG, "开始获取足迹动态列表");
        
        // 显示加载状态
        showLoading(true);
        
        // 调用获取足迹动态接口
        apiService.getFootprintMessages(
            1, // 第一页
            20, // 每页20条
            new HutoolApiService.SuccessCallback<FootprintMessageResponse.Data>() {
                @Override
                public void onSuccess(FootprintMessageResponse.Data data) {
                    // 隐藏加载状态
                    showLoading(false);
                    
                    // 处理获取成功
                    handleGetFootprintMessagesSuccess(data);
                }
            },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    // 隐藏加载状态
                    showLoading(false);
                    
                    // 处理获取失败
                    handleGetFootprintMessagesError(errorMessage);
                }
            }
        );
    }

    /**
     * 处理获取足迹动态成功
     * 
     * @param data 足迹动态数据
     */
    private void handleGetFootprintMessagesSuccess(FootprintMessageResponse.Data data) {
        Log.d(TAG, "获取足迹动态成功");
        
        if (data != null) {
            Log.d(TAG, "足迹动态数据: " + data.toString());
            
            showToast("获取足迹动态成功");
            
            // 更新UI显示数据
            // updateFootprintMessagesList(data.getList());
        } else {
            Log.w(TAG, "获取足迹动态成功但数据为空");
            showToast("暂无足迹动态数据");
        }
    }

    /**
     * 处理获取足迹动态失败
     * 
     * @param errorMessage 错误消息
     */
    private void handleGetFootprintMessagesError(String errorMessage) {
        Log.e(TAG, "获取足迹动态失败: " + errorMessage);
        
        showToast("获取足迹动态失败: " + errorMessage);
    }

    /**
     * 显示或隐藏加载状态
     * 
     * @param show 是否显示加载状态
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        
        // 禁用或启用按钮
        if (btnLogin != null) {
            btnLogin.setEnabled(!show);
        }
        if (btnGetFootprints != null) {
            btnGetFootprints.setEnabled(!show);
        }
        
        Log.d(TAG, show ? "显示加载状态" : "隐藏加载状态");
    }

    /**
     * 显示Toast消息
     * 
     * @param message 消息内容
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Toast: " + message);
    }

    /**
     * 使用Lambda表达式的示例（Java 8+）
     * 展示更简洁的回调写法
     */
    private void performLoginWithLambda() {
        String phone = "13800138000";
        String code = "123456";
        String deviceId = "example_device_id";
        
        showLoading(true);
        
        // 使用Lambda表达式简化回调
        apiService.smsLogin(
            phone, code, deviceId,
            data -> {
                // 成功回调
                showLoading(false);
                Log.d(TAG, "登录成功: " + (data != null ? data.toString() : "数据为空"));
                showToast("登录成功");
            },
            errorMessage -> {
                // 错误回调
                showLoading(false);
                Log.e(TAG, "登录失败: " + errorMessage);
                showToast("登录失败: " + errorMessage);
            }
        );
    }

    /**
     * 批量请求示例
     * 展示如何同时发起多个请求
     */
    private void performBatchRequests() {
        Log.d(TAG, "开始批量请求");
        
        // 同时获取多页数据
        for (int page = 1; page <= 3; page++) {
            final int currentPage = page;
            
            apiService.getFootprintMessages(
                currentPage,
                10,
                data -> {
                    Log.d(TAG, "第" + currentPage + "页数据获取成功");
                    // 处理每页数据
                },
                errorMessage -> {
                    Log.e(TAG, "第" + currentPage + "页数据获取失败: " + errorMessage);
                }
            );
        }
    }

    /**
     * 错误重试示例
     * 展示如何实现手动重试机制
     */
    private void performLoginWithRetry() {
        performLoginWithRetry(3); // 最多重试3次
    }
    
    private void performLoginWithRetry(int maxRetries) {
        String phone = "13800138000";
        String code = "123456";
        String deviceId = "example_device_id";
        
        apiService.smsLogin(
            phone, code, deviceId,
            data -> {
                // 成功，不需要重试
                Log.d(TAG, "登录成功，无需重试");
                handleLoginSuccess(data);
            },
            errorMessage -> {
                Log.e(TAG, "登录失败: " + errorMessage);
                
                // 判断是否需要重试
                if (maxRetries > 0 && shouldRetry(errorMessage)) {
                    Log.d(TAG, "准备重试，剩余重试次数: " + (maxRetries - 1));
                    
                    // 延迟1秒后重试
                    new android.os.Handler().postDelayed(() -> {
                        performLoginWithRetry(maxRetries - 1);
                    }, 1000);
                } else {
                    // 不再重试，显示最终错误
                    handleLoginError(errorMessage);
                }
            }
        );
    }
    
    /**
     * 判断是否应该重试
     * 
     * @param errorMessage 错误消息
     * @return 是否应该重试
     */
    private boolean shouldRetry(String errorMessage) {
        // 网络相关错误可以重试
        return errorMessage.contains("网络") || 
               errorMessage.contains("超时") || 
               errorMessage.contains("连接");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 释放资源
        if (apiService != null) {
            apiService.destroy();
        }
        
        Log.d(TAG, "Activity销毁，资源已释放");
    }
}