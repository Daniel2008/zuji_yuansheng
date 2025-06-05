package com.damors.zuji;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.damors.zuji.R;
import com.damors.zuji.MainActivity;
import com.damors.zuji.LoginActivity;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.network.HutoolApiService;

/**
 * 启动页面活动
 * 负责应用启动时的自动登录验证和页面跳转逻辑
 * 
 * 功能说明：
 * 1. 显示应用启动画面
 * 2. 检查用户是否已登录
 * 3. 验证token有效性
 * 4. 根据验证结果跳转到相应页面
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
public class SplashActivity extends AppCompatActivity {
    
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 2000; // 启动页显示时间（毫秒）
    
    private ImageView logoImageView;
    private TextView appNameTextView;
    private TextView loadingTextView;
    
    private HutoolApiService apiService;
    private UserManager userManager;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 隐藏状态栏，全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_splash);
        
        // 初始化组件
        initComponents();
        
        // 初始化视图
        initViews();
        
        // 开始自动登录检查
        startAutoLoginCheck();
    }
    
    /**
     * 初始化组件
     */
    private void initComponents() {
        apiService = HutoolApiService.getInstance(this);
        userManager = UserManager.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "组件初始化完成");
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
        loadingTextView = findViewById(R.id.loadingTextView);
        
        // 设置应用名称
        appNameTextView.setText("足迹");
        loadingTextView.setText("正在启动...");
        
        Log.d(TAG, "视图初始化完成");
    }
    
    /**
     * 开始自动登录检查
     */
    private void startAutoLoginCheck() {
        Log.d(TAG, "开始自动登录检查");
        
        // 更新加载提示
        loadingTextView.setText("正在验证登录状态...");
        
        // 检查用户是否已登录
        if (userManager.isLoggedIn()) {
            Log.d(TAG, "发现本地登录信息，开始验证token有效性");
            
            // 验证token有效性
            userManager.validateTokenAndUpdateUserInfo(apiService, new UserManager.TokenValidationCallback() {
                @Override
                public void onValidationResult(boolean isValid, String message) {
                    Log.d(TAG, "Token验证结果: " + (isValid ? "有效" : "无效") + ", 消息: " + message);
                    
                    if (isValid) {
                        // Token有效，跳转到主页面
                        navigateToMainActivity();
                    } else {
                        // Token无效，跳转到登录页面
                        navigateToLoginActivity();
                    }
                }
            });
        } else {
            Log.d(TAG, "未发现本地登录信息，延迟后跳转到登录页面");
            
            // 没有登录信息，延迟后跳转到登录页面
            mainHandler.postDelayed(this::navigateToLoginActivity, SPLASH_DELAY_MS);
        }
    }
    
    /**
     * 跳转到主页面
     */
    private void navigateToMainActivity() {
        Log.d(TAG, "跳转到主页面");
        
        loadingTextView.setText("登录成功，正在进入...");
        
        // 延迟一小段时间以显示成功消息
        mainHandler.postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // 结束启动页面
        }, 500);
    }
    
    /**
     * 跳转到登录页面
     */
    private void navigateToLoginActivity() {
        Log.d(TAG, "跳转到登录页面");
        
        loadingTextView.setText("请登录...");
        
        // 延迟一小段时间以显示消息
        mainHandler.postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // 结束启动页面
        }, 500);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 清理Handler中的回调，防止内存泄漏
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        
        Log.d(TAG, "SplashActivity销毁");
    }
}