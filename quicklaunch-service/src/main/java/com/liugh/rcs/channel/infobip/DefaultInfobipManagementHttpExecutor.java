package com.liugh.rcs.channel.infobip;

import java.nio.charset.StandardCharsets;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

@Component
public class DefaultInfobipManagementHttpExecutor
        implements InfobipManagementHttpExecutor {

    @Override
    public String request(String method, String url, String authorization,
            String body) {
        RequestConfig timeout = RequestConfig.custom()
                .setConnectionRequestTimeout(10000)
                .setConnectTimeout(10000)
                .setSocketTimeout(20000)
                .build();
        RequestBuilder builder = RequestBuilder.create(method).setUri(url)
                .setHeader("Authorization", authorization)
                .setHeader("Accept", "application/json");
        if (body != null) {
            builder.setHeader("Content-Type", "application/json");
            builder.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        }
        HttpUriRequest request = builder.build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(timeout).build();
                CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() == null ? ""
                    : EntityUtils.toString(response.getEntity(),
                            StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) {
                throw new InfobipManagementHttpException(status,
                        "Infobip management request rejected with HTTP " + status);
            }
            return responseBody;
        } catch (InfobipManagementHttpException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Infobip management HTTP request failed",
                    e);
        }
    }

    @Override
    public String upload(String url, String authorization, String fileName,
            String contentType, byte[] content) {
        RequestConfig timeout = RequestConfig.custom()
                .setConnectionRequestTimeout(10000)
                .setConnectTimeout(10000)
                .setSocketTimeout(30000)
                .build();
        ContentType type = ContentType.create(
                contentType == null ? "application/octet-stream" : contentType);
        HttpUriRequest request = RequestBuilder.post(url)
                .setHeader("Authorization", authorization)
                .setHeader("Accept", "application/json")
                .setEntity(MultipartEntityBuilder.create()
                        .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                        .addBinaryBody("document", content, type,
                                fileName == null ? "document" : fileName)
                        .build())
                .build();
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(timeout).build();
                CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() == null ? ""
                    : EntityUtils.toString(response.getEntity(),
                            StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) {
                throw new InfobipManagementHttpException(status,
                        "Infobip document upload rejected with HTTP " + status);
            }
            return responseBody;
        } catch (InfobipManagementHttpException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Infobip document upload failed", e);
        }
    }
}
