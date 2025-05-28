package com.damors.zuji.model;

import java.util.List;

/**
 * 足迹动态实体类
 * 用于表示用户发布的足迹动态信息
 */
public class FootprintMessage {
    
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
     * 删除标志
     */
    private String delFlag;
    
    /**
     * 关联文件列表
     */
    private List<GuluFile> guluFiles;
    
    // 构造函数
    public FootprintMessage() {
    }
    
    // Getter和Setter方法
    public String getCreateBy() {
        return createBy;
    }
    
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public String getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    
    public String getUpdateBy() {
        return updateBy;
    }
    
    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
    
    public String getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getMsgType() {
        return msgType;
    }
    
    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }
    
    public String getTextContent() {
        return textContent;
    }
    
    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public double getLng() {
        return lng;
    }
    
    public void setLng(double lng) {
        this.lng = lng;
    }
    
    public double getLat() {
        return lat;
    }
    
    public void setLat(double lat) {
        this.lat = lat;
    }
    
    public String getLocaltionTitle() {
        return localtionTitle;
    }
    
    public void setLocaltionTitle(String localtionTitle) {
        this.localtionTitle = localtionTitle;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getUserAvatar() {
        return userAvatar;
    }
    
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
    
    public String getDelFlag() {
        return delFlag;
    }
    
    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }
    
    public List<GuluFile> getGuluFiles() {
        return guluFiles;
    }
    
    public void setGuluFiles(List<GuluFile> guluFiles) {
        this.guluFiles = guluFiles;
    }
    
    @Override
    public String toString() {
        return "FootprintMessage{" +
                "createBy='" + createBy + '\'' +
                ", createTime='" + createTime + '\'' +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime='" + updateTime + '\'' +
                ", remark='" + remark + '\'' +
                ", id=" + id +
                ", msgType=" + msgType +
                ", textContent='" + textContent + '\'' +
                ", tag='" + tag + '\'' +
                ", lng=" + lng +
                ", lat=" + lat +
                ", localtionTitle='" + localtionTitle + '\'' +
                ", userId=" + userId +
                ", userAvatar='" + userAvatar + '\'' +
                ", delFlag='" + delFlag + '\'' +
                ", guluFiles=" + guluFiles +
                '}';
    }
}