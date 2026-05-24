package com.kobe.warehouse.service.pharmaml.service;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.service.pharmaml.dto.CsrpEnveloppe;
import com.kobe.warehouse.service.pharmaml.dto.EnvoiParamsDTO;
import com.kobe.warehouse.service.pharmaml.dto.LigneRetourDTO;
import java.util.List;

/**
 * Service responsable de la construction des payloads XML PharmaML. Ce service gère uniquement la
 * création des enveloppes CSRP pour les différents types de messages.
 */
public interface PharmaMlPayloadBuilderService {

    /**
     * Construit le payload pour l'envoi d'une commande.
     *
     * @param commande    la commande à envoyer
     * @param params      les paramètres d'envoi
     * @param fournisseur le fournisseur destinataire
     * @param refMessage  la référence unique du message
     * @return l'enveloppe CSRP prête à être sérialisée
     */
    CsrpEnveloppe buildCommandePayload(Commande commande, EnvoiParamsDTO params,
        Fournisseur fournisseur, String refMessage);


    /**
     * Construit le payload pour une demande d'information (disponibilité).
     *
     * @param commande    la commande pour laquelle on demande la disponibilité
     * @param fournisseur le fournisseur destinataire
     * @param refMessage  la référence unique du message
     * @return l'enveloppe CSRP prête à être sérialisée
     */
    CsrpEnveloppe buildInfoPayload(Commande commande, Fournisseur fournisseur, String refMessage);

    /**
     * Construit le payload pour une demande d'information (disponibilité) à partir de lignes de
     * suggestion.
     *
     * @param lignes      les lignes de suggestion dont on demande la disponibilité
     * @param fournisseur le fournisseur destinataire
     * @param refMessage  la référence unique du message
     * @return l'enveloppe CSRP prête à être sérialisée
     */
    CsrpEnveloppe buildInfoPayloadFromSuggestionLines(List<SuggestionLine> lignes,
        Fournisseur fournisseur, String refMessage);

    /**
     * Construit le payload pour l'envoi d'un bon de retour fournisseur.
     *
     * @param commande    la commande d'origine du retour
     * @param fournisseur le fournisseur destinataire
     * @param lignes      les lignes de retour
     * @param refMessage  la référence unique du message
     * @return l'enveloppe CSRP prête à être sérialisée
     */
    CsrpEnveloppe buildRetourPayload(Commande commande, Fournisseur fournisseur,
        List<LigneRetourDTO> lignes, String refMessage);

    /**
     * Génère une référence de message unique.
     *
     * @return une chaîne de 14 caractères alphanumériques en majuscules
     */
    String generateRefMessage();
}

