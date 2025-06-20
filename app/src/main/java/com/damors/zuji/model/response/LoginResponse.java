package com.damors.zuji.model.response;

import com.google.gson.JsonObject;

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
public class LoginResponse extends BaseResponse<LoginResponse.Data> {
    
    /**
     * 登录响应数据内部类
     */
    public static class Data {
        /** 用户信息JSON对象 */
        private JsonObject user;
        /** 用户认证token */
        private String token;

        /**
         * 获取用户信息JSON对象
         * 
         * @return 用户信息JSON对象
         */
        public JsonObject getUser() {
            return user;
        }

        /**
         * 设置用户信息JSON对象
         * 
         * @param user 用户信息JSON对象
         */
        public void setUser(JsonObject user) {
            this.user = user;
        }

        /**
         * 获取认证token
         * 
         * @return 认证token
         */
        public String getToken() {
            return token;
        }

        /**
         * 设置认证token
         * 
         * @param token 认证token
         */
        public void setToken(String token) {
            this.token = token;
        }
    }
}
