package com.damors.zuji.utils;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 时间工具类
 */
public class TimeUtils {
    
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String ISO_PATTERN_WITH_ZONE = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    
    /**
     * 格式化时间为相对时间显示
     * @param timeStr 时间字符串
     * @return 格式化后的时间字符串
     */
    public static String formatTime(String timeStr) {
        if (TextUtils.isEmpty(timeStr)) {
            return "";
        }
        
        try {
            Date date = parseDate(timeStr);
            if (date == null) {
                return timeStr;
            }
            
            long currentTime = System.currentTimeMillis();
            long targetTime = date.getTime();
            long diff = currentTime - targetTime;
            
            // 如果时间差为负数，说明是未来时间，直接返回格式化的日期
            if (diff < 0) {
                return formatDate(date, "MM-dd HH:mm");
            }
            
            // 计算时间差
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (seconds < 60) {
                return "刚刚";
            } else if (minutes < 60) {
                return minutes + "分钟前";
            } else if (hours < 24) {
                return hours + "小时前";
            } else if (days < 7) {
                return days + "天前";
            } else {
                // 超过7天显示具体日期
                return formatDate(date, "MM-dd HH:mm");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return timeStr;
        }
    }
    
    /**
     * 解析时间字符串为Date对象
     * @param timeStr 时间字符串
     * @return Date对象
     */
    private static Date parseDate(String timeStr) {
        if (TextUtils.isEmpty(timeStr)) {
            return null;
        }
        
        // 尝试不同的时间格式
        String[] patterns = {
            DEFAULT_PATTERN,
            ISO_PATTERN,
            ISO_PATTERN_WITH_ZONE,
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "MM-dd HH:mm"
        };
        
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                return sdf.parse(timeStr);
            } catch (ParseException e) {
                // 继续尝试下一个格式
            }
        }
        
        return null;
    }
    
    /**
     * 格式化Date对象为指定格式的字符串
     * @param date Date对象
     * @param pattern 格式模式
     * @return 格式化后的字符串
     */
    private static String formatDate(Date date, String pattern) {
        if (date == null) {
            return "";
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * 获取当前时间的格式化字符串
     * @return 当前时间字符串
     */
    public static String getCurrentTime() {
        return formatDate(new Date(), DEFAULT_PATTERN);
    }
    
    /**
     * 获取当前时间的格式化字符串（指定格式）
     * @param pattern 格式模式
     * @return 当前时间字符串
     */
    public static String getCurrentTime(String pattern) {
        return formatDate(new Date(), pattern);
    }
}