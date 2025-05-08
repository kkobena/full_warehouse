package com.kobe.warehouse.service.produit_prix.dto;

import com.kobe.warehouse.domain.PrixReference;
import com.kobe.warehouse.domain.PrixReferenceType;

public class PrixReferenceDTO {
    private int valeur;
    private long id;
    private boolean enabled;
    private Long tiersPayantId;
    private Long produitId;
    private PrixReferenceType type;
    private float taux;

    public PrixReferenceDTO(PrixReference prixReference) {
        this.valeur = prixReference.getValeur();
        this.id = prixReference.getId();
        this.enabled = prixReference.isEnabled();
        this.type = prixReference.getType();
        this.taux = prixReference.getTaux();

    }

    public PrixReferenceDTO() {
    }

    public int getValeur() {
        return valeur;
    }

    public void setValeur(int valeur) {
        this.valeur = valeur;
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

    public PrixReferenceType getType() {
        return type;
    }

    public void setType(PrixReferenceType type) {
        this.type = type;
    }

    public float getTaux() {
        return taux;
    }
    public void setTaux(float taux) {
        this.taux = taux;
    }
}
