package com.liugh.rcs.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsDeliveryConfig;
import com.liugh.mapper.RcsDeliveryConfigMapper;

/**
 * Maintains exactly one customer event delivery destination per tenant.
 */
@Service
public class RcsDeliveryConfigService {

    private final RcsCustomerAccessService accessService;
    private final RcsDeliveryConfigMapper mapper;
    private final RcsSecretCipher secretCipher;

    @Autowired
    public RcsDeliveryConfigService(RcsCustomerAccessService accessService,
            RcsDeliveryConfigMapper mapper, RcsSecretCipher secretCipher) {
        this.accessService = accessService;
        this.mapper = mapper;
        this.secretCipher = secretCipher;
    }

    public JSONObject save(JSONObject request) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(request);
        String eventUrl = request == null ? null
                : request.getString("eventUrl");
        validateHttpsUrl(eventUrl);
        Integer maxRetries = request.getInteger("maxRetries");
        if (maxRetries == null) {
            maxRetries = 5;
        }
        if (maxRetries < 0 || maxRetries > 10) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }

        RcsDeliveryConfig config = findBySsid(customer.getSsid());
        boolean create = config == null;
        if (create) {
            config = new RcsDeliveryConfig();
            config.setSsid(customer.getSsid());
            config.setCreateTime(new Date());
        }
        String signatureSecret = request.getString("signatureSecret");
        if (!isBlank(signatureSecret)) {
            config.setSignatureSecretCiphertext(
                    secretCipher.encrypt(signatureSecret));
        } else if (create
                || config.getSignatureSecretCiphertext() == null) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        config.setEventUrl(eventUrl.trim());
        config.setLegacyDrEnabled(Boolean.TRUE.equals(
                request.getBoolean("legacyDrEnabled")));
        config.setMaxRetries(maxRetries);
        config.setEnabled(!request.containsKey("enabled")
                || Boolean.TRUE.equals(request.getBoolean("enabled")));
        config.setUpdateTime(new Date());
        if (create) {
            mapper.insert(config);
        } else {
            mapper.updateById(config);
        }
        return customerView(config);
    }

    public JSONObject get(JSONObject request) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(request);
        RcsDeliveryConfig config = findBySsid(customer.getSsid());
        if (config == null) {
            JSONObject empty = new JSONObject();
            empty.put("configured", false);
            return empty;
        }
        return customerView(config);
    }

    public RcsDeliveryConfig findBySsid(Integer ssid) {
        return first(mapper.selectList(new EntityWrapper<RcsDeliveryConfig>()
                .eq("ssid", ssid)));
    }

    public String decryptSecret(RcsDeliveryConfig config) {
        return secretCipher.decrypt(config.getSignatureSecretCiphertext());
    }

    private JSONObject customerView(RcsDeliveryConfig config) {
        JSONObject result = new JSONObject();
        result.put("configured", true);
        result.put("eventUrl", config.getEventUrl());
        result.put("signatureConfigured",
                config.getSignatureSecretCiphertext() != null
                        && config.getSignatureSecretCiphertext().length > 0);
        result.put("legacyDrEnabled", config.getLegacyDrEnabled());
        result.put("maxRetries", config.getMaxRetries());
        result.put("enabled", config.getEnabled());
        result.put("updatedAt", config.getUpdateTime());
        return result;
    }

    private void validateHttpsUrl(String eventUrl) {
        try {
            RcsDeliveryUrlValidator.requirePublicHttps(eventUrl, false);
        } catch (IllegalArgumentException e) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
