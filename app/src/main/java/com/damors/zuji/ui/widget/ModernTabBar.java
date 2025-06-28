package com.damors.zuji.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.damors.zuji.R;

/**
 * 现代化TabBar组件
 * 支持动画切换、图标显示、状态管理等功能
 */
public class ModernTabBar extends FrameLayout {
    
    private LinearLayout tabPersonal;
    private LinearLayout tabWorld;
    private ImageView iconPersonal;
    private ImageView iconWorld;
    private TextView textPersonal;
    private TextView textWorld;
    private View indicator;
    
    private boolean isPersonalSelected = true;
    private OnTabSelectedListener listener;
    private ValueAnimator indicatorAnimator;
    
    // 颜色资源
    private int activeColor;
    private int inactiveColor;
    private int activeIconColor;
    private int inactiveIconColor;
    
    public interface OnTabSelectedListener {
        void onTabSelected(boolean isPersonal);
    }
    
    public ModernTabBar(Context context) {
        super(context);
        init(context);
    }
    
    public ModernTabBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public ModernTabBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    /**
     * 初始化TabBar组件
     */
    private void init(Context context) {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.modern_tab_bar, this, true);
        
        // 初始化颜色资源
        initColors();
        
        // 绑定视图
        bindViews();
        
        // 设置点击监听
        setupClickListeners();
        
        // 更新初始状态
        updateTabState(false);
        
        // 调试：确保整个TabBar可见
        Log.d("ModernTabBar", "TabBar初始化完成，可见性: " + getVisibility() + ", 透明度: " + getAlpha());
        setVisibility(View.VISIBLE);
        setAlpha(1.0f);
        
        // 延迟初始化指示器位置，确保布局完成
        post(this::initializeIndicator);
    }
    
    /**
     * 初始化颜色资源
     */
    private void initColors() {
        activeColor = ContextCompat.getColor(getContext(), R.color.indigo_500);
        inactiveColor = ContextCompat.getColor(getContext(), R.color.colorDarkGray);
        activeIconColor = ContextCompat.getColor(getContext(), R.color.indigo_500);
        inactiveIconColor = ContextCompat.getColor(getContext(), R.color.colorDarkGray);
    }
    
    /**
     * 绑定视图组件
     */
    private void bindViews() {
        tabPersonal = findViewById(R.id.modern_tab_personal);
        tabWorld = findViewById(R.id.modern_tab_world);
        iconPersonal = findViewById(R.id.icon_personal);
        iconWorld = findViewById(R.id.icon_world);
        textPersonal = findViewById(R.id.text_personal);
        textWorld = findViewById(R.id.text_world);
        indicator = findViewById(R.id.modern_tab_indicator);
        
        // 调试：确保文本内容正确设置
        if (textPersonal != null) {
            textPersonal.setText("个人");
            textPersonal.setVisibility(View.VISIBLE);
            textPersonal.setAlpha(1.0f);
            Log.d("ModernTabBar", "个人文本设置: " + textPersonal.getText() + ", 大小: " + textPersonal.getTextSize() + ", 可见性: " + textPersonal.getVisibility());
        }
        if (textWorld != null) {
            textWorld.setText("世界");
            textWorld.setVisibility(View.VISIBLE);
            textWorld.setAlpha(1.0f);
            Log.d("ModernTabBar", "世界文本设置: " + textWorld.getText() + ", 大小: " + textWorld.getTextSize() + ", 可见性: " + textWorld.getVisibility());
        }
    }
    
    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        tabPersonal.setOnClickListener(v -> selectTab(true));
        tabWorld.setOnClickListener(v -> selectTab(false));
    }
    
    /**
     * 选择Tab
     * @param isPersonal 是否选择个人Tab
     */
    public void selectTab(boolean isPersonal) {
        if (isPersonalSelected != isPersonal) {
            isPersonalSelected = isPersonal;
            updateTabState(true);
            
            if (listener != null) {
                listener.onTabSelected(isPersonal);
            }
        }
    }
    
    /**
     * 更新Tab状态
     * @param withAnimation 是否使用动画
     */
    private void updateTabState(boolean withAnimation) {
        if (isPersonalSelected) {
            // 个人Tab激活
            updateTabAppearance(textPersonal, iconPersonal, tabPersonal, true);
            updateTabAppearance(textWorld, iconWorld, tabWorld, false);
        } else {
            // 世界Tab激活
            updateTabAppearance(textWorld, iconWorld, tabWorld, true);
            updateTabAppearance(textPersonal, iconPersonal, tabPersonal, false);
        }
        
        // 更新指示器位置
        if (withAnimation) {
            animateIndicator();
        } else {
            updateIndicatorPosition();
        }
    }
    
    /**
     * 更新Tab外观
     */
    private void updateTabAppearance(TextView text, ImageView icon, LinearLayout tab, boolean isActive) {
        if (isActive) {
            text.setTextColor(activeColor);
            text.setTypeface(null, android.graphics.Typeface.BOLD);
            icon.setColorFilter(activeIconColor);
            tab.setSelected(true);
            Log.d("ModernTabBar", "激活Tab - 文本: " + text.getText() + ", 颜色: " + Integer.toHexString(activeColor));
        } else {
            text.setTextColor(inactiveColor);
            text.setTypeface(null, android.graphics.Typeface.NORMAL);
            icon.setColorFilter(inactiveIconColor);
            tab.setSelected(false);
            Log.d("ModernTabBar", "非激活Tab - 文本: " + text.getText() + ", 颜色: " + Integer.toHexString(inactiveColor));
        }
        
        // 强制设置文本可见性和透明度
        text.setVisibility(View.VISIBLE);
        text.setAlpha(1.0f);
    }
    
    /**
     * 动画更新指示器位置
     */
    private void animateIndicator() {
        if (indicatorAnimator != null && indicatorAnimator.isRunning()) {
            indicatorAnimator.cancel();
        }
        
        int containerWidth = getWidth();
        if (containerWidth == 0) {
            // 如果容器宽度未测量完成，延迟执行
            post(this::animateIndicator);
            return;
        }
        
        // 获取Tab按钮的实际位置和宽度
        LinearLayout targetTab = isPersonalSelected ? tabPersonal : tabWorld;
        if (targetTab == null) {
            Log.e("ModernTabBar", "目标Tab为空，无法计算指示器位置");
            return;
        }
        
        // 等待Tab布局完成
        if (targetTab.getWidth() == 0) {
            post(this::animateIndicator);
            return;
        }
        
        float currentX = indicator.getX();
        float targetX = targetTab.getX() + (targetTab.getWidth() - indicator.getLayoutParams().width) / 2f;
        
        Log.d("ModernTabBar", "指示器动画 - 当前选择: " + (isPersonalSelected ? "个人" : "世界") + 
              ", 目标X: " + targetX + ", 当前X: " + currentX + ", Tab宽度: " + targetTab.getWidth());
        
        // 创建位置动画
        indicatorAnimator = ValueAnimator.ofFloat(currentX, targetX);
        indicatorAnimator.setDuration(300);
        indicatorAnimator.setInterpolator(new DecelerateInterpolator());
        indicatorAnimator.addUpdateListener(animation -> {
            float value = (Float) animation.getAnimatedValue();
            indicator.setX(value);
        });
        
        // 确保指示器可见
        indicator.setVisibility(View.VISIBLE);
        indicator.setAlpha(1.0f);
        
        indicatorAnimator.start();
    }
    
    /**
     * 直接更新指示器位置（无动画）
     */
    private void updateIndicatorPosition() {
        post(() -> {
            // 获取目标Tab
            LinearLayout targetTab = isPersonalSelected ? tabPersonal : tabWorld;
            if (targetTab == null || targetTab.getWidth() == 0) {
                // 如果Tab还未布局完成，再次延迟执行
                post(this::updateIndicatorPosition);
                return;
            }
            
            // 计算指示器宽度（Tab宽度的80%）
            int indicatorWidth = (int) (targetTab.getWidth() * 0.8f);
            
            // 计算指示器位置（居中对齐到目标Tab）
            float targetX = targetTab.getX() + (targetTab.getWidth() - indicatorWidth) / 2f;
            
            Log.d("ModernTabBar", "更新指示器位置 - 选择: " + (isPersonalSelected ? "个人" : "世界") + 
                  ", X: " + targetX + ", 宽度: " + indicatorWidth + ", Tab宽度: " + targetTab.getWidth());
            
            // 设置指示器位置和宽度
            indicator.setX(targetX);
            indicator.getLayoutParams().width = indicatorWidth;
            indicator.setVisibility(View.VISIBLE);
            indicator.setAlpha(1.0f);
            indicator.requestLayout();
        });
    }
   /**
     * 设置Tab选择监听器
     * @param listener 监听器
     */
    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    
    /**
     * 初始化指示器位置和样式
     */
    private void initializeIndicator() {
        if (indicator == null) {
            Log.e("ModernTabBar", "指示器为空，无法初始化");
            return;
        }
        
        // 获取当前选中的Tab
        LinearLayout selectedTab = isPersonalSelected ? tabPersonal : tabWorld;
        if (selectedTab == null || selectedTab.getWidth() == 0) {
            // 如果Tab还未布局完成，延迟执行
            post(this::initializeIndicator);
            return;
        }
        
        // 计算指示器宽度（Tab宽度的80%）
        int indicatorWidth = (int) (selectedTab.getWidth() * 0.8f);
        
        // 计算指示器位置（居中对齐到选中Tab）
        float indicatorX = selectedTab.getX() + (selectedTab.getWidth() - indicatorWidth) / 2f;
        
        // 设置指示器属性
        indicator.getLayoutParams().width = indicatorWidth;
        indicator.setX(indicatorX);
        indicator.setVisibility(View.VISIBLE);
        indicator.setAlpha(1.0f);
        indicator.requestLayout();
        
        Log.d("ModernTabBar", "指示器初始化完成 - 位置: " + indicatorX + ", 宽度: " + indicatorWidth + 
              ", 选中Tab: " + (isPersonalSelected ? "个人" : "世界"));
    }
    
    /**
     * 获取当前选中状态
     */
    public boolean isPersonalSelected() {
        return isPersonalSelected;
    }
    
    /**
     * 设置选中状态（无动画）
     */
    public void setSelectedTab(boolean isPersonal) {
        isPersonalSelected = isPersonal;
        updateTabState(false);
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (indicatorAnimator != null && indicatorAnimator.isRunning()) {
            indicatorAnimator.cancel();
        }
    }
}