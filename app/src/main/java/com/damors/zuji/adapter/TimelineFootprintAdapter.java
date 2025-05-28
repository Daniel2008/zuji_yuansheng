package com.damors.zuji.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.damors.zuji.AddFootprintActivity;
import com.damors.zuji.R;
import com.damors.zuji.data.FootprintEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 时间轴足迹适配器
 * 用于在历史页面中以时间轴形式显示足迹记录
 */
public class TimelineFootprintAdapter extends RecyclerView.Adapter<TimelineFootprintAdapter.FootprintViewHolder> {

    private List<FootprintEntity> footprints;
    private Context context;
    private OnItemClickListener listener;

    /**
     * 构造函数
     * @param context 上下文
     * @param footprints 足迹列表
     */
    public TimelineFootprintAdapter(Context context, List<FootprintEntity> footprints) {
        this.context = context;
        this.footprints = footprints != null ? footprints : new ArrayList<>();
    }

    /**
     * 设置足迹数据
     * @param footprints 足迹列表
     */
    public void setFootprints(List<FootprintEntity> footprints) {
        this.footprints = footprints != null ? footprints : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FootprintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline_footprint, parent, false);
        return new FootprintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FootprintViewHolder holder, int position) {
        FootprintEntity footprint = footprints.get(position);
        
        // 设置头像（暂时使用默认头像占位）
        holder.avatarImageView.setImageResource(R.drawable.ic_person);
        
        // 设置日期和时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date date = new Date(footprint.getTimestamp());
        
        holder.dateTextView.setText(dateFormat.format(date));
        holder.timeTextView.setText(timeFormat.format(date));
        
        // 设置位置和描述
        String locationText = footprint.getLocationName();
        if (locationText == null || locationText.isEmpty()) {
            locationText = String.format(Locale.getDefault(), "%.6f, %.6f", 
                    footprint.getLatitude(), footprint.getLongitude());
        } else if (footprint.getCityName() != null && !footprint.getCityName().isEmpty()) {
            locationText = footprint.getCityName() + " · " + locationText;
        }
        
        holder.locationTextView.setText(locationText);
        holder.descriptionTextView.setText(footprint.getDescription());
        
        // 设置分类标签
        if (footprint.getCategory() != null && !footprint.getCategory().isEmpty()) {
            holder.categoryTextView.setVisibility(View.VISIBLE);
            holder.categoryTextView.setText(footprint.getCategory());
        } else {
            holder.categoryTextView.setVisibility(View.GONE);
        }
        
        // 设置图片预览（如果有）
        if (footprint.getImageUris() != null && !footprint.getImageUris().isEmpty()) {
            holder.previewImageView.setVisibility(View.VISIBLE);
            // 使用Glide加载第一张图片
            RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image_error)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop();

            Glide.with(context)
                .load(Uri.parse(footprint.getImageUriList().get(0)))
                .apply(options)
                .into(holder.previewImageView);
        } else {
            holder.previewImageView.setVisibility(View.GONE);
        }
        
        // 设置视频标记（如果有）
        holder.videoIndicator.setVisibility(
                footprint.getVideoUri() != null && !footprint.getVideoUri().isEmpty() 
                ? View.VISIBLE : View.GONE);
        
        // 设置时间轴样式
        if (position == 0) {
            // 第一个项目
            holder.timelineTopLine.setVisibility(View.INVISIBLE);
            holder.timelineBottomLine.setVisibility(
                    footprints.size() > 1 ? View.VISIBLE : View.INVISIBLE);
        } else if (position == footprints.size() - 1) {
            // 最后一个项目
            holder.timelineTopLine.setVisibility(View.VISIBLE);
            holder.timelineBottomLine.setVisibility(View.INVISIBLE);
        } else {
            // 中间项目
            holder.timelineTopLine.setVisibility(View.VISIBLE);
            holder.timelineBottomLine.setVisibility(View.VISIBLE);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(footprint);
            } else {
                // 默认打开详情页
                Intent intent = new Intent(context, AddFootprintActivity.class);
                intent.putExtra("footprint_id", footprint.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return footprints.size();
    }

    /**
     * 设置项目点击监听器
     * @param listener 监听器接口
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * 项目点击监听器接口
     */
    public interface OnItemClickListener {
        void onItemClick(FootprintEntity footprint);
    }

    /**
     * 足迹视图持有者
     */
    static class FootprintViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView dateTextView;
        TextView timeTextView;
        TextView locationTextView;
        TextView descriptionTextView;
        TextView categoryTextView;
        ImageView previewImageView;
        View videoIndicator;
        View timelineTopLine;
        View timelineBottomLine;
        View timelineDot;

        public FootprintViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.image_view_avatar);
            dateTextView = itemView.findViewById(R.id.text_view_date);
            timeTextView = itemView.findViewById(R.id.text_view_time);
            locationTextView = itemView.findViewById(R.id.text_view_location);
            descriptionTextView = itemView.findViewById(R.id.text_view_description);
            categoryTextView = itemView.findViewById(R.id.text_view_category);
            previewImageView = itemView.findViewById(R.id.image_view_preview);
            videoIndicator = itemView.findViewById(R.id.view_video_indicator);
            timelineTopLine = itemView.findViewById(R.id.view_timeline_top);
            timelineBottomLine = itemView.findViewById(R.id.view_timeline_bottom);
            timelineDot = itemView.findViewById(R.id.view_timeline_dot);
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
            List<FootprintEntity> testData = new ArrayList<>();
            
            FootprintEntity footprint1 = new FootprintEntity();
            footprint1.setId(1);
            footprint1.setLatitude(39.9087);
            footprint1.setLongitude(116.3975);
            footprint1.setTimestamp(System.currentTimeMillis());
            footprint1.setDescription("测试足迹1");
            footprint1.setLocationName("测试位置1");
            footprint1.setCityName("北京");
            footprint1.setCategory("旅游");
            
            FootprintEntity footprint2 = new FootprintEntity();
            footprint2.setId(2);
            footprint2.setLatitude(31.2304);
            footprint2.setLongitude(121.4737);
            footprint2.setTimestamp(System.currentTimeMillis() - 86400000); // 前一天
            footprint2.setDescription("测试足迹2");
            footprint2.setLocationName("测试位置2");
            footprint2.setCityName("上海");
            footprint2.setCategory("美食");
            
            testData.add(footprint1);
            testData.add(footprint2);
            
            // 测试设置数据
            setFootprints(testData);
            
            // 验证数据
            return getItemCount() == 2 &&
                   footprints.get(0).getId() == 1 &&
                   footprints.get(1).getId() == 2 &&
                   footprints.get(0).getDescription().equals("测试足迹1") &&
                   footprints.get(1).getDescription().equals("测试足迹2");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}