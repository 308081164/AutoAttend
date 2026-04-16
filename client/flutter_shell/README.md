# 流帮 Project 壳应用（Flutter + WebView）

- **源码**：`lib/main.dart` 使用系统 WebView 加载 Web 控制台，首屏 URL 通过 `--dart-define=APP_BASE_URL=https://你的域名` 注入。
- **版本号**：`pubspec.yaml` 的 `version: x.y.z+build`；GitHub Actions 在打 `app-v*` tag 时用运行号覆盖 `+build`。
- **客户端身份**：首屏在 query 中附加 `clientVersion`、`clientBuild`、`clientPlatform`；Web 应用 `main.js` 写入 sessionStorage 并在后续 API 请求中带上 `X-Client-*` 头，供服务端版本策略校验。
- **CI**：`.github/workflows/flutter-shell-ci.yml`（analyze/test）；发版见 `flutter-shell-release.yml`（需仓库 Variables：`APP_SHELL_BASE_URL` 可选）。
- **监测台**：「系统配置」中维护最低版本、黑名单与各端 GitHub Release 直链；官网首页从 `GET /api/public/client/downloads` 读取展示。

本地调试（需本机安装 Flutter）：

```bash
cd client/flutter_shell
flutter pub get
flutter run -d chrome --dart-define=APP_BASE_URL=https://你的测试域名
```
