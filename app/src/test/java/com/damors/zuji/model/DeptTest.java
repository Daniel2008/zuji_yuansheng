package com.damors.zuji.model;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Dept部门模型类单元测试
 * 
 * 测试功能：
 * 1. 测试部门对象的创建和初始化
 * 2. 测试Getter和Setter方法
 * 3. 测试equals和hashCode方法
 * 4. 测试toString方法
 * 5. 测试构造函数
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
@RunWith(JUnit4.class)
public class DeptTest {
    
    private Dept dept;
    private Dept dept2;
    
    /**
     * 测试前的初始化设置
     */
    @Before
    public void setUp() {
        dept = new Dept();
        dept2 = new Dept(101, "测试部门");
    }
    
    /**
     * 测试默认构造函数
     */
    @Test
    public void testDefaultConstructor() {
        Dept newDept = new Dept();
        assertNotNull("部门对象不应为空", newDept);
        assertEquals("默认部门ID应为0", 0, newDept.getDeptId());
        assertNull("默认部门名称应为空", newDept.getDeptName());
    }
    
    /**
     * 测试带参数的构造函数
     */
    @Test
    public void testParameterizedConstructor() {
        assertEquals("部门ID应为101", 101, dept2.getDeptId());
        assertEquals("部门名称应为'测试部门'", "测试部门", dept2.getDeptName());
    }
    
    /**
     * 测试部门ID的设置和获取
     */
    @Test
    public void testDeptIdGetterAndSetter() {
        int expectedId = 105;
        dept.setDeptId(expectedId);
        assertEquals("部门ID应正确设置和获取", expectedId, dept.getDeptId());
    }
    
    /**
     * 测试部门名称的设置和获取
     */
    @Test
    public void testDeptNameGetterAndSetter() {
        String expectedName = "开发部";
        dept.setDeptName(expectedName);
        assertEquals("部门名称应正确设置和获取", expectedName, dept.getDeptName());
    }
    
    /**
     * 测试父部门ID的设置和获取
     */
    @Test
    public void testParentIdGetterAndSetter() {
        int expectedParentId = 100;
        dept.setParentId(expectedParentId);
        assertEquals("父部门ID应正确设置和获取", expectedParentId, dept.getParentId());
    }
    
    /**
     * 测试祖级列表的设置和获取
     */
    @Test
    public void testAncestorsGetterAndSetter() {
        String expectedAncestors = "0,100,101";
        dept.setAncestors(expectedAncestors);
        assertEquals("祖级列表应正确设置和获取", expectedAncestors, dept.getAncestors());
    }
    
    /**
     * 测试显示顺序的设置和获取
     */
    @Test
    public void testOrderNumGetterAndSetter() {
        int expectedOrderNum = 3;
        dept.setOrderNum(expectedOrderNum);
        assertEquals("显示顺序应正确设置和获取", expectedOrderNum, dept.getOrderNum());
    }
    
    /**
     * 测试负责人的设置和获取
     */
    @Test
    public void testLeaderGetterAndSetter() {
        String expectedLeader = "张三";
        dept.setLeader(expectedLeader);
        assertEquals("负责人应正确设置和获取", expectedLeader, dept.getLeader());
    }
    
    /**
     * 测试联系电话的设置和获取
     */
    @Test
    public void testPhoneGetterAndSetter() {
        String expectedPhone = "010-12345678";
        dept.setPhone(expectedPhone);
        assertEquals("联系电话应正确设置和获取", expectedPhone, dept.getPhone());
    }
    
    /**
     * 测试邮箱的设置和获取
     */
    @Test
    public void testEmailGetterAndSetter() {
        String expectedEmail = "dept@example.com";
        dept.setEmail(expectedEmail);
        assertEquals("邮箱应正确设置和获取", expectedEmail, dept.getEmail());
    }
    
    /**
     * 测试部门状态的设置和获取
     */
    @Test
    public void testStatusGetterAndSetter() {
        String expectedStatus = "0";
        dept.setStatus(expectedStatus);
        assertEquals("部门状态应正确设置和获取", expectedStatus, dept.getStatus());
    }
    
    /**
     * 测试删除标志的设置和获取
     */
    @Test
    public void testDelFlagGetterAndSetter() {
        String expectedDelFlag = "0";
        dept.setDelFlag(expectedDelFlag);
        assertEquals("删除标志应正确设置和获取", expectedDelFlag, dept.getDelFlag());
    }
    
    /**
     * 测试父部门名称的设置和获取
     */
    @Test
    public void testParentNameGetterAndSetter() {
        String expectedParentName = "总公司";
        dept.setParentName(expectedParentName);
        assertEquals("父部门名称应正确设置和获取", expectedParentName, dept.getParentName());
    }
    
    /**
     * 测试子部门列表的设置和获取
     */
    @Test
    public void testChildrenGetterAndSetter() {
        List<Dept> expectedChildren = new ArrayList<>();
        expectedChildren.add(new Dept(201, "子部门1"));
        expectedChildren.add(new Dept(202, "子部门2"));
        
        dept.setChildren(expectedChildren);
        assertEquals("子部门列表应正确设置和获取", expectedChildren, dept.getChildren());
        assertEquals("子部门数量应为2", 2, dept.getChildren().size());
    }
    
    /**
     * 测试创建相关字段的设置和获取
     */
    @Test
    public void testCreateFieldsGetterAndSetter() {
        String expectedCreateBy = "admin";
        String expectedCreateTime = "2024-01-01 10:00:00";
        
        dept.setCreateBy(expectedCreateBy);
        dept.setCreateTime(expectedCreateTime);
        
        assertEquals("创建者应正确设置和获取", expectedCreateBy, dept.getCreateBy());
        assertEquals("创建时间应正确设置和获取", expectedCreateTime, dept.getCreateTime());
    }
    
    /**
     * 测试更新相关字段的设置和获取
     */
    @Test
    public void testUpdateFieldsGetterAndSetter() {
        String expectedUpdateBy = "user";
        String expectedUpdateTime = "2024-01-02 15:30:00";
        
        dept.setUpdateBy(expectedUpdateBy);
        dept.setUpdateTime(expectedUpdateTime);
        
        assertEquals("更新者应正确设置和获取", expectedUpdateBy, dept.getUpdateBy());
        assertEquals("更新时间应正确设置和获取", expectedUpdateTime, dept.getUpdateTime());
    }
    
    /**
     * 测试备注的设置和获取
     */
    @Test
    public void testRemarkGetterAndSetter() {
        String expectedRemark = "这是一个测试部门";
        dept.setRemark(expectedRemark);
        assertEquals("备注应正确设置和获取", expectedRemark, dept.getRemark());
    }
    
    /**
     * 测试equals方法
     */
    @Test
    public void testEquals() {
        Dept dept1 = new Dept(101, "部门A");
        Dept dept2 = new Dept(101, "部门B");
        Dept dept3 = new Dept(102, "部门C");
        
        // 相同ID的部门应该相等
        assertTrue("相同ID的部门应该相等", dept1.equals(dept2));
        
        // 不同ID的部门应该不相等
        assertFalse("不同ID的部门应该不相等", dept1.equals(dept3));
        
        // 与自身比较应该相等
        assertTrue("与自身比较应该相等", dept1.equals(dept1));
        
        // 与null比较应该不相等
        assertFalse("与null比较应该不相等", dept1.equals(null));
        
        // 与不同类型对象比较应该不相等
        assertFalse("与不同类型对象比较应该不相等", dept1.equals("字符串"));
    }
    
    /**
     * 测试hashCode方法
     */
    @Test
    public void testHashCode() {
        Dept dept1 = new Dept(101, "部门A");
        Dept dept2 = new Dept(101, "部门B");
        Dept dept3 = new Dept(102, "部门C");
        
        // 相等的对象应该有相同的hashCode
        assertEquals("相等的对象应该有相同的hashCode", dept1.hashCode(), dept2.hashCode());
        
        // 不相等的对象可能有不同的hashCode
        assertNotEquals("不相等的对象应该有不同的hashCode", dept1.hashCode(), dept3.hashCode());
    }
    
    /**
     * 测试toString方法
     */
    @Test
    public void testToString() {
        dept.setDeptId(105);
        dept.setDeptName("测试部门");
        dept.setParentId(100);
        dept.setLeader("张三");
        dept.setStatus("0");
        
        String result = dept.toString();
        
        assertNotNull("toString结果不应为空", result);
        assertTrue("toString应包含部门ID", result.contains("deptId=105"));
        assertTrue("toString应包含部门名称", result.contains("deptName='测试部门'"));
        assertTrue("toString应包含父部门ID", result.contains("parentId=100"));
        assertTrue("toString应包含负责人", result.contains("leader='张三'"));
        assertTrue("toString应包含状态", result.contains("status='0'"));
    }
    
    /**
     * 测试完整的部门信息设置
     */
    @Test
    public void testCompleteSetup() {
        // 设置完整的部门信息
        dept.setDeptId(105);
        dept.setParentId(101);
        dept.setAncestors("0,100,101");
        dept.setDeptName("测试部门");
        dept.setOrderNum(3);
        dept.setLeader("若依");
        dept.setPhone("010-12345678");
        dept.setEmail("test@example.com");
        dept.setStatus("0");
        dept.setDelFlag("0");
        dept.setParentName("开发部");
        dept.setCreateBy("admin");
        dept.setCreateTime("2024-11-09 01:11:45");
        dept.setRemark("这是一个测试部门");
        
        // 验证所有字段都正确设置
        assertEquals(105, dept.getDeptId());
        assertEquals(101, dept.getParentId());
        assertEquals("0,100,101", dept.getAncestors());
        assertEquals("测试部门", dept.getDeptName());
        assertEquals(3, dept.getOrderNum());
        assertEquals("若依", dept.getLeader());
        assertEquals("010-12345678", dept.getPhone());
        assertEquals("test@example.com", dept.getEmail());
        assertEquals("0", dept.getStatus());
        assertEquals("0", dept.getDelFlag());
        assertEquals("开发部", dept.getParentName());
        assertEquals("admin", dept.getCreateBy());
        assertEquals("2024-11-09 01:11:45", dept.getCreateTime());
        assertEquals("这是一个测试部门", dept.getRemark());
    }
}