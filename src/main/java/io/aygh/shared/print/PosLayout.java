package io.aygh.shared.print;

/**
 * Fully-resolved layout numbers (page width, margins, font sizes, line heights) derived
 * from a {@link PosPaper}. Every POS drawing/height routine takes one of these instead of
 * reading fixed constants, which is what lets the same drawing code print correctly on a
 * narrow 58mm roll, anywhere in the 75–80mm band, or a wide 112mm roll.
 * <p>
 * Typography scales <em>continuously</em> with the roll width (relative to the 80mm
 * baseline) rather than in fixed buckets, so a 75mm and a 76mm roll differ by exactly the
 * ~1.3% you would expect instead of snapping to the same numbers.
 */
public record PosLayout(
        PosPaper paper,
        float width,
        float margin,
        float contentWidth,
        float extraIndent,
        float titleSize,
        float subtitleSize,
        float bodySize,
        float smallSize,
        float lineHeight,
        float extraLineHeight,
        float sectionGap) {

    // Baseline typography, tuned for an 80mm roll.
    private static final float BASE_TITLE = 14f;
    private static final float BASE_SUBTITLE = 10f;
    private static final float BASE_BODY = 9f;
    private static final float BASE_SMALL = 8f;
    private static final float BASE_LINE_HEIGHT = 12f;
    private static final float BASE_EXTRA_LINE_HEIGHT = 9f;
    private static final float BASE_SECTION_GAP = 8f;
    private static final float BASE_EXTRA_INDENT = 12f;

    // Type never shrinks below 82% (unreadable on paper) nor grows past 110% (wastes roll).
    private static final float MIN_SCALE = 0.82f;
    private static final float MAX_SCALE = 1.10f;

    private static final float MIN_MARGIN = 4f;
    private static final float MAX_MARGIN = 8f;

    public static PosLayout of(PosPaper paper) {
        float scale = clamp(paper.widthMm() / PosPaper.DEFAULT_WIDTH_MM, MIN_SCALE, MAX_SCALE);
        // Margin tracks the roll width so narrow rolls don't give away their content width
        // and wide rolls don't print hard against the edge of the paper. It stays small
        // because the page is already inset from the physical roll (see width, below) —
        // the unprintable strip is the real margin.
        float margin = clamp(paper.widthMm() * 0.0625f, MIN_MARGIN, MAX_MARGIN);
        // The page is the PRINTABLE width, not the physical roll width. Sizing it to the
        // roll leaves the right-hand column outside the print head's reach, which is why
        // right-aligned totals came out clipped.
        float widthPt = paper.printableWidthPoints();

        return new PosLayout(
                paper,
                widthPt,
                margin,
                widthPt - 2 * margin,
                BASE_EXTRA_INDENT * scale,
                BASE_TITLE * scale,
                BASE_SUBTITLE * scale,
                BASE_BODY * scale,
                BASE_SMALL * scale,
                BASE_LINE_HEIGHT * scale,
                BASE_EXTRA_LINE_HEIGHT * scale,
                BASE_SECTION_GAP * scale
        );
    }

    /**
     * Same page geometry, all type and vertical spacing scaled by {@code factor}. Used by
     * documents that need bigger print than a receipt — a shelf ticket has to be readable at arm's
     * length — without re-deriving the paper geometry.
     */
    public PosLayout withFontScale(float factor) {
        return new PosLayout(
                paper, width, margin, contentWidth,
                extraIndent * factor,
                titleSize * factor,
                subtitleSize * factor,
                bodySize * factor,
                smallSize * factor,
                lineHeight * factor,
                extraLineHeight * factor,
                sectionGap * factor
        );
    }

    /**
     * Vertical drop after a divider rule. A text baseline sits one section gap below the
     * rule, but glyphs rise above their baseline — on narrow rolls (where the gap scales
     * down) that ascent would collide with the rule, so the drop also clears the body
     * font's ascender. Divider drawing and every height calculation use this same number.
     */
    public float dividerGap() {
        return sectionGap + bodySize * 0.45f;
    }

    /** X coordinate of the right edge of the content column. */
    public float contentRight() {
        return margin + contentWidth;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
