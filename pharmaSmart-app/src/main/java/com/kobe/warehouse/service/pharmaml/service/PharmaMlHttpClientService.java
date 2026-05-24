package com.kobe.warehouse.service.pharmaml.service;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.PharmaMlEnvoi;
import com.kobe.warehouse.service.pharmaml.dto.CsrpEnveloppe;
import com.kobe.warehouse.service.pharmaml.dto.InfoProduitDTO;
import com.kobe.warehouse.service.pharmaml.dto.PharmamlCommandeResponse;

import java.util.List;

/**
 * Service responsable des communications HTTP avec les serveurs PharmaML.
 * Ce service gère la sérialisation XML, l'envoi des requêtes, et le parsing des réponses.
 */
public interface PharmaMlHttpClientService {

    /**
     * Envoie une commande au serveur PharmaML avec gestion des retries.
     *
     * @param payload     l'enveloppe CSRP à envoyer
     * @param commande    la commande associée (pour le traitement de la réponse)
     * @param fournisseur le fournisseur destinataire
     * @param envoi       l'entité d'historique d'envoi (pour mise à jour des tentatives)
     * @param fileName    le nom de fichier pour la sauvegarde des logs XML
     * @return la réponse PharmaML parsée
     */
    PharmamlCommandeResponse sendCommandeWithRetry(
        CsrpEnveloppe payload,
        Commande commande,
        Fournisseur fournisseur,
        PharmaMlEnvoi envoi,
        String fileName
    );

    /**
     * Envoie un message simple (ACK, annulation, retour) et vérifie le code de retour HTTP.
     *
     * @param payload     l'enveloppe CSRP à envoyer
     * @param fournisseur le fournisseur destinataire
     * @param fileName    le nom de fichier pour la sauvegarde (optionnel)
     * @param actionName  nom de l'action pour les logs (ex: "ACQ_RECEPTION", "REQ_ANNULATION")
     */
    void sendSimpleMessage(CsrpEnveloppe payload, Fournisseur fournisseur, String fileName, String actionName);

    /**
     * Envoie une demande d'information et parse la réponse.
     *
     * @param payload     l'enveloppe CSRP à envoyer
     * @param fournisseur le fournisseur destinataire
     * @return la liste des informations produits retournées
     */
    List<InfoProduitDTO> sendInfoRequest(CsrpEnveloppe payload, Fournisseur fournisseur);

    /**
     * Sérialise un payload en chaîne XML.
     *
     * @param payload l'enveloppe à sérialiser
     * @return la chaîne XML
     */
    String serializePayload(CsrpEnveloppe payload);

    /**
     * Sauvegarde un fichier XML dans le répertoire de stockage PharmaML.
     *
     * @param payload  l'objet à sérialiser et sauvegarder
     * @param prefix   le préfixe du fichier (C_, R_, ACK_, ANN_, RET_)
     * @param fileName le nom de base du fichier
     */
    void saveXmlFile(Object payload, String prefix, String fileName);

    /**
     * Génère le nom de fichier standard pour une commande.
     *
     * @param orderReference la référence de la commande
     * @param fournisseurLibelle le libellé du fournisseur
     * @return le nom de fichier formaté
     */
    String generateFileName(String orderReference, String fournisseurLibelle);
}

