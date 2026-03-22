package com.kobe.warehouse.service.pharmaml.service;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.service.pharmaml.dto.CsrpEnveloppe;
import com.kobe.warehouse.service.pharmaml.dto.EnvoiParamsDTO;
import com.kobe.warehouse.service.pharmaml.dto.LigneRetourDTO;

import java.util.List;

/**
 * Service responsable de la construction des payloads XML PharmaML.
 * Ce service gère uniquement la création des enveloppes CSRP pour les différents types de messages.
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
    CsrpEnveloppe buildCommandePayload(Commande commande, EnvoiParamsDTO params, Fournisseur fournisseur, String refMessage);

    /**
     * Construit le payload pour un accusé de réception.
     *
     * @param commande    la commande concernée
     * @param fournisseur le fournisseur destinataire
     * @param refMessage  la référence unique du message
     * @return l'enveloppe CSRP prête à être sérialisée
     */
    CsrpEnveloppe buildAckPayload(Commande commande, Fournisseur fournisseur, String refMessage);

    /**
     * Construit le payload pour une annulation de commande.
     *
     * @param commande    la commande à annuler
     * @param fournisseur le fournisseur destinataire
     * @param refMessage  la référence unique du message
     * @param motif       le motif d'annulation
     * @return l'enveloppe CSRP prête à être sérialisée
     */
    CsrpEnveloppe buildAnnulationPayload(Commande commande, Fournisseur fournisseur, String refMessage, String motif);

    /**
     * Construit le payload pour une demande de retour.
     *
     * @param commande    la commande concernée
     * @param fournisseur le fournisseur destinataire
     * @param refMessage  la référence unique du message
     * @param lignes      les lignes à retourner
     * @return l'enveloppe CSRP prête à être sérialisée
     */
    CsrpEnveloppe buildRetourPayload(Commande commande, Fournisseur fournisseur, String refMessage, List<LigneRetourDTO> lignes);

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
     * Construit le payload pour une demande d'information (disponibilité) à partir de lignes de suggestion.
     *
     * @param lignes      les lignes de suggestion dont on demande la disponibilité
     * @param fournisseur le fournisseur destinataire
     * @param refMessage  la référence unique du message
     * @return l'enveloppe CSRP prête à être sérialisée
     */
    CsrpEnveloppe buildInfoPayloadFromSuggestionLines(List<SuggestionLine> lignes, Fournisseur fournisseur, String refMessage);

    /**
     * Génère une référence de message unique.
     *
     * @return une chaîne de 14 caractères alphanumériques en majuscules
     */
    String generateRefMessage();
}

