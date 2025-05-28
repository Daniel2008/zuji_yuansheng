package com.damors.zuji;

import android.content.Intent;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.damors.zuji.adapter.FootprintMessageAdapter;
import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.response.FootprintMessageResponse;
import com.damors.zuji.network.HutoolApiService;
import com.damors.zuji.viewmodel.FootprintViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 历史记录Fragment
 * 显示用户的足迹历史记录，通过getFootprintMessages获取数据并以时间轴形式展示
 */
public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";
    private static final int PAGE_SIZE = 20; // 每页显示数量
    
    private FootprintViewModel viewModel;
    private RecyclerView recyclerView;
    private FootprintMessageAdapter adapter;
    private HutoolApiService apiService;
    private TextView emptyView;
    private int currentPage = 1;
    private boolean isLoading = false;
    private List<FootprintMessage> footprintMessages = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        
        // 初始化API服务
        apiService = HutoolApiService.getInstance(requireContext());
        
        // 初始化RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
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
            
            @Override
            public void onFavoriteClick(FootprintMessage message, int position) {
                // 处理收藏点击事件
                handleFavoriteClick(message, position);
            }
            
            @Override
            public void onCommentClick(FootprintMessage message, int position) {
                // 处理评论点击事件
                handleCommentClick(message, position);
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
            
            // 更新适配器
            adapter.notifyDataSetChanged();
            
            // 更新UI显示状态
            updateUIVisibility();
            
            Log.d(TAG, "成功加载 " + newMessages.size() + " 条足迹动态数据");
        } else {
            Log.w(TAG, "获取到的足迹动态数据为空");
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
        boolean newLikeStatus = !message.isLiked();
        int newLikeCount = message.getLikeCount() + (newLikeStatus ? 1 : -1);
        
        // 更新适配器中的数据
        adapter.updateItemLikeStatus(position, newLikeStatus, newLikeCount);
        
        // 这里可以添加网络请求，将点赞状态同步到服务器
        // 例如：调用API更新点赞状态
        // apiService.updateLikeStatus(message.getId(), newLikeStatus);
        
        // 显示提示信息
        String toastMessage = newLikeStatus ? "已点赞" : "已取消点赞";
        Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 处理收藏点击事件
     * @param message 足迹动态
     * @param position 位置
     */
    private void handleFavoriteClick(FootprintMessage message, int position) {
        // 切换收藏状态
        boolean newFavoriteStatus = !message.isFavorited();
        int newFavoriteCount = message.getFavoriteCount() + (newFavoriteStatus ? 1 : -1);
        
        // 更新适配器中的数据
        adapter.updateItemFavoriteStatus(position, newFavoriteStatus, newFavoriteCount);
        
        // 这里可以添加网络请求，将收藏状态同步到服务器
        // 例如：调用API更新收藏状态
        // apiService.updateFavoriteStatus(message.getId(), newFavoriteStatus);
        
        // 显示提示信息
        String toastMessage = newFavoriteStatus ? "已收藏" : "已取消收藏";
        Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 处理评论点击事件
     * @param message 足迹动态
     * @param position 位置
     */
    private void handleCommentClick(FootprintMessage message, int position) {
        // 这里可以打开评论页面或评论对话框
        // 例如：跳转到评论详情页面
        // Intent intent = new Intent(requireContext(), CommentActivity.class);
        // intent.putExtra("footprint_message_id", message.getId());
        // startActivity(intent);
        
        // 临时显示提示信息
        Toast.makeText(requireContext(), "评论功能开发中", Toast.LENGTH_SHORT).show();
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
}