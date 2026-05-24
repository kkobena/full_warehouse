package com.kobe.warehouse.service.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class RequestOrigineService {

    private static final String TAURI_HEADER = "X-Tauri-App";

    public boolean isLocalHostRequest() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return false;
        }

        String remoteAddr = request.getRemoteAddr();
        String serverAddr = request.getLocalAddr();

        // Check if remote address matches server address
        if (remoteAddr != null && remoteAddr.equals(serverAddr)) {
            return true;
        }

        // Also check for standard localhost addresses
        return isLocalhostAddress(remoteAddr);
    }

    public boolean isTauriRequest() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return false;
        }

        // Check for custom Tauri header (most reliable)
        if (hasTauriHeader(request)) {
            return true;
        }

        // Check for Tauri User-Agent pattern
        if (hasTauriUserAgent(request)) {
            return isLocalHostRequest();
        }

        return false;
    }

    public boolean isLocalTauriRequest() {
        return isTauriRequest() && isLocalHostRequest();
    }

    public boolean isLocalAndNotTauriRequest() {
        return !isTauriRequest() && isLocalHostRequest();
    }

    /**
     * Check if request has custom Tauri header
     */
    private boolean hasTauriHeader(HttpServletRequest request) {
        String tauriHeader = request.getHeader(TAURI_HEADER);
        return tauriHeader != null && "true".equalsIgnoreCase(tauriHeader);
    }

    private boolean hasTauriUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return false;
        }

        userAgent = userAgent.toLowerCase();

        // Check for Tauri-specific patterns
        return (
            userAgent.contains("tauri") ||
            userAgent.contains("wry") || // Tauri's webview library
            userAgent.contains("pharma-smart-desktop")
        ); // Your custom app name
    }

    /**
     * Check if an IP address is localhost
     */
    private boolean isLocalhostAddress(String address) {
        if (address == null) {
            return false;
        }

        return (
            "127.0.0.1".equals(address) ||
            "0:0:0:0:0:0:0:1".equals(address) || // IPv6 localhost
            "::1".equals(address) || // IPv6 localhost short form
            "localhost".equalsIgnoreCase(address)
        );
    }

    /**
     * Get the current HTTP request from RequestContextHolder
     */
    private HttpServletRequest getCurrentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }
}
