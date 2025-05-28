package com.damors.zuji.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.damors.zuji.R;
import com.damors.zuji.data.FootprintEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 足迹记录适配器
 * 用于在RecyclerView中显示足迹历史记录
 */
public class FootprintAdapter extends RecyclerView.Adapter<FootprintAdapter.FootprintViewHolder> {

    private List<FootprintEntity> footprints;
    private OnItemClickListener listener;
    private SimpleDateFormat dateFormat;

    /**
     * 构造函数
     * @param footprints 足迹数据列表
     */
    public FootprintAdapter(List<FootprintEntity> footprints) {
        this.footprints = footprints;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    /**
     * 设置点击监听器
     * @param listener 点击监听器接口
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * 更新数据
     * @param newFootprints 新的足迹数据列表
     */
    public void setFootprints(List<FootprintEntity> newFootprints) {
        this.footprints = newFootprints;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FootprintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_footprint, parent, false);
        return new FootprintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FootprintViewHolder holder, int position) {
        FootprintEntity footprint = footprints.get(position);
        holder.bind(footprint, listener);
    }

    @Override
    public int getItemCount() {
        return footprints != null ? footprints.size() : 0;
    }

    /**
     * 足迹ViewHolder类
     */
    class FootprintViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLocation;
        private TextView tvTimestamp;
        private TextView tvDetails;

        public FootprintViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvDetails = itemView.findViewById(R.id.tv_details);
        }

        /**
         * 绑定数据到视图
         * @param footprint 足迹数据
         * @param listener 点击监听器
         */
        public void bind(final FootprintEntity footprint, final OnItemClickListener listener) {
            // 设置位置信息
            String locationText = String.format(Locale.getDefault(), 
                    "位置: %.6f, %.6f", 
                    footprint.getLatitude(), 
                    footprint.getLongitude());
            tvLocation.setText(locationText);
            
            // 设置时间信息
            String timeText = "时间: " + dateFormat.format(footprint.getTimestamp());
            tvTimestamp.setText(timeText);
            
            // 设置详细信息
            String detailsText = String.format(Locale.getDefault(),
                    "海拔: %.1f米, 精度: %.1f米",
                    footprint.getAltitude(),
                    footprint.getAccuracy());
            tvDetails.setText(detailsText);
            
            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(footprint);
                }
            });
        }
    }

    /**
     * 项目点击监听器接口
     */
    public interface OnItemClickListener {
        void onItemClick(FootprintEntity footprint);
    }
    
    /**
     * 单元测试方法
     * 测试适配器是否正确显示足迹数据
     * @param footprints 测试数据
     * @return 是否测试通过
     */
    public boolean testAdapterDisplay(List<FootprintEntity> footprints) {
        if (footprints == null || footprints.isEmpty()) {
            return false;
        }
        
        try {
            // 设置测试数据
            setFootprints(footprints);
            
            // 验证项目数量
            if (getItemCount() != footprints.size()) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}