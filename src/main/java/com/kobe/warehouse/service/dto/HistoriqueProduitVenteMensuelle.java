package com.kobe.warehouse.service.dto;

import java.time.Month;

public record HistoriqueProduitVenteMensuelle(Integer annee, Integer mois, Integer quantite,Integer montantTtc) {


    public Month getMonth() {
        if (mois == null) {
            return null;
        }
        return Month.of(mois);
    }
}
