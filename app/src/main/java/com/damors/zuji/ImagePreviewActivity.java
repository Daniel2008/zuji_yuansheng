package com.damors.zuji;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import android.util.Log;

/**
 * 图片全屏预览Activity
 * 用于显示足迹中的图片大图
 */
public class ImagePreviewActivity extends AppCompatActivity {

    private static final String EXTRA_IMAGE_URI = "image_uri";
    private static final String EXTRA_POSITION = "position";
    
    private ImageView fullImageView;
    private ImageButton closeButton;
    
    /**
     * 创建启动此Activity的Intent
     * @param context 上下文
     * @param imageUri 图片URI
     * @param position 图片在列表中的位置
     * @return Intent对象
     */
    public static Intent newIntent(Context context, String imageUri, int position) {
        Intent intent = new Intent(context, ImagePreviewActivity.class);
        intent.putExtra(EXTRA_IMAGE_URI, imageUri);
        intent.putExtra(EXTRA_POSITION, position);
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
        fullImageView = findViewById(R.id.image_view_full);
        closeButton = findViewById(R.id.button_close);
        
        // 获取传入的图片URI
        String imageUri = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        int position = getIntent().getIntExtra(EXTRA_POSITION, 0);
        
        // 使用Glide加载网络图片
        if (imageUri != null && !imageUri.isEmpty()) {
            Log.d("ImagePreview", "Loading image: " + imageUri);
            
            Glide.with(this)
                .load(imageUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_error_image)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        Log.e("ImagePreview", "Failed to load image: " + imageUri, e);
                        return false; // 让Glide显示错误图片
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("ImagePreview", "Image loaded successfully: " + imageUri);
                        return false; // 让Glide正常显示图片
                    }
                })
                .into(fullImageView);
        } else {
            Log.w("ImagePreview", "Image URI is null or empty");
        }
        
        // 设置关闭按钮点击事件
        closeButton.setOnClickListener(v -> finish());
        
        // 设置图片点击事件（点击图片也可以关闭预览）
        fullImageView.setOnClickListener(v -> {
            // 切换顶部关闭按钮的可见性
            if (closeButton.getVisibility() == View.VISIBLE) {
                closeButton.setVisibility(View.GONE);
            } else {
                closeButton.setVisibility(View.VISIBLE);
            }
        });
    }
    
    /**
     * 单元测试方法
     * 测试图片预览功能
     * @return 是否测试通过
     */
    public boolean testImagePreview() {
        try {
            // 测试Intent创建
            Intent intent = newIntent(this, "content://test/image.jpg", 0);
            if (intent == null) return false;
            
            // 验证Intent中的数据
            String uri = intent.getStringExtra(EXTRA_IMAGE_URI);
            int position = intent.getIntExtra(EXTRA_POSITION, -1);
            
            return uri.equals("content://test/image.jpg") && position == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}