package com.kobe.warehouse.service.exports;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.ExportModele;
import com.kobe.warehouse.service.dto.exports.DemandeExportDTO;
import com.kobe.warehouse.service.dto.exports.ExportFichierDTO;
import com.kobe.warehouse.service.dto.exports.FichierExporteDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Fichiers d'export : demande (générée en tâche de fond), historique, téléchargement, expiration. */
public interface ExportsFichiersService {
    /** Inscrit l'export dans l'historique et lance sa génération ; refusé (403) sans le droit de la rubrique. */
    ExportFichierDTO demander(DemandeExportDTO demande);

    /** Lance l'export d'un modèle pour {@code demandeur} ; les droits ont été vérifiés par l'appelant. */
    ExportFichierDTO lancerModele(ExportModele modele, AppUser demandeur, LocalDate aujourdhui);

    /** Ses propres exports ; ceux de toute l'officine pour l'administrateur. */
    Page<ExportFichierDTO> listerHistorique(Pageable page);

    /** Le fichier d'un export terminé et non expiré, au demandeur ou à l'administrateur, si le droit de la rubrique tient toujours. */
    FichierExporteDTO lireFichier(Long id);

    /** Supprime les fichiers expirés ; leur trace reste dans l'historique. */
    void purgerExpires(LocalDateTime maintenant);
}
