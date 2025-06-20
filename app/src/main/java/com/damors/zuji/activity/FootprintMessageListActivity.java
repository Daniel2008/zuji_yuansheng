package com.damors.zuji.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.damors.zuji.ImagePreviewActivity;
import com.damors.zuji.R;
import com.damors.zuji.adapter.FootprintMessageAdapter;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.GuluFile;
import com.damors.zuji.model.response.FootprintMessageResponse;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.model.response.BaseResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 足迹动态列表Activity
 * 展示用户发布的足迹动态信息
 */
public class FootprintMessageListActivity extends AppCompatActivity {
    
    private static final String TAG = "FootprintMessageList";
    
    private RecyclerView recyclerView;
    private FootprintMessageAdapter adapter;
    private List<FootprintMessage> messageList;
    private RetrofitApiService apiService;
    
    // 分页参数
    private int currentPage = 1;
    private int pageSize = 10;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_footprint_message_list);
        
        initViews();
        initData();
        loadFootprintMessages();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        messageList = new ArrayList<>();
        adapter = new FootprintMessageAdapter(this, messageList);
        recyclerView.setAdapter(adapter);
        
        // 设置点击事件监听器
        adapter.setOnItemClickListener(new FootprintMessageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FootprintMessage message, int position) {
                // 处理条目点击事件
            }
            
            @Override
            public void onUserAvatarClick(FootprintMessage message, int position) {
                // 处理用户头像点击事件
            }
            
            @Override
            public void onLocationClick(FootprintMessage message, int position) {
                // 处理位置点击事件
            }
            
            @Override
            public void onLikeClick(FootprintMessage message, int position) {
                // 处理点赞点击事件
            }
            
            // 收藏功能已移除
            
            @Override
            public void onCommentClick(FootprintMessage message, int position) {
                // 处理评论点击事件
            }
            
            @Override
            public void onImageClick(FootprintMessage message, int position, int imageIndex, List<GuluFile> imageFiles) {
                // 处理图片点击事件，启动图片预览
                handleImageClick(message, position, imageIndex, imageFiles);
            }
            
            @Override
            public void onDeleteClick(FootprintMessage message, int position) {
                // 处理删除点击事件
                handleDeleteClick(message, position);
            }
        });
        
        // 设置滚动监听，实现分页加载
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMoreData();
                    }
                }
            }
        });
    }
    
    private void initData() {
        apiService = RetrofitApiService.getInstance(getApplicationContext());
    }
    
    /**
     * 加载足迹动态列表
     */
    private void loadFootprintMessages() {
        if (isLoading) {
            return;
        }
        
        isLoading = true;
        Log.d(TAG, "开始加载足迹动态列表，页码: " + currentPage);
        
        apiService.getFootprintMessages(
            currentPage,
            pageSize,
            new RetrofitApiService.SuccessCallback<BaseResponse<FootprintMessageResponse.Data>>() {
                @Override
                public void onSuccess(BaseResponse<FootprintMessageResponse.Data> response) {
                    isLoading = false;
                    
                    if (response.getCode() == 200 && response.getData() != null) {
                        FootprintMessageResponse.Data data = response.getData();
                        if (data.getRecords() != null) {
                            List<FootprintMessage> newMessages = data.getRecords();
                            Log.d(TAG, "获取到 " + newMessages.size() + " 条足迹动态");
                            
                            if (currentPage == 1) {
                                // 第一页，清空现有数据
                                messageList.clear();
                            }
                            
                            messageList.addAll(newMessages);
                            adapter.notifyDataSetChanged();
                            
                            // 检查是否还有更多数据
                            hasMoreData = newMessages.size() >= pageSize;
                            
                            Log.d(TAG, "当前共有 " + messageList.size() + " 条足迹动态，是否还有更多: " + hasMoreData);
                        } else {
                            Log.w(TAG, "响应数据为空");
                            hasMoreData = false;
                        }
                    } else {
                        String msg = response.getMsg() != null ? response.getMsg() : "加载失败";
                        Log.e(TAG, "加载足迹动态列表失败: " + msg);
                        Toast.makeText(FootprintMessageListActivity.this, 
                            "加载足迹动态失败: " + msg, 
                            Toast.LENGTH_SHORT).show();
                        hasMoreData = false;
                    }
                }
            },
            new RetrofitApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    isLoading = false;
                    Log.e(TAG, "加载足迹动态列表失败: " + errorMessage);
                    
                    Toast.makeText(FootprintMessageListActivity.this, 
                        "加载足迹动态失败: " + errorMessage, 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    /**
     * 加载更多数据
     */
    private void loadMoreData() {
        if (!hasMoreData || isLoading) {
            return;
        }
        
        currentPage++;
        loadFootprintMessages();
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
            
            Log.d("FootprintMessageList", "Image click - Original path: " + originalPath);
            Log.d("FootprintMessageList", "Image click - Full URL: " + imageUrl);
            
            // 构建图片URL列表
            java.util.ArrayList<String> imageUrls = new java.util.ArrayList<>();
            for (GuluFile file : imageFiles) {
                imageUrls.add(getFullImageUrl(file.getFilePath()));
            }
            
            // 启动图片预览Activity
            Intent intent = ImagePreviewActivity.newIntent(this, imageUrls, imageIndex);
            startActivity(intent);
        } else {
            Log.w("FootprintMessageList", "Invalid image click - imageFiles: " + imageFiles + ", imageIndex: " + imageIndex);
        }
    }
    
    /**
     * 构建完整的图片URL
     * @param imagePath 图片路径
     * @return 完整的图片URL
     */
    private String getFullImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.w("FootprintMessageList", "Image path is null or empty");
            return "";
        }
        
        // 如果已经是完整的URL，直接返回
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            Log.d("FootprintMessageList", "Image path is already a full URL: " + imagePath);
            return imagePath;
        }
        
        // 使用ApiConfig中的图片基础URL构建完整的图片URL
        String imageBaseUrl = ApiConfig.getImageBaseUrl();
        String fullUrl = imageBaseUrl + imagePath;
        
        Log.d("FootprintMessageList", "Building full URL - Image base: " + imageBaseUrl + ", Full URL: " + fullUrl);
        
        return fullUrl;
    }
    
    /**
     * 处理删除点击事件
     * @param message 足迹动态
     * @param position 位置
     */
    private void handleDeleteClick(FootprintMessage message, int position) {
        // 显示确认删除对话框
        new AlertDialog.Builder(this)
            .setTitle("删除足迹")
            .setMessage("确定要删除这条足迹吗？删除后无法恢复。")
            .setPositiveButton("删除", (dialog, which) -> {
                // 执行删除操作
                deleteFootprint(message, position);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 删除足迹
     * @param message 足迹动态
     * @param position 位置
     */
    private void deleteFootprint(FootprintMessage message, int position) {
        // 调用API删除足迹
        apiService.deleteFootprint(message.getId(),
            new RetrofitApiService.SuccessCallback<BaseResponse<String>>() {
                @Override
                public void onSuccess(BaseResponse<String> response) {
                    if (response.getCode() == 200) {
                        // 删除成功，从列表中移除该项
                        messageList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, messageList.size());
                        
                        // 显示成功提示
                        Toast.makeText(FootprintMessageListActivity.this, "足迹删除成功", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "足迹删除成功: " + response.toString());
                    } else {
                        // 删除失败
                        String msg = response.getMsg() != null ? response.getMsg() : "删除失败";
                        Toast.makeText(FootprintMessageListActivity.this, msg, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "足迹删除失败: " + msg);
                    }
                }
            },
            new RetrofitApiService.ErrorCallback() {
                @Override
                public void onError(String error) {
                    Toast.makeText(FootprintMessageListActivity.this, "网络错误: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "删除足迹网络错误: " + error);
                }
            }
        );
    }
}