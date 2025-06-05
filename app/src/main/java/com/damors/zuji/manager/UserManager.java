package com.damors.zuji.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.damors.zuji.network.HutoolApiService;
import com.damors.zuji.model.UserInfoResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserManager {
    private static final String PREF_NAME = "user_pref";
    private static final String KEY_USER = "user_info";
    private static final String KEY_TOKEN = "user_token";

    private static UserManager instance;
    private final SharedPreferences preferences;
    private final Gson gson;
    private String currentUserJson; // 存储用户JSON数据
    private String token;

    private UserManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadUserData();
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager(context);
                }
            }
        }
    }

    public static UserManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("UserManager must be initialized first");
        }
        return instance;
    }

    private void loadUserData() {
        currentUserJson = preferences.getString(KEY_USER, null);
        token = preferences.getString(KEY_TOKEN, null);
    }

    /**
     * 保存用户JSON数据和token
     * 
     * @param userJson 用户JSON字符串
     * @param token 用户token
     */
    public void saveUserAndToken(String userJson, String token) {
        this.currentUserJson = userJson;
        this.token = token;

        SharedPreferences.Editor editor = preferences.edit();
        if (!TextUtils.isEmpty(userJson)) {
            editor.putString(KEY_USER, userJson);
        } else {
            editor.remove(KEY_USER);
        }

        if (!TextUtils.isEmpty(token)) {
            editor.putString(KEY_TOKEN, token);
        } else {
            editor.remove(KEY_TOKEN);
        }

        editor.apply();
    }

    public void logout() {
        currentUserJson = null;
        token = null;
        preferences.edit().clear().apply();
    }

    /**
     * 获取当前用户JSON数据
     * 
     * @return 用户JSON字符串，如果未登录则返回null
     */
    public String getCurrentUserJson() {
        return currentUserJson;
    }
    
    /**
     * 获取用户指定字段的值
     * 
     * @param fieldName 字段名
     * @return 字段值，如果字段不存在或用户未登录则返回null
     */
    public String getUserField(String fieldName) {
        if (TextUtils.isEmpty(currentUserJson)) {
            return null;
        }
        
        try {
            JsonObject userObj = JsonParser.parseString(currentUserJson).getAsJsonObject();
            if (userObj.has(fieldName) && !userObj.get(fieldName).isJsonNull()) {
                return userObj.get(fieldName).getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    public String getToken() {
        return token;
    }

    public boolean isLoggedIn() {
        return !TextUtils.isEmpty(currentUserJson) && !TextUtils.isEmpty(token);
    }

    /**
     * 验证token有效性并更新用户信息
     * 
     * @param apiService API服务实例
     * @param callback 验证结果回调
     */
    public void validateTokenAndUpdateUserInfo(com.damors.zuji.network.HutoolApiService apiService, 
                                              TokenValidationCallback callback) {
        if (TextUtils.isEmpty(token)) {
            // 没有token，直接返回失败
            if (callback != null) {
                callback.onValidationResult(false, "没有有效的token");
            }
            return;
        }

        // 使用token获取用户信息
        apiService.getUserInfo(token, 
            response -> {
                // 检查响应是否成功
                if (response != null && response.isSuccess() && response.getData() != null) {
                    // token有效，更新用户信息缓存
                    String userJson = gson.toJson(response.getData().getUser());
                    String newToken = response.getData().getToken();
                    
                    // 使用返回的新token或保持原token
                    String tokenToSave = (newToken != null && !newToken.isEmpty()) ? newToken : token;
                    saveUserAndToken(userJson, tokenToSave);
                    
                    if (callback != null) {
                        callback.onValidationResult(true, "token验证成功，用户信息已更新");
                    }
                } else {
                    // 响应失败，需要判断是否为token失效
                    String errorMsg = (response != null) ? response.getMsg() : "获取用户信息失败";
                    
                    // 判断是否为网络相关错误
                    boolean isNetworkError = errorMsg != null && (
                        errorMsg.contains("网络") || 
                        errorMsg.contains("连接") || 
                        errorMsg.contains("超时") ||
                        errorMsg.contains("timeout") ||
                        errorMsg.contains("connection") ||
                        errorMsg.contains("network")
                    );
                    
                    // 检查是否为token相关错误（通常返回401或403状态码对应的消息）
                    boolean isTokenError = response != null && (
                        response.getCode() == 401 || 
                        response.getCode() == 403 ||
                        (errorMsg != null && (
                            errorMsg.contains("未授权") ||
                            errorMsg.contains("token") ||
                            errorMsg.contains("登录") ||
                            errorMsg.contains("权限")
                        ))
                    );
                    
                    // 只有在明确是token错误时才清除登录状态
                    if (isTokenError && !isNetworkError) {
                        logout();
                    }
                    
                    if (callback != null) {
                        callback.onValidationResult(false, errorMsg);
                    }
                }
            },
            error -> {
                // 判断是否为网络错误，避免因网络问题误清除登录状态
                boolean isNetworkError = error != null && (
                    error.contains("网络") || 
                    error.contains("连接") || 
                    error.contains("超时") ||
                    error.contains("timeout") ||
                    error.contains("connection") ||
                    error.contains("network")
                );
                
                if (!isNetworkError) {
                    // 非网络错误，可能是token真正失效，清除本地数据
                    logout();
                }
                
                if (callback != null) {
                    String message = isNetworkError ? "网络连接失败，请检查网络后重试" : error;
                    callback.onValidationResult(false, message);
                }
            }
        );
    }

    /**
     * Token验证结果回调接口
     */
    public interface TokenValidationCallback {
        /**
         * 验证结果回调
         * 
         * @param isValid 是否有效
         * @param message 结果消息
         */
        void onValidationResult(boolean isValid, String message);
    }
}