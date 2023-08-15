package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.dto.*;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.stat.DashboardService;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping("/dashboard/yearly-quantity")
  public ResponseEntity<List<StatistiqueProduit>> statistiqueProduitsQunatityYearly(
      @RequestParam(name = "maxResult", defaultValue = "5", required = false) int maxResul) {
    List<StatistiqueProduit> data =
        dashboardService.statistiqueProduitsQunatityYearly(LocalDate.now(), maxResul);
    return ResponseEntity.ok().body(data);
  }

  @GetMapping("/dashboard/yearly-amount")
  public ResponseEntity<List<StatistiqueProduit>> statistiqueProduitsAmountYearly(
      @RequestParam(name = "maxResult", defaultValue = "5", required = false) int maxResul) {
    List<StatistiqueProduit> data =
        dashboardService.statistiqueProduitsAmountYearly(LocalDate.now(), maxResul);
    return ResponseEntity.ok().body(data);
  }

  @GetMapping("/dashboard/monthly-quantity")
  public ResponseEntity<List<StatistiqueProduit>> statistiqueProduitsQunatityMonthly(
      @RequestParam(name = "maxResult", defaultValue = "5", required = false) int maxResul) {
    List<StatistiqueProduit> data =
        dashboardService.statistiqueProduitsQunatityMonthly(LocalDate.now(), maxResul);
    return ResponseEntity.ok().body(data);
  }

  @GetMapping("/dashboard/monthly-amount")
  public ResponseEntity<List<StatistiqueProduit>> statistiqueProduitsAmountMonthly(
      @RequestParam(name = "maxResult", defaultValue = "5", required = false) int maxResul) {
    List<StatistiqueProduit> data =
        dashboardService.statistiqueProduitsAmountMonthly(LocalDate.now(), maxResul);
    return ResponseEntity.ok().body(data);
  }
}
