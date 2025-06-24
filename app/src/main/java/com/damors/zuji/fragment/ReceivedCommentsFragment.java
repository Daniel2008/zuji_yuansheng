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
import com.damors.zuji.adapter.ReceivedCommentAdapter;
import com.damors.zuji.model.CommentModel;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.utils.LoadingDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * 收到的评论Fragment
 * 显示其他用户对我的评论
 */
public class ReceivedCommentsFragment extends Fragment {
    
    private static final String TAG = "ReceivedCommentsFragment";
    
    // UI组件
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private ReceivedCommentAdapter adapter;
    
    // 数据相关
    private List<CommentModel> receivedCommentsList;
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
        View view = inflater.inflate(R.layout.fragment_received_comments, container, false);
        initViews(view);
        initData();
        loadReceivedComments();
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
        receivedCommentsList = new ArrayList<>();
        adapter = new ReceivedCommentAdapter(getContext(), receivedCommentsList);
        recyclerView.setAdapter(adapter);
        
        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMoreData = true;
            loadReceivedComments();
        });
        
        // 设置适配器点击事件
        setupAdapterClickListeners();
    }
    
    /**
     * 设置适配器点击事件监听器
     */
    private void setupAdapterClickListeners() {
        adapter.setOnCommentClickListener(new ReceivedCommentAdapter.OnCommentClickListener() {
            @Override
            public void onCommentClick(CommentModel comment) {
                // 点击评论，跳转到对应动态的评论列表
                CommentListActivity.start(getContext(), comment.getMsgId(), "动态详情");
                
                // 标记为已读
                markCommentAsRead(comment);
            }
            
            @Override
            public void onReplyClick(CommentModel comment) {
                // 回复评论
                CommentListActivity.start(getContext(), comment.getMsgId(), "动态详情");
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
     * 加载收到的评论数据
     */
    private void loadReceivedComments() {
        if (isLoading) return;
        
        isLoading = true;
        
        if (currentPage == 1 && !swipeRefreshLayout.isRefreshing()) {
            loadingDialog.show("加载中...");
        }
        
        // TODO: 调用API获取收到的评论数据
        // 这里先模拟数据
        simulateLoadReceivedComments();
    }
    
    /**
     * 模拟加载收到的评论数据
     */
    private void simulateLoadReceivedComments() {
        // 模拟网络延迟
        new android.os.Handler().postDelayed(() -> {
            List<CommentModel> mockData = createMockReceivedCommentsData();
            
            if (currentPage == 1) {
                receivedCommentsList.clear();
            }
            
            receivedCommentsList.addAll(mockData);
            adapter.notifyDataSetChanged();
            
            // 更新UI状态
            updateUIState();
            
            isLoading = false;
            swipeRefreshLayout.setRefreshing(false);
            loadingDialog.dismiss();
            
        }, 1000);
    }
    
    /**
     * 创建模拟收到的评论数据
     */
    private List<CommentModel> createMockReceivedCommentsData() {
        List<CommentModel> mockList = new ArrayList<>();
        
        for (int i = 0; i < 8; i++) {
            CommentModel comment = new CommentModel();
            comment.setId(i + 1);
            comment.setMsgId(i + 1);
            comment.setContent("这是用户" + (i + 1) + "对我动态的评论内容，很有意思的分享！");
            comment.setUserId(100 + i);
            comment.setUserName("用户" + (i + 1));
            comment.setUserAvatar("/avatar/user" + (i + 1) + ".jpg");
            comment.setCreateTime("2024-01-" + String.format("%02d", (20 + i)) + " 14:30:00");
            
            // 设置动态相关信息（用于显示是对哪条动态的评论）
            comment.setRemark("我的第" + (i + 1) + "条动态内容"); // 使用remark字段存储动态内容
            
            // 模拟未读状态
            if (i < 3) {
                comment.setDelFlag("0"); // 0表示未读，1表示已读
            } else {
                comment.setDelFlag("1");
            }
            
            mockList.add(comment);
        }
        
        return mockList;
    }
    
    /**
     * 更新UI状态
     */
    private void updateUIState() {
        if (receivedCommentsList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 标记评论为已读
     */
    private void markCommentAsRead(CommentModel comment) {
        if ("0".equals(comment.getDelFlag())) { // 如果是未读状态
            comment.setDelFlag("1"); // 标记为已读
            
            // 通知适配器更新
            int position = receivedCommentsList.indexOf(comment);
            if (position != -1) {
                adapter.notifyItemChanged(position);
            }
            
            // TODO: 调用API标记为已读
            Log.d(TAG, "标记评论为已读: " + comment.getId());
        }
    }
    
    /**
     * 刷新数据
     */
    public void refreshData() {
        currentPage = 1;
        hasMoreData = true;
        loadReceivedComments();
    }
    
    /**
     * 获取未读评论数量
     */
    public int getUnreadCount() {
        int count = 0;
        for (CommentModel comment : receivedCommentsList) {
            if ("0".equals(comment.getDelFlag())) {
                count++;
            }
        }
        return count;
    }
}