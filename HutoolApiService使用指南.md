# HutoolApiService 使用指南

## 概述

`HutoolApiService` 是使用 Hutool 工具库重新构造的网络请求服务类，用于替代原有的基于 Volley 的 `ApiService`。新的实现具有以下优势：

- **更简洁的代码**: 使用 Hutool 的 HttpUtil 简化网络请求代码
- **更好的性能**: 使用线程池管理网络请求，避免频繁创建线程
- **更清晰的回调**: 使用自定义的回调接口，类型更明确
- **保持兼容性**: 保留原有的网络状态监控和自动重试功能

## 主要特性

### 1. 网络状态监控
- 自动检测网络状态变化
- 网络断开时将请求加入待处理队列
- 网络恢复时自动重试待处理的请求

### 2. 自动重试机制
- 网络异常时自动重试
- 支持配置超时时间和重试次数
- 智能判断是否为网络相关异常

### 3. 线程管理
- 使用线程池执行网络请求，避免阻塞主线程
- 回调在主线程执行，方便UI更新
- 支持并发请求处理

### 4. 统一的错误处理
- 统一的错误回调接口
- 详细的错误日志记录
- 区分网络错误和业务错误

## 使用方法

### 1. 获取服务实例

```java
// 获取 HutoolApiService 实例
HutoolApiService apiService = HutoolApiService.getInstance(context);
```

### 2. 短信登录示例

```java
// 原有的 ApiService 调用方式
ApiService.getInstance(context).smsLogin(
    phone, 
    code, 
    deviceId,
    new Response.Listener<LoginResponse.Data>() {
        @Override
        public void onResponse(LoginResponse.Data response) {
            // 处理成功响应
        }
    },
    new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            // 处理错误
        }
    }
);

// 新的 HutoolApiService 调用方式
HutoolApiService.getInstance(context).smsLogin(
    phone,
    code, 
    deviceId,
    new HutoolApiService.SuccessCallback<LoginResponse.Data>() {
        @Override
        public void onSuccess(LoginResponse.Data data) {
            // 处理成功响应
            Log.d(TAG, "登录成功: " + data.toString());
        }
    },
    new HutoolApiService.ErrorCallback() {
        @Override
        public void onError(String errorMessage) {
            // 处理错误
            Log.e(TAG, "登录失败: " + errorMessage);
        }
    }
);
```

### 3. 获取足迹动态列表示例

```java
HutoolApiService.getInstance(context).getFootprintMessages(
    1, // 页码
    20, // 每页大小
    new HutoolApiService.SuccessCallback<FootprintMessageResponse.Data>() {
        @Override
        public void onSuccess(FootprintMessageResponse.Data data) {
            // 处理成功响应
            Log.d(TAG, "获取足迹动态成功");
        }
    },
    new HutoolApiService.ErrorCallback() {
        @Override
        public void onError(String errorMessage) {
            // 处理错误
            Log.e(TAG, "获取足迹动态失败: " + errorMessage);
        }
    }
);
```

### 4. 使用 Lambda 表达式简化代码（Java 8+）

```java
// 使用 Lambda 表达式
HutoolApiService.getInstance(context).smsLogin(
    phone, code, deviceId,
    data -> {
        // 处理成功响应
        Log.d(TAG, "登录成功: " + data.toString());
    },
    errorMessage -> {
        // 处理错误
        Log.e(TAG, "登录失败: " + errorMessage);
    }
);
```

## 迁移指南

### 从 ApiService 迁移到 HutoolApiService

1. **替换服务实例获取**:
   ```java
   // 原来
   ApiService apiService = ApiService.getInstance(context);
   
   // 现在
   HutoolApiService apiService = HutoolApiService.getInstance(context);
   ```

2. **替换回调接口**:
   ```java
   // 原来
   Response.Listener<T> successListener
   Response.ErrorListener errorListener
   
   // 现在
   HutoolApiService.SuccessCallback<T> successCallback
   HutoolApiService.ErrorCallback errorCallback
   ```

3. **更新错误处理**:
   ```java
   // 原来
   public void onErrorResponse(VolleyError error) {
       String message = error.getMessage();
   }
   
   // 现在
   public void onError(String errorMessage) {
       // errorMessage 已经是处理过的错误信息
   }
   ```

## 配置说明

### 1. 超时配置
超时时间通过 `ApiConfig.TIMEOUT_MS` 配置，默认值可在 `ApiConfig` 类中修改。

### 2. 重试配置
重试次数通过 `ApiConfig.MAX_RETRIES` 配置。

### 3. 线程池配置
默认使用4个线程的固定线程池，可根据需要在 `HutoolApiService` 构造函数中调整。

## 日志说明

`HutoolApiService` 提供了详细的日志记录：

- **请求日志**: 记录请求URL、参数等信息
- **响应日志**: 记录响应状态码、内容等信息
- **错误日志**: 记录详细的错误信息和堆栈跟踪
- **网络状态日志**: 记录网络状态变化和重试信息

查看日志的命令：
```bash
adb logcat -s HutoolApiService
```

## 注意事项

1. **依赖要求**: 确保项目中已添加 Hutool 依赖
   ```gradle
   implementation 'cn.hutool:hutool-all:5.8.16'
   ```

2. **网络权限**: 确保在 AndroidManifest.xml 中添加了网络权限
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
   ```

3. **资源释放**: 在应用退出时调用 `destroy()` 方法释放资源
   ```java
   @Override
   protected void onDestroy() {
       super.onDestroy();
       HutoolApiService.getInstance(this).destroy();
   }
   ```

4. **线程安全**: `HutoolApiService` 是线程安全的，可以在多个线程中同时使用

## 性能优化建议

1. **复用实例**: 使用单例模式，避免重复创建服务实例
2. **合理设置超时**: 根据网络环境调整超时时间
3. **控制并发**: 避免同时发起过多网络请求
4. **及时释放**: 在不需要时及时调用 `destroy()` 方法

## 故障排除

### 常见问题

1. **网络请求失败**
   - 检查网络连接
   - 查看日志中的详细错误信息
   - 确认服务器地址和端口是否正确

2. **解析错误**
   - 检查服务器返回的数据格式
   - 确认响应数据类型是否匹配
   - 查看原始响应内容日志

3. **回调不执行**
   - 确认网络请求是否成功发起
   - 检查是否有异常被捕获
   - 查看线程相关日志

### 调试技巧

1. **启用详细日志**: 在开发环境中启用详细的网络请求日志
2. **使用网络抓包工具**: 如 Charles、Fiddler 等
3. **模拟网络异常**: 测试网络异常情况下的重试机制

## 总结

`HutoolApiService` 提供了一个更现代、更简洁的网络请求解决方案，同时保持了与原有 `ApiService` 的功能兼容性。通过使用 Hutool 工具库，代码更加简洁易读，性能也得到了提升。建议在新项目中使用 `HutoolApiService`，在现有项目中可以逐步迁移。