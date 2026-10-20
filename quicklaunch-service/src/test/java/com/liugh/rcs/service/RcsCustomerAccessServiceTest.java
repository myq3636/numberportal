package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RoutingInfo;
import com.liugh.rcs.service.RcsRoutingSnapshot;
import com.liugh.service.util.GlobalMap;

public class RcsCustomerAccessServiceTest {

    private final RcsCustomerAccessService accessService = new RcsCustomerAccessService();
    private AdminInfo customer;

    @Before
    public void setUp() {
        GlobalMap.admin.clear();
        GlobalMap.routingTable.clear();
        GlobalMap.rcsRoutingSnapshot = null;

        customer = new AdminInfo();
        customer.setId(100L);
        customer.setSsid(100);
        GlobalMap.admin.put("customer-token", customer);
    }

    @After
    public void tearDown() {
        GlobalMap.admin.clear();
        GlobalMap.routingTable.clear();
        GlobalMap.rcsRoutingSnapshot = null;
    }

    @Test
    public void resolvesExistingBodyTokenFields() {
        JSONObject upperCase = new JSONObject();
        upperCase.put("Token", "customer-token");
        assertSame(customer, accessService.requireCustomer(upperCase));

        JSONObject lowerCase = new JSONObject();
        lowerCase.put("token", "customer-token");
        assertSame(customer, accessService.requireCustomer(lowerCase));
    }

    @Test
    public void rejectsAllCustomerVisibleRoutingFields() {
        assertInvalidRoutingField("channel");
        assertInvalidRoutingField("channelCode");
        assertInvalidRoutingField("rssid");
    }

    @Test
    public void rejectsNestedCustomerVisibleRoutingFields() {
        final JSONObject nested = new JSONObject();
        nested.put("rssid", 901);
        JSONArray items = new JSONArray();
        items.add(nested);
        final JSONObject request = new JSONObject();
        request.put("items", items);

        assertAccessError("INVALID_ARGUMENT", new Runnable() {
            @Override
            public void run() {
                accessService.validateCustomerPayload(request);
            }
        });
    }

    @Test
    public void enforcesResourceTenantOwnership() {
        accessService.requireOwned(100, 100);
        assertAccessError("FORBIDDEN_RESOURCE", new Runnable() {
            @Override
            public void run() {
                accessService.requireOwned(100, 101);
            }
        });
        assertAccessError("RESOURCE_NOT_FOUND", new Runnable() {
            @Override
            public void run() {
                accessService.requireOwned(100, null);
            }
        });
    }

    @Test
    public void routerUsesPlatformRouteEvenIfInternalCallerPassesOverrideFields() {
        AdminInfo routedSupplier = supplier(200L, "INFOBIP");
        AdminInfo attemptedOverride = supplier(201L, "SINCH");
        GlobalMap.admin.put("200", routedSupplier);
        GlobalMap.admin.put("201", attemptedOverride);

        RoutingInfo common = new RoutingInfo();
        common.setStatus(0);
        common.setOssid(100);
        common.setRssid(200);
        common.setCountryCode("CN");
        common.setType(3);
        common.setRatio(100);
        List<RoutingInfo> infos = new ArrayList<RoutingInfo>();
        infos.add(common);
        GlobalMap.rcsRoutingSnapshot = RcsRoutingSnapshot.build(infos, rssid -> true);

        JSONObject request = new JSONObject();
        request.put("rssid", 201);
        request.put("channelCode", "SINCH");

        assertSame(routedSupplier, new RcsRouter().route(customer, "CN"));
    }

    private void assertInvalidRoutingField(String field) {
        final JSONObject request = new JSONObject();
        request.put(field, field.equals("rssid") ? 201 : "SINCH");
        assertAccessError("INVALID_ARGUMENT", new Runnable() {
            @Override
            public void run() {
                accessService.validateCustomerPayload(request);
            }
        });
    }

    private void assertAccessError(String code, Runnable action) {
        try {
            action.run();
            fail("Expected " + code);
        } catch (RcsCustomerAccessException e) {
            assertEquals(code, e.getErrorCode());
        }
    }

    private AdminInfo supplier(Long id, String channelCode) {
        AdminInfo supplier = new AdminInfo();
        supplier.setId(id);
        supplier.setStatus(0);
        supplier.setChannelCode(channelCode);
        return supplier;
    }
}
