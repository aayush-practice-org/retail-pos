package io.aygh.shared.entity;

/**
 * Whether a document is raised under VAT.
 * <p>
 * A mart registered for VAT still issues both: a VAT bill to a registered buyer
 * and an abbreviated one to a walk-in. The scheme is therefore a property of the
 * document, not of the mart.
 */
public enum TaxScheme {

    /**
     * VAT is worked out on the taxable amount and shown as its own line.
     */
    VAT,

    /**
     * No VAT line. The prices are all there is.
     */
    NON_VAT
}
