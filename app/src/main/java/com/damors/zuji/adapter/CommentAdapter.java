package com.damors.zuji.adapter;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.damors.zuji.R;
import com.damors.zuji.model.CommentModel;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 评论列表适配器
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private Context context;
    private List<CommentModel> comments;
    private List<CommentModel> allComments; // 存储所有评论数据，包括回复
    private OnCommentClickListener onCommentClickListener;
    
    public interface OnCommentClickListener {
        void onReplyClick(CommentModel comment);
        void onDeleteClick(CommentModel comment);
    }
    
    public CommentAdapter(Context context, List<CommentModel> comments, List<CommentModel> allComments) {
        this.context = context;
        this.comments = comments != null ? comments : new ArrayList<>();
        this.allComments = allComments != null ? allComments : new ArrayList<>();
    }
    
    public void setComments(List<CommentModel> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * 设置所有评论数据（包括回复）
     * @param allComments 所有评论数据
     */
    public void setAllComments(List<CommentModel> allComments) {
        this.allComments = allComments != null ? allComments : new ArrayList<>();
    }
    
    /**
     * 同时设置主评论和所有评论数据
     * @param comments 主评论列表
     * @param allComments 所有评论数据（包括回复）
     */
    public void setCommentsAndAllComments(List<CommentModel> comments, List<CommentModel> allComments) {
        this.comments = comments != null ? comments : new ArrayList<>();
        this.allComments = allComments != null ? allComments : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addComments(List<CommentModel> newComments) {
        if (newComments != null && !newComments.isEmpty()) {
            int startPosition = this.comments.size();
            this.comments.addAll(newComments);
            notifyItemRangeInserted(startPosition, newComments.size());
        }
    }
    
    public void setOnCommentClickListener(OnCommentClickListener listener) {
        this.onCommentClickListener = listener;
    }
    
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommentModel comment = comments.get(position);
        
        // 设置用户头像
        if (!TextUtils.isEmpty(comment.getUserAvatar())) {
            String avatarUrl = ApiConfig.getImageBaseUrl() + comment.getUserAvatar();
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
        holder.tvUserName.setText(comment.getUserName() != null ? comment.getUserName() : "匿名用户");
        
        // 设置评论内容
        holder.tvContent.setText(comment.getContent());
        
        // 设置时间
        if (!TextUtils.isEmpty(comment.getCreateTime())) {
            holder.tvTime.setText(TimeUtils.formatTime(comment.getCreateTime()));
        } else {
            holder.tvTime.setText("");
        }
        
        // 设置回复按钮点击事件
        holder.tvReply.setOnClickListener(v -> {
            if (onCommentClickListener != null) {
                onCommentClickListener.onReplyClick(comment);
            }
        });
        
        // 设置删除按钮点击事件
        holder.tvDelete.setOnClickListener(v -> {
            if (onCommentClickListener != null) {
                onCommentClickListener.onDeleteClick(comment);
            }
        });
        
        // 根据评论所有者显示删除按钮（这里可以根据实际需求判断是否显示删除按钮）
        // TODO: 添加判断当前用户是否为评论作者的逻辑
        holder.tvDelete.setVisibility(View.VISIBLE); // 暂时显示所有删除按钮，后续可根据用户权限控制
        
        // 处理子评论显示
        android.util.Log.d("CommentAdapter", "处理评论ID=" + comment.getId() + ", parentId=" + comment.getParentId() + ", 内容=" + comment.getContent());
        List<CommentModel> replies = getRepliesByParentId(comment.getId());
        if (replies != null && !replies.isEmpty()) {
            holder.tvReplyCount.setVisibility(View.VISIBLE);
            
            // 根据展开状态设置文本
            if (holder.isExpanded) {
                holder.tvReplyCount.setText("收起回复");
                showReplies(holder, replies);
            } else {
                holder.tvReplyCount.setText(String.format("查看%d条回复", replies.size()));
                holder.layoutReplies.setVisibility(View.GONE);
            }
            
            holder.tvReplyCount.setOnClickListener(v -> {
                holder.isExpanded = !holder.isExpanded;
                if (holder.isExpanded) {
                    holder.tvReplyCount.setText("收起回复");
                    showReplies(holder, replies);
                } else {
                    holder.tvReplyCount.setText(String.format("查看%d条回复", replies.size()));
                    holder.layoutReplies.setVisibility(View.GONE);
                }
            });
        } else {
            holder.tvReplyCount.setVisibility(View.GONE);
            holder.layoutReplies.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return comments.size();
    }
    
    /**
     * 根据父评论ID获取回复列表
     * @param parentId 父评论ID
     * @return 回复列表
     */
    private List<CommentModel> getRepliesByParentId(Integer parentId) {
        if (allComments == null || parentId == null) {
            return new ArrayList<>();
        }
        
        List<CommentModel> replies = new ArrayList<>();
        for (CommentModel comment : allComments) {
            if (parentId.equals(comment.getParentId())) {
                replies.add(comment);
            }
        }
        
        // 添加调试日志
        android.util.Log.d("CommentAdapter", "查找parentId=" + parentId + "的回复，找到" + replies.size() + "条回复");
        
        return replies;
    }

    /**
     * 显示子评论列表
     * @param holder ViewHolder
     * @param replies 子评论列表
     */
    private void showReplies(CommentViewHolder holder, List<CommentModel> replies) {
        holder.layoutReplies.removeAllViews();
        holder.layoutReplies.setVisibility(View.VISIBLE);
        
        LayoutInflater inflater = LayoutInflater.from(context);
        
        for (CommentModel reply : replies) {
            View replyView = inflater.inflate(R.layout.item_reply, holder.layoutReplies, false);
            
            // 绑定子评论数据
            ImageView ivUserAvatar = replyView.findViewById(R.id.iv_user_avatar);
            TextView tvUserName = replyView.findViewById(R.id.tv_user_name);
            TextView tvContent = replyView.findViewById(R.id.tv_content);
            TextView tvTime = replyView.findViewById(R.id.tv_time);
            TextView tvReply = replyView.findViewById(R.id.tv_reply);
            
            // 设置用户头像
            if (!TextUtils.isEmpty(reply.getUserAvatar())) {
                // 构建完整的头像URL
                String replyAvatarUrl = ApiConfig.getImageBaseUrl() + reply.getUserAvatar();
                Glide.with(context)
                    .load(replyAvatarUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop() // 添加圆形裁剪，与主评论保持一致
                    .into(ivUserAvatar);
            } else {
                ivUserAvatar.setImageResource(R.drawable.ic_default_avatar);
            }
            
            // 设置用户名
            tvUserName.setText(reply.getUserName() != null ? reply.getUserName() : "匿名用户");
            
            // 设置回复内容，按照新的格式："回复 被回复用户名：回复内容"
            if (!TextUtils.isEmpty(reply.getParentUserName())) {
                // 创建SpannableString来设置不同颜色
                SpannableString spannableContent = new SpannableString(
                    "回复 " + reply.getParentUserName() + "：" + reply.getContent());
                
                // 设置"回复 被回复用户名"为蓝色
                int blueTextEnd = ("回复 " + reply.getParentUserName()).length();
                spannableContent.setSpan(
                    new ForegroundColorSpan(
                        ContextCompat.getColor(context, R.color.primary_color)),
                    0, blueTextEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                tvContent.setText(spannableContent);
            } else {
                tvContent.setText(reply.getContent());
            }
            
            // 设置时间
            if (!TextUtils.isEmpty(reply.getCreateTime())) {
                tvTime.setText(TimeUtils.formatTime(reply.getCreateTime()));
            } else {
                tvTime.setText("");
            }
            
            // 设置回复按钮点击事件
            tvReply.setOnClickListener(v -> {
                if (onCommentClickListener != null) {
                    onCommentClickListener.onReplyClick(reply);
                }
            });
            
            // 设置删除按钮点击事件
            TextView tvDelete = replyView.findViewById(R.id.tv_delete);
            tvDelete.setOnClickListener(v -> {
                if (onCommentClickListener != null) {
                    onCommentClickListener.onDeleteClick(reply);
                }
            });
            
            // 根据回复所有者显示删除按钮（这里可以根据实际需求判断是否显示删除按钮）
            // TODO: 添加判断当前用户是否为回复作者的逻辑
            tvDelete.setVisibility(View.VISIBLE); // 暂时显示所有删除按钮，后续可根据用户权限控制
            
            holder.layoutReplies.addView(replyView);
        }
    }
    
    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUserName;
        TextView tvContent;
        TextView tvTime;
        TextView tvReply;
        TextView tvDelete;
        TextView tvReplyCount;
        LinearLayout layoutReplies;
        boolean isExpanded = false;
        
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvReply = itemView.findViewById(R.id.tv_reply);
            tvDelete = itemView.findViewById(R.id.tv_delete);
            tvReplyCount = itemView.findViewById(R.id.tv_reply_count);
            layoutReplies = itemView.findViewById(R.id.layout_replies);
        }
    }
}