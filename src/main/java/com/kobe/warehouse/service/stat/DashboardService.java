package com.kobe.warehouse.service.stat;

import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.DailyCa;
import com.kobe.warehouse.service.dto.MonthlyCa;
import com.kobe.warehouse.service.dto.StatistiqueProduit;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.WeeklyCa;
import com.kobe.warehouse.service.dto.YearlyCa;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import java.time.LocalDate;
import java.util.List;

public interface DashboardService {

  DailyCa getDailyCa(LocalDate localDate);

  WeeklyCa getWeeklyCa(LocalDate localDate);

  MonthlyCa getMonthlyCa(LocalDate localDate);

  YearlyCa getYearlyCa(LocalDate localDate);

  List<StatistiqueProduit> statistiqueProduitsQunatityMonthly(LocalDate localDate, int maxResult);

  List<StatistiqueProduit> statistiqueProduitsQunatityYearly(LocalDate localDate, int maxResult);

  List<StatistiqueProduit> statistiqueProduitsAmountYearly(LocalDate localDate, int maxResult);

  List<StatistiqueProduit> statistiqueProduitsAmountMonthly(LocalDate localDate, int maxResult);


    VenteRecordWrapper getPeridiqueCa(
        VenteRecordParamDTO venteRecordParamDTO);

    AchatRecord getAchatPeriode(AchatRecordParamDTO achatRecordParam);
}
