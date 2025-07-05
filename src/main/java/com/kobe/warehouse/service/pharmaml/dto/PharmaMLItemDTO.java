package com.kobe.warehouse.service.pharmaml.dto;

import java.io.Serializable;

public class PharmaMLItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    boolean livraisonPartielle, reliquats, livraisonEquivalente, livre = true;
    int quantite, prixUn, prixAchat, amount;
    private String cip, ean, libelle;
    private String typeCodification;
    private int codeRetour;

    public PharmaMLItemDTO() {}

    public String getCip() {
        return cip;
    }

    public PharmaMLItemDTO setCip(String cip) {
        this.cip = cip;
        return this;
    }

    public String getEan() {
        return ean;
    }

    public PharmaMLItemDTO setEan(String ean) {
        this.ean = ean;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public PharmaMLItemDTO setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public boolean isLivraisonPartielle() {
        return livraisonPartielle;
    }

    public PharmaMLItemDTO setLivraisonPartielle(boolean livraisonPartielle) {
        this.livraisonPartielle = livraisonPartielle;
        return this;
    }

    public boolean isReliquats() {
        return reliquats;
    }

    public PharmaMLItemDTO setReliquats(boolean reliquats) {
        this.reliquats = reliquats;
        return this;
    }

    public boolean isLivraisonEquivalente() {
        return livraisonEquivalente;
    }

    public PharmaMLItemDTO setLivraisonEquivalente(boolean livraisonEquivalente) {
        this.livraisonEquivalente = livraisonEquivalente;
        return this;
    }

    public boolean isLivre() {
        return livre;
    }

    public PharmaMLItemDTO setLivre(boolean livre) {
        this.livre = livre;
        return this;
    }

    public int getQuantite() {
        return quantite;
    }

    public PharmaMLItemDTO setQuantite(int quantite) {
        this.quantite = quantite;
        return this;
    }

    public int getPrixUn() {
        return prixUn;
    }

    public PharmaMLItemDTO setPrixUn(int prixUn) {
        this.prixUn = prixUn;
        return this;
    }

    public int getPrixAchat() {
        return prixAchat;
    }

    public PharmaMLItemDTO setPrixAchat(int prixAchat) {
        this.prixAchat = prixAchat;
        return this;
    }

    public int getAmount() {
        return amount;
    }

    public PharmaMLItemDTO setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    public String getTypeCodification() {
        return typeCodification;
    }

    public PharmaMLItemDTO setTypeCodification(String typeCodification) {
        this.typeCodification = typeCodification;
        return this;
    }

    public int getCodeRetour() {
        return codeRetour;
    }

    public PharmaMLItemDTO setCodeRetour(int codeRetour) {
        this.codeRetour = codeRetour;
        return this;
    }
}
