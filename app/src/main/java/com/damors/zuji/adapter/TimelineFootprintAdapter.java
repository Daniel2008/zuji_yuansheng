package com.damors.zuji.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
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
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.core.LatLonPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 时间轴足迹适配器
 * 用于在历史页面中以时间轴形式显示足迹记录
 */
public class TimelineFootprintAdapter extends RecyclerView.Adapter<TimelineFootprintAdapter.FootprintViewHolder> implements GeocodeSearch.OnGeocodeSearchListener {

    private List<FootprintEntity> footprints;
    private Context context;
    private OnItemClickListener listener;
    private GeocodeSearch geocodeSearch;

    /**
     * 构造函数
     * @param context 上下文
     * @param footprints 足迹列表
     */
    public TimelineFootprintAdapter(Context context, List<FootprintEntity> footprints) {
        this.context = context;
        this.footprints = footprints != null ? footprints : new ArrayList<>();
        try {
            this.geocodeSearch = new GeocodeSearch(context);
            this.geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (Exception e) {
            Log.e(TAG, "初始化GeocodeSearch失败: " + e.getMessage());
        }
    }

    /**
     * 点击事件监听器接口
     */
    public interface OnItemClickListener {
        void onItemClick(FootprintEntity footprint);
    }

    /**
     * 设置点击事件监听器
     * @param listener 监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FootprintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_timeline_footprint, parent, false);
        return new FootprintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FootprintViewHolder holder, int position) {
        FootprintEntity footprint = footprints.get(position);
        
        // 设置时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        holder.timeTextView.setText(dateFormat.format(new Date(footprint.getTimestamp())));
        
        // 设置位置信息（使用地理编码）
        setLocationInfo(holder, footprint);
        
        // 设置描述
        if (footprint.getDescription() != null && !footprint.getDescription().isEmpty()) {
            holder.descriptionTextView.setText(footprint.getDescription());
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }
        
        // 设置分类
        if (footprint.getCategory() != null && !footprint.getCategory().isEmpty()) {
            holder.categoryTextView.setText(footprint.getCategory());
            holder.categoryTextView.setVisibility(View.VISIBLE);
        } else {
            holder.categoryTextView.setVisibility(View.GONE);
        }
        
        // 设置图片网格布局
        if (footprint.getImageUris() != null && !footprint.getImageUris().isEmpty()) {
            holder.gridImageLayout.setVisibility(View.VISIBLE);
            // 这里可以进一步实现九宫格图片显示逻辑
            // 暂时只显示布局，具体的图片加载可以在后续完善
        } else {
            holder.gridImageLayout.setVisibility(View.GONE);
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
    
    /**
     * 设置位置信息显示
     * 优先显示地址描述，如果没有则通过地理编码获取
     * @param holder ViewHolder
     * @param footprint 足迹实体
     */
    private void setLocationInfo(FootprintViewHolder holder, FootprintEntity footprint) {
        String locationText = footprint.getLocationName();
        
        if (locationText != null && !locationText.isEmpty()) {
            // 如果已有位置名称，直接显示
            if (footprint.getCityName() != null && !footprint.getCityName().isEmpty()) {
                locationText = footprint.getCityName() + " · " + locationText;
            }
            holder.locationTextView.setText(locationText);
        } else {
            // 如果没有位置名称，先显示经纬度，然后异步获取地址描述
            String defaultLocation = String.format(Locale.getDefault(), "%.6f, %.6f", 
                    footprint.getLatitude(), footprint.getLongitude());
            holder.locationTextView.setText(defaultLocation);
            
            // 使用高德地图SDK进行反向地理编码
            if (geocodeSearch != null) {
                try {
                    LatLonPoint latLonPoint = new LatLonPoint(footprint.getLatitude(), footprint.getLongitude());
                    RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
                    
                    // 存储足迹ID和ViewHolder位置用于回调
                     holder.locationTextView.setTag(R.id.tag_holder, footprint.getId());
                    
                    geocodeSearch.getFromLocationAsyn(query);
                    Log.d("TimelineAdapter", "开始反向地理编码: " + footprint.getLatitude() + ", " + footprint.getLongitude());
                } catch (Exception e) {
                    Log.e("TimelineAdapter", "反向地理编码请求失败: " + e.getMessage());
                    String fallbackLocation = String.format(Locale.getDefault(), "%.6f, %.6f", 
                            footprint.getLatitude(), footprint.getLongitude());
                    holder.locationTextView.setText(fallbackLocation);
                }
            } else {
                String fallbackLocation = String.format(Locale.getDefault(), "%.6f, %.6f", 
                        footprint.getLatitude(), footprint.getLongitude());
                holder.locationTextView.setText(fallbackLocation);
            }
        }
    }

    @Override
    public int getItemCount() {
        return footprints.size();
    }

    /**
     * 更新足迹列表
     * @param newFootprints 新的足迹列表
     */
    public void updateFootprints(List<FootprintEntity> newFootprints) {
        this.footprints = newFootprints != null ? newFootprints : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * 设置足迹列表（兼容性方法）
     * @param newFootprints 新的足迹列表
     */
    public void setFootprints(List<FootprintEntity> newFootprints) {
        updateFootprints(newFootprints);
    }
    
    /**
     * 清理资源，取消待处理的地理编码请求
     */
    public void cleanup() {
        if (geocodeSearch != null) {
            geocodeSearch = null;
        }
    }
    
    private static final String TAG = "TimelineFootprintAdapter";
    
    @Override
     public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
         if (rCode == 1000) {
             if (result != null && result.getRegeocodeAddress() != null) {
                 RegeocodeAddress address = result.getRegeocodeAddress();
                 String addressText = address.getFormatAddress();
                 
                 // 在主线程中更新UI
                 if (context instanceof android.app.Activity) {
                     ((android.app.Activity) context).runOnUiThread(() -> {
                         // 遍历当前可见的ViewHolder来更新对应的位置信息
                         updateLocationTextForFootprint(addressText);
                         Log.d(TAG, "反向地理编码成功: " + addressText);
                     });
                 }
             }
         } else {
             Log.w(TAG, "反向地理编码失败，错误码: " + rCode);
         }
     }
     
     /**
      * 更新对应足迹的位置文本
      */
     private void updateLocationTextForFootprint(String addressText) {
         // 由于RecyclerView的复用机制，这里采用简化的处理方式
         // 在实际项目中，可以考虑使用更复杂的缓存机制
         notifyDataSetChanged();
     }
    
    @Override
    public void onGeocodeSearched(GeocodeResult result, int rCode) {
        // 不需要实现正向地理编码
    }

    /**
     * ViewHolder类
     */
    public static class FootprintViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView;
        TextView locationTextView;
        TextView descriptionTextView;
        TextView categoryTextView;
        View gridImageLayout;

        public FootprintViewHolder(@NonNull View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.text_view_time);
            locationTextView = itemView.findViewById(R.id.text_view_location);
            descriptionTextView = itemView.findViewById(R.id.text_view_description);
            categoryTextView = itemView.findViewById(R.id.text_view_category);
            gridImageLayout = itemView.findViewById(R.id.grid_image_layout);
        }
    }
}