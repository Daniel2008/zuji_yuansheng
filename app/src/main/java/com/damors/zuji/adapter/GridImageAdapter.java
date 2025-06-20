package com.damors.zuji.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.damors.zuji.R;
import com.damors.zuji.config.ImageDisplayConfig;
import com.damors.zuji.model.GuluFile;
import com.damors.zuji.network.ApiConfig;
import java.util.List;

/**
 * 九宫格图片适配器
 * 用于在RecyclerView中显示图片网格布局
 */
public class GridImageAdapter extends RecyclerView.Adapter<GridImageAdapter.ImageViewHolder> {

    private Context context;
    private List<GuluFile> imageFiles;
    private OnImageClickListener onImageClickListener;
    private int maxDisplayCount = ImageDisplayConfig.MAX_GRID_DISPLAY_COUNT; // 最大显示数量

    /**
     * 图片点击监听器接口
     */
    public interface OnImageClickListener {
        /**
         * 图片被点击时调用
         * @param position 点击的图片位置
         * @param imageFiles 图片文件列表
         */
        void onImageClick(int position, List<GuluFile> imageFiles);
    }

    /**
     * 构造函数
     * @param context 上下文
     * @param imageFiles 图片文件列表
     */
    public GridImageAdapter(Context context, List<GuluFile> imageFiles) {
        this.context = context;
        this.imageFiles = imageFiles;
    }
    
    /**
     * 计算RecyclerView应该的高度
     * @param context 上下文
     * @param imageCount 图片数量
     * @return RecyclerView的高度（像素）
     */
    public static int calculateRecyclerViewHeight(Context context, int imageCount) {
        int displayCount = Math.min(imageCount, ImageDisplayConfig.MAX_GRID_DISPLAY_COUNT);
        int rows = (int) Math.ceil((double) displayCount / ImageDisplayConfig.GRID_SPAN_COUNT);
        int itemHeight = ImageDisplayConfig.dpToPx(context, ImageDisplayConfig.GRID_ITEM_HEIGHT_DP);
        int spacing = ImageDisplayConfig.dpToPx(context, ImageDisplayConfig.GRID_SPACING_DP);
        return rows * itemHeight + (rows - 1) * spacing;
    }

    /**
     * 设置图片点击监听器
     * @param listener 监听器
     */
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_grid_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        GuluFile imageFile = imageFiles.get(position);
        
        // 加载图片
        String imageUrl = getFullImageUrl(imageFile.getFilePath());
        
        Glide.with(context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_placeholder_image)
            .error(R.drawable.ic_error_image)
            .centerCrop()
            .thumbnail(ImageDisplayConfig.THUMBNAIL_RATIO) // 添加缩略图支持，提升加载速度
            .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade()) // 添加渐变效果
            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                    public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        // 图片加载失败处理
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        // 图片加载成功处理
                        return false;
                    }
            })
            .into(holder.imageView);

        // 设置点击事件
        holder.imageView.setOnClickListener(v -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick(position, imageFiles);
            }
        });

        // 如果是最后一张图片且还有更多图片，显示遮罩
        if (position == maxDisplayCount - 1 && imageFiles.size() > maxDisplayCount) {
            holder.overlayView.setVisibility(View.VISIBLE);
            holder.moreCountText.setVisibility(View.VISIBLE);
            int remainingCount = imageFiles.size() - maxDisplayCount;
            holder.moreCountText.setText("+" + remainingCount);
        } else {
            holder.overlayView.setVisibility(View.GONE);
            holder.moreCountText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        // 最多显示9张图片
        return Math.min(imageFiles.size(), maxDisplayCount);
    }

    /**
     * 获取完整的图片URL
     * @param filePath 文件路径
     * @return 完整的图片URL
     */
    private String getFullImageUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        
        // 如果已经是完整URL，直接返回
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath;
        }
        
        // 使用ApiConfig中的图片基础URL构建完整的图片URL
        String imageBaseUrl = ApiConfig.getImageBaseUrl();
        // 确保路径正确拼接
        if (!filePath.startsWith("/")) {
            filePath = "/" + filePath;
        }
        String fullImageUrl = imageBaseUrl + filePath;
        
        return fullImageUrl;
    }

    @Override
    public void onViewRecycled(@NonNull ImageViewHolder holder) {
        super.onViewRecycled(holder);
        // 清理Glide加载，避免内存泄漏
        Glide.with(context).clear(holder.imageView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // 清理资源
        if (imageFiles != null) {
            imageFiles.clear();
        }
    }

    /**
     * ViewHolder类
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        View overlayView;
        TextView moreCountText;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.grid_image_view);
            overlayView = itemView.findViewById(R.id.overlay_view);
            moreCountText = itemView.findViewById(R.id.more_count_text);
        }
    }
}