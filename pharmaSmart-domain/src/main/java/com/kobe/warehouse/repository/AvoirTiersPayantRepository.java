package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AvoirTiersPayant;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AvoirTiersPayantRepository extends JpaRepository<AvoirTiersPayant, Long> {

    @Query("""
        SELECT a FROM AvoirTiersPayant a
        WHERE a.factureTiersPayant.tiersPayant.id = :tiersPayantId
          AND a.avoirDate BETWEEN :start AND :end
          AND a.statut IN :statuts
          AND (:numAvoir IS NULL OR LOWER(a.numAvoir) LIKE :numAvoir)
        """)
    Page<AvoirTiersPayant> searchByTiersPayant(
        @Param("tiersPayantId") Integer tiersPayantId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end,
        @Param("statuts") List<AvoirStatut> statuts,
        @Param("numAvoir") String numAvoir,
        Pageable pageable
    );

    @Query("""
        SELECT a FROM AvoirTiersPayant a
        WHERE a.avoirDate BETWEEN :start AND :end
          AND a.statut IN :statuts
          AND (:numAvoir IS NULL OR LOWER(a.numAvoir) LIKE :numAvoir)
        """)
    Page<AvoirTiersPayant> searchAll(
        @Param("start") LocalDate start,
        @Param("end") LocalDate end,
        @Param("statuts") List<AvoirStatut> statuts,
        @Param("numAvoir") String numAvoir,
        Pageable pageable
    );

    /**
     * Dernier numéro d'avoir attribué pour l'année donnée, 0 si elle n'en compte aucun.
     *
     * <p>Le numéro s'écrit {@code AV-2026_0007} : c'est le suffixe après le souligné qu'il faut
     * lire. La requête précédente prenait la chaîne à partir du 4e caractère — soit
     * {@code 2026_0007} — et la convertissait en entier, ce que PostgreSQL refuse. Le premier
     * avoir passait (le MAX d'une table vide n'évalue rien), le second échouait.
     *
     * <p>Le compteur est cadré sur l'année, comme celui des factures : le préfixe rend les
     * numéros uniques d'une année sur l'autre.
     */
    @Query(
        value = """
            SELECT COALESCE(MAX(CAST(SPLIT_PART(num_avoir, '_', 2) AS int)), 0)
            FROM avoir_tiers_payant
            WHERE starts_with(num_avoir, CONCAT('AV-', CAST(:annee AS text), '_'))
            """,
        nativeQuery = true
    )
    int findMaxNumeroAvoir(@Param("annee") int annee);
}
