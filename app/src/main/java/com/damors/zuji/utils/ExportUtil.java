package com.damors.zuji.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.damors.zuji.data.FootprintEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 数据导出工具类
 * 用于将足迹数据导出为JSON格式，方便备份和分享
 */
public class ExportUtil {

    private static final String TAG = "ExportUtil";

    /**
     * 将足迹列表导出为JSON文件
     * 注释：已移除本地足迹导出功能
     * @param context 上下文
     * @param footprints 足迹列表
     * @return 导出的文件URI，如果导出失败则返回null
     */
    /*
    public static Uri exportFootprintsToJson(Context context, List<FootprintEntity> footprints) {
        if (footprints == null || footprints.isEmpty()) {
            Log.e(TAG, "足迹列表为空，无法导出");
            return null;
        }

        try {
            // 创建JSON数组
            JSONArray jsonArray = new JSONArray();

            // 遍历足迹列表，转换为JSON对象
            for (FootprintEntity footprint : footprints) {
                JSONObject jsonObject = footprintToJson(footprint);
                jsonArray.put(jsonObject);
            }

            // 生成文件名
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String fileName = "footprints_" + timestamp + ".json";

            // 获取外部存储目录
            File externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (!externalDir.exists()) {
                externalDir.mkdirs();
            }

            // 创建文件
            File file = new File(externalDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jsonArray.toString().getBytes());
            fos.close();

            Log.d(TAG, "足迹数据导出成功: " + file.getAbsolutePath());
            return Uri.fromFile(file);

        } catch (IOException | JSONException e) {
            Log.e(TAG, "足迹数据导出失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    */
    
    public static Uri exportFootprintsToJson(Context context, List<FootprintEntity> footprints) {
        Log.d(TAG, "本地存储功能已移除，无法导出足迹数据");
        return null;
    }
    /**
     * 将单个足迹转换为JSON对象
     * @param footprint 足迹实体
     * @return JSON对象
     * @throws JSONException JSON异常
     */
    private static JSONObject footprintToJson(FootprintEntity footprint) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", footprint.getId());
        jsonObject.put("description", footprint.getDescription());
        jsonObject.put("locationName", footprint.getLocationName());
        jsonObject.put("cityName", footprint.getCityName());
        jsonObject.put("category", footprint.getCategory());
        jsonObject.put("latitude", footprint.getLatitude());
        jsonObject.put("longitude", footprint.getLongitude());
        jsonObject.put("altitude", footprint.getAltitude());
        jsonObject.put("accuracy", footprint.getAccuracy());
        jsonObject.put("timestamp", footprint.getTimestamp());

        // 处理图片URI列表
        if (footprint.getImageUriList() != null && !footprint.getImageUriList().isEmpty()) {
            JSONArray imageUrisArray = new JSONArray();
            for (String imageUri : footprint.getImageUriList()) {
                imageUrisArray.put(imageUri);
            }
            jsonObject.put("imageUris", imageUrisArray);
        }

        // 处理视频URI
        if (footprint.getVideoUri() != null && !footprint.getVideoUri().isEmpty()) {
            jsonObject.put("videoUri", footprint.getVideoUri());
        }

        return jsonObject;
    }

    /**
     * 单元测试方法
     * 测试JSON转换是否正确
     * @return 是否测试通过
     */
    public static boolean testJsonConversion() {
        try {
            // 创建测试数据
            FootprintEntity testFootprint = new FootprintEntity();
            testFootprint.setId(1);
            testFootprint.setDescription("测试足迹");
            testFootprint.setLocationName("测试位置");
            testFootprint.setCityName("测试城市");
            testFootprint.setCategory("测试分类");
            testFootprint.setLatitude(39.9087);
            testFootprint.setLongitude(116.3975);
            testFootprint.setAltitude(50.0);
            testFootprint.setAccuracy(10.0f);
            testFootprint.setTimestamp(System.currentTimeMillis());

            // 转换为JSON
            JSONObject jsonObject = footprintToJson(testFootprint);

            // 验证JSON字段
            boolean fieldsCorrect = jsonObject.getInt("id") == 1 &&
                    jsonObject.getString("description").equals("测试足迹") &&
                    jsonObject.getString("locationName").equals("测试位置") &&
                    jsonObject.getString("cityName").equals("测试城市") &&
                    jsonObject.getString("category").equals("测试分类") &&
                    jsonObject.getDouble("latitude") == 39.9087 &&
                    jsonObject.getDouble("longitude") == 116.3975;

            return fieldsCorrect;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}