package com.damors.zuji.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.damors.zuji.R;
import com.damors.zuji.adapter.FootprintMessageAdapter;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.TrandsMsgModel;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.model.PageTrandsMsgModel;
import com.damors.zuji.utils.LoadingDialog;
import com.gyf.immersionbar.ImmersionBar;
import com.damors.zuji.activity.CommentListActivity;
import com.damors.zuji.activity.ImagePreviewActivity;
import com.damors.zuji.model.GuluFileModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 点赞管理Activity
 * 显示用户点赞的动态列表
 */
public class LikeManagementActivity extends BaseActivity implements FootprintMessageAdapter.OnItemClickListener {
    
    private static final String TAG = "LikeManagementActivity";
    
    // UI组件
    private ImageView ivBack;
    private TextView tvTitle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    
    // 数据相关
    private FootprintMessageAdapter adapter;
    private List<TrandsMsgModel> likeList;
    private RetrofitApiService apiService;
    private LoadingDialog loadingDialog;
    
    // 分页相关
    private int currentPage = 1;
    private int pageSize = 10;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_like_management);
        
        initViews();
        initData();
        setupRecyclerView();
        loadLikeList(true);
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        // 设置状态栏
        ImmersionBar.with(this)
                .statusBarColor(R.color.white)
                .statusBarDarkFont(true)
                .init();
        
        ivBack = findViewById(R.id.iv_back);
        tvTitle = findViewById(R.id.tv_title);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        recyclerView = findViewById(R.id.recycler_view);
        tvEmpty = findViewById(R.id.tv_empty);
        
        // 设置标题
        tvTitle.setText("点赞管理");
        
        // 设置返回按钮点击事件
        ivBack.setOnClickListener(v -> finish());
        
        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentPage = 1;
            loadLikeList(true);
        });
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }
    
    /**
     * 初始化数据
     */
    private void initData() {
        likeList = new ArrayList<>();
        apiService = RetrofitApiService.getInstance(this);
        loadingDialog = new LoadingDialog(this);
    }
    
    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        adapter = new FootprintMessageAdapter(this, likeList);
        adapter.setOnItemClickListener(this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        
        // 添加滚动监听，实现分页加载
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        loadLikeList(false);
                    }
                }
            }
        });
    }
    
    /**
     * 加载点赞列表
     * @param isRefresh 是否为刷新操作
     */
    private void loadLikeList(boolean isRefresh) {
        if (isLoading) return;
        
        isLoading = true;
        
        if (isRefresh) {
            currentPage = 1;
            hasMoreData = true;
            if (!swipeRefreshLayout.isRefreshing()) {
                loadingDialog.show();
            }
        }
        
        apiService.getUserLikes(
            currentPage,
            pageSize,
            new RetrofitApiService.SuccessCallback<BaseResponse<PageTrandsMsgModel>>() {
                @Override
                public void onSuccess(BaseResponse<PageTrandsMsgModel> response) {
                    handleLoadSuccess(response, isRefresh);
                }
            },
            new RetrofitApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    handleLoadError(errorMessage);
                }
            }
        );
    }
    
    /**
     * 处理加载成功
     */
    private void handleLoadSuccess(BaseResponse<PageTrandsMsgModel> response, boolean isRefresh) {
        isLoading = false;
        loadingDialog.dismiss();
        swipeRefreshLayout.setRefreshing(false);
        
        if (response.getCode() == 200 && response.getData() != null) {
            PageTrandsMsgModel data = response.getData();
            List<TrandsMsgModel> newList = data.getRecords();
            
            if (isRefresh) {
                likeList.clear();
            }
            
            if (newList != null && !newList.isEmpty()) {
                likeList.addAll(newList);
                currentPage++;
                hasMoreData = newList.size() >= pageSize;
            } else {
                hasMoreData = false;
            }
            
            adapter.notifyDataSetChanged();
            updateEmptyView();
            
            Log.d(TAG, "点赞列表加载成功，当前页: " + (currentPage - 1) + ", 数据量: " + (newList != null ? newList.size() : 0));
        } else {
            Toast.makeText(this, "加载失败: " + response.getMsg(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "点赞列表加载失败: " + response.getMsg());
            updateEmptyView();
        }
    }
    
    /**
     * 处理加载错误
     */
    private void handleLoadError(String errorMessage) {
        isLoading = false;
        loadingDialog.dismiss();
        swipeRefreshLayout.setRefreshing(false);
        
        Toast.makeText(this, "网络错误: " + errorMessage, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "点赞列表加载错误: " + errorMessage);
        
        updateEmptyView();
    }
    
    /**
     * 更新空视图显示
     */
    private void updateEmptyView() {
        if (likeList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("暂无点赞记录");
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    // FootprintMessageAdapter.OnItemClickListener 接口实现
    @Override
    public void onItemClick(TrandsMsgModel message, int position) {
        // 跳转到评论详情页面
        CommentListActivity.start(this, message.getId(),
                message.getTextContent() != null ? message.getTextContent() : "足迹详情");
    }
    
    @Override
    public void onLikeClick(TrandsMsgModel message, int position) {
        // 处理取消点赞
        handleLikeClick(message, position);
    }
    
    @Override
    public void onCommentClick(TrandsMsgModel message, int position) {
        // 跳转到评论详情页面
        CommentListActivity.start(this, message.getId(),
                message.getTextContent() != null ? message.getTextContent() : "足迹详情");
    }

    @Override
    public void onImageClick(TrandsMsgModel message, int position, int imageIndex, List<GuluFileModel> imageFiles) {
        // 跳转到图片预览页面
        Intent intent = new Intent(this, ImagePreviewActivity.class);
        intent.putExtra("imageFiles", (java.io.Serializable) imageFiles);
        intent.putExtra("currentIndex", imageIndex);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(TrandsMsgModel message, int position) {
        // 点赞管理页面不支持删除操作
        Toast.makeText(this, "点赞管理页面不支持删除操作", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onUserAvatarClick(TrandsMsgModel message, int position) {
        // 跳转到评论详情页面
        CommentListActivity.start(this, message.getId(),
                message.getTextContent() != null ? message.getTextContent() : "足迹详情");
    }
    
    @Override
    public void onLocationClick(TrandsMsgModel message, int position) {
        // 跳转到评论详情页面
        CommentListActivity.start(this, message.getId(),
                message.getTextContent() != null ? message.getTextContent() : "足迹详情");
    }
    
    /**
     * 处理点赞点击事件（取消点赞）
     */
    private void handleLikeClick(TrandsMsgModel message, int position) {
        // 切换点赞状态
        boolean newLikeStatus = !message.isHasLiked();
        int newLikeCount = message.getLikeCount() + (newLikeStatus ? 1 : -1);
        
        // 先更新适配器中的数据，提供即时反馈
        adapter.updateItemLikeStatus(position, newLikeStatus, newLikeCount);
        
        // 调用API更新点赞状态
        apiService.toggleLike(message.getId(),
            new RetrofitApiService.SuccessCallback<BaseResponse<org.json.JSONObject>>() {
                @Override
                public void onSuccess(BaseResponse<org.json.JSONObject> response) {
                    if (response.getCode() == 200) {
                        // API调用成功，显示提示信息
                        String toastMessage = newLikeStatus ? "已点赞" : "已取消点赞";
                        Toast.makeText(LikeManagementActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "点赞状态更新成功: " + response.toString());
                        
                        // 如果是取消点赞，从列表中移除该项
                        if (!newLikeStatus) {
                            likeList.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, likeList.size() - position);
                            updateEmptyView();
                        }
                    } else {
                        // API调用失败，恢复原状态
                        adapter.updateItemLikeStatus(position, message.isHasLiked(), message.getLikeCount());
                        Toast.makeText(LikeManagementActivity.this, "操作失败: " + response.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "点赞状态更新失败: " + response.getMsg());
                    }
                }
            },
            new RetrofitApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    // 网络错误，恢复原状态
                    adapter.updateItemLikeStatus(position, message.isHasLiked(), message.getLikeCount());
                    Toast.makeText(LikeManagementActivity.this, "网络错误: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "点赞状态更新错误: " + errorMessage);
                }
            }
        );
    }
}