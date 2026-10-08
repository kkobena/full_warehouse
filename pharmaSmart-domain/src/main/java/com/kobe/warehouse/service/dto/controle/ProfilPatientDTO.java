package com.kobe.warehouse.service.dto.controle;

/** Ce que le moteur sait du patient ; une valeur inconnue ne déclenche aucune règle qui la suppose. */
public record ProfilPatientDTO(Integer age, String sexe, boolean grossesse, boolean allaitement) {}
