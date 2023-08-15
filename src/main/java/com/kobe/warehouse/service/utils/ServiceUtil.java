package com.kobe.warehouse.service.utils;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Produit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ServiceUtil {



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

  public static Integer dateDimensionKey(LocalDate date) {
    return Integer.valueOf(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
  }

  public static long computeHtaxe(long ttc, int taxe) {
    return ttc / (1 + (taxe / 100));
  }
  public static int computeHtaxe(int ttc, int taxe) {
    return ttc / (1 + (taxe / 100));
  }

  public static long computeHtaxe(long ttc, double taxe) {
    return (long) (ttc / (1 + (taxe / 100)));
  }
}
