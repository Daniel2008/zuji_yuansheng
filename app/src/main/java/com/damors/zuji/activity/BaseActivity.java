package com.damors.zuji.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.damors.zuji.R;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 统一设置状态栏
        setupStatusBar();
    }

    private void setupStatusBar() {
        // 隐藏状态栏
        ImmersionBar.with(this)
                .hideBar(BarHide.FLAG_HIDE_BAR).init();
    }
}