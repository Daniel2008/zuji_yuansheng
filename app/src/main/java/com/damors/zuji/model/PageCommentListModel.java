package com.damors.zuji.model;

import java.util.List;

/**
 * 评论列表响应数据模型
 * 用于处理带分页的评论数据
 */
public class PageCommentListModel {
    /**
     * 评论记录列表
     */
    private List<CommentModel> records;

    /**
     * 总记录数
     */
    private int total;

    /**
     * 当前页码
     */
    private int current;

    /**
     * 每页大小
     */
    private int size;

    /**
     * 总页数
     */
    private int pages;

    // Getter和Setter方法
    public List<CommentModel> getRecords() {
        return records;
    }

    public void setRecords(List<CommentModel> records) {
        this.records = records;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    /**
     * 是否还有更多数据
     * @return true表示还有更多数据
     */
    public boolean hasMore() {
        return current < pages;
    }

}