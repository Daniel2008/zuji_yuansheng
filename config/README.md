# 环境配置系统使用说明
# Environment Configuration System Guide

## 概述 Overview

本配置系统允许您轻松地在开发环境和正式环境之间切换，并管理应用的版本信息和其他环境相关配置。

This configuration system allows you to easily switch between development and production environments, and manage app version information and other environment-related configurations.

## 文件结构 File Structure

```
config/
├── environment.properties  # 环境切换配置文件 (Environment switch config)
├── dev.properties          # 开发环境配置 (Development environment config)
├── prod.properties         # 正式环境配置 (Production environment config)
└── README.md               # 使用说明 (Usage guide)
```

## 使用方法 Usage

### 1. 环境切换 Environment Switching

编辑 `config/environment.properties` 文件中的 `current.environment` 值：

Edit the `current.environment` value in `config/environment.properties`:

```properties
# 切换到开发环境 (Switch to development environment)
current.environment=dev

# 切换到正式环境 (Switch to production environment)
current.environment=prod
```

### 2. 版本配置 Version Configuration

在对应的环境配置文件中修改版本信息：

Modify version information in the corresponding environment config file:

**开发环境 (Development)** - `config/dev.properties`:
```properties
versionCode=101
versionName=1.01-dev
```

**正式环境 (Production)** - `config/prod.properties`:
```properties
versionCode=101
versionName=1.01
```

### 3. 其他配置 Other Configurations

您可以在环境配置文件中添加其他配置项，这些配置会自动添加到 BuildConfig 中：

You can add other configuration items in environment config files, which will be automatically added to BuildConfig:

- `proMode`: 是否为正式模式 (Whether it's production mode)
- `api.baseUrl`: API基础URL (API base URL)
- `api.debug`: API调试模式 (API debug mode)
- `log.enabled`: 日志启用状态 (Log enabled status)

### 4. 代码中使用 Usage in Code

在Java/Kotlin代码中，您可以通过BuildConfig访问这些配置：

In Java/Kotlin code, you can access these configurations through BuildConfig:

```java
// 获取当前环境 (Get current environment)
String environment = BuildConfig.ENVIRONMENT;

// 检查是否为正式模式 (Check if it's production mode)
boolean isProMode = BuildConfig.PRO_MODE;

// 获取API基础URL (Get API base URL)
String apiBaseUrl = BuildConfig.API_BASE_URL;

// 检查API调试模式 (Check API debug mode)
boolean apiDebug = BuildConfig.API_DEBUG;

// 检查日志启用状态 (Check log enabled status)
boolean logEnabled = BuildConfig.LOG_ENABLED;
```

## 构建流程 Build Process

1. Gradle读取 `config/environment.properties` 获取当前环境设置
2. 根据环境设置加载对应的配置文件 (`dev.properties` 或 `prod.properties`)
3. 将配置信息注入到 BuildConfig 中
4. 构建应用时使用相应的配置

1. Gradle reads `config/environment.properties` to get current environment setting
2. Load corresponding config file (`dev.properties` or `prod.properties`) based on environment setting
3. Inject configuration information into BuildConfig
4. Use appropriate configuration when building the app

## 注意事项 Notes

1. **版本控制**: `config/environment.properties` 可以提交到版本控制系统，但建议在不同分支中设置不同的默认环境。
   **Version Control**: `config/environment.properties` can be committed to version control, but it's recommended to set different default environments in different branches.

2. **备用配置**: 如果配置文件不存在，系统会自动回退到使用 `local.properties` 文件。
   **Fallback Config**: If config files don't exist, the system will automatically fall back to using `local.properties` file.

3. **构建日志**: 构建时会在控制台输出当前加载的环境配置信息。
   **Build Logs**: Current loaded environment configuration information will be output to console during build.

## 示例场景 Example Scenarios

### 开发阶段 Development Phase
```properties
# config/environment.properties
current.environment=dev
```
应用将使用开发环境配置，包括开发API地址、调试日志等。

### 发布阶段 Release Phase
```properties
# config/environment.properties
current.environment=prod
```
应用将使用正式环境配置，包括正式API地址、关闭调试功能等。