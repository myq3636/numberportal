package com.liugh.rcs.service;

import java.util.Map;

public interface RcsCustomerEventHttpClient {

    int post(String url, String body, Map<String, String> headers);
}
