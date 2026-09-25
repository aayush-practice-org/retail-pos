package io.aygh.shared.cbms;

public interface CbmsClient {

    /**
     * @return true when the IRD accepted the bill
     */
    boolean sendCbmsBillingRequest(CbmsRequest request);
}
