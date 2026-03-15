package com.kobe.warehouse.service.dto.records;

import java.time.LocalDate;

/**
 * DTO record pour {@link com.kobe.warehouse.domain.PlanningInventaireTournant}.
 */
public record PlanningInventaireTournantRecord(
    Integer id,
    String libelle,
    /** FrequenceTournant : QUOTIDIEN, HEBDO, MENSUEL, TRIMESTRIEL */
    String frequence,
    /** CritereTournant : RAYON, FAMILLE, CLASSIFICATION_ABC */
    String critere,
    Integer storageId,
    String storageLibelle,
    /**
     * Employé affecté — l'inventaire créé automatiquement lui sera assigné.
     * Null = premier utilisateur actif du système (fallback).
     */
    Integer userId,
    String userFullName,
    LocalDate prochaineExecution,
    boolean actif,
    Integer critereIndexCourant,
    /** Pour critère CLASSIFICATION_ABC : la classe Pareto courante ('A', 'B' ou 'C') */
    String classeParetoCourante,
    Integer nbExecutions,
    LocalDate derniereExecution
) {

}
