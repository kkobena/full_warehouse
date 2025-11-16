package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.util.Pair;

public interface OrderLineService {
    OrderLine createOrderLine(OrderLine orderLine);

    void updateOrderLine(OrderLine orderLine);

    OrderLine buildOrderLineFromOrderLineDTO(OrderLineDTO orderLineDTO) throws GenericError;

    OrderLine buildOrderLine(OrderLineDTO orderLineDTO, FournisseurProduit fournisseurProduit);

    Pair<OrderLine, OrderLine> updateOrderLineQuantityRequested(OrderLineDTO orderLineDTO);

    Pair<OrderLine, OrderLine> updateOrderLineUnitPrice(OrderLineDTO orderLineDTO);

    Pair<OrderLine, OrderLine> updateOrderLineCostAmount(OrderLineDTO orderLineDTO);

    void deleteOrderLine(OrderLine orderLine);

    Optional<OrderLine> findOneById(OrderLineId id);

    OrderLine save(OrderLine orderLine);

    Optional<OrderLine> findOneFromCommande(Integer produitId, CommandeId commandeId, Integer fournisseurId);

    void updateCodeCip(OrderLineDTO orderLineDTO);

    void updateOrderLine(OrderLine orderLine, int quantityRequested);

    void updateOrderLineQuantityReceived(OrderLine orderLine, int quantityReceived);

    void updateOrderLineQuantityUG(OrderLineId id, int quantityReceived);

    void saveAll(List<OrderLine> orderLines);

    void saveAll(Set<OrderLine> orderLines);

    void deleteAll(Set<OrderLine> orderLines);

    OrderLine createOrderLine(Commande commande, OrderLineDTO orderLineDTO);

    Optional<FournisseurProduit> getFournisseurProduitByCriteria(String criteria, Integer fournisseurId);

    int produitTotalStockWithQantitUg(Produit produit);

    OrderLine buildOrderLine(SuggestionLine suggestionLine);

    void changeFournisseurProduit(OrderLine orderLine, Integer fournisseurId);

    void delete(OrderLine orderLine);
    OrderLine buildDeliveryReceiptItemFromRecord(
        FournisseurProduit fournisseurProduit,
        int quantityRequested,
        int quantityReceived,
        int orderCostAmount,
        int orderUnitPrice,
        int quantityUg,
        int stock,
        int taxeAmount,
        Commande commande
    );
}
