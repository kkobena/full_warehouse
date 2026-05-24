package com.kobe.warehouse.service.pharmaml.service;

import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import com.kobe.warehouse.service.pharmaml.dto.DispoGrossisteResultDTO;
import com.kobe.warehouse.service.pharmaml.dto.DispoMultiRequestDTO;
import com.kobe.warehouse.service.pharmaml.dto.EnvoiParamsDTO;
import com.kobe.warehouse.service.pharmaml.dto.InfoProduitDTO;
import com.kobe.warehouse.service.pharmaml.dto.PharmaMlEnvoiDTO;
import com.kobe.warehouse.service.pharmaml.dto.PharmamlCommandeResponse;
import com.kobe.warehouse.service.pharmaml.dto.SubstitutionProposeeDTO;
import java.time.LocalDate;
import java.util.List;

public interface PharmaMlService {

    PharmamlCommandeResponse envoiPharmaCommande(EnvoiParamsDTO envoiParamsDTO);


    VerificationResponseCommandeDTO lignesCommandeRetour(String commandeRef, String orderId);

    void renvoiPharmaCommande(EnvoiParamsDTO envoiParamsDTO);

    VerificationResponseCommandeDTO reponseRupture(String ruptureId);

    List<PharmaMlEnvoiDTO> getHistoriqueEnvois(Integer commandeId, LocalDate orderDate);

    PharmaMlEnvoiDTO getStatutEnvoi(Integer envoiId);

    List<SubstitutionProposeeDTO> getSubstitutionsEnAttente(Integer commandeId,
        LocalDate orderDate);

    void accepterSubstitution(Integer substitutionId);

    void refuserSubstitution(Integer substitutionId);

    List<InfoProduitDTO> demanderDisponibilite(Integer commandeId, LocalDate orderDate,
        Integer grossisteId);

    List<DispoGrossisteResultDTO> demanderDisponibiliteMulti(DispoMultiRequestDTO request);

    /**
     * Envoie un bon de retour fournisseur via EDI PharmaML.
     *
     * @param retourBonId l'identifiant du retour à envoyer
     */
    void envoiRetourBon(Integer retourBonId);
}
