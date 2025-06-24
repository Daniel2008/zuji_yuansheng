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
import com.damors.zuji.model.PageCommentListModel;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.utils.LoadingDialog;

import org.json.JSONObject;

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
    private int totalPages = 0;
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
                updateCommentStatus(comment);
                // 点击评论，跳转到对应动态的评论列表
                CommentListActivity.start(getContext(), comment.getMsgId(), "动态详情");
            }
            
            @Override
            public void onReplyClick(CommentModel comment) {
                updateCommentStatus(comment);
                // 回复评论
                CommentListActivity.start(getContext(), comment.getMsgId(), "动态详情");
            }
            
            @Override
            public void onDynamicClick(CommentModel comment) {
                updateCommentStatus(comment);
                // 点击动态内容，跳转到动态详情
                CommentListActivity.start(getContext(), comment.getMsgId(), "动态详情");
            }
        });
    }

    private void updateCommentStatus(CommentModel comment) {
        apiService.updateCommentStatus(comment.getId(), new RetrofitApiService.SuccessCallback<BaseResponse<JSONObject>>() {
            @Override
            public void onSuccess(BaseResponse<JSONObject> response) {
                if (getActivity() == null) return;

                if (response.isSuccess()) {
                    markCommentAsRead(comment);
                    Log.d(TAG, "标记评论为已读成功");
                } else {
                    Log.e(TAG, "标记评论为已读失败: " + response.getMsg());
                }
            }
        }, new RetrofitApiService.ErrorCallback() {
            @Override
            public void onError(String error) {
                if (getActivity() == null) return;

                Log.e(TAG, "标记评论为已读失败: " + error);
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
        
        // 调用真实API加载数据
        loadReceivedCommentsFromApi();
    }
    
    /**
     * 从API加载收到的评论数据
     */
    private void loadReceivedCommentsFromApi() {
        if (apiService == null) {
            Log.e(TAG, "ApiService is null");
            return;
        }
        
        // 显示加载对话框（仅首次加载时）
        if (currentPage == 1 && loadingDialog != null) {
            loadingDialog.show();
        }

        apiService.getReplyComments(
                new RetrofitApiService.SuccessCallback<BaseResponse<PageCommentListModel>>() {
                    @Override
                    public void onSuccess(BaseResponse<PageCommentListModel> response) {
                        if (getActivity() == null) return;

                        // 停止刷新动画
                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }

                        // 隐藏加载对话框
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }

                        if (response.isSuccess() && response.getData() != null) {

                                PageCommentListModel data = response.getData();
                                List<CommentModel> comments = data.getRecords();

                                if (comments != null) {
                                    if (currentPage == 1) {
                                        receivedCommentsList.clear();
                                    }

                                    receivedCommentsList.addAll(comments);
                                    adapter.notifyDataSetChanged();
                                    // 更新分页信息
                                    totalPages = data.getPages();
                                    hasMoreData = data.hasMore();

                                    // 更新UI状态
                                    updateUIState();
                                }
                            } else {
                                Log.e(TAG, "API返回错误: " + response.getMsg());
                                showErrorMessage("加载失败: " + response.getMsg());
                            }


                        isLoading = false;
                    }
                },
                new RetrofitApiService.ErrorCallback() {
                    @Override
                    public void onError(String error) {
                        if (getActivity() == null) return;

                        Log.e(TAG, "网络请求异常");

                        // 停止刷新动画
                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }

                        // 隐藏加载对话框
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }

                        showErrorMessage("网络连接失败，请重试");
                        isLoading = false;
                    }
                }
        );

    }
    
    /**
     * 显示错误消息
     */
    private void showErrorMessage(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
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