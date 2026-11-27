package com.kobe.warehouse.service.remise;

import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.service.remise.dto.JournalRemisesFiltre;
import com.kobe.warehouse.service.remise.dto.LigneJournalDTO;
import com.kobe.warehouse.service.remise.dto.NatureJournal;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Le journal des remises accordées (promotions, remises produit) et des UG vendues, dans le magasin de
 * l'utilisateur (docs/PLAN-JOURNAL-REMISES.md).
 */
public interface JournalRemisesService {

    SyntheseJournalDTO calculerSynthese(JournalRemisesFiltre filtre);

    Page<LigneJournalDTO> listerLignes(NatureJournal nature, JournalRemisesFiltre filtre, Pageable pageable);

    /**
     * @param nature le journal à exporter ; nul pour la synthèse et les trois journaux
     */
    byte[] exporter(FormatExport format, NatureJournal nature, JournalRemisesFiltre filtre);

    /** Nom du fichier exporté, extension comprise. */
    String nommerFichier(FormatExport format, NatureJournal nature, JournalRemisesFiltre filtre);
}
