package com.dawn.upgrade;

import java.io.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

/**
 * 更新模块
 * @author dawn
 */
public class UpgradeUtils {
    /** TAG */
    private static final String TAG = "UpgradeUtils";
    /** 客户端校验模式 */
    public static final int MODE_CLIENT = 0;
    /** 服务端校验模式 */
    public static final int MODE_SERVER = 1;
    /** 上下文 */
    private Context mContext = null;
    /** apk来源（渠道号） */
    private String mChannel = null;
    /** 是否显示升级提示对话框 */
    private boolean mShowDialog = true;
    /** 升级检查过程中的observer */
    private UpgradeObserver mUpgradeObserver = null;
    /** 升级线程 */
    private UpgradeThread mUpgradeThread = null;
    
    /**
     * 构造方法
     * @param context 上下文
     * @param context
     */
    public UpgradeUtils(Context context) {
        this.mContext = context;
    }
    
    /**
     * 构造方法
     * @param context 上下文
     * @param channel apk来源（渠道号），不关心可填空
     * @param showDialog 是否显示升级提示对话框
     * @param upgradeObserver 检查过程中的observer，可为空，使用默认实现
     */
    public UpgradeUtils(Context context, String channel, boolean showDialog, UpgradeObserver upgradeObserver) {
        this.mContext = context;
        this.mChannel = channel;
        this.mShowDialog = showDialog;
        this.mUpgradeObserver = upgradeObserver;
    }

    /**
     * 设置更新校验方式
     * @param checkMode MODE_CLIENT 或者 MODE_SERVER
     */
    public void setCheckMode(int checkMode) {
        switch (checkMode) {
            case MODE_CLIENT:
            case MODE_SERVER:
                break;
            default:
                throw new IllegalArgumentException(TAG + ":set mode error");
        }
        UpgradeConfig.sCurrentMode = checkMode;
    }
    
    /**
     * 设置升级地址
     * @param url 服务器地址
     */
    public void setServerUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException(TAG + ":url is empty");
        }
        UpgradeConfig.sApkCheckUrl = url;
    }
    
    /**
     * 设置下载的apk文件名
     * @param apkName apk文件名
     */
    public void setApkDownloadName(String apkName) {
        if (TextUtils.isEmpty(apkName)) {
            return;
        }
        UpgradeConfig.sApkDownloadName = apkName;
    }
    
    /**
     * 获取下载的apk文件名
     * @return
     */
    public static String getApkDownloadName() {
        return UpgradeConfig.sApkDownloadName;
    }
    
    /**
     * 检查升级
     */
    public void checkVersion() {
        // 1，获取本地版本信息
        String versionName = null;
        int versionCode = 0;
        try {
            PackageManager packageManager = mContext.getPackageManager();
            // getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        // 2，生成一个观察者对象
        UpgradeObserver observer = mUpgradeObserver;
        if (null == observer) { // 如果用户没有自己实现，就使用默认实现的
            if (MODE_CLIENT == UpgradeConfig.sCurrentMode) {
                observer = new ClientCheckObserver(mContext, versionName, versionCode, mChannel, UpgradeConfig.sApkCheckUrl, mShowDialog);
            } else if (MODE_SERVER == UpgradeConfig.sCurrentMode) {
                observer = new ServerCheckObserver(mContext, versionName, versionCode, mChannel, UpgradeConfig.sApkCheckUrl, mShowDialog);
            } else {
                throw new IllegalArgumentException(TAG + " : make UpgradeObserver error");
            }
        }
        
        // 3，是否弹出等待框
        AlertDialog dialog = null;
        if (mShowDialog) {
            dialog = observer.makeLoadingDialog(mContext);
            if (dialog != null) {
                dialog.show();
            }
        }
        
        // 4，去服务器拉取更新信息
        mUpgradeThread = new UpgradeThread(mContext, observer, dialog, UpgradeConfig.sApkCheckUrl);
        mUpgradeThread.start();
    }
    
    /**
     * 取消检查更新操作
     */
    public void cancel() {
        try {
            if (mUpgradeThread != null) {
                mUpgradeThread.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    /**
     * 退出app
     */
    public static void exitApp(Context context) {
        Handler handler = new Handler(context.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    System.exit(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }, 500); // 这里时间间隔可以修改
    }
    
    /**
     * 生成一个安装intent
     * @param file 安装apk
     * @return 安装intent
     */
    public static void sendInstallIntent(Context context, File file) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
