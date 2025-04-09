package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
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

    Optional<OrderLine> findOneById(Long id);

    OrderLine save(OrderLine orderLine);

    Optional<OrderLine> findOneFromCommande(Long produitId, Long commandeId, Long fournisseurId);

    void updateCodeCip(OrderLineDTO orderLineDTO);

    void updateRequestedLineToPassedLine(List<OrderLine> orderLines);

    void updateOrderLine(OrderLine orderLine, int quantityRequested);

    void updateOrderLineQuantityReceived(OrderLine orderLine, int quantityReceived);

    void updateOrderLineQuantityUG(Long id, int quantityReceived);

    void saveAll(Set<OrderLine> orderLines);

    void deleteAll(Set<OrderLine> orderLines);

    OrderLine createOrderLine(Commande commande, OrderLineDTO orderLineDTO);

    Optional<FournisseurProduit> getFournisseurProduitByCriteria(String criteria, Long fournisseurId);

    int produitTotalStock(FournisseurProduit fournisseurProduit);

    int produitTotalStockWithQantitUg(Produit produit);

    List<FournisseurProduit> getFournisseurProduitsByFournisseur(Long founisseurId);

    void removeProductState(List<Produit> produits, OrderStatut orderStatut);

    void rollbackProductState(List<Produit> produits);
    int countByCommandeOrderStatusAndFournisseurProduitProduitId(OrderStatut orderStatut, Long produitId);
}
