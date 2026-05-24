package com.kobe.warehouse.domain.nav;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Association NavItem ↔ rôle avec permissions fines.
 */
@Entity
@Table(
    name = "nav_item_role",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "nav_item_id", "role_name" }) }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class NavItemRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "nav_item_id", nullable = false)
    private NavItem navItem;

    @NotNull
    @Column(name = "role_name", nullable = false, length = 60)
    private String roleName;

    @Column(name = "can_display", nullable = false)
    private boolean canDisplay = true;

    @Column(name = "can_access", nullable = false)
    private boolean canAccess = true;

    @Column(name = "can_create", nullable = false)
    private boolean canCreate = false;

    @Column(name = "can_edit", nullable = false)
    private boolean canEdit = false;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = false;

    @Column(name = "can_export", nullable = false)
    private boolean canExport = false;

    @Column(name = "can_execute", nullable = false)
    private boolean canExecute = false;

    public Integer getId() {
        return id;
    }

    public NavItemRole setId(Integer id) {
        this.id = id;
        return this;
    }

    public NavItem getNavItem() {
        return navItem;
    }

    public NavItemRole setNavItem(NavItem navItem) {
        this.navItem = navItem;
        return this;
    }

    public String getRoleName() {
        return roleName;
    }

    public NavItemRole setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    public boolean isCanDisplay() {
        return canDisplay;
    }

    public NavItemRole setCanDisplay(boolean canDisplay) {
        this.canDisplay = canDisplay;
        return this;
    }

    public boolean isCanAccess() {
        return canAccess;
    }

    public NavItemRole setCanAccess(boolean canAccess) {
        this.canAccess = canAccess;
        return this;
    }

    public boolean isCanCreate() {
        return canCreate;
    }

    public NavItemRole setCanCreate(boolean canCreate) {
        this.canCreate = canCreate;
        return this;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public NavItemRole setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
        return this;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public NavItemRole setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
        return this;
    }

    public boolean isCanExport() {
        return canExport;
    }

    public NavItemRole setCanExport(boolean canExport) {
        this.canExport = canExport;
        return this;
    }

    public boolean isCanExecute() {
        return canExecute;
    }

    public NavItemRole setCanExecute(boolean canExecute) {
        this.canExecute = canExecute;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavItemRole)) return false;
        return id != null && id.equals(((NavItemRole) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

