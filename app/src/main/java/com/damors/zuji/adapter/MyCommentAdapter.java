package com.damors.zuji.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.damors.zuji.R;
import com.damors.zuji.model.CommentModel;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.utils.DateUtils;
import com.damors.zuji.utils.TimeUtils;

import java.util.Date;
import java.util.List;

/**
 * 我的评论适配器
 * 用于显示用户发表的评论列表
 */
public class MyCommentAdapter extends RecyclerView.Adapter<MyCommentAdapter.ViewHolder> {

    private Context context;
    private List<CommentModel> commentList;
    private OnCommentClickListener listener;

    public MyCommentAdapter(Context context, List<CommentModel> commentList) {
        this.context = context;
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommentModel comment = commentList.get(position);

        // 设置用户头像
        if (!TextUtils.isEmpty(comment.getUserAvatar())) {
            Glide.with(context)
                    .load(ApiConfig.getBaseUrl()+comment.getUserAvatar())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar);
        }

        // 设置用户名
        holder.tvUserName.setText(comment.getUserName());

        // 设置评论时间
        holder.tvCommentTime.setText(TimeUtils.formatRelativeTime(comment.getCreateTime()));

        // 设置评论内容
        if (null != comment.getParentId() && !TextUtils.isEmpty(comment.getParentUserName())) {
            // 如果是回复评论，显示回复格式
            holder.tvCommentContent.setText(String.format("回复 @%s：%s", 
                    comment.getParentUserName(), comment.getContent()));
        } else {
            // 普通评论
            holder.tvCommentContent.setText(comment.getContent());
        }

        // 设置动态内容
        if (!TextUtils.isEmpty(comment.getRemark())) {
            holder.tvDynamicContent.setVisibility(View.VISIBLE);
            holder.tvDynamicContent.setText(comment.getRemark());
        } else {
            holder.tvDynamicContent.setVisibility(View.GONE);
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommentClick(comment);
            }
        });

        holder.tvDynamicContent.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDynamicClick(comment);
            }
        });

        holder.tvDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(comment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList == null ? 0 : commentList.size();
    }

    /**
     * ViewHolder类
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUserName;
        TextView tvCommentTime;
        TextView tvCommentContent;
        TextView tvDynamicContent;
        TextView tvDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvCommentTime = itemView.findViewById(R.id.tv_comment_time);
            tvCommentContent = itemView.findViewById(R.id.tv_comment_content);
            tvDynamicContent = itemView.findViewById(R.id.tv_dynamic_content);
            tvDelete = itemView.findViewById(R.id.tv_delete);
        }
    }

    /**
     * 设置评论点击监听器
     */
    public void setOnCommentClickListener(OnCommentClickListener listener) {
        this.listener = listener;
    }

    /**
     * 评论点击监听器接口
     */
    public interface OnCommentClickListener {
        /**
         * 评论点击
         */
        void onCommentClick(CommentModel comment);

        /**
         * 删除按钮点击
         */
        void onDeleteClick(CommentModel comment);

        /**
         * 动态内容点击
         */
        void onDynamicClick(CommentModel comment);
    }
}