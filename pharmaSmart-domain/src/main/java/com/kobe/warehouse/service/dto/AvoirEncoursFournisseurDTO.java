package com.kobe.warehouse.service.dto;

public class AvoirEncoursFournisseurDTO {

    private Integer fournisseurId;
    private String fournisseurLibelle;
    private long montantEncours;

    public AvoirEncoursFournisseurDTO() {}

    public AvoirEncoursFournisseurDTO(Integer fournisseurId, String fournisseurLibelle, long montantEncours) {
        this.fournisseurId = fournisseurId;
        this.fournisseurLibelle = fournisseurLibelle;
        this.montantEncours = montantEncours;
    }

    public Integer getFournisseurId() { return fournisseurId; }
    public AvoirEncoursFournisseurDTO setFournisseurId(Integer fournisseurId) { this.fournisseurId = fournisseurId; return this; }

    public String getFournisseurLibelle() { return fournisseurLibelle; }
    public AvoirEncoursFournisseurDTO setFournisseurLibelle(String fournisseurLibelle) { this.fournisseurLibelle = fournisseurLibelle; return this; }

    public long getMontantEncours() { return montantEncours; }
    public AvoirEncoursFournisseurDTO setMontantEncours(long montantEncours) { this.montantEncours = montantEncours; return this; }
}
