package com.footballstats.backend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SeasonProtocolArchiveService {

    private static final DateTimeFormatter KICKOFF_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Color TEXT_COLOR = new Color(0x18, 0x20, 0x33);
    private static final Color MUTED_COLOR = new Color(0x5B, 0x64, 0x77);
    private static final Color BORDER_COLOR = new Color(0xCF, 0xD7, 0xE6);
    private static final Color TEAM_HEADER_COLOR = new Color(0xE8, 0xED, 0xF7);
    private static final Color TABLE_HEADER_COLOR = new Color(0xF4, 0xF7, 0xFB);
    private static final int PAGE_WIDTH = 1240;
    private static final int PAGE_HEIGHT = 1754;
    private static final int PAGE_MARGIN = 72;

    public ArchivePayload buildSeasonProtocolsArchive(
        String seasonName,
        List<MatchProtocolService.SeasonProtocolExportMatchData> matches
    ) {
        if (matches == null || matches.isEmpty()) {
            throw new IllegalArgumentException("В выбранном сезоне нет подтвержденных протоколов для выгрузки.");
        }

        String normalizedSeasonName = seasonName == null || seasonName.isBlank() ? "season" : seasonName.trim();
        String archiveFileName = sanitizeFileName("Протоколы_" + normalizedSeasonName, "protocols") + ".zip";

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (MatchProtocolService.SeasonProtocolExportMatchData match : matches) {
                byte[] pdfBytes = renderProtocolPdf(normalizedSeasonName, match);
                ZipEntry entry = new ZipEntry(sanitizeFileName(match.fileName(), "match-protocol.pdf"));
                zipOutputStream.putNextEntry(entry);
                zipOutputStream.write(pdfBytes);
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return new ArchivePayload(archiveFileName, outputStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать архив протоколов сезона.", exception);
        }
    }

    public PdfPayload buildMatchProtocolPdf(
        String seasonName,
        MatchProtocolService.SeasonProtocolExportMatchData match
    ) {
        if (match == null) {
            throw new IllegalArgumentException("Протокол матча не найден.");
        }

        String normalizedSeasonName = seasonName == null || seasonName.isBlank() ? "season" : seasonName.trim();
        try {
            return new PdfPayload(sanitizeFileName(match.fileName(), "match-protocol.pdf"), renderProtocolPdf(normalizedSeasonName, match));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать PDF протокола матча.", exception);
        }
    }

    private byte[] renderProtocolPdf(String seasonName, MatchProtocolService.SeasonProtocolExportMatchData match) throws IOException {
        BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            configureGraphics(graphics);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);

            int cursorY = PAGE_MARGIN;
            cursorY = drawTitle(graphics, cursorY, "Протокол матча", 42, true, TEXT_COLOR);
            cursorY = drawTitle(graphics, cursorY + 8, seasonName + " · " + defaultString(match.tourName()), 22, false, MUTED_COLOR);
            cursorY += 12;
            cursorY = drawMeta(graphics, cursorY, match);
            cursorY += 24;
            cursorY = drawSectionTitle(graphics, cursorY, "Составы команд");

            for (MatchProtocolService.SeasonProtocolExportTeamData team : match.teams()) {
                cursorY = drawTeamTable(graphics, cursorY, team);
                cursorY += 24;
            }

            cursorY = drawReferees(graphics, cursorY, match.referees());
            cursorY += 24;
            cursorY = drawSectionTitle(graphics, cursorY, "Примечание");
            cursorY = drawNoteBlock(graphics, cursorY, match.note());
            drawSignature(graphics, cursorY + 48);
        } finally {
            graphics.dispose();
        }

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDImageXObject pageImage = LosslessFactory.createFromImage(document, image);
            try (var contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                contentStream.drawImage(pageImage, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            }
            document.save(pdfOutput);
            return pdfOutput.toByteArray();
        }
    }

    private void configureGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private int drawTitle(Graphics2D graphics, int topY, String text, int fontSize, boolean bold, Color color) {
        Font font = new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, fontSize);
        graphics.setFont(font);
        graphics.setColor(color);
        FontMetrics metrics = graphics.getFontMetrics(font);
        graphics.drawString(defaultString(text), PAGE_MARGIN, topY + metrics.getAscent());
        return topY + metrics.getHeight();
    }

    private int drawMeta(Graphics2D graphics, int topY, MatchProtocolService.SeasonProtocolExportMatchData match) {
        Font bodyFont = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
        Font scoreFont = new Font(Font.SANS_SERIF, Font.BOLD, 32);
        graphics.setFont(bodyFont);
        graphics.setColor(TEXT_COLOR);
        FontMetrics bodyMetrics = graphics.getFontMetrics(bodyFont);

        String kickoffLabel = match.kickoffAt() == null
            ? "Дата не назначена"
            : KICKOFF_FORMATTER.format(match.kickoffAt().atZoneSameInstant(ZoneId.systemDefault()));
        graphics.drawString("Дата: " + kickoffLabel, PAGE_MARGIN, topY + bodyMetrics.getAscent());

        String scoreLabel = buildScoreLabel(match);
        graphics.setFont(scoreFont);
        FontMetrics scoreMetrics = graphics.getFontMetrics(scoreFont);
        String scoreLine = defaultString(match.homeTeamName()) + "   " + scoreLabel + "   " + defaultString(match.awayTeamName());
        int scoreX = (PAGE_WIDTH - scoreMetrics.stringWidth(scoreLine)) / 2;
        int scoreY = topY + bodyMetrics.getHeight() + 24 + scoreMetrics.getAscent();
        graphics.drawString(scoreLine, Math.max(PAGE_MARGIN, scoreX), scoreY);

        return scoreY + scoreMetrics.getDescent();
    }

    private int drawSectionTitle(Graphics2D graphics, int topY, String text) {
        return drawTitle(graphics, topY, text, 24, true, TEXT_COLOR) + 12;
    }

    private int drawTeamTable(Graphics2D graphics, int topY, MatchProtocolService.SeasonProtocolExportTeamData team) {
        int tableWidth = PAGE_WIDTH - PAGE_MARGIN * 2;
        int[] columnWidths = new int[] {80, tableWidth - 80 - 120 * 3, 120, 120, 120};
        int rowHeight = 38;
        int x = PAGE_MARGIN;
        int y = topY;

        graphics.setColor(TEAM_HEADER_COLOR);
        graphics.fillRect(x, y, tableWidth, rowHeight);
        drawBorder(graphics, x, y, tableWidth, rowHeight);
        drawCellText(graphics, defaultString(team.teamName()), x + 14, y, tableWidth - 28, rowHeight, new Font(Font.SANS_SERIF, Font.BOLD, 20), TEXT_COLOR, false);
        y += rowHeight;

        String[] headers = new String[] {"№", "Игрок", "Г", "ЖК", "КК"};
        graphics.setColor(TABLE_HEADER_COLOR);
        graphics.fillRect(x, y, tableWidth, rowHeight);
        drawBorder(graphics, x, y, tableWidth, rowHeight);
        int headerX = x;
        for (int index = 0; index < headers.length; index += 1) {
            drawBorder(graphics, headerX, y, columnWidths[index], rowHeight);
            drawCellText(graphics, headers[index], headerX, y, columnWidths[index], rowHeight, new Font(Font.SANS_SERIF, Font.BOLD, 18), TEXT_COLOR, true);
            headerX += columnWidths[index];
        }
        y += rowHeight;

        List<MatchProtocolService.SeasonProtocolExportPlayerData> players = team.players() == null ? List.of() : team.players().stream()
            .sorted(Comparator.comparingInt(MatchProtocolService.SeasonProtocolExportPlayerData::sortOrder))
            .toList();

        if (players.isEmpty()) {
            drawBorder(graphics, x, y, tableWidth, rowHeight + 10);
            drawCellText(
                graphics,
                "Заявка команды пока не заполнена.",
                x + 14,
                y,
                tableWidth - 28,
                rowHeight + 10,
                new Font(Font.SANS_SERIF, Font.PLAIN, 18),
                MUTED_COLOR,
                false
            );
            return y + rowHeight + 10;
        }

        for (MatchProtocolService.SeasonProtocolExportPlayerData player : players) {
            int cellX = x;
            drawBorder(graphics, x, y, tableWidth, rowHeight);
            drawBorder(graphics, cellX, y, columnWidths[0], rowHeight);
            drawCellText(graphics, String.valueOf(player.sortOrder()), cellX, y, columnWidths[0], rowHeight, new Font(Font.SANS_SERIF, Font.PLAIN, 18), TEXT_COLOR, true);
            cellX += columnWidths[0];
            drawBorder(graphics, cellX, y, columnWidths[1], rowHeight);
            drawCellText(graphics, defaultString(player.playerName()), cellX + 10, y, columnWidths[1] - 20, rowHeight, new Font(Font.SANS_SERIF, Font.PLAIN, 18), TEXT_COLOR, false);
            cellX += columnWidths[1];
            drawStatCell(graphics, cellX, y, columnWidths[2], rowHeight, player.goals());
            cellX += columnWidths[2];
            drawStatCell(graphics, cellX, y, columnWidths[3], rowHeight, player.yellowCards());
            cellX += columnWidths[3];
            drawStatCell(graphics, cellX, y, columnWidths[4], rowHeight, player.redCards());
            y += rowHeight;
        }

        return y;
    }

    private int drawReferees(Graphics2D graphics, int topY, List<MatchProtocolService.SeasonProtocolExportRefereeData> referees) {
        List<String> lines = List.of(
            "Главный судья: " + findReferee(referees, "Главный арбитр"),
            "Помощник 1: " + findReferee(referees, "Помощник 1"),
            "Помощник 2: " + findReferee(referees, "Помощник 2")
        );

        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
        graphics.setFont(font);
        graphics.setColor(TEXT_COLOR);
        FontMetrics metrics = graphics.getFontMetrics(font);
        int y = topY;
        for (String line : lines) {
            graphics.drawString(line, PAGE_MARGIN, y + metrics.getAscent());
            y += metrics.getHeight() + 6;
        }
        return y;
    }

    private int drawNoteBlock(Graphics2D graphics, int topY, String note) {
        int x = PAGE_MARGIN;
        int width = PAGE_WIDTH - PAGE_MARGIN * 2;
        List<String> lines = wrapText(note == null || note.isBlank() ? "Дополнительные замечания по матчу не указаны." : note, new Font(Font.SANS_SERIF, Font.PLAIN, 18), width - 28, graphics);
        int lineHeight = graphics.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 18)).getHeight() + 4;
        int height = Math.max(110, 24 + lineHeight * lines.size());
        drawBorder(graphics, x, topY, width, height);
        int currentY = topY + 14;
        for (String line : lines) {
            drawCellText(graphics, line, x + 14, currentY, width - 28, lineHeight, new Font(Font.SANS_SERIF, Font.PLAIN, 18), TEXT_COLOR, false);
            currentY += lineHeight;
        }
        return topY + height;
    }

    private void drawSignature(Graphics2D graphics, int topY) {
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
        graphics.setFont(font);
        graphics.setColor(TEXT_COLOR);
        graphics.drawString("Главный судья", PAGE_MARGIN, topY);
        graphics.drawLine(PAGE_MARGIN + 220, topY + 4, PAGE_MARGIN + 620, topY + 4);
    }

    private void drawStatCell(Graphics2D graphics, int x, int y, int width, int height, Integer value) {
        drawBorder(graphics, x, y, width, height);
        drawCellText(graphics, value == null || value == 0 ? "—" : String.valueOf(value), x, y, width, height, new Font(Font.SANS_SERIF, Font.PLAIN, 18), TEXT_COLOR, true);
    }

    private void drawCellText(Graphics2D graphics, String text, int x, int y, int width, int height, Font font, Color color, boolean centered) {
        graphics.setFont(font);
        graphics.setColor(color);
        FontMetrics metrics = graphics.getFontMetrics(font);
        int textX = centered ? x + Math.max(0, (width - metrics.stringWidth(text)) / 2) : x;
        int textY = y + ((height - metrics.getHeight()) / 2) + metrics.getAscent();
        graphics.drawString(defaultString(text), textX, textY);
    }

    private void drawBorder(Graphics2D graphics, int x, int y, int width, int height) {
        graphics.setColor(BORDER_COLOR);
        graphics.setStroke(new BasicStroke(1f));
        graphics.drawRect(x, y, width, height);
    }

    private List<String> wrapText(String text, Font font, int maxWidth, Graphics2D graphics) {
        List<String> lines = new ArrayList<>();
        FontMetrics metrics = graphics.getFontMetrics(font);
        String[] paragraphs = defaultString(text).split("\\R");
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            String[] words = paragraph.trim().split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (metrics.stringWidth(candidate) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    if (!current.isEmpty()) {
                        lines.add(current.toString());
                    }
                    current.setLength(0);
                    current.append(word);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    private String findReferee(List<MatchProtocolService.SeasonProtocolExportRefereeData> referees, String label) {
        if (referees == null) {
            return "Не назначен";
        }
        return referees.stream()
            .filter(referee -> label.equals(referee.label()))
            .map(MatchProtocolService.SeasonProtocolExportRefereeData::name)
            .filter(name -> name != null && !name.isBlank())
            .findFirst()
            .orElse("Не назначен");
    }

    private String buildScoreLabel(MatchProtocolService.SeasonProtocolExportMatchData match) {
        if (match.homeTechnicalDefeat() || match.awayTechnicalDefeat()) {
            return "тех. пор.";
        }
        int homeScore = match.homeScore() == null ? 0 : match.homeScore();
        int awayScore = match.awayScore() == null ? 0 : match.awayScore();
        return homeScore + ":" + awayScore;
    }

    private String sanitizeFileName(String fileName, String fallback) {
        String normalized = fileName == null || fileName.isBlank() ? fallback : fileName.trim();
        return normalized
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .replaceAll("\\s+", "_");
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    public record ArchivePayload(String fileName, byte[] bytes) {}

    public record PdfPayload(String fileName, byte[] bytes) {}
}