package com.liugh.rcs.channel;

import java.util.ArrayList;
import java.util.List;

import com.liugh.entity.RcsMoInfo;

/**
 * 适配器解析一条通道回调后的归一化结果：下行回执 + 上行消息。
 */
public class RcsCallbackResult {

    private final List<RcsDrEvent> drEvents = new ArrayList<RcsDrEvent>();
    private final List<RcsMoInfo> inboundEvents = new ArrayList<RcsMoInfo>();

    public List<RcsDrEvent> getDrEvents() {
        return drEvents;
    }

    public List<RcsMoInfo> getInboundEvents() {
        return inboundEvents;
    }
}
