package com.kobe.warehouse.service.pharmaml;

import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;

public interface PharmaMlService {
    void envoiPharmaCommande(EnvoiParamsDTO envoiParamsDTO);

    void envoiPharmaInfosProduit(String commandeId);

    VerificationResponseCommandeDTO lignesCommandeRetour(String commandeRef, String orderId);

    void renvoiPharmaCommande(EnvoiParamsDTO envoiParamsDTO);

    VerificationResponseCommandeDTO reponseRupture(String ruptureId);
}
