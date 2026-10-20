package com.liugh.rcs.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsCapabilityBatch;
import com.liugh.entity.RcsCapabilityResult;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.mapper.RcsCapabilityBatchMapper;
import com.liugh.mapper.RcsCapabilityResultMapper;
import com.liugh.rcs.channel.RcsChannelCapability;
import com.liugh.rcs.channel.RcsChannelCapabilityRegistry;
import com.liugh.rcs.channel.infobip.InfobipCapabilityClient;
import com.liugh.util.PhoneUtils;

/**
 * Customer-facing capability orchestration. Channel selection and credentials
 * stay internal and are resolved from the platform route.
 */
@Service
public class RcsCapabilityService {

    private static final String INFOBIP = "INFOBIP";

    private final RcsCustomerAccessService accessService;
    private final RcsRouter router;
    private final RcsChannelCapabilityRegistry capabilityRegistry;
    private final InfobipCapabilityClient infobipClient;
    private final RcsCapabilityBatchMapper batchMapper;
    private final RcsCapabilityResultMapper resultMapper;

    private String projectUrl;

    @Autowired
    public RcsCapabilityService(RcsCustomerAccessService accessService,
            RcsRouter router,
            RcsChannelCapabilityRegistry capabilityRegistry,
            InfobipCapabilityClient infobipClient,
            RcsCapabilityBatchMapper batchMapper,
            RcsCapabilityResultMapper resultMapper) {
        this.accessService = accessService;
        this.router = router;
        this.capabilityRegistry = capabilityRegistry;
        this.infobipClient = infobipClient;
        this.batchMapper = batchMapper;
        this.resultMapper = resultMapper;
    }

    @Value("${project.url}")
    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }

    public JSONObject query(JSONObject request) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(request);
        RequestContext context = resolveRequest(customer, request,
                RcsChannelCapability.CAPABILITY_QUERY_SYNC);
        JSONObject upstream;
        try {
            upstream = infobipClient.query(context.supplier.getUrl(),
                    context.supplier.getAppId(), context.binding.getSender(),
                    context.phoneNumbers);
        } catch (IllegalArgumentException e) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        } catch (Exception e) {
            throw new RcsCustomerAccessException("UPSTREAM_REQUEST_REJECTED");
        }
        JSONObject result = new JSONObject();
        result.put("results", customerResults(upstream));
        return result;
    }

    public JSONObject createBatch(JSONObject request) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(request);
        RequestContext context = resolveRequest(customer, request,
                RcsChannelCapability.CAPABILITY_QUERY_ASYNC);
        JSONObject upstream;
        try {
            upstream = infobipClient.notify(context.supplier.getUrl(),
                    context.supplier.getAppId(), context.binding.getSender(),
                    context.phoneNumbers, capabilityNotifyUrl());
        } catch (IllegalArgumentException e) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        } catch (Exception e) {
            throw new RcsCustomerAccessException("UPSTREAM_REQUEST_REJECTED");
        }

        Date now = new Date();
        String batchId = "cap_" + UUID.randomUUID().toString().replace("-", "");
        RcsCapabilityBatch batch = new RcsCapabilityBatch();
        batch.setBatchId(batchId);
        batch.setSsid(customer.getSsid());
        batch.setSenderId(context.senderId);
        batch.setChannelCode(context.supplier.getChannelCode());
        batch.setUpstreamBulkId(upstream.getString("bulkId"));
        batch.setStatus("PENDING");
        batch.setTotalCount(context.phoneNumbers.size());
        batch.setSuccessCount(0);
        batch.setFailureCount(0);
        batch.setRequestedAt(now);
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batchMapper.insert(batch);

        JSONObject result = new JSONObject();
        result.put("batchId", batchId);
        result.put("status", batch.getStatus());
        result.put("totalCount", batch.getTotalCount());
        return result;
    }

    public JSONObject getBatch(JSONObject authRequest, String batchId) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(authRequest);
        if (isBlank(batchId)) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        RcsCapabilityBatch batch = first(batchMapper.selectList(
                new EntityWrapper<RcsCapabilityBatch>().eq("batch_id", batchId)));
        accessService.requireOwned(customer.getSsid(),
                batch == null ? null : batch.getSsid());

        List<RcsCapabilityResult> storedResults = resultMapper.selectList(
                new EntityWrapper<RcsCapabilityResult>()
                        .eq("batch_id", batchId)
                        .eq("ssid", customer.getSsid()));
        JSONArray results = new JSONArray();
        if (storedResults != null) {
            for (RcsCapabilityResult stored : storedResults) {
                JSONObject item = new JSONObject();
                item.put("recipient", stored.getRecipient());
                item.put("status", stored.getStatus());
                if (!isBlank(stored.getCapabilitiesJson())) {
                    item.put("capabilities",
                            JSONObject.parse(stored.getCapabilitiesJson()));
                }
                if (!isBlank(stored.getErrorCode())) {
                    item.put("errorCode", stored.getErrorCode());
                    item.put("errorSummary", stored.getErrorSummary());
                }
                results.add(item);
            }
        }

        JSONObject result = new JSONObject();
        result.put("batchId", batch.getBatchId());
        result.put("senderId", batch.getSenderId());
        result.put("status", batch.getStatus());
        result.put("totalCount", batch.getTotalCount());
        result.put("successCount", batch.getSuccessCount());
        result.put("failureCount", batch.getFailureCount());
        result.put("requestedAt", batch.getRequestedAt());
        result.put("completedAt", batch.getCompletedAt());
        result.put("results", results);
        return result;
    }

    private RequestContext resolveRequest(AdminInfo customer, JSONObject request,
            RcsChannelCapability capability) {
        String senderId = request == null ? null : request.getString("senderId");
        List<String> phoneNumbers = phoneNumbers(request);
        if (isBlank(senderId) || phoneNumbers.isEmpty()) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        String country = PhoneUtils.getRegionCodeByPhone(phoneNumbers.get(0));
        AdminInfo supplier = router.route(customer, country);
        if (supplier == null) {
            throw new RcsCustomerAccessException("ROUTE_NOT_CONFIGURED");
        }
        for (int i = 1; i < phoneNumbers.size(); i++) {
            String recipientCountry =
                    PhoneUtils.getRegionCodeByPhone(phoneNumbers.get(i));
            AdminInfo routedSupplier =
                    router.route(customer, recipientCountry);
            if (routedSupplier == null) {
                throw new RcsCustomerAccessException("ROUTE_NOT_CONFIGURED");
            }
            if (!sameSupplier(supplier, routedSupplier)) {
                throw new RcsCustomerAccessException(
                        "OPERATION_NOT_SUPPORTED");
            }
        }
        String channelCode = supplier.getChannelCode();
        if (!capabilityRegistry.supports(channelCode, capability)
                || !INFOBIP.equalsIgnoreCase(channelCode)) {
            throw new RcsCustomerAccessException("OPERATION_NOT_SUPPORTED");
        }
        RcsChannelAgent binding = router.resolveAgentBinding(senderId, channelCode);
        accessService.requireOwned(customer.getSsid(),
                binding == null ? null : binding.getSsid());
        if (isBlank(binding.getSender())) {
            throw new RcsCustomerAccessException("RESOURCE_NOT_FOUND");
        }
        return new RequestContext(senderId, phoneNumbers, supplier, binding);
    }

    private JSONArray customerResults(JSONObject upstream) {
        JSONArray source = upstream == null
                ? null : upstream.getJSONArray("capabilityCheckResults");
        JSONArray results = new JSONArray();
        if (source == null) {
            return results;
        }
        for (int i = 0; i < source.size(); i++) {
            JSONObject value = source.getJSONObject(i);
            JSONObject item = new JSONObject();
            item.put("recipient", firstNonBlank(value.getString("phoneNumber"),
                    value.getString("to")));
            item.put("status", firstNonBlank(value.getString("code"),
                    value.getString("status")));
            Object capabilities = value.get("features");
            if (capabilities == null) {
                capabilities = value.get("capabilities");
            }
            if (capabilities != null) {
                item.put("capabilities", capabilities);
            }
            JSONObject error = value.getJSONObject("error");
            if (error != null) {
                item.put("errorCode", firstNonBlank(error.getString("id"),
                        error.getString("code")));
                item.put("errorSummary", firstNonBlank(error.getString("name"),
                        error.getString("description")));
            }
            results.add(item);
        }
        return results;
    }

    private List<String> phoneNumbers(JSONObject request) {
        JSONArray values = request == null ? null : request.getJSONArray("phoneNumbers");
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>(values.size());
        for (int i = 0; i < values.size(); i++) {
            String value = values.getString(i);
            if (isBlank(value)) {
                throw new RcsCustomerAccessException("INVALID_ARGUMENT");
            }
            result.add(PhoneUtils.formatPhoneWithPlusSign(value.trim()));
        }
        return result;
    }

    private String capabilityNotifyUrl() {
        if (isBlank(projectUrl)) {
            throw new RcsCustomerAccessException("UPSTREAM_REQUEST_REJECTED");
        }
        String base = projectUrl.trim();
        if (!base.endsWith("/")) {
            base += "/";
        }
        return base + "rcs/callback/infobip/capabilities";
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean sameSupplier(AdminInfo left, AdminInfo right) {
        return left != null && right != null && left.getId() != null
                && left.getId().equals(right.getId());
    }

    private static final class RequestContext {
        private final String senderId;
        private final List<String> phoneNumbers;
        private final AdminInfo supplier;
        private final RcsChannelAgent binding;

        private RequestContext(String senderId, List<String> phoneNumbers,
                AdminInfo supplier, RcsChannelAgent binding) {
            this.senderId = senderId;
            this.phoneNumbers = phoneNumbers;
            this.supplier = supplier;
            this.binding = binding;
        }
    }
}
