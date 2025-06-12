package com.damors.zuji.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * 支持双指缩放和拖拽的自定义ImageView
 * 提供流畅的图片缩放和移动体验
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
public class ZoomableImageView extends AppCompatImageView implements ScaleGestureDetector.OnScaleGestureListener {
    
    private static final String TAG = "ZoomableImageView";
    
    // 缩放相关参数
    private static final float MIN_SCALE = 1.0f;  // 最小缩放倍数
    private static final float MAX_SCALE = 5.0f;  // 最大缩放倍数
    private static final float DEFAULT_SCALE = 1.0f; // 默认缩放倍数
    
    // 手势检测器
    private ScaleGestureDetector scaleGestureDetector;
    
    // 矩阵相关
    private Matrix matrix;
    private Matrix savedMatrix;
    
    // 缩放和移动状态
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    
    // 触摸点
    private PointF start = new PointF();
    private PointF mid = new PointF();
    
    // 当前缩放倍数
    private float currentScale = DEFAULT_SCALE;
    
    // 双击检测
    private long lastTouchTime = 0;
    private static final long DOUBLE_TAP_TIMEOUT = 300;
    
    /**
     * 构造函数
     */
    public ZoomableImageView(Context context) {
        super(context);
        init();
    }
    
    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    /**
     * 初始化方法
     */
    private void init() {
        matrix = new Matrix();
        savedMatrix = new Matrix();
        scaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        setScaleType(ScaleType.MATRIX);
        setImageMatrix(matrix);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 处理缩放手势
        scaleGestureDetector.onTouchEvent(event);
        
        PointF curr = new PointF(event.getX(), event.getY());
        
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(curr);
                mode = DRAG;
                
                // 检测双击
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTouchTime < DOUBLE_TAP_TIMEOUT) {
                    handleDoubleTap(curr);
                }
                lastTouchTime = currentTime;
                break;
                
            case MotionEvent.ACTION_POINTER_DOWN:
                savedMatrix.set(matrix);
                midPoint(mid, event);
                mode = ZOOM;
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG && currentScale > MIN_SCALE) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(curr.x - start.x, curr.y - start.y);
                    limitTranslation();
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }
        
        setImageMatrix(matrix);
        return true;
    }
    
    /**
     * 处理双击事件
     */
    private void handleDoubleTap(PointF point) {
        if (currentScale > MIN_SCALE) {
            // 如果已经放大，则缩小到原始大小
            resetZoom();
        } else {
            // 如果是原始大小，则放大到2倍
            zoomToPoint(2.0f, point.x, point.y);
        }
    }
    
    /**
     * 缩放到指定倍数和位置
     */
    private void zoomToPoint(float scale, float x, float y) {
        matrix.postScale(scale / currentScale, scale / currentScale, x, y);
        currentScale = scale;
        limitTranslation();
        setImageMatrix(matrix);
    }
    
    /**
     * 重置缩放
     */
    public void resetZoom() {
        matrix.reset();
        currentScale = DEFAULT_SCALE;
        setImageMatrix(matrix);
    }
    
    /**
     * 限制图片移动范围
     */
    private void limitTranslation() {
        float[] values = new float[9];
        matrix.getValues(values);
        
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];
        
        Drawable drawable = getDrawable();
        if (drawable == null) return;
        
        float imageWidth = drawable.getIntrinsicWidth() * scaleX;
        float imageHeight = drawable.getIntrinsicHeight() * scaleY;
        
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        
        float deltaX = 0;
        float deltaY = 0;
        
        // 限制水平移动
        if (imageWidth <= viewWidth) {
            deltaX = (viewWidth - imageWidth) / 2 - transX;
        } else {
            if (transX > 0) {
                deltaX = -transX;
            } else if (transX < viewWidth - imageWidth) {
                deltaX = viewWidth - imageWidth - transX;
            }
        }
        
        // 限制垂直移动
        if (imageHeight <= viewHeight) {
            deltaY = (viewHeight - imageHeight) / 2 - transY;
        } else {
            if (transY > 0) {
                deltaY = -transY;
            } else if (transY < viewHeight - imageHeight) {
                deltaY = viewHeight - imageHeight - transY;
            }
        }
        
        matrix.postTranslate(deltaX, deltaY);
    }
    
    /**
     * 计算两点中点
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
    
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        float newScale = currentScale * scaleFactor;
        
        // 限制缩放范围
        if (newScale < MIN_SCALE) {
            scaleFactor = MIN_SCALE / currentScale;
            newScale = MIN_SCALE;
        } else if (newScale > MAX_SCALE) {
            scaleFactor = MAX_SCALE / currentScale;
            newScale = MAX_SCALE;
        }
        
        matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
        currentScale = newScale;
        limitTranslation();
        
        Log.d(TAG, "当前缩放倍数: " + currentScale);
        return true;
    }
    
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mode = ZOOM;
        return true;
    }
    
    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        // 缩放结束后的处理
    }
    
    /**
     * 获取当前缩放倍数
     */
    public float getCurrentScale() {
        return currentScale;
    }
    
    /**
     * 设置最大缩放倍数
     */
    public void setMaxScale(float maxScale) {
        // 可以根据需要动态设置最大缩放倍数
    }
    
    /**
     * 设置最小缩放倍数
     */
    public void setMinScale(float minScale) {
        // 可以根据需要动态设置最小缩放倍数
    }
}