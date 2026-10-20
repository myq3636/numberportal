package com.liugh.rcs.channel;

/**
 * 通道提交结果异常：携带错误码与“是否可重试”语义。
 *
 * <p>现有 {@link RcsChannelAdapter#send} 契约只有“成功返回 channelMsgId / 失败返回 null”，
 * 无法表达错误码（如 SPARK_204）与“不自动重试”（Spark 无幂等键）。编排层
 * （{@code RcsMessageService} / {@code RcsSendTaskService}）据此落错误码并决定是否重试。</p>
 */
public class RcsChannelSubmitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String errorReason;
    private final boolean retryable;

    public RcsChannelSubmitException(String errorCode, String errorReason) {
        this(errorCode, errorReason, false);
    }

    public RcsChannelSubmitException(String errorCode, String errorReason, boolean retryable) {
        super(errorReason);
        this.errorCode = errorCode;
        this.errorReason = errorReason;
        this.retryable = retryable;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
