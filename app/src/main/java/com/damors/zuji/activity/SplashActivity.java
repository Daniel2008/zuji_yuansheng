package com.damors.zuji.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import com.damors.zuji.utils.AnimationPerformanceMonitor;
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
    private static final int SPLASH_DELAY_MS = 1500; // 启动页显示时间（毫秒）- 优化：减少等待时间
    private static final int ANIMATION_DURATION = 400; // 动画持续时间（毫秒）- 优化：缩短动画时长
    private static final int STAGGER_DELAY = 100; // 动画错开延迟（毫秒）- 优化：减少错开时间
    
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
    private AnimationPerformanceMonitor performanceMonitor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        // 初始化组件
        initComponents();
        
        // 初始化视图
        initViews();
        
        // 启用硬件加速优化
        enableHardwareAcceleration();
        
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
        performanceMonitor = AnimationPerformanceMonitor.getInstance();
        
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
     * 启用硬件加速优化
     * 为关键视图启用硬件加速以提高动画性能
     */
    private void enableHardwareAcceleration() {
        // 为主要动画视图启用硬件加速
        if (contentContainer != null) {
            contentContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        if (logoContainer != null) {
            logoContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        if (decorCircle1 != null) {
            decorCircle1.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        if (decorCircle2 != null) {
            decorCircle2.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        
        Log.d(TAG, "硬件加速优化已启用");
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
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // 结束启动页面
        // 延迟一小段时间以显示成功消息
        mainHandler.postDelayed(() -> {

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
        
        // 停止性能监控
        if (BuildConfig.DEBUG && performanceMonitor != null && performanceMonitor.isMonitoring()) {
            performanceMonitor.stopMonitoring();
        }
        
        // 清理Handler中的回调，防止内存泄漏
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        
        // 清理硬件加速，释放GPU资源
        cleanupHardwareAcceleration();
        
        Log.d(TAG, "SplashActivity销毁");
    }
    
    /**
     * 清理硬件加速资源
     * 在Activity销毁时释放GPU资源
     */
    private void cleanupHardwareAcceleration() {
        if (contentContainer != null) {
            contentContainer.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if (logoContainer != null) {
            logoContainer.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if (decorCircle1 != null) {
            decorCircle1.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if (decorCircle2 != null) {
            decorCircle2.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        
        Log.d(TAG, "硬件加速资源已清理");
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
     * 启动入场动画（优化版本）
     * 简化动画逻辑，减少同时运行的动画数量
     */
    private void startEntranceAnimations() {
        // 延迟启动动画，确保布局完成
        mainHandler.postDelayed(() -> {
            // 使用序列动画替代并行动画，提高性能
            startOptimizedAnimationSequence();
        }, 50); // 减少延迟时间
    }
    
    /**
     * 优化的动画序列
     * 使用ViewPropertyAnimator提高性能，减少动画复杂度
     */
    private void startOptimizedAnimationSequence() {
        Log.d(TAG, "开始优化动画序列");
        
        // 启动性能监控
        if (BuildConfig.DEBUG) {
            performanceMonitor.startMonitoring();
        }
        // 第一阶段：装饰圆圈简化动画（仅缩放，移除旋转）
        decorCircle1.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        
        // 第二阶段：主内容动画（延迟启动）
        mainHandler.postDelayed(() -> {
            startMainContentOptimizedAnimation();
        }, STAGGER_DELAY);
        
        // 第三阶段：装饰圆圈2和版本信息（进一步延迟）
        mainHandler.postDelayed(() -> {
            decorCircle2.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            
            versionTextView.animate()
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION)
                    .start();
        }, STAGGER_DELAY * 2);
    }
    
    /**
     * 优化的主内容动画
     * 使用ViewPropertyAnimator替代ObjectAnimator，提高性能
     */
    private void startMainContentOptimizedAnimation() {
        // Logo容器弹性缩放动画（简化弹性效果）
        logoContainer.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new OvershootInterpolator(1.1f)) // 减少弹性强度
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // 动画结束后恢复软件渲染以节省资源
                        logoContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                })
                .start();
        
        // 内容容器淡入和上移动画（同时进行）
        contentContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setStartDelay(STAGGER_DELAY)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // 动画结束后恢复软件渲染以节省资源
                        contentContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                    }
                })
                .start();
    }
    
    /**
     * 更新加载状态文本（优化版本）
     * 使用ViewPropertyAnimator提高性能
     * @param text 要显示的文本
     */
    private void updateLoadingTextWithAnimation(String text) {
        if (loadingTextView != null) {
            // 使用ViewPropertyAnimator进行优化的文本切换动画
            loadingTextView.animate()
                    .alpha(0f)
                    .setDuration(150) // 缩短动画时间
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loadingTextView.setText(text);
                            loadingTextView.animate()
                                    .alpha(1f)
                                    .setDuration(150)
                                    .setListener(null)
                                    .start();
                        }
                    })
                    .start();
        }
    }
}