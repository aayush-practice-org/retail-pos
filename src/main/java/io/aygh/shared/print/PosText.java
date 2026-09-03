package io.aygh.shared.print;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared text measuring / wrapping / drawing primitives for thermal-roll documents.
 * <p>
 * Everything here measures the <em>actual glyph width</em> of the font rather than
 * guessing a character count, which is what lets the same code lay out correctly at any
 * {@link PosPaper} width and any font size.
 */
public final class PosText {

    private PosText() {
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // Measuring
    // ═════════════════════════════════════════════════════════════════════════════

    public static float widthOf(PDFont font, float size, String text) {
        if (text == null || text.isEmpty()) return 0f;
        try {
            return font.getStringWidth(text) / 1000 * size;
        } catch (IOException | IllegalArgumentException e) {
            // Unencodable character or (practically never) an IO problem — rough estimate.
            return text.length() * size * 0.55f;
        }
    }

    /**
     * Replaces every character the font cannot encode with {@code ?}. The Standard-14
     * fonts only cover WinAnsi, so an item name typed in Devanagari (or with a smart
     * quote pasted from a phone) would otherwise blow up PDF generation at draw time.
     */
    public static String sanitize(PDFont font, String text) {
        if (text == null || text.isEmpty()) return "";
        if (isEncodable(font, text)) return text;

        StringBuilder sb = new StringBuilder(text.length());
        text.codePoints().forEach(cp -> {
            String ch = new String(Character.toChars(cp));
            sb.append(isEncodable(font, ch) ? ch : "?");
        });
        return sb.toString();
    }

    private static boolean isEncodable(PDFont font, String text) {
        try {
            font.getStringWidth(text);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // Wrapping
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Word-wraps {@code text} to fit within {@code maxWidth} points at the given font/size.
     * A single word longer than {@code maxWidth} is hard-broken so it can never overflow
     * the paper. Text is sanitized first, so the returned lines are always drawable.
     */
    public static List<String> wrap(PDFont font, float size, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }

        for (String paragraph : sanitize(font, text).split("\n", -1)) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (word.isEmpty()) continue;
                String candidate = current.isEmpty() ? word : current + " " + word;

                if (widthOf(font, size, candidate) <= maxWidth) {
                    current = new StringBuilder(candidate);
                    continue;
                }
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                if (widthOf(font, size, word) > maxWidth) {
                    lines.addAll(hardBreak(font, size, word, maxWidth));
                } else {
                    current.append(word);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    private static List<String> hardBreak(PDFont font, float size, String word, float maxWidth) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (char c : word.toCharArray()) {
            String candidate = current.toString() + c;
            if (widthOf(font, size, candidate) > maxWidth && !current.isEmpty()) {
                parts.add(current.toString());
                current = new StringBuilder(String.valueOf(c));
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) parts.add(current.toString());
        return parts;
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // Drawing
    // ═════════════════════════════════════════════════════════════════════════════

    public static void drawAt(PDPageContentStream cs, PDFont font, float size, String text,
                              float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(font, text));
        cs.endText();
    }

    /** Draws {@code text} centered within [marginX, marginX + contentWidth] at baseline y. */
    public static void drawCentered(PDPageContentStream cs, PDFont font, float size, String text,
                                    float y, float marginX, float contentWidth) throws IOException {
        String safe = sanitize(font, text);
        float w = widthOf(font, size, safe);
        drawAt(cs, font, size, safe, marginX + (contentWidth - w) / 2, y);
    }

    /** Draws {@code text} so that it ends at {@code rightX}. */
    public static void drawRightAligned(PDPageContentStream cs, PDFont font, float size, String text,
                                        float rightX, float y) throws IOException {
        String safe = sanitize(font, text);
        drawAt(cs, font, size, safe, rightX - widthOf(font, size, safe), y);
    }

    /**
     * Dashed separator across the content column; returns the new y — a
     * {@link PosLayout#dividerGap()} below the rule, i.e. clear of the next line's ascender.
     */
    public static float drawDashedDivider(PDPageContentStream cs, PosLayout layout, float y,
                                          float thickness) throws IOException {
        cs.setLineWidth(thickness);
        cs.setLineDashPattern(new float[]{2, 2}, 0);
        cs.moveTo(layout.margin(), y);
        cs.lineTo(layout.contentRight(), y);
        cs.stroke();
        cs.setLineDashPattern(new float[]{}, 0);
        return y - layout.dividerGap();
    }

    /** Solid separator across the content column; returns the new y, a divider gap below. */
    public static float drawSolidDivider(PDPageContentStream cs, PosLayout layout, float y,
                                         float thickness) throws IOException {
        cs.setLineWidth(thickness);
        cs.moveTo(layout.margin(), y);
        cs.lineTo(layout.contentRight(), y);
        cs.stroke();
        return y - layout.dividerGap();
    }
}
