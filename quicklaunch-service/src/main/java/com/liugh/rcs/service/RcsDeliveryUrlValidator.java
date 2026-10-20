package com.liugh.rcs.service;

import java.net.InetAddress;
import java.net.URI;

final class RcsDeliveryUrlValidator {

    private RcsDeliveryUrlValidator() {
    }

    static void requirePublicHttps(String value, boolean resolveDns) {
        URI uri;
        try {
            uri = URI.create(value == null ? "" : value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid delivery URL", e);
        }
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || isBlank(host) || uri.getUserInfo() != null
                || "localhost".equalsIgnoreCase(host)) {
            throw new IllegalArgumentException("Delivery URL must be public HTTPS");
        }
        if (resolveDns || isIpLiteral(host)) {
            try {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                for (InetAddress address : addresses) {
                    if (!isPublic(address)) {
                        throw new IllegalArgumentException(
                                "Delivery URL resolves to a non-public address");
                    }
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Delivery URL host cannot be resolved", e);
            }
        }
    }

    private static boolean isPublic(InetAddress address) {
        byte[] bytes = address.getAddress();
        boolean uniqueLocalIpv6 = bytes.length == 16
                && (bytes[0] & 0xfe) == 0xfc;
        return !address.isAnyLocalAddress()
                && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress()
                && !address.isSiteLocalAddress()
                && !address.isMulticastAddress()
                && !uniqueLocalIpv6;
    }

    private static boolean isIpLiteral(String host) {
        return host.indexOf(':') >= 0
                || host.matches("[0-9.]+");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
