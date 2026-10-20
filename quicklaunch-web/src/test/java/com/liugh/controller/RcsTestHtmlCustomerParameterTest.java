package com.liugh.controller;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class RcsTestHtmlCustomerParameterTest {

    @Test
    public void customerConsoleExposesAndMapsMessageAndCapabilityParameters() throws IOException {
        String html = readTestPage();

        assertTrue(html.contains("id=\"custSender\""));
        assertTrue(html.contains("id=\"custRecipient\""));
        assertTrue(html.contains("id=\"custPhoneNumbers\""));
        assertTrue(html.contains("data-cust-var=\"sender\""));
        assertTrue(html.contains("data-cust-var=\"recipient\""));
        assertTrue(html.contains("data-cust-var=\"phoneNumbers\""));
        assertTrue(html.contains("id=\"custContent\""));
        assertTrue(html.contains("data-cust-var=\"content\""));

        assertTrue(html.contains("sender:\"{{sender}}\""));
        assertTrue(html.contains("recipient:\"{{recipient}}\""));
        assertTrue(html.contains("phoneNumbers:\"{{phoneNumbers}}\""));
        assertTrue(html.contains("content:\"{{content}}\""));
        assertTrue(html.contains("sender:$(\"custSender\").value"));
        assertTrue(html.contains("recipient:$(\"custRecipient\").value"));
        assertTrue(html.contains("phoneNumbers:parseCustomerPhoneNumbers"));
        assertTrue(html.contains("content:parseCustomerContent()"));
    }

    @Test
    public void customerConsoleSynchronizesInputsAndShowsOnlyRelevantParameters() throws IOException {
        String html = readTestPage();

        assertTrue(html.contains("function customerTemplateVariables(entry)"));
        assertTrue(html.contains("function renderCustomerVariableVisibility(entry)"));
        assertTrue(html.contains("data-cust-content=\"TEXT\""));
        assertTrue(html.contains("data-cust-content=\"FILE\""));
        assertTrue(html.contains("data-cust-content=\"CARD\""));
        assertTrue(html.contains("data-cust-content=\"CAROUSEL\""));
        assertTrue(html.contains("function setCustomerContentPreset(type)"));
        assertTrue(html.contains("input.addEventListener(\"input\", renderCustomerApi)"));
    }

    private String readTestPage() throws IOException {
        Path path = Paths.get("docs", "rcs-test.html");
        if (!Files.exists(path)) {
            path = Paths.get("..", "docs", "rcs-test.html");
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
