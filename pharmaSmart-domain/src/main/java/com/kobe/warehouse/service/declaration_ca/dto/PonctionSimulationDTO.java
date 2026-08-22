package com.kobe.warehouse.service.declaration_ca.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Résultat d'une simulation, avant toute écriture.
 *
 * <p>Les trois montants d'assiette sont donnés du plus large au plus étroit : sans cette cascade, un
 * objectif refusé serait incompréhensible — le pharmacien verrait un CA de 12 millions et un refus
 * sur 4 millions, sans voir que l'assiette exonérée n'en autorisait que 3,6.
 *
 * @param caReel chiffre d'affaires encaissé sur la période
 * @param caApresExclusions après rayon, tiers-payant et unités gratuites
 * @param caAssietteTva0 part exonérée : seule ponctionnable
 * @param montantPonctionnable Σ min(plafond × vente, assiette TVA 0) — le maximum atteignable
 * @param objectifAtteignable faux si l'objectif dépasse ce maximum ; la validation est alors refusée
 * @param plafondApplique plafond par vente réellement retenu — celui saisi, ou celui de l'officine.
 *     Renvoyé pour que l'écran montre ce qui a servi au calcul, et non ce qu'il croit avoir envoyé
 * @param delaiAnnulationJours jours pendant lesquels la ponction restera annulable — l'écran doit
 *     l'annoncer avant validation, sans quoi le pharmacien découvrirait l'irréversibilité trop tard
 */
public record PonctionSimulationDTO(
    LocalDate dateDebut,
    LocalDate dateFin,
    long caReel,
    long caApresExclusions,
    long caAssietteTva0,
    long montantObjectif,
    long montantPonctionnable,
    long montantPonctionne,
    long caDeclare,
    int nombreVentesEligibles,
    int nombreVentesImpactees,
    BigDecimal tauxMoyenApplique,
    BigDecimal tauxMaxApplique,
    boolean objectifAtteignable,
    BigDecimal plafondApplique,
    int delaiAnnulationJours,
    List<PonctionLigneDTO> apercu,
    List<String> avertissements
) {}
