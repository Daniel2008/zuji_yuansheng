package com.damors.zuji.model;

import lombok.Data;

/**
 * 文件实体类
 * 用于表示足迹动态中的附件文件信息
 */
@Data
public class GuluFileModel {
    
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

}