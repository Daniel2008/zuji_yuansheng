package com.damors.zuji.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.damors.zuji.fragment.ReceivedCommentsFragment;
import com.damors.zuji.fragment.MyCommentsFragment;

/**
 * 评论管理页面的ViewPager适配器
 * 管理两个Fragment：收到的评论、我的评论
 */
public class CommentManagementPagerAdapter extends FragmentStateAdapter {
    
    private static final int TAB_COUNT = 2;
    
    public CommentManagementPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ReceivedCommentsFragment(); // 收到的评论
            case 1:
                return new MyCommentsFragment(); // 我的评论
            default:
                return new ReceivedCommentsFragment();
        }
    }
    
    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}