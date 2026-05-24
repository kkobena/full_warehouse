package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO de requête pour créer un RetourBon depuis un lot périmé.
 * Le backend résout automatiquement la Commande source via Lot → OrderLine → Commande.
 * Si {@code commandeId} et {@code commandeOrderDate} sont fournis, ils sont utilisés en fallback
 * quand le lot n'a pas d'OrderLine associée.
 */
public class RetourBonFromLotRequest {

    @NotNull
    private Integer lotId;

    @NotNull
    private Integer motifRetourId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String commentaire;

    /** Override manuel de la commande source (fallback quand lot sans OrderLine) */
    private Integer commandeId;

    /** Override manuel de la date de commande source */
    private LocalDate commandeOrderDate;

    /**
     * ID du fournisseur — fourni quand le lot n'a pas d'orderLine ET que le backend
     * a renvoyé HORS_COMMANDE_MULTI (plusieurs fournisseurs possibles).
     * Si fourni, ce fournisseur est utilisé directement (bypass résolution auto).
     */
    private Integer fournisseurId;

    public Integer getLotId() { return lotId; }
    public RetourBonFromLotRequest setLotId(Integer lotId) { this.lotId = lotId; return this; }

    public Integer getMotifRetourId() { return motifRetourId; }
    public RetourBonFromLotRequest setMotifRetourId(Integer motifRetourId) { this.motifRetourId = motifRetourId; return this; }

    public Integer getQuantity() { return quantity; }
    public RetourBonFromLotRequest setQuantity(Integer quantity) { this.quantity = quantity; return this; }

    public String getCommentaire() { return commentaire; }
    public RetourBonFromLotRequest setCommentaire(String commentaire) { this.commentaire = commentaire; return this; }

    public Integer getCommandeId() { return commandeId; }
    public RetourBonFromLotRequest setCommandeId(Integer commandeId) { this.commandeId = commandeId; return this; }

    public LocalDate getCommandeOrderDate() { return commandeOrderDate; }
    public RetourBonFromLotRequest setCommandeOrderDate(LocalDate commandeOrderDate) { this.commandeOrderDate = commandeOrderDate; return this; }

    public boolean hasCommandeOverride() {
        return commandeId != null && commandeOrderDate != null;
    }

    public Integer getFournisseurId() { return fournisseurId; }
    public RetourBonFromLotRequest setFournisseurId(Integer fournisseurId) { this.fournisseurId = fournisseurId; return this; }

    public boolean hasFournisseurOverride() {
        return fournisseurId != null;
    }

    @Override
    public String toString() {
        return "RetourBonFromLotRequest{lotId=" + lotId + ", motifRetourId=" + motifRetourId
            + ", quantity=" + quantity + ", commandeOverride=" + (hasCommandeOverride() ? commandeId + "/" + commandeOrderDate : "none") + "}";
    }
}

