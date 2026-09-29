package com.kobe.warehouse.service.customer;

import java.time.LocalDateTime;

/**
 * En-tête de la fiche client : sa situation d'un coup d'œil.
 *
 * @param encours          reste dû sur les ventes différées
 * @param derniereVisite   dernier achat clôturé, toutes périodes confondues
 * @param nombreAchats     achats clôturés sur les douze derniers mois
 * @param montantAchats    leur montant total
 */
public record CustomerSyntheseDTO(
    Integer customerId,
    long encours,
    LocalDateTime derniereVisite,
    long nombreAchats,
    long montantAchats
) {}
