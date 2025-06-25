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
import com.damors.zuji.utils.TimeUtils;

import java.util.List;

/**
 * 收到的评论适配器
 * 用于显示其他用户对我的评论
 */
public class ReceivedCommentAdapter extends RecyclerView.Adapter<ReceivedCommentAdapter.ViewHolder> {
    
    private Context context;
    private List<CommentModel> commentList;
    private OnCommentClickListener onCommentClickListener;
    
    public interface OnCommentClickListener {
        void onCommentClick(CommentModel comment);
        void onReplyClick(CommentModel comment);
        void onDynamicClick(CommentModel comment);
    }
    
    public ReceivedCommentAdapter(Context context, List<CommentModel> commentList) {
        this.context = context;
        this.commentList = commentList;
    }
    
    public void setOnCommentClickListener(OnCommentClickListener listener) {
        this.onCommentClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_received_comment, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommentModel comment = commentList.get(position);
        
        // 设置用户头像
        if (!TextUtils.isEmpty(comment.getUserAvatar())) {
            String avatarUrl = ApiConfig.getBaseUrl() + comment.getUserAvatar();
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar);
        }
        
        // 设置用户名
        holder.tvUserName.setText(comment.getUserName());
        
        // 设置评论内容
        holder.tvCommentContent.setText(comment.getContent());
        
        // 设置评论时间
        if (!TextUtils.isEmpty(comment.getCreateTime())) {
            holder.tvCommentTime.setText(TimeUtils.formatRelativeTime(comment.getCreateTime()));
        }
        
        // 设置动态内容（显示是对哪条动态的评论）
        if (!TextUtils.isEmpty(comment.getParentComment())) {
            holder.tvDynamicContent.setText("评论了你的动态：" + comment.getParentComment());
            holder.tvDynamicContent.setVisibility(View.VISIBLE);
        } else {
            holder.tvDynamicContent.setVisibility(View.GONE);
        }
        
        // 设置未读状态指示器
        if (comment.getParentIsRead() == 0) { // 未读
            holder.viewUnreadIndicator.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundResource(R.drawable.bg_unread_comment_item);
        } else { // 已读
            holder.viewUnreadIndicator.setVisibility(View.GONE);
            holder.itemView.setBackgroundResource(R.drawable.bg_comment_item);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onCommentClickListener != null) {
                onCommentClickListener.onCommentClick(comment);
            }
        });
        
        // 回复按钮点击事件
        holder.tvReply.setOnClickListener(v -> {
            if (onCommentClickListener != null) {
                onCommentClickListener.onReplyClick(comment);
            }
        });
        
        // 动态内容点击事件
        holder.tvDynamicContent.setOnClickListener(v -> {
            if (onCommentClickListener != null) {
                onCommentClickListener.onDynamicClick(comment);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUserName;
        TextView tvCommentContent;
        TextView tvCommentTime;
        TextView tvDynamicContent;
        TextView tvReply;
        View viewUnreadIndicator;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvCommentContent = itemView.findViewById(R.id.tv_comment_content);
            tvCommentTime = itemView.findViewById(R.id.tv_comment_time);
            tvDynamicContent = itemView.findViewById(R.id.tv_dynamic_content);
            tvReply = itemView.findViewById(R.id.tv_reply);
            viewUnreadIndicator = itemView.findViewById(R.id.view_unread_indicator);
        }
    }
}