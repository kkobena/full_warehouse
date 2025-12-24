package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO représentant le résultat d'une reclassification de produits.
 * Contient les statistiques de l'opération de reclassification.
 *
 * @param totalProduitsAnalyses Nombre total de produits analysés
 * @param totalChangements Nombre de produits ayant changé de classe
 * @param nbPromotions Nombre de promotions (montée en classe)
 * @param nbRetrogradations Nombre de rétrogradations (descente en classe)
 * @param nbProduitsNouveaux Nombre de produits nouveaux (non reclassifiés)
 * @param nbProduitsOverridden Nombre de produits avec override (non reclassifiés)
 * @param nbErreurs Nombre d'erreurs rencontrées
 * @param repartitionAvant Distribution des classes avant reclassification
 * @param repartitionApres Distribution des classes après reclassification
 * @param dureeExecutionMs Durée d'exécution en millisecondes
 * @param dateExecution Date et heure d'exécution
 * @param raison Raison de la reclassification (automatique, manuelle, initiale)
 */
public record ReclassificationResultDTO(
    int totalProduitsAnalyses,
    int totalChangements,
    int nbPromotions,
    int nbRetrogradations,
    int nbProduitsNouveaux,
    int nbProduitsOverridden,
    int nbErreurs,
    Map<String, Long> repartitionAvant,
    Map<String, Long> repartitionApres,
    long dureeExecutionMs,
    LocalDateTime dateExecution,
    String raison
) {

    /**
     * Calcule le pourcentage de changements
     *
     * @return Pourcentage de produits ayant changé de classe
     */
    public double getPourcentageChangements() {
        if (totalProduitsAnalyses == 0) {
            return 0.0;
        }
        return (double) totalChangements / totalProduitsAnalyses * 100.0;
    }

    /**
     * Vérifie si la reclassification a détecté une anomalie potentielle
     * (plus de 20% de changements)
     *
     * @return true si le pourcentage de changements dépasse 20%
     */
    public boolean hasAnomaliePotentielle() {
        return getPourcentageChangements() > 20.0;
    }

    /**
     * Vérifie si la reclassification s'est terminée avec succès
     *
     * @return true si aucune erreur n'a été rencontrée
     */
    public boolean isSuccess() {
        return nbErreurs == 0;
    }

    /**
     * Retourne un résumé textuel du résultat
     *
     * @return Description du résultat
     */
    public String getResume() {
        return String.format(
            "Reclassification terminée: %d produits analysés, %d changements (%.1f%%), %d promotions, %d rétrogradations en %d ms",
            totalProduitsAnalyses,
            totalChangements,
            getPourcentageChangements(),
            nbPromotions,
            nbRetrogradations,
            dureeExecutionMs
        );
    }

    /**
     * Builder pour faciliter la construction du DTO
     */
    public static class Builder {
        private int totalProduitsAnalyses = 0;
        private int totalChangements = 0;
        private int nbPromotions = 0;
        private int nbRetrogradations = 0;
        private int nbProduitsNouveaux = 0;
        private int nbProduitsOverridden = 0;
        private int nbErreurs = 0;
        private Map<String, Long> repartitionAvant = Map.of();
        private Map<String, Long> repartitionApres = Map.of();
        private long dureeExecutionMs = 0;
        private LocalDateTime dateExecution = LocalDateTime.now();
        private String raison = "Reclassification automatique";

        public Builder totalProduitsAnalyses(int val) {
            this.totalProduitsAnalyses = val;
            return this;
        }

        public Builder totalChangements(int val) {
            this.totalChangements = val;
            return this;
        }

        public Builder nbPromotions(int val) {
            this.nbPromotions = val;
            return this;
        }

        public Builder nbRetrogradations(int val) {
            this.nbRetrogradations = val;
            return this;
        }

        public Builder nbProduitsNouveaux(int val) {
            this.nbProduitsNouveaux = val;
            return this;
        }

        public Builder nbProduitsOverridden(int val) {
            this.nbProduitsOverridden = val;
            return this;
        }

        public Builder nbErreurs(int val) {
            this.nbErreurs = val;
            return this;
        }

        public Builder repartitionAvant(Map<String, Long> val) {
            this.repartitionAvant = val;
            return this;
        }

        public Builder repartitionApres(Map<String, Long> val) {
            this.repartitionApres = val;
            return this;
        }

        public Builder dureeExecutionMs(long val) {
            this.dureeExecutionMs = val;
            return this;
        }

        public Builder dateExecution(LocalDateTime val) {
            this.dateExecution = val;
            return this;
        }

        public Builder raison(String val) {
            this.raison = val;
            return this;
        }

        public ReclassificationResultDTO build() {
            return new ReclassificationResultDTO(
                totalProduitsAnalyses,
                totalChangements,
                nbPromotions,
                nbRetrogradations,
                nbProduitsNouveaux,
                nbProduitsOverridden,
                nbErreurs,
                repartitionAvant,
                repartitionApres,
                dureeExecutionMs,
                dateExecution,
                raison
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
