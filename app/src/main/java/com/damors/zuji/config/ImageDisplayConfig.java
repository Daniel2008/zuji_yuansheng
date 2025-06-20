package com.damors.zuji.config;

import android.content.Context;

/**
 * 图片显示配置常量类
 * 统一管理图片显示相关的配置参数
 */
public class ImageDisplayConfig {
    
    /** 网格布局最大显示图片数量 */
    public static final int MAX_GRID_DISPLAY_COUNT = 9;
    
    /** 网格布局列数 */
    public static final int GRID_SPAN_COUNT = 3;
    
    /** 网格间距（dp） */
    public static final int GRID_SPACING_DP = 8;
    
    /** 单张图片默认高度（dp） */
    public static final int SINGLE_IMAGE_HEIGHT_DP = 200;
    
    /** 多张图片布局高度（dp） */
    public static final int MULTI_IMAGE_HEIGHT_DP = 150;
    
    /** 网格图片项高度（dp） */
    public static final int GRID_ITEM_HEIGHT_DP = 100;
    
    /** 图片加载缩略图比例 */
    public static final float THUMBNAIL_RATIO = 0.1f;
    
    /** RecyclerView最小高度（dp） */
    public static final int RECYCLER_VIEW_MIN_HEIGHT_DP = 200;
    
    /** 图片加载超时时间（毫秒） */
    public static final int IMAGE_LOAD_TIMEOUT_MS = 10000;
    
    /** 图片缓存策略 */
    public static final String CACHE_STRATEGY = "ALL";
    
    /**
     * 私有构造函数，防止实例化
     */
    private ImageDisplayConfig() {
        throw new UnsupportedOperationException("这是一个工具类，不能被实例化");
    }
    
    /**
     * 获取网格布局的最小高度（像素）
     * @param context 上下文
     * @return 最小高度（像素）
     */
    public static int getMinGridHeight(Context context) {
        return dpToPx(context, RECYCLER_VIEW_MIN_HEIGHT_DP);
    }
    
    /**
     * 将dp转换为像素
     * @param context 上下文
     * @param dp dp值
     * @return 像素值
     */
    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    public static int getRecyclerViewMinHeightPx(android.content.Context context) {
        return dpToPx(context, RECYCLER_VIEW_MIN_HEIGHT_DP);
    }
    
    /**
     * 获取网格间距（像素）
     * @param context 上下文
     * @return 间距（像素）
     */
    public static int getGridSpacingPx(android.content.Context context) {
        return dpToPx(context, GRID_SPACING_DP);
    }

}