package com.dawn.upgrade;

/**
 * 默认实现的校验逻辑基类
 * @author dawn
 */
public abstract class DefaultUpgradeObserver implements UpgradeObserver {
    /** 版本名称 */
    protected String mVersionName = null;
    /** 版本比较数字 */
    protected int mVersionCode = 0;
    /** 版本来源（渠道号） */
    protected String mChannel = null;
    /** 检查版本更新的地址 */
    protected String mCheckUrl = null;
    /** 是否显示升级提示对话框 */
    protected boolean mShowDialog = true;
    /**
     * 构造方法
     * @param versionName 版本名称 如 V1.0.0， V2.2.8 或者其它字符串
     * @param versionCode 版本比较的数字 如 1,2
     * @param channel 来源（渠道号）
     * @param checkUrl 检查版本更新的地址
     * @param showDialog 是否显示升级提示对话框
     */
    public DefaultUpgradeObserver(String versionName, int versionCode, String channel, String checkUrl, boolean showDialog) {
        this.mVersionName = versionName;
        this.mVersionCode = versionCode;
        this.mChannel = channel;
        this.mCheckUrl = checkUrl;
        this.mShowDialog = showDialog;
    }
}