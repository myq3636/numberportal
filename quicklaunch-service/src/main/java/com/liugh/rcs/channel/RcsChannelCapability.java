package com.liugh.rcs.channel;

/**
 * Internal capabilities implemented by an RCS adapter.
 *
 * This model is used by platform routing only and must not be exposed in
 * customer request or response contracts.
 */
public enum RcsChannelCapability {
    MESSAGE_SEND,
    CAPABILITY_QUERY_SYNC,
    CAPABILITY_QUERY_ASYNC,
    OUTBOUND_EVENT,
    SENDER_MANAGEMENT,
    TEST_NUMBER_MANAGEMENT,
    TEMPLATE_MANAGEMENT,
    SENDER_LAUNCH_STATUS,
    SENDER_LAUNCH_MANAGEMENT,
    MESSAGE_RECALL
}
