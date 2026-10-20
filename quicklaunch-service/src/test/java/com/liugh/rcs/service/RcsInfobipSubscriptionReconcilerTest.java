package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.rcs.channel.infobip.InfobipManagementHttpException;
import com.liugh.rcs.channel.infobip.InfobipSubscriptionClient;
import com.liugh.service.util.GlobalMap;

public class RcsInfobipSubscriptionReconcilerTest {

    private InfobipSubscriptionClient client;
    private RcsChannelAgentMapper bindingMapper;
    private RcsInfobipSubscriptionReconciler reconciler;
    private AdminInfo supplier;

    @Before
    public void setUp() {
        GlobalMap.admin.clear();
        client = mock(InfobipSubscriptionClient.class);
        bindingMapper = mock(RcsChannelAgentMapper.class);
        reconciler = new RcsInfobipSubscriptionReconciler(client,
                bindingMapper);
        reconciler.configure(true, "https://platform.example/api/v1/",
                "callback-secret");

        supplier = new AdminInfo();
        supplier.setId(901L);
        supplier.setStatus(0);
        supplier.setChannelCode("INFOBIP");
        supplier.setUrl("https://tenant.api.infobip.com");
        supplier.setAppId("api-key");
        GlobalMap.admin.put("901", supplier);
    }

    @After
    public void tearDown() {
        GlobalMap.admin.clear();
    }

    @Test
    public void skipsWhenFeatureIsDisabled() {
        reconciler.configure(false, "https://platform.example/api/v1/",
                "callback-secret");

        reconciler.reconcileAll();

        verify(bindingMapper, never()).selectList(any());
        verify(client, never()).getProfile(any(String.class),
                any(String.class), any(String.class));
    }

    @Test
    public void skipsWhenSecureCallbackPrerequisitesAreMissing() {
        reconciler.configure(true, "http://platform.example/api/v1/", "");

        reconciler.reconcileAll();

        verify(bindingMapper, never()).selectList(any());
    }

    @Test
    public void createsMissingProfilesAndSubscriptionsOncePerSupplier() {
        when(bindingMapper.selectList(any())).thenReturn(Arrays.asList(
                binding(100), binding(200)));
        when(client.getProfile(any(String.class), any(String.class),
                any(String.class))).thenThrow(notFound());
        when(client.getSubscription(any(String.class), any(String.class),
                any(String.class), any(String.class))).thenThrow(notFound());

        reconciler.reconcileAll();

        verify(client).createProfile(eq(supplier.getUrl()),
                eq(supplier.getAppId()), any(JSONObject.class));
        verify(client).createSubscription(eq(supplier.getUrl()),
                eq(supplier.getAppId()), eq("RCS"), any(JSONObject.class));
        verify(client).createSubscription(eq(supplier.getUrl()),
                eq(supplier.getAppId()), eq("REGISTRATION"),
                any(JSONObject.class));
        ArgumentCaptor<JSONObject> rcsSubscription =
                ArgumentCaptor.forClass(JSONObject.class);
        verify(client).createSubscription(eq(supplier.getUrl()),
                eq(supplier.getAppId()), eq("RCS"),
                rcsSubscription.capture());
        assertFalse(rcsSubscription.getValue().getJSONArray("events")
                .contains("DELIVERY"));
        assertFalse(rcsSubscription.getValue().getJSONArray("events")
                .contains("SEEN"));
        assertFalse(rcsSubscription.getValue().getJSONArray("events")
                .contains("CAP_CHECK"));

        ArgumentCaptor<JSONObject> profile =
                ArgumentCaptor.forClass(JSONObject.class);
        verify(client).createProfile(eq(supplier.getUrl()),
                eq(supplier.getAppId()), profile.capture());
        assertEquals("a2p-profile-901",
                profile.getValue().getString("profileId"));
        assertEquals("https://platform.example/api/v1/rcs/callback/"
                        + "infobip/subscriptions/901",
                profile.getValue().getJSONObject("webhook")
                        .getString("notifyUrl"));
        assertEquals("callback-secret",
                profile.getValue().getJSONObject("headers")
                        .getString("X-RCS-Subscription-Token"));
    }

    @Test
    public void updatesExistingRemoteConfiguration() {
        when(bindingMapper.selectList(any())).thenReturn(
                Arrays.asList(binding(100)));
        when(client.getProfile(any(String.class), any(String.class),
                any(String.class))).thenReturn(new JSONObject());
        when(client.getSubscription(any(String.class), any(String.class),
                any(String.class), any(String.class)))
                .thenReturn(new JSONObject());

        reconciler.reconcileAll();

        verify(client).updateProfile(eq(supplier.getUrl()),
                eq(supplier.getAppId()), eq("a2p-profile-901"),
                any(JSONObject.class));
        verify(client).updateSubscription(eq(supplier.getUrl()),
                eq(supplier.getAppId()), eq("RCS"), eq("a2p-rcs-901"),
                any(JSONObject.class));
        verify(client).updateSubscription(eq(supplier.getUrl()),
                eq(supplier.getAppId()), eq("REGISTRATION"),
                eq("a2p-registration-901"), any(JSONObject.class));
    }

    private RcsChannelAgent binding(int ssid) {
        RcsChannelAgent value = new RcsChannelAgent();
        value.setSsid(ssid);
        value.setRssid(901);
        value.setChannelCode("INFOBIP");
        value.setStatus(1);
        return value;
    }

    private InfobipManagementHttpException notFound() {
        return new InfobipManagementHttpException(404, "not found");
    }
}
