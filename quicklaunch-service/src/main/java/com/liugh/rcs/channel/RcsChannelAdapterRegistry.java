package com.liugh.rcs.channel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 通道适配器注册表：Spring 注入所有 RcsChannelAdapter 实现，按 channelCode 建索引。
 */
@Component
public class RcsChannelAdapterRegistry {

    private final Map<String, RcsChannelAdapter> adapters = new HashMap<String, RcsChannelAdapter>();

    @Autowired
    public RcsChannelAdapterRegistry(List<RcsChannelAdapter> list) {
        if (list != null) {
            for (RcsChannelAdapter a : list) {
                adapters.put(a.channelCode(), a);
            }
        }
    }

    public RcsChannelAdapter get(String channelCode) {
        return channelCode == null ? null : adapters.get(channelCode);
    }

    /** 已注册的全部通道编码。 */
    public Set<String> channelCodes() {
        return Collections.unmodifiableSet(adapters.keySet());
    }
}
