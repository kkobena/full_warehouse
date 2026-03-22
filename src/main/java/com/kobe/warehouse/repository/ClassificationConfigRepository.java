package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ClassificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour ClassificationConfig.
 * Gère la configuration singleton de la classification dynamique.
 */
@Repository
public interface ClassificationConfigRepository extends JpaRepository<ClassificationConfig, Integer> {

    /**
     * Récupère la configuration unique (singleton).
     * Il ne devrait y avoir qu'une seule ligne dans cette table.
     *
     * @return La configuration si elle existe
     */
    @Query("SELECT c FROM ClassificationConfig c ORDER BY c.id ASC LIMIT 1")
    Optional<ClassificationConfig> findConfiguration();

    /**
     * Vérifie si la classification automatique est activée
     *
     * @return true si la classification auto est activée
     */
    @Query("SELECT COALESCE(c.autoClassificationEnabled, true) FROM ClassificationConfig c ORDER BY c.id ASC LIMIT 1")
    boolean isAutoClassificationEnabled();

    /**
     * Récupère le nombre minimum de mois pour considérer un produit comme nouveau
     *
     * @return Nombre de mois (défaut 6)
     */
    @Query("SELECT COALESCE(c.nbMoisMinNouveauProduit, 6) FROM ClassificationConfig c ORDER BY c.id ASC LIMIT 1")
    int getNbMoisMinNouveauProduit();

    /**
     * Récupère l'écart minimum en points Pareto pour déclencher un reclassement (hysteresis).
     *
     * @return Pourcentage minimum (défaut 3)
     */
    @Query("SELECT COALESCE(c.changementMinPourcentage, 3) FROM ClassificationConfig c ORDER BY c.id ASC LIMIT 1")
    int getChangementMinPourcentage();
}
