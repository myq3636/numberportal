package com.liugh.rcs.channel.spark;

import org.springframework.stereotype.Component;

import com.liugh.util.HttpUtil;

/**
 * Spark 默认 HTTP 执行器：复用 {@link HttpUtil#postJson}（30s 连接/读超时）。
 * 不打印 body，避免泄露 password。
 */
@Component
public class DefaultSparkHttpExecutor implements SparkHttpExecutor {

    @Override
    public String postJson(String url, String body) {
        return HttpUtil.postJson(url, body);
    }
}
