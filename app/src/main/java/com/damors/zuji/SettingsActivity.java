package com.damors.zuji;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.damors.zuji.manager.UserManager;

/**
 * 设置页面Activity
 * 提供应用的各种设置选项，包括退出登录功能
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    
    // UI组件
    private Toolbar toolbar;
    private LinearLayout layoutLogout;
    
    // 管理器
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_settings);
        
        // 初始化组件
        initViews();
        initManagers();
        setupListeners();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        layoutLogout = findViewById(R.id.layout_logout);
        
        // 设置工具栏
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设置");
        }
    }

    /**
     * 初始化管理器
     */
    private void initManagers() {
        userManager = UserManager.getInstance();
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 工具栏返回按钮
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // 退出登录点击事件
        layoutLogout.setOnClickListener(v -> showLogoutDialog());
    }

    /**
     * 显示退出登录确认对话框
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？退出后需要重新登录才能使用完整功能。")
                .setPositiveButton("确定", (dialog, which) -> performLogout())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 执行退出登录操作
     */
    private void performLogout() {
        try {
            // 清除用户登录信息
            userManager.logout(this);
            
            // 显示退出成功提示
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
            
            // 跳转到登录页面
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            
            // 结束当前Activity
            finish();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "退出登录时发生错误", e);
            Toast.makeText(this, "退出登录失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}