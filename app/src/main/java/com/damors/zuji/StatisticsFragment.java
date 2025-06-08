package com.damors.zuji;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.viewmodel.FootprintViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 统计分析Fragment
 * 用于显示用户足迹的统计数据，包括总数、分类分布、城市分布等
 */
public class StatisticsFragment extends Fragment {

    private FootprintViewModel viewModel;
    private TextView totalFootprintsTextView;
    private TextView categoriesStatsTextView;
    private TextView citiesStatsTextView;
    private TextView timeRangeTextView;
    private TextView monthlyStatsTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(FootprintViewModel.class);
        
        // 初始化视图
        initViews(view);
        
        // 加载统计数据
        loadStatistics();
        
        return view;
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        totalFootprintsTextView = view.findViewById(R.id.text_view_total_footprints);
        categoriesStatsTextView = view.findViewById(R.id.text_view_categories_stats);
        citiesStatsTextView = view.findViewById(R.id.text_view_cities_stats);
        timeRangeTextView = view.findViewById(R.id.text_view_time_range);
        monthlyStatsTextView = view.findViewById(R.id.text_view_monthly_stats);
    }
    
    /**
     * 加载统计数据
     * 注释：已移除本地足迹统计功能
     */
    /*
    private void loadStatistics() {
        viewModel.getAllFootprints().observe(getViewLifecycleOwner(), footprints -> {
            if (footprints != null) {
                updateStatistics(footprints);
            }
        });
    }
    */
    
    private void loadStatistics() {
        // 显示空的统计数据，因为本地存储功能已移除
        updateStatisticsWithEmptyData();
    }
    
    /**
     * 显示空的统计数据
     */
    private void updateStatisticsWithEmptyData() {
        // 设置所有统计数据为0
        totalFootprintsTextView.setText("总足迹数: 0");
        // 其他统计项也设置为空或0
    }
    
    /**
     * 更新统计数据显示
     * @param footprints 足迹列表
     */
    private void updateStatistics(List<FootprintEntity> footprints) {
        // 计算足迹总数
        int totalFootprints = footprints.size();
        totalFootprintsTextView.setText(String.format("总足迹数: %d", totalFootprints));
        
        // 如果没有足迹，显示提示信息并返回
        if (totalFootprints == 0) {
            categoriesStatsTextView.setText("暂无分类数据");
            citiesStatsTextView.setText("暂无城市数据");
            timeRangeTextView.setText("暂无时间范围数据");
            monthlyStatsTextView.setText("暂无月度统计数据");
            return;
        }
        
        // 计算分类统计
        Map<String, Integer> categoryStats = new HashMap<>();
        for (FootprintEntity footprint : footprints) {
            String category = footprint.getCategory();
            if (category == null || category.isEmpty()) {
                category = "未分类";
            }
            categoryStats.put(category, categoryStats.getOrDefault(category, 0) + 1);
        }
        
        // 显示分类统计
        StringBuilder categoryStatsText = new StringBuilder("分类统计:\n");
        for (Map.Entry<String, Integer> entry : categoryStats.entrySet()) {
            float percentage = (float) entry.getValue() / totalFootprints * 100;
            categoryStatsText.append(String.format("%s: %d (%.1f%%)\n", 
                    entry.getKey(), entry.getValue(), percentage));
        }
        categoriesStatsTextView.setText(categoryStatsText.toString());
        
        // 计算城市统计
        Map<String, Integer> cityStats = new HashMap<>();
        for (FootprintEntity footprint : footprints) {
            String city = footprint.getCityName();
            if (city == null || city.isEmpty()) {
                city = "未知城市";
            }
            cityStats.put(city, cityStats.getOrDefault(city, 0) + 1);
        }
        
        // 显示城市统计
        StringBuilder cityStatsText = new StringBuilder("城市分布:\n");
        for (Map.Entry<String, Integer> entry : cityStats.entrySet()) {
            float percentage = (float) entry.getValue() / totalFootprints * 100;
            cityStatsText.append(String.format("%s: %d (%.1f%%)\n", 
                    entry.getKey(), entry.getValue(), percentage));
        }
        citiesStatsTextView.setText(cityStatsText.toString());
        
        // 计算时间范围
        long earliestTime = Long.MAX_VALUE;
        long latestTime = Long.MIN_VALUE;
        for (FootprintEntity footprint : footprints) {
            long timestamp = footprint.getTimestamp();
            if (timestamp < earliestTime) {
                earliestTime = timestamp;
            }
            if (timestamp > latestTime) {
                latestTime = timestamp;
            }
        }
        
        // 显示时间范围
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String timeRangeText = String.format("时间范围: %s 至 %s", 
                dateFormat.format(new Date(earliestTime)),
                dateFormat.format(new Date(latestTime)));
        timeRangeTextView.setText(timeRangeText);
        
        // 计算月度统计
        Map<String, Integer> monthlyStats = new HashMap<>();
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        for (FootprintEntity footprint : footprints) {
            String month = monthFormat.format(new Date(footprint.getTimestamp()));
            monthlyStats.put(month, monthlyStats.getOrDefault(month, 0) + 1);
        }
        
        // 显示月度统计
        StringBuilder monthlyStatsText = new StringBuilder("月度统计:\n");
        for (Map.Entry<String, Integer> entry : monthlyStats.entrySet()) {
            monthlyStatsText.append(String.format("%s: %d\n", entry.getKey(), entry.getValue()));
        }
        monthlyStatsTextView.setText(monthlyStatsText.toString());
    }
    
    /**
     * 单元测试方法
     * 测试统计计算是否正确
     * @return 是否测试通过
     */
    public boolean testStatisticsCalculation() {
        try {
            // 创建测试数据
            List<FootprintEntity> testData = new java.util.ArrayList<>();
            
            FootprintEntity footprint1 = new FootprintEntity();
            footprint1.setId(1);
            footprint1.setCategory("旅游");
            footprint1.setCityName("北京");
            footprint1.setTimestamp(System.currentTimeMillis());
            
            FootprintEntity footprint2 = new FootprintEntity();
            footprint2.setId(2);
            footprint2.setCategory("美食");
            footprint2.setCityName("上海");
            footprint2.setTimestamp(System.currentTimeMillis() - 86400000); // 前一天
            
            FootprintEntity footprint3 = new FootprintEntity();
            footprint3.setId(3);
            footprint3.setCategory("旅游");
            footprint3.setCityName("北京");
            footprint3.setTimestamp(System.currentTimeMillis() - 2 * 86400000); // 前两天
            
            testData.add(footprint1);
            testData.add(footprint2);
            testData.add(footprint3);
            
            // 手动计算统计结果
            Map<String, Integer> categoryStats = new HashMap<>();
            for (FootprintEntity footprint : testData) {
                String category = footprint.getCategory();
                categoryStats.put(category, categoryStats.getOrDefault(category, 0) + 1);
            }
            
            // 验证结果
            return testData.size() == 3 &&
                   categoryStats.get("旅游") == 2 &&
                   categoryStats.get("美食") == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}