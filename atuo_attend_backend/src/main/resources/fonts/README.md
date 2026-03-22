# 报价单 / 合同 PDF 中文字体

OpenHTMLToPDF **不会**使用操作系统里的「字体名」自动找字库，必须在生成 PDF 时 **嵌入** 支持中文的 **TTF/OTF**（或部分环境下的 TTC）。若未嵌入，中文会显示为 **`#`**、方框或空白。

## 默认（推荐）

本仓库在 `fonts/` 下随包提供 **`NotoSansCJKsc-Regular.otf`**（SIL OFL，来源 [notofonts/noto-cjk](https://github.com/notofonts/noto-cjk)）。打包进 `jar` 后由 `QuoteDocumentExportService` 自动加载，**无需额外配置**。

若你克隆后该文件缺失（例如未提交大文件），可手动下载到本目录：

```bash
curl -L -o NotoSansCJKsc-Regular.otf "https://raw.githubusercontent.com/notofonts/noto-cjk/main/Sans/OTF/SimplifiedChinese/NotoSansCJKsc-Regular.otf"
```

（PowerShell 可用 `curl.exe` 同上。）

## 其他方式

1. **配置绝对路径**：`application.properties` 中设置 `quote.export.cjk-font-path=/path/to/YourFont.ttf`（或 `.otf`）。
2. **Windows**：若未使用 classpath 字体，会依次尝试 `%WINDIR%\Fonts\` 下的 `msyh.ttc`、`simhei.ttf`、`simsun.ttc` 等。
3. **Linux**：会尝试常见路径下的 Noto CJK、文泉驿等（见 `QuoteDocumentExportService`）。

## 历史说明

早期文档中的 **`NotoSansSC-Regular.otf`** 文件名与 Google Noto CJK 仓库现行命名不一致；代码仍会尝试加载该文件名以兼容旧部署。
