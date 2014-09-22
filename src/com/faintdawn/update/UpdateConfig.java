package com.faintdawn.update;

public class UpdateConfig {
    /** 当前校验模式，默认为客户端校验 */
    public static int sCurrentMode = UpdateUtils.MODE_CLIENT;
    /** 升级地址 */
    public static String sApkCheckUrl = ""; // TODO 这里配置apk更新检查地址
    /** apk名称 */
    public static String sApkDownloadName = "my_download.apk"; // TODO 这里配置apk下载文件名称
}
