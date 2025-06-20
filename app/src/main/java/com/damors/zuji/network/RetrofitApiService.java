package com.damors.zuji.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.damors.zuji.ZujiApp;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.AppUpdateInfo;
import com.damors.zuji.model.CommentModel;
import com.damors.zuji.model.PublishTrandsInfoPO;
import com.damors.zuji.model.UserInfoResponse;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.model.response.FootprintMessageResponse;
import com.damors.zuji.model.response.LoginResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 使用Retrofit重构的API服务类
 * 提供网络请求功能，支持自动重试、网络状态监控等特性
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
public class RetrofitApiService {
    private static final String TAG = "RetrofitApiService";
    
    private static RetrofitApiService instance;
    private Context context;
    private ApiService apiService;
    private NetworkStateMonitor networkStateMonitor;
    private Handler mainHandler;
    
    // 保存失败的请求信息，以便在网络恢复时重试
    private List<RequestInfo> pendingRequests = new ArrayList<>();
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
    private RetrofitApiService(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化Retrofit
        initRetrofit();
        
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
    public static synchronized RetrofitApiService getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitApiService(context);
        }
        return instance;
    }

    /**
     * 初始化Retrofit
     */
    private void initRetrofit() {
        // 创建日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // 创建请求头拦截器
        Interceptor headerInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Request.Builder requestBuilder = originalRequest.newBuilder();
                
                // 添加通用请求头
                Map<String, String> headers = getCommonHeaders();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
                
                return chain.proceed(requestBuilder.build());
            }
        };
        
        // 创建OkHttpClient
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(ApiConfig.TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(ApiConfig.TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(ApiConfig.TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .addInterceptor(headerInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();
        
        // 创建Gson实例
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        
        // 创建Retrofit实例
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiConfig.getBaseUrl())
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        
        // 创建API服务
        apiService = retrofit.create(ApiService.class);
    }

    /**
     * 获取通用请求头
     * 
     * @return 请求头Map
     */
    private Map<String, String> getCommonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("User-Agent", "ZujiApp/1.0");
        
        // 添加token（如果已登录）
        UserManager userManager = UserManager.getInstance();
        if (userManager.isLoggedIn()) {
            String token = userManager.getToken();
            if (!TextUtils.isEmpty(token)) {
                headers.put("Authorization", "Bearer " + token);
            }
        }
        
        return headers;
    }

    /**
     * 检查网络状态
     * 
     * @return 网络是否可用
     */
    private boolean isNetworkAvailable() {
        return networkStateMonitor != null && networkStateMonitor.isNetworkAvailable();
    }

    /**
     * 显示网络不可用提示
     */
    private void showNetworkUnavailableMessage() {
        mainHandler.post(() -> {
            Log.w(TAG, "网络不可用，请检查网络连接");
        });
    }

    /**
     * 重试待处理的请求
     */
    private void retryPendingRequests() {
        if (pendingRequests.isEmpty() || isRetryingRequests) {
            return;
        }
        
        isRetryingRequests = true;
        Log.d(TAG, "开始重试待处理请求，数量: " + pendingRequests.size());
        
        // 创建待处理请求的副本
        List<RequestInfo> requestsToRetry = new ArrayList<>(pendingRequests);
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
     * 通用的Retrofit回调处理
     * 
     * @param <T> 响应数据类型
     */
    private abstract class BaseCallback<T> implements Callback<T> {
        protected SuccessCallback<T> successCallback;
        protected ErrorCallback errorCallback;
        protected LoadingCallback loadingCallback;
        
        public BaseCallback(SuccessCallback<T> successCallback, ErrorCallback errorCallback) {
            this.successCallback = successCallback;
            this.errorCallback = errorCallback;
        }
        
        public BaseCallback(SuccessCallback<T> successCallback, ErrorCallback errorCallback, LoadingCallback loadingCallback) {
            this.successCallback = successCallback;
            this.errorCallback = errorCallback;
            this.loadingCallback = loadingCallback;
        }
        
        @Override
        public void onResponse(Call<T> call, retrofit2.Response<T> response) {
            mainHandler.post(() -> {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingEnd();
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    if (successCallback != null) {
                        successCallback.onSuccess(response.body());
                    }
                } else {
                    String errorMsg = "请求失败: " + response.code();
                    if (errorCallback != null) {
                        errorCallback.onError(errorMsg);
                    }
                }
            });
        }
        
        @Override
        public void onFailure(Call<T> call, Throwable t) {
            Log.e(TAG, "网络请求失败", t);
            
            mainHandler.post(() -> {
                if (loadingCallback != null) {
                    loadingCallback.onLoadingEnd();
                }
                
                if (errorCallback != null) {
                    errorCallback.onError("网络请求失败: " + t.getMessage());
                }
            });
        }
    }

    // ==================== 公共接口方法 ====================

    /**
     * 发送验证码
     * 
     * @param phone 手机号
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void sendVerificationCode(String phone,
                                   SuccessCallback<BaseResponse<JSONObject>> successCallback,
                                   ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法发送验证码");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<JSONObject>> call = apiService.sendVerificationCode(phone);
        call.enqueue(new BaseCallback<BaseResponse<JSONObject>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 短信登录
     * 
     * @param phone 手机号
     * @param code 验证码
     * @param deviceId 设备ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void smsLogin(String phone, String code, String deviceId,
                        SuccessCallback<BaseResponse<LoginResponse.Data>> successCallback,
                        ErrorCallback errorCallback) {
        smsLogin(phone, code, deviceId, successCallback, errorCallback, null);
    }

    /**
     * 短信登录（带加载回调）
     * 
     * @param phone 手机号
     * @param code 验证码
     * @param deviceId 设备ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     * @param loadingCallback 加载回调
     */
    public void smsLogin(String phone, String code, String deviceId,
                        SuccessCallback<BaseResponse<LoginResponse.Data>> successCallback,
                        ErrorCallback errorCallback,
                        LoadingCallback loadingCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法执行登录");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        if (loadingCallback != null) {
            loadingCallback.onLoadingStart();
        }
        
        Call<BaseResponse<LoginResponse.Data>> call = apiService.smsLogin(phone, code, deviceId);
        call.enqueue(new BaseCallback<BaseResponse<LoginResponse.Data>>(successCallback, errorCallback, loadingCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 获取用户信息
     * 
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void getUserInfo(SuccessCallback<UserInfoResponse> successCallback,
                           ErrorCallback errorCallback) {
        
        Call<UserInfoResponse> call = apiService.getUserInfo();
        call.enqueue(new BaseCallback<UserInfoResponse>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 保存用户信息
     * 
     * @param userInfo 用户信息RequestBody
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void saveUserInfo(RequestBody userInfo,
                            SuccessCallback<BaseResponse<String>> successCallback,
                            ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法保存用户信息");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<String>> call = apiService.saveUserInfo(userInfo);
        call.enqueue(new BaseCallback<BaseResponse<String>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 上传头像
     * 
     * @param avatarFile 头像文件
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void uploadAvatar(File avatarFile,
                            SuccessCallback<BaseResponse<String>> successCallback,
                            ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法上传头像");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        // 创建RequestBody
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), avatarFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", avatarFile.getName(), requestFile);
        
        Call<BaseResponse<String>> call = apiService.uploadAvatar(body);
        call.enqueue(new BaseCallback<BaseResponse<String>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 发布足迹
     * 
     * @param publishInfo 发布信息
     * @param images 图片文件列表
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void publishFootprint(Map<String, RequestBody> publishInfo,
                                 List<MultipartBody.Part> images,
                                 SuccessCallback<BaseResponse<JSONObject>> successCallback,
                                 ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法发布足迹");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<JSONObject>> call = apiService.publishFootprint(publishInfo, images);
        call.enqueue(new BaseCallback<BaseResponse<JSONObject>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 获取足迹消息列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void getFootprintMessages(int pageNum, int pageSize,
                                    SuccessCallback<BaseResponse<FootprintMessageResponse.Data>> successCallback,
                                    ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法获取足迹消息列表");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<FootprintMessageResponse.Data>> call = apiService.getFootprintMessages(pageNum, pageSize);
        call.enqueue(new BaseCallback<BaseResponse<FootprintMessageResponse.Data>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 获取地图页mark数据
     * 
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void getMsgListAll(int pageNum, int pageSize,
                             SuccessCallback<BaseResponse<FootprintMessageResponse.Data>> successCallback,
                             ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法获取地图页mark数据");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<FootprintMessageResponse.Data>> call = apiService.getMsgListAll(pageNum, pageSize);
        call.enqueue(new BaseCallback<BaseResponse<FootprintMessageResponse.Data>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 获取评论列表
     * 
     * @param msgId 消息ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void getCommentList(Integer msgId,
                              SuccessCallback<BaseResponse<List<CommentModel>>> successCallback,
                              ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法获取评论列表");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<List<CommentModel>>> call = apiService.getCommentList(msgId);
        call.enqueue(new BaseCallback<BaseResponse<List<CommentModel>>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 添加评论
     * 
     * @param msgId 消息ID
     * @param content 评论内容
     * @param parentId 父评论ID（可选）
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void addComment(Integer msgId, String content, Long parentId,
                          SuccessCallback<BaseResponse<JSONObject>> successCallback,
                          ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法添加评论");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<JSONObject>> call = apiService.addComment(msgId, content, parentId);
        call.enqueue(new BaseCallback<BaseResponse<JSONObject>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 删除评论
     * 
     * @param commentId 评论ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void deleteComment(Integer commentId,
                             SuccessCallback<BaseResponse<String>> successCallback,
                             ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法删除评论");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<String>> call = apiService.deleteComment(commentId);
        call.enqueue(new BaseCallback<BaseResponse<String>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 删除足迹
     * 
     * @param msgId 足迹消息ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void deleteFootprint(Integer msgId,
                               SuccessCallback<BaseResponse<String>> successCallback,
                               ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法删除足迹");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<String>> call = apiService.deleteFootprint(msgId);
        call.enqueue(new BaseCallback<BaseResponse<String>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 点赞/取消点赞
     * 
     * @param msgId 消息ID
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void toggleLike(Integer msgId,
                          SuccessCallback<BaseResponse<JSONObject>> successCallback,
                          ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法执行点赞操作");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<JSONObject>> call = apiService.toggleLike(msgId);
        call.enqueue(new BaseCallback<BaseResponse<JSONObject>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }

    /**
     * 检查应用更新
     * 
     * @param currentVersionCode 当前版本号
     * @param platform 平台（android）
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    public void checkAppUpdate(int currentVersionCode, String platform,
                              SuccessCallback<BaseResponse<AppUpdateInfo>> successCallback,
                              ErrorCallback errorCallback) {
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "网络不可用，无法检查更新");
            showNetworkUnavailableMessage();
            if (errorCallback != null) {
                errorCallback.onError("网络不可用，请检查网络连接");
            }
            return;
        }
        
        Call<BaseResponse<AppUpdateInfo>> call = apiService.checkAppUpdate(currentVersionCode, platform);
        call.enqueue(new BaseCallback<BaseResponse<AppUpdateInfo>>(successCallback, errorCallback) {
            // 使用父类的默认实现
        });
    }



    // ==================== 回调接口定义 ====================

    /**
     * 成功回调接口
     * 
     * @param <T> 数据类型
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
     * 加载回调接口
     */
    public interface LoadingCallback {
        /**
         * 加载开始
         */
        void onLoadingStart();

        /**
         * 加载结束
         */
        void onLoadingEnd();
    }

    // ==================== 内部类 ====================

    /**
     * 请求信息类，用于保存失败的请求以便重试
     */
    private static class RequestInfo {
        // 这里可以根据需要添加重试逻辑
        public void retry() {
            // 重试逻辑实现
            Log.d(TAG, "重试请求");
        }
    }
}