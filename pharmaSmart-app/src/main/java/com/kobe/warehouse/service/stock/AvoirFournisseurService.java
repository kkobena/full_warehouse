package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.service.dto.AvoirEncoursFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFournisseurCommand;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AvoirFournisseurService {

    AvoirFournisseurDTO create(AvoirFournisseurCommand command);

    /** Called from RetourBonService when all items are accepted via the supplier-response modal. */
    AvoirFournisseurDTO createFromRetourBon(RetourBon retourBon, List<AvoirFournisseurCommand.AvoirLigneCommand> lignes, String commentaire);

    Page<AvoirFournisseurDTO> findAll(String reference,AvoirFournisseurStatut statut, Integer fournisseurId, LocalDate dtStart, LocalDate dtEnd, Pageable pageable);

    List<AvoirEncoursFournisseurDTO> getEncoursParFournisseur();

    AvoirFournisseurDTO updateStatut(Integer id, AvoirFournisseurStatut statut);

    AvoirFournisseurDTO annuler(Integer id, String motif);

    /** Retourne le nombre d'avoirs fournisseur en statut EN_ATTENTE. */
    long countEnAttente();
}
