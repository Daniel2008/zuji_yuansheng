package com.damors.zuji;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.amap.api.maps.offlinemap.OfflineMapActivity;
import com.bumptech.glide.Glide;
import com.damors.zuji.manager.UserManager;
import com.damors.zuji.network.ApiConfig;
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
    private CircleImageView imageViewAvatar;
    private LinearLayout layoutLikeManagement;
    private LinearLayout layoutCommentManagement;
    private LinearLayout layoutMapCache;
    private LinearLayout layoutSettings;

    private ExecutorService executorService;
    private Handler mainHandler;
    private UserManager userManager;

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
            // TODO: 实现点赞管理功能
            Toast.makeText(getContext(), "点赞管理功能开发中", Toast.LENGTH_SHORT).show();
        });
        
        // 设置评论管理布局点击事件
        layoutCommentManagement.setOnClickListener(v -> {
            // TODO: 实现评论管理功能
            Toast.makeText(getContext(), "评论管理功能开发中", Toast.LENGTH_SHORT).show();
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
            // TODO: 实现点赞管理功能
            Toast.makeText(getContext(), "点赞管理功能开发中", Toast.LENGTH_SHORT).show();
        });
        
        // 设置评论管理布局点击事件
        layoutCommentManagement.setOnClickListener(v -> {
            // TODO: 实现评论管理功能
            Toast.makeText(getContext(), "评论管理功能开发中", Toast.LENGTH_SHORT).show();
        });
        
        // 设置地图缓存管理布局点击事件 - 直接打开离线地图管理
        layoutMapCache.setOnClickListener(v -> openOfflineMapManager());
        
        // 观察足迹数据变化，更新统计信息
        // 注释：已移除本地足迹统计功能
        /*
        viewModel.getAllFootprints().observe(getViewLifecycleOwner(), footprints -> {
            if (footprints != null) {
                textViewFootprintCount.setText(String.format("%d", footprints.size()));
            } else {
                textViewFootprintCount.setText("0");
            }
        });
        */
        
        // 刷新数据
        viewModel.refreshFootprints();
        
        // 延迟加载用户数据，确保UserManager完全初始化
        mainHandler.postDelayed(() -> {
            if (isAdded() && getContext() != null) {
                loadUserData();
            }
        }, 200);
        
        // 设置定时刷新机制，每30秒从缓存刷新一次数据
        startPeriodicRefresh();
        
        // 加载缓存大小信息
        loadCacheSize();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 页面恢复时刷新统计数据，从缓存中读取最新数据
        refreshUserDataFromCache();
        
        // 强制刷新用户数据，解决首次登录后显示未登录状态的问题
        // 延迟执行以确保UserManager数据完全同步
        mainHandler.postDelayed(() -> {
            if (isAdded() && getContext() != null && userManager != null) {
                userManager.reloadUserData();
                loadUserData();
                Log.d("ProfileFragment", "onResume中刷新用户数据完成");
            }
        }, 100);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // 页面启动时也刷新用户数据，确保登录状态同步
        // 延迟执行以确保UserManager数据完全同步
        mainHandler.postDelayed(() -> {
            if (isAdded() && getContext() != null && userManager != null) {
                userManager.reloadUserData();
                loadUserData();
                Log.d("ProfileFragment", "onStart中刷新用户数据完成");
            }
        }, 50);
    }
    
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // 当Fragment对用户可见时，强制刷新用户数据
        if (isVisibleToUser && isResumed() && userManager != null) {
            Log.d("ProfileFragment", "Fragment变为可见，刷新用户数据");
            userManager.reloadUserData();
            loadUserData();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * 从缓存刷新用户数据
     */
    private void refreshUserDataFromCache() {
        if (userManager != null && userManager.isLoggedIn()) {
            // 直接从缓存读取用户数据，无需网络请求
            loadUserData();
            Log.d("ProfileFragment", "从缓存刷新用户数据");
        }
    }
    
    /**
     * 公共方法：刷新用户数据
     * 供外部调用，用于在登录状态变化时刷新界面
     */
    public void refreshUserData() {
        if (userManager != null && isAdded() && getContext() != null) {
            // 强制重新加载用户数据，确保数据同步
            userManager.reloadUserData();
            
            // 在主线程中延迟执行UI更新，确保数据完全同步
            mainHandler.post(() -> {
                if (isAdded() && getContext() != null) {
                    loadUserData();
                    Log.d("ProfileFragment", "外部调用刷新用户数据完成");
                }
            });
        }
    }
    
    /**
     * 启动定时刷新机制
     */
    private void startPeriodicRefresh() {
        if (mainHandler != null) {
            // 每30秒从缓存刷新一次数据
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isAdded() && getContext() != null) {
                        refreshUserDataFromCache();
                        // 继续下一次定时刷新
                        mainHandler.postDelayed(this, 30000);
                    }
                }
            }, 30000);
        }
    }
    
    /**
     * 加载地图缓存大小信息
     */
    private void loadCacheSize() {
        executorService.execute(() -> {
            String cacheSize = MapCacheManager.getFormattedCacheSize();
            int fileCount = MapCacheManager.getCacheFileCount();
            
            mainHandler.post(() -> {
                if (textViewCacheSize != null) {
                    textViewCacheSize.setText(String.format("缓存大小: %s (%d 个文件)", cacheSize, fileCount));
                }
            });
        });
    }
    
    /**
     * 显示地图缓存管理对话框
     */
    private void showMapCacheDialog() {
        executorService.execute(() -> {
            String cacheSize = MapCacheManager.getFormattedCacheSize();
            int fileCount = MapCacheManager.getCacheFileCount();
            String cachePath = MapCacheManager.getCachePath();
            boolean isAvailable = MapCacheManager.isCacheAvailable();
            
            mainHandler.post(() -> {
                String message = String.format(
                    "缓存大小: %s\n" +
                    "文件数量: %d 个\n" +
                    "缓存路径: %s\n" +
                    "缓存状态: %s",
                    cacheSize, fileCount, cachePath, isAvailable ? "可用" : "不可用"
                );
                
                new AlertDialog.Builder(requireContext())
                    .setTitle("地图缓存管理")
                    .setMessage(message)
                    .setPositiveButton("离线地图管理", (dialog, which) -> openOfflineMapManager())
                    .setNeutralButton("清理缓存", (dialog, which) -> clearMapCache())
                    .setNegativeButton("取消", null)
                    .show();
            });
        });
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
     * 清理地图缓存
     */
    private void clearMapCache() {
        new AlertDialog.Builder(requireContext())
            .setTitle("清理地图缓存")
            .setMessage("确定要清理所有地图缓存吗？这将删除所有已下载的地图瓦片，下次使用时需要重新下载。")
            .setPositiveButton("确定", (dialog, which) -> {
                executorService.execute(() -> {
                    boolean success = MapCacheManager.clearCache();
                    
                    mainHandler.post(() -> {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(), 
                                success ? "地图缓存清理成功" : "地图缓存清理失败", 
                                Toast.LENGTH_SHORT).show();
                        }
                        // 重新加载缓存大小
                        loadCacheSize();
                    });
                });
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 清理过期的地图缓存
     */
    private void clearExpiredCache() {
        executorService.execute(() -> {
            // 清理7天前的缓存文件
            long maxAge = 7L * 24 * 60 * 60 * 1000; // 7天
            int deletedCount = MapCacheManager.clearExpiredCache(maxAge);
            
            mainHandler.post(() -> {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), 
                        String.format("已清理 %d 个过期缓存文件", deletedCount), 
                        Toast.LENGTH_SHORT).show();
                }
                // 重新加载缓存大小
                loadCacheSize();
            });
        });
    }
    
    /**
     * 加载用户数据并更新UI
     */
    private void loadUserData() {
        Log.d("ProfileFragment", "开始加载用户数据");
        
        if (userManager == null) {
            Log.w("ProfileFragment", "UserManager为null，显示默认信息");
            setDefaultUserInfo();
            return;
        }
        
        boolean isLoggedIn = userManager.isLoggedIn();
        String userJson = userManager.getCurrentUserJson();
        String token = userManager.getToken();
        
        Log.d("ProfileFragment", "登录状态检查: isLoggedIn=" + isLoggedIn);
        Log.d("ProfileFragment", "用户JSON是否为空: " + TextUtils.isEmpty(userJson));
        Log.d("ProfileFragment", "Token是否为空: " + TextUtils.isEmpty(token));
        
        if (!TextUtils.isEmpty(userJson)) {
            Log.d("ProfileFragment", "用户JSON内容: " + userJson);
        }
        
        if (!isLoggedIn) {
            Log.w("ProfileFragment", "用户未登录，显示默认信息");
            setDefaultUserInfo();
            return;
        }
        
        if (TextUtils.isEmpty(userJson)) {
            Log.w("ProfileFragment", "用户JSON为空，显示默认信息");
            setDefaultUserInfo();
            return;
        }
        
        try {
            JsonObject userObj = JsonParser.parseString(userJson).getAsJsonObject();
            
            // 更新用户头像
            String avatar = getUserFieldSafely(userObj, "avatar");
            if (!TextUtils.isEmpty(avatar) && imageViewAvatar != null) {
                // 拼接完整的头像URL
                String avatarUrl = ApiConfig.getImageBaseUrl() + avatar;
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .circleCrop()
                    .into(imageViewAvatar);
            }
            
            // 更新用户昵称
            String nickname = getUserFieldSafely(userObj, "nickName");
            if (!TextUtils.isEmpty(nickname) && textViewUsername != null) {
                textViewUsername.setText(nickname);
            } else {
                String username = getUserFieldSafely(userObj, "userName");
                if (!TextUtils.isEmpty(username) && textViewUsername != null) {
                    textViewUsername.setText(username);
                }
            }
            
            // 更新足迹数
            String footPrintCountStr = getUserFieldSafely(userObj, "footPrintCount");
            if (!TextUtils.isEmpty(footPrintCountStr) && textViewFootprintCount != null) {
                try {
                    textViewFootprintCount.setText(footPrintCountStr);
                } catch (NumberFormatException e) {
                    textViewFootprintCount.setText("0");
                }
            }
            
            // 更新城市数
            String cityCountStr = getUserFieldSafely(userObj, "cityCount");
            if (!TextUtils.isEmpty(cityCountStr) && textViewCityCount != null) {
                try {
                    textViewCityCount.setText(cityCountStr);
                } catch (NumberFormatException e) {
                    textViewCityCount.setText("0");
                }
            }
            
            // 更新天数
            String dayCountStr = getUserFieldSafely(userObj, "dayCount");
            if (!TextUtils.isEmpty(dayCountStr) && textViewDaysCount != null) {
                try {
                    textViewDaysCount.setText(dayCountStr);
                } catch (NumberFormatException e) {
                    textViewDaysCount.setText("0");
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
    }

    /**
     * 打开编辑资料页面
     */
    private void openEditProfileActivity() {
        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
        startActivityForResult(intent, 1001);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK) {
            // 编辑资料成功，从缓存重新加载用户数据
            refreshUserDataFromCache();
            Toast.makeText(getContext(), "资料更新成功", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开设置页面
     */
    private void openSettingsActivity() {
        Intent intent = new Intent(getContext(), SettingsActivity.class);
        startActivity(intent);
    }
}