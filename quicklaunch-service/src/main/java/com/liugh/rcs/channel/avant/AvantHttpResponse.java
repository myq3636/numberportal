package com.liugh.rcs.channel.avant;

public final class AvantHttpResponse {
    private final int statusCode;
    private final String body;

    public AvantHttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }
}
