package com.damors.zuji;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.damors.zuji.ImagePreviewActivity;
import com.damors.zuji.CommentListActivity;
import com.damors.zuji.adapter.FootprintMessageAdapter;
import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.GuluFile;
import com.damors.zuji.model.response.FootprintMessageResponse;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.network.HutoolApiService;
import com.damors.zuji.utils.LoadingDialog;
import com.damors.zuji.viewmodel.FootprintViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 历史记录Fragment
 * 显示用户的足迹历史记录，通过getFootprintMessages获取数据并以时间轴形式展示
 */
public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private static final int PAGE_SIZE = 10; // 每页显示数量
    
    private FootprintViewModel viewModel;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FootprintMessageAdapter adapter;
    private HutoolApiService apiService;
    private TextView emptyView;
    private LoadingDialog loadingDialog;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMoreData = true; // 是否还有更多数据
    private List<FootprintMessage> footprintMessages = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        
        // 初始化API服务
        apiService = HutoolApiService.getInstance(requireContext());
        
        // 初始化加载对话框
        loadingDialog = new LoadingDialog(requireContext());
        
        // 初始化下拉刷新布局
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
        
        // 设置下拉刷新监听器
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
        });
        
        // 初始化RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        
        // 添加滚动监听器实现上拉加载更多
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // 检查是否滚动到底部
                if (!isLoading && hasMoreData && dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    // 当滚动到倒数第3个item时开始加载下一页
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                        loadMoreData();
                    }
                }
            }
        });
        
        // 初始化适配器，使用足迹动态适配器
        adapter = new FootprintMessageAdapter(requireContext(), footprintMessages);
        recyclerView.setAdapter(adapter);
        
        // 设置点击事件
        adapter.setOnItemClickListener(new FootprintMessageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FootprintMessage message, int position) {
                // 移除跳转到发布足迹页面的功能
                // 如果需要其他处理逻辑，可以在这里添加
            }
            
            @Override
            public void onUserAvatarClick(FootprintMessage message, int position) {
                // 点击用户头像时的处理逻辑
                // 可以跳转到用户详情页或显示用户信息
            }
            
            @Override
            public void onLocationClick(FootprintMessage message, int position) {
                // 点击位置信息时的处理逻辑
                // 可以在地图上显示该位置或跳转到地图页面
                if (message.getLat() != 0.0 && message.getLng() != 0.0) {
                    // 这里可以添加跳转到地图的逻辑
                    // 例如：打开地图应用显示该位置
                }
            }
            
            @Override
            public void onLikeClick(FootprintMessage message, int position) {
                // 处理点赞点击事件
                handleLikeClick(message, position);
            }
            
            // 收藏功能已移除
            
            @Override
            public void onCommentClick(FootprintMessage message, int position) {
                // 处理评论点击事件
                handleCommentClick(message, position);
            }
            
            @Override
            public void onImageClick(FootprintMessage message, int position, int imageIndex, List<GuluFile> imageFiles) {
                // 处理图片点击事件，启动图片预览
                handleImageClick(message, position, imageIndex, imageFiles);
            }
        });
        
        // 初始化空视图
        emptyView = view.findViewById(R.id.text_view_empty);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化ViewModel（保留原有功能）
        viewModel = new ViewModelProvider(requireActivity()).get(FootprintViewModel.class);
        
        // 加载足迹动态数据
        loadFootprintMessages();
    }

    /**
     * 加载足迹动态数据
     */
    private void loadFootprintMessages() {
        if (isLoading) {
            return;
        }
        
        isLoading = true;
        Log.d(TAG, "开始加载足迹动态数据，页码: " + currentPage);
        
        apiService.getFootprintMessages(
            currentPage,
            PAGE_SIZE,
                data -> {
                    if (!isAdded() || getContext() == null) {
                        return;
                    }

                    isLoading = false;
                    handleFootprintMessagesSuccess(data);
                },
                errorMessage -> {
                    if (!isAdded() || getContext() == null) {
                        return;
                    }

                    isLoading = false;
                    handleFootprintMessagesError(errorMessage);
                },
                new HutoolApiService.LoadingCallback() {
                    @Override
                    public void onLoadingStart() {
                        if (currentPage == 1 && !swipeRefreshLayout.isRefreshing()) {
                            // 首次加载且不是下拉刷新时显示加载对话框
                            loadingDialog.show("正在加载足迹动态...");
                        }
                    }

                    @Override
                    public void onLoadingEnd() {
                        // 停止下拉刷新动画
                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        
                        if (currentPage == 1 && !swipeRefreshLayout.isRefreshing()) {
                            // 首次加载隐藏加载对话框
                            loadingDialog.dismiss();
                        }
                    }
                }
        );
    }
    
    /**
     * 处理足迹动态数据获取成功
     * @param data 足迹动态数据
     */
    private void handleFootprintMessagesSuccess(FootprintMessageResponse.Data data) {
        if (data != null && data.getRecords() != null) {
            List<FootprintMessage> newMessages = data.getRecords();
            
            if (currentPage == 1) {
                // 第一页，清空原有数据
                footprintMessages.clear();
            }
            
            // 添加新数据
            footprintMessages.addAll(newMessages);
            
            // 检查是否还有更多数据
            hasMoreData = newMessages.size() >= PAGE_SIZE;
            
            // 更新适配器
            adapter.notifyDataSetChanged();
            
            // 更新UI显示状态
            updateUIVisibility();
            
            Log.d(TAG, "成功加载 " + newMessages.size() + " 条足迹动态数据，当前页码: " + currentPage + "，是否有更多数据: " + hasMoreData);
        } else {
            Log.w(TAG, "获取到的足迹动态数据为空");
            hasMoreData = false;
            updateUIVisibility();
        }
    }
    
    /**
     * 处理足迹动态数据获取失败
     * @param errorMessage 错误信息
     */
    private void handleFootprintMessagesError(String errorMessage) {
        Log.e(TAG, "获取足迹动态数据失败: " + errorMessage);
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "获取足迹动态数据失败: " + errorMessage, Toast.LENGTH_SHORT).show();
        }
        
        // 如果是第一次加载失败，显示空视图
        if (currentPage == 1) {
            updateUIVisibility();
        }
    }
    
    /**
     * 更新UI显示状态
     */
    private void updateUIVisibility() {
        if (footprintMessages.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("暂无足迹动态数据");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
    
    /**
     * 刷新数据
     */
    public void refreshData() {
        currentPage = 1;
        hasMoreData = true;
        loadFootprintMessages();
    }
    
    /**
     * 加载更多数据
     */
    private void loadMoreData() {
        if (!hasMoreData || isLoading) {
            return;
        }
        
        currentPage++;
        Log.d(TAG, "开始加载更多数据，页码: " + currentPage);
        loadFootprintMessages();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时刷新数据
        refreshData();
    }
    
    /**
     * 单元测试方法
     * 测试Fragment是否正确初始化
     * @return 是否测试通过
     */
    public boolean testFragmentInitialization() {
        try {
            // 验证视图和适配器是否正确初始化
            return recyclerView != null && 
                   adapter != null && 
                   viewModel != null &&
                   apiService != null &&
                   emptyView != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 处理点赞点击事件
     * @param message 足迹动态
     * @param position 位置
     */
    private void handleLikeClick(FootprintMessage message, int position) {
        // 切换点赞状态
        boolean newLikeStatus = !message.getHasLiked();
        int newLikeCount = message.getLikeCount() + (newLikeStatus ? 1 : -1);
        
        // 先更新适配器中的数据，提供即时反馈
        adapter.updateItemLikeStatus(position, newLikeStatus, newLikeCount);
        
        // 调用API更新点赞状态
        apiService.toggleLike(message.getId(),
            new HutoolApiService.SuccessCallback<String>() {
                @Override
                public void onSuccess(String response) {
                    // API调用成功，显示提示信息
                    String toastMessage = newLikeStatus ? "已点赞" : "已取消点赞";
                    Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "点赞状态更新成功: " + response);
                }
            },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String error) {
                    // API调用失败，回滚UI状态
                    boolean originalStatus = !newLikeStatus;
                    int originalCount = message.getLikeCount() + (originalStatus ? 1 : -1);
                    adapter.updateItemLikeStatus(position, originalStatus, originalCount);
                    
                    Toast.makeText(requireContext(), "点赞操作失败，请重试", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "点赞状态更新失败: " + error);
                }
            }
        );
    }
    
    // 收藏功能已移除
    
    /**
     * 处理评论点击事件
     * @param message 足迹动态
     * @param position 位置
     */
    private void handleCommentClick(FootprintMessage message, int position) {
        // 跳转到评论详情页面
        CommentListActivity.start(requireContext(), message.getId(), 
            message.getTextContent() != null ? message.getTextContent() : "足迹详情");
    }
    
    /**
     * 单元测试方法
     * 测试足迹动态数据加载功能
     * @return 是否测试通过
     */
    public boolean testFootprintMessageLoading() {
        try {
            // 创建测试数据
            FootprintMessage testMessage = new FootprintMessage();
            testMessage.setId(1);
            testMessage.setTextContent("测试足迹动态");
            testMessage.setLat(39.9087);
            testMessage.setLng(116.3975);
            testMessage.setLocaltionTitle("北京天安门");
            testMessage.setCreateTime("2023-12-01 10:00:00");
            
            // 测试数据处理
            List<FootprintMessage> testMessages = new ArrayList<>();
            testMessages.add(testMessage);
            
            FootprintMessageResponse.Data testData = new FootprintMessageResponse.Data();
            testData.setRecords(testMessages);
            testData.setTotal(1);
            testData.setCurrent(1);
            testData.setSize(20);
            testData.setPages(1);
            
            // 模拟成功处理
            handleFootprintMessagesSuccess(testData);
            
            // 验证结果
            return footprintMessages.size() == 1 &&
                   footprintMessages.get(0).getId() == 1 &&
                   footprintMessages.get(0).getTextContent().equals("测试足迹动态");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 处理图片点击事件
     * @param message 足迹动态
     * @param position 位置
     * @param imageIndex 图片索引
     * @param imageFiles 图片文件列表
     */
    private void handleImageClick(FootprintMessage message, int position, int imageIndex, List<GuluFile> imageFiles) {
        if (imageFiles != null && imageIndex >= 0 && imageIndex < imageFiles.size()) {
            GuluFile imageFile = imageFiles.get(imageIndex);
            String originalPath = imageFile.getFilePath();
            String imageUrl = getFullImageUrl(originalPath);
            
            Log.d("HistoryFragment", "Image click - Original path: " + originalPath);
            Log.d("HistoryFragment", "Image click - Full URL: " + imageUrl);
            
            // 构建图片URL列表
            java.util.ArrayList<String> imageUrls = new java.util.ArrayList<>();
            for (GuluFile file : imageFiles) {
                imageUrls.add(getFullImageUrl(file.getFilePath()));
            }
            
            // 启动图片预览Activity
            Intent intent = ImagePreviewActivity.newIntent(requireContext(), imageUrls, imageIndex);
            startActivity(intent);
        } else {
            Log.w("HistoryFragment", "Invalid image click - imageFiles: " + imageFiles + ", imageIndex: " + imageIndex);
        }
    }
    
    /**
     * 构建完整的图片URL
     * @param imagePath 图片路径
     * @return 完整的图片URL
     */
    private String getFullImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.w("HistoryFragment", "Image path is null or empty");
            return "";
        }
        
        // 如果已经是完整的URL，直接返回
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            Log.d("HistoryFragment", "Image path is already a full URL: " + imagePath);
            return imagePath;
        }
        
        // 使用ApiConfig中的图片基础URL构建完整的图片URL
        String imageBaseUrl = ApiConfig.getImageBaseUrl();
        // 确保路径正确拼接
        if (!imagePath.startsWith("/")) {
            imagePath = "/" + imagePath;
        }
        String fullUrl = imageBaseUrl + imagePath;
        
        Log.d("HistoryFragment", "Building full URL - Image base: " + imageBaseUrl + ", Full URL: " + fullUrl);
        
        return fullUrl;
    }
    
    /**
     * 显示评论输入对话框
     * @param message 足迹动态消息
     */
    private void showCommentDialog(FootprintMessage message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("发表评论");
        
        // 创建输入框
        EditText editText = new EditText(requireContext());
        editText.setHint("请输入评论内容...");
        editText.setMaxLines(5);
        editText.setVerticalScrollBarEnabled(true);
        
        // 设置输入框的布局参数
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 20);
        editText.setLayoutParams(params);
        
        builder.setView(editText);
        
        builder.setPositiveButton("发表", (dialog, which) -> {
            String content = editText.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "评论内容不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 调用评论接口
            addComment(message.getId(), content);
        });
        
        builder.setNegativeButton("取消", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // 自动弹出键盘
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    /**
     * 添加评论
     * @param msgId 消息ID
     * @param content 评论内容
     */
    private void addComment(Integer msgId, String content) {
        apiService.addComment(msgId, content,
                response -> {
                    // 评论成功
                    Toast.makeText(requireContext(), "评论发表成功", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "评论发表成功: " + response);

                    // 这里可以刷新评论列表或更新UI
                    // TODO: 刷新当前页面的评论数据
                },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String error) {
                    // 评论失败
                    Toast.makeText(requireContext(), "评论发表失败，请重试", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "评论发表失败: " + error);
                }
            }
        );
    }
}