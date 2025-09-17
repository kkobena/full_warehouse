package com.kobe.warehouse.service.impl;


import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.repository.CustomizedProductService;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.OrderLineIdGeneratorService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class OrderLineServiceImpl implements OrderLineService {

    private final OrderLineRepository orderLineRepository;
    private final FournisseurProduitService fournisseurProduitService;
    private final ProduitRepository produitRepository;
    private final CustomizedProductService customizedProductService;
    private final OrderLineIdGeneratorService orderLineIdGeneratorService;

    public OrderLineServiceImpl(
        OrderLineRepository orderLineRepository,
        FournisseurProduitService fournisseurProduitService,
        ProduitRepository produitRepository,
        CustomizedProductService customizedProductService,
        OrderLineIdGeneratorService orderLineIdGeneratorService
    ) {
        this.orderLineRepository = orderLineRepository;
        this.fournisseurProduitService = fournisseurProduitService;
        this.produitRepository = produitRepository;
        this.customizedProductService = customizedProductService;
        this.orderLineIdGeneratorService = orderLineIdGeneratorService;

    }

    @Override
    public OrderLine createOrderLine(OrderLine orderLine) {
        return orderLineRepository.saveAndFlush(orderLine);
    }

    @Override
    public void updateOrderLine(OrderLine orderLine) {
    }

    @Override
    public OrderLine buildOrderLineFromOrderLineDTO(OrderLineDTO orderLineDTO) {
        Produit produit = produitRepository.getReferenceById(orderLineDTO.getProduitId());
        Optional<FournisseurProduit> fournisseurProduitOptional = fournisseurProduitService.findFirstByProduitIdAndFournisseurId(
            orderLineDTO.getProduitId(),
            orderLineDTO.getCommande().getFournisseurId()
        );
        FournisseurProduit fournisseurProduit = fournisseurProduitOptional.orElseGet(() ->
            createNewFournisseurProduit(orderLineDTO, produit)
        );
        OrderLine orderLine = buildOrderLine(orderLineDTO, fournisseurProduit);

        produitRepository.save(produit);
        return orderLine;
    }

    @Override
    public OrderLine buildOrderLine(OrderLineDTO orderLineDTO, FournisseurProduit fournisseurProduit) {
        OrderLine orderLine = new OrderLine();
        orderLine.setId(this.orderLineIdGeneratorService.nextId());
        orderLine.createdAt(LocalDateTime.now());
        orderLine.setUpdatedAt(orderLine.getCreatedAt());
        orderLine.setInitStock(orderLineDTO.getTotalQuantity());
        orderLine.setQuantityRequested(orderLineDTO.getQuantityRequested());
        orderLine.setOrderUnitPrice(fournisseurProduit.getPrixUni());
        orderLine.setOrderCostAmount(fournisseurProduit.getPrixAchat());
        orderLine.setFournisseurProduit(fournisseurProduit);
        if (ObjectUtils.isNotEmpty(orderLineDTO.getProvisionalCode())) {
            orderLine.setProvisionalCode(orderLineDTO.getProvisionalCode());
        }
        return orderLine;
    }

    @Override
    public Pair<OrderLine, OrderLine> updateOrderLineQuantityRequested(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = orderLineRepository.getReferenceById(orderLineDTO.getOrderLineId());
        OrderLine oldOrderLine = (OrderLine) orderLine.clone();
        orderLine.setQuantityRequested(orderLineDTO.getQuantityRequested());
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.saveAndFlush(orderLine);
        return Pair.of(oldOrderLine, orderLine);
    }

    @Override
    public void updateOrderLine(OrderLine orderLine, int quantityRequested) {
        orderLine.setQuantityRequested(orderLine.getQuantityRequested() + quantityRequested);
        orderLine.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public void updateOrderLineQuantityReceived(OrderLine orderLine, int quantityReceived) {
        orderLine.setQuantityReceived(quantityReceived);
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.save(orderLine);
    }

    @Override
    public void updateOrderLineQuantityUG(OrderLineId id, int quantityUg) {
        OrderLine orderLine = orderLineRepository.getReferenceById(id);
        orderLine.setFreeQty(quantityUg);
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.save(orderLine);
    }

    @Override
    public void saveAll(Set<OrderLine> orderLines) {
        orderLineRepository.saveAll(orderLines);
    }

    @Override
    public void saveAll(List<OrderLine> orderLines) {
        orderLineRepository.saveAll(orderLines);
    }

    @Override
    public void deleteAll(Set<OrderLine> orderLines) {
        orderLineRepository.deleteAll(orderLines);
    }

    @Override
    public Pair<OrderLine, OrderLine> updateOrderLineUnitPrice(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = orderLineRepository.getReferenceById(orderLineDTO.getOrderLineId());
        OrderLine oldOrderLine = (OrderLine) orderLine.clone();
        orderLine.setOrderUnitPrice(orderLineDTO.getOrderUnitPrice());
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.saveAndFlush(orderLine);
        return Pair.of(oldOrderLine, orderLine);
    }

    @Override
    public Pair<OrderLine, OrderLine> updateOrderLineCostAmount(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = orderLineRepository.getReferenceById(orderLineDTO.getOrderLineId());
        OrderLine oldOrderLine = (OrderLine) orderLine.clone();
        orderLine.setOrderCostAmount(orderLineDTO.getOrderCostAmount());
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.saveAndFlush(orderLine);
        return Pair.of(oldOrderLine, orderLine);
    }

    @Override
    public void deleteOrderLine(OrderLine orderLine) {
        orderLineRepository.delete(orderLine);
    }


    private FournisseurProduit createNewFournisseurProduit(OrderLineDTO orderLineDTO, Produit produit) throws GenericError {
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        if (fournisseurProduit == null) {
            throw new GenericError("Ce produit n'a pas de fournisseur principal ", "mainProviderNotFound");
        }
        orderLineDTO.setProvisionalCode(Boolean.TRUE);
        return createNewFromOne(fournisseurProduit, orderLineDTO.getCommande().getFournisseurId(), produit.getId());
    }

    private FournisseurProduit createNewFromOne(FournisseurProduit fournisseurProduit, Long fournisseurId, Long produitId) {
        return fournisseurProduitService.createNewFournisseurProduit(
            new FournisseurProduitDTO()
                .setCodeCip(fournisseurProduit.getCodeCip())
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now())
                .setFournisseurId(fournisseurId)
                .setPrincipal(false)
                .setPrixAchat(fournisseurProduit.getPrixAchat())
                .setPrixUni(fournisseurProduit.getPrixUni())
                .setProduitId(produitId)
        );
    }

    @Override
    public Optional<OrderLine> findOneById(OrderLineId id) {
        if (id == null) {
            return Optional.empty();
        }
        return orderLineRepository.findById(id);
    }

    @Override
    public OrderLine save(OrderLine orderLine) {
        return orderLineRepository.save(orderLine);
    }

    @Override
    public Optional<OrderLine> findOneFromCommande(Long produitId, CommandeId commandeId, Long fournisseurId) {
        Optional<FournisseurProduit> fournisseurProduitOptional = fournisseurProduitService.findFirstByProduitIdAndFournisseurId(
            produitId,
            fournisseurId
        );
        if (fournisseurProduitOptional.isEmpty()) {
            return Optional.empty();
        }
        return orderLineRepository.findFirstByFournisseurProduitIdAndCommandeIdAndCommandeOrderDate(fournisseurProduitOptional.get().getId(), commandeId.getId(), commandeId.getOrderDate());
    }

    @Override
    public void updateCodeCip(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = findOneById(orderLineDTO.getOrderLineId()).get();
        fournisseurProduitService.updateCip(orderLineDTO.getProduitCip(), orderLine.getFournisseurProduit());
        orderLine.setProvisionalCode(Boolean.FALSE);
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.save(orderLine);
    }

    @Override
    public OrderLine createOrderLine(Commande commande, OrderLineDTO orderLineDTO) {
        return buildOrderLine(commande, orderLineDTO);
    }

    @Override
    public Optional<FournisseurProduit> getFournisseurProduitByCriteria(String criteria, Long fournisseurId) {
        return customizedProductService.getFournisseurProduitByCriteria(criteria, fournisseurId);
    }


    @Override
    public int produitTotalStockWithQantitUg(Produit produit) {
        return customizedProductService.produitTotalStock(produit);
    }


    private OrderLine buildOrderLine(Commande commande, OrderLineDTO orderLineDTO) {
        OrderLine orderLine = new OrderLine();
        orderLine.setId(this.orderLineIdGeneratorService.nextId());
        orderLine.setCreatedAt(commande.getCreatedAt());
        orderLine.setUpdatedAt(commande.getCreatedAt());
        orderLine.setQuantityReceived(orderLineDTO.getQuantityReceived());
        orderLine.setInitStock(orderLineDTO.getInitStock());
        orderLine.setQuantityRequested(orderLineDTO.getQuantityRequested());
        orderLine.setOrderUnitPrice(orderLineDTO.getOrderUnitPrice());
        orderLine.setOrderCostAmount(orderLineDTO.getOrderCostAmount());
        orderLine.setProvisionalCode(orderLineDTO.getProvisionalCode());
        orderLine.setFreeQty(orderLineDTO.getFreeQty());
        orderLine.setTaxAmount(orderLine.getTaxAmount());
        return orderLine;
    }

    @Override
    public OrderLine buildOrderLine(SuggestionLine suggestionLine) {
        FournisseurProduit fournisseurProduit = suggestionLine.getFournisseurProduit();
        int stockProduit = fournisseurProduit
            .getProduit()
            .getStockProduits()
            .stream()
            .map(StockProduit::getTotalStockQuantity)
            .reduce(0, Integer::sum);
        OrderLine orderLine = new OrderLine();
        orderLine.setId(this.orderLineIdGeneratorService.nextId());
        orderLine.setCreatedAt(LocalDateTime.now());
        orderLine.setUpdatedAt(orderLine.getCreatedAt());
        orderLine.setQuantityReceived(0);
        orderLine.setInitStock(stockProduit);
        orderLine.setQuantityRequested(suggestionLine.getQuantity());
        orderLine.setOrderUnitPrice(fournisseurProduit.getPrixUni());
        orderLine.setOrderCostAmount(fournisseurProduit.getPrixAchat());
        orderLine.setProvisionalCode(false);
        orderLine.setFreeQty(0);
        orderLine.setTaxAmount(0);
        return orderLine;
    }

    @Override
    public void changeFournisseurProduit(OrderLine orderLine, Long fournisseurId) {
        Produit produit = orderLine.getFournisseurProduit().getProduit();
        this.fournisseurProduitService.findFirstByProduitIdAndFournisseurId(produit.getId(), fournisseurId).ifPresentOrElse(
            newFournisseurProduit -> {
                orderLine.setFournisseurProduit(newFournisseurProduit);
                orderLine.setProvisionalCode(false);
                updateOrderLineAmount(orderLine, newFournisseurProduit);
            },
            () -> {
                FournisseurProduit fournisseurProduit = createNewFromOne(
                    produit.getFournisseurProduitPrincipal(),
                    fournisseurId,
                    produit.getId()
                );
                orderLine.setFournisseurProduit(fournisseurProduit);
                orderLine.setProvisionalCode(true);
                updateOrderLineAmount(orderLine, fournisseurProduit);
            }
        );
    }

    @Override
    public void delete(OrderLine orderLine) {
        this.orderLineRepository.delete(orderLine);
    }

    private void updateOrderLineAmount(OrderLine orderLine, FournisseurProduit fournisseurProduit) {
        orderLine.setOrderUnitPrice(fournisseurProduit.getPrixUni());
        orderLine.setOrderCostAmount(fournisseurProduit.getPrixAchat());
        orderLineRepository.save(orderLine);
    }

    @Override
    public OrderLine buildDeliveryReceiptItemFromRecord(
        FournisseurProduit fournisseurProduit,
        int quantityRequested,
        int quantityReceived,
        int orderCostAmount,
        int orderUnitPrice,
        int quantityUg,
        int stock,
        int taxeAmount,
        Commande commande
    ) {
        OrderLine orderLine = new OrderLine();
        orderLine.setId(this.orderLineIdGeneratorService.nextId());
        orderLine.setFreeQty(quantityUg);
        orderLine.setCreatedAt(commande.getCreatedAt());
        orderLine.setQuantityReceived(quantityReceived);
        orderLine.setQuantityRequested(quantityRequested);
        orderLine.setOrderUnitPrice(orderUnitPrice > 0 ? orderUnitPrice : fournisseurProduit.getPrixUni());
        orderLine.setOrderCostAmount(orderCostAmount > 0 ? orderCostAmount : fournisseurProduit.getPrixAchat());
        orderLine.setInitStock(stock);
        orderLine.setFournisseurProduit(fournisseurProduit);
        orderLine.setTaxAmount(taxeAmount);
        return orderLine;
    }
}
