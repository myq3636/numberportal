package com.liugh.rcs.channel;

import java.util.Set;

import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.model.RcsContent;

/**
 * RCS 通道适配器 SPI。每个聚合商（Infobip / Route Mobile / Sinch...）实现一份。
 * 只负责三件与通道强相关的事：造报文、发 HTTP、解回调。
 * 鉴权、路由、写 CDR、计费、DR 回推等公共编排在 RcsMessageService / RcsCallbackService。
 */
public interface RcsChannelAdapter {

    /** 通道编码，与 rcs_channel.channel_code 一致（INFOBIP / ROUTEMOBILE ...）。 */
    String channelCode();

    /** Internal capabilities used by platform routing and orchestration. */
    Set<RcsChannelCapability> capabilities();

    /** Whether this adapter has a real implementation for the capability. */
    default boolean supports(RcsChannelCapability capability) {
        return capability != null && capabilities().contains(capability);
    }

    /**
     * 发送一条消息。
     *
     * @return 通道返回的 messageId（channelMsgId）；失败返回 null。
     */
    String send(RcsContent content, RcsSendCtx ctx) throws Exception;

    /**
     * 解析一条通道回调（原始 JSON）为归一化的下行/上行事件。不落库。
     */
    RcsCallbackResult parseCallback(JSONObject raw);

    /**
     * 该通道是否在 send() 返回时就已完成投递，无需等待异步 callback。
     * 返回 true 时，RcsMessageService 会跳过 submit→callback 流程，
     * 直接将 CDR 推进为 status=3（已送达）并触发客户 DR 通知。
     */
    default boolean isImmediateDelivery() {
        return false;
    }
}
