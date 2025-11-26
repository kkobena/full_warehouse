package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.DashboardScope;
import java.time.LocalDateTime;

/**
 * DTO for Dashboard Layout
 */
public class DashboardLayoutDTO {

    private Long id;
    private String name;
    private String description;
    private Integer userId;
    private String userLogin;
    private DashboardScope scope;
    private Boolean isDefault;
    private String layoutConfig; // JSON string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors

    public DashboardLayoutDTO() {}

    public DashboardLayoutDTO(
        Long id,
        String name,
        String description,
        Integer userId,
        String userLogin,
        DashboardScope scope,
        Boolean isDefault,
        String layoutConfig,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.userLogin = userLogin;
        this.scope = scope;
        this.isDefault = isDefault;
        this.layoutConfig = layoutConfig;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public DashboardScope getScope() {
        return scope;
    }

    public void setScope(DashboardScope scope) {
        this.scope = scope;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getLayoutConfig() {
        return layoutConfig;
    }

    public void setLayoutConfig(String layoutConfig) {
        this.layoutConfig = layoutConfig;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
