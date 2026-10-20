package com.liugh.rcs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsMoInfo;
import com.liugh.mapper.RcsMoInfoMapper;
import com.liugh.rcs.channel.RcsChannelCapability;
import com.liugh.rcs.channel.RcsChannelCapabilityRegistry;
import com.liugh.rcs.channel.infobip.InfobipOutboundEventClient;
import com.liugh.util.PhoneUtils;

/**
 * Sends customer-requested RCS agent events through the internally routed
 * sender resource.
 */
@Service
public class RcsAgentEventService {

    private static final String INFOBIP = "INFOBIP";
    private static final String TYPING_INDICATOR = "TYPING_INDICATOR";
    private static final String SEEN = "SEEN";

    private final RcsCustomerAccessService accessService;
    private final RcsRouter router;
    private final RcsChannelCapabilityRegistry capabilityRegistry;
    private final InfobipOutboundEventClient infobipClient;
    private final RcsMoInfoMapper moMapper;

    @Autowired
    public RcsAgentEventService(RcsCustomerAccessService accessService,
            RcsRouter router,
            RcsChannelCapabilityRegistry capabilityRegistry,
            InfobipOutboundEventClient infobipClient,
            RcsMoInfoMapper moMapper) {
        this.accessService = accessService;
        this.router = router;
        this.capabilityRegistry = capabilityRegistry;
        this.infobipClient = infobipClient;
        this.moMapper = moMapper;
    }

    public JSONObject send(JSONObject request) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(request);
        String senderId = value(request, "senderId");
        String recipient = value(request, "recipient");
        String type = value(request, "type");
        if (isBlank(senderId) || isBlank(recipient)
                || (!TYPING_INDICATOR.equalsIgnoreCase(type)
                        && !SEEN.equalsIgnoreCase(type))) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        recipient = PhoneUtils.formatPhoneWithPlusSign(recipient.trim());

        String country = PhoneUtils.getRegionCodeByPhone(recipient);
        AdminInfo supplier = router.route(customer, country);
        if (supplier == null) {
            throw new RcsCustomerAccessException("ROUTE_NOT_CONFIGURED");
        }
        String channelCode = supplier.getChannelCode();
        if (!capabilityRegistry.supports(channelCode,
                RcsChannelCapability.OUTBOUND_EVENT)
                || !INFOBIP.equalsIgnoreCase(channelCode)) {
            throw new RcsCustomerAccessException("OPERATION_NOT_SUPPORTED");
        }

        RcsChannelAgent binding = router.resolveAgentBinding(senderId, channelCode);
        accessService.requireOwned(customer.getSsid(),
                binding == null ? null : binding.getSsid());
        if (isBlank(binding.getSender())) {
            throw new RcsCustomerAccessException("RESOURCE_NOT_FOUND");
        }

        try {
            if (TYPING_INDICATOR.equalsIgnoreCase(type)) {
                infobipClient.typing(supplier.getUrl(), supplier.getAppId(),
                        binding.getSender(), recipient);
            } else {
                RcsMoInfo inbound = requireInboundMessage(customer.getSsid(),
                        senderId, channelCode, recipient,
                        value(request, "messageId"));
                infobipClient.seen(supplier.getUrl(), supplier.getAppId(),
                        binding.getSender(), recipient, inbound.getClientMsgId());
            }
        } catch (RcsCustomerAccessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        } catch (Exception e) {
            throw new RcsCustomerAccessException("UPSTREAM_REQUEST_REJECTED");
        }

        JSONObject result = new JSONObject();
        result.put("status", "ACCEPTED");
        result.put("type", type.toUpperCase());
        return result;
    }

    private RcsMoInfo requireInboundMessage(Integer customerSsid,
            String senderId, String channelCode, String recipient,
            String platformMessageId) {
        Long id = parseInboundId(platformMessageId);
        RcsMoInfo inbound = id == null ? null : moMapper.selectById(id);
        accessService.requireOwned(customerSsid,
                inbound == null ? null : inbound.getSsid());
        if (!equalsIgnoreCase(senderId, inbound.getAgentId())
                || !equalsIgnoreCase(channelCode, inbound.getChannelCode())
                || !recipient.equals(inbound.getMsisdn())
                || isBlank(inbound.getClientMsgId())) {
            throw new RcsCustomerAccessException("FORBIDDEN_RESOURCE");
        }
        return inbound;
    }

    private Long parseInboundId(String platformMessageId) {
        if (isBlank(platformMessageId) || !platformMessageId.startsWith("mo_")) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        try {
            return Long.valueOf(platformMessageId.substring(3));
        } catch (NumberFormatException e) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
    }

    private static String value(JSONObject request, String key) {
        return request == null ? null : request.getString(key);
    }

    private static boolean equalsIgnoreCase(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
