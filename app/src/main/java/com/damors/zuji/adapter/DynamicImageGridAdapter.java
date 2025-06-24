package com.damors.zuji.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.damors.zuji.R;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.model.GuluFile;

import java.util.List;

/**
 * 动态图片网格适配器
 */
public class DynamicImageGridAdapter extends RecyclerView.Adapter<DynamicImageGridAdapter.ImageViewHolder> {
    
    private Context context;
    private List<GuluFile> imageFiles;
    private OnImageClickListener onImageClickListener;
    
    public interface OnImageClickListener {
        void onImageClick(int position, GuluFile imageFile);
    }
    
    public DynamicImageGridAdapter(Context context, List<GuluFile> imageFiles) {
        this.context = context;
        this.imageFiles = imageFiles;
    }
    
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dynamic_image, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        GuluFile imageFile = imageFiles.get(position);
        String baseUrl = ApiConfig.getBaseUrl();
        String filePath = imageFile.getFilePath();
        String imageUrl;
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            // 如果文件路径本身就是完整URL，直接使用
            imageUrl = filePath;
        } else {
            imageUrl = baseUrl + filePath;

        }
        
        Log.d("DynamicImageGridAdapter", "Loading image at position " + position);
        Log.d("DynamicImageGridAdapter", "Base URL: " + baseUrl);
        Log.d("DynamicImageGridAdapter", "File path: " + filePath);
        Log.d("DynamicImageGridAdapter", "Full URL: " + imageUrl);
        
        Glide.with(context)
                .load(imageUrl)
                .placeholder(new ColorDrawable(ContextCompat.getColor(context, R.color.colorLightGray)))
                .error(new ColorDrawable(ContextCompat.getColor(context, R.color.colorLightGray)))
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e("DynamicImageGridAdapter", "Failed to load image: " + imageUrl, e);
                        return false;
                    }
                    
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("DynamicImageGridAdapter", "Successfully loaded image: " + imageUrl);
                        return false;
                    }
                })
                .into(holder.imageView);
        
        holder.itemView.setOnClickListener(v -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick(position, imageFile);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return imageFiles != null ? imageFiles.size() : 0;
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_dynamic_image);
        }
    }
}