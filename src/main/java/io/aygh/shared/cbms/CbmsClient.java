package io.aygh.shared.cbms;

public interface CbmsClient {

    /**
     * @return true when the IRD accepted the bill
     */
    boolean sendCbmsBillingRequest(CbmsRequest request);

    /**
     * Posts a credit note against a bill the IRD already holds.
     *
     * @return true when the IRD accepted the credit note
     */
    boolean sendCbmsSalesReturnRequest(CbmsSalesReturnRequest request);
}
