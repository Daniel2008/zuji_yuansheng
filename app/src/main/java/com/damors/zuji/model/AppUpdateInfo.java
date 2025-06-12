package com.damors.zuji.model;

/**
 * 应用更新信息模型类
 * 用于表示从服务器获取的应用版本更新信息
 */
public class AppUpdateInfo {
    private int versionCode;        // 版本号
    private String versionName;     // 版本名称
    private String downloadUrl;     // 下载链接
    private String updateContent;   // 更新内容描述
    private boolean forceUpdate;    // 是否强制更新
    private long fileSize;          // 文件大小（字节）
    private String md5;             // 文件MD5校验值
    private String releaseTime;     // 发布时间
    
    /**
     * 默认构造函数
     */
    public AppUpdateInfo() {
    }
    
    /**
     * 带参数的构造函数
     * 
     * @param versionCode 版本号
     * @param versionName 版本名称
     * @param downloadUrl 下载链接
     * @param updateContent 更新内容
     * @param forceUpdate 是否强制更新
     */
    public AppUpdateInfo(int versionCode, String versionName, String downloadUrl, 
                        String updateContent, boolean forceUpdate) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.downloadUrl = downloadUrl;
        this.updateContent = updateContent;
        this.forceUpdate = forceUpdate;
    }
    
    // Getter和Setter方法
    
    /**
     * 获取版本号
     * @return 版本号
     */
    public int getVersionCode() {
        return versionCode;
    }
    
    /**
     * 设置版本号
     * @param versionCode 版本号
     */
    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }
    
    /**
     * 获取版本名称
     * @return 版本名称
     */
    public String getVersionName() {
        return versionName;
    }
    
    /**
     * 设置版本名称
     * @param versionName 版本名称
     */
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }
    
    /**
     * 获取下载链接
     * @return 下载链接
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }
    
    /**
     * 设置下载链接
     * @param downloadUrl 下载链接
     */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
    
    /**
     * 获取更新内容描述
     * @return 更新内容描述
     */
    public String getUpdateContent() {
        return updateContent;
    }
    
    /**
     * 设置更新内容描述
     * @param updateContent 更新内容描述
     */
    public void setUpdateContent(String updateContent) {
        this.updateContent = updateContent;
    }
    
    /**
     * 是否强制更新
     * @return true表示强制更新，false表示可选更新
     */
    public boolean isForceUpdate() {
        return forceUpdate;
    }
    
    /**
     * 设置是否强制更新
     * @param forceUpdate true表示强制更新，false表示可选更新
     */
    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }
    
    /**
     * 获取文件大小
     * @return 文件大小（字节）
     */
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     * 设置文件大小
     * @param fileSize 文件大小（字节）
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    /**
     * 获取文件MD5校验值
     * @return MD5校验值
     */
    public String getMd5() {
        return md5;
    }
    
    /**
     * 设置文件MD5校验值
     * @param md5 MD5校验值
     */
    public void setMd5(String md5) {
        this.md5 = md5;
    }
    
    /**
     * 获取发布时间
     * @return 发布时间
     */
    public String getReleaseTime() {
        return releaseTime;
    }
    
    /**
     * 设置发布时间
     * @param releaseTime 发布时间
     */
    public void setReleaseTime(String releaseTime) {
        this.releaseTime = releaseTime;
    }
    
    /**
     * 获取格式化的文件大小字符串
     * @return 格式化的文件大小（如：10.5MB）
     */
    public String getFormattedFileSize() {
        if (fileSize <= 0) {
            return "未知大小";
        }
        
        final long KB = 1024;
        final long MB = KB * 1024;
        final long GB = MB * 1024;
        
        if (fileSize >= GB) {
            return String.format("%.1fGB", (double) fileSize / GB);
        } else if (fileSize >= MB) {
            return String.format("%.1fMB", (double) fileSize / MB);
        } else if (fileSize >= KB) {
            return String.format("%.1fKB", (double) fileSize / KB);
        } else {
            return fileSize + "B";
        }
    }
    
    @Override
    public String toString() {
        return "AppUpdateInfo{" +
                "versionCode=" + versionCode +
                ", versionName='" + versionName + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", updateContent='" + updateContent + '\'' +
                ", forceUpdate=" + forceUpdate +
                ", fileSize=" + fileSize +
                ", md5='" + md5 + '\'' +
                ", releaseTime='" + releaseTime + '\'' +
                '}';
    }
}