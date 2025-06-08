package com.damors.zuji;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.damors.zuji.utils.ExportUtil;
import com.damors.zuji.utils.ImportUtil;
import com.damors.zuji.viewmodel.FootprintViewModel;

/**
 * 数据管理页面
 * 提供数据导入、导出和搜索功能
 */
public class DataManagementActivity extends AppCompatActivity {

    private Button exportButton;
    private Button importButton;
    private Button searchButton;
    private Button backupButton;
    private FootprintViewModel viewModel;
    
    // 文件选择器启动器
    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importDataFromUri(uri);
                }
            });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_data_management);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(FootprintViewModel.class);
        
        // 初始化视图
        initViews();
        
        // 设置按钮点击事件
        setupClickListeners();
        
        // 初始化数据观察器
        initDataObserver();
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews() {
        exportButton = findViewById(R.id.button_export);
        importButton = findViewById(R.id.button_import);
        searchButton = findViewById(R.id.button_search);
        backupButton = findViewById(R.id.button_backup);
    }
    
    /**
     * 设置按钮点击事件
     */
    private void setupClickListeners() {
        // 导出按钮
        exportButton.setOnClickListener(v -> exportData());
        
        // 导入按钮
        importButton.setOnClickListener(v -> showImportDialog());
        
        // 搜索按钮
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });
        
        // 备份按钮
        backupButton.setOnClickListener(v -> showBackupOptions());
    }
    
    /**
     * 导出数据
     */
    private void exportData() {
        viewModel.getAllFootprints().observe(this, footprints -> {
            if (footprints != null && !footprints.isEmpty()) {
                // 导出为JSON文件
                Uri fileUri = ExportUtil.exportFootprintsToJson(this, footprints);
                if (fileUri != null) {
                    showExportSuccessDialog(fileUri);
                } else {
                    Toast.makeText(this, "导出失败，请重试", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "没有足迹数据可导出", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 显示导出成功对话框
     * @param fileUri 导出文件的URI
     */
    private void showExportSuccessDialog(Uri fileUri) {
        new AlertDialog.Builder(this)
                .setTitle("导出成功")
                .setMessage("足迹数据已成功导出到文件：" + fileUri.getPath())
                .setPositiveButton("分享", (dialog, which) -> shareExportedFile(fileUri))
                .setNegativeButton("确定", null)
                .show();
    }
    
    /**
     * 分享导出的文件
     * @param fileUri 文件URI
     */
    private void shareExportedFile(Uri fileUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        startActivity(Intent.createChooser(shareIntent, "分享足迹数据"));
    }
    
    /**
     * 显示导入对话框
     */
    private void showImportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("导入数据")
                .setMessage("导入将会添加新的足迹记录，不会覆盖现有数据。确定要导入吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 启动文件选择器
                    filePickerLauncher.launch("application/json");
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 从URI导入数据
     * @param uri 文件URI
     */
    private void importDataFromUri(Uri uri) {
        int importedCount = ImportUtil.importFootprintsFromJson(this, uri, viewModel);
        if (importedCount > 0) {
            Toast.makeText(this, "成功导入 " + importedCount + " 条足迹记录", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "导入失败，请检查文件格式", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示备份选项
     */
    private void showBackupOptions() {
        String[] options = {"导出到本地", "导入从本地"};
        
        new AlertDialog.Builder(this)
                .setTitle("备份选项")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 导出到本地
                        exportData();
                    } else if (which == 1) {
                        // 导入从本地
                        showImportDialog();
                    }
                })
                .show();
    }
    
    /**
     * 单元测试方法
     * 测试导入导出功能是否正确
     * @return 是否测试通过
     */
    public boolean testImportExport() {
        try {
            // 测试导出工具类
            boolean exportTest = ExportUtil.testJsonConversion();
            
            // 测试导入工具类
            boolean importTest = ImportUtil.testJsonToFootprintConversion();
            
            return exportTest && importTest;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 更新足迹数量显示
     * 注释：已移除本地足迹数据管理功能，此方法仅用于兼容性
     * @param count 足迹数量
     */
    private void updateFootprintCount(int count) {
        // 注释：原本用于更新UI中的足迹数量显示
        // 由于已移除本地存储功能，此方法现在为空实现
        // 保留此方法以避免编译错误
    }
    
    /**
     * 初始化数据观察
     * 注释：已移除本地足迹数据管理功能
     */
    private void initDataObserver() {
        // 观察足迹数据变化
        // 注释：已移除本地足迹数据管理功能
        /*
        viewModel.getAllFootprints().observe(this, footprints -> {
            if (footprints != null) {
                updateFootprintCount(footprints.size());
                // 可以在这里更新其他统计信息
            }
        });
        */
        
        // 显示本地存储功能已移除的提示
        updateFootprintCount(0);
        Toast.makeText(this, "本地存储功能已移除", Toast.LENGTH_SHORT).show();
    }
}