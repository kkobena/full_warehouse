package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
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

    void createCommandeFromSuggestion(Suggestion suggestion);

    void changeGrossiste(CommandeDTO commandeDTO);
}
