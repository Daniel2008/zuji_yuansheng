package com.damors.zuji.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.damors.zuji.R;
import com.damors.zuji.activity.EditProfileActivity;
import com.damors.zuji.activity.SettingsActivity;
import com.damors.zuji.activity.CommentManagementActivity;
import com.damors.zuji.activity.LikeManagementActivity;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.model.UserInfoModel;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.utils.GsonUtil;
import com.damors.zuji.utils.MapCacheManager;
import com.damors.zuji.viewmodel.FootprintViewModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 个人资料Fragment
 * 显示用户信息和应用统计数据
 */
public class ProfileFragment extends Fragment {

    private FootprintViewModel viewModel;
    private TextView textViewFootprintCount;
    private TextView textViewCityCount;
    private TextView textViewDaysCount;
    private TextView textViewCacheSize;
    private TextView textViewUsername;
    private TextView textViewBio;
    private TextView textViewCommentUnreadCount; // 未读评论数标志
    private CircleImageView imageViewAvatar;
    private LinearLayout layoutLikeManagement;
    private LinearLayout layoutCommentManagement;
    private LinearLayout layoutMapCache;
    private LinearLayout layoutSettings;

    private ExecutorService executorService;
    private Handler mainHandler;
    private UserManager userManager;
    
    // 刷新控制相关变量
    private long lastRefreshTime = 0; // 上次刷新时间
    private static final long MIN_REFRESH_INTERVAL = 20000; // 最小刷新间隔5秒
    private boolean isFirstLoad = true; // 是否首次加载

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // 初始化视图组件
        textViewFootprintCount = view.findViewById(R.id.text_view_footprint_count);
        textViewCityCount = view.findViewById(R.id.text_view_city_count);
        textViewDaysCount = view.findViewById(R.id.text_view_days_count);
        textViewUsername = view.findViewById(R.id.text_view_username);
        textViewBio = view.findViewById(R.id.text_view_bio);
        textViewCommentUnreadCount = view.findViewById(R.id.tv_comment_unread_count); // 初始化未读评论数标志
        imageViewAvatar = view.findViewById(R.id.image_view_avatar);
        layoutLikeManagement = view.findViewById(R.id.layout_like_management);
        layoutCommentManagement = view.findViewById(R.id.layout_comment_management);
        layoutMapCache = view.findViewById(R.id.layout_map_cache);
        layoutSettings = view.findViewById(R.id.layout_settings);
        
        // 初始化编辑按钮
        ImageButton buttonEditProfile = view.findViewById(R.id.button_edit_profile);
        buttonEditProfile.setOnClickListener(v -> openEditProfileActivity());

        
        // 初始化线程池和Handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化UserManager
        userManager = UserManager.getInstance();
        
        // 设置点赞管理布局点击事件
        layoutLikeManagement.setOnClickListener(v -> {
            Log.d("ProfileFragment", "点赞管理布局被点击");
            try {
                // 跳转到点赞管理页面
                Intent intent = new Intent(getContext(), LikeManagementActivity.class);
                startActivity(intent);
                Log.d("ProfileFragment", "成功启动LikeManagementActivity");
            } catch (Exception e) {
                Log.e("ProfileFragment", "启动LikeManagementActivity失败: " + e.getMessage(), e);
                Toast.makeText(getContext(), "跳转失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置评论管理布局点击事件
        layoutCommentManagement.setOnClickListener(v -> {
            // 跳转到评论管理页面
            Intent intent = new Intent(getContext(), CommentManagementActivity.class);
            startActivity(intent);
        });
        
        // 设置地图缓存管理布局点击事件 - 直接打开离线地图管理
        layoutMapCache.setOnClickListener(v -> openOfflineMapManager());
        
        // 设置设置按钮点击事件
        layoutSettings.setOnClickListener(v -> openSettingsActivity());
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(FootprintViewModel.class);
        
        // 设置点赞管理布局点击事件
        layoutLikeManagement.setOnClickListener(v -> {
            Log.d("ProfileFragment", "点赞管理布局被点击");
            try {
                // 跳转到点赞管理页面
                Intent intent = new Intent(getContext(), LikeManagementActivity.class);
                startActivity(intent);
                Log.d("ProfileFragment", "成功启动LikeManagementActivity");
            } catch (Exception e) {
                Log.e("ProfileFragment", "启动LikeManagementActivity失败: " + e.getMessage(), e);
                Toast.makeText(getContext(), "跳转失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置评论管理布局点击事件
        layoutCommentManagement.setOnClickListener(v -> {
            // 跳转到评论管理页面
            Intent intent = new Intent(getContext(), CommentManagementActivity.class);
            startActivity(intent);
        });
        
        // 设置地图缓存管理布局点击事件 - 直接打开离线地图管理
        layoutMapCache.setOnClickListener(v -> openOfflineMapManager());
        
        // 刷新数据
        viewModel.refreshFootprints();
        
        // 延迟加载用户数据，确保UserManager完全初始化
        mainHandler.postDelayed(() -> {
            if (isAdded() && getContext() != null) {
                loadUserData();
            }
        }, 200);
        
        loadUserData();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // 页面启动时检查是否需要刷新用户数据
        loadUserData();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }


    
    /**
     * 打开高德地图原生离线地图管理界面
     */
    private void openOfflineMapManager() {
        try {
            // 直接启动高德地图SDK提供的原生离线地图管理界面
            Intent intent = new Intent(requireContext(), com.amap.api.maps.offlinemap.OfflineMapActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("ProfileFragment", "启动高德地图离线地图管理界面失败", e);
            Toast.makeText(requireContext(), "启动离线地图管理失败", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * 加载用户数据并更新UI
     */
    private void loadUserData() {
        Log.d("ProfileFragment", "开始加载用户数据");

        
        boolean isLoggedIn = UserManager.isLoggedIn();
        UserInfoModel userInfoModel = UserManager.getUserInfo();
        String token = userManager.getToken();
        
        if (null != userInfoModel) {
            Log.d("ProfileFragment", "用户JSON内容: " + GsonUtil.GsonString(userInfoModel));
        }
        
        if (!isLoggedIn) {
            Log.w("ProfileFragment", "用户未登录，显示默认信息");
            setDefaultUserInfo();
            return;
        }
        
        if (null == userInfoModel) {
            Log.w("ProfileFragment", "用户JSON为空，显示默认信息");
            setDefaultUserInfo();
            return;
        }
        
        try {

            // 更新用户头像
            String avatar = userInfoModel.getAvatar();
            if (!TextUtils.isEmpty(avatar) && imageViewAvatar != null) {
                // 拼接完整的头像URL
                String avatarUrl = ApiConfig.getBaseUrl() + avatar;
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .skipMemoryCache(true)  // 跳过内存缓存
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)  // 跳过磁盘缓存
                    .circleCrop()
                    .into(imageViewAvatar);
            }
            
            // 更新用户昵称
            String nickname = userInfoModel.getNickName();
            if (!TextUtils.isEmpty(nickname) && textViewUsername != null) {
                textViewUsername.setText(nickname);
            } else {
                String username = userInfoModel.getUserName();
                if (!TextUtils.isEmpty(username) && textViewUsername != null) {
                    textViewUsername.setText(username);
                }
            }
            
            // 更新足迹数
            String footPrintCountStr = String.valueOf(userInfoModel.getFootPrintCount());
            if (!TextUtils.isEmpty(footPrintCountStr) && textViewFootprintCount != null) {
                try {
                    textViewFootprintCount.setText(footPrintCountStr);
                } catch (NumberFormatException e) {
                    textViewFootprintCount.setText("0");
                }
            }
            
            // 更新城市数
            String cityCountStr = String.valueOf(userInfoModel.getCityCount());
            if (!TextUtils.isEmpty(cityCountStr) && textViewCityCount != null) {
                try {
                    textViewCityCount.setText(cityCountStr);
                } catch (NumberFormatException e) {
                    textViewCityCount.setText("0");
                }
            }
            
            // 更新天数
            String dayCountStr = String.valueOf(userInfoModel.getDayCount());
            if (!TextUtils.isEmpty(dayCountStr) && textViewDaysCount != null) {
                try {
                    textViewDaysCount.setText(dayCountStr);
                } catch (NumberFormatException e) {
                    textViewDaysCount.setText("0");
                }
            }
            
            // 更新未读评论数标志
            String commentNoReplyCountStr = String.valueOf(userInfoModel.getCommentNoReplyCount());
            if (!TextUtils.isEmpty(commentNoReplyCountStr) && textViewCommentUnreadCount != null) {
                try {
                    int commentNoReplyCount = Integer.parseInt(commentNoReplyCountStr);
                    if (commentNoReplyCount > 0) {
                        // 显示未读评论数
                        textViewCommentUnreadCount.setText(String.valueOf(commentNoReplyCount));
                        textViewCommentUnreadCount.setVisibility(View.VISIBLE);
                    } else {
                        // 隐藏未读评论数标志
                        textViewCommentUnreadCount.setVisibility(View.GONE);
                    }
                } catch (NumberFormatException e) {
                    // 解析失败时隐藏标志
                    textViewCommentUnreadCount.setVisibility(View.GONE);
                }
            } else {
                // 数据为空时隐藏标志
                if (textViewCommentUnreadCount != null) {
                    textViewCommentUnreadCount.setVisibility(View.GONE);
                }
            }
            
        } catch (Exception e) {
            Log.e("ProfileFragment", "解析用户数据失败", e);
            setDefaultUserInfo();
        }
    }
    
    /**
     * 安全地获取用户字段值
     * 
     * @param userObj 用户JSON对象
     * @param fieldName 字段名
     * @return 字段值，如果不存在或为null则返回null
     */
    private String getUserFieldSafely(JsonObject userObj, String fieldName) {
        if (userObj.has(fieldName) && !userObj.get(fieldName).isJsonNull()) {
            return userObj.get(fieldName).getAsString();
        }
        return null;
    }
    
    /**
     * 设置默认用户信息
     */
    private void setDefaultUserInfo() {
        Log.d("ProfileFragment", "设置默认用户信息");
        if (textViewUsername != null) {
            textViewUsername.setText("未登录用户");
        }
        if (textViewFootprintCount != null) {
            textViewFootprintCount.setText("0");
        }
        if (textViewCityCount != null) {
            textViewCityCount.setText("0");
        }
        if (textViewDaysCount != null) {
            textViewDaysCount.setText("0");
        }
        if (imageViewAvatar != null) {
            imageViewAvatar.setImageResource(R.drawable.default_avatar);
        }
        // 隐藏未读评论数标志
        if (textViewCommentUnreadCount != null) {
            textViewCommentUnreadCount.setVisibility(View.GONE);
        }
    }

    /**
     * 打开编辑资料页面
     */
    private void openEditProfileActivity() {
        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
        startActivityForResult(intent, 1001);
    }

    
    /**
     * 打开设置页面
     */
    private void openSettingsActivity() {
        Intent intent = new Intent(getContext(), SettingsActivity.class);
        startActivity(intent);
    }
}