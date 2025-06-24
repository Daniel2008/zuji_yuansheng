package com.damors.zuji.model;

import java.util.Date;

import lombok.Data;

/**
 * 评论数据模型
 */
@Data
public class CommentModel {
    private Integer id;           // 对应数据库表的id字段(int类型)
    private Integer msgId;        // 对应数据库表的msg_id字段(int类型) 
    private Integer parentId;     // 对应数据库表的parent_id字段(int类型)
    /** 父评论用户ID（回复评论时使用） */
    private Long parentUserId;
    /** 父评论用户名称（回复评论时使用） */
    private String parentUserName;
    /** 父评论用户头像（回复评论时使用） */
    private String parentUserAvatar;
    private String content;       // 对应数据库表的content字段(varchar类型)
    private Integer userId;       // 对应数据库表的user_id字段(int类型)
    private String userAvatar;    // 用户头像
    private String userName;      // 用户名称
    private String createTime;    // 对应数据库表的create_time字段(datetime类型)
    private String delFlag;       // 对应数据库表的del_flag字段(varchar类型)
    private String createBy;      // 对应数据库表的create_by字段(varchar类型)
    private String updateBy;      // 对应数据库表的update_by字段(varchar类型)
    private String updateTime;    // 对应数据库表的update_time字段(datetime类型)
    private String remark;        // 对应数据库表的remark字段(varchar类型)

    /**
     * 判断是否为根评论（非回复）
     */
    public boolean isRootComment() {
        return parentId == null || parentId == 0;
    }


}