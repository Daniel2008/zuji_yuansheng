package com.damors.zuji.utils;

import android.content.Context;
import android.util.Log;

import org.osmdroid.config.Configuration;

import java.io.File;
import java.text.DecimalFormat;

/**
 * 地图缓存管理工具类
 * 提供地图瓦片缓存的管理功能，包括缓存大小查询、缓存清理等
 */
public class MapCacheManager {
    private static final String TAG = "MapCacheManager";
    
    /**
     * 获取地图缓存大小（字节）
     * @return 缓存大小，单位字节
     */
    public static long getCacheSize() {
        try {
            File cacheDir = Configuration.getInstance().getOsmdroidTileCache();
            if (cacheDir != null && cacheDir.exists()) {
                return getFolderSize(cacheDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取缓存大小失败", e);
        }
        return 0;
    }
    
    /**
     * 获取格式化的缓存大小字符串
     * @return 格式化的缓存大小，如"25.6 MB"
     */
    public static String getFormattedCacheSize() {
        long sizeInBytes = getCacheSize();
        return formatFileSize(sizeInBytes);
    }
    
    /**
     * 清理地图缓存
     * @return 是否清理成功
     */
    public static boolean clearCache() {
        try {
            File cacheDir = Configuration.getInstance().getOsmdroidTileCache();
            if (cacheDir != null && cacheDir.exists()) {
                boolean result = deleteFolder(cacheDir);
                if (result) {
                    // 重新创建缓存目录
                    cacheDir.mkdirs();
                    Log.d(TAG, "地图缓存清理成功");
                } else {
                    Log.w(TAG, "地图缓存清理失败");
                }
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "清理缓存失败", e);
        }
        return false;
    }
    
    /**
     * 清理过期的缓存文件
     * @param maxAgeMillis 最大缓存时间（毫秒）
     * @return 清理的文件数量
     */
    public static int clearExpiredCache(long maxAgeMillis) {
        int deletedCount = 0;
        try {
            File cacheDir = Configuration.getInstance().getOsmdroidTileCache();
            if (cacheDir != null && cacheDir.exists()) {
                long currentTime = System.currentTimeMillis();
                deletedCount = deleteExpiredFiles(cacheDir, currentTime - maxAgeMillis);
                Log.d(TAG, "清理过期缓存文件: " + deletedCount + " 个");
            }
        } catch (Exception e) {
            Log.e(TAG, "清理过期缓存失败", e);
        }
        return deletedCount;
    }
    
    /**
     * 获取缓存目录路径
     * @return 缓存目录路径
     */
    public static String getCachePath() {
        try {
            File cacheDir = Configuration.getInstance().getOsmdroidTileCache();
            if (cacheDir != null) {
                return cacheDir.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取缓存路径失败", e);
        }
        return "未知";
    }
    
    /**
     * 检查缓存是否可用
     * @return 缓存是否可用
     */
    public static boolean isCacheAvailable() {
        try {
            File cacheDir = Configuration.getInstance().getOsmdroidTileCache();
            return cacheDir != null && cacheDir.exists() && cacheDir.canWrite();
        } catch (Exception e) {
            Log.e(TAG, "检查缓存可用性失败", e);
        }
        return false;
    }
    
    /**
     * 获取缓存文件数量
     * @return 缓存文件数量
     */
    public static int getCacheFileCount() {
        try {
            File cacheDir = Configuration.getInstance().getOsmdroidTileCache();
            if (cacheDir != null && cacheDir.exists()) {
                return countFiles(cacheDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取缓存文件数量失败", e);
        }
        return 0;
    }
    
    /**
     * 递归计算文件夹大小
     * @param folder 文件夹
     * @return 文件夹大小（字节）
     */
    private static long getFolderSize(File folder) {
        long size = 0;
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getFolderSize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        } else {
            size = folder.length();
        }
        return size;
    }
    
    /**
     * 递归删除文件夹
     * @param folder 要删除的文件夹
     * @return 是否删除成功
     */
    private static boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!deleteFolder(file)) {
                        return false;
                    }
                }
            }
        }
        return folder.delete();
    }
    
    /**
     * 删除过期文件
     * @param folder 文件夹
     * @param cutoffTime 截止时间
     * @return 删除的文件数量
     */
    private static int deleteExpiredFiles(File folder, long cutoffTime) {
        int deletedCount = 0;
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deletedCount += deleteExpiredFiles(file, cutoffTime);
                    } else if (file.lastModified() < cutoffTime) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }
            }
        }
        return deletedCount;
    }
    
    /**
     * 递归计算文件数量
     * @param folder 文件夹
     * @return 文件数量
     */
    private static int countFiles(File folder) {
        int count = 0;
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        count += countFiles(file);
                    } else {
                        count++;
                    }
                }
            }
        } else {
            count = 1;
        }
        return count;
    }
    
    /**
     * 格式化文件大小
     * @param sizeInBytes 文件大小（字节）
     * @return 格式化的文件大小字符串
     */
    private static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) {
            return "0 B";
        }
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));
        
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        return decimalFormat.format(sizeInBytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}