package com.liugh.rcs.channel.spark;

/**
 * Spark HTTP 执行抽象，便于单测注入 capturing executor。
 * 认证在 body 内，因此无需 Authorization header。
 */
public interface SparkHttpExecutor {

    /** POST application/json，返回原样响应体；网络异常返回 null。 */
    String postJson(String url, String body);
}
