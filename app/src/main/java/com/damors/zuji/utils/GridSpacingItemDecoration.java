package com.damors.zuji.utils;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 网格间距装饰器
 * 用于为RecyclerView的网格布局添加间距
 */
public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
    
    private int spanCount; // 列数
    private int spacing; // 间距（dp转换为px）
    private boolean includeEdge; // 是否包含边缘间距
    
    /**
     * 构造函数
     * @param spanCount 网格列数
     * @param spacing 间距大小（dp）
     * @param includeEdge 是否包含边缘间距
     */
    public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
        this.spanCount = spanCount;
        this.spacing = dpToPx(spacing);
        this.includeEdge = includeEdge;
    }
    
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item位置
        int column = position % spanCount; // item列索引
        
        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
            outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)
            
            if (position < spanCount) { // 第一行
                outRect.top = spacing;
            }
            outRect.bottom = spacing; // item底部间距
        } else {
            outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
            outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
            if (position >= spanCount) {
                outRect.top = spacing; // item顶部间距
            }
        }
    }
    
    /**
     * 将dp转换为px
     * @param dp dp值
     * @return px值
     */
    private int dpToPx(int dp) {
        return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }
}