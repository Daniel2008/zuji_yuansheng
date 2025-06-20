package com.damors.zuji;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.damors.zuji.R;
import com.damors.zuji.adapter.CommentAdapter;
import com.damors.zuji.model.CommentModel;
import com.damors.zuji.model.CommentResponse;
import com.damors.zuji.network.RetrofitApiService;
import com.damors.zuji.model.response.BaseResponse;
import com.damors.zuji.utils.AndroidBug5497Workaround;
import com.damors.zuji.utils.LoadingDialog;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 评论列表页面
 */
public class CommentListActivity extends AppCompatActivity implements CommentAdapter.OnCommentClickListener {
    
    private static final String TAG = "CommentListActivity";
    private static final String EXTRA_MSG_ID = "msg_id";
    private static final String EXTRA_MSG_TITLE = "msg_title";
    
    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmpty;
    private EditText etComment;
    private ImageButton btnSend;
    
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
        
        // 隐藏状态栏
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | 
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        
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
        loadComments();
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
        swipeRefreshLayout.setOnRefreshListener(this::loadComments);
        
        // 空状态视图
        tvEmpty = findViewById(R.id.tv_empty);
        
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
     * 加载评论列表
     */
    private void loadComments() {
        Log.d(TAG, "加载评论列表，msgId: " + msgId);
        
        apiService.getCommentList(msgId,
                data -> {
                    swipeRefreshLayout.setRefreshing(false);

                    if (data != null && data.getData() != null && data.getData().size() > 0) {
                        List<CommentModel> allComments = data.getData();
                        Log.d(TAG, "获取到评论数量: " + allComments.size());

                        // 分离主评论和回复
                        List<CommentModel> mainComments = new ArrayList<>();
                        List<CommentModel> replyComments = new ArrayList<>();
                        for (CommentModel comment : allComments) {
                            if (comment.getParentId() == null || comment.getParentId() == 0) {
                                mainComments.add(comment);
                            } else {
                                replyComments.add(comment);
                            }
                        }
                        
                        Log.d(TAG, "主评论数量: " + mainComments.size() + ", 回复数量: " + replyComments.size());
                        
                        adapter.setCommentsAndAllComments(mainComments, allComments);

                        // 显示/隐藏空状态
                        if (mainComments.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.w(TAG, "评论数据为空");
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                },
            new RetrofitApiService.ErrorCallback() {
                @Override
                public void onError(String error) {
                    swipeRefreshLayout.setRefreshing(false);
                    Log.e(TAG, "加载评论失败: " + error);
                    Toast.makeText(CommentListActivity.this, "加载评论失败: " + error, Toast.LENGTH_SHORT).show();
                    
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }
        );
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
                        loadComments();
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
                        loadComments();
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