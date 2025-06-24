package com.damors.zuji.network;

import com.damors.zuji.BuildConfig;

/**
 * API配置类，集中管理API相关的配置信息
 */
public class ApiConfig {

    // 测试环境API地址
    private static final String TEST_BASE_URL = "http://192.168.1.5:8080";

    // 生产环境API地址
    private static final String PROD_BASE_URL = "https://zuji.damors.com";

    public static final String APP_UPDATE_URL = "https://zuji.damors.com/zuji/api/";

    // 从BuildConfig获取正式模式配置
    // Get production mode configuration from BuildConfig
    private static boolean proMode = BuildConfig.PRO_MODE;

    /**
     * 获取当前环境的API基础URL
     * @return API基础URL
     */
    public static String getBaseUrl() {
        if (proMode) {
            return PROD_BASE_URL;
        } else {
            // 默认使用测试环境
            return TEST_BASE_URL;
        }
    }

    // API超时设置（毫秒）
    public static final int TIMEOUT_MS = 30000;

    // API最大重试次数
    public static final int MAX_RETRIES = 1;

    // 重试等待时间的倍数
    public static final float BACKOFF_MULT = 1.0f;

    // API端点
    public static final class Endpoints {
        public static final String SEND_VERIFICATION_CODE = "/zuji/api/sendMsg";
        public static final String SMS_LOGIN = "/zuji/api/smsLogin";
        public static final String UPLOAD_AVATAR = "/zuji/api/upload";
        public static final String PUBLISH_FOOTPRINT = "/zuji/api/publishMsg";
        public static final String GET_MSG_LIST = "/zuji/api/getMsgList";
        public static final String GET_MSG_LIST_ALL = "/zuji/api/getMsgListAll"; // 地图页mark数据接口
        public static final String GET_MSG_DETAIL = "/zuji/api/getMsgDetail"; // 足迹动态消息接口
        public static final String GET_USER_INFO = "/zuji/api/getUserInfo"; // 获取用户信息接口
        public static final String SAVE_USER_INFO = "/zuji/api/saveUserInfo"; // 保存用户信息接口
        public static final String TOGGLE_LIKE = "/zuji/api/toggleLike"; // 点赞/取消点赞接口
        public static final String ADD_COMMENT = "/zuji/api/addComment"; // 添加评论接口
        public static final String GET_COMMENT_LIST = "/zuji/api/getCommentList"; // 获取评论列表接口
        public static final String DELETE_COMMENT = "/zuji/api/deleteComment"; // 删除评论接口
        public static final String CHECK_APP_UPDATE = "/checkAppUpdate"; // 检查应用更新接口
        public static final String DELETE_FOOTPRINT = "/zuji/api/deleteMsg";
    }
}
