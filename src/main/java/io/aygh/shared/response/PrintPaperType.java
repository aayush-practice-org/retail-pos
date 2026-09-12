package io.aygh.shared.response;

import org.apache.pdfbox.pdmodel.common.PDRectangle;
import io.aygh.shared.print.PosPaper;

public enum PrintPaperType {
    MM80(80f, true),
    MM75(75f, true),
    A4(null, false),
    A5(null, false),
    A6(null, false);

    private final Float posWidthMm;
    private final boolean pos;

    PrintPaperType(Float posWidthMm, boolean pos) {
        this.posWidthMm = posWidthMm;
        this.pos = pos;
    }

    public boolean isPos() {
        return pos;
    }

    public PosPaper toPosPaper() {
        if (!pos) {
            throw new IllegalStateException(this + " is not a POS paper type");
        }
        return PosPaper.ofMm(posWidthMm);
    }

    public PDRectangle toPageSize() {
        return switch (this) {
            case A4 -> PDRectangle.A4;
            case A5 -> PDRectangle.A5;
            case A6 -> PDRectangle.A6;
            default -> throw new IllegalStateException(this + " is not a page paper type");
        };
    }
}
