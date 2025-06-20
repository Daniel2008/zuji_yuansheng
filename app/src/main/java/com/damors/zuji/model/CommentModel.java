package com.damors.zuji.model;

/**
 * 评论数据模型
 */
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
    // 移除replies字段，通过parentId关联回复数据
    
    public CommentModel() {}
    
    public CommentModel(Integer id, Integer msgId, Integer parentId, String content,
                  Integer userId, String userAvatar, String userName, String createTime) {
        this.id = id;
        this.msgId = msgId;
        this.parentId = parentId;
        this.content = content;
        this.userId = userId;
        this.userAvatar = userAvatar;
        this.userName = userName;
        this.createTime = createTime;
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getMsgId() {
        return msgId;
    }
    
    public void setMsgId(Integer msgId) {
        this.msgId = msgId;
    }
    
    public Integer getParentId() {
        return parentId;
    }

    public Long getParentUserId() {
        return parentUserId;
    }

    public void setParentUserId(Long parentUserId) {
        this.parentUserId = parentUserId;
    }

    public String getParentUserName() {
        return parentUserName;
    }

    public void setParentUserName(String parentUserName) {
        this.parentUserName = parentUserName;
    }

    public String getParentUserAvatar() {
        return parentUserAvatar;
    }

    public void setParentUserAvatar(String parentUserAvatar) {
        this.parentUserAvatar = parentUserAvatar;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public String getUserAvatar() {
        return userAvatar;
    }
    
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    
    // 移除getReplies和setReplies方法
    
    // 新增字段的getter和setter方法
    public String getDelFlag() {
        return delFlag;
    }
    
    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }
    
    public String getCreateBy() {
        return createBy;
    }
    
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
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
    
    /**
     * 判断是否为根评论（非回复）
     */
    public boolean isRootComment() {
        return parentId == null || parentId == 0;
    }
    
    // 移除hasReplies和getReplyCount方法，通过parentId关联回复数据
    
    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", msgId=" + msgId +
                ", parentId=" + parentId +
                ", content='" + content + '\'' +
                ", userId=" + userId +
                ", userAvatar='" + userAvatar + '\'' +
                ", userName='" + userName + '\'' +
                ", createTime=" + createTime +
                // 移除replies字段引用
                '}';
    }
}