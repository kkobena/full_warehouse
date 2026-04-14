package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.DashboardScope;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Dashboard Layout
 */
public class DashboardLayoutDTO {

    private Integer id;
    private String name;
    private String description;
    private Integer userId;
    private String userLogin;
    /** Rôles auxquels ce layout est assigné (many-to-many via dashboard_layout_authority). */
    private List<String> authorityNames;
    private DashboardScope scope;
    private Boolean isDefault;
    /**
     * Si true : name est une route Angular → HomeComponent redirige.
     * Si false : componentKey + layoutConfig déterminent le composant et sa config.
     */
    private Boolean isRoute;
    /**
     * Clé de dispatch Angular (ignorée si isRoute=true).
     * Valeurs : PHARMACIEN | CAISSIER | COMMANDE | DEFAULT
     */
    private String componentKey;
    private String layoutConfig; // JSON string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors

    public DashboardLayoutDTO() {}

    public DashboardLayoutDTO(
        Integer id,
        String name,
        String description,
        Integer userId,
        String userLogin,
        List<String> authorityNames,
        DashboardScope scope,
        Boolean isDefault,
        Boolean isRoute,
        String componentKey,
        String layoutConfig,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.userLogin = userLogin;
        this.authorityNames = authorityNames;
        this.scope = scope;
        this.isDefault = isDefault;
        this.isRoute = isRoute;
        this.componentKey = componentKey;
        this.layoutConfig = layoutConfig;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public List<String> getAuthorityNames() {
        return authorityNames;
    }

    public void setAuthorityNames(List<String> authorityNames) {
        this.authorityNames = authorityNames;
    }

    public Boolean getIsRoute() {
        return isRoute;
    }

    public void setIsRoute(Boolean isRoute) {
        this.isRoute = isRoute;
    }

    public String getComponentKey() {
        return componentKey;
    }

    public void setComponentKey(String componentKey) {
        this.componentKey = componentKey;
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
