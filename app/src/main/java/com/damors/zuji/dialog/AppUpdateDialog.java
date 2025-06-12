package com.damors.zuji.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.damors.zuji.R;
import com.damors.zuji.manager.AppUpdateManager;
import com.damors.zuji.model.AppUpdateInfo;

/**
 * 应用更新对话框
 * 提供更新信息展示、下载进度显示等功能
 */
public class AppUpdateDialog extends Dialog {
    private static final String TAG = "AppUpdateDialog";
    
    private Context context;
    private AppUpdateInfo updateInfo;
    private AppUpdateManager updateManager;
    private Handler mainHandler;
    
    // UI组件
    private TextView tvUpdateTitle;
    private TextView tvVersionName;
    private TextView tvFileSize;
    private TextView tvReleaseTime;
    private TextView tvUpdateContent;
    private LinearLayout layoutFileSize;
    private LinearLayout layoutReleaseTime;
    private LinearLayout layoutDownloadProgress;
    private LinearLayout layoutButtons;
    private TextView tvDownloadStatus;
    private ProgressBar progressDownload;
    private TextView tvDownloadProgress;
    private Button btnCancel;
    private Button btnUpdate;
    private Button btnCancelDownload;
    
    // 回调接口
    private OnUpdateActionListener onUpdateActionListener;
    
    /**
     * 更新操作监听器
     */
    public interface OnUpdateActionListener {
        /**
         * 用户点击立即更新
         */
        void onUpdateClicked();
        
        /**
         * 用户点击稍后更新
         */
        void onCancelClicked();
        
        /**
         * 用户取消下载
         */
        void onDownloadCancelled();
    }
    
    /**
     * 构造函数
     * 
     * @param context 上下文
     * @param updateInfo 更新信息
     */
    public AppUpdateDialog(@NonNull Context context, AppUpdateInfo updateInfo) {
        super(context, R.style.CustomDialogStyle);
        this.context = context;
        this.updateInfo = updateInfo;
        this.updateManager = AppUpdateManager.getInstance(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        initDialog();
        setupViews();
        setupData();
        setupListeners();
    }
    
    /**
     * 初始化对话框
     */
    private void initDialog() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_app_update, null);
        setContentView(view);
        
        // 设置对话框属性
        setCancelable(!updateInfo.isForceUpdate()); // 强制更新时不可取消
        setCanceledOnTouchOutside(false);
        
        // 设置对话框大小
        if (getWindow() != null) {
            getWindow().setLayout(
                (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
    
    /**
     * 初始化视图组件
     */
    private void setupViews() {
        tvUpdateTitle = findViewById(R.id.tv_update_title);
        tvVersionName = findViewById(R.id.tv_version_name);
        tvFileSize = findViewById(R.id.tv_file_size);
        tvReleaseTime = findViewById(R.id.tv_release_time);
        tvUpdateContent = findViewById(R.id.tv_update_content);
        layoutFileSize = findViewById(R.id.layout_file_size);
        layoutReleaseTime = findViewById(R.id.layout_release_time);
        layoutDownloadProgress = findViewById(R.id.layout_download_progress);
        layoutButtons = findViewById(R.id.layout_buttons);
        tvDownloadStatus = findViewById(R.id.tv_download_status);
        progressDownload = findViewById(R.id.progress_download);
        tvDownloadProgress = findViewById(R.id.tv_download_progress);
        btnCancel = findViewById(R.id.btn_cancel);
        btnUpdate = findViewById(R.id.btn_update);
        btnCancelDownload = findViewById(R.id.btn_cancel_download);
    }
    
    /**
     * 设置数据
     */
    private void setupData() {
        if (updateInfo == null) {
            return;
        }
        
        // 设置标题
        tvUpdateTitle.setText("发现新版本 v" + updateInfo.getVersionName());
        
        // 设置版本名称
        tvVersionName.setText("v" + updateInfo.getVersionName());
        
        // 设置文件大小
        if (updateInfo.getFileSize() > 0) {
            layoutFileSize.setVisibility(View.VISIBLE);
            tvFileSize.setText(updateInfo.getFormattedFileSize());
        } else {
            layoutFileSize.setVisibility(View.GONE);
        }
        
        // 设置发布时间
        if (updateInfo.getReleaseTime() != null && !updateInfo.getReleaseTime().isEmpty()) {
            layoutReleaseTime.setVisibility(View.VISIBLE);
            tvReleaseTime.setText(updateInfo.getReleaseTime());
        } else {
            layoutReleaseTime.setVisibility(View.GONE);
        }
        
        // 设置更新内容
        if (updateInfo.getUpdateContent() != null && !updateInfo.getUpdateContent().isEmpty()) {
            tvUpdateContent.setText(updateInfo.getUpdateContent());
        } else {
            tvUpdateContent.setText("本次更新包含性能优化和问题修复。");
        }
        
        // 强制更新时隐藏取消按钮
        if (updateInfo.isForceUpdate()) {
            btnCancel.setVisibility(View.GONE);
        }
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 立即更新按钮
        btnUpdate.setOnClickListener(v -> {
            if (onUpdateActionListener != null) {
                onUpdateActionListener.onUpdateClicked();
            }
            startDownload();
        });
        
        // 稍后更新按钮
        btnCancel.setOnClickListener(v -> {
            if (onUpdateActionListener != null) {
                onUpdateActionListener.onCancelClicked();
            }
            dismiss();
        });
        
        // 取消下载按钮
        btnCancelDownload.setOnClickListener(v -> {
            updateManager.cancelDownload();
            if (onUpdateActionListener != null) {
                onUpdateActionListener.onDownloadCancelled();
            }
            resetToInitialState();
        });
    }
    
    /**
     * 开始下载
     */
    private void startDownload() {
        // 切换到下载状态
        layoutButtons.setVisibility(View.GONE);
        layoutDownloadProgress.setVisibility(View.VISIBLE);
        btnCancelDownload.setVisibility(View.VISIBLE);
        
        // 设置对话框不可取消
        setCancelable(false);
        
        // 开始下载
        updateManager.downloadUpdate(updateInfo, new AppUpdateManager.DownloadProgressCallback() {
            @Override
            public void onDownloadStart() {
                mainHandler.post(() -> {
                    tvDownloadStatus.setText("正在下载...");
                    progressDownload.setProgress(0);
                    tvDownloadProgress.setText("0%");
                });
            }
            
            @Override
            public void onDownloadProgress(int progress) {
                mainHandler.post(() -> {
                    progressDownload.setProgress(progress);
                    tvDownloadProgress.setText(progress + "%");
                });
            }
            
            @Override
            public void onDownloadComplete(String filePath) {
                mainHandler.post(() -> {
                    tvDownloadStatus.setText("下载完成，准备安装...");
                    progressDownload.setProgress(100);
                    tvDownloadProgress.setText("100%");
                    
                    // 延迟关闭对话框
                    mainHandler.postDelayed(() -> {
                        dismiss();
                    }, 1000);
                });
            }
            
            @Override
            public void onDownloadError(String error) {
                mainHandler.post(() -> {
                    tvDownloadStatus.setText("下载失败: " + error);
                    
                    // 延迟重置状态
                    mainHandler.postDelayed(() -> {
                        resetToInitialState();
                    }, 2000);
                });
            }
        });
    }
    
    /**
     * 重置到初始状态
     */
    private void resetToInitialState() {
        layoutDownloadProgress.setVisibility(View.GONE);
        btnCancelDownload.setVisibility(View.GONE);
        layoutButtons.setVisibility(View.VISIBLE);
        
        // 恢复对话框可取消状态（如果不是强制更新）
        setCancelable(!updateInfo.isForceUpdate());
        
        // 重置进度
        progressDownload.setProgress(0);
        tvDownloadProgress.setText("0%");
        tvDownloadStatus.setText("正在下载...");
    }
    
    /**
     * 设置更新操作监听器
     * 
     * @param listener 监听器
     */
    public void setOnUpdateActionListener(OnUpdateActionListener listener) {
        this.onUpdateActionListener = listener;
    }
    
    /**
     * 更新下载进度
     * 
     * @param progress 进度百分比 (0-100)
     */
    public void updateDownloadProgress(int progress) {
        mainHandler.post(() -> {
            if (progressDownload != null) {
                progressDownload.setProgress(progress);
                tvDownloadProgress.setText(progress + "%");
            }
        });
    }
    
    /**
     * 设置下载状态文本
     * 
     * @param status 状态文本
     */
    public void setDownloadStatus(String status) {
        mainHandler.post(() -> {
            if (tvDownloadStatus != null) {
                tvDownloadStatus.setText(status);
            }
        });
    }
    
    @Override
    public void dismiss() {
        // 取消下载
        if (updateManager != null) {
            updateManager.cancelDownload();
        }
        super.dismiss();
    }
}