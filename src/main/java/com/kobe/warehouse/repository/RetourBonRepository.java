package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.enumeration.RetourBonStatut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings("unused")
@Repository
public interface RetourBonRepository extends JpaRepository<RetourBon, Long> {
    Page<RetourBon> findAllByStatutOrderByDateMtvDesc(RetourBonStatut statut, Pageable pageable);

    Page<RetourBon> findAllByOrderByDateMtvDesc(Pageable pageable);

    @Query("SELECT r FROM RetourBon r WHERE r.commande.id = :commandeId ORDER BY r.dateMtv DESC")
    List<RetourBon> findAllByCommandeId(@Param("commandeId") Long commandeId);

    @Query("SELECT r FROM RetourBon r WHERE r.dateMtv BETWEEN :startDate AND :endDate ORDER BY r.dateMtv DESC")
    Page<RetourBon> findAllByDateMtvBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}
