package org.example.atuo_attend_backend.quote.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 将报价单/合同 HTML 转为 PDF（OpenHTMLToPDF）与 Word（POI + Jsoup 结构化转换）。
 * 中文 PDF 需嵌入支持 CJK 的 TTF/OTF：默认使用 classpath:/fonts/NotoSansCJKsc-Regular.otf（随包分发）；
 * 亦可配置 quote.export.cjk-font-path，或回退到系统字体（微软雅黑、黑体等）。
 */
@Service
public class QuoteDocumentExportService {

    private static final Logger log = LoggerFactory.getLogger(QuoteDocumentExportService.class);

    private final QuoteService quoteService;
    private final ResourceLoader resourceLoader;

    @Value("${quote.export.cjk-font-path:}")
    private String configuredCjkFontPath;

    public QuoteDocumentExportService(QuoteService quoteService, ResourceLoader resourceLoader) {
        this.quoteService = quoteService;
        this.resourceLoader = resourceLoader;
    }

    public byte[] exportQuotePdf(long projectId, Long quoteResultId) throws Exception {
        Map<String, Object> doc = quoteService.buildQuoteDocument(projectId, quoteResultId);
        long rid = ((Number) doc.get("quoteResultId")).longValue();
        String html = doc.get("html").toString();
        byte[] pdf = htmlToPdf(html);
        quoteService.persistBinaryDocument(rid, "quote_pdf", pdf);
        return pdf;
    }

    public byte[] exportQuoteDocx(long projectId, Long quoteResultId) throws Exception {
        Map<String, Object> doc = quoteService.buildQuoteDocument(projectId, quoteResultId);
        long rid = ((Number) doc.get("quoteResultId")).longValue();
        String html = doc.get("html").toString();
        byte[] docx = htmlToDocx(html);
        quoteService.persistBinaryDocument(rid, "quote_docx", docx);
        return docx;
    }

    public byte[] exportContractPdf(long quoteResultId) throws Exception {
        String html = quoteService.exportContractHtml(quoteResultId).get("html").toString();
        byte[] pdf = htmlToPdf(html);
        quoteService.persistBinaryDocument(quoteResultId, "contract_pdf", pdf);
        return pdf;
    }

    public byte[] exportContractDocx(long quoteResultId) throws Exception {
        String html = quoteService.exportContractHtml(quoteResultId).get("html").toString();
        byte[] docx = htmlToDocx(html);
        quoteService.persistBinaryDocument(quoteResultId, "contract_docx", docx);
        return docx;
    }

    public byte[] htmlToPdf(String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        if (!registerCjkFonts(builder)) {
            throw new IllegalStateException(
                    "未加载任何中文字体，PDF 将无法正确显示中文（会变为 # 等占位符）。"
                            + " 请将 NotoSansCJKsc-Regular.otf 置于 classpath:fonts/，或配置 quote.export.cjk-font-path 指向 .ttf/.otf，"
                            + " 或在服务器安装中文字体。详见 resources/fonts/README.md。");
        }
        // OpenHTMLToPDF 使用 XML 解析器：<meta>、<br> 等须为良构 XHTML
        builder.withHtmlContent(toWellFormedXhtmlForPdf(html), null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
    }

    /**
     * 将 HTML 规范为 OpenHTMLToPDF 可解析的 XHTML（自闭合 void 标签等）。
     */
    private static String toWellFormedXhtmlForPdf(String html) {
        if (html == null || html.isBlank()) {
            return "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/></head><body></body></html>";
        }
        Document doc = Jsoup.parse(html);
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .prettyPrint(false);
        return doc.html();
    }

    public byte[] htmlToDocx(String html) throws Exception {
        Document doc = Jsoup.parse(html);
        Element body = doc.body();
        if (body == null) {
            throw new IllegalArgumentException("HTML 无 body");
        }
        try (XWPFDocument xdoc = new XWPFDocument()) {
            for (Element child : body.children()) {
                appendElement(xdoc, child);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            xdoc.write(bos);
            return bos.toByteArray();
        }
    }

    private void appendElement(XWPFDocument xdoc, Element el) {
        String tag = el.tagName().toLowerCase();
        switch (tag) {
            case "h1" -> addHeading(xdoc, el.text(), 40);
            case "h2" -> addHeading(xdoc, el.text(), 32);
            case "h3" -> addHeading(xdoc, el.text(), 28);
            case "p" -> addParagraphFromElement(xdoc, el);
            case "table" -> addTable(xdoc, el);
            case "pre" -> addPreformatted(xdoc, el.text());
            case "ul", "ol" -> {
                for (Element li : el.children()) {
                    if ("li".equalsIgnoreCase(li.tagName())) {
                        XWPFParagraph p = xdoc.createParagraph();
                        XWPFRun r = p.createRun();
                        r.setText("• " + li.text());
                        applyEastAsiaFont(r);
                    }
                }
            }
            default -> {
                if (el.children().isEmpty() && StringUtils.hasText(el.text())) {
                    addPlainParagraph(xdoc, el.text());
                } else {
                    for (Element c : el.children()) {
                        appendElement(xdoc, c);
                    }
                }
            }
        }
    }

    /** halfPoints：POI 字号为半磅，如 20 磅 = 40 */
    private void addHeading(XWPFDocument xdoc, String text, int halfPoints) {
        if (!StringUtils.hasText(text)) return;
        XWPFParagraph p = xdoc.createParagraph();
        XWPFRun r = p.createRun();
        r.setBold(true);
        r.setFontSize(halfPoints);
        r.setText(text.trim());
        applyEastAsiaFont(r);
    }

    private void addParagraphFromElement(XWPFDocument xdoc, Element pEl) {
        XWPFParagraph p = xdoc.createParagraph();
        // 简单处理 strong
        for (org.jsoup.nodes.Node node : pEl.childNodes()) {
            if (node instanceof org.jsoup.nodes.TextNode tn) {
                String t = tn.getWholeText();
                if (StringUtils.hasText(t)) {
                    XWPFRun r = p.createRun();
                    r.setText(t);
                    applyEastAsiaFont(r);
                }
            } else if (node instanceof Element ce) {
                if ("strong".equalsIgnoreCase(ce.tagName()) || "b".equalsIgnoreCase(ce.tagName())) {
                    XWPFRun r = p.createRun();
                    r.setBold(true);
                    r.setText(ce.text());
                    applyEastAsiaFont(r);
                } else {
                    XWPFRun r = p.createRun();
                    r.setText(ce.text());
                    applyEastAsiaFont(r);
                }
            }
        }
        if (p.getRuns().isEmpty() && StringUtils.hasText(pEl.text())) {
            XWPFRun r = p.createRun();
            r.setText(pEl.text());
            applyEastAsiaFont(r);
        }
    }

    private void addPlainParagraph(XWPFDocument xdoc, String text) {
        XWPFParagraph p = xdoc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text.trim());
        applyEastAsiaFont(r);
    }

    private void addPreformatted(XWPFDocument xdoc, String text) {
        if (text == null) return;
        for (String line : text.split("\n")) {
            XWPFParagraph p = xdoc.createParagraph();
            XWPFRun r = p.createRun();
            r.setFontFamily("Consolas");
            r.setText(line);
            applyEastAsiaFont(r);
        }
    }

    private void addTable(XWPFDocument xdoc, Element tableEl) {
        Elements rows = tableEl.select(">tbody>tr,>tr");
        if (rows.isEmpty()) {
            rows = tableEl.select("tr");
        }
        if (rows.isEmpty()) return;
        int colCount = 0;
        for (org.jsoup.nodes.Element row : rows) {
            int n = row.select("th,td").size();
            colCount = Math.max(colCount, n);
        }
        if (colCount == 0) return;
        XWPFTable table = xdoc.createTable(rows.size(), colCount);
        for (int i = 0; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("th,td");
            XWPFTableRow tr = table.getRow(i);
            for (int j = 0; j < colCount; j++) {
                XWPFTableCell cell = tr.getCell(j);
                for (int k = cell.getParagraphs().size() - 1; k >= 0; k--) {
                    cell.removeParagraph(k);
                }
                XWPFParagraph cp = cell.addParagraph();
                XWPFRun cr = cp.createRun();
                String cellText = j < cells.size() ? cells.get(j).text() : "";
                cr.setText(cellText);
                if (cells.size() > j && "th".equalsIgnoreCase(cells.get(j).tagName())) {
                    cr.setBold(true);
                }
                applyEastAsiaFont(cr);
            }
        }
    }

    private static void applyEastAsiaFont(XWPFRun run) {
        run.setFontFamily("微软雅黑");
    }

    /**
     * @return 是否至少注册了一款字体
     */
    private boolean registerCjkFonts(PdfRendererBuilder builder) {
        List<File> fontFiles = resolveCjkFontFiles();
        if (fontFiles.isEmpty()) {
            log.error("未找到中文字体文件，PDF 中文将显示为 # 或方框。请配置字体，见 resources/fonts/README.md。");
            return false;
        }
        File primary = fontFiles.get(0);
        String[] families = {
                "sans-serif",
                "Microsoft YaHei",
                "Noto Sans SC",
                "Noto Sans CJK SC",
                "SimHei",
                "SimSun",
                "serif"
        };
        for (String family : families) {
            builder.useFont(primary, family);
        }
        for (int i = 1; i < fontFiles.size(); i++) {
            builder.useFont(fontFiles.get(i), "sans-serif");
        }
        return true;
    }

    private List<File> resolveCjkFontFiles() {
        List<File> out = new ArrayList<>();
        try {
            if (StringUtils.hasText(configuredCjkFontPath)) {
                Path p = Path.of(configuredCjkFontPath.trim());
                if (Files.isRegularFile(p) && isFontFile(p.toString())) {
                    out.add(p.toFile());
                    return out;
                }
            }
        } catch (Exception e) {
            log.debug("配置字体路径无效: {}", configuredCjkFontPath, e);
        }

        // 优先随包字体：noto-cjk 现用名 NotoSansCJKsc-Regular.otf；旧文档中的 NotoSansSC-Regular.otf 仍尝试兼容
        String[] classpathFontNames = {
                "fonts/NotoSansCJKsc-Regular.otf",
                "fonts/NotoSansSC-Regular.otf"
        };
        for (String name : classpathFontNames) {
            File fromCp = tryLoadClasspathFontFile("classpath:" + name, name);
            if (fromCp != null) {
                out.add(fromCp);
                return out;
            }
        }

        String windir = System.getenv("WINDIR");
        if (StringUtils.hasText(windir)) {
            Path fontsDir = Path.of(windir, "Fonts");
            // 中文版 Windows 常见：微软雅黑；部分环境无 simhei.ttf
            String[] winRel = {
                    "msyh.ttc",
                    "msyh.ttf",
                    "msyhbd.ttc",
                    "msyhl.ttc",
                    "simhei.ttf",
                    "simsun.ttc",
                    "simsunb.ttf",
                    "mingliub.ttc"
            };
            for (String rel : winRel) {
                Path p = fontsDir.resolve(rel);
                if (Files.isRegularFile(p)) {
                    out.add(p.toFile());
                    return out;
                }
            }
        }

        if (StringUtils.hasText(System.getenv("HOME"))) {
            String[] macFonts = {
                    System.getenv("HOME") + "/Library/Fonts/NotoSansCJKsc-Regular.otf",
                    "/Library/Fonts/Microsoft/msyh.ttc",
                    "/System/Library/Fonts/PingFang.ttc",
                    "/System/Library/Fonts/STHeiti Light.ttc",
                    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf"
            };
            for (String path : macFonts) {
                if (!StringUtils.hasText(path)) continue;
                Path p = Path.of(path);
                if (Files.isRegularFile(p)) {
                    out.add(p.toFile());
                    return out;
                }
            }
        }

        String[] linuxFonts = {
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJKsc-Regular.otf",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
                "/usr/share/fonts/truetype/arphic/uming.ttc"
        };
        for (String path : linuxFonts) {
            Path p = Path.of(path);
            if (Files.isRegularFile(p)) {
                out.add(p.toFile());
                return out;
            }
        }

        return out;
    }

    /**
     * 从 classpath 加载字体：开发态可用 getFile；jar 内复制到临时文件。
     */
    private File tryLoadClasspathFontFile(String classpathLocation, String debugName) {
        try {
            Resource res = resourceLoader.getResource(classpathLocation);
            if (!res.exists()) {
                return null;
            }
            try {
                File f = res.getFile();
                if (f.isFile()) {
                    return f;
                }
            } catch (Exception ignored) {
                // jar 内无实体文件
            }
            try (InputStream in = res.getInputStream()) {
                return copyClasspathFontToTemp(in, debugName.replace('/', '_'));
            }
        } catch (Exception e) {
            log.trace("classpath 字体不可用: {}", debugName, e);
            return null;
        }
    }

    private static File copyClasspathFontToTemp(InputStream in, String name) {
        try (InputStream stream = in) {
            File f = File.createTempFile("quote-font-", "-" + name.replace('/', '_'));
            f.deleteOnExit();
            Files.copy(stream, f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isFontFile(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc");
    }
}
