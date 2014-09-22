package com.faintdawn.update;

/**
 * 检查结果对象
 * @author dawn
 */
public class CheckResult {
    /** 检查结果 */
    private int mCheckResult = 0;
    /** 升级url */
    private String mUrl = null;
    /** 包大小 */
    private long mApkSize = 0;
    /** 强制升级版本 */
    private int mForceVersionCode = 0;
    /** 版本号 */
    private int mVersionCode = 1;
    /** 版本名称 */
    private String mVersionName = null;
    /** 版本描述 */
    private String mVersionInfo = null;

    /**
     * 获取检查结果
     * @return 检查结果
     */
    public int getCheckResult() {
        return mCheckResult;
    }

    /**
     * 设置检查结果
     * @param checkResult 检查结果
     */
    public void setCheckResult(int checkResult) {
        this.mCheckResult = checkResult;
    }

    /**
     * 获取下载url
     * @return 下载url
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * 设置下载url
     * @param url 下载url
     */
    public void setUrl(String url) {
        this.mUrl = url;
    }

    /**
     * 获取apk大小
     * @return apk大小
     */
    public long getApkSize() {
        return mApkSize;
    }

    /**
     * 设置apk大小
     * @param apkSize apk大小
     */
    public void setApkSize(long apkSize) {
        this.mApkSize = apkSize;
    }

    /**
     * 是否需要强制升级
     * @return 是否需要强制升级
     */
    public boolean isForceUpdate(int currentVersion) {
        return mForceVersionCode > currentVersion;
    }

    /**
     * 设置强制升级最低版本
     * @param version 强制升级最低版本
     */
    public void setForceVersion(int version) {
        this.mForceVersionCode = version;
    }
    
    /**
     * 获取版本code
     * @return 版本code
     */
    public int getVersionCode() {
        return mVersionCode;
    }

    /**
     * 设置版本code
     * @param versionCode 版本code
     */
    public void setVersionCode(int versionCode) {
        this.mVersionCode = versionCode;
    }

    /**
     * 获取版本名字
     * @return 版本名字
     */
    public String getVersionName() {
        return mVersionName;
    }

    /**
     * 设置版本名字
     * @param versionName 版本名字
     */
    public void setVersionName(String versionName) {
        this.mVersionName = versionName;
    }

    /**
     * 获取版本信息
     * @return 版本信息
     */
    public String getVersionInfo() {
        return mVersionInfo;
    }

    /**
     * 设置版本信息
     * @param versionInfo 版本信息
     */
    public void setVersionInfo(String versionInfo) {
        this.mVersionInfo = versionInfo;
    }

}