package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.EtatPalierRfa;

/**
 * Avancement vers le palier de remise de fin d'année d'un <b>groupe</b> de fournisseurs.
 *
 * <p>{@code fournisseurId} et {@code fournisseurName} désignent le grossiste principal, pas
 * l'agence qui a reçu les commandes : un palier se négocie avec le groupe, et c'est le cumul de
 * ses agences qui l'atteint.
 *
 * @param resteAvantPalier ce qu'il reste à facturer pour franchir le palier, 0 une fois franchi.
 *                         C'est le seul chiffre directement actionnable à la saisie d'une
 *                         commande : comparé au montant du bon en cours, il dit si ce bon fait
 *                         basculer.
 * @param etat             où l'on en est, nommé et non rédigé — la phrase se compose au front.
 */
public record RemiseRfaFournisseurDTO(
        Integer fournisseurId,
        String fournisseurName,
        Long palierRfa,
        long caCommandeN,
        double pourcentageAtteint,
        long rfaEstimee,
        long rfaRecue,
        long resteAvantPalier,
        EtatPalierRfa etat) {}
