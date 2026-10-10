package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PilotageVue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PilotageVueRepository extends JpaRepository<PilotageVue, Long> {
    /** Les vues livrées d'office d'abord, dans leur ordre, puis celles de l'utilisateur et celles partagées par l'équipe. */
    @Query(
        """
        SELECT v FROM PilotageVue v
        WHERE v.proprietaire IS NULL OR v.proprietaire.id = :utilisateurId OR v.partagee = true
        ORDER BY CASE WHEN v.proprietaire IS NULL THEN 0 ELSE 1 END, v.ordre, v.libelle
        """
    )
    List<PilotageVue> listerVisibles(Integer utilisateurId);
}
