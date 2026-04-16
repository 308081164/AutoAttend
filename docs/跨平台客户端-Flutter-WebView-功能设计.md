# 跨平台客户端（Flutter + WebView 套壳）— 功能设计文档

## 1. 文档说明

| 项 | 内容 |
|----|------|
| **功能名称** | 流帮 Project 官方桌面与移动客户端（壳应用） |
| **版本** | v0.1（设计草案） |
| **目标** | 在保留现有 Web 版本的前提下，提供 **Windows / Linux / Android / iOS** 安装包，以 **Flutter 宿主 + 系统 WebView** 加载官网/租户控制台，统一分发、版本管理与强制下线能力。 |
| **依赖** | 现有 Web 前端与后端 API；GitHub Releases 作为构建产物托管；平台监测台扩展「客户端版本策略」。 |

---

## 2. 可行性结论（Flutter + WebView）

### 2.1 是否可行

**可以。** 技术路线成熟：

| 能力 | 说明 |
|------|------|
| **Flutter 多平台** | 单一代码库产出 Windows（含 MSIX 可选）、Linux（deb/AppImage 等）、Android、iOS/iPadOS。 |
| **WebView 嵌入** | 推荐使用 [`webview_flutter`](https://pub.dev/packages/webview_flutter)（Android/iOS 系统 WebView）；桌面端使用 [`webview_flutter_web`](https://pub.dev/packages/webview_flutter_web) 或社区 **`webview_windows` / `webview_flutter_linux`** 等面向各桌面平台的实现（需在实现阶段选型并验证 Chromium 嵌入稳定性）。 |
| **与「纯网页」关系** | 壳应用本质是**带品牌、自动更新、深度链接、文件/相机等扩展能力**的浏览器容器；核心业务逻辑仍在服务端与 Web 前端，**不替代** Web 版本。 |

### 2.2 利弊简述

| 优势 | 劣势与风险 |
|------|------------|
| 用户从官网一键下载，体验接近原生应用 | WebView 与系统浏览器内核差异可能导致少量 CSS/JS 兼容问题，需回归测试 |
| 统一版本号、更新与强制下线策略 | iOS 若仅企业内部分发需 Apple 开发者计划；公网 App Store 需审核周期 |
| 构建可放 GitHub Actions，避免本地 Gradle 环境 | 首次配置 iOS/Android 签名与密钥需安全流程（Secrets） |
| 平板优先可优化横屏与触控区域 | Linux 打包格式多样，需明确优先格式（如 deb） |

### 2.3 与「纯 Flutter 重写 UI」对比

| 方案 | 适用场景 |
|------|----------|
| **WebView 套壳（本文）** | 快速覆盖四端、迭代跟随 Web；适合当前「以 Web 为主产品」的阶段。 |
| **Flutter 原生 UI** | 长期独立体验与离线能力；成本高，建议单独立项。 |

---

## 3. 产品范围与端侧优先级

### 3.1 支持平台

| 平台 | 优先级 | 说明 |
|------|--------|------|
| **Android 平板** | P0 | 触控与横屏为主；与手机共用 APK/AAB，布局以响应式 Web 为主，壳层可做全屏、返回键处理。 |
| **iPadOS** | P0 | 同上；需关注安全区与分屏。 |
| **Android 手机 / iPhone** | P1 | 可用，体验次于平板。 |
| **Windows** | P1 | 办公场景常见；安装包建议 `.exe` 或 **MSIX**（自动更新友好）。 |
| **Linux** | P2 | 开发者/运维；优先 **deb** 或 **AppImage**（择一为主，减少 CI 矩阵膨胀）。 |

### 3.2 壳应用职责边界

- **必须**：加载可配置 **Base URL**（默认生产官网域名）、展示加载/错误页、处理外链（可选系统浏览器打开）、**版本上报与更新检查**、**被服务端拒绝时的友好提示**（强制下线版本）。
- **可选后续**：文件选择上传增强、推送通知、生物识别解锁、多窗口（桌面）。

---

## 4. 分发与官网入口

### 4.1 GitHub Releases 作为唯一官方下载源

- **构建产物**：每个 Release 附带各平台资产，命名建议统一前缀，例如：  
  `LiubangProject-Windows-x64-{version}.exe`  
  `LiubangProject-Linux-x64-{version}.deb`  
  `LiubangProject-Android-{version}.apk` 或 `.aab`  
  `LiubangProject-iOS-{version}.ipa`（若走 TestFlight/App Store 则官网链到商店说明页）
- **官网首页**：提供「下载客户端」区块，**按钮直链到对应 GitHub Release 资产 URL**（或 Release 页面锚点），避免二次搬运文件。
- **版本展示**：页面上展示**当前推荐版本号**与更新时间（可从 GitHub API 或自建 `latest.json` 读取，见 §6）。

### 4.2 国内访问说明

- GitHub Releases 直连在部分网络环境下较慢或不可用；中长期可增加 **国内镜像桶（OSS + CDN）** 作为可选下载源，与本设计不冲突（同一版本号与校验和）。

---

## 5. 版本号与构建策略（GitHub 云端）

### 5.1 版本语义

建议采用 **语义化版本** `MAJOR.MINOR.PATCH`（与 Flutter `pubspec.yaml` 中 `version: x.y.z+build` 对齐）：

- **MAJOR**：不兼容的壳行为或 API 契约变更。
- **MINOR**：新功能（如新 WebView 策略、新平台包）。
- **PATCH**：缺陷修复。

Android/iOS 的 **build number**（`+` 后整数）在 CI 中**每次成功构建自动 +1**，满足商店与更新比对需求。

### 5.2 「有更新时才构建」

在 **GitHub Actions** 中采用**条件触发**，避免空转与 Gradle 下载浪费：

| 策略 | 说明 |
|------|------|
| **路径过滤** | 仅当 `client/flutter/**`（或实际壳工程路径）、`client/**/pubspec.yaml`、`.github/workflows/client-*.yml` 变更时触发 `workflow_dispatch` 或 `push` 构建。 |
| **手动发版** | 发版时打 **Git tag**（如 `app-v1.2.3`），工作流仅在 tag 匹配时执行完整四端或分平台矩阵构建。 |
| **定时** | 不建议定时全量构建；可选每周一次「依赖安全更新」检查构建。 |

### 5.3 CI 矩阵建议

- **阶段一**：Android（APK）+ Windows；Linux deb；不阻塞发版。
- **阶段二**：iOS（需 macOS runner + Apple 证书 Secrets）；上架策略另附运维说明。

### 5.4 产物与校验

- 每个 Release 附带 **`checksums.txt`（SHA256）** 与可选 **`latest.json`**（见 §6），供客户端与官网展示。

---

## 6. 客户端自动更新

### 6.1 流程概述

```
客户端启动 / 定时 / 用户手动检查
    → GET 后端「版本检查」接口（携带当前 version + build + platform）
    → 若服务端声明有更新：展示更新说明与下载链接（GitHub Release 直链）
    → 用户确认后：系统浏览器或应用内下载安装包（各平台安装方式不同，桌面可提示用户运行安装程序）
```

### 6.2 `latest.json`（建议静态托管）

可由 CI 在发 Release 时生成并上传至 **GitHub Release 附件** 或 **后端静态目录**，示例：

```json
{
  "minSupportedVersion": "1.0.0",
  "latestVersion": "1.2.0",
  "latestBuild": 42,
  "releaseNotes": "修复登录稳定性；改进平板横屏。",
  "assets": {
    "windows": "https://github.com/org/repo/releases/download/app-v1.2.0/app-windows-x64.exe",
    "linux_deb": "https://github.com/...",
    "android_apk": "https://github.com/..."
  },
  "releasedAt": "2026-04-20T08:00:00Z"
}
```

客户端优先拉取 **HTTPS** 地址；若失败可降级读取 GitHub raw（需注意国内网络）。

### 6.3 与后端接口的关系

- **轻量方案**：客户端只读 `latest.json` + 本地版本比较，**不调用**业务后端。
- **推荐方案（支持强制下线）**：启动时调用 **`GET /api/client/version-check`**（或监测台专用域名），请求头或 Query 携带：

  - `X-Client-Version`：`1.2.0`
  - `X-Client-Build`：`42`
  - `X-Client-Platform`：`android | ios | windows | linux`

  响应见 §7。

---

## 7. 强制废弃版本（监测台联动）

### 7.1 业务目标

当某客户端版本存在**安全漏洞**或**协议不兼容**时，平台管理员可在 **监测台** 将该版本标记为 **禁止连接**，壳应用在调用任意业务 API 前根据策略**拦截并提示升级**。

### 7.2 服务端策略模型（设计）

| 字段 | 说明 |
|------|------|
| `min_supported_version` | 全局最低可连接版本（低于则拒绝）。 |
| `blocked_versions` | 显式黑名单列表（如 `["1.0.0"]` 或按 build 号）。 |
| `block_message` | 用户可见文案（引导下载链接）。 |
| `download_url` | 推荐升级包链接（通常 GitHub Release）。 |

存储位置建议：

- **平台级**（`tenant_id = 0`）配置表，如扩展现有 `aa_system_config` 或新建 `aa_client_version_policy`；由监测台 CRUD。

### 7.3 后端行为

1. **网关或全局过滤器**：对带 `X-Client-Version` 的请求（可选：仅 `/api/**`）解析版本；若命中黑名单或低于 `min_supported_version`，返回 **HTTP 426** 或 **403**，Body 统一 JSON：  
   `{ "code": 4xxxx, "message": "...", "upgradeUrl": "..." }`
2. **纯 Web 浏览器**：无壳版本头时**不拦截**，避免误伤。
3. **壳应用**：所有请求由 Flutter 注入上述 Header（或登录后绑定设备令牌，二期）。

### 7.4 壳应用侧

- 收到 403/426 且 body 含 `upgradeUrl` 时：**模态框阻塞操作**，仅允许「前往下载」或「退出」。
- 启动时先调 **`/api/client/version-check`**（轻量、可匿名或仅会话），提前提示，减少用户操作一半才失败。

---

## 8. 安全与合规

- **下载完整性**：官网与客户端展示 SHA256；高级用户可自行校验。
- **HTTPS**：Base URL 强制 HTTPS；证书固定（pinning）为可选增强。
- **密钥**：Apple/Google 签名证书仅存 GitHub Encrypted Secrets；勿入库。
- **隐私**：壳应用隐私政策需说明 WebView 加载域名与可能采集的设备信息（版本号、平台）。

---

## 9. 非目标（本期不做）

- 应用内完整应用商店式更新（iOS 仍须遵守 Apple 规则）。
- 离线可用核心业务。
- 替换现有 Web 部署为主入口。

---

## 10. 实施阶段建议

| 阶段 | 内容 |
|------|------|
| **M0** | 新建 `client/flutter` 工程；Android + Windows 本地可跑通 WebView 打开官网。 |
| **M1** | GitHub Actions：tag 触发、版本号自动递增、Release 上传 APK + Windows。 |
| **M2** | 官网下载区 + `latest.json`；客户端检查更新 UI。 |
| **M3** | 后端 `version-check` + 监测台策略 + 网关拦截。 |
| **M4** | Linux deb；iOS/TestFlight（资源到位后）。 |

---

## 11. 修订记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-04-20 | v0.1 | 初稿：Flutter+WebView 可行性、四端分发、GitHub CI、版本策略、更新与强制下线。 |

---

*本文档描述产品设计，具体包名、仓库路径与工作流文件名以实现阶段为准。*
