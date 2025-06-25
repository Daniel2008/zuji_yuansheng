package com.damors.zuji.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.damors.zuji.model.UserInfoModel;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.model.response.LoginResponse;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.utils.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

public class UserManager {
    private static final String PREF_NAME = "user_pref";
    private static final String KEY_USER = "user_info";
    private static final String KEY_TOKEN = "user_token";

    private static UserManager instance;
    private static SharedPreferences preferences = null;
    private final Gson gson;
    private static RetrofitApiService apiService;

    private UserManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        apiService = RetrofitApiService.getInstance(context);
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

    public static void loadUserData() {
        // 使用token获取用户信息
        apiService.getUserInfo(
                new RetrofitApiService.SuccessCallback<BaseResponse<LoginResponse>>() {
                    @Override
                    public void onSuccess(BaseResponse<LoginResponse> response) {
                        // 检查响应是否成功
                        if (response != null && response.getCode() == 200 && response.getData() != null) {

                            saveUserAndToken(response.getData().getUser(),response.getData().getToken());
                        } else {
                            logout();
                        }
                    }
                },
                new RetrofitApiService.ErrorCallback() {
                    @Override
                    public void onError(String error) {
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
                    }
                }
        );
    }

    /**
     * 保存用户JSON数据和token
     * 
     * @param userInfoModel 用户JSON字符串
     * @param token 用户token
     */
    public static void saveUserAndToken(UserInfoModel userInfoModel, String token) {
        String currentUserJson = GsonUtil.GsonString(userInfoModel);

        SharedPreferences.Editor editor = preferences.edit();
        if (!TextUtils.isEmpty(currentUserJson)) {
            editor.putString(KEY_USER, currentUserJson);
        } else {
            editor.remove(KEY_USER);
        }

        if (!TextUtils.isEmpty(token)) {
            editor.putString(KEY_TOKEN, token);
        } else {
            editor.remove(KEY_TOKEN);
        }

        // 使用commit()确保数据立即写入磁盘，解决登录后数据同步问题
        boolean success = editor.commit();
        Log.d("UserManager", "保存用户数据结果: " + success);
    }

    public static void saveUserInfo(UserInfoModel userInfoModel) {
        String currentUserJson = GsonUtil.GsonString(userInfoModel);

        SharedPreferences.Editor editor = preferences.edit();
        if (!TextUtils.isEmpty(currentUserJson)) {
            editor.putString(KEY_USER, currentUserJson);
        } else {
            editor.remove(KEY_USER);
        }
        // 使用commit()确保数据立即写入磁盘，解决登录后数据同步问题
        boolean success = editor.commit();
        Log.d("UserManager", "保存用户数据结果: " + success);
    }

    /**
     * 获取用户数据
     *
     */
    public static UserInfoModel getUserInfo() {
        String currentUserJson = preferences.getString(KEY_USER, null);
        return GsonUtil.GsonToBean(currentUserJson, UserInfoModel.class);
    }

    /**
     * 获取用户数据
     *
     */
    public static String getToken() {
        String token = preferences.getString(KEY_TOKEN, null);
        return token;
    }


    /**
     * 清理旧的登录数据（兼容性处理）
     * 清理可能存在的"user_prefs"中的旧数据
     * 
     * @param context 应用上下文
     */
    public void clearLegacyLoginData(Context context) {
        try {
            SharedPreferences legacyPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            if (legacyPrefs.contains("token") || legacyPrefs.contains("user_data") || legacyPrefs.contains("is_logged_in")) {
                Log.d("UserManager", "发现旧的登录数据，正在清理...");
                boolean success = legacyPrefs.edit().clear().commit();
                Log.d("UserManager", "旧登录数据清理完成，结果: " + success);
            }
        } catch (Exception e) {
            Log.e("UserManager", "清理旧登录数据时发生错误", e);
        }
    }

    /**
     * 用户登出
     * 清除所有用户数据和token
     */
    public static void logout() {
        // 使用commit()确保数据立即清除
        boolean success = preferences.edit().clear().commit();
        Log.d("UserManager", "清除用户数据结果: " + success);
    }
    
    /**
     * 用户登出（带Context参数）
     * 清除所有用户数据和token，并清理旧数据
     * 
     * @param context 上下文对象
     */
    public void logout(Context context) {
        logout();
        
        // 同时清理可能存在的旧数据
        try {
            if (context != null) {
                clearLegacyLoginData(context);
            }
        } catch (Exception e) {
            Log.e("UserManager", "清理旧数据时发生错误", e);
        }
    }


    /**
     * 检查用户是否已登录
     * 增强版本：同时验证数据完整性
     * 
     * @return 是否已登录
     */
    public static boolean isLoggedIn() {
        String currentUserJson =  preferences.getString(KEY_USER, null);
        String token = preferences.getString(KEY_TOKEN,null);
        boolean hasUserJson = !TextUtils.isEmpty(currentUserJson);
        boolean hasToken = !TextUtils.isEmpty(token);
        boolean result = hasUserJson && hasToken;
        
        Log.d("UserManager", "登录状态检查: hasUserJson=" + hasUserJson + ", hasToken=" + hasToken + ", result=" + result);
        
        if (!hasUserJson) {
            Log.w("UserManager", "currentUserJson为空或null");
        }
        if (!hasToken) {
            Log.w("UserManager", "token为空或null");
        }
        
        return result;
    }

    /**
     * 检查并同步登录状态
     * 确保不会出现重复登录的情况
     * 
     * @return 登录状态是否有效
     */
    public static boolean checkAndSyncLoginState() {
        String currentUserJson = preferences.getString(KEY_USER,null);
        String token = preferences.getString(KEY_TOKEN,null);
        boolean hasValidData = !TextUtils.isEmpty(currentUserJson) && !TextUtils.isEmpty(token);
        
        if (!hasValidData) {
            // 如果数据不完整，清理所有登录状态
            Log.w("UserManager", "发现不完整的登录数据，正在清理...");
            logout();
            return false;
        }
        
        Log.d("UserManager", "登录状态检查通过");
        return true;
    }

    /**
     * 验证token有效性并更新用户信息
     * 
     * @param callback 验证结果回调
     */
    public static void validateTokenAndUpdateUserInfo(TokenValidationCallback callback) {
        String token = preferences.getString(KEY_TOKEN, null);
        if (TextUtils.isEmpty(token)) {
            // 没有token，直接返回失败
            if (callback != null) {
                callback.onValidationResult(false, "没有有效的token");
            }
            return;
        }else {
            if (callback != null) {
                callback.onValidationResult(true, "存在token");
            }
        }


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