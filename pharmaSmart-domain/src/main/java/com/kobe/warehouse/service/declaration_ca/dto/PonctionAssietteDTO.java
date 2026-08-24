package com.kobe.warehouse.service.declaration_ca.dto;

import java.time.LocalDate;

/**
 * L'assiette d'une période, sans objectif de ponction.
 *
 * <p>Répond à la question posée avant toute saisie : « combien puis-je prélever sur ces dates ? ».
 * Une simulation y répondait déjà, mais seulement au prix d'un objectif inventé — et d'un refus
 * incompréhensible quand cet objectif dépassait le maximum. Ici rien n'est demandé, donc rien ne
 * peut être refusé.
 *
 * @param caReel chiffre d'affaires encaissé sur la période
 * @param caEspece encaissé en espèces sur les ventes éligibles — ce que la ponction peut mordre
 * @param caAssietteTva0 part exonérée : seule ponctionnable
 * @param montantPonctionnable Σ min(plafond × vente, assiette TVA 0) — le maximum atteignable
 * @param nombreVentesEligibles ventes retenues par les règles d'éligibilité
 * @param plafondApplique plafond par vente utilisé pour ce calcul, en pourcentage
 */
public record PonctionAssietteDTO(
    LocalDate dateDebut,
    LocalDate dateFin,
    long caReel,
    long caEspece,
    long caAssietteTva0,
    long montantPonctionnable,
    int nombreVentesEligibles,
    java.math.BigDecimal plafondApplique
) {}
