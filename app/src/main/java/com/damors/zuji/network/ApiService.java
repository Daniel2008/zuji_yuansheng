package com.damors.zuji.network;

import com.damors.zuji.model.AppUpdateInfo;
import com.damors.zuji.model.CommentModel;
import com.damors.zuji.model.UserInfoResponse;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.model.response.FootprintMessageResponse;
import com.damors.zuji.model.response.LoginResponse;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

/**
 * Retrofit API接口定义
 * 定义所有的网络请求接口，与HutoolApiService保持一致
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
public interface ApiService {

    /**
     * 发送验证码
     * 
     * @param phone 手机号
     * @return 发送响应
     */
    @FormUrlEncoded
    @POST("sendMsg")
    Call<BaseResponse<JSONObject>> sendVerificationCode(
            @Field("phone") String phone
    );

    /**
     * 短信登录
     * 
     * @param phone 手机号
     * @param code 验证码
     * @param deviceId 设备ID
     * @return 登录响应
     */
    @FormUrlEncoded
    @POST("smsLogin")
    Call<BaseResponse<LoginResponse.Data>> smsLogin(
            @Field("phone") String phone,
            @Field("code") String code,
            @Field("deviceId") String deviceId
    );

    /**
     * 获取用户信息
     * 
     * @return 用户信息响应
     */
    @POST("getUserInfo")
    Call<UserInfoResponse> getUserInfo();

    /**
     * 保存用户信息
     * 
     * @param userInfo 用户信息JSON
     * @return 保存响应
     */
    @POST("saveUserInfo")
    Call<BaseResponse<String>> saveUserInfo(@Body RequestBody userInfo);

    /**
     * 上传头像
     * 
     * @param file 头像文件
     * @return 上传响应
     */
    @Multipart
    @POST("upload")
    Call<BaseResponse<String>> uploadAvatar(@Part MultipartBody.Part file);

    /**
     * 发布足迹
     * 
     * @param publishInfo 发布信息
     * @param images 图片文件列表
     * @return 发布响应
     */
    @Multipart
    @POST("publishMsg")
    Call<BaseResponse<JSONObject>> publishFootprint(
            @PartMap Map<String, RequestBody> publishInfo,
            @Part List<MultipartBody.Part> images
    );

    /**
     * 获取足迹消息列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 足迹消息列表响应
     */
    @FormUrlEncoded
    @POST("getMsgList")
    Call<BaseResponse<FootprintMessageResponse.Data>> getFootprintMessages(
            @Field("pageNum") int pageNum,
            @Field("pageSize") int pageSize
    );

    /**
     * 获取地图页mark数据
     * 
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 地图mark数据响应
     */
    @FormUrlEncoded
    @POST("getMsgListAll")
    Call<BaseResponse<FootprintMessageResponse.Data>> getMsgListAll(
            @Field("pageNum") int pageNum,
            @Field("pageSize") int pageSize
    );

    /**
     * 获取评论列表
     * 
     * @param msgId 消息ID
     * @return 评论列表响应
     */
    @FormUrlEncoded
    @POST("getCommentList")
    Call<BaseResponse<List<CommentModel>>> getCommentList(@Field("msgId") Integer msgId);

    /**
     * 添加评论
     * 
     * @param msgId 消息ID
     * @param content 评论内容
     * @param parentId 父评论ID（可选）
     * @return 添加响应
     */
    @FormUrlEncoded
    @POST("addComment")
    Call<BaseResponse<JSONObject>> addComment(
            @Field("msgId") Integer msgId,
            @Field("content") String content,
            @Field("parentId") Long parentId
    );

    /**
     * 删除评论
     * @param commentId 评论ID
     * @return 删除响应
     */
    @FormUrlEncoded
    @POST("deleteComment")
    Call<BaseResponse<String>> deleteComment(
            @Field("commentId") Integer commentId
    );

    /**
     * 删除足迹
     * 
     * @param msgId 足迹消息ID
     * @return 删除响应
     */
    @FormUrlEncoded
    @POST("deleteMsg")
    Call<BaseResponse<String>> deleteFootprint(@Field("msgId") Integer msgId);

    /**
     * 点赞/取消点赞
     * 
     * @param msgId 消息ID
     * @return 点赞响应
     */
    @FormUrlEncoded
    @POST("toggleLike")
    Call<BaseResponse<JSONObject>> toggleLike(@Field("msgId") Integer msgId);

    /**
     * 检查应用更新
     * 
     * @param currentVersionCode 当前版本号
     * @param platform 平台（android）
     * @return 更新信息响应
     */
    @FormUrlEncoded
    @POST("checkAppUpdate")
    Call<BaseResponse<AppUpdateInfo>> checkAppUpdate(
            @Field("currentVersionCode") int currentVersionCode,
            @Field("platform") String platform
    );

}