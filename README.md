# QQ 私信通知过滤 LSP 模块

适配 Android QQ 9.3.20+，使用 libxposed API 101/102。模块提供私聊和群聊两个独立通知开关。

## 当前功能

- 打开或关闭私聊消息通知。
- 打开或关闭群聊消息通知。
- QQNT 通知生成和 QQ 进程内 Android 通知发送拦截。
- 设置保存在模块自身 SharedPreferences，覆盖安装会保留。

## 构建

在项目目录运行：

```powershell
.\build.cmd
```

Debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。仓库不包含 Android SDK、APK、日志或本地配置。

## LSPosed 配置

在 LSPosed 中启用模块并只勾选 `com.tencent.mobileqq`。
