package com.kobe.warehouse.repository.nav;

import com.kobe.warehouse.domain.nav.NavItemRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NavItemRoleRepository extends JpaRepository<NavItemRole, Integer> {

    List<NavItemRole> findAllByNavItemIdInAndRoleNameIn(List<Integer> navItemIds, Set<String> roleNames);

    List<NavItemRole> findAllByRoleName(String roleName);

    Optional<NavItemRole> findByNavItemIdAndRoleName(Integer navItemId, String roleName);
}

