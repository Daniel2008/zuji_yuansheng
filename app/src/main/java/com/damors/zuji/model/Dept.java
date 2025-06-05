package com.damors.zuji.model;

import java.util.List;

/**
 * 部门信息模型类
 * 
 * 用于存储和管理用户所属部门的详细信息
 * 包含部门基本信息、层级关系、联系方式等
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
public class Dept {
    
    /** 创建者 */
    private String createBy;
    
    /** 创建时间 */
    private String createTime;
    
    /** 更新者 */
    private String updateBy;
    
    /** 更新时间 */
    private String updateTime;
    
    /** 备注信息 */
    private String remark;
    
    /** 部门ID */
    private int deptId;
    
    /** 父部门ID */
    private int parentId;
    
    /** 祖级列表 */
    private String ancestors;
    
    /** 部门名称 */
    private String deptName;
    
    /** 显示顺序 */
    private int orderNum;
    
    /** 负责人 */
    private String leader;
    
    /** 联系电话 */
    private String phone;
    
    /** 邮箱 */
    private String email;
    
    /** 部门状态（0正常 1停用） */
    private String status;
    
    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;
    
    /** 父部门名称 */
    private String parentName;
    
    /** 子部门列表 */
    private List<Dept> children;
    
    /**
     * 默认构造函数
     */
    public Dept() {
    }
    
    /**
     * 带参数的构造函数
     * 
     * @param deptId 部门ID
     * @param deptName 部门名称
     */
    public Dept(int deptId, String deptName) {
        this.deptId = deptId;
        this.deptName = deptName;
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
    
    public int getDeptId() {
        return deptId;
    }
    
    public void setDeptId(int deptId) {
        this.deptId = deptId;
    }
    
    public int getParentId() {
        return parentId;
    }
    
    public void setParentId(int parentId) {
        this.parentId = parentId;
    }
    
    public String getAncestors() {
        return ancestors;
    }
    
    public void setAncestors(String ancestors) {
        this.ancestors = ancestors;
    }
    
    public String getDeptName() {
        return deptName;
    }
    
    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
    
    public int getOrderNum() {
        return orderNum;
    }
    
    public void setOrderNum(int orderNum) {
        this.orderNum = orderNum;
    }
    
    public String getLeader() {
        return leader;
    }
    
    public void setLeader(String leader) {
        this.leader = leader;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDelFlag() {
        return delFlag;
    }
    
    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }
    
    public String getParentName() {
        return parentName;
    }
    
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }
    
    public List<Dept> getChildren() {
        return children;
    }
    
    public void setChildren(List<Dept> children) {
        this.children = children;
    }
    
    /**
     * 重写toString方法，便于调试和日志输出
     * 
     * @return 部门信息的字符串表示
     */
    @Override
    public String toString() {
        return "Dept{" +
                "deptId=" + deptId +
                ", deptName='" + deptName + '\'' +
                ", parentId=" + parentId +
                ", leader='" + leader + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
    
    /**
     * 重写equals方法，用于对象比较
     * 
     * @param obj 要比较的对象
     * @return 是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Dept dept = (Dept) obj;
        return deptId == dept.deptId;
    }
    
    /**
     * 重写hashCode方法
     * 
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(deptId);
    }
}