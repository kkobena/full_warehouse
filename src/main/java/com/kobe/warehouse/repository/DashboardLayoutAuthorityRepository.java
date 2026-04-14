package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DashboardLayoutAuthority;
import com.kobe.warehouse.domain.DashboardLayoutAuthorityId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardLayoutAuthorityRepository
    extends JpaRepository<DashboardLayoutAuthority, DashboardLayoutAuthorityId> {

    /**
     * Layout par défaut pour un rôle donné.
     * Utilisé dans la résolution niveau 2 (fallback authority).
     */
    @Query("""
        SELECT dla FROM DashboardLayoutAuthority dla
        JOIN FETCH dla.layout
        WHERE dla.authority.name = :authorityName
          AND dla.isDefault = true
        """)
    Optional<DashboardLayoutAuthority> findDefaultByAuthorityName(@Param("authorityName") String authorityName);

    /**
     * Tous les layouts assignés à un rôle.
     */
    @Query("SELECT dla FROM DashboardLayoutAuthority dla JOIN FETCH dla.layout WHERE dla.authority.name = :authorityName")
    List<DashboardLayoutAuthority> findByAuthorityName(@Param("authorityName") String authorityName);

    /**
     * Toutes les associations pour un layout donné.
     */
    @Query("SELECT dla FROM DashboardLayoutAuthority dla JOIN FETCH dla.authority WHERE dla.layout.id = :layoutId")
    List<DashboardLayoutAuthority> findByLayoutId(@Param("layoutId") Integer layoutId);
}
