package com.damors.zuji.network;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.damors.zuji.model.response.BaseResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 使用Gson解析JSON响应的Volley请求
 * @param <T> 响应数据类型
 * @param <D> 响应数据内部data字段类型
 */
public class GsonRequest<T extends BaseResponse<D>, D> extends Request<D> {
    private final Gson gson = new Gson();
    private final Response.Listener<D> listener;
    private final Map<String, String> headers;
    private final Map<String, String> params;
    private final Type responseType;
    private final Type dataType;

    /**
     * 构造函数
     *
     * @param method 请求方法
     * @param url 请求URL
     * @param responseType 响应类型
     * @param dataType 数据类型
     * @param headers 请求头
     * @param params 请求参数
     * @param listener 成功监听器
     * @param errorListener 错误监听器
     */
    public GsonRequest(int method, String url, Type responseType, Type dataType,
                       @Nullable Map<String, String> headers, @Nullable Map<String, String> params,
                       Response.Listener<D> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.responseType = responseType;
        this.dataType = dataType;
        this.headers = headers;
        this.params = params;
        this.listener = listener;
    }

    /**
     * 简化的构造函数，不需要请求头
     */
    public GsonRequest(int method, String url, Type responseType, Type dataType,
                       @Nullable Map<String, String> params,
                       Response.Listener<D> listener, Response.ErrorListener errorListener) {
        this(method, url, responseType, dataType, null, params, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() {
        try {
            return headers != null ? headers : super.getHeaders();
        } catch (AuthFailureError e) {
            return headers != null ? headers : new HashMap<>();
        }
    }

    @Override
    protected Map<String, String> getParams() {
        return params;
    }

    @Override
    protected Response<D> parseNetworkResponse(NetworkResponse response) {
        try {
            // 记录原始响应数据
            String json = new String(response.data, "UTF-8");
            android.util.Log.d("GsonRequest", "收到响应: " + json);

            // 检查响应数据是否为空
            if (response.data == null || response.data.length == 0) {
                android.util.Log.e("GsonRequest", "空响应数据");
                return Response.error(new ParseError(new Exception("服务器返回空响应")));
            }

            // 解析响应
            T baseResponse = gson.fromJson(json, responseType);

            // 检查解析结果
            if (baseResponse == null) {
                android.util.Log.e("GsonRequest", "解析为null的响应: " + json);
                return Response.error(new ParseError(new Exception("解析响应失败")));
            }

            // 检查业务状态
            if (!baseResponse.isSuccess()) {
                String errorMsg = baseResponse.getMsg();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = "请求失败，请稍后重试";
                }
                // 记录详细的错误信息，包括错误码
                String detailedError = "业务失败 [code: " + baseResponse.getCode() + ", msg: " + errorMsg + "]";
                android.util.Log.e("GsonRequest", detailedError);
                android.util.Log.e("GsonRequest", "服务器响应原始数据: " + json);
                return Response.error(new ParseError(new Exception(errorMsg)));
            }

            // 检查数据字段
            D data = baseResponse.getData();
            if (data == null) {
                android.util.Log.w("GsonRequest", "成功响应但数据为空: " + json);
                return Response.error(new ParseError(new Exception("未获取到数据")));
            }

            return Response.success(data, HttpHeaderParser.parseCacheHeaders(response));

        } catch (UnsupportedEncodingException e) {
            android.util.Log.e("GsonRequest", "编码错误", e);
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            android.util.Log.e("GsonRequest", "JSON语法错误", e);
            return Response.error(new ParseError(new Exception("数据格式错误")));
        } catch (Exception e) {
            android.util.Log.e("GsonRequest", "解析错误", e);
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(D response) {
        listener.onResponse(response);
    }
}
