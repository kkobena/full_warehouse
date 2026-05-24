package com.kobe.warehouse.service.dto;

public class VenteProduitDTO {

    private final Long produitId;
    private final String produitLibelle;
    private final long quantiteTotale;

    public VenteProduitDTO(Long produitId, String produitLibelle, long quantiteTotale) {
        this.produitId = produitId;
        this.produitLibelle = produitLibelle;
        this.quantiteTotale = quantiteTotale;
    }

    public Long getProduitId() {
        return produitId;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public long getQuantiteTotale() {
        return quantiteTotale;
    }
}
