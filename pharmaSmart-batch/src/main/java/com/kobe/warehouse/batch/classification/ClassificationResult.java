package com.kobe.warehouse.batch.classification;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.service.classification.ClassificationBatchProcessor.ParetoScore;

/**
 * Résultat de la classification pour un produit (Phase 2).
 * Uniquement produit reclassifié avec changement effectif (le processor filtre les non-changements).
 */
public record ClassificationResult(
    Produit produit,
    ClasseCriticite ancienneClasse,
    ClasseCriticite nouvelleClasse,
    boolean changed,
    ParetoScore paretoScore
) {}
