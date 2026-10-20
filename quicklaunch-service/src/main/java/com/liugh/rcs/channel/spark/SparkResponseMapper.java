package com.liugh.rcs.channel.spark;

import com.alibaba.fastjson.JSONObject;

/**
 * Spark 响应解析：成功 {code:200, desc, uuid}，失败 {code, desc}。
 * 空响应 / 非 JSON / 无 code 归一为 SPARK_SUBMIT_UNKNOWN。
 */
public final class SparkResponseMapper {

    private SparkResponseMapper() {
    }

    public static final class Result {
        private final String code;
        private final String desc;
        private final String uuid;

        private Result(String code, String desc, String uuid) {
            this.code = code;
            this.desc = desc;
            this.uuid = uuid;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        public String getUuid() {
            return uuid;
        }

        public boolean isSuccess() {
            return "200".equals(code);
        }

        public String getErrorCode() {
            return "SPARK_" + (code == null ? "SUBMIT_UNKNOWN" : code);
        }
    }

    public static Result parse(String rawBody) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            return unknown("empty response");
        }
        JSONObject json;
        try {
            json = JSONObject.parseObject(rawBody);
        } catch (Exception e) {
            return unknown("non-json response");
        }
        if (json == null) {
            return unknown("non-json response");
        }
        String code = json.getString("code");
        if (code == null) {
            return unknown("missing code");
        }
        return new Result(code, json.getString("desc"), json.getString("uuid"));
    }

    private static Result unknown(String reason) {
        return new Result(null, reason, null);
    }
}
