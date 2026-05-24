package com.kobe.warehouse.service.dto;

import java.time.Month;

public record HistoriqueProduitAchatMensuelle(Integer annee, Integer mois, int quantite, Integer montantAchat) {
    public Month getMonth() {
        if (mois == null) {
            return null;
        }
        return Month.of(mois);
    }
}
