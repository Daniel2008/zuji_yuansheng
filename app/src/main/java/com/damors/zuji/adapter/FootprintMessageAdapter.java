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
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.damors.zuji.R;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.GuluFile;
import com.damors.zuji.config.ImageDisplayConfig;
import com.damors.zuji.utils.GridSpacingItemDecoration;
import android.widget.LinearLayout;
import android.widget.FrameLayout;

import java.util.List;

/**
 * 足迹动态列表适配器
 */
public class FootprintMessageAdapter extends RecyclerView.Adapter<FootprintMessageAdapter.ViewHolder> {
    
    private Context context;
    private List<FootprintMessage> messageList;
    private OnItemClickListener onItemClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(FootprintMessage message, int position);
        void onUserAvatarClick(FootprintMessage message, int position);
        void onLocationClick(FootprintMessage message, int position);
        void onLikeClick(FootprintMessage message, int position);
        void onDeleteClick(FootprintMessage message, int position);
        void onCommentClick(FootprintMessage message, int position);
        void onImageClick(FootprintMessage message, int position, int imageIndex, List<GuluFile> imageFiles);
    }
    
    public FootprintMessageAdapter(Context context, List<FootprintMessage> messageList) {
        this.context = context;
        this.messageList = messageList;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用时间轴布局
        View view = LayoutInflater.from(context).inflate(R.layout.item_timeline_footprint, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FootprintMessage message = messageList.get(position);
        
        // 格式化时间显示
        formatDateTime(holder, message.getCreateTime());
        
        // 设置用户头像
        setUserAvatar(holder, message);
        
        // 设置位置信息
        setLocationInfo(holder, message);
        
        // 设置内容信息
        setContentInfo(holder, message);
        
        // 设置图片预览
        setImagePreview(holder, message);
        
        // 设置时间轴线条
        setTimelineLines(holder, position);
        
        // 设置操作栏数据和点击事件
        setActionBar(holder, message, position);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(message, position);
            }
        });
    }
    
    /**
     * 设置用户头像
     * @param holder ViewHolder
     * @param message 足迹消息
     */
    private void setUserAvatar(ViewHolder holder, FootprintMessage message) {
        if (holder.imageViewAvatar != null) {
            String userAvatar = message.getUserAvatar();
            if (userAvatar != null && !userAvatar.isEmpty()) {
                // 构建完整的头像URL
                String avatarUrl = getFullImageUrl(userAvatar);
                
                // 使用Glide加载头像
                Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(holder.imageViewAvatar);
                    
                // 设置头像点击事件
                holder.imageViewAvatar.setOnClickListener(v -> {
                    if (onItemClickListener != null) {
                        onItemClickListener.onUserAvatarClick(message, holder.getAdapterPosition());
                    }
                });
            } else {
                // 没有头像时显示默认头像
                holder.imageViewAvatar.setImageResource(R.drawable.ic_default_avatar);
                holder.imageViewAvatar.setOnClickListener(null);
            }
        }
    }
    
    /**
     * 格式化日期时间显示
     * @param holder ViewHolder
     * @param createTime 创建时间
     */
    private void formatDateTime(ViewHolder holder, String createTime) {
        if (createTime != null && !createTime.isEmpty()) {
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                java.util.Date date = inputFormat.parse(createTime);
                
                if (date != null) {
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                    
                    holder.dateTextView.setText(dateFormat.format(date));
                    holder.timeTextView.setText(timeFormat.format(date));
                } else {
                    holder.dateTextView.setText("未知日期");
                    holder.timeTextView.setText("未知时间");
                }
            } catch (java.text.ParseException e) {
                e.printStackTrace();
                holder.dateTextView.setText("日期解析错误");
                holder.timeTextView.setText("时间解析错误");
            }
        } else {
            holder.dateTextView.setText("无日期");
            holder.timeTextView.setText("无时间");
        }
    }
    
    /**
     * 设置位置信息
     * @param holder ViewHolder
     * @param message 足迹动态
     */
    private void setLocationInfo(ViewHolder holder, FootprintMessage message) {
        String locationText = "";
        
        if (message.getLocaltionTitle() != null && !message.getLocaltionTitle().isEmpty()) {
            locationText = message.getLocaltionTitle();
        } else {
            // 如果没有位置标题，显示经纬度
            locationText = String.format(java.util.Locale.getDefault(), "%.6f, %.6f", 
                message.getLat(), message.getLng());
        }
        
        holder.locationTextView.setText(locationText);
    }
    
    /**
     * 设置内容信息
     * @param holder ViewHolder
     * @param message 足迹动态
     */
    private void setContentInfo(ViewHolder holder, FootprintMessage message) {
        // 设置标签
        if (message.getTag() != null && !message.getTag().isEmpty()) {
            holder.categoryTextView.setText(message.getTag());
            holder.categoryTextView.setVisibility(View.VISIBLE);
        } else {
            holder.categoryTextView.setVisibility(View.GONE);
        }
        
        // 设置文本内容
        if (message.getTextContent() != null && !message.getTextContent().isEmpty()) {
            holder.descriptionTextView.setText(message.getTextContent());
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }
    }
    
    /**
     * 设置图片预览（九宫格布局）
     * @param holder ViewHolder
     * @param message 足迹动态
     */
    private void setImagePreview(ViewHolder holder, FootprintMessage message) {
        android.util.Log.d("FootprintMessageAdapter", "开始设置九宫格图片预览，GuluFiles: " + (message.getGuluFiles() != null ? message.getGuluFiles().size() : "null"));
        
        // 首先隐藏所有布局
        hideAllImageLayouts(holder);
        
        if (message.getGuluFiles() != null && !message.getGuluFiles().isEmpty()) {
            // 分类文件：只处理图片文件
            java.util.List<GuluFile> imageFiles = new java.util.ArrayList<>();
            
            for (GuluFile file : message.getGuluFiles()) {
                if (isImageFile(file.getFileType())) {
                    imageFiles.add(file);
                }
            }
            
            android.util.Log.d("FootprintMessageAdapter", "图片文件数量: " + imageFiles.size());
            
            if (!imageFiles.isEmpty()) {
                holder.gridImageLayout.setVisibility(View.VISIBLE);
                
                switch (imageFiles.size()) {
                    case 1:
                        showSingleImage(holder, imageFiles.get(0));
                        break;
                    case 2:
                        showTwoImages(holder, imageFiles);
                        break;
                    case 3:
                        showThreeImages(holder, imageFiles);
                        break;
                    default:
                        showGridImages(holder, imageFiles);
                        break;
                }
            } else {
                holder.gridImageLayout.setVisibility(View.GONE);
            }
        } else {
            holder.gridImageLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 隐藏所有图片布局
     * @param holder ViewHolder
     */
    private void hideAllImageLayouts(ViewHolder holder) {
        holder.singleImage.setVisibility(View.GONE);
        holder.twoImagesLayout.setVisibility(View.GONE);
        holder.threeImagesLayout.setVisibility(View.GONE);
        holder.gridRecyclerView.setVisibility(View.GONE);
        holder.moreImagesOverlay.setVisibility(View.GONE);
    }
    
    /**
     * 显示单张图片
     * @param holder ViewHolder
     * @param imageFile 图片文件
     */
    private void showSingleImage(ViewHolder holder, GuluFile imageFile) {
        holder.singleImage.setVisibility(View.VISIBLE);
        loadImageIntoView(holder.singleImage, imageFile);
        
        // 添加图片点击事件
        holder.singleImage.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                FootprintMessage message = messageList.get(holder.getAdapterPosition());
                List<GuluFile> imageFiles = getImageFiles(message);
                onItemClickListener.onImageClick(message, holder.getAdapterPosition(), 0, imageFiles);
            }
        });
        
        android.util.Log.d("FootprintMessageAdapter", "显示单张图片");
    }
    
    /**
     * 显示两张图片
     * @param holder ViewHolder
     * @param imageFiles 图片文件列表
     */
    private void showTwoImages(ViewHolder holder, java.util.List<GuluFile> imageFiles) {
        holder.twoImagesLayout.setVisibility(View.VISIBLE);
        loadImageIntoView(holder.image1Of2, imageFiles.get(0));
        loadImageIntoView(holder.image2Of2, imageFiles.get(1));
        
        // 添加图片点击事件
        holder.image1Of2.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                FootprintMessage message = messageList.get(holder.getAdapterPosition());
                onItemClickListener.onImageClick(message, holder.getAdapterPosition(), 0, imageFiles);
            }
        });
        
        holder.image2Of2.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                FootprintMessage message = messageList.get(holder.getAdapterPosition());
                onItemClickListener.onImageClick(message, holder.getAdapterPosition(), 1, imageFiles);
            }
        });
        
        android.util.Log.d("FootprintMessageAdapter", "显示两张图片");
    }
    
    /**
     * 显示三张图片
     * @param holder ViewHolder
     * @param imageFiles 图片文件列表
     */
    private void showThreeImages(ViewHolder holder, java.util.List<GuluFile> imageFiles) {
        holder.threeImagesLayout.setVisibility(View.VISIBLE);
        loadImageIntoView(holder.image1Of3, imageFiles.get(0));
        loadImageIntoView(holder.image2Of3, imageFiles.get(1));
        loadImageIntoView(holder.image3Of3, imageFiles.get(2));
        
        // 添加图片点击事件
        holder.image1Of3.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                FootprintMessage message = messageList.get(holder.getAdapterPosition());
                onItemClickListener.onImageClick(message, holder.getAdapterPosition(), 0, imageFiles);
            }
        });
        
        holder.image2Of3.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                FootprintMessage message = messageList.get(holder.getAdapterPosition());
                onItemClickListener.onImageClick(message, holder.getAdapterPosition(), 1, imageFiles);
            }
        });
        
        holder.image3Of3.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                FootprintMessage message = messageList.get(holder.getAdapterPosition());
                onItemClickListener.onImageClick(message, holder.getAdapterPosition(), 2, imageFiles);
            }
        });
        
        android.util.Log.d("FootprintMessageAdapter", "显示三张图片");
    }
    
    /**
     * 显示九宫格图片（4张及以上）
     * @param holder ViewHolder
     * @param imageFiles 图片文件列表
     */
    private void showGridImages(ViewHolder holder, java.util.List<GuluFile> imageFiles) {
        holder.gridRecyclerView.setVisibility(View.VISIBLE);
        
        // 设置网格布局管理器
        androidx.recyclerview.widget.GridLayoutManager gridLayoutManager = 
            new androidx.recyclerview.widget.GridLayoutManager(context, ImageDisplayConfig.GRID_SPAN_COUNT);
        holder.gridRecyclerView.setLayoutManager(gridLayoutManager);
        
        // 添加网格间距装饰器
        if (holder.gridRecyclerView.getItemDecorationCount() == 0) {
            holder.gridRecyclerView.addItemDecoration(new GridSpacingItemDecoration(ImageDisplayConfig.GRID_SPAN_COUNT, ImageDisplayConfig.GRID_SPACING_DP, true));
        }
        
        // 计算并设置RecyclerView的高度
        int calculatedHeight = com.damors.zuji.adapter.GridImageAdapter.calculateRecyclerViewHeight(context, imageFiles.size());
        ViewGroup.LayoutParams layoutParams = holder.gridRecyclerView.getLayoutParams();
        layoutParams.height = calculatedHeight;
        holder.gridRecyclerView.setLayoutParams(layoutParams);
        
        // 设置适配器
        GridImageAdapter adapter = new GridImageAdapter(context, imageFiles);
        
        // 设置图片点击事件
        adapter.setOnImageClickListener((position, files) -> {
            if (onItemClickListener != null) {
                FootprintMessage message = messageList.get(holder.getAdapterPosition());
                onItemClickListener.onImageClick(message, holder.getAdapterPosition(), position, imageFiles);
            }
        });
        
        holder.gridRecyclerView.setAdapter(adapter);
        
        // 如果图片数量超过9张，显示更多图片的遮罩
        if (imageFiles.size() > 9) {
            holder.moreImagesOverlay.setVisibility(View.VISIBLE);
            holder.moreImagesText.setText("+" + (imageFiles.size() - 8));
        }
        
        // android.util.Log.d("FootprintMessageAdapter", "显示九宫格图片，总数: " + imageFiles.size() + "，计算高度: " + calculatedHeight);
    }
    
    /**
     * 获取图片文件列表
     * @param message 足迹动态
     * @return 图片文件列表
     */
    private List<GuluFile> getImageFiles(FootprintMessage message) {
        List<GuluFile> imageFiles = new java.util.ArrayList<>();
        if (message.getGuluFiles() != null) {
            for (GuluFile file : message.getGuluFiles()) {
                if (isImageFile(file.getFileType())) {
                    imageFiles.add(file);
                }
            }
        }
        return imageFiles;
    }
    
    /**
     * 加载图片到ImageView
     * @param imageView 目标ImageView
     * @param imageFile 图片文件
     */
    private void loadImageIntoView(ImageView imageView, GuluFile imageFile) {
        String imageUrl = getFullImageUrl(imageFile.getFilePath());
        android.util.Log.d("FootprintMessageAdapter", "加载图片: " + imageUrl);
        
        Glide.with(context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_placeholder_image)
            .error(R.drawable.ic_placeholder_image)
            .centerCrop()
            .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                    android.util.Log.e("FootprintMessageAdapter", "图片加载失败: " + model, e);
                    return false;
                }

                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                    android.util.Log.d("FootprintMessageAdapter", "图片加载成功: " + model);
                    return false;
                }
            })
            .into(imageView);
    }
    
    /**
     * 设置操作栏数据和点击事件
     * @param holder ViewHolder
     * @param message 足迹动态
     * @param position 位置
     */
    private void setActionBar(ViewHolder holder, FootprintMessage message, int position) {
        // 设置点赞状态和数量
        updateLikeStatus(holder, message.getHasLiked(), message.getLikeCount());
        
        // 收藏功能已移除
        
        // 设置评论数量
        holder.tvCommentCount.setText(String.valueOf(message.getCommentCount()));
        
        // 设置点击事件
        holder.layoutLike.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onLikeClick(message, position);
            }
        });
        
        holder.layoutDelete.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onDeleteClick(message, position);
            }
        });
        
        holder.layoutComment.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onCommentClick(message, position);
            }
        });
    }
    
    /**
     * 更新点赞状态
     * @param holder ViewHolder
     * @param isLiked 是否已点赞
     * @param likeCount 点赞数量
     */
    private void updateLikeStatus(ViewHolder holder, boolean isLiked, int likeCount) {
        if (isLiked) {
            holder.ivLike.setImageResource(R.drawable.ic_like_filled);
            holder.ivLike.setColorFilter(context.getResources().getColor(R.color.action_icon_active_color));
            holder.tvLikeCount.setTextColor(context.getResources().getColor(R.color.action_text_active_color));
        } else {
            holder.ivLike.setImageResource(R.drawable.ic_like_outline);
            holder.ivLike.setColorFilter(context.getResources().getColor(R.color.action_icon_color));
            holder.tvLikeCount.setTextColor(context.getResources().getColor(R.color.action_text_color));
        }
        holder.tvLikeCount.setText(String.valueOf(likeCount));
    }
    
    // 收藏功能已移除
    
    /**
     * 公开方法：更新指定位置的点赞状态
     * @param position 位置
     * @param isLiked 是否已点赞
     * @param likeCount 点赞数量
     */
    public void updateItemLikeStatus(int position, boolean isLiked, int likeCount) {
        if (position >= 0 && position < messageList.size()) {
            FootprintMessage message = messageList.get(position);
            message.setHasLiked(isLiked);
            message.setLikeCount(likeCount);
            notifyItemChanged(position);
        }
    }
    
    // 收藏功能已移除
    
    /**
     * 公开方法：更新指定位置的评论数量
     * @param position 位置
     * @param commentCount 评论数量
     */
    public void updateItemCommentCount(int position, int commentCount) {
        if (position >= 0 && position < messageList.size()) {
            FootprintMessage message = messageList.get(position);
            message.setCommentCount(commentCount);
            notifyItemChanged(position);
        }
    }
    
    /**
     * 设置时间轴线条
     * @param holder ViewHolder
     * @param position 位置
     */
    private void setTimelineLines(ViewHolder holder, int position) {
        // 第一个项目隐藏上方线条
        if (position == 0) {
            holder.topLineView.setVisibility(View.INVISIBLE);
        } else {
            holder.topLineView.setVisibility(View.VISIBLE);
        }
        
        // 最后一个项目隐藏下方线条
        if (position == getItemCount() - 1) {
            holder.bottomLineView.setVisibility(View.INVISIBLE);
        } else {
            holder.bottomLineView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 判断是否为图片文件
     * @param fileType 文件类型
     * @return 是否为图片
     */
    private boolean isImageFile(String fileType) {
        if (fileType == null) return false;
        String type = fileType.toLowerCase();
        return type.equals("jpg")
            || type.equals("jpeg")
            || type.equals("png")
            || type.equals("gif")
            || type.equals("bmp")
            || type.equals("webp")
                || type.equals("image/*")
            || type.equals("image/jpeg");
    }
    
    /**
     * 判断是否为视频文件
     * @param fileType 文件类型
     * @return 是否为视频
     */
    private boolean isVideoFile(String fileType) {
        if (fileType == null) return false;
        String type = fileType.toLowerCase();
        return type.equals("mp4") || type.equals("avi") || type.equals("mov") || 
               type.equals("wmv") || type.equals("flv") || type.equals("mkv");
    }
    
    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }
    
    /**
     * 获取完整的图片URL
     * 使用ApiConfig中的图片基础URL
     */
    private String getFullImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            android.util.Log.w("FootprintMessageAdapter", "图片路径为空");
            return "";
        }
        
        // 如果已经是完整URL，直接返回
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            android.util.Log.d("FootprintMessageAdapter", "使用完整URL: " + imagePath);
            return imagePath;
        }
        
        // 使用ApiConfig中的图片基础URL构建完整的图片URL
        String imageBaseUrl = ApiConfig.getImageBaseUrl();
        // 确保路径正确拼接
        if (!imagePath.startsWith("/")) {
            imagePath = "/" + imagePath;
        }
        String fullImageUrl = imageBaseUrl + imagePath;
        
        android.util.Log.d("FootprintMessageAdapter", "构建图片URL: " + imagePath + " -> " + fullImageUrl);
        android.util.Log.d("FootprintMessageAdapter", "图片基础URL: " + imageBaseUrl);
        return fullImageUrl;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        // 时间轴相关控件
        TextView dateTextView;
        TextView timeTextView;
        View topLineView;
        View bottomLineView;
        View timelineDot;
        
        // 用户头像控件
        ImageView imageViewAvatar;
        
        // 内容相关控件
        TextView locationTextView;
        TextView categoryTextView;
        TextView descriptionTextView;
        
        // 九宫格图片布局相关控件
        View gridImageLayout;
        ImageView singleImage;
        LinearLayout twoImagesLayout;
        ImageView image1Of2, image2Of2;
        LinearLayout threeImagesLayout;
        ImageView image1Of3, image2Of3, image3Of3;
        androidx.recyclerview.widget.RecyclerView gridRecyclerView;
        FrameLayout moreImagesOverlay;
        TextView moreImagesText;
        
        // 操作栏相关控件
        LinearLayout layoutActions;
        LinearLayout layoutLike;
        ImageView ivLike;
        TextView tvLikeCount;
        LinearLayout layoutDelete;
        ImageView ivDelete;
        TextView tvDelete;
        LinearLayout layoutComment;
        ImageView ivComment;
        TextView tvCommentCount;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 初始化时间轴控件
            dateTextView = itemView.findViewById(R.id.text_view_date);
            timeTextView = itemView.findViewById(R.id.text_view_time);
            topLineView = itemView.findViewById(R.id.view_timeline_top);
            bottomLineView = itemView.findViewById(R.id.view_timeline_bottom);
            timelineDot = itemView.findViewById(R.id.view_timeline_dot);
            
            // 初始化用户头像控件
            imageViewAvatar = itemView.findViewById(R.id.image_view_avatar);
            
            // 初始化内容控件
            locationTextView = itemView.findViewById(R.id.text_view_location);
            categoryTextView = itemView.findViewById(R.id.text_view_category);
            descriptionTextView = itemView.findViewById(R.id.text_view_description);
            
            // 初始化九宫格图片布局控件
            gridImageLayout = itemView.findViewById(R.id.grid_image_layout);
            singleImage = itemView.findViewById(R.id.single_image);
            twoImagesLayout = itemView.findViewById(R.id.two_images_layout);
            image1Of2 = itemView.findViewById(R.id.image_1_of_2);
            image2Of2 = itemView.findViewById(R.id.image_2_of_2);
            threeImagesLayout = itemView.findViewById(R.id.three_images_layout);
            image1Of3 = itemView.findViewById(R.id.image_1_of_3);
            image2Of3 = itemView.findViewById(R.id.image_2_of_3);
            image3Of3 = itemView.findViewById(R.id.image_3_of_3);
            gridRecyclerView = itemView.findViewById(R.id.grid_recycler_view);
            moreImagesOverlay = itemView.findViewById(R.id.more_images_overlay);
            moreImagesText = itemView.findViewById(R.id.more_images_text);
            
            // 初始化操作栏控件
            layoutActions = itemView.findViewById(R.id.layout_actions);
            layoutLike = itemView.findViewById(R.id.layout_like);
            ivLike = itemView.findViewById(R.id.iv_like);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            layoutDelete = itemView.findViewById(R.id.layout_delete);
            ivDelete = itemView.findViewById(R.id.iv_delete);
            tvDelete = itemView.findViewById(R.id.tv_delete);
            layoutComment = itemView.findViewById(R.id.layout_comment);
            ivComment = itemView.findViewById(R.id.iv_comment);
            tvCommentCount = itemView.findViewById(R.id.tv_comment_count);
        }
    }
}