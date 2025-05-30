package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import com.kobe.warehouse.repository.CustomizedProductService;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.ProductStateService;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderLineServiceImpl implements OrderLineService {

    private final OrderLineRepository orderLineRepository;
    private final FournisseurProduitService fournisseurProduitService;
    private final ProduitRepository produitRepository;
    private final CustomizedProductService customizedProductService;
    private final ProductStateService productStateService;

    public OrderLineServiceImpl(
        OrderLineRepository orderLineRepository,
        FournisseurProduitService fournisseurProduitService,
        ProduitRepository produitRepository,
        CustomizedProductService customizedProductService,
        ProductStateService productStateService
    ) {
        this.orderLineRepository = orderLineRepository;
        this.fournisseurProduitService = fournisseurProduitService;
        this.produitRepository = produitRepository;
        this.customizedProductService = customizedProductService;
        this.productStateService = productStateService;
    }

    @Override
    public OrderLine createOrderLine(OrderLine orderLine) {
        return orderLineRepository.saveAndFlush(orderLine);
    }

    @Override
    public void updateOrderLine(OrderLine orderLine) {}

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
        //  updateProduitParcours(produit, ParcoursProduitStatut.COMMANDE_EN_COURS);
        if (!this.productStateService.existsByStateAndProduitId(ProductStateEnum.COMMANDE_EN_COURS, orderLineDTO.getProduitId())) {
            this.productStateService.addState(produit, ProductStateEnum.COMMANDE_EN_COURS);
        }

        produitRepository.save(produit);
        return orderLine;
    }

    @Override
    public OrderLine buildOrderLine(OrderLineDTO orderLineDTO, FournisseurProduit fournisseurProduit) {
        OrderLine orderLine = new OrderLine();
        orderLine.createdAt(LocalDateTime.now());
        orderLine.setUpdatedAt(orderLine.getCreatedAt());
        orderLine.setInitStock(orderLineDTO.getTotalQuantity());
        orderLine.setQuantityRequested(orderLineDTO.getQuantityRequested());
        orderLine.setOrderUnitPrice(fournisseurProduit.getPrixUni());
        orderLine.setOrderCostAmount(fournisseurProduit.getPrixAchat());
        orderLine.setCostAmount(fournisseurProduit.getPrixAchat());
        orderLine.setRegularUnitPrice(fournisseurProduit.getPrixUni());
        orderLine.setFournisseurProduit(fournisseurProduit);
        orderLine.setOrderAmount(fournisseurProduit.getPrixUni() * orderLineDTO.getQuantityRequested());
        orderLine.setGrossAmount(fournisseurProduit.getPrixAchat() * orderLineDTO.getQuantityRequested());
        if (ObjectUtils.isNotEmpty(orderLineDTO.getProvisionalCode())) {
            orderLine.setProvisionalCode(orderLineDTO.getProvisionalCode());
        }
        return orderLine;
    }

    @Override
    public Pair<OrderLine, OrderLine> updateOrderLineQuantityRequested(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = orderLineRepository.getReferenceById(orderLineDTO.getId());
        OrderLine oldOrderLine = (OrderLine) orderLine.clone();
        orderLine.setQuantityRequested(orderLineDTO.getQuantityRequested());
        orderLine.setOrderAmount(orderLine.getRegularUnitPrice() * orderLine.getQuantityRequested());
        orderLine.setGrossAmount(orderLine.getCostAmount() * orderLine.getQuantityRequested());
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.saveAndFlush(orderLine);
        return Pair.of(oldOrderLine, orderLine);
    }

    @Override
    public void updateOrderLine(OrderLine orderLine, int quantityRequested) {
        orderLine.setQuantityRequested(orderLine.getQuantityRequested() + quantityRequested);
        orderLine.setOrderAmount(orderLine.getRegularUnitPrice() * orderLine.getQuantityRequested());
        orderLine.setGrossAmount(orderLine.getCostAmount() * orderLine.getQuantityRequested());
        orderLine.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public void updateOrderLineQuantityReceived(OrderLine orderLine, int quantityReceived) {
        orderLine.setQuantityReceived(quantityReceived);
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.save(orderLine);
    }

    @Override
    public void updateOrderLineQuantityUG(Long id, int quantityUg) {
        OrderLine orderLine = orderLineRepository.getReferenceById(id);
        orderLine.setQuantityUg(quantityUg);
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.save(orderLine);
    }

    @Override
    public void saveAll(Set<OrderLine> orderLines) {
        orderLineRepository.saveAll(orderLines);
    }

    @Override
    public void deleteAll(Set<OrderLine> orderLines) {
        orderLineRepository.deleteAll(orderLines);
    }

    @Override
    public Pair<OrderLine, OrderLine> updateOrderLineUnitPrice(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = orderLineRepository.getReferenceById(orderLineDTO.getId());
        OrderLine oldOrderLine = (OrderLine) orderLine.clone();
        orderLine.setOrderUnitPrice(orderLineDTO.getOrderUnitPrice());
        orderLine.setOrderAmount(orderLineDTO.getOrderUnitPrice() * orderLine.getQuantityRequested());
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.saveAndFlush(orderLine);
        return Pair.of(oldOrderLine, orderLine);
    }

    @Override
    public Pair<OrderLine, OrderLine> updateOrderLineCostAmount(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = orderLineRepository.getReferenceById(orderLineDTO.getId());
        OrderLine oldOrderLine = (OrderLine) orderLine.clone();
        orderLine.setOrderCostAmount(orderLineDTO.getOrderCostAmount());
        orderLine.setGrossAmount(orderLine.getOrderCostAmount() * orderLine.getQuantityRequested());
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.saveAndFlush(orderLine);
        return Pair.of(oldOrderLine, orderLine);
    }

    @Override
    public void deleteOrderLine(OrderLine orderLine) {
        removeProductState(orderLine.getFournisseurProduit().getProduit(), orderLine.getCommande().getOrderStatus());
        orderLineRepository.delete(orderLine);
    }

    @Override
    public void removeProductState(List<Produit> produits, OrderStatut orderStatut) {
        produits.forEach(produit -> removeProductState(produit, orderStatut));
    }

    @Override
    public void rollbackProductState(List<Produit> produits) {
        produits.forEach(this::rollbackProductState);
    }

    @Override
    public int countByCommandeOrderStatusAndFournisseurProduitProduitId(OrderStatut orderStatut, Long produitId) {
        return this.orderLineRepository.countByCommandeOrderStatusAndFournisseurProduitProduitId(orderStatut, produitId);
    }

    private void rollbackProductState(Produit produit) {
        int count = this.countByCommandeOrderStatusAndFournisseurProduitProduitId(OrderStatut.PASSED, produit.getId());
        if (count == 1) {
            this.productStateService.removeByProduitAndState(produit, ProductStateEnum.COMMANDE_PASSE);
        }
        addProductStateEnCours(produit);
    }

    private void addProductStateEnCours(Produit produit) {
        if (!this.productStateService.existsByStateAndProduitId(ProductStateEnum.COMMANDE_EN_COURS, produit.getId())) {
            this.productStateService.addState(produit, ProductStateEnum.COMMANDE_EN_COURS);
        }
    }

    private void removeProductState(Produit produit, OrderStatut orderStatut) {
        int count = this.orderLineRepository.countByCommandeOrderStatusAndFournisseurProduitProduitId(orderStatut, produit.getId());
        if (count == 1) {
            switch (orderStatut) {
                case PASSED:
                    this.productStateService.removeByProduitAndState(produit, ProductStateEnum.COMMANDE_PASSE);
                    break;
                case REQUESTED:
                    this.productStateService.removeByProduitAndState(produit, ProductStateEnum.COMMANDE_EN_COURS);
                    break;
                default:
                    break;
            }
        }
    }

    private FournisseurProduit createNewFournisseurProduit(OrderLineDTO orderLineDTO, Produit produit) throws GenericError {
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        if (fournisseurProduit == null) {
            throw new GenericError("Ce produit n'a pas de fournisseur principal ", "mainProviderNotFound");
        }
        orderLineDTO.setProvisionalCode(Boolean.TRUE);
        return fournisseurProduitService.createNewFournisseurProduit(
            new FournisseurProduitDTO()
                .setCodeCip(fournisseurProduit.getCodeCip())
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now())
                .setFournisseurId(orderLineDTO.getCommande().getFournisseurId())
                .setPrincipal(false)
                .setPrixAchat(fournisseurProduit.getPrixAchat())
                .setPrixUni(fournisseurProduit.getPrixUni())
                .setProduitId(produit.getId())
        );
    }

    @Override
    public Optional<OrderLine> findOneById(Long id) {
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
    public Optional<OrderLine> findOneFromCommande(Long produitId, Long commandeId, Long fournisseurId) {
        Optional<FournisseurProduit> fournisseurProduitOptional = fournisseurProduitService.findFirstByProduitIdAndFournisseurId(
            produitId,
            fournisseurId
        );
        if (fournisseurProduitOptional.isEmpty()) {
            return Optional.empty();
        }
        return orderLineRepository.findFirstByFournisseurProduitIdAndCommandeId(fournisseurProduitOptional.get().getId(), commandeId);
    }

    @Override
    public void updateCodeCip(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = findOneById(orderLineDTO.getId()).get();
        fournisseurProduitService.updateCip(orderLineDTO.getProduitCip(), orderLine.getFournisseurProduit());
        orderLine.setProvisionalCode(Boolean.FALSE);
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.save(orderLine);
    }

    @Override
    public void updateRequestedLineToPassedLine(List<OrderLine> orderLines) {
        OrderStatut orderStatut = orderLines.getFirst().getCommande().getOrderStatus();

        orderLines.forEach(orderLine -> {
            removeProductState(orderLine.getFournisseurProduit().getProduit(), orderStatut);
            this.productStateService.addState(orderLine.getFournisseurProduit().getProduit(), ProductStateEnum.COMMANDE_PASSE);
        });
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
    public int produitTotalStock(FournisseurProduit fournisseurProduit) {
        return customizedProductService.produitTotalStock(fournisseurProduit.getProduit());
    }

    @Override
    public int produitTotalStockWithQantitUg(Produit produit) {
        return customizedProductService.produitTotalStock(produit);
    }

    @Override
    public List<FournisseurProduit> getFournisseurProduitsByFournisseur(Long founisseurId) {
        return fournisseurProduitService.getFournisseurProduitsByFournisseur(founisseurId, PageRequest.of(0, 1000));
    }

    private OrderLine buildOrderLine(Commande commande, OrderLineDTO orderLineDTO) {
        OrderLine orderLine = new OrderLine();
        orderLine.setCreatedAt(commande.getCreatedAt());
        orderLine.setUpdatedAt(commande.getCreatedAt());
        orderLine.setQuantityReceived(orderLineDTO.getQuantityReceived());
        orderLine.setInitStock(orderLineDTO.getInitStock());
        orderLine.setQuantityRequested(orderLineDTO.getQuantityRequested());
        orderLine.setOrderUnitPrice(orderLineDTO.getOrderUnitPrice());
        orderLine.setOrderCostAmount(orderLineDTO.getOrderCostAmount());
        orderLine.setCostAmount(orderLineDTO.getCostAmount());
        orderLine.setRegularUnitPrice(orderLineDTO.getRegularUnitPrice());
        orderLine.setOrderAmount(orderLineDTO.getOrderAmount());
        orderLine.setGrossAmount(orderLineDTO.getGrossAmount());
        orderLine.setProvisionalCode(orderLineDTO.getProvisionalCode());
        orderLine.setQuantityUg(orderLineDTO.getQuantityUg());
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
        orderLine.setCreatedAt(LocalDateTime.now());
        orderLine.setUpdatedAt(orderLine.getCreatedAt());
        orderLine.setQuantityReceived(0);
        orderLine.setInitStock(stockProduit);
        orderLine.setQuantityRequested(suggestionLine.getQuantity());
        orderLine.setOrderUnitPrice(fournisseurProduit.getPrixUni());
        orderLine.setOrderCostAmount(fournisseurProduit.getPrixAchat());
        orderLine.setOrderAmount(orderLine.getOrderUnitPrice() * suggestionLine.getQuantity());
        orderLine.setCostAmount(fournisseurProduit.getPrixAchat());
        orderLine.setRegularUnitPrice(fournisseurProduit.getPrixUni());
        orderLine.setGrossAmount(orderLine.getOrderCostAmount() * suggestionLine.getQuantity());
        orderLine.setProvisionalCode(false);
        orderLine.setQuantityUg(0);
        orderLine.setTaxAmount(0);
        return orderLine;
    }
}
