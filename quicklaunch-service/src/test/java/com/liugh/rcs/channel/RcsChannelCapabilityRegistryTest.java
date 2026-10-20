package com.liugh.rcs.channel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.liugh.rcs.channel.infobip.InfobipChannelAdapter;

public class RcsChannelCapabilityRegistryTest {

    @Test
    public void infobipDeclaresItsRealPhaseOneCapabilities() {
        RcsChannelAdapter adapter = new InfobipChannelAdapter();

        assertTrue(adapter.supports(RcsChannelCapability.MESSAGE_SEND));
        assertTrue(adapter.supports(RcsChannelCapability.CAPABILITY_QUERY_SYNC));
        assertTrue(adapter.supports(RcsChannelCapability.CAPABILITY_QUERY_ASYNC));
        assertTrue(adapter.supports(RcsChannelCapability.OUTBOUND_EVENT));
        assertTrue(adapter.supports(RcsChannelCapability.SENDER_MANAGEMENT));
        assertTrue(adapter.supports(RcsChannelCapability.TEST_NUMBER_MANAGEMENT));
        assertTrue(adapter.supports(RcsChannelCapability.TEMPLATE_MANAGEMENT));
        assertTrue(adapter.supports(
                RcsChannelCapability.SENDER_LAUNCH_MANAGEMENT));
        assertFalse(adapter.supports(RcsChannelCapability.MESSAGE_RECALL));
    }

    @Test
    public void registryReturnsUnsupportedForUnknownInternalRoute() {
        RcsChannelCapabilityRegistry registry = new RcsChannelCapabilityRegistry(
                Arrays.<RcsChannelAdapter>asList(new InfobipChannelAdapter()));

        assertTrue(registry.supports("INFOBIP", RcsChannelCapability.MESSAGE_SEND));
        assertFalse(registry.supports("UNKNOWN", RcsChannelCapability.MESSAGE_SEND));
        assertFalse(registry.supports(null, RcsChannelCapability.MESSAGE_SEND));
    }
}
