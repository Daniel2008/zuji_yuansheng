package com.damors.zuji.manager;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.damors.zuji.R;
import com.damors.zuji.activity.CommentManagementActivity;
import com.damors.zuji.activity.MainActivity;
import com.damors.zuji.fragment.ProfileFragment;
import com.damors.zuji.model.UserInfoModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 角标管理器
 * 统一管理底部导航栏、我的页面评论管理角标及评论管理页面角标的更新
 */
public class BadgeManager {
    private static final String TAG = "BadgeManager";
    private static BadgeManager instance;
    
    // 使用弱引用避免内存泄漏
    private List<WeakReference<BadgeUpdateListener>> listeners = new ArrayList<>();
    
    // 当前未读评论数量
    private int currentUnreadCount = 0;
    
    private BadgeManager() {}
    
    public static synchronized BadgeManager getInstance() {
        if (instance == null) {
            instance = new BadgeManager();
        }
        return instance;
    }
    
    /**
     * 角标更新监听器接口
     */
    public interface BadgeUpdateListener {
        void onBadgeUpdate(int unreadCount);
    }
    
    /**
     * 注册角标更新监听器
     * @param listener 监听器
     */
    public void registerListener(BadgeUpdateListener listener) {
        if (listener != null) {
            listeners.add(new WeakReference<>(listener));
            Log.d(TAG, "注册角标更新监听器，当前监听器数量: " + listeners.size());
        }
    }
    
    /**
     * 注销角标更新监听器
     * @param listener 监听器
     */
    public void unregisterListener(BadgeUpdateListener listener) {
        if (listener != null) {
            Iterator<WeakReference<BadgeUpdateListener>> iterator = listeners.iterator();
            while (iterator.hasNext()) {
                WeakReference<BadgeUpdateListener> ref = iterator.next();
                BadgeUpdateListener l = ref.get();
                if (l == null || l == listener) {
                    iterator.remove();
                }
            }
            Log.d(TAG, "注销角标更新监听器，当前监听器数量: " + listeners.size());
        }
    }
    
    /**
     * 更新角标数量
     * @param unreadCount 未读评论数量
     */
    public void updateBadgeCount(int unreadCount) {
        currentUnreadCount = unreadCount;
        Log.d(TAG, "更新角标数量: " + unreadCount);
        
        // 通知所有监听器
        notifyListeners(unreadCount);
    }
    
    /**
     * 从用户信息更新角标
     */
    public void updateFromUserInfo() {
        try {
            UserInfoModel userInfo = UserManager.getInstance().getUserInfo();
            if (userInfo != null && userInfo.getCommentNoReplyCount() != null) {
                int unreadCount = userInfo.getCommentNoReplyCount().intValue();
                updateBadgeCount(unreadCount);
            } else {
                updateBadgeCount(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "从用户信息更新角标失败: " + e.getMessage(), e);
            updateBadgeCount(0);
        }
    }
    
    /**
     * 减少未读数量
     * @param count 减少的数量
     */
    public void decreaseUnreadCount(int count) {
        int newCount = Math.max(0, currentUnreadCount - count);
        updateBadgeCount(newCount);
    }
    
    /**
     * 增加未读数量
     * @param count 增加的数量
     */
    public void increaseUnreadCount(int count) {
        int newCount = currentUnreadCount + count;
        updateBadgeCount(newCount);
    }
    
    /**
     * 获取当前未读数量
     * @return 未读数量
     */
    public int getCurrentUnreadCount() {
        return currentUnreadCount;
    }
    
    /**
     * 通知所有监听器
     * @param unreadCount 未读数量
     */
    private void notifyListeners(int unreadCount) {
        Iterator<WeakReference<BadgeUpdateListener>> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            WeakReference<BadgeUpdateListener> ref = iterator.next();
            BadgeUpdateListener listener = ref.get();
            if (listener == null) {
                // 移除已被回收的监听器
                iterator.remove();
            } else {
                try {
                    listener.onBadgeUpdate(unreadCount);
                } catch (Exception e) {
                    Log.e(TAG, "通知监听器失败: " + e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * 更新MainActivity中的底部导航栏角标
     * @param activity MainActivity实例
     */
    public static void updateMainActivityBadge(MainActivity activity) {
        if (activity != null) {
            View redDotBadge = activity.getRedDotBadgeView();
            if (redDotBadge != null) {
                int count = UserManager.getUserInfo().getCommentNoReplyCount().intValue();
                if (count > 0) {
                    redDotBadge.setVisibility(View.VISIBLE);
                } else {
                    redDotBadge.setVisibility(View.GONE);
                }
            }
        }
    }
    
    /**
     * 更新ProfileFragment中的评论管理角标
     * @param fragment ProfileFragment实例
     */
    public static void updateProfileFragmentBadge(ProfileFragment fragment) {
        if (fragment != null && fragment.getView() != null) {
            TextView textViewCommentUnreadCount = fragment.getCommentUnreadCountTextView();
            if (textViewCommentUnreadCount != null) {
                int count = UserManager.getUserInfo().getCommentNoReplyCount().intValue();
                if (count > 0) {
                    textViewCommentUnreadCount.setText(String.valueOf(count));
                    textViewCommentUnreadCount.setVisibility(View.VISIBLE);
                } else {
                    textViewCommentUnreadCount.setVisibility(View.GONE);
                }
            }
        }
    }
    
    /**
     * 更新CommentManagementActivity中的角标
     * @param activity CommentManagementActivity实例
     */
    public static void updateCommentManagementActivityBadge(CommentManagementActivity activity) {
        if (activity != null) {
            TextView tvUnreadCount = activity.getUnreadCountTextView();
            if (tvUnreadCount != null) {
                int count = UserManager.getUserInfo().getCommentNoReplyCount().intValue();
                if (count > 0) {
                    tvUnreadCount.setText(String.valueOf(count));
                    tvUnreadCount.setVisibility(View.VISIBLE);
                } else {
                    tvUnreadCount.setVisibility(View.GONE);
                }
            }
        }
    }
    
    /**
     * 统一更新所有角标
     * 通过Context查找当前活动的Activity并更新对应角标
     * @param context 上下文
     */
    public static void updateAllBadges(Context context) {
        try {
            // 更新BadgeManager实例
            getInstance().updateFromUserInfo();
            
            // 如果context是Activity，尝试直接更新
            if (context instanceof MainActivity) {
                updateMainActivityBadge((MainActivity) context);
            } else if (context instanceof CommentManagementActivity) {
                updateCommentManagementActivityBadge((CommentManagementActivity) context);
            }
            
            Log.d(TAG, "统一更新所有角标完成");
        } catch (Exception e) {
            Log.e(TAG, "统一更新所有角标失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理所有监听器
     */
    public void clearAllListeners() {
        listeners.clear();
        Log.d(TAG, "清理所有角标监听器");
    }
}