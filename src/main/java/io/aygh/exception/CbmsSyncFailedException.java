package io.aygh.exception;

/**
 * The IRD's CBMS refused a bill, or could not be reached. Rolls the sale back:
 * a bill the IRD has not accepted must not leave the till.
 */
public class CbmsSyncFailedException extends RuntimeException {

    public CbmsSyncFailedException(String message) {
        super(message);
    }
}
