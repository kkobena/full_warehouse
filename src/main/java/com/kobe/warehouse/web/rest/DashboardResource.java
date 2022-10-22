package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.DashboardService;
import com.kobe.warehouse.service.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardResource {
    private final DashboardService dashboardService;

    public DashboardResource(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/daily")
    public ResponseEntity<DailyCa> getDailyCa() {
        DailyCa dailyCa = dashboardService.getDailyCa(LocalDate.now());
        return ResponseEntity.ok().body(dailyCa);
    }

    @GetMapping("/dashboard/weekly")
    public ResponseEntity<WeeklyCa> getWeeklyCa() {
        WeeklyCa weeklyCa = dashboardService.getWeeklyCa(LocalDate.now());
        return ResponseEntity.ok().body(weeklyCa);
    }

    @GetMapping("/dashboard/monthly")
    public ResponseEntity<MonthlyCa> getMonthlyCa() {
        MonthlyCa monthly = dashboardService.getMonthlyCa(LocalDate.now());
        return ResponseEntity.ok().body(monthly);
    }

    @GetMapping("/dashboard/yearly")
    public ResponseEntity<YearlyCa> getYearlyCa() {
        YearlyCa yearlyCa = dashboardService.getYearlyCa(LocalDate.now());
        return ResponseEntity.ok().body(yearlyCa);
    }

    @GetMapping("/dashboard/yearly-quantity")
    public ResponseEntity<List<StatistiqueProduit>> statistiqueProduitsQunatityYearly(@RequestParam(name = "maxResult", defaultValue = "5", required = false) int maxResul) {
        List<StatistiqueProduit> data = dashboardService.statistiqueProduitsQunatityYearly(LocalDate.now(), maxResul);
        return ResponseEntity.ok().body(data);
    }
    @GetMapping("/dashboard/yearly-amount")
    public ResponseEntity<List<StatistiqueProduit>> statistiqueProduitsAmountYearly(@RequestParam(name = "maxResult", defaultValue = "5", required = false) int maxResul) {
        List<StatistiqueProduit> data = dashboardService.statistiqueProduitsAmountYearly(LocalDate.now(), maxResul);
        return ResponseEntity.ok().body(data);
    }


    @GetMapping("/dashboard/monthly-quantity")
    public ResponseEntity<List<StatistiqueProduit>> statistiqueProduitsQunatityMonthly(@RequestParam(name = "maxResult", defaultValue = "5", required = false) int maxResul) {
        List<StatistiqueProduit> data = dashboardService.statistiqueProduitsQunatityMonthly(LocalDate.now(), maxResul);
        return ResponseEntity.ok().body(data);
    }
    @GetMapping("/dashboard/monthly-amount")
    public ResponseEntity<List<StatistiqueProduit>> statistiqueProduitsAmountMonthly(@RequestParam(name = "maxResult", defaultValue = "5", required = false) int maxResul) {
        List<StatistiqueProduit> data = dashboardService.statistiqueProduitsAmountMonthly(LocalDate.now(), maxResul);
        return ResponseEntity.ok().body(data);
    }
}
