package com.kobe.warehouse.service.pharmaml.dto;

import com.kobe.warehouse.domain.enumeration.SubstitutionStatut;
import java.time.LocalDateTime;

public record SubstitutionProposeeDTO(
    Integer id,
    String cipPropose,
    String designation,
    String typeCodification,
    int quantite,
    SubstitutionStatut statut,
    String cipOriginal,
    String designationOriginale,
    LocalDateTime createdAt,
    String codeReponse,
    String additif,
    String typeRemplacement,
    boolean substitutConnu
) {}
