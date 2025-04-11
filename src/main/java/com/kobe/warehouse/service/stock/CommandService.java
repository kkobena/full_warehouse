package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CommandService {
    Commande createNewCommande(Commande commande);

    Commande createNewCommandeFromCommandeDTO(CommandeDTO commande);

    Commande createOrUpdateOrderLine(OrderLineDTO orderLineDTO);

    Commande updateQuantityRequested(OrderLineDTO orderLineDTO);

    void updateOrderLineQuantityReceived(OrderLineDTO orderLineDTO);

    void updateOrderLineQuantityUg(OrderLineDTO orderLineDTO);

    Commande updateOrderCostAmount(OrderLineDTO orderLineDTO);

    Commande updateOrderUnitPrice(OrderLineDTO orderLineDTO);

    void deleteOrderLineById(Long orderLineId);

    void deleteById(Long id);

    void rollback(Long id);

    void updateCodeCip(OrderLineDTO orderLineDTO);

    void deleteOrderLinesByIds(Long commandeId, List<Long> ids);

    void closeCommandeEnCours(Long commandeId);

    void fusionner(List<Long> ids);

    void deleteAll(List<Long> ids);

    VerificationResponseCommandeDTO importerReponseCommande(Long commandeId, MultipartFile multipartFile);

    CommandeResponseDTO uploadNewCommande(Long fournisseurId, CommandeModel commandeModel, MultipartFile multipartFile);
    void createCommandeFromSuggestion(Suggestion suggestion);
}
