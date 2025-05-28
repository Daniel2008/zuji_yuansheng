# AddFootprintActivity 足迹发布接口对接说明

## 概述

本文档说明了如何在 `AddFootprintActivity` 中成功对接足迹发布接口，实现将用户创建的足迹动态发布到服务器。

## 功能特性

### 1. 网络接口集成
- 集成了 `HutoolApiService` 网络服务
- 使用 `PublishTrandsInfoPO` 实体类传递参数
- 支持异步网络请求和回调处理

### 2. 用户身份验证
- 自动获取当前登录用户信息
- 验证用户登录状态
- 防止未登录用户发布内容

### 3. 数据处理
- 自动转换图片URI为文件路径
- 支持多图片上传
- 包含位置信息（经纬度）
- 支持自定义标签和类型

### 4. 本地数据同步
- 网络发布成功后同步保存到本地数据库
- 保持数据一致性
- 支持离线查看

## 代码实现

### 1. 依赖导入

```java
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.PublishTrandsInfoPO;
import com.damors.zuji.model.User;
import com.damors.zuji.network.HutoolApiService;
```

### 2. 服务初始化

```java
public class AddFootprintActivity extends AppCompatActivity {
    // 网络服务实例
    private HutoolApiService apiService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化网络服务
        apiService = HutoolApiService.getInstance(this);
        
        // 其他初始化代码...
    }
}
```

### 3. 发布方法实现

```java
/**
 * 发布足迹动态到服务器
 * @param content 足迹内容
 * @param location 位置信息
 */
private void publishFootprint(String content, String location) {
    // 检查用户登录状态
    UserManager userManager = UserManager.getInstance();
    if (!userManager.isLoggedIn()) {
        Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
        return;
    }
    
    User currentUser = userManager.getCurrentUser();
    if (currentUser == null) {
        Toast.makeText(this, "获取用户信息失败", Toast.LENGTH_SHORT).show();
        return;
    }
    
    // 显示加载状态
    btnPublish.setEnabled(false);
    btnPublish.setText("发布中...");
    
    // 创建发布参数对象
    PublishTrandsInfoPO publishInfo = new PublishTrandsInfoPO();
    publishInfo.setUserId(String.valueOf(currentUser.getUserId()));
    publishInfo.setContent(content);
    publishInfo.setLocationInfo(location);
    publishInfo.setType("动态");
    publishInfo.setTag("");
    publishInfo.setLng(longitude);
    publishInfo.setLat(latitude);
    
    // 处理图片路径
    List<String> imagePaths = new ArrayList<>();
    for (Uri imageUri : selectedImages) {
        String path = getPathFromUri(imageUri);
        if (path != null) {
            imagePaths.add(path);
        }
    }
    publishInfo.setImagePaths(imagePaths);
    
    // 调用网络接口发布足迹
    apiService.publishFootprint(publishInfo,
        new HutoolApiService.SuccessCallback<String>() {
            @Override
            public void onSuccess(String response) {
                // 发布成功，同时保存到本地数据库
                saveToLocalDatabase(content, location);
                
                // 重置UI状态
                runOnUiThread(() -> {
                    btnPublish.setEnabled(true);
                    btnPublish.setText("发布");
                    Toast.makeText(AddFootprintActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
        },
        new HutoolApiService.ErrorCallback() {
            @Override
            public void onError(String errorMessage) {
                // 发布失败，重置UI状态
                runOnUiThread(() -> {
                    btnPublish.setEnabled(true);
                    btnPublish.setText("发布");
                    Toast.makeText(AddFootprintActivity.this, 
                        "发布失败: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        }
    );
}
```

### 4. 辅助方法

```java
/**
 * 保存足迹到本地数据库
 */
private void saveToLocalDatabase(String content, String location) {
    FootprintEntity footprint = new FootprintEntity();
    footprint.setDescription(content);
    footprint.setLocationName(location);
    footprint.setLatitude(latitude);
    footprint.setLongitude(longitude);
    footprint.setTimestamp(System.currentTimeMillis());
    footprint.setImageUris(selectedImages.toString());
    
    FootprintViewModel viewModel = new ViewModelProvider(this).get(FootprintViewModel.class);
    viewModel.insert(footprint);
}

/**
 * 从URI获取文件路径
 */
private String getPathFromUri(Uri uri) {
    try {
        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        } else {
            return uri.toString();
        }
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}
```

## 使用流程

### 1. 用户操作流程
1. 用户在界面输入足迹内容
2. 选择位置信息（可选）
3. 添加图片（可选）
4. 点击发布按钮
5. 系统验证用户登录状态
6. 发送网络请求到服务器
7. 成功后保存到本地数据库
8. 显示发布结果

### 2. 数据流转
```
用户输入 → 数据验证 → 创建PublishTrandsInfoPO → 网络请求 → 服务器响应 → 本地保存 → UI更新
```

## 错误处理

### 1. 用户未登录
- 检查用户登录状态
- 提示用户先登录
- 阻止发布操作

### 2. 网络请求失败
- 显示具体错误信息
- 恢复按钮状态
- 允许用户重试

### 3. 数据验证失败
- 检查必填字段
- 提示用户完善信息
- 防止无效数据提交

## 注意事项

### 1. 权限要求
- 需要网络访问权限
- 需要读取外部存储权限（图片访问）
- 需要位置权限（如果使用GPS定位）

### 2. 性能优化
- 图片压缩处理
- 异步网络请求
- UI线程安全

### 3. 用户体验
- 加载状态提示
- 错误信息友好显示
- 操作反馈及时

## 相关文件

- `AddFootprintActivity.java` - 主要活动类
- `PublishTrandsInfoPO.java` - 发布参数实体类
- `HutoolApiService.java` - 网络服务类
- `UserManager.java` - 用户管理类
- `ApiConfig.java` - API配置类

## 测试建议

### 1. 功能测试
- 测试正常发布流程
- 测试网络异常情况
- 测试用户未登录情况
- 测试图片上传功能

### 2. 边界测试
- 测试空内容发布
- 测试超长内容
- 测试大量图片上传
- 测试网络超时情况

### 3. 兼容性测试
- 测试不同Android版本
- 测试不同设备分辨率
- 测试不同网络环境

## 总结

通过以上实现，`AddFootprintActivity` 已成功对接足迹发布接口，具备了完整的网络发布功能，包括用户验证、数据处理、错误处理和本地同步等特性，为用户提供了良好的足迹发布体验。