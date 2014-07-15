DUpdate
=======
功能介绍：

DUpdate是一个针对android平台的升级模块，可以满足大部分应用的升级需求，包括检查更新，校验版本，下载升级包并提示安装，强制升级等

整体逻辑就是DUpdate向后台发一个请求，后台返回一个json字段，包括了新版本的特性，然后DUpdate根据访问策略来解析这个字符串，来决定是否升级


如何使用：

1、在UpgradeConfig中配置相关参数，并且要和apk升级检查的后台确定返回的json字段，json字段的解析在ClientCheckObserver中，可以根据需要修改


2、在相关检查逻辑中加入代码

        mUpdate = new UpdateUtils(this);
        mUpdate.checkVersion();
        
3、取消检查代码

        mUpdate.cancel();


如果你有意见或建议，请给我发邮件dawnkuo.hk@gmail.com

android交流群：369553261
