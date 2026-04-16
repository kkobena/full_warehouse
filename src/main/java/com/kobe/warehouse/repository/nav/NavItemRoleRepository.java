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
}

