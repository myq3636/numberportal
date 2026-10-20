package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsDeliveryConfig;
import com.liugh.mapper.RcsDeliveryConfigMapper;

public class RcsDeliveryConfigServiceTest {

    private RcsCustomerAccessService accessService;
    private RcsDeliveryConfigMapper mapper;
    private RcsDeliveryConfigService service;
    private AdminInfo customer;

    @Before
    public void setUp() {
        accessService = mock(RcsCustomerAccessService.class);
        mapper = mock(RcsDeliveryConfigMapper.class);
        byte[] key = "0123456789abcdef0123456789abcdef"
                .getBytes(StandardCharsets.UTF_8);
        service = new RcsDeliveryConfigService(accessService, mapper,
                new RcsSecretCipher(key));
        customer = new AdminInfo();
        customer.setSsid(100);
        when(accessService.requireCustomer(any(JSONObject.class))).thenReturn(customer);
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    @Test
    public void storesSingleEncryptedDeliveryConfiguration() {
        JSONObject result = service.save(request(
                "https://customer.example.com/rcs/events", "customer-secret"));

        ArgumentCaptor<RcsDeliveryConfig> captor =
                ArgumentCaptor.forClass(RcsDeliveryConfig.class);
        verify(mapper).insert(captor.capture());
        RcsDeliveryConfig stored = captor.getValue();
        assertEquals(Integer.valueOf(100), stored.getSsid());
        assertFalse(new String(stored.getSignatureSecretCiphertext(),
                StandardCharsets.UTF_8).contains("customer-secret"));
        assertFalse(stored.toString().contains("customer-secret"));
        assertFalse(result.containsKey("signatureSecret"));
        assertEquals(Boolean.TRUE, result.getBoolean("signatureConfigured"));
    }

    @Test
    public void updatesExistingTenantRowInsteadOfCreatingSecondAddress() {
        RcsDeliveryConfig existing = new RcsDeliveryConfig();
        existing.setId(7L);
        existing.setSsid(100);
        when(mapper.selectList(any()))
                .thenReturn(Collections.singletonList(existing));

        service.save(request("https://new.example.com/events", "new-secret"));

        verify(mapper).updateById(existing);
        verify(mapper, never()).insert(any(RcsDeliveryConfig.class));
        assertEquals("https://new.example.com/events", existing.getEventUrl());
    }

    @Test
    public void rejectsNonHttpsEventAddress() {
        try {
            service.save(request("http://customer.example.com/events", "secret"));
            fail("Expected INVALID_ARGUMENT");
        } catch (RcsCustomerAccessException e) {
            assertEquals("INVALID_ARGUMENT", e.getErrorCode());
        }
        verify(mapper, never()).insert(any(RcsDeliveryConfig.class));
    }

    @Test
    public void rejectsPrivateNetworkEventAddress() {
        try {
            service.save(request("https://127.0.0.1/events", "secret"));
            fail("Expected INVALID_ARGUMENT");
        } catch (RcsCustomerAccessException e) {
            assertEquals("INVALID_ARGUMENT", e.getErrorCode());
        }
        verify(mapper, never()).insert(any(RcsDeliveryConfig.class));
    }

    private JSONObject request(String eventUrl, String secret) {
        JSONObject request = new JSONObject();
        request.put("Token", "customer-token");
        request.put("eventUrl", eventUrl);
        request.put("signatureSecret", secret);
        request.put("legacyDrEnabled", false);
        request.put("maxRetries", 5);
        request.put("enabled", true);
        return request;
    }
}
