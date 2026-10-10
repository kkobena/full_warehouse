package com.kobe.warehouse.service.exports;

import com.kobe.warehouse.service.dto.exports.ExportFichierDTO;
import com.kobe.warehouse.service.dto.exports.ExportModeleDTO;
import java.time.LocalDateTime;
import java.util.List;

/** Modèles d'export : enregistrés, partagés avec l'équipe, rejoués en un clic, programmés. */
public interface ModelesExportService {
    List<ExportModeleDTO> listerModeles();

    /** Crée (sans id) ou modifie un modèle de l'utilisateur courant ; calcule sa prochaine exécution s'il est programmé. */
    ExportModeleDTO enregistrerModele(ExportModeleDTO modele);

    void supprimerModele(Long id);

    /** Rejoue un modèle visible (le sien ou partagé) sur sa période relative recalculée. */
    ExportFichierDTO executerModele(Long id);

    /** Lance les modèles programmés arrivés à échéance, au nom de leur propriétaire. */
    void executerProgrammes(LocalDateTime maintenant);
}
