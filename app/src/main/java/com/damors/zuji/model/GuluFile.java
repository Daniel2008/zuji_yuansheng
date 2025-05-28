package com.damors.zuji.model;

/**
 * 文件实体类
 * 用于表示足迹动态中的附件文件信息
 */
public class GuluFile {
    
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
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 关联类型
     */
    private String ofType;
    
    /**
     * 关联ID
     */
    private int ofId;
    
    /**
     * 删除标志
     */
    private String delFlag;
    
    // 构造函数
    public GuluFile() {
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
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getOfType() {
        return ofType;
    }
    
    public void setOfType(String ofType) {
        this.ofType = ofType;
    }
    
    public int getOfId() {
        return ofId;
    }
    
    public void setOfId(int ofId) {
        this.ofId = ofId;
    }
    
    public String getDelFlag() {
        return delFlag;
    }
    
    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }
    
    @Override
    public String toString() {
        return "GuluFile{" +
                "createBy='" + createBy + '\'' +
                ", createTime='" + createTime + '\'' +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime='" + updateTime + '\'' +
                ", remark='" + remark + '\'' +
                ", id=" + id +
                ", fileName='" + fileName + '\'' +
                ", fileType='" + fileType + '\'' +
                ", filePath='" + filePath + '\'' +
                ", ofType='" + ofType + '\'' +
                ", ofId=" + ofId +
                ", delFlag='" + delFlag + '\'' +
                '}';
    }
}