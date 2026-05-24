package com.kobe.warehouse.domain.nav;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.NavBadgeType;
import com.kobe.warehouse.domain.enumeration.NavTargetType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entité de navigation dynamique — indépendante de l'entité Menu existante.
 */
@Entity
@Table(name = "nav_item", uniqueConstraints = { @UniqueConstraint(columnNames = { "code" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class NavItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "code", nullable = false, length = 60, unique = true)
    private String code;

    @NotNull
    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @Column(name = "icon", length = 80)
    private String icon;

    @Column(name = "router_link", length = 150)
    private String routerLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnoreProperties(value = "children", allowSetters = true)
    private NavItem parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<NavItem> children = new ArrayList<>();

    @Column(name = "ordre", nullable = false)
    private int ordre;

    @Column(name = "niveau", nullable = false)
    private int niveau = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", length = 20)
    private NavBadgeType badgeType = NavBadgeType.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private NavTargetType targetType = NavTargetType.ROUTE;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false)
    private LocalDateTime created;

    @UpdateTimestamp
    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    public Integer getId() {
        return id;
    }

    public NavItem setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public NavItem setCode(String code) {
        this.code = code;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public NavItem setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public String getIcon() {
        return icon;
    }

    public NavItem setIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public String getRouterLink() {
        return routerLink;
    }

    public NavItem setRouterLink(String routerLink) {
        this.routerLink = routerLink;
        return this;
    }

    public NavItem getParent() {
        return parent;
    }

    public NavItem setParent(NavItem parent) {
        this.parent = parent;
        return this;
    }

    public List<NavItem> getChildren() {
        return children;
    }

    public NavItem setChildren(List<NavItem> children) {
        this.children = children;
        return this;
    }

    public int getOrdre() {
        return ordre;
    }

    public NavItem setOrdre(int ordre) {
        this.ordre = ordre;
        return this;
    }

    public int getNiveau() {
        return niveau;
    }

    public NavItem setNiveau(int niveau) {
        this.niveau = niveau;
        return this;
    }

    public NavBadgeType getBadgeType() {
        return badgeType;
    }

    public NavItem setBadgeType(NavBadgeType badgeType) {
        this.badgeType = badgeType;
        return this;
    }

    public NavTargetType getTargetType() {
        return targetType;
    }

    public NavItem setTargetType(NavTargetType targetType) {
        this.targetType = targetType;
        return this;
    }

    public boolean isActif() {
        return actif;
    }

    public NavItem setActif(boolean actif) {
        this.actif = actif;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavItem)) return false;
        return id != null && id.equals(((NavItem) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "NavItem{id=" + id + ", code='" + code + "', libelle='" + libelle + "'}";
    }
}

