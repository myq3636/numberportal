package com.liugh.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

public class RcsCustomerApiUrlMappingTest {

    private static final Class<?>[] CONTROLLERS = {
            RcsAgentEventController.class,
            RcsCustomerEventController.class,
            RcsFileResourceController.class,
            RcsDeliveryConfigController.class,
            RcsSenderManagementController.class,
            RcsLaunchManagementController.class
    };

    @Test
    public void exposesApprovedUniqueCustomerUrls() {
        Set<String> mappings = mappings();

        for (String expected : Arrays.asList(
                "POST /rcs/events/send",
                "GET /rcs/events/list",
                "GET /rcs/files/{uid}/metadata",
                "POST /rcs/files/{uid}/delete",
                "GET /rcs/delivery-config/query",
                "POST /rcs/delivery-config/save",
                "POST /rcs/senders/create",
                "GET /rcs/senders/list",
                "GET /rcs/senders/{senderId}/detail",
                "POST /rcs/senders/{senderId}/update",
                "POST /rcs/senders/{senderId}/platform-parameters/update",
                "POST /rcs/senders/{senderId}/test-numbers/add",
                "GET /rcs/senders/{senderId}/test-numbers/list",
                "POST /rcs/senders/{senderId}/test-numbers/{number}/primary",
                "POST /rcs/senders/{senderId}/templates/create",
                "GET /rcs/senders/{senderId}/templates/list",
                "GET /rcs/senders/{senderId}/templates/{templateId}/detail",
                "POST /rcs/senders/{senderId}/templates/{templateId}/update",
                "POST /rcs/senders/{senderId}/templates/{templateId}/delete",
                "POST /rcs/senders/{senderId}/launch-requests/submit",
                "GET /rcs/senders/{senderId}/launch-requests/list",
                "GET /rcs/launch-requests/{requestId}/detail",
                "POST /rcs/launch-requests/{requestId}/update",
                "POST /rcs/launch-requests/{requestId}/cancel")) {
            assertTrue("Missing mapping: " + expected, mappings.contains(expected));
        }
    }

    @Test
    public void exposesOnlyGetAndPostMethods() {
        for (String mapping : mappings()) {
            assertTrue("Unsupported customer HTTP method: " + mapping,
                    mapping.startsWith("GET ") || mapping.startsWith("POST "));
        }
    }

    @Test
    public void doesNotExposeDifferentMethodsOnTheSameCustomerUrl() {
        Set<String> paths = new HashSet<String>();
        for (String mapping : mappings()) {
            String path = mapping.substring(mapping.indexOf(' ') + 1);
            assertTrue("Duplicate customer URL: " + path, paths.add(path));
        }
        assertFalse(paths.isEmpty());
        assertEquals(mappings().size(), paths.size());
    }

    private Set<String> mappings() {
        Set<String> result = new HashSet<String>();
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                add(result, "GET", method.getAnnotation(GetMapping.class));
                add(result, "POST", method.getAnnotation(PostMapping.class));
                add(result, "PUT", method.getAnnotation(PutMapping.class));
                add(result, "PATCH", method.getAnnotation(PatchMapping.class));
                add(result, "DELETE", method.getAnnotation(DeleteMapping.class));
            }
        }
        return result;
    }

    private void add(Set<String> result, String httpMethod,
            GetMapping mapping) {
        if (mapping != null) {
            add(result, httpMethod, mapping.value());
        }
    }

    private void add(Set<String> result, String httpMethod,
            PostMapping mapping) {
        if (mapping != null) {
            add(result, httpMethod, mapping.value());
        }
    }

    private void add(Set<String> result, String httpMethod,
            PutMapping mapping) {
        if (mapping != null) {
            add(result, httpMethod, mapping.value());
        }
    }

    private void add(Set<String> result, String httpMethod,
            PatchMapping mapping) {
        if (mapping != null) {
            add(result, httpMethod, mapping.value());
        }
    }

    private void add(Set<String> result, String httpMethod,
            DeleteMapping mapping) {
        if (mapping != null) {
            add(result, httpMethod, mapping.value());
        }
    }

    private void add(Set<String> result, String httpMethod,
            String[] paths) {
        for (String path : paths) {
            result.add(httpMethod + " " + path);
        }
    }
}
