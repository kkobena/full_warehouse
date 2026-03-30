package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO;
import com.kobe.warehouse.service.dto.FournisseurStatsServiceDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.SemoisCommanderDTO;
import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CommandService {
    CommandeLiteDTO createNewCommandeFromCommandeDTO(CommandeDTO commande);

    CommandeLiteDTO createOrUpdateOrderLine(OrderLineDTO orderLineDTO);

    CommandeLiteDTO updateQuantityRequested(OrderLineDTO orderLineDTO);

    void updateOrderLineQuantityReceived(OrderLineDTO orderLineDTO);

    void updateOrderLineQuantityUg(OrderLineDTO orderLineDTO);

    Commande updateOrderCostAmount(OrderLineDTO orderLineDTO);

    Commande updateOrderUnitPrice(OrderLineDTO orderLineDTO);

    void deleteOrderLineById(OrderLineId orderLineId);

    void deleteById(CommandeId id);

    void rollback(CommandeId id);

    void updateCodeCip(OrderLineDTO orderLineDTO);

    void deleteOrderLinesByIds(CommandeId commandeId, List<OrderLineId> ids);

    void fusionner(List<CommandeId> ids);

    void deleteAll(List<CommandeId> ids);

    VerificationResponseCommandeDTO importerReponseCommande(CommandeId commandeId, MultipartFile multipartFile);

    CommandeResponseDTO uploadNewCommande(Integer fournisseurId, CommandeModel commandeModel, MultipartFile multipartFile);

    /**
     * Crée une commande à partir de toutes les lignes d'une suggestion.
     *
     * @param suggestion      Suggestion source
     * @param fournisseurId   Fournisseur cible (null = utilise le fournisseur de la suggestion)
     */
    CommandeId createCommandeFromSuggestion(Suggestion suggestion, Integer fournisseurId);

    /**
     * Crée une commande à partir d'une sélection de lignes de suggestion.
     *
     * @param suggestion      Suggestion source
     * @param lignes          Sélection de lignes avec quantités
     * @param fournisseurId   Fournisseur cible (null = utilise le fournisseur de la suggestion)
     */
    CommandeId createCommandeFromSelection(Suggestion suggestion, List<CommanderSelectionDTO.LigneSelection> lignes, Integer fournisseurId);

    /**
     * Crée une commande à partir d'une liste de lignes SEMOIS pour un fournisseur donné.
     * Utilisé par le module SEMOIS pour déclencher les réapprovisionnements urgents.
     *
     * @param fournisseurId ID du fournisseur cible
     * @param lignes        Lignes SEMOIS (produitId + quantite) pour ce fournisseur
     */
    void createCommandeFromSemoisLines(Integer fournisseurId, List<SemoisCommanderDTO.LigneSemois> lignes);

    void importSuggestionIntoCommande(CommandeId commandeId, Integer suggestionId);

    void changeGrossiste(CommandeDTO commandeDTO);

    /**
     * Calcule le taux de service et le délai moyen de livraison pour un fournisseur
     * sur une période glissante de {@code periodeJours} jours.
     *
     * @param fournisseurId identifiant du fournisseur
     * @param periodeJours  nombre de jours en arrière (ex. 30, 60, 90)
     */
    FournisseurStatsServiceDTO getStatsService(Integer fournisseurId, int periodeJours);

    /**
     * Crée un reliquat à partir d'une commande clôturée.
     * Les lignes dont la quantité reçue est inférieure à la quantité commandée
     * sont reportées dans une nouvelle commande REQUESTED avec le même fournisseur.
     *
     * @param commandeId identifiant de la commande source (déjà CLOSED)
     * @return la nouvelle commande reliquat (REQUESTED)
     * @throws com.kobe.warehouse.service.errors.GenericError si aucune ligne partielle n'est détectée
     */
    CommandeLiteDTO createReliquat(CommandeId commandeId);
}
