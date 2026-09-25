package io.aygh.shared.cbms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Posts bills and credit notes (sales returns) to the IRD's Central Billing
 * Monitoring System.
 * <p>
 * <b>CBMS syncing is switched off for now.</b> The mart is not yet registered with
 * the IRD, so every document is reported as accepted and nothing leaves the
 * building. The real request is kept below, commented out, so turning it back on
 * is a matter of restoring both send methods to call {@code postToIrd} once the
 * registration is in place — the call sites already handle a rejection.
 */
@Service
@Slf4j
public class CbmsClientImpl implements CbmsClient {

    @Override
    public boolean sendCbmsBillingRequest(CbmsRequest request) {
        // TODO: restore `return postToIrd(BILLING_PATH, request, request.invoiceNumber());`
        //  once the mart is registered with the IRD.
        log.info("CBMS syncing is disabled: invoice {} was not sent to the IRD.", request.invoiceNumber());
        return true;
    }

    @Override
    public boolean sendCbmsSalesReturnRequest(CbmsSalesReturnRequest request) {
        // TODO: restore `return postToIrd(SALES_RETURN_PATH, request, request.creditNoteNumber());`
        //  once the mart is registered with the IRD.
        log.info("CBMS syncing is disabled: credit note {} against invoice {} was not sent to the IRD.",
                request.creditNoteNumber(), request.refInvoiceNumber());
        return true;
    }

//    private static final String BILLING_PATH = "/api/bill";
//    private static final String SALES_RETURN_PATH = "/api/billreturn";
//
//    private final RestClient restClient = RestClient.builder()
//            .baseUrl("https://cbapi.ird.gov.np")
//            .build();
//
//    /**
//     * Bills and credit notes share one response contract: the body is a code, and
//     * only "200" means the IRD accepted the document.
//     */
//    private boolean postToIrd(String path, Object body, String documentNumber) {
//        try {
//            ResponseEntity<String> response = restClient.post()
//                    .uri(path)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .body(body)
//                    .retrieve()
//                    // A rejected document is an outcome to report back, not an exception:
//                    // swallow the default error handler and read the status ourselves.
//                    .onStatus(HttpStatusCode::isError, (req, res) -> {
//                    })
//                    .toEntity(String.class);
//
//            if (!response.getStatusCode().is2xxSuccessful()) {
//                throw new CbmsSyncFailedException("CBMS rejected " + documentNumber);
//            }
//
//            String code = response.getBody() == null ? null : response.getBody().trim();
//            switch (code) {
//                case "200" -> {
//                    log.info("{} synchronized with the IRD.", documentNumber);
//                    return true;
//                }
//                case "100" -> throw new CbmsSyncFailedException("CBMS rejected " + documentNumber
//                        + ": API credentials do not match. Please check credentials and try again.");
//                case "101" -> throw new CbmsSyncFailedException("CBMS rejected " + documentNumber
//                        + ": this bill number has already been entered, or the bill does not exist. Please contact support.");
//                case "102" -> throw new CbmsSyncFailedException("CBMS rejected " + documentNumber
//                        + ": the IRD could not save the bill details. Please check the bill and try again.");
//                case "103" -> throw new CbmsSyncFailedException("CBMS rejected " + documentNumber
//                        + ": unknown error at the IRD. Please contact support.");
//                case "104" -> throw new CbmsSyncFailedException("CBMS rejected " + documentNumber
//                        + ": the IRD found the document invalid.");
//                case "105" -> throw new CbmsSyncFailedException("CBMS rejected " + documentNumber
//                        + ": the bill being returned does not exist at the IRD.");
//                case null, default -> throw new CbmsSyncFailedException("Sync failed: unexpected IRD response code " + code);
//            }
//        } catch (CbmsSyncFailedException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new CbmsSyncFailedException("CBMS rejected " + documentNumber + ". Some unknown error occurred.");
//        }
//    }
}
