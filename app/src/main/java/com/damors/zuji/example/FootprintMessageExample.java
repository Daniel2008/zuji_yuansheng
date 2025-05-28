package com.damors.zuji.example;

import android.content.Context;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.response.FootprintMessageResponse;
import com.damors.zuji.network.HutoolApiService;

import java.util.List;

/**
 * 足迹动态接口使用示例
 * 展示如何调用足迹动态列表接口
 */
public class FootprintMessageExample {
    
    private static final String TAG = "FootprintMessageExample";
    
    /**
     * 获取足迹动态列表的示例方法
     * @param context 上下文
     */
    public static void getFootprintMessageListExample(Context context) {
        // 获取HutoolApiService实例
        HutoolApiService apiService = HutoolApiService.getInstance(context);
        
        // 设置分页参数
        int pageNum = 1;    // 第一页
        int pageSize = 10;  // 每页10条
        
        Log.d(TAG, "开始获取足迹动态列表...");
        
        // 调用接口
        apiService.getFootprintMessages(
            pageNum,
            pageSize,
            new HutoolApiService.SuccessCallback<FootprintMessageResponse.Data>() {
                @Override
                public void onSuccess(FootprintMessageResponse.Data response) {
                    // 请求成功
                    Log.d(TAG, "获取足迹动态列表成功");
                    
                    if (response != null && response.getRecords() != null) {
                        List<FootprintMessage> messageList = response.getRecords();
                        Log.d(TAG, "获取到 " + messageList.size() + " 条足迹动态");
                        
                        // 处理数据
                        for (FootprintMessage message : messageList) {
                            Log.d(TAG, "足迹动态: " + message.getTextContent());
                            Log.d(TAG, "发布者: " + message.getCreateBy());
                            Log.d(TAG, "发布时间: " + message.getCreateTime());
                            Log.d(TAG, "位置: " + message.getLocaltionTitle());
                            
                            // 处理附件文件
                            if (message.getGuluFiles() != null && !message.getGuluFiles().isEmpty()) {
                                Log.d(TAG, "包含 " + message.getGuluFiles().size() + " 个附件");
                            }
                            
                            Log.d(TAG, "---");
                        }
                    } else {
                        Log.w(TAG, "响应数据为空");
                    }
                }
            },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    // 请求失败
                    Log.e(TAG, "获取足迹动态列表失败: " + errorMessage);
                }
            }
        );
    }
    
    /**
     * 分页加载足迹动态列表的示例方法
     * @param context 上下文
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页大小
     */
    public static void loadFootprintMessagePage(Context context, int pageNum, int pageSize) {
        HutoolApiService apiService = HutoolApiService.getInstance(context);
        
        Log.d(TAG, "加载第 " + pageNum + " 页足迹动态，每页 " + pageSize + " 条");
        
        apiService.getFootprintMessages(
            pageNum,
            pageSize,
            new HutoolApiService.SuccessCallback<FootprintMessageResponse.Data>() {
                @Override
                public void onSuccess(FootprintMessageResponse.Data response) {
                    if (response != null && response.getRecords() != null) {
                        List<FootprintMessage> messageList = response.getRecords();
                        
                        Log.d(TAG, "第 " + pageNum + " 页加载成功，获取到 " + messageList.size() + " 条数据");
                        
                        // 判断是否还有更多数据
                        boolean hasMoreData = messageList.size() >= pageSize;
                        Log.d(TAG, "是否还有更多数据: " + hasMoreData);
                        
                        // 如果还有更多数据，可以继续加载下一页
                        if (hasMoreData) {
                            Log.d(TAG, "可以加载第 " + (pageNum + 1) + " 页");
                        } else {
                            Log.d(TAG, "已加载完所有数据");
                        }
                    }
                }
            },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "加载第 " + pageNum + " 页失败: " + errorMessage);
                }
            }
        );
    }
}