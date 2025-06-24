package com.damors.zuji.model;

import java.util.List;

import lombok.Data;

@Data
public class PageTrandsMsgModel {
    /**
     * 记录列表
     */
    private List<TrandsMsgModel> records;

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
}
