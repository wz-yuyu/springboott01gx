package com.service.impl;

import com.dao.CommonDao;
import com.service.impl.CommonServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * CommonServiceImpl 单元测试
 * 针对 MyBatis/MyBatis-Plus 数据访问层的Service测试
 */
@ExtendWith(MockitoExtension.class)
class CommonServiceImplTest {

    @Mock
    private CommonDao commonDao;

    @InjectMocks
    private CommonServiceImpl commonService;

    // ====================== getOption 方法测试 ======================

    @Test
    void testGetOption_WithTableAndColumn_ShouldReturnOptions() {
        // 准备：模拟从数据库查询选项的场景
        Map<String, Object> params = new HashMap<>();
        params.put("table", "users");
        params.put("column", "role");

        List<String> expectedOptions = Arrays.asList("管理员", "普通用户", "VIP用户");

        // 配置Mock
        when(commonDao.getOption(eq(params))).thenReturn(expectedOptions);

        // 执行
        List<String> result = commonService.getOption(params);

        // 验证
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("管理员"));
        assertEquals("管理员", result.get(0));
    }

    @Test
    void testGetOption_WithNullParams_ShouldHandleGracefully() {
        // 配置：当DAO接收到null时返回空列表（根据你的业务逻辑调整）
        when(commonDao.getOption(isNull())).thenReturn(new ArrayList<>());

        // 执行 & 验证
        List<String> result = commonService.getOption(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ====================== getFollowByOption 方法测试 ======================

    @Test
    void testGetFollowByOption_ShouldReturnMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 1001L);

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("followCount", 25);
        expectedResult.put("fansCount", 120);
        expectedResult.put("mutualFollow", 15);

        when(commonDao.getFollowByOption(params)).thenReturn(expectedResult);

        Map<String, Object> result = commonService.getFollowByOption(params);

        assertNotNull(result);
        assertEquals(25, result.get("followCount"));
        assertEquals(120, result.get("fansCount"));
        // 注意：getFollowByOption2 方法在Service中未实现，所以不测试
    }

    // ====================== sh 方法测试 ======================

    @Test
    void testSh_ShouldCallDaoWithoutException() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", 5001L);
        params.put("status", "审核通过");
        params.put("auditor", "admin");

        // 配置void方法：doNothing是默认行为，这里显式写出
        doNothing().when(commonDao).sh(params);

        // 验证不会抛出异常
        assertDoesNotThrow(() -> commonService.sh(params));

        // 验证方法被调用
        verify(commonDao, times(1)).sh(params);
    }

    // ====================== remindCount 方法测试 ======================

    @Test
    void testRemindCount_ShouldReturnInteger() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 2001L);
        params.put("readStatus", "未读");

        when(commonDao.remindCount(params)).thenReturn(7);

        int count = commonService.remindCount(params);

        assertEquals(7, count);
    }

    @Test
    void testRemindCount_WithNoReminders_ShouldReturnZero() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 9999L);

        when(commonDao.remindCount(anyMap())).thenReturn(0);

        assertEquals(0, commonService.remindCount(params));
    }

    // ====================== selectCal 方法测试 ======================

    @Test
    void testSelectCal_ShouldReturnCalculationResult() {
        Map<String, Object> params = new HashMap<>();
        params.put("startDate", "2025-01-01");
        params.put("endDate", "2025-12-31");

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("totalAmount", 150000.75);
        expectedResult.put("averageAmount", 12500.06);
        expectedResult.put("maxAmount", 30000.00);
        expectedResult.put("minAmount", 500.50);
        expectedResult.put("transactionCount", 12);

        when(commonDao.selectCal(params)).thenReturn(expectedResult);

        Map<String, Object> result = commonService.selectCal(params);

        assertNotNull(result);
        assertEquals(150000.75, (Double) result.get("totalAmount"), 0.01);
        assertEquals(12, result.get("transactionCount"));
    }

    // ====================== selectGroup 方法测试 ======================

    @Test
    void testSelectGroup_WithUserData_ShouldReturnGroupedResults() {
        // 准备：模拟按用户角色分组统计
        Map<String, Object> params = new HashMap<>();
        params.put("groupField", "role");
        params.put("table", "users");

        List<Map<String, Object>> expectedGroups = new ArrayList<>();

        // 第一组：管理员
        Map<String, Object> adminGroup = new HashMap<>();
        adminGroup.put("role", "管理员");
        adminGroup.put("count", 5);
        adminGroup.put("avgId", 100.2); // 示例数据
        expectedGroups.add(adminGroup);

        // 第二组：普通用户
        Map<String, Object> userGroup = new HashMap<>();
        userGroup.put("role", "普通用户");
        userGroup.put("count", 150);
        userGroup.put("avgId", 500.8);
        expectedGroups.add(userGroup);

        // 第三组：VIP用户
        Map<String, Object> vipGroup = new HashMap<>();
        vipGroup.put("role", "VIP用户");
        vipGroup.put("count", 30);
        vipGroup.put("avgId", 300.5);
        expectedGroups.add(vipGroup);

        when(commonDao.selectGroup(params)).thenReturn(expectedGroups);

        List<Map<String, Object>> result = commonService.selectGroup(params);

        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证第一组数据
        assertEquals("管理员", result.get(0).get("role"));
        assertEquals(5, result.get(0).get("count"));

        // 验证包含所有组
        List<String> roles = Arrays.asList("管理员", "普通用户", "VIP用户");
        assertTrue(roles.contains(result.get(0).get("role")));
        assertTrue(roles.contains(result.get(1).get("role")));
        assertTrue(roles.contains(result.get(2).get("role")));
    }

    // ====================== selectValue 方法测试 ======================

    @Test
    void testSelectValue_ShouldReturnUserListAsMaps() {
        // 准备：模拟查询用户列表，返回Map形式（类似MyBatis结果）
        Map<String, Object> params = new HashMap<>();
        params.put("table", "users");
        params.put("limit", 3);

        List<Map<String, Object>> expectedUsers = new ArrayList<>();

        // 用户1
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", 1L);
        user1.put("username", "admin");
        user1.put("role", "管理员");
        user1.put("addtime", new Date());
        expectedUsers.add(user1);

        // 用户2
        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", 2L);
        user2.put("username", "zhangsan");
        user2.put("role", "普通用户");
        user2.put("addtime", new Date());
        expectedUsers.add(user2);

        // 用户3
        Map<String, Object> user3 = new HashMap<>();
        user3.put("id", 3L);
        user3.put("username", "vip_user");
        user3.put("role", "VIP用户");
        user3.put("addtime", new Date());
        expectedUsers.add(user3);

        when(commonDao.selectValue(params)).thenReturn(expectedUsers);

        List<Map<String, Object>> result = commonService.selectValue(params);

        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证用户数据结构和内容
        Map<String, Object> firstUser = result.get(0);
        assertEquals(1L, firstUser.get("id"));
        assertEquals("admin", firstUser.get("username"));
        assertNotNull(firstUser.get("addtime"));

        // 验证所有用户都有必要字段
        for (Map<String, Object> user : result) {
            assertTrue(user.containsKey("id"));
            assertTrue(user.containsKey("username"));
            assertTrue(user.containsKey("role"));
        }
    }

    @Test
    void testSelectValue_WithEmptyResult_ShouldReturnEmptyList() {
        Map<String, Object> params = new HashMap<>();
        params.put("table", "non_existent_table");

        when(commonDao.selectValue(params)).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = commonService.selectValue(params);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ====================== 边界和异常测试 ======================

    @Test
    void testGetOption_WhenDaoThrowsException_ShouldPropagate() {
        Map<String, Object> params = new HashMap<>();
        params.put("table", "invalid_table");

        // 配置Mock抛出RuntimeException（模拟数据库异常）
        when(commonDao.getOption(params)).thenThrow(new RuntimeException("数据库连接失败"));

        // 验证异常被传播
        assertThrows(RuntimeException.class, () -> {
            commonService.getOption(params);
        });
    }

    @Test
    void testAllMethods_VerifyMethodCalls() {
        // 验证所有Service方法都调用了对应的DAO方法
        Map<String, Object> testParams = Collections.singletonMap("test", "value");

        // 配置所有Mock
        when(commonDao.getOption(anyMap())).thenReturn(new ArrayList<>());
        when(commonDao.getFollowByOption(anyMap())).thenReturn(new HashMap<>());
        when(commonDao.remindCount(anyMap())).thenReturn(0);
        when(commonDao.selectCal(anyMap())).thenReturn(new HashMap<>());
        when(commonDao.selectGroup(anyMap())).thenReturn(new ArrayList<>());
        when(commonDao.selectValue(anyMap())).thenReturn(new ArrayList<>());

        // 调用所有方法
        commonService.getOption(testParams);
        commonService.getFollowByOption(testParams);
        commonService.remindCount(testParams);
        commonService.selectCal(testParams);
        commonService.selectGroup(testParams);
        commonService.selectValue(testParams);

        // 验证每个DAO方法都被调用一次
        verify(commonDao, times(1)).getOption(anyMap());
        verify(commonDao, times(1)).getFollowByOption(anyMap());
        verify(commonDao, times(1)).remindCount(anyMap());
        verify(commonDao, times(1)).selectCal(anyMap());
        verify(commonDao, times(1)).selectGroup(anyMap());
        verify(commonDao, times(1)).selectValue(anyMap());
    }
}