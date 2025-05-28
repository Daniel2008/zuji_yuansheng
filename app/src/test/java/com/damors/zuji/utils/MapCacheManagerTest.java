package com.damors.zuji.utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.osmdroid.config.Configuration;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * MapCacheManager的单元测试类
 * 测试地图缓存管理功能的各种方法
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MapCacheManagerTest {

    @Mock
    private Configuration mockConfiguration;
    
    @Mock
    private File mockCacheDir;
    
    @Mock
    private File mockCacheFile;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试获取缓存大小功能
     * 验证能够正确计算缓存目录的大小
     */
    @Test
    public void testGetCacheSize() {
        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            // 模拟Configuration返回缓存目录
            configurationMock.when(Configuration::getInstance).thenReturn(mockConfiguration);
            when(mockConfiguration.getOsmdroidTileCache()).thenReturn(mockCacheDir);
            when(mockCacheDir.exists()).thenReturn(true);
            when(mockCacheDir.isDirectory()).thenReturn(true);
            
            // 模拟缓存目录包含文件
            File[] mockFiles = {mockCacheFile};
            when(mockCacheDir.listFiles()).thenReturn(mockFiles);
            when(mockCacheFile.isDirectory()).thenReturn(false);
            when(mockCacheFile.length()).thenReturn(1024L); // 1KB
            
            // 执行测试
            long cacheSize = MapCacheManager.getCacheSize();
            
            // 验证结果
            assertEquals(1024L, cacheSize);
        }
    }

    /**
     * 测试格式化缓存大小功能
     * 验证能够正确格式化文件大小显示
     */
    @Test
    public void testGetFormattedCacheSize() {
        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            // 模拟Configuration返回缓存目录
            configurationMock.when(Configuration::getInstance).thenReturn(mockConfiguration);
            when(mockConfiguration.getOsmdroidTileCache()).thenReturn(mockCacheDir);
            when(mockCacheDir.exists()).thenReturn(true);
            when(mockCacheDir.isDirectory()).thenReturn(true);
            
            // 模拟缓存目录包含文件
            File[] mockFiles = {mockCacheFile};
            when(mockCacheDir.listFiles()).thenReturn(mockFiles);
            when(mockCacheFile.isDirectory()).thenReturn(false);
            when(mockCacheFile.length()).thenReturn(1024L * 1024L); // 1MB
            
            // 执行测试
            String formattedSize = MapCacheManager.getFormattedCacheSize();
            
            // 验证结果
            assertTrue("格式化大小应包含MB单位", formattedSize.contains("MB"));
            assertFalse("格式化大小不应为空", formattedSize.isEmpty());
        }
    }

    /**
     * 测试清理缓存功能
     * 验证能够正确清理缓存目录
     */
    @Test
    public void testClearCache() {
        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            // 模拟Configuration返回缓存目录
            configurationMock.when(Configuration::getInstance).thenReturn(mockConfiguration);
            when(mockConfiguration.getOsmdroidTileCache()).thenReturn(mockCacheDir);
            when(mockCacheDir.exists()).thenReturn(true);
            when(mockCacheDir.isDirectory()).thenReturn(true);
            
            // 模拟删除操作成功
            File[] mockFiles = {mockCacheFile};
            when(mockCacheDir.listFiles()).thenReturn(mockFiles);
            when(mockCacheFile.isDirectory()).thenReturn(false);
            when(mockCacheFile.delete()).thenReturn(true);
            when(mockCacheDir.delete()).thenReturn(true);
            when(mockCacheDir.mkdirs()).thenReturn(true);
            
            // 执行测试
            boolean result = MapCacheManager.clearCache();
            
            // 验证结果
            assertTrue("清理缓存应该成功", result);
            verify(mockCacheDir).mkdirs(); // 验证重新创建了目录
        }
    }

    /**
     * 测试检查缓存可用性功能
     * 验证能够正确检查缓存目录的可用性
     */
    @Test
    public void testIsCacheAvailable() {
        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            // 模拟Configuration返回缓存目录
            configurationMock.when(Configuration::getInstance).thenReturn(mockConfiguration);
            when(mockConfiguration.getOsmdroidTileCache()).thenReturn(mockCacheDir);
            when(mockCacheDir.exists()).thenReturn(true);
            when(mockCacheDir.canWrite()).thenReturn(true);
            
            // 执行测试
            boolean isAvailable = MapCacheManager.isCacheAvailable();
            
            // 验证结果
            assertTrue("缓存应该可用", isAvailable);
        }
    }

    /**
     * 测试获取缓存路径功能
     * 验证能够正确获取缓存目录路径
     */
    @Test
    public void testGetCachePath() {
        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            // 模拟Configuration返回缓存目录
            configurationMock.when(Configuration::getInstance).thenReturn(mockConfiguration);
            when(mockConfiguration.getOsmdroidTileCache()).thenReturn(mockCacheDir);
            when(mockCacheDir.getAbsolutePath()).thenReturn("/test/cache/path");
            
            // 执行测试
            String cachePath = MapCacheManager.getCachePath();
            
            // 验证结果
            assertEquals("/test/cache/path", cachePath);
        }
    }

    /**
     * 测试获取缓存文件数量功能
     * 验证能够正确计算缓存目录中的文件数量
     */
    @Test
    public void testGetCacheFileCount() {
        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            // 模拟Configuration返回缓存目录
            configurationMock.when(Configuration::getInstance).thenReturn(mockConfiguration);
            when(mockConfiguration.getOsmdroidTileCache()).thenReturn(mockCacheDir);
            when(mockCacheDir.exists()).thenReturn(true);
            when(mockCacheDir.isDirectory()).thenReturn(true);
            
            // 模拟缓存目录包含3个文件
            File[] mockFiles = {mockCacheFile, mockCacheFile, mockCacheFile};
            when(mockCacheDir.listFiles()).thenReturn(mockFiles);
            when(mockCacheFile.isDirectory()).thenReturn(false);
            
            // 执行测试
            int fileCount = MapCacheManager.getCacheFileCount();
            
            // 验证结果
            assertEquals(3, fileCount);
        }
    }

    /**
     * 测试清理过期缓存功能
     * 验证能够正确清理过期的缓存文件
     */
    @Test
    public void testClearExpiredCache() {
        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            // 模拟Configuration返回缓存目录
            configurationMock.when(Configuration::getInstance).thenReturn(mockConfiguration);
            when(mockConfiguration.getOsmdroidTileCache()).thenReturn(mockCacheDir);
            when(mockCacheDir.exists()).thenReturn(true);
            when(mockCacheDir.isDirectory()).thenReturn(true);
            
            // 模拟过期文件
            File[] mockFiles = {mockCacheFile};
            when(mockCacheDir.listFiles()).thenReturn(mockFiles);
            when(mockCacheFile.isDirectory()).thenReturn(false);
            when(mockCacheFile.lastModified()).thenReturn(System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000); // 8天前
            when(mockCacheFile.delete()).thenReturn(true);
            
            // 执行测试 - 清理7天前的文件
            int deletedCount = MapCacheManager.clearExpiredCache(7L * 24 * 60 * 60 * 1000);
            
            // 验证结果
            assertEquals(1, deletedCount);
            verify(mockCacheFile).delete();
        }
    }

    /**
     * 测试异常情况处理
     * 验证在缓存目录不存在时的处理
     */
    @Test
    public void testCacheSizeWhenDirectoryNotExists() {
        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            // 模拟Configuration返回不存在的缓存目录
            configurationMock.when(Configuration::getInstance).thenReturn(mockConfiguration);
            when(mockConfiguration.getOsmdroidTileCache()).thenReturn(mockCacheDir);
            when(mockCacheDir.exists()).thenReturn(false);
            
            // 执行测试
            long cacheSize = MapCacheManager.getCacheSize();
            
            // 验证结果
            assertEquals(0L, cacheSize);
        }
    }

    /**
     * 测试异常情况处理
     * 验证在Configuration为null时的处理
     */
    @Test
    public void testCacheSizeWhenConfigurationIsNull() {
        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            // 模拟Configuration返回null
            configurationMock.when(Configuration::getInstance).thenReturn(mockConfiguration);
            when(mockConfiguration.getOsmdroidTileCache()).thenReturn(null);
            
            // 执行测试
            long cacheSize = MapCacheManager.getCacheSize();
            
            // 验证结果
            assertEquals(0L, cacheSize);
        }
    }
}