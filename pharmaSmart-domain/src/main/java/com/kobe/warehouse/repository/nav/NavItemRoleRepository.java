package com.kobe.warehouse.repository.nav;

import com.kobe.warehouse.domain.enumeration.NavTargetType;
import com.kobe.warehouse.domain.nav.NavItemRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.kobe.warehouse.service.dto.projection.NavItemCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NavItemRoleRepository extends JpaRepository<NavItemRole, Integer> {

    List<NavItemRole> findAllByNavItemIdInAndRoleNameIn(List<Integer> navItemIds, Set<String> roleNames);

    List<NavItemRole> findAllByRoleName(String roleName);

    Optional<NavItemRole> findByNavItemIdAndRoleName(Integer navItemId, String roleName);
    @Query("SELECT DISTINCT r.navItem.code AS code FROM NavItemRole r WHERE r.roleName = :roleName AND r.canExecute = true AND r.navItem.targetType = :targetType")
    Set<NavItemCode> findAllNavItemCodeByRoleNameAndCanExecuteTrueAndNavItemTargetType(String roleName, NavTargetType targetType);

    /**
     * Codes des items ACTION exécutables par l'union des rôles donnés.
     * Utilisé par les clients légers (mobile) qui n'ont besoin que des permissions
     * d'action, sans construire tout l'arbre de navigation.
     */
    @Query(
        """
            SELECT DISTINCT r.navItem.code FROM NavItemRole r
            WHERE r.roleName IN :roleNames
              AND r.canExecute = true
              AND r.navItem.actif = true
              AND r.navItem.targetType = :targetType
            """
    )
    Set<String> findExecutableCodesByRoles(Set<String> roleNames, NavTargetType targetType);
}

