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
import com.damors.zuji.model.CommentModel;
import com.damors.zuji.model.FootprintMessage;
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
        // TODO: 调用API获取未读评论数量
        // 这里先模拟数据
        unreadCommentCount = 5;
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
        if (unreadCommentCount > 0) {
            unreadCommentCount--;
            updateUnreadCountBadge();
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
        // 页面恢复时刷新未读数量
        refreshUnreadCount();
    }
}