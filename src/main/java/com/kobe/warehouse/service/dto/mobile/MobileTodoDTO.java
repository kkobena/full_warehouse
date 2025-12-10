package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Todo list for mobile priority actions screen.
 */
public record MobileTodoDTO(
    List<TodoItemDTO> urgent,
    List<TodoItemDTO> important,
    List<TodoItemDTO> normal,
    int totalCount,
    LocalDateTime generatedAt
) {
    /**
     * Single todo item.
     */
    public record TodoItemDTO(
        Long id,
        String type,              // REORDER, CALL_CLIENT, CREATE_DISCOUNT, INVENTORY
        String title,
        String description,
        TodoPriority priority,
        String icon,
        String color,

        // Action button
        String actionLabel,       // "Commander", "Appeler", "Creer promotion", etc.
        String actionType,        // NAVIGATE, CALL, CREATE_ORDER, CREATE_DISCOUNT

        // Action metadata for navigation/action
        Map<String, Object> actionData,

        // Related entity
        Long relatedEntityId,
        String relatedEntityType,
        String relatedEntityName,

        // Timestamps
        LocalDateTime createdAt,
        LocalDateTime dueDate,

        // Status
        boolean isDismissed
    ) {}

    /**
     * Todo priority level.
     */
    public enum TodoPriority {
        URGENT("red", 1),
        IMPORTANT("orange", 2),
        NORMAL("blue", 3);

        private final String color;
        private final int order;

        TodoPriority(String color, int order) {
            this.color = color;
            this.order = order;
        }

        public String getColor() {
            return color;
        }

        public int getOrder() {
            return order;
        }
    }

    /**
     * Builder for creating todo lists.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<TodoItemDTO> urgent = List.of();
        private List<TodoItemDTO> important = List.of();
        private List<TodoItemDTO> normal = List.of();

        public Builder urgent(List<TodoItemDTO> urgent) {
            this.urgent = urgent;
            return this;
        }

        public Builder important(List<TodoItemDTO> important) {
            this.important = important;
            return this;
        }

        public Builder normal(List<TodoItemDTO> normal) {
            this.normal = normal;
            return this;
        }

        public MobileTodoDTO build() {
            int totalCount = urgent.size() + important.size() + normal.size();
            return new MobileTodoDTO(urgent, important, normal, totalCount, LocalDateTime.now());
        }
    }

    /**
     * Item builder for creating todo items.
     */
    public static TodoItemBuilder itemBuilder() {
        return new TodoItemBuilder();
    }

    public static class TodoItemBuilder {
        private Long id;
        private String type;
        private String title;
        private String description;
        private TodoPriority priority;
        private String icon;
        private String color;
        private String actionLabel;
        private String actionType;
        private Map<String, Object> actionData;
        private Long relatedEntityId;
        private String relatedEntityType;
        private String relatedEntityName;
        private LocalDateTime createdAt;
        private LocalDateTime dueDate;
        private boolean isDismissed;

        public TodoItemBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TodoItemBuilder type(String type) {
            this.type = type;
            return this;
        }

        public TodoItemBuilder title(String title) {
            this.title = title;
            return this;
        }

        public TodoItemBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TodoItemBuilder priority(TodoPriority priority) {
            this.priority = priority;
            this.color = priority.getColor();
            return this;
        }

        public TodoItemBuilder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public TodoItemBuilder actionLabel(String actionLabel) {
            this.actionLabel = actionLabel;
            return this;
        }

        public TodoItemBuilder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        public TodoItemBuilder actionData(Map<String, Object> actionData) {
            this.actionData = actionData;
            return this;
        }

        public TodoItemBuilder relatedEntityId(Long relatedEntityId) {
            this.relatedEntityId = relatedEntityId;
            return this;
        }

        public TodoItemBuilder relatedEntityType(String relatedEntityType) {
            this.relatedEntityType = relatedEntityType;
            return this;
        }

        public TodoItemBuilder relatedEntityName(String relatedEntityName) {
            this.relatedEntityName = relatedEntityName;
            return this;
        }

        public TodoItemBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TodoItemBuilder dueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public TodoItemBuilder isDismissed(boolean isDismissed) {
            this.isDismissed = isDismissed;
            return this;
        }

        public TodoItemDTO build() {
            return new TodoItemDTO(
                id, type, title, description, priority, icon, color,
                actionLabel, actionType, actionData,
                relatedEntityId, relatedEntityType, relatedEntityName,
                createdAt, dueDate, isDismissed
            );
        }
    }
}
