package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.WarehouseCalendar;
import com.kobe.warehouse.repository.WarehouseCalendarRepository;
import com.kobe.warehouse.service.ReferenceService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SaleCommonService {
  private final ReferenceService referenceService;
  private final WarehouseCalendarRepository warehouseCalendarRepository;

  public SaleCommonService(
      ReferenceService referenceService, WarehouseCalendarRepository warehouseCalendarRepository) {
    this.referenceService = referenceService;
    this.warehouseCalendarRepository = warehouseCalendarRepository;
  }

  public void computeSaleEagerAmount(Sales c, int amount, int oldSalesAmount) {
    c.setSalesAmount((c.getSalesAmount() - oldSalesAmount) + amount);
  }

  public void computeSaleLazyAmount(Sales c, SalesLine saleLine, SalesLine oldSaleLine) {
    if (oldSaleLine != null) {
      c.setCostAmount(
          (c.getCostAmount() - (oldSaleLine.getQuantityRequested() * oldSaleLine.getCostAmount()))
              + (saleLine.getQuantityRequested() * saleLine.getCostAmount()));
    } else {
      c.setCostAmount(
          c.getCostAmount() + (saleLine.getQuantityRequested() * saleLine.getCostAmount()));
    }
  }

  public void processDiscountCashSale(CashSale c, SalesLine saleLine, SalesLine oldSaleLine) {
    if (oldSaleLine != null) {
      c.setDiscountAmount(
          (c.getDiscountAmount() - oldSaleLine.getDiscountAmount()) + saleLine.getDiscountAmount());
      c.setDiscountAmountUg(
          (c.getDiscountAmountUg() - oldSaleLine.getDiscountAmountUg())
              + saleLine.getDiscountAmountUg());
      c.setDiscountAmountHorsUg(
          (c.getDiscountAmountHorsUg() - saleLine.getDiscountAmountHorsUg())
              + saleLine.getDiscountAmountHorsUg());
      c.setNetAmount(c.getSalesAmount() - c.getDiscountAmount());
    } else {
      c.setDiscountAmount(c.getDiscountAmount() + saleLine.getDiscountAmount());
      c.setDiscountAmountUg(c.getDiscountAmountUg() + saleLine.getDiscountAmountUg());
      c.setDiscountAmountHorsUg(c.getDiscountAmountHorsUg() + saleLine.getDiscountAmountHorsUg());
      c.setNetAmount(c.getSalesAmount() - c.getDiscountAmount());
    }
  }

  public void computeUgTvaAmount(Sales c, SalesLine saleLine, SalesLine oldSaleLine) {
    if (saleLine.getQuantityUg().compareTo(0) == 0) return;
    if (oldSaleLine == null) {
      int htc = saleLine.getQuantityUg() * saleLine.getRegularUnitPrice();
      int costAmount = saleLine.getQuantityUg() * saleLine.getCostAmount();
      if (saleLine.getTaxValue().compareTo(0) == 0) {
        c.setHtAmountUg(c.getHtAmountUg() + htc);
        c.setMontantttcUg(c.getMontantttcUg() + htc);
      } else {
        Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
        int htAmont = (int) Math.ceil(htc / valeurTva);
        int montantTva = htc - htAmont;
        c.setMontantTvaUg(c.getMontantTvaUg() + montantTva);
        c.setHtAmountUg(c.getHtAmountUg() + htAmont);
      }
      c.setMargeUg(c.getMargeUg() + (htc - costAmount));
      c.setNetUgAmount(
          c.getMontantttcUg()
              + ((saleLine.getQuantityUg() * saleLine.getRegularUnitPrice())
                  - saleLine.getDiscountAmountUg()));

    } else {
      int htcOld = oldSaleLine.getQuantityUg() * oldSaleLine.getRegularUnitPrice();
      int htc = saleLine.getQuantityUg() * saleLine.getRegularUnitPrice();
      int costAmountOld = oldSaleLine.getQuantityUg() * oldSaleLine.getCostAmount();
      int costAmount = saleLine.getQuantityUg() * saleLine.getCostAmount();
      if (saleLine.getTaxValue().compareTo(0) == 0) {
        c.setHtAmountUg((c.getHtAmountUg() - htcOld) + htc);
        c.setMontantttcUg((c.getMontantttcUg() - htcOld) + htc);
      } else {
        Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
        int htAmont = (int) Math.ceil(htc / valeurTva);
        int montantTva = htc - htAmont;
        int htAmontOld = (int) Math.ceil(htcOld / valeurTva);
        int montantTvaOld = htcOld - htAmontOld;

        c.setMontantTvaUg((c.getMontantTvaUg() - montantTvaOld) + montantTva);
        c.setHtAmountUg((c.getHtAmountUg() - htAmontOld) + htAmont);
      }
      c.setMargeUg((c.getMargeUg() - (htcOld - costAmountOld)) + (htc - costAmount));
      c.setNetUgAmount(
          (c.getMontantttcUg()
                  - ((oldSaleLine.getQuantityUg() * oldSaleLine.getRegularUnitPrice())
                      - oldSaleLine.getDiscountAmountUg()))
              + ((saleLine.getQuantityUg() * saleLine.getRegularUnitPrice())
                  - saleLine.getDiscountAmountUg()));
    }
  }

  public void computeTvaAmount(Sales c, SalesLine saleLine, SalesLine oldSaleLine) {
    if (oldSaleLine == null) {
      if (saleLine.getTaxValue().compareTo(0) == 0) {
        c.setHtAmount(c.getHtAmount() + saleLine.getSalesAmount());
      } else {
        Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
        int htAmont = (int) Math.ceil(saleLine.getSalesAmount() / valeurTva);
        int montantTva = saleLine.getSalesAmount() - htAmont;
        c.setTaxAmount(c.getTaxAmount() + montantTva);
        c.setHtAmount(c.getHtAmount() + htAmont);
      }
    } else {
      if (saleLine.getTaxValue().compareTo(0) == 0) {
        c.setHtAmount((c.getHtAmount() - oldSaleLine.getSalesAmount()) + saleLine.getSalesAmount());
      } else {
        Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
        int htAmont = (int) Math.ceil(saleLine.getSalesAmount() / valeurTva);
        int montantTva = saleLine.getSalesAmount() - htAmont;
        int htAmontOld = (int) Math.ceil(oldSaleLine.getSalesAmount() / valeurTva);
        int montantTvaOld = oldSaleLine.getSalesAmount() - htAmontOld;
        c.setTaxAmount((c.getTaxAmount() - montantTvaOld) + montantTva);
        c.setHtAmount((c.getHtAmount() - htAmontOld) + htAmont);
      }
    }
  }

  public void processDiscountSaleOnRemovingItem(Sales c, SalesLine saleLine) {
    c.setDiscountAmount(c.getDiscountAmount() - saleLine.getDiscountAmount());
    c.setDiscountAmountUg(c.getDiscountAmountUg() - saleLine.getDiscountAmountUg());
    c.setDiscountAmountHorsUg(c.getDiscountAmountHorsUg() - saleLine.getDiscountAmountHorsUg());
    c.setNetAmount(c.getSalesAmount() - c.getDiscountAmount());
  }

  public void computeSaleEagerAmountOnRemovingItem(Sales c, SalesLine saleLine) {
    c.setSalesAmount(c.getSalesAmount() - saleLine.getSalesAmount());
  }

  public void computeSaleLazyAmountOnRemovingItem(Sales c, SalesLine saleLine) {
    c.setCostAmount(
        c.getCostAmount() - (saleLine.getQuantityRequested() * saleLine.getCostAmount()));
  }

  public void computeUgTvaAmountOnRemovingItem(Sales c, SalesLine saleLine) {
    if (saleLine.getQuantityUg().compareTo(0) == 0) return;
    int htc = saleLine.getQuantityUg() * saleLine.getRegularUnitPrice();
    int costAmount = saleLine.getQuantityUg() * saleLine.getCostAmount();
    if (saleLine.getTaxValue().compareTo(0) == 0) {
      c.setHtAmountUg(c.getHtAmountUg() - htc);
      c.setMontantttcUg(c.getMontantttcUg() - htc);
    } else {
      Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
      int htAmont = (int) Math.ceil(htc / valeurTva);
      int montantTva = htc - htAmont;
      c.setMontantTvaUg(c.getMontantTvaUg() - montantTva);
      c.setHtAmountUg(c.getHtAmountUg() - htAmont);
    }
    c.setMargeUg(c.getMargeUg() - (htc - costAmount));
    c.setNetUgAmount(
        c.getMontantttcUg()
            - ((saleLine.getQuantityUg() * saleLine.getRegularUnitPrice())
                - saleLine.getDiscountAmountUg()));
  }

  public void computeTvaAmountOnRemovingItem(Sales c, SalesLine saleLine) {
    if (saleLine.getTaxValue().compareTo(0) == 0) {
      c.setHtAmount(c.getHtAmount() - saleLine.getSalesAmount());
    } else {
      Double valeurTva = 1 + (Double.valueOf(saleLine.getTaxValue()) / 100);
      int htAmont = (int) Math.ceil(saleLine.getSalesAmount() / valeurTva);
      int montantTva = saleLine.getSalesAmount() - htAmont;
      c.setTaxAmount(c.getTaxAmount() - montantTva);
      c.setHtAmount(c.getHtAmount() - htAmont);
    }
  }

  public void buildReference(Sales sales) {
    String ref =
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            .concat(referenceService.buildNumSale());
    sales.setNumberTransaction(ref);
  }

  public void buildPreventeReference(Sales sales) {
    String ref =
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            .concat(referenceService.buildNumPreventeSale());
    sales.setNumberTransaction(ref);
  }

  public String buildTvaData(Set<SalesLine> salesLines) {
    if (salesLines != null && salesLines.size() > 0) {
      JSONArray array = new JSONArray();
      salesLines.stream()
          .filter(saleLine -> saleLine.getTaxValue() > 0)
          .collect(Collectors.groupingBy(SalesLine::getTaxValue))
          .forEach(
              (k, v) -> {
                JSONObject json = new JSONObject();

                int totalTva = 0;
                for (SalesLine item : v) {
                  Double valeurTva = 1 + (Double.valueOf(k) / 100);
                  int htAmont = (int) Math.ceil(item.getSalesAmount() / valeurTva);
                  totalTva += (item.getSalesAmount() - htAmont);
                }
                try {
                  json.put("tva", k);
                  json.put("amount", totalTva);
                  array.put(json);
                } catch (JSONException e) {

                }
              });
      if (array.length() > 0) return array.toString();
    }
    return null;
  }

  public int roundedAmount(int payrollAmount) {
    int rest = payrollAmount % 5;
    if (rest == 0) {
      return payrollAmount;
    } else {
      if (rest >= 3) {
        return payrollAmount + (5 - rest);
      } else {
        return payrollAmount - rest;
      }
    }
  }

  @Async
  public void initCalendar() {
    LocalDate now = LocalDate.now();
    Optional<WarehouseCalendar> optionalWarehouseCalendar =
        warehouseCalendarRepository.findById(now);
    if (optionalWarehouseCalendar.isEmpty()) {

      warehouseCalendarRepository.save(
          new WarehouseCalendar()
              .setWorkDay(now)
              .setWorkMonth(now.getMonthValue())
              .setWorkYear(now.getYear()));
    }
  }
}
