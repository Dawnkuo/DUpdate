package com.faintdawn.update;

import java.io.IOException;

import android.app.AlertDialog;
import android.content.Context;

/**
 * 升级校验回调
 * @author dawn
 */
public interface UpdateObserver {
    /** 不需要升级 */
    int RESULT_NO = 0;
    /** 需要普通升级 */
    int RESULT_YES = 1;
    /** 需要强制升级 */
    int RESULT_FORCE = 2;
    /** 用户取消 */
    int RESULT_CANCEL = 3;
    /** 异常：未知异常 */
    int RESULT_ERROR_UNKNOW = -1;
    /** 异常：网络下载失败 */
    int RESULT_ERROR_NET = -2;
    /** 异常：数据格式错误 */
    int RESULT_ERROR_DATA = -3;
    /**
     * 创建升级检查提示窗口
     */
    public AlertDialog makeLoadingDialog(Context context);
    /**
     * 发送给服务器的版本数据
     */
    public String makeLoaclVersionInfo();
    /**
     * 发送请求给服务端
     * @param localVersionInfo 本地版本信息
     */
    public String sendRequest(Context context, String url, String localVersionInfo) throws IOException;
    /**
     * 取消升级检验
     */
    public void cancel();
    /**
     * 校验版本逻辑
     * @param versionString 服务端版本信息
     * @return 校验版本逻辑
     */
    public CheckResult doCheck(String versionString);
    /**
     * 出错回调
     * @param context 上下文
     * @param errorCode 错误代码
     */
    public void onError(Context context, int errorCode);
    /**
     * 返回结果
     * @param context 上下文
     * @param result 检查结果
     */
    public void onResult(Context context, CheckResult result);
    /**
     * 用户取消的操作
     * @param context 上下文
     */
    public void onCancel(Context context);
}