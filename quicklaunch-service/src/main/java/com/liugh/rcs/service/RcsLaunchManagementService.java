package com.liugh.rcs.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsLaunchRequest;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.mapper.RcsLaunchRequestMapper;
import com.liugh.rcs.channel.infobip.InfobipRcsManagementClient;

/**
 * Tenant-safe facade over Infobip Resource Request API for RCS sender launch.
 */
@Service
public class RcsLaunchManagementService {

    private static final String INFOBIP = "INFOBIP";

    private final RcsCustomerAccessService access;
    private final RcsRouter router;
    private final InfobipRcsManagementClient infobip;
    private final RcsChannelAgentMapper bindingMapper;
    private final RcsLaunchRequestMapper launchMapper;

    @Autowired
    public RcsLaunchManagementService(RcsCustomerAccessService access,
            RcsRouter router, InfobipRcsManagementClient infobip,
            RcsChannelAgentMapper bindingMapper,
            RcsLaunchRequestMapper launchMapper) {
        this.access = access;
        this.router = router;
        this.infobip = infobip;
        this.bindingMapper = bindingMapper;
        this.launchMapper = launchMapper;
    }

    public JSONObject requirements(JSONObject request, String senderId) {
        final SenderContext c = ownedSender(request, senderId);
        final JSONObject body = cleanBody(request);
        body.put("type", "RCS_SENDER_LAUNCH");
        return customerJson(invoke(new Call() {
            @Override public JSONObject execute() {
                return infobip.lookupResourceRequirements(c.supplier.getUrl(),
                        c.supplier.getAppId(), body);
            }
        }));
    }

    public JSONObject uploadDocument(JSONObject auth, String senderId,
            final byte[] bytes, final String fileName,
            final String contentType) {
        final SenderContext c = ownedSender(auth, senderId);
        if (bytes == null || bytes.length == 0) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        return customerJson(invoke(new Call() {
            @Override public JSONObject execute() {
                return infobip.uploadResourceDocument(c.supplier.getUrl(),
                        c.supplier.getAppId(), fileName, contentType, bytes);
            }
        }));
    }

    public JSONObject submit(JSONObject request, String senderId) {
        final AdminInfo customer = requireCustomer(request);
        final SenderContext c = ownedSender(customer, senderId);
        final JSONObject body = cleanBody(request);
        body.put("type", "RCS_SENDER_LAUNCH");
        body.put("senderName", c.binding.getSender());
        JSONObject upstream = invoke(new Call() {
            @Override public JSONObject execute() {
                return infobip.submitResourceRequest(c.supplier.getUrl(),
                        c.supplier.getAppId(), body);
            }
        });
        String upstreamId = upstream.getString("id");
        if (isBlank(upstreamId)) {
            throw new RcsCustomerAccessException("UPSTREAM_REQUEST_REJECTED");
        }

        Date now = new Date();
        RcsLaunchRequest stored = new RcsLaunchRequest();
        stored.setRequestId(newId("launch_"));
        stored.setSsid(customer.getSsid());
        stored.setRssid(c.binding.getRssid());
        stored.setSenderId(c.binding.getAgentId());
        stored.setUpstreamRequestId(upstreamId);
        stored.setStatus(upstream.getString("status"));
        stored.setCreateTime(now);
        stored.setUpdateTime(now);
        if (launchMapper.insert(stored) != 1) {
            throw new RcsCustomerAccessException("INTERNAL_ERROR");
        }

        JSONObject result = customerJson(upstream);
        result.remove("id");
        result.put("requestId", stored.getRequestId());
        result.put("senderId", stored.getSenderId());
        return result;
    }

    public JSONObject list(JSONObject auth, String senderId) {
        AdminInfo customer = requireCustomer(auth);
        ownedSender(customer, senderId);
        List<RcsLaunchRequest> stored = launchMapper.selectList(
                new EntityWrapper<RcsLaunchRequest>()
                        .eq("ssid", customer.getSsid())
                        .eq("sender_id", senderId));
        JSONArray requests = new JSONArray();
        if (stored != null) {
            for (RcsLaunchRequest item : stored) {
                requests.add(getOwned(customer, item));
            }
        }
        JSONObject result = new JSONObject();
        result.put("requests", requests);
        return result;
    }

    public JSONObject get(JSONObject auth, String requestId) {
        AdminInfo customer = requireCustomer(auth);
        return getOwned(customer, ownedRequest(customer, requestId));
    }

    public JSONObject update(JSONObject request, String requestId) {
        final AdminInfo customer = requireCustomer(request);
        final RcsLaunchRequest stored = ownedRequest(customer, requestId);
        final SenderContext c = ownedSender(customer, stored.getSenderId());
        final JSONObject body = cleanBody(request);
        JSONObject upstream = customerJson(invoke(new Call() {
            @Override public JSONObject execute() {
                return infobip.updateResourceRequest(c.supplier.getUrl(),
                        c.supplier.getAppId(), stored.getUpstreamRequestId(),
                        body);
            }
        }));
        updateStatus(stored, upstream.getString("status"));
        return requestView(stored, upstream);
    }

    public JSONObject cancel(JSONObject auth, String requestId) {
        final AdminInfo customer = requireCustomer(auth);
        final RcsLaunchRequest stored = ownedRequest(customer, requestId);
        final SenderContext c = ownedSender(customer, stored.getSenderId());
        JSONObject upstream = customerJson(invoke(new Call() {
            @Override public JSONObject execute() {
                return infobip.cancelResourceRequest(c.supplier.getUrl(),
                        c.supplier.getAppId(), stored.getUpstreamRequestId());
            }
        }));
        updateStatus(stored, firstNonBlank(upstream.getString("status"),
                "CANCELED"));
        return requestView(stored, upstream);
    }

    private JSONObject getOwned(AdminInfo customer,
            final RcsLaunchRequest stored) {
        final SenderContext c = ownedSender(customer, stored.getSenderId());
        JSONObject upstream = customerJson(invoke(new Call() {
            @Override public JSONObject execute() {
                return infobip.getResourceRequest(c.supplier.getUrl(),
                        c.supplier.getAppId(), stored.getUpstreamRequestId());
            }
        }));
        updateStatus(stored, upstream.getString("status"));
        return requestView(stored, upstream);
    }

    private JSONObject requestView(RcsLaunchRequest stored,
            JSONObject upstream) {
        JSONObject result = customerJson(upstream);
        result.remove("id");
        result.put("requestId", stored.getRequestId());
        result.put("senderId", stored.getSenderId());
        return result;
    }

    private void updateStatus(RcsLaunchRequest stored, String status) {
        if (!isBlank(status) && !status.equals(stored.getStatus())) {
            stored.setStatus(status);
            stored.setUpdateTime(new Date());
            launchMapper.updateById(stored);
        }
    }

    private RcsLaunchRequest ownedRequest(AdminInfo customer, String requestId) {
        if (isBlank(requestId)) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        RcsLaunchRequest stored = first(launchMapper.selectList(
                new EntityWrapper<RcsLaunchRequest>()
                        .eq("request_id", requestId)));
        access.requireOwned(customer.getSsid(),
                stored == null ? null : stored.getSsid());
        return stored;
    }

    private SenderContext ownedSender(JSONObject auth, String senderId) {
        return ownedSender(requireCustomer(auth), senderId);
    }

    private SenderContext ownedSender(AdminInfo customer, String senderId) {
        if (isBlank(senderId)) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        RcsChannelAgent binding = first(bindingMapper.selectList(
                new EntityWrapper<RcsChannelAgent>()
                        .eq("agent_id", senderId)
                        .eq("status", 1)));
        access.requireOwned(customer.getSsid(),
                binding == null ? null : binding.getSsid());
        AdminInfo supplier = router.resolveSupplier(binding);
        if (supplier == null || !INFOBIP.equalsIgnoreCase(
                supplier.getChannelCode())) {
            throw new RcsCustomerAccessException("OPERATION_NOT_SUPPORTED");
        }
        if (isBlank(supplier.getUrl()) || isBlank(supplier.getAppId())) {
            throw new RcsCustomerAccessException("ROUTE_NOT_CONFIGURED");
        }
        return new SenderContext(binding, supplier);
    }

    private AdminInfo requireCustomer(JSONObject request) {
        access.requirePhase1Enabled();
        return access.requireCustomer(request);
    }

    private JSONObject invoke(Call call) {
        try {
            JSONObject value = call.execute();
            return value == null ? new JSONObject() : value;
        } catch (RcsCustomerAccessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        } catch (Exception e) {
            throw new RcsCustomerAccessException("UPSTREAM_REQUEST_REJECTED");
        }
    }

    private static JSONObject cleanBody(JSONObject request) {
        JSONObject value = request == null ? new JSONObject()
                : JSONObject.parseObject(request.toJSONString());
        remove(value, "Token");
        remove(value, "token");
        remove(value, "senderId");
        remove(value, "requestId");
        remove(value, "channel");
        remove(value, "channelCode");
        remove(value, "rssid");
        remove(value, "platform");
        return value;
    }

    private static JSONObject customerJson(JSONObject source) {
        JSONObject value = source == null ? new JSONObject()
                : JSONObject.parseObject(source.toJSONString());
        sanitize(value);
        return value;
    }

    private static void sanitize(Object value) {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            for (String key : new String[]{"channel", "channelCode", "rssid",
                    "apiKey", "authorization", "appId", "appSecurity",
                    "platform"}) {
                remove(object, key);
            }
            for (Object child : object.values()) {
                sanitize(child);
            }
        } else if (value instanceof JSONArray) {
            JSONArray values = (JSONArray) value;
            for (int i = 0; i < values.size(); i++) {
                sanitize(values.get(i));
            }
        }
    }

    private static void remove(JSONObject value, String name) {
        String found = null;
        for (String key : value.keySet()) {
            if (name.equalsIgnoreCase(key)) {
                found = key;
                break;
            }
        }
        if (found != null) {
            value.remove(found);
        }
    }

    private static String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private static String newId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private interface Call {
        JSONObject execute();
    }

    private static final class SenderContext {
        private final RcsChannelAgent binding;
        private final AdminInfo supplier;

        private SenderContext(RcsChannelAgent binding, AdminInfo supplier) {
            this.binding = binding;
            this.supplier = supplier;
        }
    }
}
