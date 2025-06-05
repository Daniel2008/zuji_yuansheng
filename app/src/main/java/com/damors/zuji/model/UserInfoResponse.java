package com.damors.zuji.model;

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
public class UserInfoResponse {
    
    /** 响应状态码 */
    private int code;
    
    /** 响应消息 */
    private String msg;
    
    /** 响应数据 */
    private UserInfoData data;
    
    /**
     * 默认构造函数
     */
    public UserInfoResponse() {
    }
    
    /**
     * 带参数的构造函数
     * 
     * @param code 响应状态码
     * @param msg 响应消息
     * @param data 响应数据
     */
    public UserInfoResponse(int code, String msg, UserInfoData data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
    
    /**
     * 获取响应状态码
     * 
     * @return 状态码
     */
    public int getCode() {
        return code;
    }
    
    /**
     * 设置响应状态码
     * 
     * @param code 状态码
     */
    public void setCode(int code) {
        this.code = code;
    }
    
    /**
     * 获取响应消息
     * 
     * @return 响应消息
     */
    public String getMsg() {
        return msg;
    }
    
    /**
     * 设置响应消息
     * 
     * @param msg 响应消息
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    /**
     * 获取响应数据
     * 
     * @return 用户信息数据
     */
    public UserInfoData getData() {
        return data;
    }
    
    /**
     * 设置响应数据
     * 
     * @param data 用户信息数据
     */
    public void setData(UserInfoData data) {
        this.data = data;
    }
    
    /**
     * 判断请求是否成功
     * 
     * @return 是否成功（状态码为200表示成功）
     */
    public boolean isSuccess() {
        return code == 200;
    }
    
    /**
     * 重写toString方法，便于调试和日志输出
     * 
     * @return 响应信息的字符串表示
     */
    @Override
    public String toString() {
        return "UserInfoResponse{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
    
    /**
     * 用户信息数据内部类
     * 
     * 包含用户对象和token信息
     */
    public static class UserInfoData {
        
        /** 用户信息JSON对象 */
        private cn.hutool.json.JSONObject user;
        
        /** 用户token */
        private String token;
        
        /**
         * 默认构造函数
         */
        public UserInfoData() {
        }
        
        /**
         * 带参数的构造函数
         * 
         * @param user 用户信息JSON对象
         * @param token 用户token
         */
        public UserInfoData(cn.hutool.json.JSONObject user, String token) {
            this.user = user;
            this.token = token;
        }
        
        /**
         * 获取用户信息JSON对象
         * 
         * @return 用户信息JSON对象
         */
        public cn.hutool.json.JSONObject getUser() {
            return user;
        }
        
        /**
         * 设置用户信息JSON对象
         * 
         * @param user 用户信息JSON对象
         */
        public void setUser(cn.hutool.json.JSONObject user) {
            this.user = user;
        }
        
        /**
         * 获取用户token
         * 
         * @return 用户token
         */
        public String getToken() {
            return token;
        }
        
        /**
         * 设置用户token
         * 
         * @param token 用户token
         */
        public void setToken(String token) {
            this.token = token;
        }
        
        /**
         * 重写toString方法
         * 
         * @return 数据信息的字符串表示
         */
        @Override
        public String toString() {
            return "UserInfoData{" +
                    "user=" + user +
                    ", token='" + token + '\'' +
                    '}';
        }
    }
}