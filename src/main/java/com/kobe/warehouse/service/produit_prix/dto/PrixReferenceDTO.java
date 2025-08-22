package com.kobe.warehouse.service.produit_prix.dto;

import com.kobe.warehouse.domain.OptionPrixProduit;
import com.kobe.warehouse.domain.enumeration.OptionPrixType;
import jakarta.validation.constraints.NotNull;

public class PrixReferenceDTO {

    private int price;
    private long id;
    private boolean enabled;
    private Long tiersPayantId;
    private Long produitId;

    @NotNull
    private OptionPrixType type;

    private float rate;
    private String tiersPayantName;
    private String produitName;
    private String produitCode;
    private String typeLibelle;

    public PrixReferenceDTO(OptionPrixProduit optionPrixProduit) {
        this.price = optionPrixProduit.getPrice();
        this.id = optionPrixProduit.getId();
        this.enabled = optionPrixProduit.isEnabled();
        this.type = optionPrixProduit.getType();
        this.rate = optionPrixProduit.getRate();
        this.typeLibelle = optionPrixProduit.getType().getLibelle();
    }

    public PrixReferenceDTO() {}

    public String getProduitCode() {
        return produitCode;
    }

    public void setProduitCode(String produitCode) {
        this.produitCode = produitCode;
    }

    public String getProduitName() {
        return produitName;
    }

    public void setProduitName(String produitName) {
        this.produitName = produitName;
    }

    public String getTiersPayantName() {
        return tiersPayantName;
    }

    public void setTiersPayantName(String tiersPayantName) {
        this.tiersPayantName = tiersPayantName;
    }

    public boolean enabled() {
        return enabled;
    }

    public String produitName() {
        return produitName;
    }

    public String produitCode() {
        return produitCode;
    }

    public String tiersPayantName() {
        return tiersPayantName;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getTiersPayantId() {
        return tiersPayantId;
    }

    public void setTiersPayantId(Long tiersPayantId) {
        this.tiersPayantId = tiersPayantId;
    }

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public OptionPrixType getType() {
        return type;
    }

    public void setType(OptionPrixType type) {
        this.type = type;
    }

    public float getRate() {
        return rate;
    }

    public void setRate(float rate) {
        this.rate = rate;
    }

    public String getTypeLibelle() {
        return typeLibelle;
    }

    public void setTypeLibelle(String typeLibelle) {
        this.typeLibelle = typeLibelle;
    }
}
