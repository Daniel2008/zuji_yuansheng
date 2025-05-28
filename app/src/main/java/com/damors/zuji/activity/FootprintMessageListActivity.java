package com.damors.zuji.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.damors.zuji.R;
import com.damors.zuji.adapter.FootprintMessageAdapter;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.response.FootprintMessageResponse;
import com.damors.zuji.network.HutoolApiService;

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
    private HutoolApiService apiService;
    
    // 分页参数
    private int currentPage = 1;
    private int pageSize = 10;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        apiService = HutoolApiService.getInstance(this);
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
            new HutoolApiService.SuccessCallback<FootprintMessageResponse.Data>() {
                @Override
                public void onSuccess(FootprintMessageResponse.Data response) {
                    isLoading = false;
                    
                    if (response != null && response.getRecords() != null) {
                        List<FootprintMessage> newMessages = response.getRecords();
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
                }
            },
            new HutoolApiService.ErrorCallback() {
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
}