# 足迹 (Zuji) - Android 足迹记录应用

<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="足迹应用图标" width="120" height="120">
  
  <p>一款基于高德地图的Android足迹记录应用，让您记录生活中的每一个精彩瞬间</p>
  
  ![Android](https://img.shields.io/badge/Android-24%2B-green.svg)
  ![Java](https://img.shields.io/badge/Java-11-orange.svg)
  ![License](https://img.shields.io/badge/License-MIT-blue.svg)
</div>

## 📱 应用简介

足迹是一款功能丰富的Android应用，专为记录和分享生活足迹而设计。通过集成高德地图SDK，用户可以在地图上标记重要位置，添加照片和文字描述，创建属于自己的生活轨迹。

## ✨ 主要功能

### 🗺️ 地图功能
- **实时定位**：基于高德地图的精准定位服务
- **足迹标记**：在地图上添加个人足迹点
- **地图缓存**：支持离线地图缓存管理
- **多种地图模式**：普通、卫星、夜间等多种地图显示模式

### 📝 足迹管理
- **添加足迹**：支持添加位置、照片、文字描述
- **足迹分类**：按类别组织管理足迹
- **时间轴视图**：按时间顺序浏览历史足迹
- **足迹搜索**：快速查找特定足迹

### 📊 数据统计
- **足迹统计**：总足迹数、访问城市数、记录天数
- **分类统计**：按类别统计足迹分布
- **时间统计**：按月份统计足迹活跃度
- **可视化图表**：直观展示统计数据

### 👤 个人中心
- **用户资料**：头像、昵称、个人简介管理
- **数据同步**：云端数据同步与备份
- **设置管理**：应用偏好设置
- **缓存管理**：地图缓存大小查看与清理

### 🔄 应用更新
- **自动检查**：启动时自动检查新版本
- **在线更新**：支持APK在线下载更新
- **强制更新**：支持强制更新模式
- **下载进度**：实时显示下载进度
- **MD5校验**：确保下载文件完整性

## 🛠️ 技术栈

### 开发环境
- **开发语言**：Java 11
- **最低SDK版本**：Android 7.0 (API 24)
- **目标SDK版本**：Android 14 (API 35)
- **编译SDK版本**：Android 14 (API 35)

### 核心依赖
- **UI框架**：Material Design Components 1.11.0
- **地图服务**：高德地图SDK (3DMap 10.1.300)
- **数据库**：Room 2.6.1
- **网络请求**：Hutool 5.8.16
- **图片加载**：Glide 4.12.0
- **图片预览**：PhotoView 2.3.0
- **JSON解析**：Gson 2.10.1
- **架构组件**：ViewModel & LiveData 2.7.0

### 项目架构
```
app/src/main/java/com/damors/zuji/
├── activity/           # Activity类
│   ├── MainActivity.java
│   ├── AddFootprintActivity.java
│   ├── EditProfileActivity.java
│   └── ...
├── fragment/           # Fragment类
│   ├── MapFragment.java
│   ├── HistoryFragment.java
│   ├── ProfileFragment.java
│   └── StatisticsFragment.java
├── adapter/            # RecyclerView适配器
├── model/              # 数据模型
├── database/           # Room数据库
├── network/            # 网络服务
├── manager/            # 管理类
├── utils/              # 工具类
├── dialog/             # 自定义对话框
└── viewmodel/          # ViewModel类
```

## 🚀 快速开始

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK API 24 或更高版本
- 高德地图开发者账号和API Key

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/your-username/zuji-android.git
   cd zuji-android
   ```

2. **配置高德地图API Key**
   - 在高德开放平台申请API Key
   - 在 `AndroidManifest.xml` 中配置API Key：
   ```xml
   <meta-data
       android:name="com.amap.api.v2.apikey"
       android:value="您的高德地图API Key" />
   ```

3. **配置网络服务**
   - 在 `ApiConfig.java` 中配置服务器地址
   - 确保服务器端API接口正常运行

4. **编译运行**
   ```bash
   ./gradlew assembleDebug
   ```

### 权限说明

应用需要以下权限：
- **位置权限**：获取用户当前位置
- **网络权限**：数据同步和地图加载
- **存储权限**：保存照片和缓存数据
- **相机权限**：拍摄足迹照片
- **安装权限**：应用自动更新

## 📖 功能详解

### 地图模块
- 集成高德地图3D SDK
- 支持实时定位和位置追踪
- 自定义地图标记样式
- 地图缓存管理功能

### 足迹管理
- 支持多媒体足迹（文字+图片）
- 足迹分类和标签管理
- 时间轴和地图两种浏览模式
- 足迹数据云端同步

### 数据统计
- 基于Room数据库的本地统计
- 多维度数据分析
- 可视化图表展示
- 导出统计报告

### 应用更新
- 完整的在线更新解决方案
- 支持增量更新和全量更新
- 断点续传和MD5校验
- 用户友好的更新体验

## 🧪 测试

### 运行单元测试
```bash
./gradlew test
```

### 运行UI测试
```bash
./gradlew connectedAndroidTest
```

### 测试覆盖率
项目包含以下测试：
- 单元测试（JUnit + Mockito）
- UI测试（Espresso）
- 集成测试（Robolectric）

## 📦 构建发布

### Debug版本
```bash
./gradlew assembleDebug
```

### Release版本
```bash
./gradlew assembleRelease
```

### 签名配置
在 `app/build.gradle` 中配置签名信息：
```gradle
signingConfigs {
    release {
        storeFile file('your-keystore.jks')
        storePassword 'your-store-password'
        keyAlias 'your-key-alias'
        keyPassword 'your-key-password'
    }
}
```

## 🤝 贡献指南

我们欢迎任何形式的贡献！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 代码规范
- 遵循Java代码规范
- 添加必要的注释
- 编写单元测试
- 确保代码格式化

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 联系我们

- **项目维护者**：[您的姓名]
- **邮箱**：your-email@example.com
- **项目地址**：https://github.com/your-username/zuji-android
- **问题反馈**：https://github.com/your-username/zuji-android/issues

## 🙏 致谢

感谢以下开源项目和服务：
- [高德地图开放平台](https://lbs.amap.com/)
- [Material Design Components](https://material.io/components)
- [Glide](https://github.com/bumptech/glide)
- [Room](https://developer.android.com/training/data-storage/room)
- [Hutool](https://hutool.cn/)

## 📱 应用截图

<div align="center">
  <img src="screenshots/map_view.png" alt="地图视图" width="200">
  <img src="screenshots/history_view.png" alt="历史记录" width="200">
  <img src="screenshots/profile_view.png" alt="个人中心" width="200">
  <img src="screenshots/statistics_view.png" alt="数据统计" width="200">
</div>

---

<div align="center">
  <p>如果这个项目对您有帮助，请给我们一个 ⭐️</p>
</div>