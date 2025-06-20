package com.kobe.warehouse.service.mobile.dto;

public record TvaItem(int code,
                      String ttc,
                      String tva,
                      String ht,
                      double pourcentage,String date) {
}
