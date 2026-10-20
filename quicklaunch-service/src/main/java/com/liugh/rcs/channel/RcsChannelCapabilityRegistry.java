package com.liugh.rcs.channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Internal capability lookup used after platform routing has selected an
 * adapter.
 */
@Component
public class RcsChannelCapabilityRegistry {

    private final Map<String, RcsChannelAdapter> adapters =
            new HashMap<String, RcsChannelAdapter>();

    @Autowired
    public RcsChannelCapabilityRegistry(List<RcsChannelAdapter> adapterList) {
        if (adapterList != null) {
            for (RcsChannelAdapter adapter : adapterList) {
                adapters.put(adapter.channelCode(), adapter);
            }
        }
    }

    public boolean supports(String internalChannelCode, RcsChannelCapability capability) {
        RcsChannelAdapter adapter = internalChannelCode == null
                ? null : adapters.get(internalChannelCode);
        return adapter != null && adapter.supports(capability);
    }
}
