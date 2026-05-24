package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import com.kobe.warehouse.domain.enumeration.Periodicite;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record PlanificationDto(
    Integer id,
    String libelle,
    Periodicite periodicite,
    LocalTime heureDeclenchement,
    boolean factureProvisoire,
    boolean actif,
    LocalDate dernierePeriodeFin,
    LocalDateTime prochaineExecution,
    LocalDateTime derniereExecution,
    ExecutionStatut dernierStatut,
    String dernierMessage,
    long nombreOrganismes
) {}
