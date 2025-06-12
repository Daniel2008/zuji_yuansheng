package com.damors.zuji.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.damors.zuji.model.AppUpdateInfo;
import com.damors.zuji.network.HutoolApiService;

import java.io.File;
import java.security.MessageDigest;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 应用更新管理器
 * 负责检查应用更新、下载APK文件、安装应用等功能
 */
public class AppUpdateManager {
    private static final String TAG = "AppUpdateManager";
    private static AppUpdateManager instance;
    
    private Context context;
    private HutoolApiService apiService;
    private DownloadManager downloadManager;
    private long downloadId = -1;
    private AppUpdateInfo currentUpdateInfo;
    private UpdateCheckCallback updateCheckCallback;
    private DownloadReceiver downloadReceiver;
    
    /**
     * 更新检查回调接口
     */
    public interface UpdateCheckCallback {
        /**
         * 发现新版本
         * @param updateInfo 更新信息
         */
        void onUpdateAvailable(AppUpdateInfo updateInfo);
        
        /**
         * 已是最新版本
         */
        void onNoUpdateAvailable();
        
        /**
         * 检查更新失败
         * @param error 错误信息
         */
        void onCheckUpdateError(String error);
    }
    
    /**
     * 下载进度回调接口
     */
    public interface DownloadProgressCallback {
        /**
         * 下载开始
         */
        void onDownloadStart();
        
        /**
         * 下载进度更新
         * @param progress 进度百分比 (0-100)
         */
        void onDownloadProgress(int progress);
        
        /**
         * 下载完成
         * @param filePath 下载文件路径
         */
        void onDownloadComplete(String filePath);
        
        /**
         * 下载失败
         * @param error 错误信息
         */
        void onDownloadError(String error);
    }
    
    /**
     * 私有构造函数
     */
    private AppUpdateManager(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = HutoolApiService.getInstance(context);
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized AppUpdateManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppUpdateManager(context);
        }
        return instance;
    }
    
    /**
     * 检查应用更新
     * @param callback 更新检查回调
     */
    public void checkUpdate(UpdateCheckCallback callback) {
        this.updateCheckCallback = callback;
        
        int currentVersionCode = getCurrentVersionCode();
        Log.d(TAG, "当前版本号: " + currentVersionCode);
        
        apiService.checkAppUpdate(currentVersionCode, 
            new HutoolApiService.SuccessCallback<AppUpdateInfo>() {
                @Override
                public void onSuccess(AppUpdateInfo updateInfo) {
                    Log.d(TAG, "检查更新成功: " + updateInfo.toString());
                    
                    if (updateInfo != null && updateInfo.getVersionCode() > currentVersionCode) {
                        // 发现新版本
                        currentUpdateInfo = updateInfo;
                        if (callback != null) {
                            callback.onUpdateAvailable(updateInfo);
                        }
                    } else {
                        // 已是最新版本
                        if (callback != null) {
                            callback.onNoUpdateAvailable();
                        }
                    }
                }
            },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "检查更新失败: " + errorMessage);
                    if (callback != null) {
                        callback.onCheckUpdateError(errorMessage);
                    }
                }
            }
        );
    }
    
    /**
     * 显示更新对话框
     * @param activity 当前Activity
     * @param updateInfo 更新信息
     */
    public void showUpdateDialog(Activity activity, AppUpdateInfo updateInfo) {
        if (activity == null || updateInfo == null) {
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("发现新版本 v" + updateInfo.getVersionName());
        
        StringBuilder message = new StringBuilder();
        if (updateInfo.getUpdateContent() != null && !updateInfo.getUpdateContent().isEmpty()) {
            message.append("更新内容:\n").append(updateInfo.getUpdateContent()).append("\n\n");
        }
        if (updateInfo.getFileSize() > 0) {
            message.append("安装包大小: ").append(updateInfo.getFormattedFileSize()).append("\n");
        }
        if (updateInfo.getReleaseTime() != null && !updateInfo.getReleaseTime().isEmpty()) {
            message.append("发布时间: ").append(updateInfo.getReleaseTime());
        }
        
        builder.setMessage(message.toString());
        
        // 立即更新按钮
        builder.setPositiveButton("立即更新", (dialog, which) -> {
            downloadUpdate(updateInfo, null);
        });
        
        // 如果不是强制更新，显示稍后更新按钮
        if (!updateInfo.isForceUpdate()) {
            builder.setNegativeButton("稍后更新", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.setCancelable(true);
        } else {
            builder.setCancelable(false);
        }
        
        builder.show();
    }
    
    /**
     * 下载更新
     * @param updateInfo 更新信息
     * @param progressCallback 下载进度回调
     */
    public void downloadUpdate(AppUpdateInfo updateInfo, DownloadProgressCallback progressCallback) {
        if (updateInfo == null || updateInfo.getDownloadUrl() == null || updateInfo.getDownloadUrl().isEmpty()) {
            Log.e(TAG, "下载链接为空");
            if (progressCallback != null) {
                progressCallback.onDownloadError("下载链接为空");
            }
            return;
        }
        
        // 检查存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Environment.isExternalStorageManager()) {
                Log.w(TAG, "没有存储权限，尝试使用应用私有目录");
            }
        }
        
        try {
            String fileName = "zuji_v" + updateInfo.getVersionName() + ".apk";
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateInfo.getDownloadUrl()));
            request.setTitle("足迹应用更新");
            request.setDescription("正在下载 v" + updateInfo.getVersionName());
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setAllowedOverRoaming(false);
            
            // 开始下载
            downloadId = downloadManager.enqueue(request);
            currentUpdateInfo = updateInfo;
            
            Log.d(TAG, "开始下载更新，下载ID: " + downloadId);
            
            if (progressCallback != null) {
                progressCallback.onDownloadStart();
            }
            
            // 注册下载完成广播接收器
            registerDownloadReceiver(progressCallback);
            
        } catch (Exception e) {
            Log.e(TAG, "下载更新失败", e);
            if (progressCallback != null) {
                progressCallback.onDownloadError("下载失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 注册下载完成广播接收器
     */
    private void registerDownloadReceiver(DownloadProgressCallback progressCallback) {
        if (downloadReceiver != null) {
            try {
                context.unregisterReceiver(downloadReceiver);
            } catch (Exception e) {
                Log.w(TAG, "取消注册下载接收器失败", e);
            }
        }
        
        downloadReceiver = new DownloadReceiver(progressCallback);
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        context.registerReceiver(downloadReceiver, filter);
    }
    
    /**
     * 下载完成广播接收器
     */
    private class DownloadReceiver extends BroadcastReceiver {
        private DownloadProgressCallback progressCallback;
        
        public DownloadReceiver(DownloadProgressCallback progressCallback) {
            this.progressCallback = progressCallback;
        }
        
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                Log.d(TAG, "下载完成，下载ID: " + id);
                
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(query);
                
                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        String localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        Log.d(TAG, "下载成功，文件路径: " + localUri);
                        
                        if (progressCallback != null) {
                            progressCallback.onDownloadComplete(localUri);
                        }
                        
                        // 验证文件并安装
                        installApk(localUri);
                        
                    } else {
                        String reason = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                        Log.e(TAG, "下载失败，原因: " + reason);
                        
                        if (progressCallback != null) {
                            progressCallback.onDownloadError("下载失败: " + reason);
                        }
                    }
                }
                cursor.close();
                
                // 取消注册广播接收器
                try {
                    context.unregisterReceiver(this);
                } catch (Exception e) {
                    Log.w(TAG, "取消注册下载接收器失败", e);
                }
            }
        }
    }
    
    /**
     * 安装APK
     * @param filePath 文件路径
     */
    private void installApk(String filePath) {
        try {
            File apkFile = new File(Uri.parse(filePath).getPath());
            
            if (!apkFile.exists()) {
                Log.e(TAG, "APK文件不存在: " + filePath);
                Toast.makeText(context, "安装文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 验证文件MD5（如果有提供）
            if (currentUpdateInfo != null && currentUpdateInfo.getMd5() != null && !currentUpdateInfo.getMd5().isEmpty()) {
                String fileMd5 = calculateMD5(apkFile);
                if (!currentUpdateInfo.getMd5().equalsIgnoreCase(fileMd5)) {
                    Log.e(TAG, "文件MD5校验失败，期望: " + currentUpdateInfo.getMd5() + ", 实际: " + fileMd5);
                    Toast.makeText(context, "文件校验失败，请重新下载", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0及以上使用FileProvider
                Uri apkUri = FileProvider.getUriForFile(context, 
                    "com.damors.zuji.fileprovider", apkFile);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // 检查是否允许安装未知来源应用
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.getPackageManager().canRequestPackageInstalls()) {
                    // 跳转到设置页面
                    Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    settingsIntent.setData(Uri.parse("package:" + context.getPackageName()));
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(settingsIntent);
                    Toast.makeText(context, "请允许安装未知来源应用，然后重新尝试", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            
            context.startActivity(intent);
            Log.d(TAG, "启动APK安装");
            
        } catch (Exception e) {
            Log.e(TAG, "安装APK失败", e);
            Toast.makeText(context, "安装失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 计算文件MD5值
     * @param file 文件
     * @return MD5值
     */
    private String calculateMD5(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            
            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            
            fis.close();
            
            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "计算MD5失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前应用版本号
     * @return 版本号
     */
    private int getCurrentVersionCode() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) packageInfo.getLongVersionCode();
            } else {
                return packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "获取版本号失败", e);
            return 1;
        }
    }
    
    /**
     * 获取当前应用版本名称
     * @return 版本名称
     */
    public String getCurrentVersionName() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "获取版本名称失败", e);
            return "1.0";
        }
    }
    
    /**
     * 取消下载
     */
    public void cancelDownload() {
        if (downloadId != -1) {
            downloadManager.remove(downloadId);
            downloadId = -1;
            Log.d(TAG, "取消下载");
        }
        
        if (downloadReceiver != null) {
            try {
                context.unregisterReceiver(downloadReceiver);
                downloadReceiver = null;
            } catch (Exception e) {
                Log.w(TAG, "取消注册下载接收器失败", e);
            }
        }
    }
    
    /**
     * 清理资源
     */
    public void destroy() {
        cancelDownload();
        updateCheckCallback = null;
        currentUpdateInfo = null;
        instance = null;
    }
}