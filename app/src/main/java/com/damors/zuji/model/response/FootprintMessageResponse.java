package com.damors.zuji.model.response;

import com.damors.zuji.model.FootprintMessage;
import java.util.List;

/**
 * 足迹动态列表响应数据模型
 */
public class FootprintMessageResponse {
    
    /**
     * 分页数据
     */
    public static class Data {
        /**
         * 记录列表
         */
        private List<FootprintMessage> records;
        
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
        
        // 构造函数
        public Data() {
        }
        
        // Getter和Setter方法
        public List<FootprintMessage> getRecords() {
            return records;
        }
        
        public void setRecords(List<FootprintMessage> records) {
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
        
        @Override
        public String toString() {
            return "Data{" +
                    "records=" + records +
                    ", total=" + total +
                    ", current=" + current +
                    ", size=" + size +
                    ", pages=" + pages +
                    '}';
        }
    }
}