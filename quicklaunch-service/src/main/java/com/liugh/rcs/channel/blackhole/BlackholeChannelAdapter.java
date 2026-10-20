package com.liugh.rcs.channel.blackhole;

import java.util.Collections;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.channel.RcsCallbackResult;
import com.liugh.rcs.channel.RcsChannelAdapter;
import com.liugh.rcs.channel.RcsChannelCapability;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.RcsContent;

/**
 * Blackhole 通道适配器：不发任何 HTTP 请求，send() 返回即视为投递成功。
 * RcsMessageService 检测到 isImmediateDelivery()==true 后会跳过
 * submit→callback 流程，直接将 CDR 推进为 status=3 并触发客户 DR 通知。
 *
 * 典型用途：压测占位、灰度流量吸收、开发/测试环境模拟成功投递。
 */
@Component
public class BlackholeChannelAdapter implements RcsChannelAdapter {

    public static final String CODE = "BLACKHOLE";

    @Override
    public String channelCode() {
        return CODE;
    }

    @Override
    public Set<RcsChannelCapability> capabilities() {
        return Collections.singleton(RcsChannelCapability.MESSAGE_SEND);
    }

    @Override
    public boolean isImmediateDelivery() {
        return true;
    }

    /** 无上游调用。返回带 BH- 前缀的合成 channelMsgId，便于日志追踪。 */
    @Override
    public String send(RcsContent content, RcsSendCtx ctx) {
        return "BH-" + ctx.getMsgId();
    }

    /** Blackhole 不接收真实回调，始终返回空结果。 */
    @Override
    public RcsCallbackResult parseCallback(JSONObject raw) {
        return new RcsCallbackResult();
    }
}
