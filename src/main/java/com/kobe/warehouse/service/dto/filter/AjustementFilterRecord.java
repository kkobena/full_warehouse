package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import java.time.LocalDate;

public record AjustementFilterRecord(LocalDate fromDate, LocalDate toDate, Long userId, String search, AjustementStatut statut) {}
