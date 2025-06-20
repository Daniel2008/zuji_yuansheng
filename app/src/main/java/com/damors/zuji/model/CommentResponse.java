package com.damors.zuji.model;

import java.util.List;

/**
 * 评论列表响应数据模型
 */
public class CommentResponse {
    private int code;
    private String msg;
    private Data data;
    
    public CommentResponse() {}
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMsg() {
        return msg;
    }
    
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public Data getData() {
        return data;
    }
    
    public void setData(Data data) {
        this.data = data;
    }
    
    /**
     * 评论列表数据
     */
    public static class Data {
        private List<CommentModel> records;
        private int total;
        private int size;
        private int current;
        private int pages;
        
        public Data() {}
        
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
        
        public int getSize() {
            return size;
        }
        
        public void setSize(int size) {
            this.size = size;
        }
        
        public int getCurrent() {
            return current;
        }
        
        public void setCurrent(int current) {
            this.current = current;
        }
        
        public int getPages() {
            return pages;
        }
        
        public void setPages(int pages) {
            this.pages = pages;
        }
        
        @Override
        public String toString() {
            return "Data{" +
                    "records=" + (records != null ? records.size() : 0) + " comments" +
                    ", total=" + total +
                    ", size=" + size +
                    ", current=" + current +
                    ", pages=" + pages +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "CommentResponse{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}