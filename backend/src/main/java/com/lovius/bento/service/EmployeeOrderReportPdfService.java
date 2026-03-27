package com.lovius.bento.service;

import com.lovius.bento.model.EmployeeOrderReportRow;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmployeeOrderReportPdfService {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeOrderReportPdfService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String SAMPLE_TEXT = "員工訂餐報表查詢區間排序方式部門員工姓名便當名稱廠商";
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float SCALE = 2F;
    private static final float TITLE_FONT_SIZE = 18F;
    private static final float BODY_FONT_SIZE = 10.5F;
    private static final float LINE_HEIGHT = 18F;
    private static final float START_X = 36F;
    private static final float START_Y = 800F;
    private static final float PAGE_BOTTOM = 50F;
    private static final float TABLE_HEADER_Y = 746F;
    private static final float FIRST_ROW_Y = 722F;
    private static final int ROWS_PER_PAGE = (int) ((FIRST_ROW_Y - PAGE_BOTTOM) / LINE_HEIGHT) + 1;
    private final String configuredFontPath;

    public EmployeeOrderReportPdfService(@Value("${app.pdf.font-path:}") String configuredFontPath) {
        this.configuredFontPath = configuredFontPath == null ? "" : configuredFontPath.trim();
    }

    public byte[] generatePdf(
            LocalDate dateFrom,
            LocalDate dateTo,
            String sortBy,
            List<EmployeeOrderReportRow> rows) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Font baseFont = loadFont();
            if (rows.isEmpty()) {
                addPage(document, renderPage(baseFont, dateFrom, dateTo, sortBy, List.of()));
            } else {
                for (int index = 0; index < rows.size(); index += ROWS_PER_PAGE) {
                    int endIndex = Math.min(index + ROWS_PER_PAGE, rows.size());
                    addPage(document, renderPage(baseFont, dateFrom, dateTo, sortBy, rows.subList(index, endIndex)));
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException | FontFormatException exception) {
            throw new IllegalStateException("無法產生員工訂餐報表 PDF", exception);
        }
    }

    private Font loadFont() throws IOException, FontFormatException {
        List<String> candidates = resolveFontCandidates();
        List<String> failedCandidates = new ArrayList<>();
        for (String candidate : candidates) {
            File fontFile = new File(candidate);
            if (!fontFile.exists()) {
                continue;
            }
            try {
                Font font = loadFontFromFile(fontFile);
                logger.info("A013 PDF font file selected: {} ({})", candidate, font.getFontName());
                return font;
            } catch (IOException | FontFormatException exception) {
                failedCandidates.add(candidate + " (" + exception.getMessage() + ")");
                logger.warn("A013 PDF font file rejected: {}", candidate, exception);
            }
        }
        throw new IllegalStateException(
                "找不到可用的 A013 PDF 字型檔。請設定 app.pdf.font-path 或 APP_PDF_FONT_PATH。"
                        + " 已檢查路徑："
                        + String.join(", ", candidates)
                        + (failedCandidates.isEmpty()
                                ? ""
                                : "。載入失敗： " + String.join(", ", failedCandidates)));
    }

    private Font loadFontFromFile(File fontFile) throws IOException, FontFormatException {
        Font[] fonts = Font.createFonts(fontFile);
        if (fonts.length == 0) {
            throw new IOException("字型檔不含可用字型");
        }
        for (Font font : fonts) {
            if (font.canDisplayUpTo(SAMPLE_TEXT) == -1) {
                return font;
            }
        }
        throw new IOException("字型檔可載入，但無法顯示中文樣本文字");
    }

    private void addPage(PDDocument document, BufferedImage image) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            stream.drawImage(pdfImage, 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        }
    }

    private BufferedImage renderPage(
            Font baseFont,
            LocalDate dateFrom,
            LocalDate dateTo,
            String sortBy,
            List<EmployeeOrderReportRow> rows) {
        BufferedImage image =
                new BufferedImage(toPixels(PAGE_WIDTH), toPixels(PAGE_HEIGHT), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font titleFont = baseFont.deriveFont(TITLE_FONT_SIZE * SCALE);
            Font bodyFont = baseFont.deriveFont(BODY_FONT_SIZE * SCALE);

            drawText(graphics, titleFont, "員工訂餐報表", START_X, START_Y);
            drawText(
                    graphics,
                    bodyFont,
                    "查詢區間：" + DATE_FORMATTER.format(dateFrom) + " ~ " + DATE_FORMATTER.format(dateTo),
                    START_X,
                    772F);
            drawText(graphics, bodyFont, "排序方式：" + toSortLabel(sortBy), START_X, 754F);

            drawText(graphics, bodyFont, "日期", 36F, TABLE_HEADER_Y);
            drawText(graphics, bodyFont, "部門", 110F, TABLE_HEADER_Y);
            drawText(graphics, bodyFont, "員工姓名", 210F, TABLE_HEADER_Y);
            drawText(graphics, bodyFont, "便當名稱", 320F, TABLE_HEADER_Y);
            drawText(graphics, bodyFont, "廠商", 470F, TABLE_HEADER_Y);

            float rowY = FIRST_ROW_Y;
            if (rows.isEmpty()) {
                drawText(graphics, bodyFont, "查無符合條件資料", START_X, rowY);
                return image;
            }
            for (EmployeeOrderReportRow row : rows) {
                drawText(graphics, bodyFont, DATE_FORMATTER.format(row.orderDate()), 36F, rowY);
                drawText(graphics, bodyFont, fitText(graphics, bodyFont, row.departmentName(), 90F), 110F, rowY);
                drawText(graphics, bodyFont, fitText(graphics, bodyFont, row.employeeName(), 100F), 210F, rowY);
                drawText(graphics, bodyFont, fitText(graphics, bodyFont, row.menuName(), 140F), 320F, rowY);
                drawText(graphics, bodyFont, fitText(graphics, bodyFont, row.supplierName(), 90F), 470F, rowY);
                rowY -= LINE_HEIGHT;
            }
            return image;
        } finally {
            graphics.dispose();
        }
    }

    private void drawText(Graphics2D graphics, Font font, String text, float x, float y) {
        graphics.setFont(font);
        graphics.drawString(text == null ? "" : text, toPixels(x), toImageY(y));
    }

    private String fitText(Graphics2D graphics, Font font, String text, float maxWidth) {
        if (text == null || text.isBlank()) {
            return "";
        }
        graphics.setFont(font);
        int maxWidthPixels = toPixels(maxWidth);
        if (graphics.getFontMetrics().stringWidth(text) <= maxWidthPixels) {
            return text;
        }
        String ellipsis = "...";
        int endIndex = text.length();
        while (endIndex > 0
                && graphics.getFontMetrics().stringWidth(text.substring(0, endIndex) + ellipsis) > maxWidthPixels) {
            endIndex -= 1;
        }
        return text.substring(0, Math.max(0, endIndex)) + ellipsis;
    }

    private String toSortLabel(String sortBy) {
        return switch (sortBy) {
            case "department" -> "部門";
            case "employee" -> "員工姓名";
            case "supplier" -> "廠商";
            default -> "日期";
        };
    }

    private List<String> resolveFontCandidates() {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (!configuredFontPath.isBlank()) {
            candidates.add(configuredFontPath);
        }
        candidates.add("/System/Library/Fonts/Supplemental/Arial Unicode.ttf");
        candidates.add("/System/Library/Fonts/Hiragino Sans GB.ttc");
        candidates.add("/System/Library/Fonts/STHeiti Medium.ttc");
        candidates.add("/System/Library/Fonts/STHeiti Light.ttc");
        candidates.add("/System/Library/Fonts/Supplemental/Songti.ttc");
        candidates.add("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc");
        candidates.add("/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc");
        candidates.add("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc");
        candidates.add("/usr/share/fonts/truetype/noto/NotoSansCJKtc-Regular.otf");
        candidates.add("/usr/share/fonts/truetype/arphic/uming.ttc");
        candidates.add("/usr/share/fonts/truetype/arphic/ukai.ttc");
        return new ArrayList<>(candidates);
    }

    private int toPixels(float points) {
        return Math.round(points * SCALE);
    }

    private int toImageY(float pdfY) {
        return Math.round((PAGE_HEIGHT - pdfY) * SCALE);
    }
}
