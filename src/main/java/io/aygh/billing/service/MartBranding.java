package io.aygh.billing.service;

/**
 * The mart's own details, as they are printed at the head of a bill.
 * <p>
 * A snapshot taken per request rather than a live entity: the PDF is drawn
 * outside the transaction that read it, and a detached entity going lazy
 * halfway down a page is not a failure mode worth having.
 */
public record MartBranding(
        String companyName,
        String companyAddress,
        String companyPhone,
        String email,
        String registrationNumber
) {

    /**
     * What a bill prints when the mart's details cannot be read — the super
     * admin exercising an endpoint, or an account whose tenant row has gone.
     * Never blank: a header with an empty company name looks like a bug on
     * paper, where nobody can check.
     */
    public static MartBranding unknown() {
        return new MartBranding("MART", null, null, null, null);
    }
}
