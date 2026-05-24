package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Detailed alert information for mobile alerts screen.
 */
public record MobileAlertDetailDTO(
    Long id,
    String type,              // STOCK_RUPTURE, EXPIRY, CASH_DISCREPANCY, INVOICE_OVERDUE
    String severity,          // CRITICAL, WARNING, INFO
    String title,
    String message,
    String icon,
    String color,
    LocalDateTime createdAt,
    boolean isRead,
    boolean isResolved,

    // Action metadata for deep linking
    String actionType,        // VIEW_PRODUCT, VIEW_INVOICE, VIEW_CASH_REGISTER, CREATE_ORDER
    Map<String, Object> actionData,

    // Related entity info
    Long relatedEntityId,
    String relatedEntityType,
    String relatedEntityName
) {
    /**
     * Builder for creating alerts from different sources.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String type;
        private String severity;
        private String title;
        private String message;
        private String icon;
        private String color;
        private LocalDateTime createdAt;
        private boolean isRead;
        private boolean isResolved;
        private String actionType;
        private Map<String, Object> actionData;
        private Long relatedEntityId;
        private String relatedEntityType;
        private String relatedEntityName;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder isRead(boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public Builder isResolved(boolean isResolved) {
            this.isResolved = isResolved;
            return this;
        }

        public Builder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder actionData(Map<String, Object> actionData) {
            this.actionData = actionData;
            return this;
        }

        public Builder relatedEntityId(Long relatedEntityId) {
            this.relatedEntityId = relatedEntityId;
            return this;
        }

        public Builder relatedEntityType(String relatedEntityType) {
            this.relatedEntityType = relatedEntityType;
            return this;
        }

        public Builder relatedEntityName(String relatedEntityName) {
            this.relatedEntityName = relatedEntityName;
            return this;
        }

        public MobileAlertDetailDTO build() {
            return new MobileAlertDetailDTO(
                id, type, severity, title, message, icon, color,
                createdAt, isRead, isResolved, actionType, actionData,
                relatedEntityId, relatedEntityType, relatedEntityName
            );
        }
    }
}
