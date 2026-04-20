package com.kobe.warehouse.service.dto;

import java.util.List;

public class RetourBonGroupeDTO {

    private Integer fournisseurId;
    private String fournisseurLibelle;
    private int nbRetours;
    private long montantTotal;
    private List<RetourBonDTO> retourBons;

    public RetourBonGroupeDTO() {}

    public RetourBonGroupeDTO(Integer fournisseurId, String fournisseurLibelle, List<RetourBonDTO> retourBons) {
        this.fournisseurId = fournisseurId;
        this.fournisseurLibelle = fournisseurLibelle;
        this.retourBons = retourBons;
        this.nbRetours = retourBons.size();
        this.montantTotal = retourBons.stream().mapToLong(RetourBonDTO::getMontantTotal).sum();
    }

    public Integer getFournisseurId() { return fournisseurId; }
    public RetourBonGroupeDTO setFournisseurId(Integer fournisseurId) { this.fournisseurId = fournisseurId; return this; }

    public String getFournisseurLibelle() { return fournisseurLibelle; }
    public RetourBonGroupeDTO setFournisseurLibelle(String fournisseurLibelle) { this.fournisseurLibelle = fournisseurLibelle; return this; }

    public int getNbRetours() { return nbRetours; }
    public RetourBonGroupeDTO setNbRetours(int nbRetours) { this.nbRetours = nbRetours; return this; }

    public long getMontantTotal() { return montantTotal; }
    public RetourBonGroupeDTO setMontantTotal(long montantTotal) { this.montantTotal = montantTotal; return this; }

    public List<RetourBonDTO> getRetourBons() { return retourBons; }
    public RetourBonGroupeDTO setRetourBons(List<RetourBonDTO> retourBons) { this.retourBons = retourBons; return this; }
}
