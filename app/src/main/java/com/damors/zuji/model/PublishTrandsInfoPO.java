package com.damors.zuji.model;

import java.io.File;
import java.util.List;

/**
 * 足迹发布参数实体类
 * 用于向服务器发布足迹动态信息
 */
public class PublishTrandsInfoPO {
    
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 城市
     */
    private String city;
    
    /**
     * 位置信息
     */
    private String locationInfo;
    
    /**
     * 类型
     */
    private String type;
    
    /**
     * 内容
     */
    private String content;
    
    /**
     * 标签
     */
    private String tag;
    
    /**
     * 图片文件对象列表（用于文件上传）
     */
    private List<File> images;
    
    /**
     * 经度
     */
    private Double lng;
    
    /**
     * 纬度
     */
    private Double lat;
    
    /**
     * 消息类型 (1: 公开, 2: 个人可见)
     */
    private Integer msgType;
    
    // 构造函数
    public PublishTrandsInfoPO() {
    }
    
    public PublishTrandsInfoPO(String userId, String locationInfo, String type, 
                              String content, String tag, List<File> images,
                              Double lng, Double lat, Integer msgType) {
        this.userId = userId;
        this.locationInfo = locationInfo;
        this.type = type;
        this.content = content;
        this.tag = tag;
        this.images = images;
        this.lng = lng;
        this.lat = lat;
        this.msgType = msgType;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    // Getter和Setter方法
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getLocationInfo() {
        return locationInfo;
    }
    
    public void setLocationInfo(String locationInfo) {
        this.locationInfo = locationInfo;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public List<File> getImages() {
        return images;
    }
    
    public void setImages(List<File> images) {
        this.images = images;
    }
    
    public Double getLng() {
        return lng;
    }
    
    public void setLng(Double lng) {
        this.lng = lng;
    }
    
    public Double getLat() {
        return lat;
    }
    
    public void setLat(Double lat) {
        this.lat = lat;
    }
    
    public Integer getMsgType() {
        return msgType;
    }
    
    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }
    
    @Override
    public String toString() {
        return "PublishTrandsInfoPO{" +
                "userId='" + userId + '\'' +
                ", locationInfo='" + locationInfo + '\'' +
                ", type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", tag='" + tag + '\'' +
                ", lng=" + lng +
                ", lat=" + lat +
                ", msgType=" + msgType +
                '}';
    }
}