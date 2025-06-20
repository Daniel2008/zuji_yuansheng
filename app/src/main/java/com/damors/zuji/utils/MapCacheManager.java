package com.damors.zuji.utils;

import android.content.Context;
import android.util.Log;

// 高德地图缓存管理不需要特殊导入

import java.io.File;
import java.text.DecimalFormat;

/**
 * 地图缓存管理工具类
 * 提供地图瓦片缓存的管理功能，包括缓存大小查询、缓存清理等
 */
public class MapCacheManager {
    private static final String TAG = "MapCacheManager";
    private static Context sContext;
    
    /**
     * 初始化缓存管理器
     * @param context 应用上下文
     */
    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }
    
    /**
     * 获取高德地图缓存目录
     * @return 高德地图缓存目录
     */
    private static File getAMapCacheDir() {
        if (sContext == null) {
            Log.w(TAG, "MapCacheManager未初始化，请先调用init方法");
            return null;
        }
        
        // 高德地图可能的缓存目录路径
        String[] possibleCachePaths = {
            "amap",           // 应用缓存目录下的amap文件夹
            "map",            // 应用缓存目录下的map文件夹
            "tiles",          // 瓦片缓存目录
            "offlinemap",     // 离线地图缓存目录
            "com.amap.api"    // 高德API缓存目录
        };
        
        File largestCacheDir = null;
        long largestSize = 0;
        
        // 检查应用缓存目录下的各种可能路径
        for (String cachePath : possibleCachePaths) {
            File cacheDir = new File(sContext.getCacheDir(), cachePath);
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                long size = getFolderSize(cacheDir);
                if (size > largestSize) {
                    largestSize = size;
                    largestCacheDir = cacheDir;
                }
            }
        }
        
        // 检查应用文件目录下的缓存
        for (String cachePath : possibleCachePaths) {
            File cacheDir = new File(sContext.getFilesDir(), cachePath);
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                long size = getFolderSize(cacheDir);
                if (size > largestSize) {
                    largestSize = size;
                    largestCacheDir = cacheDir;
                }
            }
        }
        
        // 检查外部缓存目录
        if (sContext.getExternalCacheDir() != null) {
            for (String cachePath : possibleCachePaths) {
                File cacheDir = new File(sContext.getExternalCacheDir(), cachePath);
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    long size = getFolderSize(cacheDir);
                    if (size > largestSize) {
                        largestSize = size;
                        largestCacheDir = cacheDir;
                    }
                }
            }
        }
        
        // 如果没有找到现有缓存目录，创建默认的amap目录
        if (largestCacheDir == null) {
            largestCacheDir = new File(sContext.getCacheDir(), "amap");
            if (!largestCacheDir.exists()) {
                largestCacheDir.mkdirs();
            }
        }
        
        return largestCacheDir;
    }
    
    /**
     * 获取地图缓存大小（字节）
     * @return 缓存大小，单位字节
     */
    public static long getCacheSize() {
        try {
            long totalSize = 0;
            
            // 获取所有可能的缓存目录并计算总大小
            String[] possibleCachePaths = {
                "amap", "map", "tiles", "offlinemap", "com.amap.api"
            };
            
            // 检查应用缓存目录
            for (String cachePath : possibleCachePaths) {
                File cacheDir = new File(sContext.getCacheDir(), cachePath);
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    totalSize += getFolderSize(cacheDir);
                }
            }
            
            // 检查应用文件目录
            for (String cachePath : possibleCachePaths) {
                File cacheDir = new File(sContext.getFilesDir(), cachePath);
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    totalSize += getFolderSize(cacheDir);
                }
            }
            
            // 检查外部缓存目录
            if (sContext.getExternalCacheDir() != null) {
                for (String cachePath : possibleCachePaths) {
                    File cacheDir = new File(sContext.getExternalCacheDir(), cachePath);
                    if (cacheDir.exists() && cacheDir.isDirectory()) {
                        totalSize += getFolderSize(cacheDir);
                    }
                }
            }
            
            return totalSize;
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
            boolean allSuccess = true;
            int clearedDirs = 0;
            
            String[] possibleCachePaths = {
                "amap", "map", "tiles", "offlinemap", "com.amap.api"
            };
            
            // 清理应用缓存目录下的地图缓存
            for (String cachePath : possibleCachePaths) {
                File cacheDir = new File(sContext.getCacheDir(), cachePath);
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    boolean result = deleteFolder(cacheDir);
                    if (result) {
                        clearedDirs++;
                        // 重新创建缓存目录
                        cacheDir.mkdirs();
                    } else {
                        allSuccess = false;
                    }
                }
            }
            
            // 清理应用文件目录下的地图缓存
            for (String cachePath : possibleCachePaths) {
                File cacheDir = new File(sContext.getFilesDir(), cachePath);
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    boolean result = deleteFolder(cacheDir);
                    if (result) {
                        clearedDirs++;
                        // 重新创建缓存目录
                        cacheDir.mkdirs();
                    } else {
                        allSuccess = false;
                    }
                }
            }
            
            // 清理外部缓存目录下的地图缓存
            if (sContext.getExternalCacheDir() != null) {
                for (String cachePath : possibleCachePaths) {
                    File cacheDir = new File(sContext.getExternalCacheDir(), cachePath);
                    if (cacheDir.exists() && cacheDir.isDirectory()) {
                        boolean result = deleteFolder(cacheDir);
                        if (result) {
                            clearedDirs++;
                            // 重新创建缓存目录
                            cacheDir.mkdirs();
                        } else {
                            allSuccess = false;
                        }
                    }
                }
            }
            
            if (allSuccess && clearedDirs > 0) {
                Log.d(TAG, "地图缓存清理成功，清理了 " + clearedDirs + " 个缓存目录");
            } else if (clearedDirs > 0) {
                Log.w(TAG, "地图缓存部分清理成功，清理了 " + clearedDirs + " 个缓存目录");
            } else {
                Log.w(TAG, "未找到需要清理的地图缓存目录");
            }
            
            return allSuccess;
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
            File cacheDir = getAMapCacheDir();
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
            StringBuilder pathBuilder = new StringBuilder();
            int foundDirs = 0;
            
            String[] possibleCachePaths = {
                "amap", "map", "tiles", "offlinemap", "com.amap.api"
            };
            
            // 检查应用缓存目录
            for (String cachePath : possibleCachePaths) {
                File cacheDir = new File(sContext.getCacheDir(), cachePath);
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    if (foundDirs > 0) pathBuilder.append("\n");
                    pathBuilder.append("缓存: ").append(cacheDir.getAbsolutePath());
                    foundDirs++;
                }
            }
            
            // 检查应用文件目录
            for (String cachePath : possibleCachePaths) {
                File cacheDir = new File(sContext.getFilesDir(), cachePath);
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    if (foundDirs > 0) pathBuilder.append("\n");
                    pathBuilder.append("文件: ").append(cacheDir.getAbsolutePath());
                    foundDirs++;
                }
            }
            
            // 检查外部缓存目录
            if (sContext.getExternalCacheDir() != null) {
                for (String cachePath : possibleCachePaths) {
                    File cacheDir = new File(sContext.getExternalCacheDir(), cachePath);
                    if (cacheDir.exists() && cacheDir.isDirectory()) {
                        if (foundDirs > 0) pathBuilder.append("\n");
                        pathBuilder.append("外部: ").append(cacheDir.getAbsolutePath());
                        foundDirs++;
                    }
                }
            }
            
            if (foundDirs > 0) {
                return pathBuilder.toString();
            } else {
                return "未找到地图缓存目录";
            }
        } catch (Exception e) {
            Log.e(TAG, "获取缓存路径失败", e);
        }
        return "获取路径失败";
    }
    
    /**
     * 检查缓存是否可用
     * @return 缓存是否可用
     */
    public static boolean isCacheAvailable() {
        try {
            File cacheDir = getAMapCacheDir();
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
            int totalCount = 0;
            
            // 获取所有可能的缓存目录并计算总文件数
            String[] possibleCachePaths = {
                "amap", "map", "tiles", "offlinemap", "com.amap.api"
            };
            
            // 检查应用缓存目录
            for (String cachePath : possibleCachePaths) {
                File cacheDir = new File(sContext.getCacheDir(), cachePath);
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    totalCount += countFiles(cacheDir);
                }
            }
            
            // 检查应用文件目录
            for (String cachePath : possibleCachePaths) {
                File cacheDir = new File(sContext.getFilesDir(), cachePath);
                if (cacheDir.exists() && cacheDir.isDirectory()) {
                    totalCount += countFiles(cacheDir);
                }
            }
            
            // 检查外部缓存目录
            if (sContext.getExternalCacheDir() != null) {
                for (String cachePath : possibleCachePaths) {
                    File cacheDir = new File(sContext.getExternalCacheDir(), cachePath);
                    if (cacheDir.exists() && cacheDir.isDirectory()) {
                        totalCount += countFiles(cacheDir);
                    }
                }
            }
            
            return totalCount;
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