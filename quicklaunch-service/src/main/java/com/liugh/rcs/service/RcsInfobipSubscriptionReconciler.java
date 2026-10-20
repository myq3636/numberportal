package com.liugh.rcs.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.rcs.channel.infobip.InfobipManagementHttpException;
import com.liugh.rcs.channel.infobip.InfobipSubscriptionClient;
import com.liugh.service.util.GlobalMap;

import lombok.extern.slf4j.Slf4j;

/**
 * Reconciles account-level Infobip subscriptions for active local senders.
 */
@Service
@Slf4j
public class RcsInfobipSubscriptionReconciler {

    private static final String INFOBIP = "INFOBIP";
    private static final String TOKEN_HEADER = "X-RCS-Subscription-Token";

    private final InfobipSubscriptionClient client;
    private final RcsChannelAgentMapper bindingMapper;

    @Value("${rcs.infobip-subscriptions.enabled:false}")
    private boolean enabled;
    @Value("${project.url:}")
    private String projectUrl;
    @Value("${rcs.infobip-subscriptions.callback-token:}")
    private String callbackToken;

    @Autowired
    public RcsInfobipSubscriptionReconciler(InfobipSubscriptionClient client,
            RcsChannelAgentMapper bindingMapper) {
        this.client = client;
        this.bindingMapper = bindingMapper;
    }

    /**
     * Test and administrative entry point. No remote request is made unless all
     * safety prerequisites are satisfied.
     */
    public void reconcileAll() {
        if (!enabled) {
            return;
        }
        if (!isHttps(projectUrl) || isBlank(callbackToken)) {
            log.warn("Infobip subscription reconciliation skipped: secure "
                    + "project URL and callback token are required");
            return;
        }

        List<RcsChannelAgent> bindings = bindingMapper.selectList(
                new EntityWrapper<RcsChannelAgent>()
                        .eq("channel_code", INFOBIP)
                        .eq("status", 1));
        Set<Integer> supplierIds = new HashSet<Integer>();
        if (bindings != null) {
            for (RcsChannelAgent binding : bindings) {
                if (binding != null && binding.getRssid() != null) {
                    supplierIds.add(binding.getRssid());
                }
            }
        }
        for (Integer rssid : supplierIds) {
            try {
                reconcileSupplier(rssid);
            } catch (Exception e) {
                log.error("Infobip subscription reconciliation failed, rssid:{}",
                        rssid, e);
            }
        }
    }

    @Scheduled(fixedDelayString =
            "${rcs.infobip-subscriptions.scan-ms:300000}")
    public void scheduledReconcile() {
        reconcileAll();
    }

    public void configure(boolean enabled, String projectUrl,
            String callbackToken) {
        this.enabled = enabled;
        this.projectUrl = projectUrl;
        this.callbackToken = callbackToken;
    }

    private void reconcileSupplier(Integer rssid) {
        AdminInfo supplier = GlobalMap.rcsadmin.get(String.valueOf(rssid));
        if (!usableInfobipSupplier(supplier)) {
            log.warn("Infobip subscription skipped: invalid supplier account, "
                    + "rssid:{}", rssid);
            return;
        }

        String suffix = String.valueOf(rssid);
        String profileId = "a2p-profile-" + suffix;
        String callbackUrl = trimTrailingSlash(projectUrl)
                + "/rcs/callback/infobip/subscriptions/" + suffix;
        JSONObject profile = profile(profileId, callbackUrl);
        reconcileProfile(supplier, profileId, profile);
        reconcileSubscription(supplier, "RCS", "a2p-rcs-" + suffix,
                profileId, rcsEvents());
        reconcileSubscription(supplier, "REGISTRATION",
                "a2p-registration-" + suffix, profileId,
                registrationEvents());
    }

    private void reconcileProfile(AdminInfo supplier, String profileId,
            JSONObject body) {
        try {
            client.getProfile(supplier.getUrl(), supplier.getAppId(),
                    profileId);
            client.updateProfile(supplier.getUrl(), supplier.getAppId(),
                    profileId, body);
        } catch (InfobipManagementHttpException e) {
            if (e.getStatusCode() != 404) {
                throw e;
            }
            client.createProfile(supplier.getUrl(), supplier.getAppId(), body);
        }
    }

    private void reconcileSubscription(AdminInfo supplier, String channel,
            String subscriptionId, String profileId, JSONArray events) {
        JSONObject body = new JSONObject();
        body.put("subscriptionId", subscriptionId);
        body.put("events", events);
        JSONObject profile = new JSONObject();
        profile.put("profileId", profileId);
        body.put("profile", profile);
        try {
            client.getSubscription(supplier.getUrl(), supplier.getAppId(),
                    channel, subscriptionId);
            client.updateSubscription(supplier.getUrl(), supplier.getAppId(),
                    channel, subscriptionId, body);
        } catch (InfobipManagementHttpException e) {
            if (e.getStatusCode() != 404) {
                throw e;
            }
            client.createSubscription(supplier.getUrl(), supplier.getAppId(),
                    channel, body);
        }
    }

    private JSONObject profile(String profileId, String callbackUrl) {
        JSONObject body = new JSONObject();
        body.put("profileId", profileId);
        JSONObject webhook = new JSONObject();
        webhook.put("notifyUrl", callbackUrl);
        body.put("webhook", webhook);
        JSONObject headers = new JSONObject();
        headers.put(TOKEN_HEADER, callbackToken);
        body.put("headers", headers);
        JSONArray protocols = new JSONArray();
        protocols.add("TLSv1.2");
        protocols.add("TLSv1.3");
        body.put("enabledSSLProtocols", protocols);
        return body;
    }

    private JSONArray rcsEvents() {
        JSONArray values = new JSONArray();
        for (String value : new String[]{"CLICK", "CONVERSATION_STARTED",
                "INBOUND_MESSAGE", "SENDER_LAUNCH_UPDATE", "SENDER_UPDATE",
                "SUGGESTION", "TEMPLATE_UPDATE", "TEST_NUMBER_UPDATE",
                "TYPING_INDICATOR"}) {
            values.add(value);
        }
        return values;
    }

    private JSONArray registrationEvents() {
        JSONArray values = new JSONArray();
        values.add("RESOURCE_REQUEST_STATUS_UPDATE");
        values.add("RESOURCE_REQUEST_REMINDER");
        values.add("RESOURCE_REQUEST_FOC_DATE_CHANGE");
        return values;
    }

    private static boolean usableInfobipSupplier(AdminInfo supplier) {
        return supplier != null
                && INFOBIP.equalsIgnoreCase(supplier.getChannelCode())
                && (supplier.getStatus() == null || supplier.getStatus() == 0)
                && !isBlank(supplier.getUrl())
                && !isBlank(supplier.getAppId());
    }

    private static boolean isHttps(String value) {
        return !isBlank(value)
                && value.trim().toLowerCase().startsWith("https://");
    }

    private static String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
