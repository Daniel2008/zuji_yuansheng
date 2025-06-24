package com.damors.zuji.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.damors.zuji.R;
import com.damors.zuji.adapter.CommentManagementAdapter;
import com.damors.zuji.adapter.CommentManagementPagerAdapter;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.CommentModel;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.UserInfoModel;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.utils.LoadingDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.gyf.immersionbar.ImmersionBar;

import java.util.ArrayList;
import java.util.List;

/**
 * 评论管理页面
 * 功能包括：
 * 1. 查看动态信息
 * 2. 显示用户回复的评论信息
 * 3. 评论未读提醒带角标
 */
public class CommentManagementActivity extends BaseActivity {
    
    private static final String TAG = "CommentManagementActivity";
    
    // UI组件
    private ImageView ivBack;
    private TextView tvTitle;
    private TextView tvUnreadCount; // 未读评论数量角标
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private CommentManagementPagerAdapter pagerAdapter;
    
    // 数据相关
    private RetrofitApiService apiService;
    private LoadingDialog loadingDialog;
    private int unreadCommentCount = 0; // 未读评论数量
    
    /**
     * 启动评论管理页面
     * @param context 上下文
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, CommentManagementActivity.class);
        context.startActivity(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_management);
        
        // 设置沉浸式状态栏
        ImmersionBar.with(this)
                .statusBarColor(R.color.white)
                .statusBarDarkFont(true)
                .init();
        
        initViews();
        initData();
        setupViewPager();
        loadUnreadCommentCount();
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        tvTitle = findViewById(R.id.tv_title);
        tvUnreadCount = findViewById(R.id.tv_unread_count);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        
        // 设置标题
        tvTitle.setText("评论管理");
        
        // 设置返回按钮点击事件
        ivBack.setOnClickListener(v -> finish());
    }
    
    /**
     * 初始化数据
     */
    private void initData() {
        apiService = RetrofitApiService.getInstance(this);
        loadingDialog = new LoadingDialog(this);
    }
    
    /**
     * 设置ViewPager和TabLayout
     */
    private void setupViewPager() {
        pagerAdapter = new CommentManagementPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // 关联TabLayout和ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("收到的评论");
                    break;
                case 1:
                    tab.setText("我的评论");
                    break;
            }
        }).attach();
    }
    
    /**
     * 加载未读评论数量
     */
    private void loadUnreadCommentCount() {
        try {
            // 从UserManager获取用户信息
            UserManager userManager = UserManager.getInstance();
            UserInfoModel userInfo = userManager.getUserInfo();
            
            if (userInfo != null && userInfo.getCommentNoReplyCount() != null) {
                // 获取真实的未读评论数量
                unreadCommentCount = userInfo.getCommentNoReplyCount().intValue();
                Log.d(TAG, "获取到未读评论数量: " + unreadCommentCount);
            } else {
                // 如果用户信息为空或未读评论数为空，设置为0
                unreadCommentCount = 0;
                Log.d(TAG, "用户信息为空或未读评论数为空，设置未读评论数为0");
            }
        } catch (Exception e) {
            // 异常情况下设置为0
            Log.e(TAG, "获取未读评论数量时发生异常: " + e.getMessage(), e);
            unreadCommentCount = 0;
        }
        
        // 更新角标显示
        updateUnreadCountBadge();
    }
    
    /**
     * 更新未读评论数量角标
     */
    private void updateUnreadCountBadge() {
        if (unreadCommentCount > 0) {
            tvUnreadCount.setVisibility(View.VISIBLE);
            if (unreadCommentCount > 99) {
                tvUnreadCount.setText("99+");
            } else {
                tvUnreadCount.setText(String.valueOf(unreadCommentCount));
            }
        } else {
            tvUnreadCount.setVisibility(View.GONE);
        }
    }
    
    /**
     * 标记评论为已读
     * @param commentId 评论ID
     */
    public void markCommentAsRead(Integer commentId) {
        // TODO: 调用API标记评论为已读
        // 标记成功后，重新加载用户数据以获取最新的未读评论数量
        refreshUnreadCount();
    }
    
    /**
     * 刷新用户数据并更新未读评论数量
     * 用于在评论状态改变后同步最新数据
     */
    public void refreshUserDataAndUnreadCount() {
        try {
            // 强制重新加载用户数据
            UserManager userManager = UserManager.getInstance();
            userManager.reloadUserData();
            Log.d(TAG, "已刷新用户数据");
            
            // 重新加载未读评论数量
            loadUnreadCommentCount();
        } catch (Exception e) {
            Log.e(TAG, "刷新用户数据时发生异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 刷新未读评论数量
     */
    public void refreshUnreadCount() {
        loadUnreadCommentCount();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 页面恢复时刷新用户数据和未读数量
        refreshUserDataAndUnreadCount();
    }
    
    /**
     * 通知子Fragment刷新数据
     * 用于在评论状态改变后同步各个Fragment的数据
     */
    public void notifyFragmentsRefresh() {
        if (pagerAdapter != null) {
            // 通知ViewPager中的Fragment刷新数据
            // 这里可以通过接口回调的方式通知各个Fragment刷新
            Log.d(TAG, "通知子Fragment刷新数据");
        }
    }
}