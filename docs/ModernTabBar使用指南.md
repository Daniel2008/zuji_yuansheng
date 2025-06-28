# ModernTabBar 使用指南

## 概述

ModernTabBar 是一个现代化的 Android TabBar 组件，提供了丰富的视觉效果和流畅的动画体验。

## 特性

### 🎨 视觉特性
- **渐变背景**: 支持多种渐变效果
- **圆角设计**: 现代化的圆角外观
- **动态指示器**: 流畅的滑动动画
- **图标支持**: 每个Tab都可以添加图标
- **状态反馈**: 按下、选中状态的视觉反馈
- **阴影效果**: 立体感的阴影设计

### 🔧 功能特性
- **动画切换**: 300ms 的流畅切换动画
- **响应式设计**: 自适应不同屏幕尺寸
- **主题支持**: 支持日间/夜间模式
- **多种尺寸**: 紧凑型、标准型、大型三种尺寸
- **自定义颜色**: 可自定义激活/非激活状态颜色

## 快速开始

### 1. 在布局文件中使用

```xml
<com.damors.zuji.ui.widget.ModernTabBar
    android:id="@+id/modern_tab_bar"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_marginStart="60dp"
    android:layout_marginTop="16dp"
    android:layout_marginEnd="60dp"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent" />
```

### 2. 在代码中使用

```java
ModernTabBar tabBar = findViewById(R.id.modern_tab_bar);

// 设置Tab选择监听器
tabBar.setOnTabSelectedListener(new ModernTabBar.OnTabSelectedListener() {
    @Override
    public void onTabSelected(boolean isPersonal) {
        if (isPersonal) {
            // 处理个人Tab选中
            showPersonalContent();
        } else {
            // 处理世界Tab选中
            showWorldContent();
        }
    }
});

// 程序化选择Tab（无动画）
tabBar.setSelectedTab(false); // 选择世界Tab

// 程序化选择Tab（有动画）
tabBar.selectTab(true); // 选择个人Tab

// 获取当前选中状态
boolean isPersonalSelected = tabBar.isPersonalSelected();
```

## 主题和样式

### 标准主题
```xml
<com.damors.zuji.ui.widget.ModernTabBar
    style="@style/ModernTabBarTheme"
    ... />
```

### 夜间模式主题
```xml
<com.damors.zuji.ui.widget.ModernTabBar
    style="@style/ModernTabBarTheme.Night"
    ... />
```

### 紧凑型样式
```xml
<com.damors.zuji.ui.widget.ModernTabBar
    style="@style/ModernTabBarTheme.Compact"
    ... />
```

### 大型样式
```xml
<com.damors.zuji.ui.widget.ModernTabBar
    style="@style/ModernTabBarTheme.Large"
    ... />
```

## 自定义配置

### 颜色自定义

在 `colors.xml` 中定义自定义颜色：

```xml
<!-- 自定义TabBar颜色 -->
<color name="custom_tab_active">#FF6B35</color>
<color name="custom_tab_inactive">#BDBDBD</color>
<color name="custom_tab_background">#F8F9FA</color>
```

### 背景自定义

创建自定义背景 drawable：

```xml
<!-- custom_tab_background.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:startColor="@color/custom_tab_background"
        android:endColor="#FFFFFF"
        android:angle="90" />
    <corners android:radius="24dp" />
    <stroke
        android:width="1dp"
        android:color="@color/custom_tab_active" />
</shape>
```

### 指示器自定义

```xml
<!-- custom_tab_indicator.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:startColor="@color/custom_tab_active"
        android:endColor="#FF8A65"
        android:angle="45" />
    <corners android:radius="20dp" />
</shape>
```

## 动画配置

### 修改动画时长

在 `ModernTabBar.java` 中修改：

```java
// 默认300ms，可以调整为其他值
indicatorAnimator.setDuration(500); // 500ms动画
```

### 修改动画插值器

```java
// 使用不同的插值器
indicatorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
// 或者
indicatorAnimator.setInterpolator(new BounceInterpolator());
```

## 最佳实践

### 1. 性能优化
- 避免频繁切换Tab
- 在Fragment销毁时取消动画
- 使用合适的图片尺寸

### 2. 用户体验
- 保持Tab标签简洁明了
- 使用有意义的图标
- 确保足够的点击区域

### 3. 响应式设计
- 在不同屏幕尺寸上测试
- 考虑横屏模式的适配
- 使用dp单位而非px

## 故障排除

### 常见问题

**Q: 指示器动画不流畅？**
A: 检查是否在主线程中执行动画，避免在动画过程中进行耗时操作。

**Q: Tab点击无响应？**
A: 确保设置了 `OnTabSelectedListener` 并且Tab的 `clickable` 和 `focusable` 属性为 `true`。

**Q: 在不同设备上显示效果不一致？**
A: 使用dp单位，并在不同密度的设备上测试。

**Q: 夜间模式切换后样式异常？**
A: 确保在主题切换后重新设置TabBar的样式。

## 版本更新

### v1.0.0
- 初始版本发布
- 支持基础的Tab切换功能
- 包含动画效果和主题支持

### 未来计划
- 支持更多Tab数量
- 添加徽章(Badge)功能
- 支持自定义Tab内容
- 添加手势滑动切换

## 贡献

欢迎提交Issue和Pull Request来改进这个组件！

## 许可证

本组件遵循项目的开源许可证。