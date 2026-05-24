package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class RetourCompletCommandeRequest {

    @NotNull
    private Integer commandeId;

    @NotNull
    private LocalDate commandeOrderDate;

    @NotNull
    private Integer motifRetourId;

    private String commentaire;

    public Integer getCommandeId() {
        return commandeId;
    }

    public RetourCompletCommandeRequest setCommandeId(Integer commandeId) {
        this.commandeId = commandeId;
        return this;
    }

    public LocalDate getCommandeOrderDate() {
        return commandeOrderDate;
    }

    public RetourCompletCommandeRequest setCommandeOrderDate(LocalDate commandeOrderDate) {
        this.commandeOrderDate = commandeOrderDate;
        return this;
    }

    public Integer getMotifRetourId() {
        return motifRetourId;
    }

    public RetourCompletCommandeRequest setMotifRetourId(Integer motifRetourId) {
        this.motifRetourId = motifRetourId;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public RetourCompletCommandeRequest setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }
}
