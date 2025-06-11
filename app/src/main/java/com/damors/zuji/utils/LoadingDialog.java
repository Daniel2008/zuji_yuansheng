package com.damors.zuji.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.damors.zuji.R;

/**
 * 加载对话框工具类
 * 用于在网络请求等耗时操作时显示加载提示
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
public class LoadingDialog {
    
    private Dialog dialog;
    private TextView loadingMessageTextView;
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     */
    public LoadingDialog(@NonNull Context context) {
        initDialog(context);
    }
    
    /**
     * 初始化对话框
     * 
     * @param context 上下文
     */
    private void initDialog(@NonNull Context context) {
        // 创建对话框
        dialog = new Dialog(context, R.style.LoadingDialogStyle);
        
        // 加载布局
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        loadingMessageTextView = dialogView.findViewById(R.id.text_loading_message);
        
        // 设置对话框属性
        dialog.setContentView(dialogView);
        dialog.setCancelable(false); // 不可取消
        dialog.setCanceledOnTouchOutside(false); // 点击外部不取消
    }
    
    /**
     * 显示加载对话框
     */
    public void show() {
        show("加载中...");
    }
    
    /**
     * 显示加载对话框并设置提示信息
     * 
     * @param message 提示信息
     */
    public void show(@Nullable String message) {
        if (dialog != null && !dialog.isShowing()) {
            if (message != null && loadingMessageTextView != null) {
                loadingMessageTextView.setText(message);
            }
            dialog.show();
        }
    }
    
    /**
     * 隐藏加载对话框
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    /**
     * 更新加载提示信息
     * 
     * @param message 新的提示信息
     */
    public void updateMessage(@Nullable String message) {
        if (loadingMessageTextView != null && message != null) {
            loadingMessageTextView.setText(message);
        }
    }
    
    /**
     * 检查对话框是否正在显示
     * 
     * @return 是否正在显示
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (dialog != null) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog = null;
        }
        loadingMessageTextView = null;
    }
}