package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.CommandeId;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Résultat de la finalisation d'une entrée en stock.
 * Contient l'identifiant de la commande finalisée et
 * la liste des retours fournisseur en attente de réponse,
 * afin de notifier le pharmacien.
 */
public class StockEntryResultDTO {

    private CommandeId commandeId;
    private List<PendingRetourBon> pendingRetourBons;

    public StockEntryResultDTO(CommandeId commandeId, List<PendingRetourBon> pendingRetourBons) {
        this.commandeId = commandeId;
        this.pendingRetourBons = pendingRetourBons;
    }

    public CommandeId getCommandeId() {
        return commandeId;
    }

    public List<PendingRetourBon> getPendingRetourBons() {
        return pendingRetourBons;
    }

    public boolean hasPendingRetourBons() {
        return pendingRetourBons != null && !pendingRetourBons.isEmpty();
    }

    /**
     * Résumé léger d'un retour fournisseur en attente de réponse.
     */
    public record PendingRetourBon(Integer id, LocalDateTime dateMtv, String commentaire, int itemCount) {}
}
