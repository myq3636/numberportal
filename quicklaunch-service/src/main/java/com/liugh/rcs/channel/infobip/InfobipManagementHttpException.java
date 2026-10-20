package com.liugh.rcs.channel.infobip;

/**
 * HTTP rejection returned by an Infobip management endpoint.
 *
 * The response body is deliberately not retained because it may contain
 * account or resource details. Reconciliation only needs the status code.
 */
public class InfobipManagementHttpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public InfobipManagementHttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
