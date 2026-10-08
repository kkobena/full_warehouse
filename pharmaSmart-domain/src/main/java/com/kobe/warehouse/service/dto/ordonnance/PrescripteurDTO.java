package com.kobe.warehouse.service.dto.ordonnance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Fiche prescripteur : seul le nom est exigé, la fiche se constitue par l'usage. */
public record PrescripteurDTO(
    Integer id,
    @NotBlank @Size(max = 100) String nom,
    @Size(max = 100) String prenom,
    @Size(max = 100) String specialite,
    @Size(max = 30) String numeroOrdre,
    @Size(max = 150) String structure,
    @Size(max = 30) String telephone,
    boolean actif
) {}
