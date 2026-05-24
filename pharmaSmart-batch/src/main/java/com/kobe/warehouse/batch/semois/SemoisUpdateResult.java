package com.kobe.warehouse.batch.semois;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SemoisConfiguration;
import java.util.List;

/**
 * Résultat du calcul SEMOIS pour un produit : entités prêtes à être persistées.
 */
public record SemoisUpdateResult(Produit produit, List<SemoisConfiguration> configurations) {}
