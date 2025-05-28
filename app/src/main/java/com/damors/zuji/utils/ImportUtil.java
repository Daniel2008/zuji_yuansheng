package com.damors.zuji.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.viewmodel.FootprintViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据导入工具类
 * 用于从JSON文件中导入足迹数据，与导出功能配套使用
 */
public class ImportUtil {

    private static final String TAG = "ImportUtil";

    /**
     * 从JSON文件导入足迹数据
     * @param context 上下文
     * @param uri 文件URI
     * @param viewModel 足迹ViewModel，用于保存导入的数据
     * @return 导入的足迹数量，如果导入失败则返回-1
     */
    public static int importFootprintsFromJson(Context context, Uri uri, FootprintViewModel viewModel) {
        if (uri == null || viewModel == null) {
            Log.e(TAG, "参数无效，无法导入数据");
            return -1;
        }

        try {
            // 读取文件内容
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "无法打开文件流");
                return -1;
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
            String jsonString = stringBuilder.toString();
            JSONArray jsonArray = new JSONArray(jsonString);
            List<FootprintEntity> importedFootprints = new ArrayList<>();

            // 遍历JSON数组，转换为足迹实体
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                FootprintEntity footprint = jsonToFootprint(jsonObject);
                importedFootprints.add(footprint);
            }

            // 保存导入的足迹数据
            for (FootprintEntity footprint : importedFootprints) {
                // 重置ID，避免与现有数据冲突
                footprint.setId(0);
                viewModel.insert(footprint);
            }

            Log.d(TAG, "成功导入 " + importedFootprints.size() + " 条足迹数据");
            return importedFootprints.size();

        } catch (IOException | JSONException e) {
            Log.e(TAG, "足迹数据导入失败: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 将JSON对象转换为足迹实体
     * @param jsonObject JSON对象
     * @return 足迹实体
     * @throws JSONException JSON异常
     */
    private static FootprintEntity jsonToFootprint(JSONObject jsonObject) throws JSONException {
        FootprintEntity footprint = new FootprintEntity();

        // 设置基本信息
        if (jsonObject.has("description")) {
            footprint.setDescription(jsonObject.getString("description"));
        }
        if (jsonObject.has("locationName")) {
            footprint.setLocationName(jsonObject.getString("locationName"));
        }
        if (jsonObject.has("cityName")) {
            footprint.setCityName(jsonObject.getString("cityName"));
        }
        if (jsonObject.has("category")) {
            footprint.setCategory(jsonObject.getString("category"));
        }

        // 设置位置信息
        if (jsonObject.has("latitude")) {
            footprint.setLatitude(jsonObject.getDouble("latitude"));
        }
        if (jsonObject.has("longitude")) {
            footprint.setLongitude(jsonObject.getDouble("longitude"));
        }
        if (jsonObject.has("altitude")) {
            footprint.setAltitude(jsonObject.getDouble("altitude"));
        }
        if (jsonObject.has("accuracy")) {
            footprint.setAccuracy((float) jsonObject.getDouble("accuracy"));
        }
        if (jsonObject.has("timestamp")) {
            footprint.setTimestamp(jsonObject.getLong("timestamp"));
        }

        // 设置图片URI列表
        if (jsonObject.has("imageUris")) {
            JSONArray imageUrisArray = jsonObject.getJSONArray("imageUris");
            List<String> imageUriList = new ArrayList<>();
            for (int i = 0; i < imageUrisArray.length(); i++) {
                imageUriList.add(imageUrisArray.getString(i));
            }
            footprint.setImageUriList(imageUriList);
        }

        // 设置视频URI
        if (jsonObject.has("videoUri")) {
            footprint.setVideoUri(jsonObject.getString("videoUri"));
        }

        return footprint;
    }

    /**
     * 单元测试方法
     * 测试JSON转换为足迹实体是否正确
     * @return 是否测试通过
     */
    public static boolean testJsonToFootprintConversion() {
        try {
            // 创建测试JSON数据
            JSONObject testJson = new JSONObject();
            testJson.put("id", 1);
            testJson.put("description", "测试足迹");
            testJson.put("locationName", "测试位置");
            testJson.put("cityName", "测试城市");
            testJson.put("category", "测试分类");
            testJson.put("latitude", 39.9087);
            testJson.put("longitude", 116.3975);
            testJson.put("altitude", 50.0);
            testJson.put("accuracy", 10.0);
            testJson.put("timestamp", 1625097600000L); // 2021-07-01 00:00:00

            // 转换为足迹实体
            FootprintEntity footprint = jsonToFootprint(testJson);

            // 验证字段
            boolean fieldsCorrect = footprint.getDescription().equals("测试足迹") &&
                    footprint.getLocationName().equals("测试位置") &&
                    footprint.getCityName().equals("测试城市") &&
                    footprint.getCategory().equals("测试分类") &&
                    footprint.getLatitude() == 39.9087 &&
                    footprint.getLongitude() == 116.3975 &&
                    footprint.getAltitude() == 50.0 &&
                    footprint.getAccuracy() == 10.0f &&
                    footprint.getTimestamp() == 1625097600000L;

            return fieldsCorrect;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}