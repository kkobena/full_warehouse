package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.DashboardComponentKey;
import com.kobe.warehouse.domain.enumeration.DashboardScope;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Dashboard Layout Entity
 * Stores customizable dashboard configurations for users
 */
@Entity
@Table(name = "dashboard_layout")
public class DashboardLayout implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    /**
     * Associations avec les Authority (table d'association dashboard_layout_authority).
     * Un même layout peut être assigné à plusieurs rôles sans duplication.
     * is_default est porté par l'association (par rôle), pas ici.
     */
    @OneToMany(mappedBy = "layout", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DashboardLayoutAuthority> authorityAssignments = new HashSet<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 10)
    private DashboardScope scope; // PRIVATE, SHARED, PUBLIC

    @Column(name = "is_default")
    private Boolean isDefault = false;

    /**
     * Si true : name contient une route Angular (ex: /sales-home/prevente).
     * HomeComponent redirige vers cette route au lieu d'afficher un layout GridStack.
     * layoutConfig est ignoré dans ce cas.
     */
    @Column(name = "is_route", nullable = false)
    private Boolean isRoute = false;

    /**
     * Clé de dispatch Angular — identifie le composant Angular à rendre.
     * Indépendant du rôle : un nouveau rôle peut recevoir componentKey='PHARMACIEN'
     * et bénéficier de jhi-home-base sans modifier le code frontend.
     * Valeurs : PHARMACIEN | CAISSIER | COMMANDE
     * Ignoré si isRoute=true (la route prend le dessus).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "component_key", length = 30)
    private DashboardComponentKey componentKey ;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_config", columnDefinition = "jsonb")
    private String layoutConfig; // JSON object with GridStack configuration (null si isRoute=true)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt= LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Set<DashboardLayoutAuthority> getAuthorityAssignments() {
        return authorityAssignments;
    }

    public void setAuthorityAssignments(Set<DashboardLayoutAuthority> authorityAssignments) {
        this.authorityAssignments = authorityAssignments;
    }

    public Boolean getIsRoute() {
        return isRoute;
    }

    public void setIsRoute(Boolean isRoute) {
        this.isRoute = isRoute;
    }

    public DashboardComponentKey getComponentKey() {
        return componentKey;
    }

    public DashboardLayout setComponentKey(DashboardComponentKey componentKey) {
        this.componentKey = componentKey;
        return this;
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
