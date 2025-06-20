package com.damors.zuji;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.response.LoginResponse;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.utils.DeviceUtils;
import com.damors.zuji.utils.LoadingDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;
import com.google.gson.Gson;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText phoneEditText;
    private TextInputEditText verificationCodeEditText;
    private MaterialButton getVerificationCodeButton;
    private TextView countdownTextView;
    private MaterialButton loginButton;
    private CountDownTimer countDownTimer;
    private RetrofitApiService apiService;
    private LoadingDialog loadingDialog; // 加载弹窗

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);
        // 初始化视图
        initViews();
        // 初始化API服务
        apiService = RetrofitApiService.getInstance(this);
        
        // 初始化加载弹窗
        loadingDialog = new LoadingDialog(this);
        // 设置点击事件
        setupClickListeners();
    }

    private void initViews() {
        phoneEditText = findViewById(R.id.phoneEditText);
        verificationCodeEditText = findViewById(R.id.verificationCodeEditText);
        getVerificationCodeButton = findViewById(R.id.getVerificationCodeButton);
        countdownTextView = findViewById(R.id.countdownTextView);
        loginButton = findViewById(R.id.loginButton);
    }

    private void setupClickListeners() {
        // 获取验证码按钮点击事件
        getVerificationCodeButton.setOnClickListener(v -> {
            String phone = phoneEditText.getText().toString().trim();
            if (validatePhone(phone)) {
                sendVerificationCode(phone);
            }
        });

        // 登录按钮点击事件
        loginButton.setOnClickListener(v -> {
            String phone = phoneEditText.getText().toString().trim();
            String code = verificationCodeEditText.getText().toString().trim();
            if (validateInputs(phone, code)) {
                performLogin(phone, code);
            }
        });
    }

    boolean validatePhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phone.length() != 11) {
            Toast.makeText(this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    boolean validateInputs(String phone, String code) {
        if (!validatePhone(phone)) {
            return false;
        }
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (code.length() != 6) {
            Toast.makeText(this, "请输入6位验证码", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    void sendVerificationCode(String phone) {
        // 显示加载状态
        getVerificationCodeButton.setEnabled(false);

        // 调用发送验证码API
        apiService.sendVerificationCode(phone,
                response -> {
                    // 记录原始响应
                    Log.d("LoginActivity", "验证码响应: " + response.toString());

                    if (response.getCode() == 200) {
                        // 验证码发送成功，开始倒计时
                        Toast.makeText(LoginActivity.this, "验证码已发送", Toast.LENGTH_SHORT).show();
                        startCountdown();
                    } else {
                        // 根据响应消息判断
                        String msg = response.getMsg() != null ? response.getMsg() : "验证码发送失败";
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                        getVerificationCodeButton.setEnabled(true);
                    }
                },
                new RetrofitApiService.ErrorCallback() {
                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        getVerificationCodeButton.setEnabled(true);
                        Log.e("LoginActivity", "发送验证码失败: " + errorMessage);
                    }
                });
    }

    private void startCountdown() {
        // 隐藏按钮，显示倒计时文本
        getVerificationCodeButton.setVisibility(View.INVISIBLE);
        countdownTextView.setVisibility(View.VISIBLE);

        // 创建并启动60秒倒计时
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownTextView.setText(millisUntilFinished / 1000 + "秒后可重新获取");
            }

            @Override
            public void onFinish() {
                // 倒计时结束，恢复按钮状态
                getVerificationCodeButton.setVisibility(View.VISIBLE);
                countdownTextView.setVisibility(View.INVISIBLE);
                getVerificationCodeButton.setEnabled(true);
            }
        }.start();
    }

    /**
     * 执行登录操作
     * 修复重复登录问题：统一使用UserManager管理用户数据
     * 
     * @param phone 手机号
     * @param code 验证码
     */
    void performLogin(String phone, String code) {
        // 显示加载状态
        loginButton.setEnabled(false);
        loadingDialog.show("正在登录..."); // 显示登录等待弹窗
        Log.d("LoginActivity", "开始执行登录操作: phone=" + phone);

        // 获取设备ID
        String deviceId = DeviceUtils.getDeviceId(this);

        // 调用登录API
        apiService.smsLogin(phone, code, deviceId,
                new RetrofitApiService.SuccessCallback<BaseResponse<LoginResponse.Data>>() {
                    @Override
                    public void onSuccess(BaseResponse<LoginResponse.Data> response) {
                        try {
                            if (response.getCode() == 200 && response.getData() != null) {
                                LoginResponse.Data data = response.getData();
                                // 登录成功，获取用户信息和token
                                String token = data.getToken();
                                Log.d("LoginActivity", "登录成功，获取到token: " + (token != null ? "有效" : "无效"));

                                // 统一使用UserManager保存用户信息和token（修复重复保存问题）
                                Gson gson = new Gson();
                                String userDataJson = gson.toJson(data.getUser());
                                UserManager.getInstance().saveUserAndToken(userDataJson, token);
                                Log.d("LoginActivity", "用户信息已保存到UserManager");

                                // 隐藏加载弹窗
                                loadingDialog.dismiss();
                                
                                // 跳转到主页面（数据已通过commit()同步保存）
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                                
                                Log.d("LoginActivity", "登录完成，已跳转到主页面");
                            } else {
                                Log.e("LoginActivity", "登录响应异常: code=" + response.getCode());
                                loadingDialog.dismiss(); // 隐藏加载弹窗
                                String msg = response.getMsg() != null ? response.getMsg() : "登录失败，请稍后重试";
                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                                loginButton.setEnabled(true);
                            }
                        } catch (Exception e) {
                            Log.e("LoginActivity", "处理登录响应时发生异常", e);
                            loadingDialog.dismiss(); // 隐藏加载弹窗
                            Toast.makeText(LoginActivity.this, "请求失败，请稍后重试", Toast.LENGTH_SHORT).show();
                            loginButton.setEnabled(true);
                        }
                    }
                },
                new RetrofitApiService.ErrorCallback() {
                    @Override
                    public void onError(String errorMessage) {
                        // 处理登录失败
                        Log.e("LoginActivity", "登录失败: " + errorMessage);
                        loadingDialog.dismiss(); // 隐藏加载弹窗
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        loginButton.setEnabled(true);
                    }
                });
    }

    // 已移除saveUserSession方法，统一使用UserManager管理用户数据

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消倒计时
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        // 确保加载弹窗被关闭，避免内存泄漏
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }
}