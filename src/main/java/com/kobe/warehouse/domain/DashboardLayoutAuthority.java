package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Table d'association entre DashboardLayout et Authority.
 *
 * Permet d'assigner un même layout à plusieurs rôles sans duplication.
 * Le champ isDefault est porté ICI (pas sur DashboardLayout) pour permettre
 * à chaque rôle d'avoir son propre layout par défaut de façon indépendante.
 *
 * Exemple :
 *   Layout "Pharmacien" → ROLE_ADMIN (isDefault=true), ROLE_PHARMACIEN (isDefault=true)
 *   Layout "Pharmacien" → ROLE_NOUVEAU_ROLE (isDefault=true)  ← sans changer les autres
 */
@Entity
@Table(name = "dashboard_layout_authority")
public class DashboardLayoutAuthority implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private DashboardLayoutAuthorityId id = new DashboardLayoutAuthorityId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("layoutId")
    @JoinColumn(name = "layout_id")
    private DashboardLayout layout;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("authorityName")
    @JoinColumn(name = "authority_name", referencedColumnName = "name")
    private Authority authority;

    /**
     * Ce layout est-il le dashboard par défaut pour ce rôle ?
     * Indépendant des autres (layout × authority).
     */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    public DashboardLayoutAuthority() {}

    public DashboardLayoutAuthority(DashboardLayout layout, Authority authority, boolean isDefault) {
        this.layout = layout;
        this.authority = authority;
        this.isDefault = isDefault;
        this.id = new DashboardLayoutAuthorityId(layout.getId(), authority.getName());
    }

    public DashboardLayoutAuthorityId getId() { return id; }
    public void setId(DashboardLayoutAuthorityId id) { this.id = id; }

    public DashboardLayout getLayout() { return layout; }
    public void setLayout(DashboardLayout layout) { this.layout = layout; }

    public Authority getAuthority() { return authority; }
    public void setAuthority(Authority authority) { this.authority = authority; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
