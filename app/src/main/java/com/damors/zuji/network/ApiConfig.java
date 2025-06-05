package com.damors.zuji.network;

/**
 * API配置类，集中管理API相关的配置信息
 */
public class ApiConfig {
    // 开发环境API地址
    private static final String DEV_BASE_URL = "http://192.168.1.5:8080/zuji/api/";

    // 测试环境API地址
    private static final String TEST_BASE_URL = "http://192.168.1.5:8080/zuji/api/";

    // 生产环境API地址
    private static final String PROD_BASE_URL = "https://api.zuji.com/api/";

    // 图片API地址
    private static final String IMAGE_BASE_URL = "http://192.168.1.5:8080";

    // 当前环境，可以通过BuildConfig.DEBUG等条件来自动切换
    private static final boolean IS_PRODUCTION = false;

    /**
     * 获取当前环境的API基础URL
     * @return API基础URL
     */
    public static String getBaseUrl() {
        if (IS_PRODUCTION) {
            return PROD_BASE_URL;
        } else {
            // 默认使用测试环境
            return TEST_BASE_URL;
        }
    }

    /**
     * 获取图片服务器基础URL
     * @return 图片基础URL
     */
    public static String getImageBaseUrl() {
        return IMAGE_BASE_URL;
    }

    // API超时设置（毫秒）
    public static final int TIMEOUT_MS = 30000;

    // API最大重试次数
    public static final int MAX_RETRIES = 1;

    // 重试等待时间的倍数
    public static final float BACKOFF_MULT = 1.0f;

    // API端点
    public static final class Endpoints {
        public static final String SEND_VERIFICATION_CODE = "sendMsg";
        public static final String SMS_LOGIN = "smsLogin";
        public static final String PUBLISH_FOOTPRINT = "publishMsg";
        public static final String GET_MSG_LIST = "getMsgList";
        public static final String FOOTPRINT_MESSAGES = "getMsgList"; // 足迹动态消息接口
        public static final String GET_USER_INFO = "getUserInfo"; // 获取用户信息接口
    }
}
