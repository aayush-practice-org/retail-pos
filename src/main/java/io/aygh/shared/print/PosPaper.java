package io.aygh.shared.print;

/**
 * Width of a thermal (POS) paper roll, in millimetres.
 * <p>
 * Rolls are not standardised across printer vendors: the "80mm" class alone ships as
 * 75, 76, 78 and 80mm, and each of those has a slightly different printable width.
 * Instead of pinning the layout to a handful of enum constants, this type accepts any
 * width in [{@value #MIN_WIDTH_MM}, {@value #MAX_WIDTH_MM}]mm, so a tenant can configure
 * the exact roll their printer feeds — 75.5mm is as valid as 80mm — and every document
 * ({@code InvoicePdfService} receipts) lays itself out from that single number
 * via {@link PosLayout}.
 * <p>
 * Constants are provided for the common rolls, including the whole 75–80mm band.
 */
public record PosPaper(float widthMm) {

    /** Narrowest roll the layout still renders legibly. */
    public static final float MIN_WIDTH_MM = 50f;

    /** Widest roll supported (beyond this a receipt layout stops making sense — use A4). */
    public static final float MAX_WIDTH_MM = 120f;

    /** Used whenever the caller does not specify a width. */
    public static final float DEFAULT_WIDTH_MM = 80f;

    private static final float POINTS_PER_MM = 2.834645669f;

    /**
     * Thermal printers never print edge to edge: the print head covers only the middle
     * band of the roll (an "80mm" roll is commonly 72mm printable, a 58mm roll 48mm).
     * Anything laid out against the <em>physical</em> width therefore falls outside the
     * head's reach and is shaved off — and because drivers align the page at the left of
     * the printable band, the loss always shows up on the <em>right</em> edge.
     * <p>
     * These two numbers describe the unprintable strip on each side; the whole page is
     * laid out inside {@link #printableWidthPoints()} so no driver alignment can push
     * content past the head.
     */
    private static final float EDGE_INSET_RATIO = 0.05f;
    private static final float MIN_EDGE_INSET_MM = 5f;

    // ── Common rolls ─────────────────────────────────────────────────────────────
    public static final PosPaper MM_58 = new PosPaper(58f);
    public static final PosPaper MM_72 = new PosPaper(72f);
    public static final PosPaper MM_75 = new PosPaper(75f);
    public static final PosPaper MM_76 = new PosPaper(76f);
    public static final PosPaper MM_78 = new PosPaper(78f);
    public static final PosPaper MM_80 = new PosPaper(80f);
    public static final PosPaper MM_112 = new PosPaper(112f);

    /** 80mm — the most common till roll. */
    public static final PosPaper DEFAULT = MM_80;

    public PosPaper {
        if (Float.isNaN(widthMm) || widthMm < MIN_WIDTH_MM || widthMm > MAX_WIDTH_MM) {
            throw new IllegalArgumentException(
                    "POS paper width must be between " + MIN_WIDTH_MM + "mm and " + MAX_WIDTH_MM
                            + "mm, got: " + widthMm + "mm");
        }
    }

    public static PosPaper ofMm(float widthMm) {
        return new PosPaper(widthMm);
    }

    /**
     * Resolves an optional (typically request-supplied) width, falling back to
     * {@link #DEFAULT} when {@code widthMm} is {@code null}.
     */
    public static PosPaper orDefault(Float widthMm) {
        return widthMm == null ? DEFAULT : new PosPaper(widthMm);
    }

    /** Paper width converted to PDF user-space units (points). */
    public float widthPoints() {
        return widthMm * POINTS_PER_MM;
    }

    /** Unprintable strip on each side of the roll, in millimetres. */
    public float edgeInsetMm() {
        return Math.max(MIN_EDGE_INSET_MM, widthMm * EDGE_INSET_RATIO);
    }

    /**
     * Width the print head can actually reach, in points. Documents size their page to
     * this — not to {@link #widthPoints()} — so nothing is clipped at the right edge.
     */
    public float printableWidthPoints() {
        return (widthMm - 2 * edgeInsetMm()) * POINTS_PER_MM;
    }

    @Override
    public String toString() {
        return widthMm + "mm";
    }
}
