package com.damors.zuji.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;

/**
 * 动画性能监控工具类
 * 用于监控动画性能和检测掉帧情况
 * 
 * 功能说明：
 * 1. 监控帧率变化
 * 2. 检测掉帧情况
 * 3. 提供性能优化建议
 * 
 * @author 开发者
 * @version 1.0
 * @since 2024
 */
public class AnimationPerformanceMonitor {
    
    private static final String TAG = "AnimationPerformanceMonitor";
    private static final long FRAME_INTERVAL_NANOS = 16_666_666L; // 60fps = 16.67ms
    private static final int DROPPED_FRAME_THRESHOLD = 2; // 掉帧阈值
    
    private static AnimationPerformanceMonitor instance;
    private Choreographer choreographer;
    private Handler mainHandler;
    private boolean isMonitoring = false;
    
    // 性能统计
    private long lastFrameTime = 0;
    private int totalFrames = 0;
    private int droppedFrames = 0;
    private long monitorStartTime = 0;
    
    private AnimationPerformanceMonitor() {
        choreographer = Choreographer.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized AnimationPerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new AnimationPerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * 开始监控动画性能
     */
    public void startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "性能监控已在运行中");
            return;
        }
        
        isMonitoring = true;
        lastFrameTime = 0;
        totalFrames = 0;
        droppedFrames = 0;
        monitorStartTime = System.currentTimeMillis();
        
        choreographer.postFrameCallback(frameCallback);
        Log.d(TAG, "开始监控动画性能");
    }
    
    /**
     * 停止监控并输出性能报告
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            Log.w(TAG, "性能监控未在运行");
            return;
        }
        
        isMonitoring = false;
        choreographer.removeFrameCallback(frameCallback);
        
        // 输出性能报告
        generatePerformanceReport();
        Log.d(TAG, "停止监控动画性能");
    }
    
    /**
     * 帧回调监听器
     */
    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!isMonitoring) {
                return;
            }
            
            totalFrames++;
            
            if (lastFrameTime != 0) {
                long frameDuration = frameTimeNanos - lastFrameTime;
                long expectedFrames = frameDuration / FRAME_INTERVAL_NANOS;
                
                // 检测掉帧
                if (expectedFrames > DROPPED_FRAME_THRESHOLD) {
                    droppedFrames += (expectedFrames - 1);
                    Log.w(TAG, String.format("检测到掉帧: %d帧, 帧间隔: %.2fms", 
                            expectedFrames - 1, frameDuration / 1_000_000.0));
                }
            }
            
            lastFrameTime = frameTimeNanos;
            
            // 继续监控下一帧
            choreographer.postFrameCallback(this);
        }
    };
    
    /**
     * 生成性能报告
     */
    private void generatePerformanceReport() {
        long monitorDuration = System.currentTimeMillis() - monitorStartTime;
        double averageFps = totalFrames * 1000.0 / monitorDuration;
        double dropRate = totalFrames > 0 ? (droppedFrames * 100.0 / totalFrames) : 0;
        
        Log.i(TAG, "=== 动画性能报告 ===");
        Log.i(TAG, String.format("监控时长: %dms", monitorDuration));
        Log.i(TAG, String.format("总帧数: %d", totalFrames));
        Log.i(TAG, String.format("掉帧数: %d", droppedFrames));
        Log.i(TAG, String.format("平均帧率: %.1f fps", averageFps));
        Log.i(TAG, String.format("掉帧率: %.2f%%", dropRate));
        
        // 性能评估和建议
        if (averageFps >= 55) {
            Log.i(TAG, "性能评估: 优秀 - 动画流畅度很好");
        } else if (averageFps >= 45) {
            Log.i(TAG, "性能评估: 良好 - 动画基本流畅");
        } else if (averageFps >= 30) {
            Log.w(TAG, "性能评估: 一般 - 建议优化动画复杂度");
        } else {
            Log.e(TAG, "性能评估: 较差 - 需要立即优化动画性能");
        }
        
        if (dropRate > 5) {
            Log.w(TAG, "优化建议: 掉帧率较高，建议:");
            Log.w(TAG, "1. 启用硬件加速");
            Log.w(TAG, "2. 减少同时运行的动画数量");
            Log.w(TAG, "3. 缩短动画持续时间");
            Log.w(TAG, "4. 使用ViewPropertyAnimator替代ObjectAnimator");
        }
        
        Log.i(TAG, "===================");
    }
    
    /**
     * 获取当前帧率
     */
    public double getCurrentFps() {
        if (!isMonitoring || monitorStartTime == 0) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long duration = currentTime - monitorStartTime;
        return duration > 0 ? (totalFrames * 1000.0 / duration) : 0;
    }
    
    /**
     * 获取掉帧率
     */
    public double getDropRate() {
        return totalFrames > 0 ? (droppedFrames * 100.0 / totalFrames) : 0;
    }
    
    /**
     * 是否正在监控
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }
}