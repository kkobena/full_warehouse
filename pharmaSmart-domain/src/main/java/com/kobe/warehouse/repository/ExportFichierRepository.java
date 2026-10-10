package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ExportFichier;
import com.kobe.warehouse.domain.enumeration.StatutExport;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExportFichierRepository extends JpaRepository<ExportFichier, Long> {
    /** Historique d'un utilisateur, le plus récent d'abord. */
    @EntityGraph(attributePaths = { "demandePar", "modele" })
    Page<ExportFichier> findByDemandeParIdOrderByDemandeLeDesc(Integer utilisateurId, Pageable page);

    /** Historique de toute l'officine (administrateur). */
    @EntityGraph(attributePaths = { "demandePar", "modele" })
    Page<ExportFichier> findAllByOrderByDemandeLeDesc(Pageable page);

    @Query("SELECT f FROM ExportFichier f WHERE f.statut = :statut AND f.expireLe < :maintenant")
    List<ExportFichier> listerExpires(StatutExport statut, LocalDateTime maintenant);
}
