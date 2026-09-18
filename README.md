# rtp2httpd-app

基于 [stackia/rtp2httpd](https://github.com/stackia/rtp2httpd) 的安卓手机 App 壳，自用不上架。

App 用一个 WebView 打开 rtp2httpd 内置的 Web 播放器页面（`http://<server>:5140/player`），首次启动需在「设置」里填入 rtp2httpd 服务器地址。

## 功能

- WebView 包装 rtp2httpd 内置 Web 播放器
- 支持全屏视频播放（HLS 等）
- 支持文件下载交给系统处理
- 设置页保存服务器地址（持久化）
- 自定义 User-Agent：`Rtp2HttpdApp/1.0`
- 使用 rtp2httpd 项目原生图标（紫色渐变 + 白色播放列表/三角 glyph）

## 技术规格

- 包名：`com.rtp2httpd.app`
- minSdk：24（Android 7.0）
- targetSdk：34（Android 14）
- 构建工具：Gradle 8.9 + AGP 8.7.3 + Kotlin 1.9.24
- 图标：取自 rtp2httpd 项目 `icon_1024.png`，经 `tools/generate_icons.ps1` 生成各密度 mipmap 与 adaptive icon drawable

## 使用方法

1. 在 [Releases](../../releases/latest) 页面下载最新 APK
2. 手机开启「允许安装未知来源应用」后安装
3. 打开 App，首次进入会提示「尚未配置服务器地址」
4. 点击「去设置」，填入 rtp2httpd 服务器地址
   - 示例：`http://192.168.1.100:5140`（rtp2httpd 默认端口为 5140）
5. 返回主界面，App 会自动加载 rtp2httpd 的 Web 播放器
6. 可在右上角菜单点击「刷新」或「服务器设置」

## 在线构建

本项目通过 GitHub Actions 在线构建 APK，无需本地部署 Android 开发环境。

每次推送到 `main` 分支或手动触发 workflow，都会：
1. 拉取代码
2. 安装 JDK 17 + Android SDK
3. 用 Gradle 构建 release APK
4. 用持久化在 cache 中的 keystore 签名（保证每次签名一致，可正常升级安装）
5. 上传 APK 到 GitHub Release

## 本地生成图标（可选）

如需重新生成图标，在 Windows PowerShell 执行：

```powershell
# 把 rtp2httpd 项目的 icon_1024.png 放到项目根目录
powershell -ExecutionPolicy Bypass -File tools\generate_icons.ps1
```

## 致谢

- 原项目：[stackia/rtp2httpd](https://github.com/stackia/rtp2httpd) — IPTV 流媒体转发服务器
- 图标版权归 rtp2httpd 项目所有（GPL-2.0）
