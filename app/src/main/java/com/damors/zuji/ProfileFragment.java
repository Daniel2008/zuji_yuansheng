package com.damors.zuji;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.damors.zuji.viewmodel.FootprintViewModel;
import com.damors.zuji.utils.MapCacheManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 个人资料Fragment
 * 显示用户信息和应用统计数据
 */
public class ProfileFragment extends Fragment {

    private FootprintViewModel viewModel;
    private TextView textViewFootprintCount;
    private TextView textViewCityCount;
    private TextView textViewCacheSize;
    private Button buttonClearAll;
    private LinearLayout layoutDataManagement;
    private LinearLayout layoutSearch;
    private LinearLayout layoutMapCache;

    private ExecutorService executorService;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // 初始化视图组件
        textViewFootprintCount = view.findViewById(R.id.text_view_footprint_count);
        textViewCityCount = view.findViewById(R.id.text_view_city_count);
        textViewCacheSize = view.findViewById(R.id.text_view_cache_size);
        buttonClearAll = view.findViewById(R.id.button_clear_all);
        layoutDataManagement = view.findViewById(R.id.layout_data_management);
        layoutSearch = view.findViewById(R.id.layout_search);
        layoutMapCache = view.findViewById(R.id.layout_map_cache);

        
        // 初始化线程池和Handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 设置清除所有足迹的按钮点击事件
        buttonClearAll.setOnClickListener(v -> {
            // 显示确认对话框
            new AlertDialog.Builder(requireContext())
                .setTitle("清除所有足迹")
                .setMessage(getString(R.string.clear_all_footprints))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    viewModel.deleteAllFootprints();
                    // 检查Fragment是否仍然附加到Activity，避免IllegalStateException
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "已清除所有足迹记录", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
        });
        
        // 设置数据管理布局点击事件
        layoutDataManagement.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DataManagementActivity.class);
            startActivity(intent);
        });
        
        // 设置搜索布局点击事件
        layoutSearch.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SearchActivity.class);
            startActivity(intent);
        });
        
        // 设置地图缓存管理布局点击事件 - 直接打开离线地图管理
        layoutMapCache.setOnClickListener(v -> openOfflineMapManager());
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(FootprintViewModel.class);
        
        // 注释：已移除清除所有足迹布局点击事件，因为对应的布局元素不存在
        
        // 设置数据管理布局点击事件
        layoutDataManagement.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), DataManagementActivity.class);
            startActivity(intent);
        });
        
        // 设置搜索布局点击事件
        layoutSearch.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SearchActivity.class);
            startActivity(intent);
        });
        
        // 设置地图缓存管理布局点击事件 - 直接打开离线地图管理
        layoutMapCache.setOnClickListener(v -> openOfflineMapManager());
        
        // 观察足迹数据变化，更新统计信息
        // 注释：已移除本地足迹统计功能
        /*
        viewModel.getAllFootprints().observe(getViewLifecycleOwner(), footprints -> {
            if (footprints != null) {
                textViewFootprintCount.setText(String.format("%d", footprints.size()));
            } else {
                textViewFootprintCount.setText("0");
            }
        });
        */
        
        // 设置固定的足迹数量显示
        textViewFootprintCount.setText("0");
        
        // 观察城市数量变化，更新统计信息
        viewModel.getCityCount().observe(getViewLifecycleOwner(), cityCount -> {
            if (cityCount != null) {
                textViewCityCount.setText(String.format("%d", cityCount));
            } else {
                textViewCityCount.setText("0");
            }
        });
        
        // 刷新数据
        viewModel.refreshFootprints();
        
        // 加载缓存大小信息
        loadCacheSize();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * 加载地图缓存大小信息
     */
    private void loadCacheSize() {
        executorService.execute(() -> {
            String cacheSize = MapCacheManager.getFormattedCacheSize();
            int fileCount = MapCacheManager.getCacheFileCount();
            
            mainHandler.post(() -> {
                if (textViewCacheSize != null) {
                    textViewCacheSize.setText(String.format("缓存大小: %s (%d 个文件)", cacheSize, fileCount));
                }
            });
        });
    }
    
    /**
     * 显示地图缓存管理对话框
     */
    private void showMapCacheDialog() {
        executorService.execute(() -> {
            String cacheSize = MapCacheManager.getFormattedCacheSize();
            int fileCount = MapCacheManager.getCacheFileCount();
            String cachePath = MapCacheManager.getCachePath();
            boolean isAvailable = MapCacheManager.isCacheAvailable();
            
            mainHandler.post(() -> {
                String message = String.format(
                    "缓存大小: %s\n" +
                    "文件数量: %d 个\n" +
                    "缓存路径: %s\n" +
                    "缓存状态: %s",
                    cacheSize, fileCount, cachePath, isAvailable ? "可用" : "不可用"
                );
                
                new AlertDialog.Builder(requireContext())
                    .setTitle("地图缓存管理")
                    .setMessage(message)
                    .setPositiveButton("离线地图管理", (dialog, which) -> openOfflineMapManager())
                    .setNeutralButton("清理缓存", (dialog, which) -> clearMapCache())
                    .setNegativeButton("取消", null)
                    .show();
            });
        });
    }
    
    /**
     * 打开高德地图原生离线地图管理界面
     */
    private void openOfflineMapManager() {
        try {
            // 直接启动高德地图SDK提供的原生离线地图管理界面
            Intent intent = new Intent(requireContext(), com.amap.api.maps.offlinemap.OfflineMapActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("ProfileFragment", "启动高德地图离线地图管理界面失败", e);
            Toast.makeText(requireContext(), "启动离线地图管理失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 清理地图缓存
     */
    private void clearMapCache() {
        new AlertDialog.Builder(requireContext())
            .setTitle("清理地图缓存")
            .setMessage("确定要清理所有地图缓存吗？这将删除所有已下载的地图瓦片，下次使用时需要重新下载。")
            .setPositiveButton("确定", (dialog, which) -> {
                executorService.execute(() -> {
                    boolean success = MapCacheManager.clearCache();
                    
                    mainHandler.post(() -> {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(), 
                                success ? "地图缓存清理成功" : "地图缓存清理失败", 
                                Toast.LENGTH_SHORT).show();
                        }
                        // 重新加载缓存大小
                        loadCacheSize();
                    });
                });
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 清理过期的地图缓存
     */
    private void clearExpiredCache() {
        executorService.execute(() -> {
            // 清理7天前的缓存文件
            long maxAge = 7L * 24 * 60 * 60 * 1000; // 7天
            int deletedCount = MapCacheManager.clearExpiredCache(maxAge);
            
            mainHandler.post(() -> {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), 
                        String.format("已清理 %d 个过期缓存文件", deletedCount), 
                        Toast.LENGTH_SHORT).show();
                }
                // 重新加载缓存大小
                loadCacheSize();
            });
        });
    }
}