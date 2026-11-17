package com.kobe.warehouse.service.dto;

import java.util.List;

/**
 * Données agrégées pour le tableau de bord des commandes.
 *
 * <p>{@code totalRequested} comptait indistinctement les bons en cours de saisie et ceux déjà
 * partis chez le grossiste. Les deux appellent pourtant des gestes opposés : un brouillon attend
 * qu'on le complète et qu'on l'envoie, une commande envoyée attend une livraison — et se relance
 * si elle tarde. D'où trois compteurs là où il n'y en avait qu'un.
 */
public record CommandeDashboardDTO(
    long totalRequested,
    long totalReceived,
    long totalPharmamlPending,
    /** Bons en cours de saisie, jamais transmis. */
    long totalBrouillons,
    /** Commandes transmises, en attente de livraison. */
    long totalEnvoyees,
    /** Parmi les envoyées, celles dont le délai annoncé est dépassé. */
    long totalEnRetard,
    List<CommandeResumeeDTO> commandesRequested,
    List<CommandeResumeeDTO> commandesReceived,
    List<PharmaMlEnvoiResumeeDTO> envoisPending,
    /** Les plus anciennes d'abord : c'est par celles-là qu'il faut relancer. */
    List<CommandeResumeeDTO> commandesEnRetard
) {}
