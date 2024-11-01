package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import java.time.LocalDate;
import java.util.Set;

public record EditionSearchParams(
    ModeEditionSort sort,
    ModeEditionEnum modeEdition,
    LocalDate startDate,
    LocalDate endDate,
    Set<Long> groupIds,
    Set<Long> tiersPayantIds,
    Set<Long> ids,
    boolean all,
    Set<TiersPayantCategorie> categorieTiersPayants,
    boolean factureProvisoire
) {}
