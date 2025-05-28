package com.damors.zuji.utils;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.damors.zuji.data.FootprintEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 足迹统计分析工具类
 * 提供足迹数据的统计和分析功能
 */
public class FootprintStatsUtil {

    private static final String TAG = "FootprintStatsUtil";

    /**
     * 按分类统计足迹数量
     * @param footprints 足迹列表
     * @return 分类统计结果，键为分类名称，值为该分类的足迹数量
     */
    public static Map<String, Integer> countByCategory(List<FootprintEntity> footprints) {
        Map<String, Integer> categoryStats = new HashMap<>();
        
        if (footprints == null || footprints.isEmpty()) {
            return categoryStats;
        }
        
        // 遍历足迹列表，按分类统计
        for (FootprintEntity footprint : footprints) {
            String category = footprint.getCategory();
            if (category == null || category.isEmpty()) {
                category = "未分类";
            }
            
            // 更新分类计数
            if (categoryStats.containsKey(category)) {
                categoryStats.put(category, categoryStats.get(category) + 1);
            } else {
                categoryStats.put(category, 1);
            }
        }
        
        return categoryStats;
    }
    
    /**
     * 按城市统计足迹数量
     * @param footprints 足迹列表
     * @return 城市统计结果，键为城市名称，值为该城市的足迹数量
     */
    public static Map<String, Integer> countByCity(List<FootprintEntity> footprints) {
        Map<String, Integer> cityStats = new HashMap<>();
        
        if (footprints == null || footprints.isEmpty()) {
            return cityStats;
        }
        
        // 遍历足迹列表，按城市统计
        for (FootprintEntity footprint : footprints) {
            String city = footprint.getCityName();
            if (city == null || city.isEmpty()) {
                city = "未知城市";
            }
            
            // 更新城市计数
            if (cityStats.containsKey(city)) {
                cityStats.put(city, cityStats.get(city) + 1);
            } else {
                cityStats.put(city, 1);
            }
        }
        
        return cityStats;
    }
    
    /**
     * 按月份统计足迹数量
     * @param footprints 足迹列表
     * @return 月份统计结果，键为月份（格式：yyyy-MM），值为该月的足迹数量
     */
    public static Map<String, Integer> countByMonth(List<FootprintEntity> footprints) {
        Map<String, Integer> monthStats = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        
        if (footprints == null || footprints.isEmpty()) {
            return monthStats;
        }
        
        // 遍历足迹列表，按月份统计
        for (FootprintEntity footprint : footprints) {
            long timestamp = footprint.getTimestamp();
            String month = sdf.format(new Date(timestamp));
            
            // 更新月份计数
            if (monthStats.containsKey(month)) {
                monthStats.put(month, monthStats.get(month) + 1);
            } else {
                monthStats.put(month, 1);
            }
        }
        
        return monthStats;
    }
    
    /**
     * 获取足迹时间跨度（最早和最晚的足迹时间）
     * @param footprints 足迹列表
     * @return 时间跨度，格式为[最早时间, 最晚时间]，如果列表为空则返回null
     */
    public static Date[] getTimeSpan(List<FootprintEntity> footprints) {
        if (footprints == null || footprints.isEmpty()) {
            return null;
        }
        
        long earliestTime = Long.MAX_VALUE;
        long latestTime = Long.MIN_VALUE;
        
        // 遍历足迹列表，找出最早和最晚的时间
        for (FootprintEntity footprint : footprints) {
            long timestamp = footprint.getTimestamp();
            if (timestamp < earliestTime) {
                earliestTime = timestamp;
            }
            if (timestamp > latestTime) {
                latestTime = timestamp;
            }
        }
        
        return new Date[] {new Date(earliestTime), new Date(latestTime)};
    }
    
    /**
     * 获取足迹活跃度统计（按天统计足迹数量）
     * @param footprints 足迹列表
     * @param days 统计的天数（从今天往前推算）
     * @return 活跃度统计结果，键为日期（格式：yyyy-MM-dd），值为该日的足迹数量
     */
    public static Map<String, Integer> getActivityStats(List<FootprintEntity> footprints, int days) {
        Map<String, Integer> activityStats = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        if (footprints == null || footprints.isEmpty() || days <= 0) {
            return activityStats;
        }
        
        // 初始化日期范围（从今天往前推算days天）
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        // 初始化所有日期的计数为0
        for (int i = 0; i < days; i++) {
            String dateStr = sdf.format(calendar.getTime());
            activityStats.put(dateStr, 0);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        
        // 重置日历到今天
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long todayStart = calendar.getTimeInMillis();
        
        // 计算days天前的起始时间
        calendar.add(Calendar.DAY_OF_MONTH, -(days - 1));
        long startTime = calendar.getTimeInMillis();
        
        // 遍历足迹列表，统计每天的足迹数量
        for (FootprintEntity footprint : footprints) {
            long timestamp = footprint.getTimestamp();
            
            // 只统计指定时间范围内的足迹
            if (timestamp >= startTime && timestamp <= todayStart + 24 * 60 * 60 * 1000) {
                Date footprintDate = new Date(timestamp);
                String dateStr = sdf.format(footprintDate);
                
                if (activityStats.containsKey(dateStr)) {
                    activityStats.put(dateStr, activityStats.get(dateStr) + 1);
                }
            }
        }
        
        return activityStats;
    }
    
    /**
     * 获取足迹距离统计（计算相邻足迹之间的距离总和）
     * @param footprints 足迹列表
     * @return 总距离（单位：米）
     */
    public static double getTotalDistance(List<FootprintEntity> footprints) {
        if (footprints == null || footprints.size() < 2) {
            return 0;
        }
        
        // 按时间排序足迹
        List<FootprintEntity> sortedFootprints = new ArrayList<>(footprints);
        Collections.sort(sortedFootprints, new Comparator<FootprintEntity>() {
            @Override
            public int compare(FootprintEntity f1, FootprintEntity f2) {
                return Long.compare(f1.getTimestamp(), f2.getTimestamp());
            }
        });
        
        double totalDistance = 0;
        
        // 计算相邻足迹之间的距离
        for (int i = 0; i < sortedFootprints.size() - 1; i++) {
            FootprintEntity current = sortedFootprints.get(i);
            FootprintEntity next = sortedFootprints.get(i + 1);
            
            // 使用Haversine公式计算两点之间的距离
            double distance = calculateDistance(
                    current.getLatitude(), current.getLongitude(),
                    next.getLatitude(), next.getLongitude());
            
            totalDistance += distance;
        }
        
        return totalDistance;
    }
    
    /**
     * 使用Haversine公式计算两个坐标点之间的距离
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 两点之间的距离（单位：米）
     */
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（千米）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c * 1000; // 转换为米
    }
    
    /**
     * 获取足迹热力图数据
     * @param footprints 足迹列表
     * @return 热力图数据，每个元素包含经纬度和权重
     */
    public static List<Map<String, Object>> getHeatmapData(List<FootprintEntity> footprints) {
        List<Map<String, Object>> heatmapData = new ArrayList<>();
        
        if (footprints == null || footprints.isEmpty()) {
            return heatmapData;
        }
        
        // 统计每个位置的足迹数量
        Map<String, Integer> locationCounts = new HashMap<>();
        Map<String, double[]> locationCoords = new HashMap<>();
        
        for (FootprintEntity footprint : footprints) {
            double lat = footprint.getLatitude();
            double lng = footprint.getLongitude();
            
            // 使用经纬度的字符串表示作为键
            String key = lat + "," + lng;
            
            if (locationCounts.containsKey(key)) {
                locationCounts.put(key, locationCounts.get(key) + 1);
            } else {
                locationCounts.put(key, 1);
                locationCoords.put(key, new double[]{lat, lng});
            }
        }
        
        // 转换为热力图数据格式
        for (Map.Entry<String, Integer> entry : locationCounts.entrySet()) {
            String key = entry.getKey();
            int count = entry.getValue();
            double[] coords = locationCoords.get(key);
            
            Map<String, Object> point = new HashMap<>();
            point.put("lat", coords[0]);
            point.put("lng", coords[1]);
            point.put("weight", count);
            
            heatmapData.add(point);
        }
        
        return heatmapData;
    }
    
    /**
     * 单元测试方法
     * 测试分类统计功能
     * @return 是否测试通过
     */
    public static boolean testCategoryStats() {
        try {
            // 创建测试足迹列表
            List<FootprintEntity> testFootprints = new ArrayList<>();
            
            FootprintEntity footprint1 = new FootprintEntity();
            footprint1.setCategory("旅游");
            testFootprints.add(footprint1);
            
            FootprintEntity footprint2 = new FootprintEntity();
            footprint2.setCategory("美食");
            testFootprints.add(footprint2);
            
            FootprintEntity footprint3 = new FootprintEntity();
            footprint3.setCategory("旅游");
            testFootprints.add(footprint3);
            
            // 执行分类统计
            Map<String, Integer> categoryStats = countByCategory(testFootprints);
            
            // 验证统计结果
            return categoryStats.size() == 2 && 
                   categoryStats.get("旅游") == 2 && 
                   categoryStats.get("美食") == 1;
            
        } catch (Exception e) {
            Log.e(TAG, "测试分类统计功能失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 单元测试方法
     * 测试距离计算功能
     * @return 是否测试通过
     */
    public static boolean testDistanceCalculation() {
        try {
            // 测试两个已知距离的点
            // 北京天安门（39.9087° N, 116.3975° E）和上海外滩（31.2304° N, 121.4737° E）
            double distance = calculateDistance(39.9087, 116.3975, 31.2304, 121.4737);
            
            // 两点之间的实际距离约为1067公里，允许5%的误差
            double expectedDistance = 1067000; // 1067公里转换为米
            double errorMargin = expectedDistance * 0.05; // 5%误差范围
            
            return Math.abs(distance - expectedDistance) <= errorMargin;
            
        } catch (Exception e) {
            Log.e(TAG, "测试距离计算功能失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 单元测试方法
     * 测试时间统计功能
     * @return 是否测试通过
     */
    public static boolean testTimeStats() {
        try {
            // 创建测试足迹列表
            List<FootprintEntity> testFootprints = new ArrayList<>();
            
            // 创建三个不同时间的足迹
            Calendar cal = Calendar.getInstance();
            
            FootprintEntity footprint1 = new FootprintEntity();
            footprint1.setTimestamp(cal.getTimeInMillis());
            testFootprints.add(footprint1);
            
            cal.add(Calendar.DAY_OF_MONTH, -1);
            FootprintEntity footprint2 = new FootprintEntity();
            footprint2.setTimestamp(cal.getTimeInMillis());
            testFootprints.add(footprint2);
            
            cal.add(Calendar.DAY_OF_MONTH, -5);
            FootprintEntity footprint3 = new FootprintEntity();
            footprint3.setTimestamp(cal.getTimeInMillis());
            testFootprints.add(footprint3);
            
            // 获取时间跨度
            Date[] timeSpan = getTimeSpan(testFootprints);
            
            // 验证时间跨度
            return timeSpan != null && 
                   timeSpan.length == 2 && 
                   timeSpan[0].getTime() == footprint3.getTimestamp() && 
                   timeSpan[1].getTime() == footprint1.getTimestamp();
            
        } catch (Exception e) {
            Log.e(TAG, "测试时间统计功能失败: " + e.getMessage());
            return false;
        }
    }
}