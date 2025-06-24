package com.damors.zuji.model;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 足迹动态实体类
 * 用于表示用户发布的足迹动态信息
 */
@Data
public class TrandsMsgModel implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 创建者
     */
    private String createBy;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    /**
     * 更新者
     */
    private String updateBy;
    
    /**
     * 更新时间
     */
    private String updateTime;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 主键ID
     */
    private int id;
    
    /**
     * 消息类型
     */
    private int msgType;
    
    /**
     * 文本内容
     */
    private String textContent;
    
    /**
     * 标签
     */
    private String tag;
    
    /**
     * 经度
     */
    private double lng;
    
    /**
     * 纬度
     */
    private double lat;
    
    /**
     * 位置标题
     */
    private String localtionTitle;
    
    /**
     * 用户ID
     */
    private int userId;
    
    /**
     * 用户头像
     */
    private String userAvatar;
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 删除标志
     */
    private String delFlag;
    
    /**
     * 关联文件列表
     */
    private List<GuluFileModel> guluFiles;
    /**
     * 关联评论列表
     */
    private List<CommentModel> comments;
    
    /**
     * 点赞数量
     */
    private int likeCount;
    
    /**
     * 评论数量
     */
    private int commentCount;
    
    /**
     * 当前用户是否已点赞
     */
    private boolean hasLiked;

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
     * 图片文件对象列表（用于文件上传）
     */
    private List<File> images;


}