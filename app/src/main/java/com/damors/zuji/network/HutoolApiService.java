package com.damors.zuji.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.damors.zuji.ZujiApp;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.UserInfoResponse;

import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.GuluFile;
import com.damors.zuji.model.PublishTrandsInfoPO;
import com.damors.zuji.model.Comment;
import com.damors.zuji.model.CommentResponse;
import com.damors.zuji.model.AppUpdateInfo;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.model.response.LoginResponse;
import com.damors.zuji.model.response.FootprintMessageResponse;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 使用Hutool重构的API服务类
 * 提供网络请求功能，支持自动重试、网络状态监控等特性
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
public class HutoolApiService {
    private static final String TAG = "HutoolApiService";
    
    // 使用ApiConfig中的配置
    private static final String BASE_URL = ApiConfig.getBaseUrl();
    private static final int TIMEOUT_MS = ApiConfig.TIMEOUT_MS;
    private static final int MAX_RETRIES = ApiConfig.MAX_RETRIES;
    
    private static HutoolApiService instance;
    private Context context;
    private NetworkStateMonitor networkStateMonitor;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // 保存失败的请求信息，以便在网络恢复时重试
    private List<RequestInfo<?>> pendingRequests = new ArrayList<>();
    private boolean isRetryingRequests = false;

    /**
     * 网络状态变化监听器
     */
    private NetworkStateMonitor.NetworkStateListener networkStateListener = new NetworkStateMonitor.NetworkStateListener() {
        @Override
        public void onNetworkStateChanged(boolean isAvailable) {
            Log.d(TAG, "网络状态变化: " + (isAvailable ? "可用" : "不可用"));
            
            // 如果网络恢复，尝试重试失败的请求
            if (isAvailable && !pendingRequests.isEmpty() && !isRetryingRequests) {
                retryPendingRequests();
            }
            
            // 如果网络断开，显示提示
            if (!isAvailable) {
                showNetworkUnavailableMessage();
            }
        }
    };

    /**
     * 私有构造函数，实现单例模式
     * 
     * @param context 应用上下文
     */
    private HutoolApiService(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(4); // 创建线程池
        this.mainHandler = new Handler(Looper.getMainLooper()); // 主线程Handler
        
        // 获取ZujiApp中的NetworkStateMonitor实例
        this.networkStateMonitor = ((ZujiApp) this.context).getNetworkStateMonitor();
        if (this.networkStateMonitor != null) {
            this.networkStateMonitor.addNetworkStateListener(networkStateListener);
        } else {
            Log.e(TAG, "NetworkStateMonitor未初始化");
        }
    }

    /**
     * 获取ApiService单例实例
     * 
     * @param context 应用上下文
     * @return ApiService实例
     */
    public static synchronized HutoolApiService getInstance(Context context) {
        if (instance == null) {
            instance = new HutoolApiService(context);
        }
        return instance;
    }

    /**
     * 检查网络是否可用
     * 
     * @return 网络是否可用
     */
    private boolean isNetworkAvailable() {
        return networkStateMonitor != null && networkStateMonitor.isNetworkAvailable();
    }

    /**
     * 获取通用请求头
     * 
     * @return 请求头Map
     */
    private Map<String, String> getCommonHeaders() {
        Map<String, String> headers = new HashMap<>();
        // 注意：不要手动设置Content-Type，让Hutool自动处理multipart/form-data的boundary
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "ZujiApp/1.0 Android");
        
        // 添加认证token
        try {
            if (ZujiApp.getInstance() != null) {
                try {
                    UserManager userManager = UserManager.getInstance();
                    if (userManager != null && userManager.isLoggedIn()) {
                        headers.put("Authorization", "Bearer " + userManager.getToken());
                    }
                } catch (IllegalStateException e) {
                    Log.w(TAG, "UserManager未初始化，跳过添加认证token", e);
                }
            } else {
                Log.w(TAG, "ZujiApp未初始化，跳过添加认证token");
            }
        } catch (Exception e) {
            Log.e(TAG, "获取认证token时发生错误", e);
        }
        
        return headers;
    }

    /**
     * 执行POST请求
     * 
     * @param url 请求URL
     * @param params 请求参数
     * @param responseClass 响应数据类型
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param <T> 响应数据泛型
     */
    private <T> void executePostRequest(String url, Map<String, Object> params,
                                       Class<T> responseClass,
                                       SuccessCallback<T> successCallback,
                                       ErrorCallback errorCallback) {
        executePostRequest(url, params, responseClass, successCallback, errorCallback, null);
    }
    
    private <T> void executePostRequest(String url, Map<String, Object> params,
                                       Class<T> responseClass,
                                       SuccessCallback<T> successCallback,
                                       ErrorCallback errorCallback,
                                       LoadingCallback loadingCallback) {
        
        // 检查网络状态
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，将请求添加到待处理队列: " + url);
            addPendingRequest(url, params, responseClass, successCallback, errorCallback);
            showNetworkUnavailableMessage();
            return;
        }
        
        // 在主线程调用加载开始回调
        if (loadingCallback != null) {
            mainHandler.post(loadingCallback::onLoadingStart);
        }

        // 在后台线程执行网络请求
        executorService.execute(() -> {
            try {
                Log.d(TAG, "发起POST请求: " + url);
                Log.d(TAG, "请求参数: " + JSONUtil.toJsonStr(params));

                // 使用 HttpRequest 构建请求，添加请求头和表单参数
                String response = HttpRequest.post(url)
                        .headerMap(getCommonHeaders(), true)
                        .form(params)
                        .timeout(TIMEOUT_MS)
                        .execute()
                        .body();
                
                Log.d(TAG, "收到响应: " + response);
                
                // 处理响应
                handleResponse(response, responseClass, successCallback, errorCallback, 
                             url, params, loadingCallback);
                
            } catch (Exception e) {
                Log.e(TAG, "网络请求异常: " + url, e);
                
                // 在主线程回调错误和加载结束
                mainHandler.post(() -> {
                    if (errorCallback != null) {
                        errorCallback.onError("网络请求失败: " + e.getMessage());
                    }
                    if (loadingCallback != null) {
                        loadingCallback.onLoadingEnd();
                    }
                });
                
                // 如果是网络相关异常，添加到待处理队列
                if (isNetworkException(e)) {
                    addPendingRequest(url, params, responseClass, successCallback, errorCallback);
                }
            }
        });
    }

    /**
     * 手动解析JSON数据，避免使用JSONUtil.toBean导致的java.beans相关错误
     */
    @SuppressWarnings("unchecked")
    private <T> T parseDataManually(Object dataObj, Class<T> responseClass) {
        if (dataObj == null) {
            return null;
        }
        Gson gson = new Gson();
        try {
            if (responseClass == String.class) {
                return (T) dataObj.toString();
            }
            
            // 处理CommentResponse.Data类型的特殊情况：后端可能直接返回JSONArray
            if (responseClass.getName().contains("CommentResponse$Data")) {
                if (dataObj instanceof JSONArray) {
                    // 后端直接返回评论数组，需要包装成CommentResponse.Data格式
                    Log.d(TAG, "后端直接返回评论数组，进行格式转换");
                    JSONArray commentsArray = (JSONArray) dataObj;
                    CommentResponse.Data data = new CommentResponse.Data();
                    
                    List<Comment> records = new ArrayList<>();
                    for (Object item : commentsArray) {
                        if (item instanceof JSONObject) {
                            Comment comment = gson.fromJson(JSON.toJSONString(item), Comment.class);
                            records.add(comment);
                        }
                    }
                    
                    data.setRecords(records);
                    data.setTotal(records.size());
                    data.setSize(records.size());
                    data.setCurrent(1);
                    data.setPages(1);
                    
                    return (T) data;
                } else if (dataObj instanceof JSONObject) {
                    // 标准的分页响应格式
                    return (T) parseCommentResponseData((JSONObject) dataObj);
                }
            }
            
            if (dataObj instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) dataObj;
                
                // 处理 LoginResponse.Data
                if (responseClass.getName().contains("LoginResponse$Data")) {
                    return (T) parseLoginData(jsonObj);
                }
                
                // 处理 FootprintMessageResponse.Data
                if (responseClass.getName().contains("FootprintMessageResponse$Data")) {
                    return (T) parseFootprintMessageData(jsonObj);
                }
                
                // 处理 UserInfoResponse
                if (responseClass == UserInfoResponse.class) {
                    return (T) parseUserInfoResponse(jsonObj);
                }
                
                // 处理 AppUpdateInfo
                if (responseClass == AppUpdateInfo.class) {
                    return (T) parseAppUpdateInfo(jsonObj);
                }
            }
            
            // 对于其他类型，尝试简单转换
            return (T) dataObj;
            
        } catch (Exception e) {
            Log.e(TAG, "手动解析JSON数据失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 解析登录响应数据
     */
    private LoginResponse.Data parseLoginData(JSONObject jsonObj) {
        LoginResponse.Data data = new LoginResponse.Data();
        
        // 解析token
        if (jsonObj.containsKey("token")) {
            data.setToken(jsonObj.getStr("token"));
        }
        
        // 解析user对象
        if (jsonObj.containsKey("user")) {
            Object userObj = jsonObj.get("user");
            if (userObj instanceof JSONObject) {
                data.setUser((JSONObject) userObj);
            }
        }
        
        return data;
    }
    

    
    /**
     * 解析应用更新信息
     */
    private AppUpdateInfo parseAppUpdateInfo(JSONObject jsonObj) {
        AppUpdateInfo updateInfo = new AppUpdateInfo();
        
        if (jsonObj.containsKey("versionCode")) {
            updateInfo.setVersionCode(jsonObj.getInt("versionCode"));
        }
        if (jsonObj.containsKey("versionName")) {
            updateInfo.setVersionName(jsonObj.getStr("versionName"));
        }
        if (jsonObj.containsKey("downloadUrl")) {
            updateInfo.setDownloadUrl(jsonObj.getStr("downloadUrl"));
        }
        if (jsonObj.containsKey("updateContent")) {
            updateInfo.setUpdateContent(jsonObj.getStr("updateContent"));
        }
        if (jsonObj.containsKey("forceUpdate")) {
            updateInfo.setForceUpdate(jsonObj.getBool("forceUpdate", false));
        }
        if (jsonObj.containsKey("fileSize")) {
            updateInfo.setFileSize(jsonObj.getLong("fileSize", 0L));
        }
        if (jsonObj.containsKey("md5")) {
            updateInfo.setMd5(jsonObj.getStr("md5"));
        }
        if (jsonObj.containsKey("releaseTime")) {
            updateInfo.setReleaseTime(jsonObj.getStr("releaseTime"));
        }
        
        return updateInfo;
    }
    
    /**
     * 解析足迹消息响应数据
     */
    private FootprintMessageResponse.Data parseFootprintMessageData(JSONObject jsonObj) {
        FootprintMessageResponse.Data data = new FootprintMessageResponse.Data();
        
        if (jsonObj.containsKey("total")) {
            data.setTotal(jsonObj.getInt("total"));
        }
        if (jsonObj.containsKey("current")) {
            data.setCurrent(jsonObj.getInt("current"));
        }
        if (jsonObj.containsKey("size")) {
            data.setSize(jsonObj.getInt("size"));
        }
        if (jsonObj.containsKey("pages")) {
            data.setPages(jsonObj.getInt("pages"));
        }
        
        // 解析records列表
        if (jsonObj.containsKey("records")) {
            Object recordsObj = jsonObj.get("records");
            if (recordsObj instanceof JSONArray) {
                JSONArray recordsArray = (JSONArray) recordsObj;
                List<FootprintMessage> records = new ArrayList<>();
                
                for (Object item : recordsArray) {
                    if (item instanceof JSONObject) {
                        FootprintMessage message = parseFootprintMessage((JSONObject) item);
                        records.add(message);
                    }
                }
                
                data.setRecords(records);
            }
        }
        
        return data;
    }
    
    /**
     * 解析足迹消息数据
     */
    private FootprintMessage parseFootprintMessage(JSONObject jsonObj) {
        FootprintMessage message = new FootprintMessage();
        
        if (jsonObj.containsKey("id")) {
            message.setId(jsonObj.getInt("id"));
        }
        if (jsonObj.containsKey("msgType")) {
            message.setMsgType(jsonObj.getInt("msgType"));
        }
        if (jsonObj.containsKey("textContent")) {
            message.setTextContent(jsonObj.getStr("textContent"));
        }
        if (jsonObj.containsKey("tag")) {
            message.setTag(jsonObj.getStr("tag"));
        }
        if (jsonObj.containsKey("lng")) {
            message.setLng(jsonObj.getDouble("lng"));
        }
        if (jsonObj.containsKey("lat")) {
            message.setLat(jsonObj.getDouble("lat"));
        }
        if (jsonObj.containsKey("localtionTitle")) {
            message.setLocaltionTitle(jsonObj.getStr("localtionTitle"));
        }
        if (jsonObj.containsKey("userId")) {
            message.setUserId(jsonObj.getInt("userId"));
        }
        if (jsonObj.containsKey("userAvatar")) {
            message.setUserAvatar(jsonObj.getStr("userAvatar"));
        }
        if (jsonObj.containsKey("createTime")) {
            message.setCreateTime(jsonObj.getStr("createTime"));
        }
        if (jsonObj.containsKey("createBy")) {
            message.setCreateBy(jsonObj.getStr("createBy"));
        }
        
        // 解析点赞相关字段
        Log.d(TAG, "解析足迹消息，检查点赞相关字段: " + jsonObj.toString());
        
        if (jsonObj.containsKey("likeCount")) {
            int likeCount = jsonObj.getInt("likeCount");
            message.setLikeCount(likeCount);
            Log.d(TAG, "设置点赞数量: " + likeCount);
        } else {
            Log.d(TAG, "后端数据中没有likeCount字段");
        }
        
        if (jsonObj.containsKey("commentCount")) {
            int commentCount = jsonObj.getInt("commentCount");
            message.setCommentCount(commentCount);
            Log.d(TAG, "设置评论数量: " + commentCount);
        } else {
            Log.d(TAG, "后端数据中没有commentCount字段");
        }
        
        if (jsonObj.containsKey("hasLiked")) {
            boolean hasLiked = jsonObj.getBool("hasLiked");
            message.setHasLiked(hasLiked);
            Log.d(TAG, "设置点赞状态: " + hasLiked);
        } else {
            Log.d(TAG, "后端数据中没有hasLiked字段");
        }
        
        // 解析guluFiles字段
        if (jsonObj.containsKey("guluFiles")) {
            JSONArray guluFilesArray = jsonObj.getJSONArray("guluFiles");
            if (guluFilesArray != null && !guluFilesArray.isEmpty()) {
                List<GuluFile> guluFiles = new ArrayList<>();
                for (int i = 0; i < guluFilesArray.size(); i++) {
                    JSONObject fileObj = guluFilesArray.getJSONObject(i);
                    GuluFile guluFile = parseGuluFile(fileObj);
                    if (guluFile != null) {
                        guluFiles.add(guluFile);
                    }
                }
                message.setGuluFiles(guluFiles);
                Log.d(TAG, "解析到 " + guluFiles.size() + " 个文件");
            }
        }
        
        return message;
    }
    
    /**
     * 处理HTTP响应
     * 
     * @param responseBody HTTP响应内容
     * @param responseClass 响应数据类型
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param url 请求URL（用于日志）
     * @param params 请求参数（用于重试）
     * @param <T> 响应数据泛型
     */
    private <T> void handleResponse(String responseBody, Class<T> responseClass,
                                   SuccessCallback<T> successCallback, ErrorCallback errorCallback,
                                   String url, Map<String, Object> params) {
        handleResponse(responseBody, responseClass, successCallback, errorCallback, url, params, null);
    }
    
    private <T> void handleResponse(String responseBody, Class<T> responseClass,
                                   SuccessCallback<T> successCallback, ErrorCallback errorCallback,
                                   String url, Map<String, Object> params, LoadingCallback loadingCallback) {
        try {
            Log.d(TAG, "响应内容: " + responseBody);
            
            // 解析响应数据
            if (StrUtil.isNotBlank(responseBody)) {
                try {
                    // 先解析为JSONObject
                    JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
                    
                    // 提取基本字段
                    int code = jsonResponse.getInt("code", -1);
                    String msg = jsonResponse.getStr("msg", "");
                    
                    if (code == 200) {
                        // 成功响应，解析数据
                        T data = null;
                        
                        // 对于UserInfoResponse，需要传递完整的响应对象
                        if (responseClass == UserInfoResponse.class) {
                            data = parseDataManually(jsonResponse, responseClass);
                        } else {
                            // 对于其他类型，传递data字段
                            Object dataObj = jsonResponse.get("data");
                            if (dataObj != null) {
                                data = parseDataManually(dataObj, responseClass);
                            }
                        }
                        
                        Log.d(TAG, "请求成功: " + url);
                        final T finalData = data;
                        mainHandler.post(() -> {
                            if (successCallback != null) {
                                successCallback.onSuccess(finalData);
                            }
                            if (loadingCallback != null) {
                                loadingCallback.onLoadingEnd();
                            }
                        });
                    } else {
                        // 业务失败
                        String errorMsg = StrUtil.isNotBlank(msg) ? msg : "请求失败，请稍后重试";
                        Log.e(TAG, "业务失败: " + errorMsg + ", 错误码: " + code);
                        Log.e(TAG, "原始响应数据: " + responseBody);
                        
                        mainHandler.post(() -> {
                            if (errorCallback != null) {
                                errorCallback.onError(errorMsg);
                            }
                            if (loadingCallback != null) {
                                loadingCallback.onLoadingEnd();
                            }
                        });
                    }
                } catch (Exception parseException) {
                    Log.e(TAG, "解析响应数据异常: " + parseException.getMessage());
                    Log.e(TAG, "原始响应数据: " + responseBody);
                    
                    mainHandler.post(() -> {
                        if (errorCallback != null) {
                            errorCallback.onError("数据解析失败: " + parseException.getMessage());
                        }
                        if (loadingCallback != null) {
                            loadingCallback.onLoadingEnd();
                        }
                    });
                }
            } else {
                Log.e(TAG, "响应内容为空");
                mainHandler.post(() -> {
                    if (errorCallback != null) {
                        errorCallback.onError("服务器响应为空");
                    }
                    if (loadingCallback != null) {
                        loadingCallback.onLoadingEnd();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "解析响应数据异常", e);
            mainHandler.post(() -> {
                if (errorCallback != null) {
                    errorCallback.onError("数据解析失败: " + e.getMessage());
                }
                if (loadingCallback != null) {
                    loadingCallback.onLoadingEnd();
                }
            });
        }
    }

    /**
     * 判断是否为网络相关异常
     * 
     * @param e 异常
     * @return 是否为网络异常
     */
    private boolean isNetworkException(Exception e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("timeout") ||
                message.contains("connection") ||
                message.contains("network") ||
                message.contains("socket")
        );
    }

    /**
     * 添加待处理请求
     * 
     * @param url 请求URL
     * @param params 请求参数
     * @param responseClass 响应类型
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param <T> 响应数据泛型
     */
    private <T> void addPendingRequest(String url, Map<String, Object> params,
                                      Class<T> responseClass,
                                      SuccessCallback<T> successCallback,
                                      ErrorCallback errorCallback) {
        RequestInfo<T> requestInfo = new RequestInfo<>(url, params, responseClass,
                                                       successCallback, errorCallback);
        pendingRequests.add(requestInfo);
        Log.d(TAG, "添加待处理请求，当前待处理请求数: " + pendingRequests.size());
    }

    /**
     * 重试所有待处理的请求
     */
    private void retryPendingRequests() {
        if (pendingRequests.isEmpty() || isRetryingRequests) {
            return;
        }
        
        isRetryingRequests = true;
        Log.d(TAG, "开始重试待处理请求，数量: " + pendingRequests.size());
        
        // 创建待处理请求的副本
        List<RequestInfo<?>> requestsToRetry = new ArrayList<>(pendingRequests);
        pendingRequests.clear();
        
        // 在主线程上执行重试
        mainHandler.post(() -> {
            for (RequestInfo requestInfo : requestsToRetry) {
                requestInfo.retry();
            }
            isRetryingRequests = false;
        });
    }

    /**
     * 显示网络不可用消息
     */
    private void showNetworkUnavailableMessage() {
        mainHandler.post(() -> {
            // 这里可以显示Toast或其他UI提示
            Log.w(TAG, "网络不可用，请检查网络连接");
        });
    }

    /**
     * 发送验证码接口
     * 
     * @param phone 手机号
     * @param successCallback 成功回调（返回原始响应字符串）
     * @param errorCallback 错误回调
     */
    public void sendVerificationCode(String phone,
                                   SuccessCallback<String> successCallback,
                                   ErrorCallback errorCallback) {
        sendVerificationCode(phone, successCallback, errorCallback, null);
    }
    
    /**
     * 发送验证码接口（带加载回调）
     * 
     * @param phone 手机号
     * @param successCallback 成功回调（返回原始响应字符串）
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    public void sendVerificationCode(String phone,
                                   SuccessCallback<String> successCallback,
                                   ErrorCallback errorCallback,
                                   LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.SEND_VERIFICATION_CODE;
        
        // 准备请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("phone", phone);
        
        // 检查网络状态
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，将验证码请求添加到待处理队列");
            
            // 保存请求信息到待处理队列
            addPendingRequest(url, params, String.class, successCallback, errorCallback);
            
            // 显示网络不可用提示
            showNetworkUnavailableMessage();
            return;
        }
        
        Log.d(TAG, "发送验证码请求: phone=" + phone);
        
        // 执行POST请求
        executePostRequest(url, params, String.class, successCallback, errorCallback, loadingCallback);
    }
    
    /**
     * 短信登录接口
     * 
     * @param phone 手机号
     * @param code 验证码
     * @param deviceId 设备ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void smsLogin(String phone, String code, String deviceId,
                        SuccessCallback<LoginResponse.Data> successCallback,
                        ErrorCallback errorCallback) {
        smsLogin(phone, code, deviceId, successCallback, errorCallback, null);
    }
    
    /**
     * 短信登录接口（带加载回调）
     * 
     * @param phone 手机号
     * @param code 验证码
     * @param deviceId 设备ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    public void smsLogin(String phone, String code, String deviceId,
                        SuccessCallback<LoginResponse.Data> successCallback,
                        ErrorCallback errorCallback,
                        LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.SMS_LOGIN;
        
        // 准备请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("phone", phone);
        params.put("code", code);
        params.put("deviceId", deviceId);
        
        Log.d(TAG, "发起登录请求: phone=" + phone + ", code=" + code + ", deviceId=" + deviceId);
        
        executePostRequest(url, params, LoginResponse.Data.class, successCallback, errorCallback, loadingCallback);
    }

    /**
     * 获取用户信息
     * 
     * @param token 用户token
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void getUserInfo(String token,
                           SuccessCallback<UserInfoResponse> successCallback,
                           ErrorCallback errorCallback) {
        getUserInfo(token, successCallback, errorCallback, null);
    }
    
    /**
     * 获取用户信息（带加载回调）
     * 
     * @param token 用户token
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    public void getUserInfo(String token,
                           SuccessCallback<UserInfoResponse> successCallback,
                           ErrorCallback errorCallback,
                           LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.GET_USER_INFO;

        // 准备请求参数
        Map<String, Object> params = new HashMap<>();
        
        executePostRequest(url, params, UserInfoResponse.class, successCallback, errorCallback, loadingCallback);
    }

    /**
     * 获取足迹动态列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void getFootprintMessages(int page, int size,
                                   SuccessCallback<FootprintMessageResponse.Data> successCallback,
                                   ErrorCallback errorCallback) {
        getFootprintMessages(page, size, successCallback, errorCallback, null);
    }
    
    /**
     * 获取足迹动态列表（带加载回调）
     * 
     * @param page 页码
     * @param size 每页大小
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    public void getFootprintMessages(int page, int size,
                                   SuccessCallback<FootprintMessageResponse.Data> successCallback,
                                   ErrorCallback errorCallback,
                                   LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.FOOTPRINT_MESSAGES;
        
        Map<String, Object> params = new HashMap<>();
        params.put("pageNum", page);
        params.put("pageSize", size);
        
        Log.d(TAG, "获取足迹动态列表: page=" + page + ", size=" + size);
        
        executePostRequest(url, params, FootprintMessageResponse.Data.class, 
                         successCallback, errorCallback, loadingCallback);
    }

    /**
     * 获取地图页mark数据（getMsgListAll接口）
     * 调用参数与getMsgList一致
     * 
     * @param page 页码
     * @param size 每页大小
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void getMsgListAll(int page, int size,
                             SuccessCallback<FootprintMessageResponse.Data> successCallback,
                             ErrorCallback errorCallback) {
        getMsgListAll(page, size, successCallback, errorCallback, null);
    }
    
    /**
     * 获取地图页mark数据（getMsgListAll接口，带加载回调）
     * 调用参数与getMsgList一致
     * 
     * @param page 页码
     * @param size 每页大小
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    public void getMsgListAll(int page, int size,
                             SuccessCallback<FootprintMessageResponse.Data> successCallback,
                             ErrorCallback errorCallback,
                             LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.GET_MSG_LIST_ALL;
        
        Map<String, Object> params = new HashMap<>();
        params.put("pageNum", page);
        params.put("pageSize", size);
        
        Log.d(TAG, "获取地图页mark数据: page=" + page + ", size=" + size);
        
        executePostRequest(url, params, FootprintMessageResponse.Data.class, 
                         successCallback, errorCallback, loadingCallback);
    }

    /**
     * 发布足迹动态
     * 
     * @param publishInfo 发布信息
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void publishFootprint(PublishTrandsInfoPO publishInfo,
                               SuccessCallback<String> successCallback,
                               ErrorCallback errorCallback) {
        publishFootprint(publishInfo, successCallback, errorCallback, null);
    }
    
    /**
     * 发布足迹动态（带加载回调）
     * 
     * @param publishInfo 发布信息
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    public void publishFootprint(PublishTrandsInfoPO publishInfo,
                               SuccessCallback<String> successCallback,
                               ErrorCallback errorCallback,
                               LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.PUBLISH_FOOTPRINT;
        
        // 检查网络状态
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法发布足迹");
            showNetworkUnavailableMessage();
            return;
        }
        
        // 调用加载开始回调
        if (loadingCallback != null) {
            mainHandler.post(() -> loadingCallback.onLoadingStart());
        }
        
        // 在后台线程执行文件上传请求
        executorService.execute(() -> {
            try {
                Log.d(TAG, "发布足迹动态: userId=" + publishInfo.getUserId() + 
                          ", content=" + publishInfo.getContent() + 
                          ", location=" + publishInfo.getLocationInfo() + 
                          ", city=" + publishInfo.getCity());
                
                // 构建multipart请求
                HttpRequest request = HttpRequest.post(url)
                        .headerMap(getCommonHeaders(), true)
                        .timeout(TIMEOUT_MS);
                
                // 添加普通表单字段
                request.form("userId", publishInfo.getUserId());
                request.form("city", publishInfo.getCity()); // 添加城市信息
                request.form("locationInfo", publishInfo.getLocationInfo());
                request.form("type", publishInfo.getType());
                request.form("content", publishInfo.getContent());
                request.form("tag", publishInfo.getTag());
                request.form("lng", publishInfo.getLng());
                request.form("lat", publishInfo.getLat());
                request.form("msgType", publishInfo.getMsgType());
                
                // 添加图片文件
                if (publishInfo.getImages() != null && !publishInfo.getImages().isEmpty()) {
                    for (File imageFile : publishInfo.getImages()) {
                        if (imageFile != null && imageFile.exists()) {
                            request.form("images", imageFile);
                        }
                    }
                }
                
                // 执行请求
                String response = request.execute().body();
                
                Log.d(TAG, "收到响应: " + response);
                
                // 处理响应
                handleResponse(response, String.class, successCallback, errorCallback, 
                             url, new HashMap<>(), loadingCallback);
                
            } catch (Exception e) {
                Log.e(TAG, "发布足迹请求异常: " + url, e);
                
                // 在主线程回调错误
                mainHandler.post(() -> {
                    if (errorCallback != null) {
                        errorCallback.onError("发布足迹失败: " + e.getMessage());
                    }
                    if (loadingCallback != null) {
                        loadingCallback.onLoadingEnd();
                    }
                });
            }
        });
    }

    /**
     * 检查应用更新
     * 
     * @param currentVersionCode 当前应用版本号
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void checkAppUpdate(int currentVersionCode,
                              SuccessCallback<AppUpdateInfo> successCallback,
                              ErrorCallback errorCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.CHECK_APP_UPDATE;
        
        Map<String, Object> params = new HashMap<>();
        params.put("currentVersionCode", currentVersionCode);
        params.put("platform", "android");
        
        Log.d(TAG, "检查应用更新: currentVersionCode=" + currentVersionCode);
        
        executePostRequest(url, params, AppUpdateInfo.class, successCallback, errorCallback);
    }
    

    
    /**
     * 上传头像文件
     * 
     * @param avatarFile 头像文件
     * @param successCallback 成功回调，返回fileName
     * @param errorCallback 错误回调
     */
    public void uploadAvatar(File avatarFile,
                            SuccessCallback<String> successCallback,
                            ErrorCallback errorCallback) {
        String url = BASE_URL + "upload"; // 使用指定的upload接口
        
        // 检查网络状态
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法上传头像");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                mainHandler.post(() -> errorCallback.onError("网络不可用，请检查网络连接"));
            }
            return;
        }
        
        // 在后台线程执行头像上传请求
        executorService.execute(() -> {
            try {
                Log.d(TAG, "上传头像文件: " + avatarFile.getName() + ", 大小: " + avatarFile.length() + " bytes");
                
                // 构建multipart请求
                HttpRequest request = HttpRequest.post(url)
                        .headerMap(getCommonHeaders(), true)
                        .timeout(TIMEOUT_MS);
                
                // 添加文件参数，参数名为file
                request.form("file", avatarFile);
                
                // 执行请求
                String response = request.execute().body();
                
                Log.d(TAG, "头像上传响应: " + response);
                
                // 解析响应，提取fileName
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String fileName = null;
                    
                    // 检查响应格式，可能是直接返回fileName或在data中
                    if (jsonResponse.containsKey("fileName")) {
                        fileName = jsonResponse.getStr("fileName");
                    } else if (jsonResponse.containsKey("data")) {
                        Object dataObj = jsonResponse.get("data");
                        if (dataObj instanceof JSONObject) {
                            JSONObject dataJson = (JSONObject) dataObj;
                            if (dataJson.containsKey("fileName")) {
                                fileName = dataJson.getStr("fileName");
                            }
                        } else if (dataObj instanceof String) {
                            fileName = (String) dataObj;
                        }
                    }
                    
                    final String finalFileName = fileName; // 创建final变量供lambda使用
                    
                    if (finalFileName != null && !finalFileName.isEmpty()) {
                        Log.d(TAG, "头像上传成功，获取到fileName: " + finalFileName);
                        // 在主线程回调成功，返回fileName
                        mainHandler.post(() -> {
                            if (successCallback != null) {
                                successCallback.onSuccess(finalFileName);
                            }
                        });
                    } else {
                        Log.e(TAG, "头像上传响应中未找到fileName字段");
                        mainHandler.post(() -> {
                            if (errorCallback != null) {
                                errorCallback.onError("头像上传失败：响应中未找到fileName");
                            }
                        });
                    }
                } catch (Exception parseException) {
                    Log.e(TAG, "解析头像上传响应失败", parseException);
                    mainHandler.post(() -> {
                        if (errorCallback != null) {
                            errorCallback.onError("头像上传失败：响应解析错误");
                        }
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "头像上传请求异常: " + url, e);
                
                // 在主线程回调错误
                mainHandler.post(() -> {
                    if (errorCallback != null) {
                        errorCallback.onError("头像上传失败: " + e.getMessage());
                    }
                });
            }
        });
    }

    /**
     * 保存用户信息（带加载回调）
     * 
     * @param userInfoJson 完整的用户信息JSON字符串
     * @param avatarFile 头像文件（可为null）
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    public void saveUserInfo(String userInfoJson, File avatarFile,
                            SuccessCallback<String> successCallback,
                            ErrorCallback errorCallback,
                            LoadingCallback loadingCallback) {
        
        // 从用户信息JSON中提取token和nickName
        String nickName = null;
        
        try {
            JSONObject userObj = new JSONObject(userInfoJson);

            if (userObj.containsKey("nickName")) {
                nickName = userObj.getStr("nickName");
            }
        } catch (Exception e) {
            Log.e(TAG, "解析用户信息JSON失败", e);
            if (errorCallback != null) {
                mainHandler.post(() -> errorCallback.onError("用户信息格式错误"));
            }
            return;
        }
        
        // 调用加载开始回调
        if (loadingCallback != null) {
            mainHandler.post(() -> loadingCallback.onLoadingStart());
        }
        
        // 如果有头像文件，先上传头像
        if (avatarFile != null && avatarFile.exists()) {
            uploadAvatar(avatarFile, 
                // 头像上传成功回调
                fileName -> {
                    Log.d(TAG, "头像上传成功，获取到fileName: " + fileName + "，开始保存用户信息");
                    // 头像上传成功后，保存用户信息（包含头像fileName）
                    saveUserInfoWithAvatar(userInfoJson, fileName, successCallback, errorCallback, loadingCallback);
                },
                // 头像上传失败回调
                avatarError -> {
                    Log.e(TAG, "头像上传失败: " + avatarError);
                    // 头像上传失败，但仍然尝试保存用户信息（不包含头像）
                    saveUserInfoWithoutAvatar(userInfoJson, successCallback, errorCallback, loadingCallback);
                }
            );
        } else {
            // 没有头像文件，直接保存用户信息
            saveUserInfoWithoutAvatar(userInfoJson, successCallback, errorCallback, loadingCallback);
        }
    }
    
    /**
     * 保存用户信息（包含头像fileName）
     * 
     * @param userInfoJson 完整的用户信息JSON字符串
     * @param avatarFileName 头像文件名
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    private void saveUserInfoWithAvatar(String userInfoJson, String avatarFileName,
                                       SuccessCallback<String> successCallback,
                                       ErrorCallback errorCallback,
                                       LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.SAVE_USER_INFO;
        
        // 检查网络状态
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法保存用户信息");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                mainHandler.post(() -> errorCallback.onError("网络不可用，请检查网络连接"));
            }
            if (loadingCallback != null) {
                mainHandler.post(() -> loadingCallback.onLoadingEnd());
            }
            return;
        }
        
        // 在后台线程执行用户信息保存请求
        executorService.execute(() -> {
            try {
                Log.d(TAG, "保存用户信息，包含头像: " + avatarFileName);
                
                // 解析用户信息JSON并添加头像fileName
                JSONObject jsonParams = new JSONObject(userInfoJson);
                jsonParams.set("avatar", avatarFileName); // 更新头像fileName
                jsonParams.remove("dept");
                // 获取通用请求头并添加JSON Content-Type
                Map<String, String> headers = getCommonHeaders();
                headers.put("Content-Type", "application/json");
                
                // 构建请求
                HttpRequest request = HttpRequest.post(url)
                        .headerMap(headers, true)
                        .timeout(TIMEOUT_MS)
                        .body(jsonParams.toString());
                
                // 执行请求
                String response = request.execute().body();
                
                Log.d(TAG, "保存用户信息响应: " + response);
                
                // 处理响应
                handleResponse(response, String.class, successCallback, errorCallback, 
                             url, jsonParams, loadingCallback);
                
            } catch (Exception e) {
                Log.e(TAG, "保存用户信息请求异常: " + url, e);
                
                // 在主线程回调错误
                mainHandler.post(() -> {
                    if (errorCallback != null) {
                        errorCallback.onError("保存用户信息失败: " + e.getMessage());
                    }
                    if (loadingCallback != null) {
                        loadingCallback.onLoadingEnd();
                    }
                });
            }
        });
    }
    
    /**
     * 保存用户信息（不包含头像文件）
     * 
     * @param userInfoJson 完整的用户信息JSON字符串
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    private void saveUserInfoWithoutAvatar(String userInfoJson,
                                          SuccessCallback<String> successCallback,
                                          ErrorCallback errorCallback,
                                          LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.SAVE_USER_INFO;
        
        // 检查网络状态
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法保存用户信息");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                mainHandler.post(() -> errorCallback.onError("网络不可用，请检查网络连接"));
            }
            if (loadingCallback != null) {
                mainHandler.post(() -> loadingCallback.onLoadingEnd());
            }
            return;
        }
        
        // 在后台线程执行用户信息保存请求
        executorService.execute(() -> {
            try {
                Log.d(TAG, "保存用户信息（不包含头像）");
                
                // 解析用户信息JSON
                JSONObject jsonParams = new JSONObject(userInfoJson);
                jsonParams.remove("dept");
                // 获取通用请求头并添加JSON Content-Type
                Map<String, String> headers = getCommonHeaders();
                headers.put("Content-Type", "application/json");
                
                // 构建请求
                HttpRequest request = HttpRequest.post(url)
                        .headerMap(headers, true)
                        .timeout(TIMEOUT_MS)
                        .body(jsonParams.toString());
                
                // 执行请求
                String response = request.execute().body();
                
                Log.d(TAG, "保存用户信息响应: " + response);
                
                // 处理响应
                handleResponse(response, String.class, successCallback, errorCallback, 
                             url, jsonParams, loadingCallback);
                
            } catch (Exception e) {
                Log.e(TAG, "保存用户信息请求异常: " + url, e);
                
                // 在主线程回调错误
                mainHandler.post(() -> {
                    if (errorCallback != null) {
                        errorCallback.onError("保存用户信息失败: " + e.getMessage());
                    }
                    if (loadingCallback != null) {
                        loadingCallback.onLoadingEnd();
                    }
                });
            }
        });
    }

    /**
     * 检查应用更新（带加载回调）
     * 
     * @param currentVersionCode 当前应用版本号
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    public void checkAppUpdate(int currentVersionCode,
                              SuccessCallback<AppUpdateInfo> successCallback,
                              ErrorCallback errorCallback,
                              LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.CHECK_APP_UPDATE;
        
        Map<String, Object> params = new HashMap<>();
        params.put("currentVersionCode", currentVersionCode);
        params.put("platform", "android");
        
        Log.d(TAG, "检查应用更新: currentVersionCode=" + currentVersionCode);
        
        executePostRequest(url, params, AppUpdateInfo.class, successCallback, errorCallback, loadingCallback);
    }

    /**
     * 成功回调接口
     * 
     * @param <T> 响应数据类型
     */
    public interface SuccessCallback<T> {
        /**
         * 成功回调
         * 
         * @param data 响应数据
         */
        void onSuccess(T data);
    }

    /**
     * 错误回调接口
     */
    public interface ErrorCallback {
        /**
         * 错误回调
         * 
         * @param errorMessage 错误消息
         */
        void onError(String errorMessage);
    }
    
    /**
     * 加载状态回调接口
     */
    public interface LoadingCallback {
        /**
         * 请求开始时调用
         */
        void onLoadingStart();
        
        /**
         * 请求结束时调用（无论成功或失败）
         */
        void onLoadingEnd();
    }

    /**
     * 解析GuluFile对象
     * 
     * @param jsonObj JSON对象
     * @return GuluFile对象
     */
    private GuluFile parseGuluFile(JSONObject jsonObj) {
        if (jsonObj == null) {
            return null;
        }
        
        GuluFile guluFile = new GuluFile();
        
        if (jsonObj.containsKey("id")) {
            guluFile.setId(jsonObj.getInt("id"));
        }
        if (jsonObj.containsKey("fileName")) {
            guluFile.setFileName(jsonObj.getStr("fileName"));
        }
        if (jsonObj.containsKey("fileType")) {
            guluFile.setFileType(jsonObj.getStr("fileType"));
        }
        if (jsonObj.containsKey("filePath")) {
            guluFile.setFilePath(jsonObj.getStr("filePath"));
        }
        if (jsonObj.containsKey("ofType")) {
            guluFile.setOfType(jsonObj.getStr("ofType"));
        }
        if (jsonObj.containsKey("ofId")) {
            guluFile.setOfId(jsonObj.getInt("ofId"));
        }
        if (jsonObj.containsKey("createBy")) {
            guluFile.setCreateBy(jsonObj.getStr("createBy"));
        }
        if (jsonObj.containsKey("createTime")) {
            guluFile.setCreateTime(jsonObj.getStr("createTime"));
        }
        if (jsonObj.containsKey("updateBy")) {
            guluFile.setUpdateBy(jsonObj.getStr("updateBy"));
        }
        if (jsonObj.containsKey("updateTime")) {
            guluFile.setUpdateTime(jsonObj.getStr("updateTime"));
        }
        if (jsonObj.containsKey("remark")) {
            guluFile.setRemark(jsonObj.getStr("remark"));
        }
        if (jsonObj.containsKey("delFlag")) {
            guluFile.setDelFlag(jsonObj.getStr("delFlag"));
        }
        
        Log.d(TAG, "解析GuluFile: " + guluFile.getFileName() + ", 类型: " + guluFile.getFileType());
        
        return guluFile;
    }
    
    /**
     * 解析用户信息响应数据
     */
    private UserInfoResponse parseUserInfoResponse(JSONObject jsonObj) {
        UserInfoResponse response = new UserInfoResponse();
        
        // 解析基本响应信息
        if (jsonObj.containsKey("code")) {
            response.setCode(jsonObj.getInt("code"));
        }
        
        if (jsonObj.containsKey("msg")) {
            response.setMsg(jsonObj.getStr("msg"));
        }
        
        // 解析data对象
        if (jsonObj.containsKey("data")) {
            Object dataObj = jsonObj.get("data");
            if (dataObj instanceof JSONObject) {
                UserInfoResponse.UserInfoData data = parseUserInfoData((JSONObject) dataObj);
                response.setData(data);
            }
        }
        
        return response;
    }
    
    /**
     * 解析用户信息数据
     */
    private UserInfoResponse.UserInfoData parseUserInfoData(JSONObject jsonObj) {
        UserInfoResponse.UserInfoData data = new UserInfoResponse.UserInfoData();
        
        // 注意：根据实际JSON结构，token可能不在data级别，而在user级别或其他位置
        // 这里先尝试在data级别查找token
        if (jsonObj.containsKey("token")) {
            data.setToken(jsonObj.getStr("token"));
        }
        
        // 解析user对象
        if (jsonObj.containsKey("user")) {
            Object userObj = jsonObj.get("user");
            if (userObj instanceof JSONObject) {
                // 将JSONObject转换为cn.hutool.json.JSONObject
                JSONObject userJsonObj = (JSONObject) userObj;
                cn.hutool.json.JSONObject hutoolUserObj = new cn.hutool.json.JSONObject();
                
                // 复制所有字段到hutool的JSONObject
                for (String key : userJsonObj.keySet()) {
                    hutoolUserObj.set(key, userJsonObj.get(key));
                }
                
                data.setUser(hutoolUserObj);
            }
        }
        
        return data;
    }
    
    /**
     * 点赞/取消点赞接口
     * 
     * @param msgId 消息ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void toggleLike(Integer msgId,
                          SuccessCallback<String> successCallback,
                          ErrorCallback errorCallback) {
        toggleLike(msgId, successCallback, errorCallback, null);
    }
    
    /**
     * 点赞/取消点赞接口（带加载回调）
     * 
     * @param msgId 消息ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    public void toggleLike(Integer msgId,
                          SuccessCallback<String> successCallback,
                          ErrorCallback errorCallback,
                          LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.TOGGLE_LIKE;
        
        // 准备请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("msgId", msgId);
        
        // 检查网络状态
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，将点赞请求添加到待处理队列");
            
            // 保存请求信息到待处理队列
            addPendingRequest(url, params, String.class, successCallback, errorCallback);
            
            // 显示网络不可用提示
            showNetworkUnavailableMessage();
            return;
        }
        
        Log.d(TAG, "发送点赞请求: msgId=" + msgId);
        
        // 执行POST请求
        executePostRequest(url, params, String.class, successCallback, errorCallback, loadingCallback);
    }

    /**
     * 添加评论接口
     * 
     * @param msgId 消息ID
     * @param content 评论内容
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void addComment(Integer msgId, String content,
                          SuccessCallback<String> successCallback,
                          ErrorCallback errorCallback) {
        addComment(msgId, null, content, successCallback, errorCallback, null);
    }
    
    /**
     * 添加评论接口（回复评论）
     * 
     * @param msgId 消息ID
     * @param parentId 父评论ID（回复评论时使用）
     * @param content 评论内容
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void addComment(Integer msgId, Long parentId, String content,
                          SuccessCallback<String> successCallback,
                          ErrorCallback errorCallback) {
        addComment(msgId, parentId, content, successCallback, errorCallback, null);
    }
    
    /**
     * 添加评论接口（带加载回调）
     * 
     * @param msgId 消息ID
     * @param parentId 父评论ID（回复评论时使用，可为null）
     * @param content 评论内容
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载状态回调
     */
    public void addComment(Integer msgId, Long parentId, String content,
                          SuccessCallback<String> successCallback,
                          ErrorCallback errorCallback,
                          LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.ADD_COMMENT;
        
        // 准备请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("msgId", msgId);
        params.put("content", content);
        if(ObjectUtil.isNotEmpty(parentId)){
            params.put("parentId", parentId);
        }
        // 检查网络状态
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，将评论请求添加到待处理队列");
            
            // 保存请求信息到待处理队列
            addPendingRequest(url, params, String.class, successCallback, errorCallback);
            
            // 显示网络不可用提示
            showNetworkUnavailableMessage();
            return;
        }
        
        Log.d(TAG, "发送评论请求: msgId=" + msgId + ", content=" + content + 
                   (parentId != null ? ", parentId=" + parentId : ""));
        
        // 执行POST请求
        executePostRequest(url, params, String.class, successCallback, errorCallback, loadingCallback);
    }

    /**
     * 获取评论列表
     * @param msgId 足迹消息ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void getCommentList(Integer msgId,
                              SuccessCallback<CommentResponse.Data> successCallback,
                              ErrorCallback errorCallback) {
        getCommentList(msgId, successCallback, errorCallback, null);
    }

    /**
     * 获取评论列表（带加载回调）
     * @param msgId 足迹消息ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载回调
     */
    public void getCommentList(Integer msgId,
                              SuccessCallback<CommentResponse.Data> successCallback,
                              ErrorCallback errorCallback,
                              LoadingCallback loadingCallback) {
        String url = BASE_URL + ApiConfig.Endpoints.GET_COMMENT_LIST;
        
        // 准备请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("msgId", msgId);
        
        // 检查网络状态
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，将评论列表请求添加到待处理队列");
            
            // 保存请求信息到待处理队列
            addPendingRequest(url, params, CommentResponse.Data.class, successCallback, errorCallback);
            
            // 显示网络不可用提示
            showNetworkUnavailableMessage();
            return;
        }
        
        Log.d(TAG, "获取评论列表，msgId: " + msgId);
        
        // 执行POST请求
        executePostRequest(url, params, CommentResponse.Data.class, successCallback, errorCallback, loadingCallback);
    }

    /**
     * 解析评论响应数据
     */
    private CommentResponse.Data parseCommentResponseData(JSONObject jsonObj) {
        CommentResponse.Data data = new CommentResponse.Data();
        
        if (jsonObj.containsKey("total")) {
            data.setTotal(jsonObj.getInt("total"));
        }
        if (jsonObj.containsKey("current")) {
            data.setCurrent(jsonObj.getInt("current"));
        }
        if (jsonObj.containsKey("size")) {
            data.setSize(jsonObj.getInt("size"));
        }
        if (jsonObj.containsKey("pages")) {
            data.setPages(jsonObj.getInt("pages"));
        }
        
        // 解析records列表
        if (jsonObj.containsKey("records")) {
            Object recordsObj = jsonObj.get("records");
            if (recordsObj instanceof JSONArray) {
                JSONArray recordsArray = (JSONArray) recordsObj;
                List<Comment> records = new ArrayList<>();
                
                for (Object item : recordsArray) {
                    if (item instanceof JSONObject) {
                        Comment comment = parseComment((JSONObject) item);
                        records.add(comment);
                    }
                }
                
                data.setRecords(records);
            }
        }
        
        return data;
    }
    
    /**
     * 解析评论数据
     */
    private Comment parseComment(JSONObject jsonObj) {
        Comment comment = new Comment();
        
        // 解析基本字段，匹配数据库表结构
        if (jsonObj.containsKey("id")) {
            comment.setId(jsonObj.getInt("id"));
        }
        if (jsonObj.containsKey("msgId") || jsonObj.containsKey("msg_id")) {
            String key = jsonObj.containsKey("msgId") ? "msgId" : "msg_id";
            comment.setMsgId(jsonObj.getInt(key));
        }
        if (jsonObj.containsKey("content")) {
            comment.setContent(jsonObj.getStr("content"));
        }
        if (jsonObj.containsKey("userId") || jsonObj.containsKey("user_id")) {
            String key = jsonObj.containsKey("userId") ? "userId" : "user_id";
            comment.setUserId(jsonObj.getInt(key));
        }
        if (jsonObj.containsKey("userName") || jsonObj.containsKey("user_name")) {
            String key = jsonObj.containsKey("userName") ? "userName" : "user_name";
            comment.setUserName(jsonObj.getStr(key));
        }
        if (jsonObj.containsKey("userAvatar") || jsonObj.containsKey("user_avatar")) {
            String key = jsonObj.containsKey("userAvatar") ? "userAvatar" : "user_avatar";
            comment.setUserAvatar(jsonObj.getStr(key));
        }
        if (jsonObj.containsKey("parentId") || jsonObj.containsKey("parent_id")) {
            String key = jsonObj.containsKey("parentId") ? "parentId" : "parent_id";
            comment.setParentId(jsonObj.getInt(key));
        }
        if (jsonObj.containsKey("createTime") || jsonObj.containsKey("create_time")) {
            String key = jsonObj.containsKey("createTime") ? "createTime" : "create_time";
            comment.setCreateTime(jsonObj.getStr(key));
        }
        
        // 解析数据库表中的其他字段
        if (jsonObj.containsKey("delFlag") || jsonObj.containsKey("del_flag")) {
            String key = jsonObj.containsKey("delFlag") ? "delFlag" : "del_flag";
            comment.setDelFlag(jsonObj.getStr(key));
        }
        if (jsonObj.containsKey("createBy") || jsonObj.containsKey("create_by")) {
            String key = jsonObj.containsKey("createBy") ? "createBy" : "create_by";
            comment.setCreateBy(jsonObj.getStr(key));
        }
        if (jsonObj.containsKey("updateBy") || jsonObj.containsKey("update_by")) {
            String key = jsonObj.containsKey("updateBy") ? "updateBy" : "update_by";
            comment.setUpdateBy(jsonObj.getStr(key));
        }
        if (jsonObj.containsKey("updateTime") || jsonObj.containsKey("update_time")) {
            String key = jsonObj.containsKey("updateTime") ? "updateTime" : "update_time";
            comment.setUpdateTime(jsonObj.getStr(key));
        }
        if (jsonObj.containsKey("remark")) {
            comment.setRemark(jsonObj.getStr("remark"));
        }
        
        return comment;
    }

    /**
     * 请求信息类，用于保存请求的详细信息
     */
    private class RequestInfo<T> {
        private final String url;
        private final Map<String, Object> params;
        private final Class<T> responseClass;
        private final SuccessCallback<T> successCallback;
        private final ErrorCallback errorCallback;
        
        public RequestInfo(String url, Map<String, Object> params, Class<T> responseClass,
                          SuccessCallback<T> successCallback, ErrorCallback errorCallback) {
            this.url = url;
            this.params = params != null ? new HashMap<>(params) : null;
            this.responseClass = responseClass;
            this.successCallback = successCallback;
            this.errorCallback = errorCallback;
        }
        
        public void retry() {
            Log.d(TAG, "重试请求: " + url);
            executePostRequest(url, params, responseClass, successCallback, errorCallback);
        }
    }
}