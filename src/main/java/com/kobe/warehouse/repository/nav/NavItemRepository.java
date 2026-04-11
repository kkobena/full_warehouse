package com.kobe.warehouse.repository.nav;

import com.kobe.warehouse.domain.nav.NavItem;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NavItemRepository extends JpaRepository<NavItem, Integer> {

    List<NavItem> findAllByActifTrueOrderByOrdreAsc();

    /**
     * Charge tous les NavItem actifs ayant au moins un NavItemRole pour les rôles donnés.
     */
    @Query(
        """
        SELECT DISTINCT ni FROM NavItem ni
        JOIN NavItemRole nir ON nir.navItem = ni
        WHERE ni.actif = true
          AND nir.roleName IN :roles
        ORDER BY ni.ordre ASC
        """
    )
    List<NavItem> findAllActiveByRoles(@Param("roles") Set<String> roles);
}

