package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.liugh.entity.RcsCustomerEvent;
import com.liugh.entity.RcsDeliveryConfig;
import com.liugh.entity.RcsEventDeliveryAttempt;
import com.liugh.mapper.RcsCustomerEventMapper;
import com.liugh.mapper.RcsEventDeliveryAttemptMapper;

public class RcsCustomerEventDeliveryServiceTest {

    private RcsCustomerEventMapper eventMapper;
    private RcsEventDeliveryAttemptMapper attemptMapper;
    private RcsDeliveryConfigService configService;
    private CapturingHttpClient httpClient;
    private RcsCustomerEventDeliveryService service;
    private RcsCustomerEvent event;

    @Before
    public void setUp() {
        eventMapper = mock(RcsCustomerEventMapper.class);
        attemptMapper = mock(RcsEventDeliveryAttemptMapper.class);
        configService = mock(RcsDeliveryConfigService.class);
        httpClient = new CapturingHttpClient();
        service = new RcsCustomerEventDeliveryService(eventMapper,
                attemptMapper, configService, httpClient);

        event = new RcsCustomerEvent();
        event.setId(1L);
        event.setEventId("evt_1");
        event.setSsid(100);
        event.setPayloadJson("{\"eventId\":\"evt_1\",\"type\":\"MESSAGE_DELIVERED\"}");
        event.setDeliverable(true);
        event.setDeliveryStatus("PENDING");
        event.setAttemptCount(0);

        RcsDeliveryConfig config = new RcsDeliveryConfig();
        config.setSsid(100);
        config.setEventUrl("https://customer.example.com/rcs/events");
        config.setSignatureSecretCiphertext("cipher".getBytes(StandardCharsets.UTF_8));
        config.setMaxRetries(3);
        config.setEnabled(true);
        when(configService.findBySsid(100)).thenReturn(config);
        when(configService.decryptSecret(config)).thenReturn("customer-secret");
        when(eventMapper.update(any(RcsCustomerEvent.class), any())).thenReturn(1);
    }

    @Test
    public void postsExactJsonWithSignatureHeadersAndAcceptsAny2xx() {
        httpClient.status = 204;

        service.deliver(event);

        assertEquals("https://customer.example.com/rcs/events", httpClient.url);
        assertEquals(event.getPayloadJson(), httpClient.body);
        assertEquals("evt_1", httpClient.headers.get("X-RCS-Event-Id"));
        assertNotNull(httpClient.headers.get("X-RCS-Timestamp"));
        assertTrue(httpClient.headers.get("X-RCS-Signature")
                .startsWith("sha256="));
        assertEquals("DELIVERED", event.getDeliveryStatus());
        assertEquals(Integer.valueOf(1), event.getAttemptCount());
        verify(attemptMapper).insert(any(RcsEventDeliveryAttempt.class));
    }

    @Test
    public void schedulesFiniteRetryAfterNon2xx() {
        httpClient.status = 503;

        service.deliver(event);

        assertEquals("RETRY", event.getDeliveryStatus());
        assertNotNull(event.getNextAttemptTime());
    }

    @Test
    public void stopsAfterConfiguredAttemptLimit() {
        event.setAttemptCount(2);
        httpClient.status = 500;

        service.deliver(event);

        assertEquals("FAILED", event.getDeliveryStatus());
        assertEquals(Integer.valueOf(3), event.getAttemptCount());
    }

    @Test
    public void secretFailureDoesNotLeaveEventStuckDelivering() {
        when(configService.decryptSecret(any(RcsDeliveryConfig.class)))
                .thenThrow(new IllegalStateException("missing key"));

        service.deliver(event);

        assertEquals("RETRY", event.getDeliveryStatus());
        assertEquals(Integer.valueOf(1), event.getAttemptCount());
        verify(attemptMapper).insert(any(RcsEventDeliveryAttempt.class));
    }

    @Test
    public void claimPreventsConcurrentDuplicateDelivery() {
        when(eventMapper.update(any(RcsCustomerEvent.class), any())).thenReturn(0);

        service.deliver(event);

        assertEquals(0, httpClient.callCount);
        verify(attemptMapper, never()).insert(any(RcsEventDeliveryAttempt.class));
    }

    @Test
    public void manualRetryResetsFailedOwnedEvent() {
        event.setDeliveryStatus("FAILED");
        event.setAttemptCount(3);
        when(eventMapper.selectList(any()))
                .thenReturn(Collections.singletonList(event));

        service.manualRetry(100, "evt_1");

        assertEquals("PENDING", event.getDeliveryStatus());
        assertEquals(Integer.valueOf(3), event.getAttemptCount());
        verify(eventMapper).updateById(event);
    }

    private static final class CapturingHttpClient
            implements RcsCustomerEventHttpClient {
        private int status = 200;
        private int callCount;
        private String url;
        private String body;
        private Map<String, String> headers;

        @Override
        public int post(String url, String body, Map<String, String> headers) {
            this.callCount++;
            this.url = url;
            this.body = body;
            this.headers = headers;
            return status;
        }
    }
}
