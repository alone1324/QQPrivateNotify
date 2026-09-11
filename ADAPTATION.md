# v1.1 适配说明

本模块使用 libxposed API 101/102，目标为 Android QQ 9.3.20 及以上版本。

## 作用域

LSPosed 中只启用 com.tencent.mobileqq 作用域。模块不需要 Android System 作用域，也不注册通知监听服务。

## 工作方式

- 在 QQNT 通知生成入口识别私聊和群聊类型。
- 在 QQ 进程内的 Android NotificationManager 发送入口再次检查。
- 设置页提供私聊、群聊两个独立开关。
- 配置保存在模块 SharedPreferences，覆盖安装会保留。

## 构建

在项目目录运行：

`powershell
.\build.cmd
`

Debug APK 输出到 pp/build/outputs/apk/debug/app-debug.apk。
