# 报价单 / 合同 PDF 中文字体（可选）

OpenHTMLToPDF 需要本地 **TTF/OTF/TTC** 才能正确渲染中文。

任选其一：

1. 将 **Noto Sans SC** 常规体复制为：`NotoSansSC-Regular.otf`，放在本目录（与 `README.md` 同级），重新打包运行。
2. 或在 `application.properties` / 环境变量中设置 `quote.export.cjk-font-path` 指向任意 `.ttf` / `.otf` 文件。
3. **Windows**：若未放置字体，会尝试使用 `%WINDIR%\Fonts\simhei.ttf` 或 `simsun.ttc`。
4. **Linux**：会尝试常见路径下的 Noto CJK / 文泉驿 等（`.ttc` 部分环境可用）。

字体可自 [Google Noto](https://fonts.google.com/noto/specimen/Noto+Sans+SC) 等渠道获取。
