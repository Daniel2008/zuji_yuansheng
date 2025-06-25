package com.damors.zuji.model.response;

import com.damors.zuji.model.UserInfoModel;
import com.google.gson.JsonObject;

import lombok.Data;

/**
 * 登录响应数据模型
 * 
 * 用于封装登录接口返回的用户信息和token
 * 现在直接使用JSONObject存储用户数据，避免对User类的依赖
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
@Data
public class LoginResponse {
    /** 用户信息JSON对象 */
    private UserInfoModel user;
    /** 用户认证token */
    private String token;

}
