package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import java.time.LocalDate;

public class BedLigneDTO {

    private Integer id;
    private LocalDate orderDate;
    private Integer produitId;
    private String produitLibelle;
    private String codeCip;
    private Integer fournisseurProduitId;
    private int quantite;
    private int prixAchat;
    private int prixVente;

    public BedLigneDTO() {}

    public BedLigneDTO(OrderLine line) {
        FournisseurProduit fp = line.getFournisseurProduit();
        this.id = line.getId().getId();
        this.orderDate = line.getOrderDate();
        this.fournisseurProduitId = fp.getId();
        this.codeCip = fp.getCodeCip();
        this.prixAchat = line.getOrderCostAmount();
        this.prixVente = line.getOrderUnitPrice();
        this.quantite = line.getQuantityRequested();
        if (fp.getProduit() != null) {
            this.produitId = fp.getProduit().getId();
            this.produitLibelle = fp.getProduit().getLibelle();
        }
    }

    public Integer getId() { return id; }
    public BedLigneDTO setId(Integer id) { this.id = id; return this; }

    public LocalDate getOrderDate() { return orderDate; }
    public BedLigneDTO setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; return this; }

    public Integer getProduitId() { return produitId; }
    public BedLigneDTO setProduitId(Integer produitId) { this.produitId = produitId; return this; }

    public String getProduitLibelle() { return produitLibelle; }
    public BedLigneDTO setProduitLibelle(String produitLibelle) { this.produitLibelle = produitLibelle; return this; }

    public String getCodeCip() { return codeCip; }
    public BedLigneDTO setCodeCip(String codeCip) { this.codeCip = codeCip; return this; }

    public Integer getFournisseurProduitId() { return fournisseurProduitId; }
    public BedLigneDTO setFournisseurProduitId(Integer fournisseurProduitId) { this.fournisseurProduitId = fournisseurProduitId; return this; }

    public int getQuantite() { return quantite; }
    public BedLigneDTO setQuantite(int quantite) { this.quantite = quantite; return this; }

    public int getPrixAchat() { return prixAchat; }
    public BedLigneDTO setPrixAchat(int prixAchat) { this.prixAchat = prixAchat; return this; }

    public int getPrixVente() { return prixVente; }
    public BedLigneDTO setPrixVente(int prixVente) { this.prixVente = prixVente; return this; }
}
