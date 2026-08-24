package com.kobe.warehouse.service.declaration_ca.dto;

import java.time.LocalDate;

/**
 * Une vente touchée par la ponction, telle qu'affichée dans l'aperçu et l'historique.
 *
 * @param montantVente total de la vente, qui sert d'assiette au plafond
 * @param montantBase part à TVA 0, seule réellement ponctionnable
 * @param montantPonctionne ce qui est retiré, borné par les deux précédents
 */
public record PonctionLigneDTO(
    Long saleId,
    LocalDate saleDate,
    String numeroTransaction,
    long montantVente,
    long montantBase,
    long montantPonctionne,
    int rang
) {}
