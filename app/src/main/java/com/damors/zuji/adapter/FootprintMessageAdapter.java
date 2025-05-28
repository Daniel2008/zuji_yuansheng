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
import com.damors.zuji.R;
import com.damors.zuji.model.FootprintMessage;
import com.damors.zuji.model.GuluFile;

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
        
        // 设置位置信息
        setLocationInfo(holder, message);
        
        // 设置内容信息
        setContentInfo(holder, message);
        
        // 设置图片预览
        setImagePreview(holder, message);
        
        // 设置时间轴线条
        setTimelineLines(holder, position);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(message, position);
            }
        });
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
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault());
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
     * 设置图片预览
     * @param holder ViewHolder
     * @param message 足迹动态
     */
    private void setImagePreview(ViewHolder holder, FootprintMessage message) {
        if (message.getGuluFiles() != null && !message.getGuluFiles().isEmpty()) {
            // 查找第一个图片文件
            GuluFile firstImageFile = null;
            boolean hasVideo = false;
            
            for (GuluFile file : message.getGuluFiles()) {
                if (file.getFileType() != null) {
                    if (isImageFile(file.getFileType()) && firstImageFile == null) {
                        firstImageFile = file;
                    } else if (isVideoFile(file.getFileType())) {
                        hasVideo = true;
                    }
                }
            }
            
            // 显示第一张图片
            if (firstImageFile != null) {
                holder.previewImageView.setVisibility(View.VISIBLE);
                
                String imageUrl = getFullImageUrl(firstImageFile.getFilePath());
                // 添加日志输出，便于调试
                android.util.Log.d("FootprintMessageAdapter", "加载图片URL: " + imageUrl);
                
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_error_image)
                    .centerCrop()
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("FootprintMessageAdapter", "图片加载失败: " + imageUrl, e);
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("FootprintMessageAdapter", "图片加载成功: " + imageUrl);
                            return false;
                        }
                    })
                    .into(holder.previewImageView);
                    
                // 如果有多张图片，显示数量标识
                if (message.getGuluFiles().size() > 1) {
                    // 可以在这里添加多图标识的逻辑
                }
            } else {
                holder.previewImageView.setVisibility(View.GONE);
            }
        } else {
            holder.previewImageView.setVisibility(View.GONE);
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
        return type.equals("jpg") || type.equals("jpeg") || type.equals("png") || 
               type.equals("gif") || type.equals("bmp") || type.equals("webp");
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
     */
    private String getFullImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "";
        }
        
        // 如果已经是完整URL，直接返回
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }
        
        // 拼接基础URL
        String baseUrl = "http://192.168.1.5:8080";
        if (imagePath.startsWith("/")) {
            return baseUrl + imagePath;
        } else {
            return baseUrl + "/" + imagePath;
        }
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        // 时间轴相关控件
        TextView dateTextView;
        TextView timeTextView;
        View topLineView;
        View bottomLineView;
        View timelineDot;
        
        // 内容相关控件
        TextView locationTextView;
        TextView categoryTextView;
        TextView descriptionTextView;
        ImageView previewImageView;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 初始化时间轴控件
            dateTextView = itemView.findViewById(R.id.text_view_date);
            timeTextView = itemView.findViewById(R.id.text_view_time);
            topLineView = itemView.findViewById(R.id.view_timeline_top);
            bottomLineView = itemView.findViewById(R.id.view_timeline_bottom);
            timelineDot = itemView.findViewById(R.id.view_timeline_dot);
            
            // 初始化内容控件
            locationTextView = itemView.findViewById(R.id.text_view_location);
            categoryTextView = itemView.findViewById(R.id.text_view_category);
            descriptionTextView = itemView.findViewById(R.id.text_view_description);
            previewImageView = itemView.findViewById(R.id.image_view_preview);
        }
    }
}