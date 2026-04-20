package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AvoirFournisseur;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvoirFournisseurRepository extends JpaRepository<AvoirFournisseur, Integer> {

    @Query("""
        SELECT a FROM AvoirFournisseur a
        WHERE (:statut IS NULL OR a.statut = :statut)
          AND (:fournisseurId IS NULL OR a.fournisseur.id = :fournisseurId)
          AND (:dtStart IS NULL OR a.dateMtv >= :dtStart)
          AND (:dtEnd IS NULL OR a.dateMtv <= :dtEnd)
        ORDER BY a.dateMtv DESC
        """)
    Page<AvoirFournisseur> findAll(
        @Param("statut") AvoirFournisseurStatut statut,
        @Param("fournisseurId") Integer fournisseurId,
        @Param("dtStart") LocalDateTime dtStart,
        @Param("dtEnd") LocalDateTime dtEnd,
        Pageable pageable
    );

    @Query("""
        SELECT a.fournisseur.id, a.fournisseur.libelle, SUM(a.montant)
        FROM AvoirFournisseur a
        WHERE a.statut = 'EN_ATTENTE'
        GROUP BY a.fournisseur.id, a.fournisseur.libelle
        ORDER BY SUM(a.montant) DESC
        """)
    List<Object[]> sumEncoursParFournisseur();
}
