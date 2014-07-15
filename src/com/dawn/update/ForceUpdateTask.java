package com.dawn.update;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.dawn.updatedemo.R;

/**
 * 强制升级异步task
 * @author dawn
 */
public class ForceUpdateTask extends AsyncTask<String, Long, String> {
    /** 是否取消标记位 */
    private volatile boolean isCanceled = false;
    /** 上下文 */
    private Context mContext = null;
    /** 安装包大小 */
    private long mTotalBytes = 0;
    /** 进度框 */
    private ProgressDialog mProgressDialog = null;
    
    /**
     * 默认构造方法
     * @param context 上下文
     */
    public ForceUpdateTask(Context context) {
        mContext = context;
    }
    
    /**
     * 是否是data下的存储路径
     * @param path 当前目录
     * @return 当前目录是否是data下的目录
     */
    private boolean isFilesDir(String path) {
        return !TextUtils.isEmpty(path) && path.contains(mContext.getFilesDir().getAbsolutePath());
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(100);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle(R.string.update_download_new_app);
//        mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            
//            @Override
//            public void onDismiss(DialogInterface dialog) {
//                ForceUpgradeTask.this.onCancelled();
//            }
//        });
        mProgressDialog.show();
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected String doInBackground(String... params) {
        String apkUrl = params[0];
        String apkSavePath = params[1];
        File apkSaveFile = null;
        
        FileOutputStream fileOutputStream = null;
        BufferedInputStream bufferedInput = null;
        BufferedOutputStream bufferedOutput = null;
        HttpEntity httpEntity = null;
        try {
            apkSaveFile = new File(apkSavePath);
            if (apkSaveFile.exists()) {
                apkSaveFile.delete();
            }

            publishProgress(0l, 1l);
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(apkUrl);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            
            bufferedInput = new BufferedInputStream(inputStream);
            
            if (isFilesDir(apkSavePath)) {
                bufferedOutput = new BufferedOutputStream(mContext.openFileOutput(UpdateUtils.getApkDownloadName(), Context.MODE_WORLD_READABLE));
            } else {
                fileOutputStream = new FileOutputStream(apkSaveFile);
                bufferedOutput = new BufferedOutputStream(fileOutputStream);
            }
            mTotalBytes = httpEntity.getContentLength();
            publishProgress(0l, mTotalBytes);

            // 缓冲数组
            byte[] buff = new byte[4096];
            int len = 0;
            long downLoadBytes = 0;
            while (!isCanceled && (len = bufferedInput.read(buff)) != -1) {
                bufferedOutput.write(buff, 0, len);
                downLoadBytes += len;
                publishProgress(downLoadBytes, mTotalBytes);
            }
            // 刷新此缓冲的输出流
            bufferedOutput.flush();

            // 如果取消了，删除安装文件
            if (isCanceled) {
                if (apkSaveFile.exists()) {
                    apkSaveFile.delete();
                }
            }
            return apkSavePath;
        } catch (Exception e) {
            e.printStackTrace();

            // 如果出现异常，删除安装文件
            if (apkSaveFile != null && apkSaveFile.exists()) {
                apkSaveFile.delete();
            }
        } finally {

            if (bufferedOutput != null) {
                try {
                    bufferedOutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            if (bufferedInput != null) {
                try {
                    bufferedInput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            if (httpEntity != null) {
                try {
                    httpEntity.consumeContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "";
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        if (isCanceled) {
            return;
        }

        long downLoadByes = values[0];
        long totalBytes = values[1];
        int pencent = 0;
        if (totalBytes > 0) {
            pencent = (int) (downLoadByes * 100 / totalBytes);
            mProgressDialog.setProgress(pencent);
        } else {
            mProgressDialog.setProgress(0);
        }
        
    }

    @Override
    protected void onPostExecute(String apkPath) {
        File file = new File(apkPath);
        if (isCanceled) {
            UpdateUtils.exitApp(mContext);
        } else {
            if (!TextUtils.isEmpty(apkPath) && file.exists()) {
                UpdateUtils.sendInstallIntent(mContext, file);
                UpdateUtils.exitApp(mContext);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(R.string.update_alert_title);
                builder.setCancelable(false);
                builder.setMessage(mContext.getString(R.string.update_alert_error));
                builder.setPositiveButton(R.string.update_alert_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UpdateUtils.exitApp(mContext);
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
        
        mProgressDialog.dismiss();
    }

    @Override
    protected void onCancelled() {
        isCanceled = true;
    }
}
