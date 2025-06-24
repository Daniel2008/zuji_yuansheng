package com.damors.zuji.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.damors.zuji.R;
import com.damors.zuji.activity.CommentListActivity;
import com.damors.zuji.adapter.MyCommentAdapter;
import com.damors.zuji.model.CommentModel;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.utils.LoadingDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的评论Fragment
 * 显示当前用户发表的所有评论
 */
public class MyCommentsFragment extends Fragment {
    
    private static final String TAG = "MyCommentsFragment";
    
    // UI组件
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private MyCommentAdapter adapter;
    
    // 数据相关
    private List<CommentModel> myCommentsList;
    private RetrofitApiService apiService;
    private LoadingDialog loadingDialog;
    
    // 分页参数
    private int currentPage = 1;
    private int pageSize = 10;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_comments, container, false);
        initViews(view);
        initData();
        loadMyComments();
        return view;
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        recyclerView = view.findViewById(R.id.recycler_view);
        tvEmpty = view.findViewById(R.id.tv_empty);
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myCommentsList = new ArrayList<>();
        adapter = new MyCommentAdapter(getContext(), myCommentsList);
        recyclerView.setAdapter(adapter);
        
        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMoreData = true;
            loadMyComments();
        });
        
        // 设置适配器点击事件
        setupAdapterClickListeners();
    }
    
    /**
     * 设置适配器点击事件监听器
     */
    private void setupAdapterClickListeners() {
        adapter.setOnCommentClickListener(new MyCommentAdapter.OnCommentClickListener() {
            @Override
            public void onCommentClick(CommentModel comment) {
                // 点击评论，跳转到对应动态的评论列表
                CommentListActivity.start(getContext(), comment.getMsgId(), "动态详情");
            }
            
            @Override
            public void onDeleteClick(CommentModel comment) {
                // 删除评论
                showDeleteConfirmDialog(comment);
            }
            
            @Override
            public void onDynamicClick(CommentModel comment) {
                // 点击动态内容，跳转到动态详情
                CommentListActivity.start(getContext(), comment.getMsgId(), "动态详情");
            }
        });
    }
    
    /**
     * 初始化数据
     */
    private void initData() {
        apiService = RetrofitApiService.getInstance(getContext());
        loadingDialog = new LoadingDialog(getContext());
    }
    
    /**
     * 加载我的评论数据
     */
    private void loadMyComments() {
        if (isLoading) return;
        
        isLoading = true;
        
        if (currentPage == 1 && !swipeRefreshLayout.isRefreshing()) {
            loadingDialog.show("加载中...");
        }
        
        // TODO: 调用API获取我的评论数据
        // 这里先模拟数据
        simulateLoadMyComments();
    }
    
    /**
     * 模拟加载我的评论数据
     */
    private void simulateLoadMyComments() {
        // 模拟网络延迟
        new android.os.Handler().postDelayed(() -> {
            List<CommentModel> mockData = createMockMyCommentsData();
            
            if (currentPage == 1) {
                myCommentsList.clear();
            }
            
            myCommentsList.addAll(mockData);
            adapter.notifyDataSetChanged();
            
            // 更新UI状态
            updateUIState();
            
            isLoading = false;
            swipeRefreshLayout.setRefreshing(false);
            loadingDialog.dismiss();
            
        }, 1000);
    }
    
    /**
     * 创建模拟我的评论数据
     */
    private List<CommentModel> createMockMyCommentsData() {
        List<CommentModel> mockList = new ArrayList<>();
        
        for (int i = 0; i < 6; i++) {
            CommentModel comment = new CommentModel();
            comment.setId(i + 1);
            comment.setMsgId(i + 1);
            comment.setContent("这是我发表的第" + (i + 1) + "条评论，分享我的看法和感受");
            comment.setUserId(1); // 当前用户ID
            comment.setUserName("我");
            comment.setUserAvatar("/avatar/me.jpg");
            comment.setCreateTime("2024-01-" + String.format("%02d", (25 + i)) + " 16:30:00");
            
            // 设置被评论的动态信息（用于显示是对哪条动态的评论）
            comment.setRemark("用户" + (i + 1) + "的动态：今天天气真不错，出去走走"); // 使用remark字段存储动态信息
            
            // 如果是回复评论，设置父评论信息
            if (i % 3 == 0) {
                comment.setParentId(i + 10); // 父评论ID
                comment.setParentUserName("用户" + (i + 1));
            }
            
            mockList.add(comment);
        }
        
        return mockList;
    }
    
    /**
     * 更新UI状态
     */
    private void updateUIState() {
        if (myCommentsList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(CommentModel comment) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("删除评论")
                .setMessage("确定要删除这条评论吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteComment(comment);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 删除评论
     */
    private void deleteComment(CommentModel comment) {
        // TODO: 调用API删除评论
        
        // 模拟删除成功
        int position = myCommentsList.indexOf(comment);
        if (position != -1) {
            myCommentsList.remove(position);
            adapter.notifyItemRemoved(position);
            Toast.makeText(getContext(), "评论删除成功", Toast.LENGTH_SHORT).show();
            
            // 更新UI状态
            updateUIState();
        }
        
        Log.d(TAG, "删除评论: " + comment.getId());
    }
    
    /**
     * 刷新数据
     */
    public void refreshData() {
        currentPage = 1;
        hasMoreData = true;
        loadMyComments();
    }
}