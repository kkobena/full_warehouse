package com.kobe.warehouse.service.pharmaml.service;

import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import com.kobe.warehouse.service.pharmaml.dto.EnvoiParamsDTO;
import com.kobe.warehouse.service.pharmaml.dto.PharmamlCommandeResponse;

public interface PharmaMlService {
    PharmamlCommandeResponse envoiPharmaCommande(EnvoiParamsDTO envoiParamsDTO);

    void envoiPharmaInfosProduit(String commandeId);

    VerificationResponseCommandeDTO lignesCommandeRetour(String commandeRef, String orderId);

    void renvoiPharmaCommande(EnvoiParamsDTO envoiParamsDTO);

    VerificationResponseCommandeDTO reponseRupture(String ruptureId);
}
