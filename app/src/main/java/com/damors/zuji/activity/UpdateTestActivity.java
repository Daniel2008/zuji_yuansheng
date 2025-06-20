package com.damors.zuji.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.damors.zuji.R;
import com.damors.zuji.dialog.AppUpdateDialog;
import com.damors.zuji.model.AppUpdateInfo;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.model.response.BaseResponse;

/**
 * 更新测试Activity
 * 用于开发阶段测试应用更新功能
 */
public class UpdateTestActivity extends AppCompatActivity {
    private static final String TAG = "UpdateTestActivity";
    
    private TextView tvCurrentVersion;
    private Button btnCheckUpdate;
    private Button btnTestDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_test);
        
        initViews();
        setupListeners();
        displayCurrentVersion();
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        tvCurrentVersion = findViewById(R.id.tv_current_version);
        btnCheckUpdate = findViewById(R.id.btn_check_update);
        btnTestDialog = findViewById(R.id.btn_test_dialog);
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 检查更新按钮
        btnCheckUpdate.setOnClickListener(v -> checkAppUpdate());
        
        // 测试对话框按钮
        btnTestDialog.setOnClickListener(v -> showTestDialog());
    }
    
    /**
     * 显示当前版本信息
     */
    private void displayCurrentVersion() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            tvCurrentVersion.setText("当前版本: v" + versionName + " (" + versionCode + ")");
        } catch (Exception e) {
            Log.e(TAG, "获取版本信息失败: " + e.getMessage(), e);
            tvCurrentVersion.setText("获取版本信息失败");
        }
    }
    
    /**
     * 检查应用更新
     */
    private void checkAppUpdate() {
        btnCheckUpdate.setEnabled(false);
        btnCheckUpdate.setText("检查中...");
        
        try {
            // 获取当前版本号
            int currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            
            // 调用API检查更新
            RetrofitApiService.getInstance(getApplicationContext()).checkAppUpdate(
                currentVersionCode,
                "android", // 添加平台参数
                new RetrofitApiService.SuccessCallback<BaseResponse<AppUpdateInfo>>() {
                    @Override
                    public void onSuccess(BaseResponse<AppUpdateInfo> response) {
                        runOnUiThread(() -> {
                            btnCheckUpdate.setEnabled(true);
                            btnCheckUpdate.setText("检查更新");
                            
                            AppUpdateInfo updateInfo = response.getData();
                            if (updateInfo != null && updateInfo.getVersionCode() > currentVersionCode) {
                                // 有新版本，显示更新对话框
                                showUpdateDialog(updateInfo);
                                Toast.makeText(UpdateTestActivity.this, "发现新版本: v" + updateInfo.getVersionName(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(UpdateTestActivity.this, "当前已是最新版本", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                },
                new RetrofitApiService.ErrorCallback() {
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnCheckUpdate.setEnabled(true);
                            btnCheckUpdate.setText("检查更新");
                            Toast.makeText(UpdateTestActivity.this, "检查更新失败: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "检查更新失败: " + error);
                        });
                    }
                }
            );
        } catch (Exception e) {
            btnCheckUpdate.setEnabled(true);
            btnCheckUpdate.setText("检查更新");
            Toast.makeText(this, "检查更新异常: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "检查更新异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 显示测试对话框
     */
    private void showTestDialog() {
        // 创建测试用的更新信息
        AppUpdateInfo testUpdateInfo = new AppUpdateInfo();
        testUpdateInfo.setVersionCode(2);
        testUpdateInfo.setVersionName("2.0.0");
        testUpdateInfo.setDownloadUrl("https://example.com/app-release.apk");
        testUpdateInfo.setUpdateContent("1. 新增在线更新功能\n2. 优化用户界面\n3. 修复已知问题\n4. 提升应用性能");
        testUpdateInfo.setForceUpdate(false);
        testUpdateInfo.setFileSize(25 * 1024 * 1024); // 25MB
        testUpdateInfo.setMd5("abcd1234567890efgh");
        testUpdateInfo.setReleaseTime("2024-01-15 10:30:00");
        
        showUpdateDialog(testUpdateInfo);
    }
    
    /**
     * 显示更新对话框
     * 
     * @param updateInfo 更新信息
     */
    private void showUpdateDialog(AppUpdateInfo updateInfo) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        AppUpdateDialog updateDialog = new AppUpdateDialog(this, updateInfo);
        updateDialog.setOnUpdateActionListener(new AppUpdateDialog.OnUpdateActionListener() {
            @Override
            public void onUpdateClicked() {
                Log.d(TAG, "用户选择立即更新");
                Toast.makeText(UpdateTestActivity.this, "开始下载更新", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onCancelClicked() {
                Log.d(TAG, "用户选择稍后更新");
                Toast.makeText(UpdateTestActivity.this, "稍后更新", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onDownloadCancelled() {
                Log.d(TAG, "用户取消下载");
                Toast.makeText(UpdateTestActivity.this, "取消下载", Toast.LENGTH_SHORT).show();
            }
        });
        
        updateDialog.show();
    }
}