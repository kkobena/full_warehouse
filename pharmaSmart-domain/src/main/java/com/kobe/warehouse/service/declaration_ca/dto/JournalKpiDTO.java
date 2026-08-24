package com.kobe.warehouse.service.declaration_ca.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Les indicateurs d'un journal d'exclusion, sur la période et les filtres en vigueur.
 *
 * <p>Calculés par une agrégation propre sur l'ensemble filtré, jamais à partir des lignes renvoyées :
 * celles-ci sont plafonnées, et sommer un tableau tronqué afficherait des totaux inférieurs à la
 * réalité sans que rien ne le signale.
 *
 * @param tauxMarge marge rapportée à la valeur TTC, en pourcentage ; {@code 0} si rien n'est vendu
 */
public record JournalKpiDTO(
    long nombreVentes,
    long nombreLignes,
    long quantite,
    long quantiteUg,
    long valeurTtc,
    long montantExclu,
    long marge,
    BigDecimal tauxMarge
) {
    /**
     * Constructeur des requêtes, qui ne fournissent que les agrégats.
     *
     * <p>Le taux se déduit de deux d'entre eux : le calculer ici plutôt que dans un service évite
     * qu'un second appelant l'obtienne un jour par une division légèrement différente.
     */
    public JournalKpiDTO(
        Long nombreVentes,
        Long nombreLignes,
        Long quantite,
        Long quantiteUg,
        Long valeurTtc,
        Long montantExclu,
        Long marge
    ) {
        this(
            zeroSiNul(nombreVentes),
            zeroSiNul(nombreLignes),
            zeroSiNul(quantite),
            zeroSiNul(quantiteUg),
            zeroSiNul(valeurTtc),
            zeroSiNul(montantExclu),
            zeroSiNul(marge),
            tauxMarge(zeroSiNul(marge), zeroSiNul(valeurTtc))
        );
    }

    /** Les indicateurs d'un ensemble vide : des zéros, jamais des trous à l'écran. */
    public static JournalKpiDTO vide() {
        return new JournalKpiDTO(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    private static long zeroSiNul(Long valeur) {
        return Objects.requireNonNullElse(valeur, 0L);
    }

    private static BigDecimal tauxMarge(long marge, long valeurTtc) {
        if (valeurTtc == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(marge).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(valeurTtc), 2, RoundingMode.HALF_UP);
    }
}
