package com.liugh.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.util.HttpUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Infobip RCS API 模拟器，仅在 {@code infobip.simulator.enabled=true} 时激活。
 * 实现与真实 Infobip 相同的请求/响应格式，平台测试时将供应商 baseUrl
 * 指向本应用，无需真实 Infobip 账号。
 *
 * 收到发送请求后，按 {@code infobip.simulator.dr-delay-ms}（默认 500ms）的延迟
 * 向请求体中的 {@code webhooks.delivery.url} 推送 DR 回调。
 *
 * 诊断端点（仅模拟器模式可用）：
 *   GET    /simulator/messages             — 列出所有已捕获的发送载荷
 *   DELETE /simulator/messages             — 清空捕获缓冲区
 *   POST   /simulator/push-dr/{simMsgId}  — 手动触发指定消息的 DR
 */
@RestController
@ConditionalOnProperty(name = "infobip.simulator.enabled", havingValue = "true")
@Slf4j
public class InfobipSimulatorController {

    // 保留最近 N 条发送载荷，供诊断端点查询
    private static final int MAX_CAPTURED = 200;

    /** 模拟存储的 sender 记录：senderName → senderObject */
    private final Map<String, JSONObject> senders = new ConcurrentHashMap<>();

    /** 模拟存储的 test-number 记录：senderName/number → object */
    private final Map<String, JSONObject> testNumbers = new ConcurrentHashMap<>();

    /** 模拟存储的 template 记录：senderName/templateId → object */
    private final Map<String, JSONObject> templates = new ConcurrentHashMap<>();

    /** 已捕获的发送请求（ringbuffer 语义，超限后丢最旧的） */
    private final List<JSONObject> capturedSends = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "infobip-sim-dr");
                t.setDaemon(true);
                return t;
            });

    /** DR 推送延迟（毫秒），可通过 infobip.simulator.dr-delay-ms 配置 */
    private long drDelayMs = 500;

    /** 模拟器默认 DR 结果，可通过 infobip.simulator.dr-status 配置：DELIVERED / REJECTED / EXPIRED */
    private String defaultDrStatus = "DELIVERED";

    @Value("${infobip.simulator.dr-delay-ms:500}")
    public void setDrDelayMs(long drDelayMs) {
        this.drDelayMs = drDelayMs;
    }

    @Value("${infobip.simulator.dr-status:DELIVERED}")
    public void setDefaultDrStatus(String defaultDrStatus) {
        this.defaultDrStatus = defaultDrStatus == null ? "DELIVERED"
                : defaultDrStatus.toUpperCase();
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // =====================================================================
    // Messaging — POST /rcs/2/messages
    // =====================================================================

    @PostMapping("/rcs/2/messages")
    public ResponseEntity<JSONObject> sendMessage(@RequestBody JSONObject body) {
        String bulkId = "SIM-BULK-" + uuid();
        JSONArray messages = body.getJSONArray("messages");
        JSONArray responseMessages = new JSONArray();

        if (messages != null) {
            for (int i = 0; i < messages.size(); i++) {
                JSONObject msg = messages.getJSONObject(i);
                String simMsgId = "SIM-" + uuid();
                JSONObject entry = new JSONObject();
                entry.put("messageId", simMsgId);
                JSONObject status = new JSONObject();
                status.put("groupId", 1);
                status.put("groupName", "PENDING");
                status.put("id", 7);
                status.put("name", "PENDING_ENROUTE");
                entry.put("status", status);
                responseMessages.add(entry);

                // capture for diagnostics (keep ring bounded)
                JSONObject captured = new JSONObject();
                captured.put("simMsgId", simMsgId);
                captured.put("bulkId", bulkId);
                captured.put("payload", msg);
                captured.put("capturedAt", System.currentTimeMillis());
                capturedSends.add(captured);
                while (capturedSends.size() > MAX_CAPTURED) {
                    capturedSends.remove(0);
                }

                // extract callbackData (our msgId) and notify URL, then schedule DR
                JSONObject webhooks = msg.getJSONObject("webhooks");
                String callbackData = webhooks == null ? null
                        : webhooks.getString("callbackData");
                String deliveryUrl = webhooks == null ? null
                        : deepString(webhooks, "delivery", "url");
                scheduleAutoDelivery(simMsgId, callbackData, deliveryUrl, drDelayMs);
            }
        }

        JSONObject resp = new JSONObject();
        resp.put("bulkId", bulkId);
        resp.put("messages", responseMessages);
        log.info("[sim] sendMessage bulkId={} count={}", bulkId,
                responseMessages.size());
        return ResponseEntity.ok(resp);
    }

    // =====================================================================
    // Sender management — /rcs/1/senders
    // =====================================================================

    @PostMapping("/rcs/1/senders")
    public ResponseEntity<JSONObject> createSender(@RequestBody JSONObject body) {
        String name = body.getString("name");
        if (isBlank(name)) {
            return badRequest("sender name is required");
        }
        body.put("status", "PENDING");
        senders.put(name, body);
        log.info("[sim] createSender name={}", name);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/rcs/1/senders")
    public ResponseEntity<JSONObject> listSenders() {
        JSONObject resp = new JSONObject();
        resp.put("senders", new JSONArray(new java.util.ArrayList<>(senders.values())));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/rcs/1/senders/{senderName}")
    public ResponseEntity<JSONObject> getSender(@PathVariable String senderName) {
        JSONObject s = senders.get(senderName);
        return s != null ? ResponseEntity.ok(s) : notFound("sender not found");
    }

    @PutMapping("/rcs/1/senders/{senderName}")
    public ResponseEntity<JSONObject> updateSender(@PathVariable String senderName,
            @RequestBody JSONObject body) {
        if (!senders.containsKey(senderName)) {
            return notFound("sender not found");
        }
        body.put("name", senderName);
        senders.put(senderName, body);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/rcs/1/senders/{senderName}/platform-parameters")
    public ResponseEntity<JSONObject> updateSenderPlatformParameters(
            @PathVariable String senderName, @RequestBody JSONObject body) {
        JSONObject s = senders.get(senderName);
        if (s == null) {
            return notFound("sender not found");
        }
        s.put("platformParameters", body);
        return ResponseEntity.ok(s);
    }

    @GetMapping("/rcs/1/senders/{senderName}/launch-status")
    public ResponseEntity<JSONObject> getLaunchStatus(@PathVariable String senderName) {
        JSONObject resp = new JSONObject();
        resp.put("senderName", senderName);
        resp.put("status", "LAUNCHED");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/rcs/1/senders/providers")
    public ResponseEntity<JSONObject> getProviders(
            @RequestParam(required = false) String countryCode) {
        JSONObject resp = new JSONObject();
        JSONArray providers = new JSONArray();
        JSONObject provider = new JSONObject();
        provider.put("name", "SIM_PROVIDER");
        provider.put("countryCode", countryCode != null ? countryCode : "US");
        providers.add(provider);
        resp.put("providers", providers);
        return ResponseEntity.ok(resp);
    }

    // =====================================================================
    // Test-number management — /rcs/1/senders/{senderName}/test-numbers
    // =====================================================================

    @PostMapping("/rcs/1/senders/{senderName}/test-numbers")
    public ResponseEntity<JSONObject> addTestNumber(
            @PathVariable String senderName, @RequestBody JSONObject body) {
        if (!senders.containsKey(senderName)) {
            return notFound("sender not found");
        }
        String number = body.getString("number");
        if (isBlank(number)) {
            return badRequest("number is required");
        }
        body.put("status", "PENDING");
        testNumbers.put(senderName + "/" + number, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/rcs/1/senders/{senderName}/test-numbers")
    public ResponseEntity<JSONObject> listTestNumbers(@PathVariable String senderName) {
        JSONArray list = new JSONArray();
        String prefix = senderName + "/";
        for (Map.Entry<String, JSONObject> e : testNumbers.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                list.add(e.getValue());
            }
        }
        JSONObject resp = new JSONObject();
        resp.put("testNumbers", list);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/rcs/1/senders/{senderName}/test-numbers/{number}")
    public ResponseEntity<JSONObject> getTestNumber(
            @PathVariable String senderName, @PathVariable String number) {
        JSONObject tn = testNumbers.get(senderName + "/" + number);
        return tn != null ? ResponseEntity.ok(tn) : notFound("test number not found");
    }

    @PutMapping("/rcs/1/senders/{senderName}/test-numbers/{number}/refresh")
    public ResponseEntity<JSONObject> refreshTestNumber(
            @PathVariable String senderName, @PathVariable String number) {
        String key = senderName + "/" + number;
        JSONObject tn = testNumbers.get(key);
        if (tn == null) {
            return notFound("test number not found");
        }
        tn.put("refreshedAt", System.currentTimeMillis());
        return ResponseEntity.ok(tn);
    }

    @PatchMapping("/rcs/1/senders/{senderName}/test-numbers/{number}")
    public ResponseEntity<JSONObject> updatePrimaryTestNumber(
            @PathVariable String senderName, @PathVariable String number,
            @RequestBody JSONObject body) {
        String key = senderName + "/" + number;
        JSONObject tn = testNumbers.get(key);
        if (tn == null) {
            return notFound("test number not found");
        }
        if (body.containsKey("primary")) {
            tn.put("primary", body.getBoolean("primary"));
        }
        return ResponseEntity.ok(tn);
    }

    @PostMapping("/rcs/1/senders/{senderName}/test-numbers/{number}/request-status")
    public ResponseEntity<JSONObject> getTestNumberRequestStatus(
            @PathVariable String senderName, @PathVariable String number) {
        JSONObject resp = new JSONObject();
        resp.put("number", number);
        resp.put("status", "APPROVED");
        return ResponseEntity.ok(resp);
    }

    // =====================================================================
    // Template management — /rcs/1/senders/{senderName}/templates
    // =====================================================================

    @PostMapping("/rcs/1/senders/{senderName}/templates")
    public ResponseEntity<JSONObject> createTemplate(
            @PathVariable String senderName, @RequestBody JSONObject body) {
        if (!senders.containsKey(senderName)) {
            return notFound("sender not found");
        }
        String templateId = "SIM-TPL-" + uuid();
        body.put("id", templateId);
        body.put("status", "PENDING");
        templates.put(senderName + "/" + templateId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/rcs/1/senders/{senderName}/templates")
    public ResponseEntity<JSONObject> listTemplates(@PathVariable String senderName) {
        JSONArray list = new JSONArray();
        String prefix = senderName + "/";
        for (Map.Entry<String, JSONObject> e : templates.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                list.add(e.getValue());
            }
        }
        JSONObject resp = new JSONObject();
        resp.put("templates", list);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/rcs/1/senders/{senderName}/templates/{templateId}")
    public ResponseEntity<JSONObject> getTemplate(
            @PathVariable String senderName, @PathVariable String templateId) {
        JSONObject t = templates.get(senderName + "/" + templateId);
        return t != null ? ResponseEntity.ok(t) : notFound("template not found");
    }

    @PutMapping("/rcs/1/senders/{senderName}/templates/{templateId}")
    public ResponseEntity<JSONObject> updateTemplate(
            @PathVariable String senderName, @PathVariable String templateId,
            @RequestBody JSONObject body) {
        String key = senderName + "/" + templateId;
        if (!templates.containsKey(key)) {
            return notFound("template not found");
        }
        body.put("id", templateId);
        templates.put(key, body);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/rcs/1/senders/{senderName}/templates/{templateId}/register")
    public ResponseEntity<JSONObject> registerTemplate(
            @PathVariable String senderName, @PathVariable String templateId,
            @RequestBody(required = false) JSONObject body) {
        String key = senderName + "/" + templateId;
        JSONObject t = templates.get(key);
        if (t == null) {
            return notFound("template not found");
        }
        t.put("status", "APPROVED");
        return ResponseEntity.ok(t);
    }

    @DeleteMapping("/rcs/1/senders/{senderName}/templates/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable String senderName, @PathVariable String templateId) {
        templates.remove(senderName + "/" + templateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rcs/1/senders/{senderName}/templates/{templateId}/status")
    public ResponseEntity<JSONObject> getTemplateStatus(
            @PathVariable String senderName, @PathVariable String templateId) {
        JSONObject t = templates.get(senderName + "/" + templateId);
        if (t == null) {
            return notFound("template not found");
        }
        JSONObject resp = new JSONObject();
        resp.put("id", templateId);
        resp.put("status", t.getString("status"));
        return ResponseEntity.ok(resp);
    }

    // =====================================================================
    // Resource management — /resources/1/...
    // =====================================================================

    @PostMapping("/resources/1/requirements-lookup")
    public ResponseEntity<JSONObject> lookupResourceRequirements(
            @RequestBody JSONObject body) {
        JSONObject resp = new JSONObject();
        resp.put("requirements", new JSONArray());
        resp.put("eligible", true);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/resources/1/requests")
    public ResponseEntity<JSONObject> submitResourceRequest(
            @RequestBody JSONObject body) {
        JSONObject resp = new JSONObject();
        resp.put("requestId", "SIM-REQ-" + uuid());
        resp.put("status", "PENDING");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/resources/1/requests/{requestId}")
    public ResponseEntity<JSONObject> getResourceRequest(
            @PathVariable String requestId) {
        JSONObject resp = new JSONObject();
        resp.put("requestId", requestId);
        resp.put("status", "APPROVED");
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/resources/1/requests/{requestId}")
    public ResponseEntity<JSONObject> updateResourceRequest(
            @PathVariable String requestId, @RequestBody JSONObject body) {
        body.put("requestId", requestId);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/resources/1/requests/{requestId}/cancel")
    public ResponseEntity<JSONObject> cancelResourceRequest(
            @PathVariable String requestId) {
        JSONObject resp = new JSONObject();
        resp.put("requestId", requestId);
        resp.put("status", "CANCELLED");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/resources/1/documents")
    public ResponseEntity<JSONObject> uploadResourceDocument() {
        JSONObject resp = new JSONObject();
        resp.put("documentId", "SIM-DOC-" + uuid());
        resp.put("status", "UPLOADED");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // =====================================================================
    // Diagnostic endpoints (only active when simulator is enabled)
    // =====================================================================

    /** 返回已捕获的最近发送载荷，方便断言请求结构是否正确。 */
    @GetMapping("/simulator/messages")
    public ResponseEntity<JSONObject> listCapturedMessages(
            @RequestParam(defaultValue = "50") int limit) {
        int size = capturedSends.size();
        int from = Math.max(0, size - Math.min(limit, MAX_CAPTURED));
        JSONObject resp = new JSONObject();
        resp.put("total", size);
        resp.put("messages", new JSONArray(new ArrayList<>(capturedSends.subList(from, size))));
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/simulator/messages")
    public ResponseEntity<Void> clearCapturedMessages() {
        capturedSends.clear();
        return ResponseEntity.noContent().build();
    }

    /**
     * 手动向平台推送某条消息的 DR，用于测试特定终态（DELIVERED/REJECTED/EXPIRED）。
     * 需要先通过 GET /simulator/messages 查到 simMsgId 和对应的 deliveryUrl，
     * 或者直接在请求体里提供：{"deliveryUrl":"…","callbackData":"…","drStatus":"REJECTED"}。
     */
    @PostMapping("/simulator/push-dr/{simMsgId}")
    public ResponseEntity<JSONObject> manualPushDr(
            @PathVariable String simMsgId,
            @RequestBody(required = false) JSONObject body) {
        String deliveryUrl = null;
        String callbackData = simMsgId;
        String drStatus = defaultDrStatus;

        // 先从捕获缓冲区里找原始载荷
        for (int i = capturedSends.size() - 1; i >= 0; i--) {
            JSONObject captured = capturedSends.get(i);
            if (simMsgId.equals(captured.getString("simMsgId"))) {
                JSONObject payload = captured.getJSONObject("payload");
                if (payload != null) {
                    JSONObject webhooks = payload.getJSONObject("webhooks");
                    if (webhooks != null) {
                        deliveryUrl = deepString(webhooks, "delivery", "url");
                        callbackData = webhooks.getString("callbackData");
                    }
                }
                break;
            }
        }
        // 请求体可覆盖
        if (body != null) {
            if (!isBlank(body.getString("deliveryUrl"))) {
                deliveryUrl = body.getString("deliveryUrl");
            }
            if (!isBlank(body.getString("callbackData"))) {
                callbackData = body.getString("callbackData");
            }
            if (!isBlank(body.getString("drStatus"))) {
                drStatus = body.getString("drStatus").toUpperCase();
            }
        }
        if (isBlank(deliveryUrl)) {
            return badRequest("deliveryUrl is required (not found in captured "
                    + "messages; supply it in the request body)");
        }
        final String finalUrl = deliveryUrl;
        final String finalCallbackData = callbackData;
        final String finalDrStatus = drStatus;
        scheduler.schedule(
                () -> doPushDr(simMsgId, finalCallbackData, finalUrl, finalDrStatus),
                0, TimeUnit.MILLISECONDS);

        JSONObject resp = new JSONObject();
        resp.put("simMsgId", simMsgId);
        resp.put("deliveryUrl", deliveryUrl);
        resp.put("drStatus", drStatus);
        resp.put("scheduled", true);
        return ResponseEntity.ok(resp);
    }

    // =====================================================================
    // Internal — DR push and helpers
    // =====================================================================

    private void scheduleAutoDelivery(String simMsgId, String callbackData,
            String deliveryUrl, long delayMs) {
        if (isBlank(deliveryUrl)) {
            log.warn("[sim] no deliveryUrl in webhooks, DR push skipped for simMsgId={}", simMsgId);
            return;
        }
        scheduler.schedule(
                () -> doPushDr(simMsgId, callbackData, deliveryUrl, defaultDrStatus),
                delayMs, TimeUnit.MILLISECONDS);
    }

    private void doPushDr(String simMsgId, String callbackData,
            String deliveryUrl, String drStatus) {
        try {
            JSONObject drPayload = buildDrPayload(simMsgId, callbackData, drStatus);
            String body = JSON.toJSONString(drPayload);
            log.info("[sim] pushing DR to {} status={} callbackData={}", deliveryUrl, drStatus, callbackData);
            HttpUtil.postJson(deliveryUrl, body);
        } catch (Exception e) {
            log.error("[sim] DR push failed, simMsgId={}", simMsgId, e);
        }
    }

    /**
     * 构造符合平台 InfobipChannelAdapter.parseDr() 期望格式的回调 payload。
     * groupName 选项：DELIVERED / REJECTED / UNDELIVERABLE / EXPIRED / SEEN。
     */
    private static JSONObject buildDrPayload(String simMsgId, String callbackData,
            String groupName) {
        JSONObject result = new JSONObject();
        result.put("messageId", simMsgId);
        result.put("callbackData", callbackData != null ? callbackData : simMsgId);

        JSONObject status = new JSONObject();
        status.put("groupName", groupName);
        if ("DELIVERED".equals(groupName)) {
            status.put("groupId", 3);
            status.put("id", 5);
            status.put("name", "DELIVERED_TO_HANDSET");
        } else if ("REJECTED".equals(groupName) || "UNDELIVERABLE".equals(groupName)) {
            status.put("groupId", 5);
            status.put("id", 21);
            status.put("name", "ERROR_NOT_DELIVERED");
            JSONObject error = new JSONObject();
            error.put("id", "SIM_REJECTED");
            error.put("name", "Simulator rejected delivery");
            result.put("error", error);
        } else if ("EXPIRED".equals(groupName)) {
            status.put("groupId", 4);
            status.put("id", 14);
            status.put("name", "EXPIRED_UNKNOWN");
        } else if ("SEEN".equals(groupName)) {
            status.put("groupId", 3);
            status.put("id", 9);
            status.put("name", "SEEN");
            result.put("seenAt", System.currentTimeMillis());
        }
        result.put("status", status);

        JSONObject price = new JSONObject();
        price.put("pricePerMessage", "0.00");
        price.put("currency", "EUR");
        result.put("price", price);

        JSONArray results = new JSONArray();
        results.add(result);
        JSONObject payload = new JSONObject();
        payload.put("results", results);
        return payload;
    }

    /** 安全读取嵌套 JSONObject 字段的字符串值。 */
    private static String deepString(JSONObject root, String key1, String key2) {
        if (root == null) {
            return null;
        }
        JSONObject inner = root.getJSONObject(key1);
        return inner == null ? null : inner.getString(key2);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @SuppressWarnings("unchecked")
    private static <T> ResponseEntity<T> notFound(String message) {
        JSONObject err = new JSONObject();
        err.put("error", message);
        return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @SuppressWarnings("unchecked")
    private static <T> ResponseEntity<T> badRequest(String message) {
        JSONObject err = new JSONObject();
        err.put("error", message);
        return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }
}
