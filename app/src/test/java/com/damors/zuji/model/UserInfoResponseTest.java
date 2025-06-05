package com.damors.zuji.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * UserInfoResponse用户信息响应模型类单元测试
 * 
 * 测试功能：
 * 1. 测试响应对象的创建和初始化
 * 2. 测试Getter和Setter方法
 * 3. 测试isSuccess方法
 * 4. 测试toString方法
 * 5. 测试内部类UserInfoData
 * 6. 测试构造函数
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
@RunWith(JUnit4.class)
public class UserInfoResponseTest {
    
    private UserInfoResponse response;
    private UserInfoResponse.UserInfoData data;
    private cn.hutool.json.JSONObject user;
    
    /**
     * 测试前的初始化设置
     */
    @Before
    public void setUp() {
        response = new UserInfoResponse();
        data = new UserInfoResponse.UserInfoData();
        user = new cn.hutool.json.JSONObject();
        
        // 设置用户基本信息
        user.put("userId", 102);
        user.put("userName", "testUser");
        user.put("nickName", "测试用户");
        user.put("email", "test@example.com");
    }
    
    /**
     * 测试默认构造函数
     */
    @Test
    public void testDefaultConstructor() {
        UserInfoResponse newResponse = new UserInfoResponse();
        assertNotNull("响应对象不应为空", newResponse);
        assertEquals("默认状态码应为0", 0, newResponse.getCode());
        assertNull("默认消息应为空", newResponse.getMsg());
        assertNull("默认数据应为空", newResponse.getData());
    }
    
    /**
     * 测试带参数的构造函数
     */
    @Test
    public void testParameterizedConstructor() {
        UserInfoResponse.UserInfoData testData = new UserInfoResponse.UserInfoData(user, "test-token");
        UserInfoResponse testResponse = new UserInfoResponse(200, "操作成功", testData);
        
        assertEquals("状态码应为200", 200, testResponse.getCode());
        assertEquals("消息应为'操作成功'", "操作成功", testResponse.getMsg());
        assertNotNull("数据不应为空", testResponse.getData());
        assertEquals("数据应为设置的值", testData, testResponse.getData());
    }
    
    /**
     * 测试状态码的设置和获取
     */
    @Test
    public void testCodeGetterAndSetter() {
        int expectedCode = 200;
        response.setCode(expectedCode);
        assertEquals("状态码应正确设置和获取", expectedCode, response.getCode());
    }
    
    /**
     * 测试消息的设置和获取
     */
    @Test
    public void testMsgGetterAndSetter() {
        String expectedMsg = "操作成功";
        response.setMsg(expectedMsg);
        assertEquals("消息应正确设置和获取", expectedMsg, response.getMsg());
    }
    
    /**
     * 测试数据的设置和获取
     */
    @Test
    public void testDataGetterAndSetter() {
        UserInfoResponse.UserInfoData expectedData = new UserInfoResponse.UserInfoData(user, "test-token");
        response.setData(expectedData);
        assertEquals("数据应正确设置和获取", expectedData, response.getData());
    }
    
    /**
     * 测试isSuccess方法 - 成功情况
     */
    @Test
    public void testIsSuccessTrue() {
        response.setCode(200);
        assertTrue("状态码为200时应返回true", response.isSuccess());
    }
    
    /**
     * 测试isSuccess方法 - 失败情况
     */
    @Test
    public void testIsSuccessFalse() {
        response.setCode(400);
        assertFalse("状态码为400时应返回false", response.isSuccess());
        
        response.setCode(500);
        assertFalse("状态码为500时应返回false", response.isSuccess());
        
        response.setCode(0);
        assertFalse("状态码为0时应返回false", response.isSuccess());
    }
    
    /**
     * 测试toString方法
     */
    @Test
    public void testToString() {
        response.setCode(200);
        response.setMsg("操作成功");
        response.setData(new UserInfoResponse.UserInfoData(user, "test-token"));
        
        String result = response.toString();
        
        assertNotNull("toString结果不应为空", result);
        assertTrue("toString应包含状态码", result.contains("code=200"));
        assertTrue("toString应包含消息", result.contains("msg='操作成功'"));
        assertTrue("toString应包含data字段", result.contains("data="));
    }
    
    /**
     * 测试UserInfoData内部类的默认构造函数
     */
    @Test
    public void testUserInfoDataDefaultConstructor() {
        UserInfoResponse.UserInfoData newData = new UserInfoResponse.UserInfoData();
        assertNotNull("UserInfoData对象不应为空", newData);
        assertNull("默认用户应为空", newData.getUser());
        assertNull("默认token应为空", newData.getToken());
    }
    
    /**
     * 测试UserInfoData内部类的带参数构造函数
     */
    @Test
    public void testUserInfoDataParameterizedConstructor() {
        String expectedToken = "test-token-123";
        UserInfoResponse.UserInfoData testData = new UserInfoResponse.UserInfoData(user, expectedToken);
        
        assertEquals("用户应为设置的值", user, testData.getUser());
        assertEquals("token应为设置的值", expectedToken, testData.getToken());
    }
    
    /**
     * 测试UserInfoData用户的设置和获取
     */
    @Test
    public void testUserInfoDataUserGetterAndSetter() {
        data.setUser(user);
        assertEquals("用户应正确设置和获取", user, data.getUser());
    }
    
    /**
     * 测试UserInfoData token的设置和获取
     */
    @Test
    public void testUserInfoDataTokenGetterAndSetter() {
        String expectedToken = "abc123def456";
        data.setToken(expectedToken);
        assertEquals("token应正确设置和获取", expectedToken, data.getToken());
    }
    
    /**
     * 测试UserInfoData的toString方法
     */
    @Test
    public void testUserInfoDataToString() {
        data.setUser(user);
        data.setToken("test-token");
        
        String result = data.toString();
        
        assertNotNull("toString结果不应为空", result);
        assertTrue("toString应包含user字段", result.contains("user="));
        assertTrue("toString应包含token字段", result.contains("token='test-token'"));
    }
    
    /**
     * 测试完整的响应数据设置
     */
    @Test
    public void testCompleteResponseSetup() {
        // 设置完整的用户信息
        user.setUserId(102);
        user.setUserName("NFlrciVn");
        user.setNickName("沉默的查理");
        user.setEmail("1413772573@qq.com");
        user.setPhonenumber("18201307930");
        user.setSex("0");
        user.setAvatar("/profile/upload/2024/12/05/tmp_98a509bc19013df39071ce5878a8d0683739f64557f8631e_20241205060716A001.jpg");
        user.setStatus("0");
        user.setLoginIp("192.168.1.5");
        
        // 设置部门信息
        Dept dept = new Dept();
        dept.setDeptId(105);
        dept.setDeptName("测试部门");
        dept.setLeader("若依");
        user.setDept(dept);
        
        // 设置响应数据
        UserInfoResponse.UserInfoData responseData = new UserInfoResponse.UserInfoData(user, "928054f9-e914-4380-be59-0d5c2387a545");
        response.setCode(200);
        response.setMsg("操作成功");
        response.setData(responseData);
        
        // 验证所有字段都正确设置
        assertTrue("响应应为成功状态", response.isSuccess());
        assertEquals("状态码应为200", 200, response.getCode());
        assertEquals("消息应为'操作成功'", "操作成功", response.getMsg());
        assertNotNull("响应数据不应为空", response.getData());
        
        UserInfoResponse.UserInfoData actualData = response.getData();
        assertNotNull("用户信息不应为空", actualData.getUser());
        assertEquals("token应正确设置", "928054f9-e914-4380-be59-0d5c2387a545", actualData.getToken());
        
        cn.hutool.json.JSONObject actualUser = actualData.getUser();
        assertEquals("用户ID应正确", 102, actualUser.getInt("userId"));
        assertEquals("用户名应正确", "NFlrciVn", actualUser.getStr("userName"));
        assertEquals("昵称应正确", "沉默的查理", actualUser.getStr("nickName"));
        assertEquals("邮箱应正确", "1413772573@qq.com", actualUser.getStr("email"));
        
        cn.hutool.json.JSONObject actualDept = actualUser.getJSONObject("dept");
        assertNotNull("部门信息不应为空", actualDept);
        assertEquals("部门ID应正确", 105, actualDept.getInt("deptId"));
        assertEquals("部门名称应正确", "测试部门", actualDept.getStr("deptName"));
    }
    
    /**
     * 测试错误响应的处理
     */
    @Test
    public void testErrorResponse() {
        response.setCode(400);
        response.setMsg("请求参数错误");
        response.setData(null);
        
        assertFalse("错误响应应返回false", response.isSuccess());
        assertEquals("错误码应正确", 400, response.getCode());
        assertEquals("错误消息应正确", "请求参数错误", response.getMsg());
        assertNull("错误响应的数据应为空", response.getData());
    }
    
    /**
     * 测试空数据的处理
     */
    @Test
    public void testNullDataHandling() {
        response.setCode(200);
        response.setMsg("操作成功");
        response.setData(null);
        
        assertTrue("状态码200应返回成功", response.isSuccess());
        assertNull("数据应为空", response.getData());
    }
    
    /**
     * 测试UserInfoData中空值的处理
     */
    @Test
    public void testUserInfoDataNullHandling() {
        data.setUser(null);
        data.setToken(null);
        
        assertNull("用户应为空", data.getUser());
        assertNull("token应为空", data.getToken());
        
        String result = data.toString();
        assertNotNull("toString结果不应为空", result);
        assertTrue("toString应包含null值", result.contains("null"));
    }
}