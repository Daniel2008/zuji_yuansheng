package com.damors.zuji.model;

import java.io.File;
import java.util.List;

import lombok.Data;

/**
 * 足迹发布参数实体类
 * 用于向服务器发布足迹动态信息
 */
@Data
public class TrandsInfoModel {
    
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

}