package com.damors.zuji.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.damors.zuji.fragment.ReceivedCommentsFragment;
import com.damors.zuji.fragment.MyCommentsFragment;

public class CommentManagementAdapter extends FragmentStateAdapter {
    
    public CommentManagementAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ReceivedCommentsFragment();
            case 1:
                return new MyCommentsFragment();
            default:
                return new ReceivedCommentsFragment();
        }
    }
    
    @Override
    public int getItemCount() {
        return 2; // 两个Tab：收到的评论、我的评论
    }
}