package com.damors.zuji.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.damors.zuji.data.FootprintEntity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 足迹数据备份恢复工具类
 * 提供足迹数据的备份和恢复功能
 */
public class BackupRestoreUtil {

    private static final String TAG = "BackupRestoreUtil";
    private static final String BACKUP_FOLDER_NAME = "ZujiBackups";
    private static final String BACKUP_FILE_PREFIX = "zuji_backup_";
    private static final String BACKUP_FILE_EXTENSION = ".json";

    /**
     * 备份足迹数据到JSON文件
     * @param context 上下文
     * @param footprints 足迹列表
     * @return 备份文件的URI，如果备份失败则返回null
     */
    public static Uri backupFootprints(Context context, List<FootprintEntity> footprints) {
        if (footprints == null || footprints.isEmpty()) {
            Toast.makeText(context, "没有足迹数据可备份", Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            // 创建备份目录
            File backupDir = new File(context.getExternalFilesDir(null), BACKUP_FOLDER_NAME);
            if (!backupDir.exists()) {
                if (!backupDir.mkdirs()) {
                    Log.e(TAG, "无法创建备份目录");
                    return null;
                }
            }

            // 创建备份文件名（使用时间戳）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String fileName = BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_EXTENSION;
            File backupFile = new File(backupDir, fileName);

            // 将足迹数据转换为JSON
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonData = gson.toJson(footprints);

            // 写入文件
            FileWriter writer = new FileWriter(backupFile);
            writer.write(jsonData);
            writer.close();

            Log.d(TAG, "足迹数据已备份到: " + backupFile.getAbsolutePath());
            return Uri.fromFile(backupFile);

        } catch (IOException e) {
            Log.e(TAG, "备份失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从JSON文件恢复足迹数据
     * @param context 上下文
     * @param uri 备份文件的URI
     * @return 恢复的足迹列表，如果恢复失败则返回null
     */
    public static List<FootprintEntity> restoreFootprints(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }

        try {
            // 读取备份文件内容
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "无法打开备份文件");
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();
            inputStream.close();

            // 解析JSON数据
            String jsonData = stringBuilder.toString();
            Gson gson = new Gson();
            Type footprintListType = new TypeToken<ArrayList<FootprintEntity>>(){}.getType();
            List<FootprintEntity> restoredFootprints = gson.fromJson(jsonData, footprintListType);

            Log.d(TAG, "已恢复 " + (restoredFootprints != null ? restoredFootprints.size() : 0) + " 条足迹数据");
            return restoredFootprints;

        } catch (IOException e) {
            Log.e(TAG, "恢复失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "解析备份文件失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取所有备份文件列表
     * @param context 上下文
     * @return 备份文件列表
     */
    public static List<File> getBackupFiles(Context context) {
        List<File> backupFiles = new ArrayList<>();
        File backupDir = new File(context.getExternalFilesDir(null), BACKUP_FOLDER_NAME);
        
        if (backupDir.exists() && backupDir.isDirectory()) {
            File[] files = backupDir.listFiles((dir, name) -> 
                    name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));
            
            if (files != null) {
                for (File file : files) {
                    backupFiles.add(file);
                }
            }
        }
        
        return backupFiles;
    }

    /**
     * 导出备份文件到外部存储
     * @param context 上下文
     * @param backupFileUri 备份文件URI
     * @param destinationUri 目标URI
     * @return 是否导出成功
     */
    public static boolean exportBackup(Context context, Uri backupFileUri, Uri destinationUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(backupFileUri);
            OutputStream outputStream = context.getContentResolver().openOutputStream(destinationUri);
            
            if (inputStream == null || outputStream == null) {
                return false;
            }
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            
            return true;
        } catch (IOException e) {
            Log.e(TAG, "导出备份失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除备份文件
     * @param backupFile 备份文件
     * @return 是否删除成功
     */
    public static boolean deleteBackup(File backupFile) {
        if (backupFile != null && backupFile.exists()) {
            return backupFile.delete();
        }
        return false;
    }

    /**
     * 单元测试方法
     * 测试备份功能
     * @return 是否测试通过
     */
    public static boolean testBackupFunction() {
        try {
            // 创建测试足迹列表
            List<FootprintEntity> testFootprints = new ArrayList<>();
            
            FootprintEntity footprint1 = new FootprintEntity();
            footprint1.setDescription("测试足迹1");
            footprint1.setLocationName("测试位置1");
            footprint1.setLatitude(39.9087);
            footprint1.setLongitude(116.3975);
            footprint1.setTimestamp(new Date().getTime());
            testFootprints.add(footprint1);
            
            FootprintEntity footprint2 = new FootprintEntity();
            footprint2.setDescription("测试足迹2");
            footprint2.setLocationName("测试位置2");
            footprint2.setLatitude(31.2304);
            footprint2.setLongitude(121.4737);
            footprint2.setTimestamp(new Date().getTime());
            testFootprints.add(footprint2);
            
            // 将足迹列表转换为JSON
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonData = gson.toJson(testFootprints);
            
            // 从JSON解析回足迹列表
            Type footprintListType = new TypeToken<ArrayList<FootprintEntity>>(){}.getType();
            List<FootprintEntity> restoredFootprints = gson.fromJson(jsonData, footprintListType);
            
            // 验证数据是否一致
            if (restoredFootprints == null || restoredFootprints.size() != 2) {
                return false;
            }
            
            FootprintEntity restored1 = restoredFootprints.get(0);
            FootprintEntity restored2 = restoredFootprints.get(1);
            
            return restored1.getDescription().equals("测试足迹1") &&
                   restored1.getLocationName().equals("测试位置1") &&
                   restored1.getLatitude() == 39.9087 &&
                   restored1.getLongitude() == 116.3975 &&
                   restored2.getDescription().equals("测试足迹2") &&
                   restored2.getLocationName().equals("测试位置2") &&
                   restored2.getLatitude() == 31.2304 &&
                   restored2.getLongitude() == 121.4737;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 单元测试方法
     * 测试文件操作功能
     * @return 是否测试通过
     */
    public static boolean testFileOperations() {
        try {
            // 创建临时测试文件
            File tempFile = File.createTempFile("test_backup", ".json");
            
            // 写入测试数据
            FileWriter writer = new FileWriter(tempFile);
            writer.write("{\"test\": \"data\"}");
            writer.close();
            
            // 验证文件是否存在且可读
            boolean fileExists = tempFile.exists() && tempFile.canRead();
            
            // 删除测试文件
            boolean fileDeleted = tempFile.delete();
            
            return fileExists && fileDeleted;
            
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}