package com.damors.zuji.adapter;

import com.damors.zuji.R;


import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ImageGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ADD = 0;
    private static final int TYPE_IMAGE = 1;

    private Context context;
    private List<Uri> images;
    private OnImageActionListener listener;
    private int maxCount = 9;

    public ImageGridAdapter(Context context, List<Uri> images, OnImageActionListener listener) {
        this.context = context;
        this.images = images;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == images.size() && position < maxCount) ? TYPE_ADD : TYPE_IMAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_add_image, parent, false);
            return new AddImageHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
            return new ImageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ImageHolder) {
            Uri imageUri = images.get(position);
            ImageHolder imageHolder = (ImageHolder) holder;
            Glide.with(context).load(imageUri).into(imageHolder.imageView);

            // 设置删除按钮点击事件
            imageHolder.btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageRemove(position);
                }
            });

            holder.itemView.setOnClickListener(v -> {
                // 点击查看大图
            });
        } else if (holder instanceof AddImageHolder) {
            holder.itemView.setOnClickListener(v -> listener.onAddImageClick());
        }
    }

    @Override
    public int getItemCount() {
        return images.size() < maxCount ? images.size() + 1 : images.size();
    }

    static class ImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView btnRemove;

        public ImageHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_image);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }

    static class AddImageHolder extends RecyclerView.ViewHolder {
        public AddImageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface OnImageActionListener {
        void onAddImageClick();
        void onImageRemove(int position);
    }
}