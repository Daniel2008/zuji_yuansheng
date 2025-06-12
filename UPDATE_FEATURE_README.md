# 应用在线更新功能说明

## 功能概述

本项目已集成完整的Android应用在线更新功能，支持版本检查、APK下载、自动安装等功能。

## 功能特性

- ✅ 自动版本检查
- ✅ 美观的更新对话框
- ✅ 下载进度显示
- ✅ 强制更新支持
- ✅ 断点续传
- ✅ MD5校验
- ✅ 自动安装
- ✅ 错误处理

## 文件结构

```
app/src/main/java/com/damors/zuji/
├── model/
│   └── AppUpdateInfo.java          # 更新信息数据模型
├── manager/
│   └── AppUpdateManager.java       # 更新管理器
├── dialog/
│   └── AppUpdateDialog.java        # 更新对话框
├── activity/
│   └── UpdateTestActivity.java     # 测试Activity (开发用)
├── network/
│   ├── HutoolApiService.java       # API服务 (已添加更新检查接口)
│   └── ApiConfig.java              # API配置 (已添加更新接口)
└── MainActivity.java                # 主Activity (已集成更新检查)

app/src/main/res/
├── layout/
│   ├── dialog_app_update.xml       # 更新对话框布局
│   └── activity_update_test.xml    # 测试Activity布局
├── drawable/
│   ├── bg_dialog_rounded.xml       # 对话框圆角背景
│   ├── bg_rounded_light_gray.xml   # 浅灰色圆角背景
│   ├── btn_primary_background.xml   # 主要按钮背景
│   └── btn_secondary_background.xml # 次要按钮背景
├── anim/
│   ├── dialog_enter.xml            # 对话框进入动画
│   └── dialog_exit.xml             # 对话框退出动画
├── values/
│   └── dialog_styles.xml           # 对话框样式
└── xml/
    └── file_paths.xml              # FileProvider路径配置 (已添加下载路径)
```

## 服务端API接口

### 检查更新接口

**接口地址：** `POST /api/checkAppUpdate`

**请求参数：**
```json
{
    "versionCode": 1,
    "platform": "android"
}
```

**响应格式：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "versionCode": 2,
        "versionName": "2.0.0",
        "downloadUrl": "https://example.com/app-release.apk",
        "updateContent": "1. 新增功能\n2. 修复问题",
        "forceUpdate": false,
        "fileSize": 25165824,
        "md5": "abcd1234567890efgh",
        "releaseTime": "2024-01-15 10:30:00"
    }
}
```

**字段说明：**
- `versionCode`: 版本号（整数）
- `versionName`: 版本名称（字符串）
- `downloadUrl`: APK下载地址
- `updateContent`: 更新内容描述
- `forceUpdate`: 是否强制更新
- `fileSize`: 文件大小（字节）
- `md5`: 文件MD5校验值
- `releaseTime`: 发布时间

## 使用方法

### 1. 自动检查更新

应用启动时会自动检查更新，无需额外代码。检查逻辑已集成在 `MainActivity.onCreate()` 中。

### 2. 手动检查更新

```java
// 获取当前版本号
int currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;

// 调用API检查更新
HutoolApiService.getInstance().checkAppUpdate(
    currentVersionCode,
    "android",
    new HutoolApiService.SuccessCallback<AppUpdateInfo>() {
        @Override
        public void onSuccess(AppUpdateInfo updateInfo) {
            if (updateInfo != null && updateInfo.getVersionCode() > currentVersionCode) {
                // 显示更新对话框
                showUpdateDialog(updateInfo);
            }
        }
    },
    new HutoolApiService.ErrorCallback() {
        @Override
        public void onError(String error) {
            // 处理错误
        }
    }
);
```

### 3. 显示更新对话框

```java
AppUpdateDialog updateDialog = new AppUpdateDialog(this, updateInfo);
updateDialog.setOnUpdateActionListener(new AppUpdateDialog.OnUpdateActionListener() {
    @Override
    public void onUpdateClicked() {
        // 用户点击立即更新
    }
    
    @Override
    public void onCancelClicked() {
        // 用户点击稍后更新
    }
    
    @Override
    public void onDownloadCancelled() {
        // 用户取消下载
    }
});
updateDialog.show();
```

## 测试功能

### 测试Activity

项目包含一个测试Activity (`UpdateTestActivity`)，可用于开发阶段测试更新功能：

```java
// 启动测试Activity
Intent intent = new Intent(this, UpdateTestActivity.class);
startActivity(intent);
```

测试Activity提供以下功能：
- 显示当前版本信息
- 检查服务器更新
- 测试更新对话框

### 模拟测试数据

可以使用测试Activity中的"测试更新对话框"按钮来查看对话框效果，无需真实的服务器数据。

## 配置说明

### 1. API配置

在 `ApiConfig.java` 中配置服务器地址：

```java
public static final String DEV_BASE_URL = "http://your-dev-server.com";
public static final String TEST_BASE_URL = "http://your-test-server.com";
public static final String PROD_BASE_URL = "http://your-prod-server.com";
```

### 2. 权限配置

已在 `AndroidManifest.xml` 中添加必要权限：

```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 文件读写权限 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- 下载权限 -->
<uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />

<!-- 安装权限 (Android 8.0+) -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

### 3. FileProvider配置

已在 `AndroidManifest.xml` 中配置FileProvider，并在 `file_paths.xml` 中添加下载路径。

## 注意事项

### 1. 安全性

- 建议使用HTTPS协议下载APK文件
- 启用MD5校验确保文件完整性
- 验证下载URL的合法性

### 2. 用户体验

- 强制更新时不允许用户取消
- 下载失败时提供重试机制
- 在WiFi环境下提示用户下载大文件

### 3. 兼容性

- Android 8.0+ 需要REQUEST_INSTALL_PACKAGES权限
- Android 10+ 需要适配分区存储
- 不同厂商ROM可能有安装限制

### 4. 测试建议

- 在不同Android版本上测试
- 测试网络异常情况
- 测试存储空间不足情况
- 测试强制更新和普通更新场景

## 发布注意

在正式发布时，建议：

1. 移除或隐藏 `UpdateTestActivity`
2. 配置正确的生产服务器地址
3. 测试完整的更新流程
4. 确保服务器API正常工作

## 故障排除

### 常见问题

1. **更新检查失败**
   - 检查网络连接
   - 验证API接口地址
   - 查看服务器日志

2. **下载失败**
   - 检查存储权限
   - 验证下载URL
   - 检查网络状态

3. **安装失败**
   - 检查安装权限
   - 验证APK文件完整性
   - 检查签名是否一致

### 日志调试

使用以下TAG查看相关日志：
- `AppUpdateManager`: 更新管理器日志
- `AppUpdateDialog`: 对话框日志
- `HutoolApiService`: API调用日志
- `MainActivity`: 主Activity日志

## 版本历史

- v1.0.0: 初始版本，基础更新功能
- 后续版本将根据需求持续优化