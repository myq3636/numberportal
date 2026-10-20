package com.liugh.rcs.service;

import java.util.List;
import java.util.UUID;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsTemplateBinding;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.mapper.RcsTemplateBindingMapper;
import com.liugh.rcs.channel.infobip.InfobipRcsManagementClient;

/**
 * Unified customer-facing sender resource management.
 *
 * Customer contracts contain no provider selection. New senders use the
 * platform country route once, then all child-resource operations use the
 * persisted supplier binding.
 */
@Service
public class RcsSenderManagementService {

    private static final String INFOBIP = "INFOBIP";

    private final RcsCustomerAccessService accessService;
    private final RcsRouter router;
    private final InfobipRcsManagementClient infobip;
    private final RcsChannelAgentMapper bindingMapper;
    private final RcsTemplateBindingMapper templateBindingMapper;

    @Autowired
    public RcsSenderManagementService(RcsCustomerAccessService accessService,
            RcsRouter router, InfobipRcsManagementClient infobip,
            RcsChannelAgentMapper bindingMapper,
            RcsTemplateBindingMapper templateBindingMapper) {
        this.accessService = accessService;
        this.router = router;
        this.infobip = infobip;
        this.bindingMapper = bindingMapper;
        this.templateBindingMapper = templateBindingMapper;
    }

    public JSONObject createSender(JSONObject request) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(request);
        String country = request == null ? null : request.getString("country");
        String senderName = request == null
                ? null : request.getString("senderName");
        if (isBlank(country) || isBlank(senderName)) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        AdminInfo supplier = router.route(customer, country.trim().toUpperCase());
        requireInfobip(supplier);

        JSONObject upstream = invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.createSender(supplier.getUrl(),
                        supplier.getAppId(), upstreamBody(request));
            }
        });
        String resolvedSender = firstNonBlank(upstream.getString("senderName"),
                senderName);
        RcsChannelAgent binding = new RcsChannelAgent();
        binding.setAgentId(newId("snd_"));
        binding.setSsid(customer.getSsid());
        binding.setRssid(toInt(supplier.getId()));
        binding.setChannelCode(INFOBIP);
        binding.setSender(resolvedSender);
        binding.setStatus(1);
        if (bindingMapper.insert(binding) != 1) {
            throw new RcsCustomerAccessException("INTERNAL_ERROR");
        }

        JSONObject result = customerJson(upstream);
        result.put("senderId", binding.getAgentId());
        if (!result.containsKey("senderName")) {
            result.put("senderName", resolvedSender);
        }
        return result;
    }

    public JSONObject listSenders(JSONObject auth) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(auth);
        List<RcsChannelAgent> bindings = bindingMapper.selectList(
                new EntityWrapper<RcsChannelAgent>()
                        .eq("ssid", customer.getSsid())
                        .eq("status", 1));
        JSONArray values = new JSONArray();
        if (bindings != null) {
            for (RcsChannelAgent binding : bindings) {
                AdminInfo supplier = boundSupplier(binding);
                JSONObject value = getUpstreamSender(supplier, binding);
                value.put("senderId", binding.getAgentId());
                values.add(value);
            }
        }
        JSONObject result = new JSONObject();
        result.put("senders", values);
        return result;
    }

    public JSONObject getSender(JSONObject auth, String senderId) {
        SenderContext context = ownedSender(auth, senderId);
        JSONObject result = getUpstreamSender(context.supplier, context.binding);
        result.put("senderId", context.binding.getAgentId());
        return result;
    }

    public JSONObject updateSender(JSONObject request, String senderId) {
        final SenderContext context = ownedSender(request, senderId);
        final JSONObject body = upstreamBody(request);
        JSONObject result = invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.updateSender(context.supplier.getUrl(),
                        context.supplier.getAppId(),
                        context.binding.getSender(), body);
            }
        });
        result = customerJson(result);
        result.put("senderId", context.binding.getAgentId());
        return result;
    }

    public JSONObject getSenderStatus(JSONObject auth, String senderId) {
        return getSender(auth, senderId);
    }

    public JSONObject updateSenderPlatformParameters(JSONObject request,
            String senderId) {
        final SenderContext c = ownedSender(request, senderId);
        final JSONObject body = upstreamBody(request);
        JSONObject result = customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.updateSenderPlatformParameters(
                        c.supplier.getUrl(), c.supplier.getAppId(),
                        c.binding.getSender(), body);
            }
        }));
        result.put("senderId", c.binding.getAgentId());
        return result;
    }

    public JSONObject getProviders(JSONObject auth, String country) {
        AdminInfo customer = accessService.requireCustomer(auth);
        if (isBlank(country)) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        final AdminInfo supplier = router.route(customer,
                country.trim().toUpperCase());
        requireInfobip(supplier);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.getProviders(supplier.getUrl(),
                        supplier.getAppId(), country.trim().toUpperCase());
            }
        }));
    }

    public JSONObject addTestNumber(JSONObject request, String senderId) {
        final SenderContext c = ownedSender(request, senderId);
        final JSONObject body = upstreamBody(request);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.addTestNumber(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), body);
            }
        }));
    }

    public JSONObject listTestNumbers(JSONObject auth, String senderId) {
        final SenderContext c = ownedSender(auth, senderId);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.listTestNumbers(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender());
            }
        }));
    }

    public JSONObject getTestNumber(JSONObject auth, String senderId,
            final String number) {
        final SenderContext c = ownedSender(auth, senderId);
        requireValue(number);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.getTestNumber(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), number);
            }
        }));
    }

    public JSONObject refreshTestNumber(JSONObject auth, String senderId,
            final String number) {
        final SenderContext c = ownedSender(auth, senderId);
        requireValue(number);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.refreshTestNumber(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), number);
            }
        }));
    }

    public JSONObject setPrimaryTestNumber(JSONObject request, String senderId,
            final String number) {
        final SenderContext c = ownedSender(request, senderId);
        final boolean primary = request == null
                || request.getBoolean("primary") == null
                || request.getBooleanValue("primary");
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.updatePrimaryTestNumber(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), number,
                        primary);
            }
        }));
    }

    public JSONObject getTestNumberRequestStatus(JSONObject auth,
            String senderId, final String number) {
        final SenderContext c = ownedSender(auth, senderId);
        requireValue(number);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.getTestNumberRequestStatus(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), number);
            }
        }));
    }

    public JSONObject createTemplate(JSONObject request, String senderId) {
        final SenderContext c = ownedSender(request, senderId);
        final JSONObject body = upstreamBody(request);
        final String requestedName = firstNonBlank(body.getString("name"),
                body.getString("templateName"));
        if (isBlank(requestedName)) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        RcsTemplateBinding existing = findTemplateBinding(c.binding.getRssid(),
                requestedName);
        if (existing != null
                && (!same(existing.getSsid(), c.binding.getSsid())
                || !same(existing.getSenderId(), c.binding.getAgentId()))) {
            throw new RcsCustomerAccessException("RESOURCE_CONFLICT");
        }
        JSONObject upstream = invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.createTemplate(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), body);
            }
        });
        String templateName = firstNonBlank(upstream.getString("name"),
                upstream.getString("templateName"), requestedName);
        String upstreamId = firstNonBlank(upstream.getString("id"),
                upstream.getString("templateId"), templateName);
        saveTemplateBinding(existing, c.binding, templateName, upstreamId,
                firstNonBlank(upstream.getString("templateStatus"),
                        upstream.getString("status")));
        return customerJson(upstream);
    }

    public JSONObject listTemplates(JSONObject auth, String senderId) {
        final SenderContext c = ownedSender(auth, senderId);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.listTemplates(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender());
            }
        }));
    }

    public JSONObject getTemplate(JSONObject auth, String senderId,
            final String templateId) {
        final SenderContext c = ownedSender(auth, senderId);
        requireValue(templateId);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.getTemplate(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), templateId);
            }
        }));
    }

    public JSONObject updateTemplate(JSONObject request, String senderId,
            final String templateId) {
        final SenderContext c = ownedSender(request, senderId);
        final JSONObject body = upstreamBody(request);
        requireValue(templateId);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.updateTemplate(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), templateId,
                        body);
            }
        }));
    }

    public JSONObject registerTemplate(JSONObject request, String senderId,
            final String templateId) {
        final SenderContext c = ownedSender(request, senderId);
        final JSONObject body = upstreamBody(request);
        requireValue(templateId);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.registerTemplate(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), templateId,
                        body);
            }
        }));
    }

    public JSONObject deleteTemplate(JSONObject auth, String senderId,
            final String templateId) {
        final SenderContext c = ownedSender(auth, senderId);
        requireValue(templateId);
        JSONObject result = customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.deleteTemplate(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), templateId);
            }
        }));
        deleteTemplateBindings(c.binding, templateId);
        return result;
    }

    public JSONObject getTemplateStatus(JSONObject auth, String senderId,
            final String templateId) {
        final SenderContext c = ownedSender(auth, senderId);
        requireValue(templateId);
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.getTemplateStatus(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender(), templateId);
            }
        }));
    }

    public JSONObject getLaunchStatus(JSONObject auth, String senderId) {
        final SenderContext c = ownedSender(auth, senderId);
        JSONObject result = customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.getSenderLaunchStatus(c.supplier.getUrl(),
                        c.supplier.getAppId(), c.binding.getSender());
            }
        }));
        result.put("senderId", c.binding.getAgentId());
        return result;
    }

    private SenderContext ownedSender(JSONObject auth, String senderId) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(auth);
        if (isBlank(senderId)) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        RcsChannelAgent binding = first(bindingMapper.selectList(
                new EntityWrapper<RcsChannelAgent>()
                        .eq("agent_id", senderId)
                        .eq("status", 1)));
        accessService.requireOwned(customer.getSsid(),
                binding == null ? null : binding.getSsid());
        if (binding == null || isBlank(binding.getSender())) {
            throw new RcsCustomerAccessException("RESOURCE_NOT_FOUND");
        }
        return new SenderContext(binding, boundSupplier(binding));
    }

    private AdminInfo boundSupplier(RcsChannelAgent binding) {
        AdminInfo supplier = router.resolveSupplier(binding);
        requireInfobip(supplier);
        return supplier;
    }

    private RcsTemplateBinding findTemplateBinding(Integer rssid,
            String templateName) {
        return first(templateBindingMapper.selectList(
                new EntityWrapper<RcsTemplateBinding>()
                        .eq("rssid", rssid)
                        .eq("template_name", templateName)));
    }

    private void saveTemplateBinding(RcsTemplateBinding existing,
            RcsChannelAgent binding, String templateName, String upstreamId,
            String status) {
        Date now = new Date();
        RcsTemplateBinding value = existing == null
                ? new RcsTemplateBinding() : existing;
        value.setRssid(binding.getRssid());
        value.setSsid(binding.getSsid());
        value.setSenderId(binding.getAgentId());
        value.setSenderName(binding.getSender());
        value.setTemplateName(templateName);
        value.setUpstreamTemplateId(upstreamId);
        value.setStatus(status);
        value.setUpdateTime(now);
        if (existing == null) {
            value.setCreateTime(now);
            try {
                if (templateBindingMapper.insert(value) != 1) {
                    throw new RcsCustomerAccessException("INTERNAL_ERROR");
                }
            } catch (RcsCustomerAccessException e) {
                throw e;
            } catch (RuntimeException duplicateOrDatabaseFailure) {
                RcsTemplateBinding concurrent = findTemplateBinding(
                        binding.getRssid(), templateName);
                if (concurrent != null) {
                    throw new RcsCustomerAccessException("RESOURCE_CONFLICT");
                }
                throw new RcsCustomerAccessException("INTERNAL_ERROR");
            }
        } else if (templateBindingMapper.updateById(value) != 1) {
            throw new RcsCustomerAccessException("INTERNAL_ERROR");
        }
    }

    private void deleteTemplateBindings(RcsChannelAgent binding,
            String templateId) {
        List<RcsTemplateBinding> values = templateBindingMapper.selectList(
                new EntityWrapper<RcsTemplateBinding>()
                        .eq("rssid", binding.getRssid())
                        .eq("sender_id", binding.getAgentId()));
        if (values == null) {
            return;
        }
        for (RcsTemplateBinding value : values) {
            if (same(templateId, value.getUpstreamTemplateId())
                    || same(templateId, value.getTemplateName())) {
                templateBindingMapper.deleteById(value.getId());
            }
        }
    }

    private void requireInfobip(AdminInfo supplier) {
        if (supplier == null) {
            throw new RcsCustomerAccessException("ROUTE_NOT_CONFIGURED");
        }
        if (!INFOBIP.equalsIgnoreCase(supplier.getChannelCode())) {
            throw new RcsCustomerAccessException("OPERATION_NOT_SUPPORTED");
        }
        if (isBlank(supplier.getUrl()) || isBlank(supplier.getAppId())) {
            throw new RcsCustomerAccessException("ROUTE_NOT_CONFIGURED");
        }
    }

    private JSONObject getUpstreamSender(final AdminInfo supplier,
            final RcsChannelAgent binding) {
        return customerJson(invoke(new UpstreamCall() {
            @Override
            public JSONObject execute() {
                return infobip.getSender(supplier.getUrl(), supplier.getAppId(),
                        binding.getSender());
            }
        }));
    }

    private JSONObject invoke(UpstreamCall call) {
        try {
            JSONObject result = call.execute();
            return result == null ? new JSONObject() : result;
        } catch (RcsCustomerAccessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        } catch (Exception e) {
            throw new RcsCustomerAccessException("UPSTREAM_REQUEST_REJECTED");
        }
    }

    private static JSONObject upstreamBody(JSONObject request) {
        JSONObject body = request == null ? new JSONObject()
                : JSONObject.parseObject(request.toJSONString());
        removeIgnoreCase(body, "Token");
        removeIgnoreCase(body, "token");
        removeIgnoreCase(body, "country");
        removeIgnoreCase(body, "senderId");
        removeIgnoreCase(body, "channel");
        removeIgnoreCase(body, "channelCode");
        removeIgnoreCase(body, "rssid");
        return body;
    }

    private static JSONObject customerJson(JSONObject upstream) {
        JSONObject copy = upstream == null ? new JSONObject()
                : JSONObject.parseObject(upstream.toJSONString());
        sanitize(copy);
        return copy;
    }

    private static void sanitize(Object value) {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            String[] forbidden = {"channel", "channelCode", "rssid",
                    "apiKey", "authorization", "appId", "appSecurity",
                    "platform"};
            for (String key : forbidden) {
                removeIgnoreCase(object, key);
            }
            for (Object child : object.values()) {
                sanitize(child);
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.size(); i++) {
                sanitize(array.get(i));
            }
        }
    }

    private static void removeIgnoreCase(JSONObject object, String name) {
        String found = null;
        for (String key : object.keySet()) {
            if (name.equalsIgnoreCase(key)) {
                found = key;
                break;
            }
        }
        if (found != null) {
            object.remove(found);
        }
    }

    private static void requireValue(String value) {
        if (isBlank(value)) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
    }

    private static Integer toInt(Long value) {
        if (value == null || value > Integer.MAX_VALUE
                || value < Integer.MIN_VALUE) {
            throw new RcsCustomerAccessException("ROUTE_NOT_CONFIGURED");
        }
        return value.intValue();
    }

    private static String newId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private static String firstNonBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (!isBlank(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private static boolean same(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private interface UpstreamCall {
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
