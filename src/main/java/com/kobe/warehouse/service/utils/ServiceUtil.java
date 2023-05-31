package com.kobe.warehouse.service.utils;

import com.kobe.warehouse.domain.DateDimension;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Produit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ServiceUtil {

  public static DateDimension DateDimension(LocalDate date) {
    int dateKey = Integer.parseInt(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    DateDimension dateDimension = new DateDimension();
    dateDimension.setDateKey(dateKey);
    return dateDimension;
  }

  public static Produit produitFromId(Long id) {
    Produit produit = new Produit();
    produit.setId(id);
    return produit;
  }

  public static PaymentMode getPaymentMode(String code) {
    PaymentMode paymentMode = new PaymentMode();
    paymentMode.setCode(code);
    return paymentMode;
  }

  public static Integer DateDimensionKey(LocalDate date) {
    return Integer.valueOf(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
  }
}
