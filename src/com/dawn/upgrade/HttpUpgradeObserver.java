package com.dawn.upgrade;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.dawn.upgradedemo.R;

/**
 * Http方式访问服务器的默认实现
 * @author dawn
 */
public abstract class HttpUpgradeObserver extends DefaultUpgradeObserver {
    /** tag */
    private static final String TAG = "HttpUpgradeObserver";
    /** 下载id标记 */
    private static final String DL_ID = "downloadId";
    /** 上下文 */
    protected Context mContext = null;
    /** http请求参数 */
    protected String mParams = null;
    /** 是否取消标记位 */
    protected volatile boolean mCancel = false;
    /** DownloadManager */
    private DownloadManager mDownloadManager = null;
    /** SharedPreferences */
    private SharedPreferences mSharedPreferences = null;  
    /** DownloadManager状态监听器 */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {   
        @Override   
        public void onReceive(Context context, Intent intent) {   
            queryDownloadStatus();   
        }   
    };
    
    /**
     * 构造方法
     * @param context 上下文
     * @param versionName 版本名称 如 V1.0.0， V2.2.8 或者其它字符串
     * @param versionCode 版本比较的数字 如 1,2
     * @param channel 来源（渠道号）
     * @param checkUrl 检查版本更新的地址
     * @param showDialog 是否显示升级提示对话框
     */
    public HttpUpgradeObserver(Context context, String versionName, int versionCode, String channel, String checkUrl, boolean showDialog) {
        super(versionName, versionCode, channel, checkUrl, showDialog);
        mContext = context;
        mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }
    
    /**
     * 设置http请求参数
     * @param params http请求参数
     */
    public void setParams(String params) {
        mParams = params;
    }
    
    @Override
    public AlertDialog makeLoadingDialog(Context context) {
        ProgressDialog progressDlg = new ProgressDialog(context);
        progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDlg.setMessage(context.getString(R.string.upgrade_please_wait));
        return progressDlg;
    }
    
    @Override
    public String makeLoaclVersionInfo() {
        // 为什么在客户端校验也要传这些东西？ 传给服务端做数据统计用
        StringBuilder sb = new StringBuilder();
        sb.append("{\"vn\"=").append("\"").append(mVersionName).append("\",")
          .append("\"vc\"=").append("\"").append(mVersionCode).append("\",")
          .append("\"c\"=").append("\"").append(mChannel).append("\"}");
        return sb.toString();
    }
    
    @Override
    public String sendRequest(Context context, String url, String localVersionInfo) throws IOException {
        String result = null;
        if (mCancel) {
            result = "cancel";
            return result;
        }
        
        //result = "{\"vc\":5, \"url\":\"http://down.myapp.com/myapp/qqteam/AndroidQQ/qq_4.7.2.2185_android.apk\",\"fv\":5}";
        
        HttpURLConnection connection = getHttpConnection(mCheckUrl, isWiFiActive(context));

        connection.setDoInput(true);// 允许输入
        connection.setDoOutput(true);// 允许输出
        connection.setUseCaches(false);// 不使用Cache
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Charset", "UTF-8");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.connect();
        DataOutputStream bufOutPut = new DataOutputStream(connection.getOutputStream());
        if (TextUtils.isEmpty(mParams)) {
            mParams = "";
        }
        bufOutPut.write(mParams.getBytes());
        bufOutPut.flush();

        // int retCode = connection.getResponseCode();
        result = StreamToString(connection.getInputStream());

        bufOutPut.close();
        connection.disconnect();

        return result;
    }
    
    @Override
    public void cancel() {
        mCancel = true;
        mContext.unregisterReceiver(mReceiver);
    }
    
    @Override
    public void onError(final Context context, int result) {
        Handler handler = new Handler(context.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, R.string.upgrade_alert_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onResult(final Context context, final CheckResult result) {
        if (RESULT_NO == result.getCheckResult()) {
            Handler handler = new Handler(context.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mCancel) {
                        onCancel(context);
                        return;
                    }
                    AlertDialog dialog = new AlertDialog.Builder(context)
                                         .setTitle(R.string.upgrade_alert_title)
                                         .setMessage(R.string.upgrade_alert_not_need)
                                         .setPositiveButton(R.string.upgrade_alert_ok, null)
                                         .create();
                    dialog.show();
                }
            });
        } else if (RESULT_YES == result.getCheckResult()) {
            Handler handler = new Handler(context.getMainLooper());
            int infoId = 0;
            if (result.isForceUpgrade(mVersionCode)) {
                infoId = R.string.upgrade_force_info;
            } else {
                infoId = R.string.upgrade_info;
            }
            final String info = context.getString(infoId, result.getVersionName(), result.getVersionInfo());
            final String apkPath = getApkPath(context) + UpgradeUtils.getApkDownloadName();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mCancel) {
                        onCancel(context);
                        return;
                    }
                    AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.upgrade_alert_title)
                    .setMessage(info)
                    .setPositiveButton(R.string.upgrade_alert_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (result.isForceUpgrade(mVersionCode)) {
                                forceDownload(context, apkPath, result.getUrl());
                            } else {
                                normalDownload(apkPath, result.getUrl());
                            }
                        }
                    })
                    .setNegativeButton(R.string.upgrade_alert_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (result.isForceUpgrade(mVersionCode)) {
                                UpgradeUtils.exitApp(context);
                            }
                        }
                    })
                    .create();
                    dialog.show();
                }
            });
        }
    }
    
    @Override
    public void onCancel(final Context context) {
        Log.e(TAG, "onCancel");
    }
    
    /**
     * 强制升级
     * @param context 上下文
     * @param apkPath apk下载路径
     * @param url apk下载地址
     */
    private void forceDownload(final Context context, final String apkPath, final String url) {
        (new ForceUpgradeTask(context)).execute(url, apkPath);
    }
    
    /**
     * 普通升级，交给download manager
     * @param apkPath apk下载路径
     * @param url apk下载地址
     */
    private void normalDownload(final String apkPath, final String url) {
        File apkSaveFile = new File(apkPath);
        if (apkSaveFile.exists()) {
            apkSaveFile.delete();
        }
        mDownloadManager.remove(mSharedPreferences.getLong(DL_ID, 0));   
        mSharedPreferences.edit().clear().commit();
        //开始下载   
        Uri resource = Uri.parse(encodeGB(url));   
        DownloadManager.Request request = new DownloadManager.Request(resource);   
        request.setAllowedNetworkTypes(Request.NETWORK_MOBILE | Request.NETWORK_WIFI);   
        request.setAllowedOverRoaming(false);   
        //设置文件类型  
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();  
        String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));  
        request.setMimeType(mimeString);  
        //在通知栏中显示   
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);  
        request.setVisibleInDownloadsUi(true);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        //sdcard的目录下的download文件夹  
        request.setDestinationInExternalPublicDir("", UpgradeUtils.getApkDownloadName());
        request.setTitle(mContext.getString(R.string.app_name));   
        long id = mDownloadManager.enqueue(request);   
        //保存id   
        mSharedPreferences.edit().putLong(DL_ID, id).commit();   
        mContext.registerReceiver(mReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
    
    /**
     * 把输入流转化为字符串
     * @param inputStream 输入流
     * @return 字符串
     */
    private String StreamToString(InputStream inputStream) {
        if (mCancel) {
            return "cancel";
        }
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuffer sb = new StringBuffer(1024);
            String tmpLine = null;
            while ((tmpLine = bufferedReader.readLine()) != null) {
                sb.append(tmpLine);
            }
            if (mCancel) {
                return "cancel";
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 检查是否wifi网络
     * @param context 上下文
     * @return 是否wifi网络
     */
    public static boolean isWiFiActive(Context context) {
        ConnectivityManager connectManager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo info = connectManager.getActiveNetworkInfo();
        if (info != null) {
            String netType = info.getTypeName();
            if ("WIFI".equalsIgnoreCase(netType)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取网络连接,默认get方式
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    private static HttpURLConnection getHttpConnection(String urlString, boolean isWifi) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = null;

        if (isWifi) {
            // wifi网络
            connection = (HttpURLConnection) url.openConnection();
            // connection.setRequestProperty("Host", url.getHost());
        } else {
            String defProxyHost = android.net.Proxy.getDefaultHost();
            int defPort = android.net.Proxy.getDefaultPort();

            if (defProxyHost != null && defPort != -1) {
                // 使用代理,一般是移动的wap网络
                /*
                 * java.net.Proxy proxy = new java.net.Proxy(
                 * java.net.Proxy.Type.HTTP, new InetSocketAddress(
                 * defProxyHost, defPort)); connection = (HttpURLConnection)
                 * url.openConnection(proxy);
                 */
                connection = (HttpURLConnection) url.openConnection();
                // connection.setRequestProperty("X-Online-Host",
                // url.getHost());
                // connection.addRequestProperty("Host", "10.0.0.172");
            } else {
                // net网络
                connection = (HttpURLConnection) url.openConnection();
                // connection.addRequestProperty("Host", url.getHost());
            }
        }

        // 先设为10秒，可以根据需要改
        connection.setConnectTimeout(10 * 1000);
        connection.setReadTimeout(10 * 1000);

        return connection;
    }
    
    /**
     * 获取下载安装文件的目录
     * @param context 上下文
     * @return 安装文件的目录
     */
    private String getApkPath(Context context) {
        String externalPath = "";
        if (isSDCardAviliable()) {
            externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            return externalPath + File.separator;
        } else {
            return context.getFilesDir() + File.separator;
        }
    }
    
    /** 
     * sd卡是否可用
     */
    private boolean isSDCardAviliable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
    
    /**
     * 查询apk下载状态
     */
    private void queryDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(mSharedPreferences.getLong(DL_ID, 0));
        Cursor c = mDownloadManager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_PAUSED:
                    Log.e(TAG, "STATUS_PAUSED");
                    break;
                case DownloadManager.STATUS_PENDING:
                    Log.e(TAG, "STATUS_PENDING");
                    break;
                case DownloadManager.STATUS_RUNNING:
                    Log.e(TAG, "STATUS_RUNNING");
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    Log.e(TAG, "STATUS_SUCCESSFUL");
                    try {
                        String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        File apkSaveFile = new File(new URI(uri));
                        UpgradeUtils.sendInstallIntent(mContext, apkSaveFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case DownloadManager.STATUS_FAILED:
                    Log.e(TAG, "STATUS_FAILED");
                    deleteFile();
                    break;
            }
        }
    }
    
    /**
     * 删除文件
     */
    private void deleteFile() {
        try {
            mDownloadManager.remove(mSharedPreferences.getLong(DL_ID, 0));
            mSharedPreferences.edit().clear().commit();
            String apkPath = getApkPath(mContext);
            File apkSaveFile = new File(apkPath + UpgradeUtils.getApkDownloadName());
            if (apkSaveFile.exists()) {
                apkSaveFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /** 
     * 如果服务器不支持中文路径的情况下需要转换url的编码。 
     * @param url url
     * @return 转码后的url
     */  
    private String encodeGB(String url)  
    {  
        //转换中文编码  
        String split[] = url.split("/");  
        for (int i = 1; i < split.length; i++) {  
            try {  
                split[i] = URLEncoder.encode(split[i], "GB2312");  
            } catch (UnsupportedEncodingException e) {  
                e.printStackTrace();  
            }  
            split[0] = split[0]+"/"+split[i];  
        }  
        split[0] = split[0].replaceAll("\\+", "%20");//处理空格  
        return split[0];  
    }  
    
}