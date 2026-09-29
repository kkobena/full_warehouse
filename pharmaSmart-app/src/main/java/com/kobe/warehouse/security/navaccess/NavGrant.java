package com.kobe.warehouse.security.navaccess;

import com.kobe.warehouse.domain.enumeration.NavTargetType;
import com.kobe.warehouse.domain.nav.NavItem;
import com.kobe.warehouse.domain.nav.NavItemRole;

/** Droits d'un ensemble de rôles sur un {@code nav_item}, fusionnés par OU logique. */
public record NavGrant(
    String code,
    String libelle,
    NavTargetType targetType,
    String requiredFeature,
    boolean display,
    boolean access,
    boolean create,
    boolean edit,
    boolean delete,
    boolean export,
    boolean execute
) {
    static NavGrant of(NavItemRole role) {
        NavItem item = role.getNavItem();
        return new NavGrant(
            item.getCode(),
            item.getLibelle(),
            item.getTargetType(),
            item.getRequiredFeature(),
            role.isCanDisplay(),
            role.isCanAccess(),
            role.isCanCreate(),
            role.isCanEdit(),
            role.isCanDelete(),
            role.isCanExport(),
            role.isCanExecute()
        );
    }

    NavGrant merge(NavGrant other) {
        return new NavGrant(
            code,
            libelle,
            targetType,
            requiredFeature,
            display || other.display,
            access || other.access,
            create || other.create,
            edit || other.edit,
            delete || other.delete,
            export || other.export,
            execute || other.execute
        );
    }

    public boolean allows(NavAction action) {
        return switch (action) {
            case DISPLAY -> display;
            case ACCESS, AUTO -> access;
            case CREATE -> create;
            case EDIT -> edit;
            case DELETE -> delete;
            case EXPORT -> export;
            case EXECUTE -> execute;
        };
    }
}
