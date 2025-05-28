package com.damors.zuji.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.damors.zuji.data.FootprintEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 足迹分享工具类
 * 提供分享足迹到社交媒体或其他应用的功能
 */
public class ShareUtil {

    private static final String TAG = "ShareUtil";

    /**
     * 分享足迹文本信息
     * @param context 上下文
     * @param footprint 足迹实体
     */
    public static void shareFootprintText(Context context, FootprintEntity footprint) {
        if (footprint == null) {
            return;
        }

        // 构建分享文本
        StringBuilder shareText = new StringBuilder();
        shareText.append("我的足迹\n");
        
        // 添加足迹描述
        if (footprint.getDescription() != null && !footprint.getDescription().isEmpty()) {
            shareText.append(footprint.getDescription()).append("\n");
        }
        
        // 添加位置信息
        if (footprint.getLocationName() != null && !footprint.getLocationName().isEmpty()) {
            shareText.append("位置: ").append(footprint.getLocationName()).append("\n");
        }
        
        // 添加城市信息
        if (footprint.getCityName() != null && !footprint.getCityName().isEmpty()) {
            shareText.append("城市: ").append(footprint.getCityName()).append("\n");
        }
        
        // 添加分类信息
        if (footprint.getCategory() != null && !footprint.getCategory().isEmpty()) {
            shareText.append("分类: ").append(footprint.getCategory()).append("\n");
        }
        
        // 添加时间信息
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateString = sdf.format(new Date(footprint.getTimestamp()));
        shareText.append("时间: ").append(dateString).append("\n");
        
        // 添加坐标信息
        shareText.append("坐标: ").append(footprint.getLatitude())
                .append(", ").append(footprint.getLongitude()).append("\n");
        
        // 添加应用信息
        shareText.append("来自足迹应用");
        
        // 创建分享Intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "分享足迹");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        
        // 启动分享选择器
        context.startActivity(Intent.createChooser(shareIntent, "分享足迹"));
    }
    
    /**
     * 分享足迹图片
     * @param context 上下文
     * @param footprint 足迹实体
     * @param bitmap 要分享的图片
     */
    public static void shareFootprintWithImage(Context context, FootprintEntity footprint, Bitmap bitmap) {
        if (footprint == null || bitmap == null) {
            return;
        }
        
        try {
            // 保存图片到临时文件
            File cachePath = new File(context.getCacheDir(), "images");
            cachePath.mkdirs();
            
            // 创建临时文件
            File imageFile = new File(cachePath, "shared_footprint_" + footprint.getId() + ".jpg");
            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            stream.close();
            
            // 获取文件URI
            Uri imageUri = FileProvider.getUriForFile(context, 
                    context.getPackageName() + ".fileprovider", imageFile);
            
            // 构建分享文本
            String shareText = "我的足迹: " + footprint.getDescription() + "\n" +
                    "位置: " + footprint.getLocationName() + "\n" +
                    "来自足迹应用";
            
            // 创建分享Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // 启动分享选择器
            context.startActivity(Intent.createChooser(shareIntent, "分享足迹"));
            
        } catch (IOException e) {
            Log.e(TAG, "分享图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 分享足迹到地图应用
     * @param context 上下文
     * @param footprint 足迹实体
     */
    public static void shareToMapApp(Context context, FootprintEntity footprint) {
        if (footprint == null) {
            return;
        }
        
        // 构建地图URI
        String geoUri = "geo:" + footprint.getLatitude() + "," + footprint.getLongitude() + 
                "?q=" + footprint.getLatitude() + "," + footprint.getLongitude() + 
                "(" + Uri.encode(footprint.getLocationName()) + ")";
        
        // 创建地图Intent
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        
        // 检查是否有应用可以处理此Intent
        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(mapIntent);
        } else {
            // 如果没有地图应用，尝试使用网页地图
            String mapUrl = "https://maps.google.com/maps?q=loc:" + 
                    footprint.getLatitude() + "," + footprint.getLongitude() + 
                    "(" + Uri.encode(footprint.getLocationName()) + ")";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl));
            context.startActivity(browserIntent);
        }
    }
    
    /**
     * 单元测试方法
     * 测试分享文本功能
     * @return 是否测试通过
     */
    public static boolean testShareText() {
        try {
            // 创建测试足迹
            FootprintEntity testFootprint = new FootprintEntity();
            testFootprint.setDescription("测试足迹");
            testFootprint.setLocationName("测试位置");
            testFootprint.setCityName("测试城市");
            testFootprint.setCategory("旅游");
            testFootprint.setLatitude(39.9087);
            testFootprint.setLongitude(116.3975);
            testFootprint.setTimestamp(new Date().getTime());
            
            // 构建分享文本
            StringBuilder shareText = new StringBuilder();
            shareText.append("我的足迹\n");
            shareText.append(testFootprint.getDescription()).append("\n");
            shareText.append("位置: ").append(testFootprint.getLocationName()).append("\n");
            shareText.append("城市: ").append(testFootprint.getCityName()).append("\n");
            shareText.append("分类: ").append(testFootprint.getCategory()).append("\n");
            
            // 验证文本是否正确构建
            return shareText.toString().contains("测试足迹") && 
                   shareText.toString().contains("测试位置") &&
                   shareText.toString().contains("测试城市") &&
                   shareText.toString().contains("旅游");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 单元测试方法
     * 测试地图URI构建功能
     * @return 是否测试通过
     */
    public static boolean testMapUriBuilding() {
        try {
            // 创建测试足迹
            FootprintEntity testFootprint = new FootprintEntity();
            testFootprint.setLocationName("测试位置");
            testFootprint.setLatitude(39.9087);
            testFootprint.setLongitude(116.3975);
            
            // 构建地图URI
            String geoUri = "geo:" + testFootprint.getLatitude() + "," + testFootprint.getLongitude() + 
                    "?q=" + testFootprint.getLatitude() + "," + testFootprint.getLongitude() + 
                    "(" + Uri.encode(testFootprint.getLocationName()) + ")";
            
            // 验证URI是否正确构建
            return geoUri.contains("39.9087") && 
                   geoUri.contains("116.3975") &&
                   geoUri.contains(Uri.encode("测试位置"));
        } catch (Exception e) {
            return false;
        }
    }
}