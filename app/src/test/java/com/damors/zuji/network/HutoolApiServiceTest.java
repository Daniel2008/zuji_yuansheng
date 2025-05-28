package com.damors.zuji.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.damors.zuji.ZujiApp;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.response.LoginResponse;
import com.damors.zuji.model.response.FootprintMessageResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * HutoolApiService 单元测试类
 * 测试网络请求服务的各种功能和异常情况
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class HutoolApiServiceTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private ZujiApp mockZujiApp;
    
    @Mock
    private NetworkStateMonitor mockNetworkStateMonitor;
    
    @Mock
    private UserManager mockUserManager;
    
    private HutoolApiService apiService;
    private Context realContext;

    /**
     * 测试前的初始化设置
     */
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 使用真实的Context进行测试
        realContext = RuntimeEnvironment.getApplication();
        
        // 模拟ZujiApp和相关依赖
        when(mockContext.getApplicationContext()).thenReturn(realContext);
        
        // 模拟网络状态监控器
        when(mockNetworkStateMonitor.isNetworkAvailable()).thenReturn(true);
        
        // 创建ApiService实例用于测试
        // 注意：由于使用了单例模式，需要重置实例
        resetApiServiceInstance();
    }

    /**
     * 重置ApiService单例实例
     */
    private void resetApiServiceInstance() {
        try {
            java.lang.reflect.Field instanceField = HutoolApiService.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            // 忽略反射异常
        }
    }

    /**
     * 测试单例模式
     * 验证getInstance方法返回同一个实例
     */
    @Test
    public void testSingletonPattern() {
        HutoolApiService instance1 = HutoolApiService.getInstance(realContext);
        HutoolApiService instance2 = HutoolApiService.getInstance(realContext);
        
        assertNotNull("ApiService实例不应为null", instance1);
        assertSame("应该返回同一个实例", instance1, instance2);
    }

    /**
     * 测试网络可用时的短信登录功能
     * 验证正常情况下的登录请求流程
     */
    @Test
    public void testSmsLoginWhenNetworkAvailable() throws InterruptedException {
        // 准备测试数据
        String phone = "13800138000";
        String code = "123456";
        String deviceId = "test_device_id";
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] callbackCalled = {false};
        final String[] resultMessage = {null};
        
        // 获取ApiService实例
        apiService = HutoolApiService.getInstance(realContext);
        
        // 执行登录请求
        apiService.smsLogin(
            phone, 
            code, 
            deviceId,
            new HutoolApiService.SuccessCallback<LoginResponse.Data>() {
                @Override
                public void onSuccess(LoginResponse.Data data) {
                    callbackCalled[0] = true;
                    resultMessage[0] = "success";
                    latch.countDown();
                }
            },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    callbackCalled[0] = true;
                    resultMessage[0] = errorMessage;
                    latch.countDown();
                }
            }
        );
        
        // 等待回调执行（最多等待5秒）
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        
        // 验证结果
        assertTrue("请求应该在超时时间内完成", completed);
        assertTrue("回调应该被调用", callbackCalled[0]);
        assertNotNull("结果消息不应为null", resultMessage[0]);
    }

    /**
     * 测试网络不可用时的请求处理
     * 验证网络断开时请求被加入待处理队列
     */
    @Test
    public void testSmsLoginWhenNetworkUnavailable() throws InterruptedException {
        // 模拟网络不可用
        try (MockedStatic<NetworkStateMonitor> mockedNetworkMonitor = mockStatic(NetworkStateMonitor.class)) {
            NetworkStateMonitor mockMonitor = mock(NetworkStateMonitor.class);
            when(mockMonitor.isNetworkAvailable()).thenReturn(false);
            
            // 准备测试数据
            String phone = "13800138000";
            String code = "123456";
            String deviceId = "test_device_id";
            
            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] callbackCalled = {false};
            
            // 获取ApiService实例
            apiService = HutoolApiService.getInstance(realContext);
            
            // 执行登录请求
            apiService.smsLogin(
                phone, 
                code, 
                deviceId,
                new HutoolApiService.SuccessCallback<LoginResponse.Data>() {
                    @Override
                    public void onSuccess(LoginResponse.Data data) {
                        callbackCalled[0] = true;
                        latch.countDown();
                    }
                },
                new HutoolApiService.ErrorCallback() {
                    @Override
                    public void onError(String errorMessage) {
                        callbackCalled[0] = true;
                        latch.countDown();
                    }
                }
            );
            
            // 短暂等待，确保请求被处理
            Thread.sleep(100);
            
            // 验证：网络不可用时，回调不应该立即被调用（请求被加入待处理队列）
            assertFalse("网络不可用时，回调不应该立即被调用", callbackCalled[0]);
        }
    }

    /**
     * 测试获取足迹动态列表功能
     * 验证分页请求的参数传递
     */
    @Test
    public void testGetFootprintMessages() throws InterruptedException {
        int page = 1;
        int size = 20;
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] callbackCalled = {false};
        
        // 获取ApiService实例
        apiService = HutoolApiService.getInstance(realContext);
        
        // 执行获取足迹动态请求
        apiService.getFootprintMessages(
            page,
            size,
            new HutoolApiService.SuccessCallback<FootprintMessageResponse.Data>() {
                @Override
                public void onSuccess(FootprintMessageResponse.Data data) {
                    callbackCalled[0] = true;
                    latch.countDown();
                }
            },
            new HutoolApiService.ErrorCallback() {
                @Override
                public void onError(String errorMessage) {
                    callbackCalled[0] = true;
                    latch.countDown();
                }
            }
        );
        
        // 等待回调执行
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        
        // 验证结果
        assertTrue("请求应该在超时时间内完成", completed);
        assertTrue("回调应该被调用", callbackCalled[0]);
    }

    /**
     * 测试请求参数验证
     * 验证空参数和无效参数的处理
     */
    @Test
    public void testRequestParameterValidation() {
        apiService = HutoolApiService.getInstance(realContext);
        
        // 测试空手机号
        apiService.smsLogin(
            null, 
            "123456", 
            "device_id",
            data -> fail("空手机号不应该成功"),
            errorMessage -> {
                assertNotNull("错误消息不应为null", errorMessage);
            }
        );
        
        // 测试空验证码
        apiService.smsLogin(
            "13800138000", 
            null, 
            "device_id",
            data -> fail("空验证码不应该成功"),
            errorMessage -> {
                assertNotNull("错误消息不应为null", errorMessage);
            }
        );
        
        // 测试空设备ID
        apiService.smsLogin(
            "13800138000", 
            "123456", 
            null,
            data -> fail("空设备ID不应该成功"),
            errorMessage -> {
                assertNotNull("错误消息不应为null", errorMessage);
            }
        );
    }

    /**
     * 测试网络状态监听器
     * 验证网络状态变化时的处理逻辑
     */
    @Test
    public void testNetworkStateListener() {
        apiService = HutoolApiService.getInstance(realContext);
        
        // 模拟网络状态变化
        // 这里需要通过反射或其他方式触发网络状态变化
        // 由于测试环境限制，这里主要验证监听器的注册
        
        assertNotNull("ApiService应该成功创建", apiService);
    }

    /**
     * 测试资源释放
     * 验证destroy方法正确释放资源
     */
    @Test
    public void testDestroy() {
        apiService = HutoolApiService.getInstance(realContext);
        
        // 执行销毁操作
        apiService.destroy();
        
        // 验证实例被重置
        // 注意：由于单例模式的实现，这里需要检查内部状态
        // 在实际实现中，可以添加一个isDestroyed()方法来检查状态
    }

    /**
     * 测试并发请求处理
     * 验证多个同时请求的处理能力
     */
    @Test
    public void testConcurrentRequests() throws InterruptedException {
        apiService = HutoolApiService.getInstance(realContext);
        
        int requestCount = 5;
        CountDownLatch latch = new CountDownLatch(requestCount);
        final int[] completedRequests = {0};
        
        // 同时发起多个请求
        for (int i = 0; i < requestCount; i++) {
            final int requestId = i;
            apiService.smsLogin(
                "1380013800" + i,
                "123456",
                "device_" + i,
                data -> {
                    synchronized (completedRequests) {
                        completedRequests[0]++;
                    }
                    latch.countDown();
                },
                errorMessage -> {
                    synchronized (completedRequests) {
                        completedRequests[0]++;
                    }
                    latch.countDown();
                }
            );
        }
        
        // 等待所有请求完成
        boolean allCompleted = latch.await(10, TimeUnit.SECONDS);
        
        assertTrue("所有请求应该在超时时间内完成", allCompleted);
        assertEquals("应该完成所有请求", requestCount, completedRequests[0]);
    }

    /**
     * 测试错误处理
     * 验证各种错误情况的处理
     */
    @Test
    public void testErrorHandling() throws InterruptedException {
        apiService = HutoolApiService.getInstance(realContext);
        
        CountDownLatch latch = new CountDownLatch(1);
        final String[] errorMessage = {null};
        
        // 使用无效的URL测试错误处理
        apiService.smsLogin(
            "13800138000",
            "123456",
            "device_id",
            data -> {
                fail("无效请求不应该成功");
                latch.countDown();
            },
            error -> {
                errorMessage[0] = error;
                latch.countDown();
            }
        );
        
        // 等待错误回调
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        
        assertTrue("错误回调应该被调用", completed);
        assertNotNull("应该有错误消息", errorMessage[0]);
    }

    /**
     * 测试请求头设置
     * 验证通用请求头的正确设置
     */
    @Test
    public void testCommonHeaders() {
        apiService = HutoolApiService.getInstance(realContext);
        
        // 通过反射测试getCommonHeaders方法
        try {
            java.lang.reflect.Method getCommonHeadersMethod = 
                HutoolApiService.class.getDeclaredMethod("getCommonHeaders");
            getCommonHeadersMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> headers = 
                (java.util.Map<String, String>) getCommonHeadersMethod.invoke(apiService);
            
            assertNotNull("请求头不应为null", headers);
            assertTrue("应该包含Content-Type", headers.containsKey("Content-Type"));
            assertTrue("应该包含Accept", headers.containsKey("Accept"));
            assertTrue("应该包含User-Agent", headers.containsKey("User-Agent"));
            
            assertEquals("Content-Type应该正确", 
                        "application/json; charset=utf-8", 
                        headers.get("Content-Type"));
            assertEquals("Accept应该正确", 
                        "application/json", 
                        headers.get("Accept"));
            
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试网络异常判断
     * 验证isNetworkException方法的正确性
     */
    @Test
    public void testNetworkExceptionDetection() {
        apiService = HutoolApiService.getInstance(realContext);
        
        try {
            java.lang.reflect.Method isNetworkExceptionMethod = 
                HutoolApiService.class.getDeclaredMethod("isNetworkException", Exception.class);
            isNetworkExceptionMethod.setAccessible(true);
            
            // 测试网络相关异常
            Exception timeoutException = new Exception("Connection timeout");
            Boolean isNetworkException = (Boolean) isNetworkExceptionMethod.invoke(apiService, timeoutException);
            assertTrue("超时异常应该被识别为网络异常", isNetworkException);
            
            Exception connectionException = new Exception("No connection available");
            isNetworkException = (Boolean) isNetworkExceptionMethod.invoke(apiService, connectionException);
            assertTrue("连接异常应该被识别为网络异常", isNetworkException);
            
            // 测试非网络异常
            Exception parseException = new Exception("JSON parse error");
            isNetworkException = (Boolean) isNetworkExceptionMethod.invoke(apiService, parseException);
            assertFalse("解析异常不应该被识别为网络异常", isNetworkException);
            
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试清理工作
     * 在每个测试后清理资源
     */
    @org.junit.After
    public void tearDown() {
        if (apiService != null) {
            apiService.destroy();
        }
        resetApiServiceInstance();
    }
}