package io.aygh.shared.cbms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Posts a bill to the IRD's Central Billing Monitoring System.
 * <p>
 * <b>CBMS syncing is switched off for now.</b> The mart is not yet registered with
 * the IRD, so every bill is reported as accepted and nothing leaves the building.
 * The real request is kept below, commented out, so turning it back on is a matter
 * of restoring {@link #sendCbmsBillingRequest} to call {@code postToIrd} once the
 * registration is in place — the call site already handles a rejection.
 */
@Service
@Slf4j
public class CbmsClientImpl implements CbmsClient {

    @Override
    public boolean sendCbmsBillingRequest(CbmsRequest request) {
        // TODO: restore `return postToIrd(request);` once the mart is registered with the IRD.
        log.info("CBMS syncing is disabled: invoice {} was not sent to the IRD.", request.invoiceNumber());
        return true;
    }

//    private static final String BILLING_PATH = "/api/bill";
//
//    private final RestClient restClient = RestClient.builder()
//            .baseUrl("https://cbapi.ird.gov.np")
//            .build();
//
//    private boolean postToIrd(CbmsRequest request) {
//        try {
//            ResponseEntity<String> response = restClient.post()
//                    .uri(BILLING_PATH)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .body(request)
//                    .retrieve()
//                    // A rejected bill is an outcome to report back, not an exception:
//                    // swallow the default error handler and read the status ourselves.
//                    .onStatus(HttpStatusCode::isError, (req, res) -> {
//                    })
//                    .toEntity(String.class);
//
//            if (!response.getStatusCode().is2xxSuccessful()) {
//                throw new CbmsSyncFailedException("CBMS rejected invoice " + request.invoiceNumber());
//            }
//
//            switch (response.getBody()) {
//                case "200" -> {
//                    log.info("Invoice {} synchronized with the IRD.", request.invoiceNumber());
//                    return true;
//                }
//                case "100" -> throw new CbmsSyncFailedException("CBMS rejected invoice "
//                        + request.invoiceNumber() + ". Please check credentials and try again.");
//                case "101" -> throw new CbmsSyncFailedException("CBMS rejected invoice "
//                        + request.invoiceNumber() + ". This bill number has already been entered. Please contact support.");
//                case null, default -> throw new CbmsSyncFailedException("Sync failed: unexpected IRD response code");
//            }
//        } catch (CbmsSyncFailedException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new CbmsSyncFailedException("CBMS rejected invoice " + request.invoiceNumber()
//                    + ". Some unknown error occurred.");
//        }
//    }
}
