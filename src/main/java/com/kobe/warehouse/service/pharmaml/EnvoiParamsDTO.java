package com.kobe.warehouse.service.pharmaml;

import java.time.LocalDate;

public class EnvoiParamsDTO {

    private Long grossisteId;
    private Long commandeId;
    private LocalDate dateLivraisonSouhaitee;
    private int typeCommande;
    private String typeCommandeExecptionel;
    private String commentaire;
    private Long ruptureId;

    public Long getCommandeId() {
        return commandeId;
    }

    public EnvoiParamsDTO setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
        return this;
    }

    public LocalDate getDateLivraisonSouhaitee() {
        return dateLivraisonSouhaitee;
    }

    public EnvoiParamsDTO setDateLivraisonSouhaitee(LocalDate dateLivraisonSouhaitee) {
        this.dateLivraisonSouhaitee = dateLivraisonSouhaitee;
        return this;
    }

    public int getTypeCommande() {
        return typeCommande;
    }

    public EnvoiParamsDTO setTypeCommande(int typeCommande) {
        this.typeCommande = typeCommande;
        return this;
    }

    public String getTypeCommandeExecptionel() {
        return typeCommandeExecptionel;
    }

    public EnvoiParamsDTO setTypeCommandeExecptionel(String typeCommandeExecptionel) {
        this.typeCommandeExecptionel = typeCommandeExecptionel;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public EnvoiParamsDTO setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public Long getRuptureId() {
        return ruptureId;
    }

    public EnvoiParamsDTO setRuptureId(Long ruptureId) {
        this.ruptureId = ruptureId;
        return this;
    }

    public Long getGrossisteId() {
        return grossisteId;
    }

    public EnvoiParamsDTO setGrossisteId(Long grossisteId) {
        this.grossisteId = grossisteId;
        return this;
    }
}
