package com.liugh.rcs.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

@Component
public class DefaultRcsCustomerEventHttpClient
        implements RcsCustomerEventHttpClient {

    @Override
    public int post(String url, String body, Map<String, String> headers) {
        // Re-resolve immediately before connecting to block DNS rebinding and
        // private/link-local destinations.
        RcsDeliveryUrlValidator.requirePublicHttps(url, true);
        RequestConfig timeout = RequestConfig.custom()
                .setConnectionRequestTimeout(10000)
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .build();
        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(timeout).build()) {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    request.setHeader(header.getKey(), header.getValue());
                }
            }
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getEntity() != null) {
                    EntityUtils.consume(response.getEntity());
                }
                return response.getStatusLine().getStatusCode();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Customer event HTTP request failed", e);
        }
    }
}
