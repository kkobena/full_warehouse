package com.kobe.warehouse.service.reassort.dto;

import com.kobe.warehouse.domain.enumeration.TypeRepartition;

import java.time.LocalDate;

public record RepartionSearchQueryDto(Integer storageId, Integer userId, String searchTerm, LocalDate dateDebut,
                                      LocalDate dateFin,
                                      TypeRepartition typeRepartition, Integer stockProduitId) {

}
