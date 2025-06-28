package com.damors.zuji.activity;

import static com.amap.api.maps.model.BitmapDescriptorFactory.getContext;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.damors.zuji.manager.BadgeManager;
import com.damors.zuji.utils.ImageUtils;

import com.damors.zuji.R;
import com.damors.zuji.adapter.CommentAdapter;
import com.damors.zuji.model.CommentModel;
import com.damors.zuji.model.TrandsMsgModel;
import com.damors.zuji.model.GuluFileModel;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.network.ApiConfig;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.utils.LoadingDialog;
import com.damors.zuji.utils.TimeUtils;
import com.damors.zuji.utils.AndroidBug5497Workaround;
import android.widget.LinearLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

/**
 * 评论列表页面
 */
public class CommentListActivity extends BaseActivity implements CommentAdapter.OnCommentClickListener {

    
    private static final String TAG = "CommentListActivity";
    private static final String EXTRA_MSG_ID = "msg_id";
    private static final String EXTRA_MSG_TITLE = "msg_title";
    
    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmpty;
    private EditText etComment;
    private ImageButton btnSend;
    
    // 动态显示相关组件
    private androidx.constraintlayout.widget.ConstraintLayout llDynamicContainer;
    private ImageView ivUserAvatar;
    private TextView tvUserName;
    private TextView tvPublishTime;
    private TextView tvTag;
    private TextView tvDynamicContent;
    private LinearLayout layoutLocation;
    private TextView tvLocation;
    private TextView tvCoordinates;
    private FrameLayout frameLayoutImages;
    private ImageView imageViewSingle;
    private View gridImageLayout;
    private TextView textViewImageCount;
    private LinearLayout layoutActions;
    private LinearLayout layoutLike;
    private ImageView ivLike;
    private TextView tvLikeCount;
    private LinearLayout layoutComment;
    private ImageView ivComment;
    private TextView tvCommentCount;
    private View dividerDynamic;
    
    private RetrofitApiService apiService;
    private LoadingDialog loadingDialog;
    private CommentModel currentReplyComment; // 当前回复的评论
    
    private Integer msgId;
    private String msgTitle;
    
    /**
     * 启动评论列表页面
     * @param context 上下文
     * @param msgId 足迹消息ID
     * @param msgTitle 足迹标题
     */
    public static void start(Context context, Integer msgId, String msgTitle) {
        Intent intent = new Intent(context, CommentListActivity.class);
        intent.putExtra(EXTRA_MSG_ID, msgId);
        intent.putExtra(EXTRA_MSG_TITLE, msgTitle);
        context.startActivity(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_comment_list);
        AndroidBug5497Workaround.assistActivity(this);

        // 获取传递的参数
        msgId = getIntent().getIntExtra(EXTRA_MSG_ID, -1);
        msgTitle = getIntent().getStringExtra(EXTRA_MSG_TITLE);
        
        if (msgId == -1) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        initData();
        setupKeyboardListener();
        loadMsgDetail(); // 加载动态详情和评论
    }
    
    private void initViews() {
        // 设置标题
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("评论详情");
        
        // 返回按钮
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        
        // 初始化RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 初始化适配器
        adapter = new CommentAdapter(this, new ArrayList<>(), new ArrayList<>());
        adapter.setOnCommentClickListener(this);
        recyclerView.setAdapter(adapter);
        
        // 下拉刷新
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::loadMsgDetail);
        
        // 初始化嵌套滚动视图
        androidx.core.widget.NestedScrollView nestedScrollView = findViewById(R.id.nested_scroll_view);
        
        // 空状态视图
        tvEmpty = findViewById(R.id.tv_empty);
        
        // 动态显示相关组件
        llDynamicContainer = findViewById(R.id.ll_dynamic_container);
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        tvUserName = findViewById(R.id.tv_user_name);
        tvPublishTime = findViewById(R.id.tv_publish_time);
        tvTag = findViewById(R.id.tv_tag);
        tvDynamicContent = findViewById(R.id.tv_dynamic_content);
        layoutLocation = findViewById(R.id.layout_location);
        tvLocation = findViewById(R.id.tv_location);
        tvCoordinates = findViewById(R.id.tv_coordinates);
        layoutActions = findViewById(R.id.layout_actions);
        layoutLike = findViewById(R.id.layout_like);
        ivLike = findViewById(R.id.iv_like);
        tvLikeCount = findViewById(R.id.tv_like_count);
        layoutComment = findViewById(R.id.layout_comment);
        ivComment = findViewById(R.id.iv_comment);
        tvCommentCount = findViewById(R.id.tv_comment_count);
        dividerDynamic = findViewById(R.id.divider_dynamic);

        // 获取图片相关控件
        frameLayoutImages = findViewById(R.id.frame_layout_images);
        imageViewSingle = findViewById(R.id.image_view_single);
        gridImageLayout = findViewById(R.id.grid_image_layout);
        textViewImageCount = findViewById(R.id.text_view_image_count);
        
        // 设置点赞按钮点击事件
        layoutLike.setOnClickListener(v -> {
            // 调用API更新点赞状态
            apiService.toggleLike(msgId,
                    new RetrofitApiService.SuccessCallback<BaseResponse<JSONObject>>() {
                        @Override
                        public void onSuccess(BaseResponse<JSONObject> response) {
                            if (response != null && response.getCode() == 200) {
                                // API调用成功，显示提示信息
                                Toast.makeText(getContext(), "点赞状态更新成功", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "点赞状态更新成功: " + response.getData());
                            } else {

                                String msg = response != null ? response.getMsg() : "点赞操作失败";
                                Toast.makeText(getContext(), "点赞状态更新失败", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "点赞状态更新失败: " + msg);
                            }
                        }
                    },
                    new RetrofitApiService.ErrorCallback() {
                        @Override
                        public void onError(String error) {

                            Toast.makeText(getContext(), "点赞操作失败，请重试", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "点赞状态更新失败: " + error);
                        }
                    }
            );
        });
        
        // 设置评论按钮点击事件
        layoutComment.setOnClickListener(v -> {
            etComment.requestFocus();
            // 弹出软键盘
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        
        // 评论输入框
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_send);
        
        // 设置输入框焦点监听
        etComment.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 延迟执行，等待软键盘完全弹出后滚动到底部
                etComment.postDelayed(() -> {
                    // 滚动到最底部，让用户看到最新评论
                    if (adapter.getItemCount() > 0) {
                        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                }, 200);
            }
        });
        
        btnSend.setOnClickListener(v -> sendComment());
    }
    
    private void initData() {
        apiService = RetrofitApiService.getInstance(this);
        loadingDialog = new LoadingDialog(this);
    }
    
    /**
     * 设置软键盘监听器
     * 使用adjustResize模式，确保输入框正确响应软键盘
     */
    private void setupKeyboardListener() {
        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                
                // 如果软键盘高度大于屏幕高度的15%，认为软键盘已弹出
                if (keypadHeight > screenHeight * 0.15) {
                    // 软键盘弹出时，确保最新评论可见
                    etComment.post(() -> {
                        if (adapter.getItemCount() > 0) {
                            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                        }
                    });
                }
            }
        });
        
        // 输入框获得焦点时的处理
        etComment.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 延迟滚动，确保键盘完全弹出后再滚动
                etComment.postDelayed(() -> {
                    if (adapter.getItemCount() > 0) {
                        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                }, 200);
            }
        });
    }
    
    /**
     * 加载动态详情和评论
     */
    private void loadMsgDetail() {
        Log.d(TAG, "加载动态详情和评论，msgId: " + msgId);
        
        apiService.getMsgDetail(msgId,
                new RetrofitApiService.SuccessCallback<BaseResponse<TrandsMsgModel>>() {
                    @Override
                    public void onSuccess(BaseResponse<TrandsMsgModel> response) {
                        swipeRefreshLayout.setRefreshing(false);
                        
                        if (response != null && response.isSuccess() && response.getData() != null) {
                            TrandsMsgModel msgDetail = response.getData();
                            // 显示动态详情
                            displayMsgDetail(msgDetail);
                            
                            // 处理评论数据
                            List<CommentModel> allComments = msgDetail.getComments();
                            if (allComments != null && !allComments.isEmpty()) {
                                Log.d(TAG, "获取到评论数量: " + allComments.size());
                                
                                // 分离主评论和回复
                                List<CommentModel> mainComments = new ArrayList<>();
                                for (CommentModel comment : allComments) {
                                    if (comment.getParentId() == null || comment.getParentId() == 0) {
                                        mainComments.add(comment);
                                    }
                                }
                                
                                Log.d(TAG, "主评论数量: " + mainComments.size());
                                
                                adapter.setCommentsAndAllComments(mainComments, allComments);
                                
                                // 显示评论列表
                                tvEmpty.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            } else {
                                Log.w(TAG, "评论数据为空");
                                tvEmpty.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            }
                        } else {
                            Log.w(TAG, "动态详情数据为空或获取失败");
                            // 隐藏动态显示区域
                            llDynamicContainer.setVisibility(View.GONE);
                            dividerDynamic.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    }
                },
                new RetrofitApiService.ErrorCallback() {
                    @Override
                    public void onError(String error) {
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "加载动态详情失败: " + error);
                        Toast.makeText(CommentListActivity.this, "加载失败: " + error, Toast.LENGTH_SHORT).show();
                        
                        // 隐藏动态显示区域
                        llDynamicContainer.setVisibility(View.GONE);
                        dividerDynamic.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                }
        );
    }
    
    /**
     * 显示动态详情
     * @param msgDetail 动态详情数据
     */
    private void displayMsgDetail(TrandsMsgModel msgDetail) {
        // 显示动态容器
        llDynamicContainer.setVisibility(View.VISIBLE);
        dividerDynamic.setVisibility(View.VISIBLE);
        
        // 设置用户头像
        if (msgDetail.getUserAvatar() != null && !msgDetail.getUserAvatar().isEmpty()) {
            Glide.with(this)
                .load(ApiConfig.getBaseUrl() + msgDetail.getUserAvatar())
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(ivUserAvatar);
        } else {
            ivUserAvatar.setImageResource(R.drawable.ic_person);
        }
        
        // 设置用户名
        if (msgDetail.getUserName() != null && !msgDetail.getUserName().isEmpty()) {
            tvUserName.setText(msgDetail.getUserName());
        } else {
            tvUserName.setText("匿名用户");
        }
        
        // 设置发布时间
        if (msgDetail.getCreateTime() != null) {
            tvPublishTime.setText(TimeUtils.formatRelativeTime(msgDetail.getCreateTime()));
        } else {
            tvPublishTime.setText("未知时间");
        }
        
        // 设置标签（暂时隐藏，可根据需要显示）
        tvTag.setVisibility(View.GONE);
        
        // 设置动态内容
        if (msgDetail.getTextContent() != null && !msgDetail.getTextContent().isEmpty()) {
            tvDynamicContent.setText(msgDetail.getTextContent());
        } else {
            tvDynamicContent.setText("[图片动态]");
        }
        
        // 设置位置信息
        if (msgDetail.getLocaltionTitle() != null && !msgDetail.getLocaltionTitle().isEmpty()) {
            layoutLocation.setVisibility(View.VISIBLE);
            tvLocation.setText(msgDetail.getLocaltionTitle());
            
            // 设置坐标信息
            if (msgDetail.getLat() != 0.0 && msgDetail.getLng() != 0.0) {
                tvCoordinates.setVisibility(View.VISIBLE);
                tvCoordinates.setText(String.format("坐标: %.6f, %.6f", 
                    msgDetail.getLat(), msgDetail.getLng()));
            } else {
                tvCoordinates.setVisibility(View.GONE);
            }
        } else {
            layoutLocation.setVisibility(View.GONE);
            tvCoordinates.setVisibility(View.GONE);
        }
        
        renderImageContent(msgDetail, frameLayoutImages, imageViewSingle, gridImageLayout, textViewImageCount);
        
        // 设置点赞数
        tvLikeCount.setText(String.valueOf(msgDetail.getLikeCount()));
        
        // 设置评论数
        tvCommentCount.setText(String.valueOf(msgDetail.getCommentCount()));
        
        // 根据用户是否已点赞设置点赞图标
        if (msgDetail.isHasLiked()) {
            ivLike.setImageResource(R.drawable.ic_like_filled);
            ivLike.setColorFilter(getResources().getColor(R.color.colorAccent));
        } else {
            ivLike.setImageResource(R.drawable.ic_like_outline);
            ivLike.clearColorFilter();
        }
    }

    /**
     * 渲染图片内容
     * @param message 足迹消息
     * @param frameLayoutImages 图片容器
     * @param imageViewSingle 单张图片视图
     * @param gridImageLayout 网格图片布局
     * @param textViewImageCount 图片数量文本
     */
    private void renderImageContent(TrandsMsgModel message, FrameLayout frameLayoutImages,
                                    ImageView imageViewSingle, View gridImageLayout, TextView textViewImageCount) {

        // 首先隐藏所有图片相关控件
        frameLayoutImages.setVisibility(View.GONE);
        imageViewSingle.setVisibility(View.GONE);
        if (gridImageLayout != null) {
            gridImageLayout.setVisibility(View.GONE);
        }
        textViewImageCount.setVisibility(View.GONE);

        // 检查是否有图片文件
        if (message.getGuluFiles() != null && !message.getGuluFiles().isEmpty()) {
            // 筛选出图片文件
            java.util.List<GuluFileModel> imageFiles = new java.util.ArrayList<>();

            for (GuluFileModel file : message.getGuluFiles()) {
                if (ImageUtils.isImageFile(file.getFileType())) {
                    imageFiles.add(file);
                }
            }

            Log.d("MapFragment", "找到图片文件数量: " + imageFiles.size());

            if (!imageFiles.isEmpty()) {
                frameLayoutImages.setVisibility(View.VISIBLE);

                if (imageFiles.size() == 1) {
                    // 显示单张图片
                    imageViewSingle.setVisibility(View.VISIBLE);
                    loadImageIntoView(imageViewSingle, imageFiles.get(0));

                    // 添加点击事件查看大图
                    imageViewSingle.setOnClickListener(v -> {
                        openImagePreview(imageFiles, 0);
                    });

                } else {
                    // 多张图片使用网格布局
                    if (gridImageLayout != null) {
                        gridImageLayout.setVisibility(View.VISIBLE);
                        setupGridImageLayout(gridImageLayout, imageFiles);
                    }

                    // 显示图片数量
                    textViewImageCount.setVisibility(View.VISIBLE);
                    textViewImageCount.setText(String.format("共%d张", imageFiles.size()));
                }
            }
        }
    }

    /**
     * 设置网格图片布局
     */
    private void setupGridImageLayout(View gridImageLayout, java.util.List<GuluFileModel> imageFiles) {
        try {
            if (gridImageLayout == null || imageFiles == null || imageFiles.isEmpty()) {
                Log.w("MapFragment", "setupGridImageLayout: 参数无效");
                return;
            }

            ImageView singleImage = gridImageLayout.findViewById(R.id.single_image);
            LinearLayout twoImagesLayout = gridImageLayout.findViewById(R.id.two_images_layout);
            LinearLayout threeImagesLayout = gridImageLayout.findViewById(R.id.three_images_layout);
            androidx.recyclerview.widget.RecyclerView gridRecyclerView = gridImageLayout.findViewById(R.id.grid_recycler_view);

            // 隐藏所有布局
            if (singleImage != null) singleImage.setVisibility(View.GONE);
            if (twoImagesLayout != null) twoImagesLayout.setVisibility(View.GONE);
            if (threeImagesLayout != null) threeImagesLayout.setVisibility(View.GONE);
            if (gridRecyclerView != null) gridRecyclerView.setVisibility(View.GONE);

            int imageCount = imageFiles.size();
            Log.d("MapFragment", "设置网格布局，图片数量: " + imageCount);

            if (imageCount == 2 && twoImagesLayout != null) {
                twoImagesLayout.setVisibility(View.VISIBLE);
                ImageView image1 = twoImagesLayout.findViewById(R.id.image_1_of_2);
                ImageView image2 = twoImagesLayout.findViewById(R.id.image_2_of_2);

                if (image1 != null && image2 != null) {
                    loadImageIntoView(image1, imageFiles.get(0));
                    loadImageIntoView(image2, imageFiles.get(1));
                    image1.setOnClickListener(v -> openImagePreview(imageFiles, 0));
                    image2.setOnClickListener(v -> openImagePreview(imageFiles, 1));
                }
            } else if (imageCount == 3 && threeImagesLayout != null) {
                threeImagesLayout.setVisibility(View.VISIBLE);
                ImageView image1 = threeImagesLayout.findViewById(R.id.image_1_of_3);
                ImageView image2 = threeImagesLayout.findViewById(R.id.image_2_of_3);
                ImageView image3 = threeImagesLayout.findViewById(R.id.image_3_of_3);

                if (image1 != null && image2 != null && image3 != null) {
                    loadImageIntoView(image1, imageFiles.get(0));
                    loadImageIntoView(image2, imageFiles.get(1));
                    loadImageIntoView(image3, imageFiles.get(2));
                    image1.setOnClickListener(v -> openImagePreview(imageFiles, 0));
                    image2.setOnClickListener(v -> openImagePreview(imageFiles, 1));
                    image3.setOnClickListener(v -> openImagePreview(imageFiles, 2));
                }
            } else if (imageCount >= 4 && gridRecyclerView != null) {
                Log.d("MapFragment", "显示4张及以上图片，使用RecyclerView，图片数量: " + imageCount);
                gridRecyclerView.setVisibility(View.VISIBLE);
                androidx.recyclerview.widget.GridLayoutManager gridLayoutManager =
                        new androidx.recyclerview.widget.GridLayoutManager(getContext(), com.damors.zuji.config.ImageDisplayConfig.GRID_SPAN_COUNT);
                gridRecyclerView.setLayoutManager(gridLayoutManager);

                // 添加网格间距装饰器
                if (gridRecyclerView.getItemDecorationCount() == 0) {
                    gridRecyclerView.addItemDecoration(new com.damors.zuji.utils.GridSpacingItemDecoration(com.damors.zuji.config.ImageDisplayConfig.GRID_SPAN_COUNT, com.damors.zuji.config.ImageDisplayConfig.GRID_SPACING_DP, true));
                }

                // 计算并设置RecyclerView的高度
                int calculatedHeight = com.damors.zuji.adapter.GridImageAdapter.calculateRecyclerViewHeight(getContext(), imageFiles.size());
                ViewGroup.LayoutParams layoutParams = gridRecyclerView.getLayoutParams();
                layoutParams.height = calculatedHeight;
                gridRecyclerView.setLayoutParams(layoutParams);

                com.damors.zuji.adapter.GridImageAdapter adapter = new com.damors.zuji.adapter.GridImageAdapter(getContext(), imageFiles);
                adapter.setOnImageClickListener((position, files) -> openImagePreview(files, position));
                gridRecyclerView.setAdapter(adapter);
                Log.d("MapFragment", "GridImageAdapter已设置，适配器项目数量: " + adapter.getItemCount());

                // Log.d("MapFragment", "设置RecyclerView高度: " + calculatedHeight + "px，图片数量: " + imageFiles.size());
            }
        } catch (Exception e) {
            Log.e("MapFragment", "设置网格图片布局时发生错误", e);
            // 发生错误时隐藏图片布局
            if (gridImageLayout != null) {
                gridImageLayout.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 加载图片到ImageView
     */
    private void loadImageIntoView(ImageView imageView, GuluFileModel imageFile) {
        try {
            if (imageView == null || imageFile == null) {
                Log.w("MapFragment", "loadImageIntoView: 参数无效");
                return;
            }

            String imageUrl = ImageUtils.getFullImageUrl(imageFile.getFilePath());
            if (imageUrl.isEmpty()) {
                Log.w("MapFragment", "图片URL为空，使用默认占位图");
                imageView.setImageResource(R.drawable.ic_placeholder_image);
                return;
            }

            com.bumptech.glide.Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_placeholder_image)
                    .centerCrop()
                    .thumbnail(com.damors.zuji.config.ImageDisplayConfig.THUMBNAIL_RATIO)
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade())
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            Log.e("MapFragment", "图片加载失败: " + model, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            Log.d("MapFragment", "图片加载成功: " + model);
                            return false;
                        }
                    })
                    .into(imageView);
        } catch (Exception e) {
            Log.e("MapFragment", "加载图片时发生错误", e);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_placeholder_image);
            }
        }
    }

    /**
     * 打开图片预览
     */
    private void openImagePreview(java.util.List<GuluFileModel> imageFiles, int currentIndex) {
        if (getContext() == null || imageFiles == null || imageFiles.isEmpty()) {
            return;
        }

        try {
            Intent intent = new Intent(getContext(), ImagePreviewActivity.class);

            java.util.ArrayList<String> imageUrls = new java.util.ArrayList<>();
            for (GuluFileModel file : imageFiles) {
                imageUrls.add(ImageUtils.getFullImageUrl(file.getFilePath()));
            }

            intent.putStringArrayListExtra("image_urls", imageUrls);
            intent.putExtra("current_index", currentIndex);
            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "打开图片预览失败", e);
        }
    }
    

    
    /**
     * 发送评论
     */
    private void sendComment() {
        String content = etComment.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
            return;
        }
        
        loadingDialog.show("发送中...");
        
        // 判断是否为回复评论
        Long parentId = null;
        if (currentReplyComment != null) {
            // 如果回复的是子评论，则使用其父评论ID；如果回复的是根评论，则使用其ID
            parentId = currentReplyComment.isRootComment() ? 
                       currentReplyComment.getId().longValue() : 
                       currentReplyComment.getParentId().longValue();
        }
        
        apiService.addComment(msgId, content, parentId,
            new RetrofitApiService.SuccessCallback<BaseResponse<JSONObject>>() {
                @Override
                public void onSuccess(BaseResponse<JSONObject> response) {
                    loadingDialog.dismiss();
                    if (response != null && response.getCode() == 200) {
                        String message = currentReplyComment != null ? "回复发表成功" : "评论发表成功";
                        Toast.makeText(CommentListActivity.this, message, Toast.LENGTH_SHORT).show();
                        
                        // 清空输入框和重置回复状态
                        etComment.setText("");
                        resetReplyState();
                        
                        // 刷新评论列表
                        loadMsgDetail();
                    } else {
                        String msg = response != null ? response.getMsg() : "发表失败";
                        String message = currentReplyComment != null ? "回复发表失败: " : "评论发表失败: ";
                        Toast.makeText(CommentListActivity.this, message + msg, Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new RetrofitApiService.ErrorCallback() {
                @Override
                public void onError(String error) {
                    loadingDialog.dismiss();
                    String message = currentReplyComment != null ? "回复发表失败: " : "评论发表失败: ";
                    Toast.makeText(CommentListActivity.this, message + error, Toast.LENGTH_SHORT).show();
                }
            }
        );
    }
    
    /**
     * 重置回复状态
     */
    private void resetReplyState() {
        currentReplyComment = null;
        etComment.setHint("写下你的评论...");
    }

    @Override
    public void onReplyClick(CommentModel comment) {
        // 设置回复状态
        currentReplyComment = comment;
        
        // 更新输入框提示文本
        String hintText = "回复 @" + comment.getUserName() + ":";
        etComment.setHint(hintText);
        
        // 聚焦到输入框
        etComment.requestFocus();
        
        // 显示软键盘
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
        }
        
        // 滚动到输入框位置
        etComment.postDelayed(() -> {
            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        }, 300);
    }

    @Override
    public void onDeleteClick(CommentModel comment) {
        // 显示删除确认对话框
        new AlertDialog.Builder(this)
                .setTitle("删除评论")
                .setMessage("确定要删除这条评论吗？删除后无法恢复。")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteComment(comment.getId());
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除评论
     * 
     * @param commentId 评论ID
     */
    private void deleteComment(Integer commentId) {
        apiService.deleteComment(commentId,
                response -> {
                    if (response != null && response.isSuccess()) {
                        // 删除成功
                        Toast.makeText(CommentListActivity.this, "评论删除成功", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "评论删除成功: " + response.getData());
                        
                        // 刷新评论列表
                        loadMsgDetail();
                    } else {
                        String msg = response != null ? response.getMsg() : "评论删除失败";
                        Toast.makeText(CommentListActivity.this, msg, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "评论删除失败: " + msg);
                    }
                },
                error -> {
                    // 删除失败
                    Toast.makeText(CommentListActivity.this, "评论删除失败，请重试", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "评论删除失败: " + error);
                }
        );
    }
}