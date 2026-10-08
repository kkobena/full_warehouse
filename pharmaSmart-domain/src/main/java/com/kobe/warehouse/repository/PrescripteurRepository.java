package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ordonnance.Prescripteur;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PrescripteurRepository extends JpaRepository<Prescripteur, Integer> {
    List<Prescripteur> findByActifTrueOrderByCreatedAtDescIdDesc(Pageable pageable);

    /**
     * Fiches actives dont le nom (sans accent ni casse) contient la saisie ou lui ressemble
     * (pg_trgm), ou dont le numéro d'ordre est la saisie.
     */
    @Query(
        "select p from Prescripteur p where p.actif = true and (" +
        "p.recherche like concat('%', function('ref_normaliser', :q), '%') " +
        "or cast(function('similarity', p.recherche, function('ref_normaliser', :q)) as double) > 0.3 " +
        "or p.numeroOrdre = :q) " +
        "order by cast(function('similarity', p.recherche, function('ref_normaliser', :q)) as double) desc, p.nom"
    )
    List<Prescripteur> rechercher(@Param("q") String q, Pageable pageable);

    boolean existsByNumeroOrdre(String numeroOrdre);

    boolean existsByNumeroOrdreAndIdNot(String numeroOrdre, Integer id);
}
