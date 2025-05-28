package com.damors.zuji;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.WindowManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.damors.zuji.data.FootprintEntity;
import com.damors.zuji.viewmodel.FootprintViewModel;

import com.damors.zuji.adapter.ImageGridAdapter;

import java.util.ArrayList;
import java.util.List;

public class AddFootprintActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGES = 1;
    private static final int REQUEST_SELECT_LOCATION = 2;

    private EditText etContent;
    private RecyclerView rvImages;
    private TextView tvLocation;
    private Button btnPublish;

    private List<Uri> selectedImages = new ArrayList<>();
    private ImageGridAdapter imageAdapter;
    private String selectedLocation;
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_add_footprint);

        initViews();
        setupAdapters();
        setupClickListeners();
    }

    private void initViews() {
        etContent = findViewById(R.id.et_content);
        rvImages = findViewById(R.id.rv_images);
        tvLocation = findViewById(R.id.tv_location);
        btnPublish = findViewById(R.id.btn_publish);

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
    }

    private void setupAdapters() {
        imageAdapter = new ImageGridAdapter(this, selectedImages, new ImageGridAdapter.OnImageActionListener() {
            @Override
            public void onAddImageClick() {
                pickImages();
            }

            @Override
            public void onImageRemove(int position) {
                selectedImages.remove(position);
                imageAdapter.notifyDataSetChanged();
            }
        });

        rvImages.setLayoutManager(new GridLayoutManager(this, 3));
        rvImages.setAdapter(imageAdapter);
    }

    private void setupClickListeners() {
        // 位置选择
        findViewById(R.id.layout_location).setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationPickerActivity.class);
            startActivityForResult(intent, REQUEST_SELECT_LOCATION);
        });

        // 发布按钮
        btnPublish.setOnClickListener(v -> {
            String content = etContent.getText().toString();
            if (TextUtils.isEmpty(content) && selectedImages.isEmpty()) {
                Toast.makeText(this, "请填写内容或添加图片", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: 实现发布逻辑
            publishFootprint(content, selectedLocation);
        });
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_PICK_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGES && data != null) {
                if (data.getClipData() != null) {
                    // 多选图片
                    int count = Math.min(data.getClipData().getItemCount(), 9 - selectedImages.size());
                    for (int i = 0; i < count; i++) {
                        selectedImages.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else if (data.getData() != null && selectedImages.size() < 9) {
                    // 单选图片
                    selectedImages.add(data.getData());
                }
                imageAdapter.notifyDataSetChanged();
            } else if (requestCode == REQUEST_SELECT_LOCATION) {
                selectedLocation = data.getStringExtra("location");
                latitude = data.getDoubleExtra("latitude", 0);
                longitude = data.getDoubleExtra("longitude", 0);
                tvLocation.setText(selectedLocation);
            }
        }
    }

    private void publishFootprint(String content, String location) {
        // 显示加载状态
        btnPublish.setEnabled(false);
        btnPublish.setText("发布中...");

        // 创建足迹对象
        FootprintEntity footprint = new FootprintEntity();
        footprint.setDescription(content);
        footprint.setLocationName(location);
        footprint.setLatitude(latitude);
        footprint.setLongitude(longitude);
        footprint.setTimestamp(System.currentTimeMillis());
        footprint.setImageUris(selectedImages.toString());

        // 保存到数据库
        FootprintViewModel viewModel = new ViewModelProvider(this).get(FootprintViewModel.class);
        viewModel.insert(footprint);
        
        // 重置UI状态
        btnPublish.setEnabled(true);
        btnPublish.setText("发布");
        
        Toast.makeText(this, "发布成功", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}