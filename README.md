# QQPrivateNotify · QQ通知过滤模块

这是一个LSP模块 使用 **libxposed API 101/102 构建**

## 当前功能

- 打开或关闭私聊消息通知
- 打开或关闭群聊消息通知
- 设置保存在模块自身 SharedPreferences，覆盖安装会保留

## 构建

在项目目录运行：

```powershell
.\build.cmd
```

Debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。仓库不包含 Android SDK、APK、日志或本地配置  

## LSPosed 配置
**仅支持API101+版本**  
在 LSPosed 中启用模块并只勾选 `com.tencent.mobileqq`  
强行停止作用域应用再重新启动即生效  

## 适配

仅在安卓16 QQ版本9.3.50测试正常 其余版本请自行测试 （理论大部分版本都兼容）  

## 注意事项

请确保进程 `com.tencent.mobileqq:MSF`一直保持在后台  
请确保QQ主进程 `com.tencent.mobileqq`在后台运行  
 ***后台墓碑是否影响请自行测试**  

## 部分问题

### 如遇到时不时漏出几条群消息（偶尔几条）
-请确保进程`com.tencent.mobileqq:MSF`没被其他内存管理模块/程序清理 清理后的短暂几秒内会使群聊消息拦截短暂失效  
-将此进程加入模块白名单即可  

### 如遇大量群消息漏出（模块失效）
-请确保QQ主进程没有被其他第三方模块/程序清理杀后台


