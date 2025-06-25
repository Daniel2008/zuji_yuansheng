package com.damors.zuji.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.damors.zuji.BuildConfig;
import com.damors.zuji.R;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.network.RetrofitApiService;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.gyf.immersionbar.ImmersionBar;

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
public class SplashActivity extends BaseActivity {
    
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 2000; // 启动页显示时间（毫秒）
    private static final int ANIMATION_DURATION = 800; // 动画持续时间（毫秒）
    
    // UI组件
    private CardView logoContainer;
    private ImageView logoImageView;
    private TextView appNameTextView;
    private TextView appSloganTextView;
    private TextView loadingTextView;
    private TextView versionTextView;
    private CircularProgressIndicator modernProgressBar;
    private View contentContainer;
    private View decorCircle1;
    private View decorCircle2;
    
    // 业务组件
    private RetrofitApiService apiService;
    private UserManager userManager;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        apiService = RetrofitApiService.getInstance(getApplicationContext());
        userManager = UserManager.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "组件初始化完成");
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        // 初始化UI组件
        contentContainer = findViewById(R.id.contentContainer);
        logoContainer = findViewById(R.id.logoContainer);
        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
        appSloganTextView = findViewById(R.id.appSloganTextView);
        loadingTextView = findViewById(R.id.loadingTextView);
        versionTextView = findViewById(R.id.versionTextView);
        modernProgressBar = findViewById(R.id.modernProgressBar);
        decorCircle1 = findViewById(R.id.decorCircle1);
        decorCircle2 = findViewById(R.id.decorCircle2);
        
        // 设置应用信息
        appNameTextView.setText("足迹");
        appSloganTextView.setText("记录生活，留下足迹");
        loadingTextView.setText("正在启动...");
        versionTextView.setText(BuildConfig.VERSION_NAME);
        
        // 设置初始状态（为动画做准备）
        setupInitialAnimationState();
        
        // 启动入场动画
        startEntranceAnimations();
        
        Log.d(TAG, "视图初始化完成");
    }
    
    /**
     * 开始自动登录检查
     * 修复重复登录问题：增强登录状态检查逻辑
     */
    private void startAutoLoginCheck() {
        Log.d(TAG, "开始自动登录检查");
        
        // 延迟开始检查，等待动画完成
        mainHandler.postDelayed(() -> {
            // 更新加载提示（带动画效果）
            updateLoadingTextWithAnimation("欢迎回来");
            
            // 先检查并同步登录状态，确保数据一致性
            if (UserManager.checkAndSyncLoginState()) {
                Log.d(TAG, "发现有效的本地登录信息，开始验证token有效性");
                
                // 验证token有效性
                UserManager.validateTokenAndUpdateUserInfo((isValid, message) -> {
                    Log.d(TAG, "Token验证结果: " + (isValid ? "有效" : "无效") + ", 消息: " + message);

                    if (isValid) {
                        // Token有效，跳转到主页面
                        navigateToMainActivity();
                    } else {
                        // Token无效，跳转到登录页面
                        Log.d(TAG, "Token验证失败，跳转到登录页面");
                        navigateToLoginActivity();
                    }
                });
            } else {
                Log.d(TAG, "未发现有效的本地登录信息，延迟后跳转到登录页面");
                
                // 没有有效登录信息，延迟后跳转到登录页面
                mainHandler.postDelayed(this::navigateToLoginActivity, SPLASH_DELAY_MS);
            }
        }, 1000); // 等待1秒让动画完成
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
        }, 1000);
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
    
    /**
     * 设置动画初始状态
     */
    private void setupInitialAnimationState() {
        // 设置内容容器初始状态
        contentContainer.setAlpha(0f);
        contentContainer.setTranslationY(100f);
        
        // 设置Logo容器初始状态
        logoContainer.setScaleX(0f);
        logoContainer.setScaleY(0f);
        
        // 设置装饰圆圈初始状态
        decorCircle1.setScaleX(0f);
        decorCircle1.setScaleY(0f);
        decorCircle2.setScaleX(0f);
        decorCircle2.setScaleY(0f);
        
        // 设置版本信息初始状态
        versionTextView.setAlpha(0f);
        
        Log.d(TAG, "动画初始状态设置完成");
    }
    
    /**
     * 启动入场动画
     */
    private void startEntranceAnimations() {
        // 延迟启动动画，确保布局完成
        mainHandler.postDelayed(() -> {
            // 装饰圆圈动画
            startDecorativeAnimations();
            
            // 主内容动画
            startMainContentAnimations();
            
            // 版本信息动画
            startVersionInfoAnimation();
            
        }, 100);
    }
    
    /**
     * 启动装饰性动画
     */
    private void startDecorativeAnimations() {
        // 装饰圆圈1动画
        ObjectAnimator scaleX1 = ObjectAnimator.ofFloat(decorCircle1, "scaleX", 0f, 1f);
        ObjectAnimator scaleY1 = ObjectAnimator.ofFloat(decorCircle1, "scaleY", 0f, 1f);
        ObjectAnimator rotation1 = ObjectAnimator.ofFloat(decorCircle1, "rotation", 0f, 360f);
        
        AnimatorSet decorSet1 = new AnimatorSet();
        decorSet1.playTogether(scaleX1, scaleY1, rotation1);
        decorSet1.setDuration(ANIMATION_DURATION * 2);
        decorSet1.setInterpolator(new AccelerateDecelerateInterpolator());
        decorSet1.start();
        
        // 装饰圆圈2动画（延迟启动）
        mainHandler.postDelayed(() -> {
            ObjectAnimator scaleX2 = ObjectAnimator.ofFloat(decorCircle2, "scaleX", 0f, 1f);
            ObjectAnimator scaleY2 = ObjectAnimator.ofFloat(decorCircle2, "scaleY", 0f, 1f);
            ObjectAnimator rotation2 = ObjectAnimator.ofFloat(decorCircle2, "rotation", 0f, -360f);
            
            AnimatorSet decorSet2 = new AnimatorSet();
            decorSet2.playTogether(scaleX2, scaleY2, rotation2);
            decorSet2.setDuration(ANIMATION_DURATION * 2);
            decorSet2.setInterpolator(new AccelerateDecelerateInterpolator());
            decorSet2.start();
        }, 200);
    }
    
    /**
     * 启动主内容动画
     */
    private void startMainContentAnimations() {
        // Logo容器弹性缩放动画
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoContainer, "scaleX", 0f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoContainer, "scaleY", 0f, 1f);
        
        AnimatorSet logoAnimSet = new AnimatorSet();
        logoAnimSet.playTogether(logoScaleX, logoScaleY);
        logoAnimSet.setDuration(ANIMATION_DURATION);
        logoAnimSet.setInterpolator(new OvershootInterpolator(1.2f));
        logoAnimSet.setStartDelay(300);
        logoAnimSet.start();
        
        // 内容容器淡入和上移动画
        ObjectAnimator contentAlpha = ObjectAnimator.ofFloat(contentContainer, "alpha", 0f, 1f);
        ObjectAnimator contentTransY = ObjectAnimator.ofFloat(contentContainer, "translationY", 100f, 0f);
        
        AnimatorSet contentAnimSet = new AnimatorSet();
        contentAnimSet.playTogether(contentAlpha, contentTransY);
        contentAnimSet.setDuration(ANIMATION_DURATION);
        contentAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        contentAnimSet.setStartDelay(400);
        contentAnimSet.start();
    }
    
    /**
     * 启动版本信息动画
     */
    private void startVersionInfoAnimation() {
        ObjectAnimator versionAlpha = ObjectAnimator.ofFloat(versionTextView, "alpha", 0f, 1f);
        versionAlpha.setDuration(ANIMATION_DURATION);
        versionAlpha.setStartDelay(800);
        versionAlpha.start();
    }
    
    /**
     * 更新加载状态文本（带动画效果）
     * @param text 要显示的文本
     */
    private void updateLoadingTextWithAnimation(String text) {
        if (loadingTextView != null) {
            // 淡出当前文本
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(loadingTextView, "alpha", 1f, 0f);
            fadeOut.setDuration(200);
            fadeOut.start();
            
            // 延迟更新文本并淡入
            mainHandler.postDelayed(() -> {
                loadingTextView.setText(text);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(loadingTextView, "alpha", 0f, 1f);
                fadeIn.setDuration(200);
                fadeIn.start();
            }, 200);
        }
    }
}