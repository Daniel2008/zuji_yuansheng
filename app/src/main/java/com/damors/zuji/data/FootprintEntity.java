package com.damors.zuji.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 足迹实体类，用于存储用户的位置记录
 * 使用Room数据库注解
 */
@Entity(tableName = "footprints")
public class FootprintEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private double latitude;  // 纬度
    private double longitude; // 经度
    private long timestamp;   // 时间戳
    private String description; // 描述信息
    private double altitude;  // 海拔
    private float accuracy;   // 精度
    
    // 多媒体内容支持
    private String imageUris;  // 图片URI列表，以逗号分隔
    private String videoUri;   // 视频URI
    private String locationName; // 位置名称
    private String cityName;    // 城市名称
    private String category;    // 足迹分类（如旅游、美食、购物等）

    /**
     * 获取格式化的日期字符串
     * @return 格式化的日期字符串
     */
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    /**
     * 设置时间戳
     * @param date 日期对象
     */
    public void setTimestamp(Date date) {
        this.timestamp = date.getTime();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }
    
    // 新增字段的getter和setter方法
    public String getImageUris() {
        return imageUris;
    }

    public void setImageUris(String imageUris) {
        this.imageUris = imageUris;
    }

    public String getVideoUri() {
        return videoUri;
    }

    public void setVideoUri(String videoUri) {
        this.videoUri = videoUri;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    
    /**
     * 获取图片URI列表
     * @return 图片URI列表
     */
    public List<String> getImageUriList() {
        if (imageUris == null || imageUris.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(imageUris.split(","));
    }
    
    /**
     * 设置图片URI列表
     * @param uriList 图片URI列表
     */
    public void setImageUriList(List<String> uriList) {
        if (uriList == null || uriList.isEmpty()) {
            this.imageUris = "";
            return;
        }
        this.imageUris = String.join(",", uriList);
    }
    
    /**
     * 单元测试方法
     * 测试实体类是否正确存储和返回数据
     * @return 是否测试通过
     */
    public boolean testEntityData() {
        try {
            // 设置测试数据
            setLatitude(39.9087);
            setLongitude(116.3975);
            setTimestamp(new Date());
            setAltitude(50.5);
            setAccuracy(10.0f);
            setDescription("测试位置");
            setLocationName("北京天安门");
            setCityName("北京");
            setCategory("旅游");
            setImageUris("image1.jpg,image2.jpg");
            setVideoUri("video1.mp4");
            
            // 验证数据
            return getLatitude() == 39.9087 &&
                   getLongitude() == 116.3975 &&
                   getTimestamp() > 0 &&
                   getAltitude() == 50.5 &&
                   getAccuracy() == 10.0f &&
                   getDescription().equals("测试位置") &&
                   getLocationName().equals("北京天安门") &&
                   getCityName().equals("北京") &&
                   getCategory().equals("旅游") &&
                   getImageUris().equals("image1.jpg,image2.jpg") &&
                   getVideoUri().equals("video1.mp4") &&
                   getImageUriList().size() == 2;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}