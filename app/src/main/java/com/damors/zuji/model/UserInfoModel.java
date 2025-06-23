package com.damors.zuji.model;

import java.util.Date;

import lombok.Data;

/**
 * 用户信息接口响应模型类
 * 
 * 用于映射获取用户信息接口的完整响应数据
 * 包含响应状态、消息和用户数据
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
@Data
public class UserInfoModel {

    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 用户账号 */
    private String userName;

    /** 用户昵称 */
    private String nickName;

    /** 用户邮箱 */
    private String email;

    /** 手机号码 */
    private String phonenumber;

    /** 用户性别 */
    private String sex;

    /** 用户头像 */
    private String avatar;

    /** 密码 */
    private String password;
    /**
     * 小程序OpenId
     */
    private String xcxOpenId;

    /** 帐号状态（0正常 1停用） */
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 最后登录IP */
    private String loginIp;

    /** 足迹数 */
    private Long footPrintCount ;
    /** 城市数 */
    private Long cityCount;
    /** 在线天数 */
    private Long dayCount;

}