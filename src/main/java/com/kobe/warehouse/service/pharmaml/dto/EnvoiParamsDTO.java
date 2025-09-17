package com.kobe.warehouse.service.pharmaml.dto;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.service.pharmaml.dto.enumeration.TypeCommande;
import java.time.LocalDate;

public class EnvoiParamsDTO {

    private Long grossisteId;
    private CommandeId commandeId;
    private LocalDate dateLivraisonSouhaitee;
    private TypeCommande typeCommande;
    private String commentaire;
    private Long ruptureId;

    public CommandeId getCommandeId() {
        return commandeId;
    }

    public EnvoiParamsDTO setCommandeId(CommandeId commandeId) {
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

    public TypeCommande getTypeCommande() {
        return typeCommande;
    }

    public EnvoiParamsDTO setTypeCommande(TypeCommande typeCommande) {
        this.typeCommande = typeCommande;
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
