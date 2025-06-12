package com.damors.zuji.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.OnScaleChangedListener;
import com.github.chrisbanes.photoview.OnPhotoTapListener;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import android.util.Log;
import com.damors.zuji.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片预览适配器
 * 用于在足迹详情页面中显示多张图片的预览
 */
public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {

    private List<String> imageUris;
    private Context context;
    private OnImageClickListener listener;

    /**
     * 构造函数
     * @param context 上下文
     * @param imageUris 图片URI列表
     */
    public ImagePreviewAdapter(Context context, List<String> imageUris) {
        this.context = context;
        this.imageUris = imageUris != null ? imageUris : new ArrayList<>();
    }

    /**
     * 更新图片列表
     * @param imageUris 新的图片URI列表
     */
    public void updateImages(List<String> imageUris) {
        this.imageUris = imageUris != null ? imageUris : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 添加单张图片
     * @param imageUri 图片URI
     */
    public void addImage(String imageUri) {
        if (imageUri != null && !imageUri.isEmpty()) {
            this.imageUris.add(imageUri);
            notifyItemInserted(this.imageUris.size() - 1);
        }
    }

    /**
     * 删除图片
     * @param position 要删除的图片位置
     */
    public void removeImage(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, imageUris.size() - position);
        }
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUris.get(position);
        
        Log.d("ImagePreviewAdapter", "Loading image at position " + position + ": " + imageUrl);
        
        // 配置PhotoView的缩放参数
        holder.imageView.setMaximumScale(5.0f); // 最大缩放倍数
        holder.imageView.setMediumScale(2.5f);  // 中等缩放倍数
        // 不设置最小缩放，让PhotoView自动计算合适的缩放比例以保持图片原始比例
        // PhotoView会自动处理缩放类型，确保图片不被拉伸变形
        
        // 使用Glide加载图片
        Glide.with(context)
            .load(imageUrl)
            .fitCenter() // 保持图片原始比例，不拉伸变形
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_placeholder_image)
            .error(R.drawable.ic_error_image)
            .transition(DrawableTransitionOptions.withCrossFade())
            .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                    Log.e("ImagePreviewAdapter", "Failed to load image at position " + position + ": " + imageUrl, e);
                    return false;
                }
                
                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    Log.d("ImagePreviewAdapter", "Image loaded successfully at position " + position + ": " + imageUrl);
                    return false;
                }
            })
            .into(holder.imageView);
        
        // 设置PhotoView的缩放监听器
        holder.imageView.setOnScaleChangeListener(new OnScaleChangedListener() {
            @Override
            public void onScaleChange(float scaleFactor, float focusX, float focusY) {
                Log.d("ImagePreviewAdapter", "图片缩放倍数: " + scaleFactor);
            }
        });
        
        // 设置PhotoView的点击事件（点击切换UI控件可见性）
        holder.imageView.setOnPhotoTapListener(new OnPhotoTapListener() {
            @Override
            public void onPhotoTap(ImageView view, float x, float y) {
                if (listener != null) {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        listener.onImageClick(adapterPosition, imageUris.get(adapterPosition));
                    }
                }
            }
        });
        
        // 设置长按事件（长按保存图片）
        holder.imageView.setOnLongClickListener(v -> {
            if (listener != null) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onImageLongClick(adapterPosition, imageUris.get(adapterPosition));
                }
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    /**
     * 设置图片点击监听器
     * @param listener 监听器接口
     */
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.listener = listener;
    }

    /**
     * 图片点击监听器接口
     */
    public interface OnImageClickListener {
        /**
         * 图片点击事件
         * @param position 图片位置
         * @param imageUri 图片URI
         */
        void onImageClick(int position, String imageUri);
        
        /**
         * 图片长按事件
         * @param position 图片位置
         * @param imageUri 图片URI
         */
        void onImageLongClick(int position, String imageUri);
    }

    /**
     * 图片视图持有者
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        PhotoView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view_item);
        }
    }
    
    /**
     * 单元测试方法
     * 测试适配器是否正确处理数据
     * @return 是否测试通过
     */
    public boolean testAdapter() {
        try {
            // 创建测试数据
            List<String> testUris = new ArrayList<>();
            testUris.add("content://test/image1.jpg");
            testUris.add("content://test/image2.jpg");
            
            // 测试更新方法
            updateImages(testUris);
            if (imageUris.size() != 2) return false;
            
            // 测试添加方法
            addImage("content://test/image3.jpg");
            if (imageUris.size() != 3) return false;
            
            // 测试删除方法
            removeImage(1);
            if (imageUris.size() != 2) return false;
            if (!imageUris.get(0).equals("content://test/image1.jpg")) return false;
            if (!imageUris.get(1).equals("content://test/image3.jpg")) return false;
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}