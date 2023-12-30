package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.dto.*;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.stat.DashboardService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardResource {
  private final DashboardService dashboardService;

  public DashboardResource(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/dashboard/ca")
  public ResponseEntity<VenteRecordWrapper> getCa(@Valid VenteRecordParamDTO venteRecordParam) {
    return ResponseEntity.ok().body(dashboardService.getPeridiqueCa(venteRecordParam));
  }

  @GetMapping("/dashboard/ca-achats")
  public ResponseEntity<AchatRecord> getCaAchats(@Valid AchatRecordParamDTO achatRecordParam) {
    return ResponseEntity.ok().body(dashboardService.getAchatPeriode(achatRecordParam));
  }

  @GetMapping("/dashboard/ca-by-type-vente")
  public ResponseEntity<List<VenteByTypeRecord>> getCaByTypeVente(
      @Valid VenteRecordParamDTO venteRecordParam) {
    return ResponseEntity.ok().body(dashboardService.getCaGroupingByType(venteRecordParam));
  }

  @GetMapping("/dashboard/ca-by-mode-paiment")
  public ResponseEntity<List<VenteModePaimentRecord>> getCaByModePaiment(
      @Valid VenteRecordParamDTO venteRecordParam) {
    return ResponseEntity.ok().body(dashboardService.getCaGroupingByPaimentMode(venteRecordParam));
  }

  @GetMapping("/dashboard/ca-by-periode")
  public ResponseEntity<List<VentePeriodeRecord>> getCaGroupingByPeriode(
      @Valid VenteRecordParamDTO venteRecordParam) {
    return ResponseEntity.ok().body(dashboardService.getCaGroupingByPeriode(venteRecordParam));
  }
}
