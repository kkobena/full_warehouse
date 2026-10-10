package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ExportModele;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExportModeleRepository extends JpaRepository<ExportModele, Long> {
    /** Les modèles de l'utilisateur, puis ceux que l'équipe partage. */
    @Query(
        """
        SELECT m FROM ExportModele m JOIN FETCH m.proprietaire p
        WHERE p.id = :utilisateurId OR m.partage = true
        ORDER BY CASE WHEN p.id = :utilisateurId THEN 0 ELSE 1 END, m.libelle
        """
    )
    List<ExportModele> listerVisibles(Integer utilisateurId);

    @Query("SELECT m FROM ExportModele m JOIN FETCH m.proprietaire WHERE m.frequence IS NOT NULL AND m.prochaineExecution <= :maintenant")
    List<ExportModele> listerProgrammesDus(LocalDateTime maintenant);
}
