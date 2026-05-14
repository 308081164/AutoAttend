# 报价单 / 合同 PDF 中文字体

OpenHTMLToPDF **不会**使用操作系统里的「字体名」自动找字库，必须在生成 PDF 时 **嵌入** 支持中文的 **TTF/OTF**（或部分环境下的 **TTC**）。若未嵌入，中文会显示为 **`#`**、方框或乱码。

## 默认（推荐，已自动化）

- **`mvn package` / `mvn compile`** 的 **`generate-resources`** 阶段会执行 `scripts/fetch-noto-cjk-font.sh`，将 **NotoSansCJKsc-Regular.otf**（SIL OFL，[notofonts/noto-cjk](https://github.com/notofonts/noto-cjk)）下载到 `target/generated-resources/fonts/`，并由 Maven 合并进 **`classpath:fonts/`**，打进 jar，**无需手动放文件到 `src/main/resources/fonts/`**。
- 需要 **curl** 或 **wget**（GitHub Actions、本仓库 `Dockerfile` 构建阶段已安装 curl）。
- 离线构建：在 `pom.xml` 的 properties 里将 **`quote.skip.font.download`** 设为 **`true`**，并自行通过 **`quote.export.cjk-font-path`** 指定本机字体，或预先准备好 `target/generated-resources/fonts/NotoSansCJKsc-Regular.otf` 后再执行 `package`。

## Docker 运行时

后端运行镜像已安装 **`fonts-noto-cjk`**，作为 **classpath 字体之外的系统补充**（与 jar 内嵌字体双保险，利于缺字回退）。

## 其他方式

1. **配置绝对路径**：`application.properties` 中设置 `quote.export.cjk-font-path=/path/to/YourFont.ttf`（或 `.otf`），优先级高于 classpath。
2. **Windows**：若未使用 classpath 字体，会依次尝试 `%WINDIR%\Fonts\` 下的 `msyh.ttc`、`simhei.ttf`、`simsun.ttc` 等。
3. **Linux**：会尝试常见路径下的 Noto CJK、文泉驿等（见 `QuoteDocumentExportService`）。

## 历史说明

早期文档中的 **`NotoSansSC-Regular.otf`** 文件名与 Google Noto CJK 仓库现行命名不一致；代码仍会尝试加载该文件名以兼容旧部署。
