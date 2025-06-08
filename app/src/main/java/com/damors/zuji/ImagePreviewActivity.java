package com.damors.zuji;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import com.damors.zuji.adapter.ImagePreviewAdapter;

/**
 * 图片全屏预览Activity
 * 用于显示足迹中的图片大图
 */
public class ImagePreviewActivity extends AppCompatActivity {

    private static final String EXTRA_IMAGE_URLS = "image_urls";
    private static final String EXTRA_CURRENT_INDEX = "current_index";
    
    private ViewPager2 viewPager;
    private ImageButton closeButton;
    private TextView pageIndicator;
    private ImagePreviewAdapter adapter;
    private ArrayList<String> imageUrls;
    private int currentIndex;
    
    /**
     * 创建启动此Activity的Intent
     * @param context 上下文
     * @param imageUrls 图片URL列表
     * @param currentIndex 当前图片索引
     * @return Intent对象
     */
    public static Intent newIntent(Context context, ArrayList<String> imageUrls, int currentIndex) {
        Intent intent = new Intent(context, ImagePreviewActivity.class);
        intent.putStringArrayListExtra(EXTRA_IMAGE_URLS, imageUrls);
        intent.putExtra(EXTRA_CURRENT_INDEX, currentIndex);
        return intent;
    }
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_image_preview);
        
        // 初始化视图
        viewPager = findViewById(R.id.view_pager_images);
        closeButton = findViewById(R.id.button_close);
        pageIndicator = findViewById(R.id.page_indicator);
        
        // 获取传入的数据
        imageUrls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);
        currentIndex = getIntent().getIntExtra(EXTRA_CURRENT_INDEX, 0);
        
        if (imageUrls != null && !imageUrls.isEmpty()) {
            // 设置适配器
            adapter = new ImagePreviewAdapter(this, imageUrls);
            viewPager.setAdapter(adapter);
            
            // 设置当前页面
            viewPager.setCurrentItem(currentIndex, false);
            
            // 更新页面指示器
            updatePageIndicator(currentIndex);
            
            // 设置页面变化监听器
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    currentIndex = position;
                    updatePageIndicator(position);
                }
            });
            
            Log.d("ImagePreview", "Loading " + imageUrls.size() + " images, starting at index " + currentIndex);
        } else {
            Log.w("ImagePreview", "Image URLs list is null or empty");
            finish();
            return;
        }
        
        // 设置关闭按钮点击事件
        closeButton.setOnClickListener(v -> finish());
    }
    
    /**
     * 更新页面指示器
     * @param position 当前页面位置
     */
    private void updatePageIndicator(int position) {
        if (pageIndicator != null && imageUrls != null) {
            if (imageUrls.size() > 1) {
                pageIndicator.setVisibility(View.VISIBLE);
                pageIndicator.setText(String.format("%d / %d", position + 1, imageUrls.size()));
            } else {
                pageIndicator.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * 单元测试方法
     * 测试图片预览功能
     * @return 是否测试通过
     */
    public boolean testImagePreview() {
        try {
            // 测试Intent创建
            ArrayList<String> testUrls = new ArrayList<>();
            testUrls.add("content://test/image1.jpg");
            testUrls.add("content://test/image2.jpg");
            Intent intent = newIntent(this, testUrls, 0);
            if (intent == null) return false;
            
            // 验证Intent中的数据
            ArrayList<String> urls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS);
            int index = intent.getIntExtra(EXTRA_CURRENT_INDEX, -1);
            
            return urls != null && urls.size() == 2 && index == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}