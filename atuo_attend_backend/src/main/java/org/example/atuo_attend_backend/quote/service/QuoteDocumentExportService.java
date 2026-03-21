package org.example.atuo_attend_backend.quote.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

/**
 * 将报价单/合同 HTML 转为 PDF（OpenHTMLToPDF）与 Word（POI + Jsoup 结构化转换）。
 * 中文 PDF 需可读的 TTF/OTF：classpath:/fonts/、配置项 quote.export.cjk-font-path，或 Windows 下 simhei.ttf。
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
        String html = quoteService.buildQuoteDocument(projectId, quoteResultId).get("html").toString();
        return htmlToPdf(html);
    }

    public byte[] exportQuoteDocx(long projectId, Long quoteResultId) throws Exception {
        String html = quoteService.buildQuoteDocument(projectId, quoteResultId).get("html").toString();
        return htmlToDocx(html);
    }

    public byte[] exportContractPdf(long quoteResultId) throws Exception {
        String html = quoteService.exportContractHtml(quoteResultId).get("html").toString();
        return htmlToPdf(html);
    }

    public byte[] exportContractDocx(long quoteResultId) throws Exception {
        String html = quoteService.exportContractHtml(quoteResultId).get("html").toString();
        return htmlToDocx(html);
    }

    public byte[] htmlToPdf(String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        registerCjkFonts(builder);
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
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

    private void registerCjkFonts(PdfRendererBuilder builder) {
        List<File> fontFiles = resolveCjkFontFiles();
        if (fontFiles.isEmpty()) {
            log.warn("未找到中文字体文件，PDF 中文可能显示为方框。请将 NotoSansSC-Regular.otf 放到 classpath:/fonts/ 或配置 quote.export.cjk-font-path（.ttf/.otf）。");
            return;
        }
        File primary = fontFiles.get(0);
        String[] families = {"sans-serif", "Microsoft YaHei", "Noto Sans SC", "SimHei", "SimSun", "serif"};
        for (String family : families) {
            builder.useFont(primary, family);
        }
        for (int i = 1; i < fontFiles.size(); i++) {
            builder.useFont(fontFiles.get(i), "sans-serif");
        }
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

        try {
            Resource res = resourceLoader.getResource("classpath:fonts/NotoSansSC-Regular.otf");
            if (res.exists()) {
                File f = res.getFile();
                if (f.isFile()) {
                    out.add(f);
                    return out;
                }
            }
        } catch (Exception ignored) {
            // jar 内资源 getFile 可能失败，见下复制到临时文件
        }

        // classpath 在 jar 内：用 InputStream 复制到临时文件（OpenHTMLToPDF 需要 File）
        try {
            Resource res = resourceLoader.getResource("classpath:fonts/NotoSansSC-Regular.otf");
            if (res.exists()) {
                File tmp = copyClasspathFontToTemp(res.getInputStream(), "NotoSansSC-Regular.otf");
                if (tmp != null) {
                    out.add(tmp);
                    return out;
                }
            }
        } catch (Exception e) {
            log.trace("classpath fonts/NotoSansSC-Regular.otf 不可用", e);
        }

        String windir = System.getenv("WINDIR");
        if (StringUtils.hasText(windir)) {
            Path simhei = Path.of(windir, "Fonts", "simhei.ttf");
            if (Files.isRegularFile(simhei)) {
                out.add(simhei.toFile());
                return out;
            }
            Path simsun = Path.of(windir, "Fonts", "simsun.ttc");
            if (Files.isRegularFile(simsun)) {
                try {
                    out.add(simsun.toFile());
                    return out;
                } catch (Exception ignored) {
                }
            }
        }

        String[] linuxFonts = {
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
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
