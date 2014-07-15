package com.dawn.update;

import java.io.IOException;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;

/**
 * 升级检查线程
 * @author dawn
 */
public class UpdateThread extends Thread {
    /** 上下文 */
    private Context mContext = null;
    /** 升级校验回调 */
    private UpdateObserver mObserver = null;
    /** loading对话框 */
    private AlertDialog mLoadingDialog = null;
    /** 升级检查url */
    private String mCheckUrl = null;
    
    /**
     * 检查升级线程构造方法
     * @param context 上下文
     * @param observer 过程观察者
     * @param showDialog 是否显示提示对话框
     * @param loadingDialog long对话框
     */
    public UpdateThread(Context context, UpdateObserver observer, AlertDialog loadingDialog, String checkUrl) {
        mContext = context;
        mObserver = observer;
        mLoadingDialog = loadingDialog;
        mCheckUrl = checkUrl;
    }
    
    /**
     * 取消更新
     */
    public void cancel() {
        try {
            if (mObserver != null) {
                mObserver.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        super.run();
        try {
            CheckResult ret = mObserver.doCheck(mObserver.sendRequest(mContext, mCheckUrl, mObserver.makeLoaclVersionInfo()));
            switch(ret.getCheckResult()) {
                case UpdateObserver.RESULT_YES:
                case UpdateObserver.RESULT_NO:
                    mObserver.onResult(mContext, ret);
                    break;
                case UpdateObserver.RESULT_CANCEL:
                    mObserver.onCancel(mContext);
                    break;
                default:
                    mObserver.onError(mContext, ret.getCheckResult());
                    break;
            }
        } catch (IOException e) {
            mObserver.onError(mContext, UpdateObserver.RESULT_ERROR_NET);
            e.printStackTrace();
        } catch (Exception e) {
            mObserver.onError(mContext, UpdateObserver.RESULT_ERROR_UNKNOW);
            e.printStackTrace();
        }

        if (mLoadingDialog != null) {
            Handler handler = new Handler(mContext.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                            mLoadingDialog.dismiss();
                            mLoadingDialog = null;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }
}