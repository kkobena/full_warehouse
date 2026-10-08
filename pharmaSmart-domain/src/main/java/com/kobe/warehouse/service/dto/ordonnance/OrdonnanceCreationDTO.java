package com.kobe.warehouse.service.dto.ordonnance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Saisie manuelle d'une ordonnance. Le client est obligatoire ; le prescripteur facultatif. */
public record OrdonnanceCreationDTO(
    @NotNull Integer customerId,
    Integer prescripteurId,
    @NotNull LocalDate datePrescription,
    @Min(0) int renouvellements,
    LocalDate dateFinValidite,
    @Size(max = 500) String note,
    @NotEmpty @Valid List<OrdonnanceLigneCreationDTO> lignes
) {}
