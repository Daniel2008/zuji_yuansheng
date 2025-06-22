package com.damors.zuji.manager;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.network.ApiConfig;
import com.xuexiang.xupdate.XUpdate;
import com.xuexiang.xupdate.listener.OnUpdateFailureListener;
import com.xuexiang.xupdate.logs.ILogger;
import com.xuexiang.xupdate.utils.UpdateUtils;
import com.xuexiang.xupdate.entity.UpdateError;
import com.damors.zuji.network.OKHttpUpdateHttpService;

/**
 * 应用更新管理器
 * 使用XUpdate库进行应用更新管理
 */
public class AppUpdateManager {
    private static final String TAG = "AppUpdateManager";
    private static AppUpdateManager instance;
    
    private static Context context;
    private RetrofitApiService apiService;
    private boolean isXUpdateInitialized = false;

    
    /**
     * 私有构造函数
     */
    private AppUpdateManager(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitApiService.getInstance(context);
        initXUpdate();
    }
    
    /**
     * 初始化XUpdate
     */
    private void initXUpdate() {
        if (!isXUpdateInitialized) {
            XUpdate.get()
                    .debug(true)  // 开启调试模式
                    .isWifiOnly(false)  // 不限制只在WiFi下更新
                    .isGet(true)  // 使用GET请求检查版本
                    .isAutoMode(false)  // 非自动模式
                    .param("versionCode", UpdateUtils.getVersionCode(context))  // 设置版本号参数
                    .param("platform", "android")  // 设置平台参数
                    .setOnUpdateFailureListener(new OnUpdateFailureListener() {
                        @Override
                        public void onFailure(UpdateError error) {
                            Log.e(TAG, "XUpdate更新失败: " + error.toString());
                        }
                    })
                    .setILogger(new ILogger() {
                        @Override
                        public void log(int priority, String tag, String message, Throwable t) {
                            Log.e(TAG, "XUpdate更新失败: " + message.toString());
                        }
                    })
                    .setIUpdateHttpService(new OKHttpUpdateHttpService())  // 设置HTTP服务
                    .supportSilentInstall(true)  // 支持静默安装
                    .init((Application) context.getApplicationContext());
            isXUpdateInitialized = true;
            Log.d(TAG, "XUpdate初始化完成");
        }
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
     * 使用XUpdate开始更新（使用自带弹窗）
     */
    public void startUpdate() {
        // 确保XUpdate已初始化
        initXUpdate();
        
        // 构建完整的检查更新接口地址
        String updateUrl = ApiConfig.getBaseUrl() + ApiConfig.Endpoints.CHECK_APP_UPDATE;
        
        XUpdate.newBuild(context)
                .updateUrl(updateUrl)
                .updateParser(new CustomUpdateParser()) //设置自定义的版本更新解析器
                .update();
        
    }




    /**
     * 获取当前应用版本号
     * @return 版本号
     */
    public static int getCurrentVersionCode() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "获取版本号失败", e);
            return 1;
        }
    }
    
    /**
     * 获取当前应用版本名称
     * @return 版本名称
     */
    public static String getCurrentVersionName() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "获取版本名称失败", e);
            return "1.0";
        }
    }

}
