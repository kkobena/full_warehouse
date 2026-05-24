package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import java.util.List;

public record RecapitulatifMensuelParams(
    int annee,
    int mois,
    List<Integer> tiersPayantIds,
    List<Integer> groupIds,
    TypeFacture typeFacture
) {}
