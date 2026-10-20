package com.liugh.rcs.channel.avant;

public interface AvantHttpExecutor {
    AvantHttpResponse postJson(String url, String authorization, String body)
            throws Exception;
}
