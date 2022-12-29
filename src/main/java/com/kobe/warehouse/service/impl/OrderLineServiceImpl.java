package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.DateDimension;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.ParcoursProduitStatut;
import com.kobe.warehouse.repository.CustomizedProductService;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.web.rest.errors.GenericError;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
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

    public OrderLineServiceImpl(
        OrderLineRepository orderLineRepository,
        FournisseurProduitService fournisseurProduitService,
        ProduitRepository produitRepository,
        CustomizedProductService customizedProductService) {
        this.orderLineRepository = orderLineRepository;
        this.fournisseurProduitService = fournisseurProduitService;
        this.produitRepository = produitRepository;
        this.customizedProductService = customizedProductService;
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
        Optional<FournisseurProduit> fournisseurProduitOptional =
            fournisseurProduitService.findFirstByProduitIdAndFournisseurId(
                orderLineDTO.getProduitId(), orderLineDTO.getCommande().getFournisseur().getId());
        FournisseurProduit fournisseurProduit =
            fournisseurProduitOptional.orElseGet(
                () -> createNewFournisseurProduit(orderLineDTO, produit));
        OrderLine orderLine = buildOrderLine(orderLineDTO, fournisseurProduit);
        //  updateProduitParcours(produit, ParcoursProduitStatut.COMMANDE_EN_COURS);
        produitRepository.save(produit);
        return orderLine;
    }

    private void updateProduitParcours__(Produit produit, ParcoursProduitStatut produitStatut) {
  /*  if (CollectionUtils.isEmpty(produit.getParcoursProduits())) {
      produit.setParcoursProduits(
          Arrays.asList(
              new ParcoursProduit().setEvtDate(LocalDate.now()).setProduitStatut(produitStatut)));

    } else {
      produit
          .getParcoursProduits()
          .add(new ParcoursProduit().setEvtDate(LocalDate.now()).setProduitStatut(produitStatut));
    }*/
        produit.setUpdatedAt(Instant.now());
    }

    @Override
    public OrderLine buildOrderLine(
        OrderLineDTO orderLineDTO, FournisseurProduit fournisseurProduit) {
        OrderLine orderLine = new OrderLine();
        orderLine.createdAt(Instant.now());
        orderLine.setUpdatedAt(orderLine.getCreatedAt());
        orderLine.setInitStock(orderLineDTO.getTotalQuantity());
        orderLine.setQuantityRequested(orderLineDTO.getQuantityRequested());
        orderLine.setOrderUnitPrice(fournisseurProduit.getPrixUni());
        orderLine.setOrderCostAmount(fournisseurProduit.getPrixAchat());
        orderLine.setCostAmount(fournisseurProduit.getPrixAchat());
        orderLine.setRegularUnitPrice(fournisseurProduit.getPrixUni());
        orderLine.setFournisseurProduit(fournisseurProduit);
        orderLine.setOrderAmount(fournisseurProduit.getPrixUni() * orderLineDTO.getQuantityRequested());
        orderLine.setGrossAmount(
            fournisseurProduit.getPrixAchat() * orderLineDTO.getQuantityRequested());
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
        orderLine.setUpdatedAt(Instant.now());
        orderLineRepository.saveAndFlush(orderLine);
        return Pair.of(oldOrderLine, orderLine);
    }


    @Override
    public void updateOrderLine(OrderLine orderLine, int quantityRequested) {
        orderLine.setQuantityRequested(orderLine.getQuantityRequested() + quantityRequested);
        orderLine.setOrderAmount(orderLine.getRegularUnitPrice() * orderLine.getQuantityRequested());
        orderLine.setGrossAmount(orderLine.getCostAmount() * orderLine.getQuantityRequested());
        orderLine.setUpdatedAt(Instant.now());
    }

    @Override
    public void updateOrderLineQuantityReceived(OrderLine orderLine, int quantityReceived) {
        orderLine.setQuantityReceived(quantityReceived);
        orderLine.setUpdatedAt(Instant.now());
        orderLineRepository.save(orderLine);
    }

    @Override
    public void updateOrderLineQuantityUG(Long id, int quantityUg) {
        OrderLine orderLine = orderLineRepository.getReferenceById(id);
        orderLine.setQuantityUg(quantityUg);
        orderLine.setUpdatedAt(Instant.now());
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
    public void updateOrderLineQuantityReturned(OrderLineDTO orderLineDTO, OrderLine orderLine) {
    }

    @Override
    public Pair<OrderLine, OrderLine> updateOrderLineUnitPrice(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = orderLineRepository.getReferenceById(orderLineDTO.getId());
        OrderLine oldOrderLine = (OrderLine) orderLine.clone();
        orderLine.setOrderUnitPrice(orderLineDTO.getOrderUnitPrice());
        orderLine.setOrderAmount(orderLineDTO.getOrderUnitPrice() * orderLine.getQuantityRequested());
        orderLine.setUpdatedAt(Instant.now());
        orderLineRepository.saveAndFlush(orderLine);
        return Pair.of(oldOrderLine, orderLine);
    }

    @Override
    public Pair<OrderLine, OrderLine> updateOrderLineCostAmount(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = orderLineRepository.getReferenceById(orderLineDTO.getId());
        OrderLine oldOrderLine = (OrderLine) orderLine.clone();
        orderLine.setOrderCostAmount(orderLineDTO.getOrderCostAmount());
        orderLine.setGrossAmount(orderLine.getOrderCostAmount() * orderLine.getQuantityRequested());
        orderLine.setUpdatedAt(Instant.now());
        orderLineRepository.saveAndFlush(orderLine);
        return Pair.of(oldOrderLine, orderLine);
    }

    @Override
    public void deleteOrderLine(OrderLine orderLine) {
        orderLineRepository.delete(orderLine);
    }

    @Override
    public void saveOrderLine(SalesLine salesLine) {
    }

    @Override
    public void createOrderLineInventory(
        OrderLine salesLine, User user, DateDimension dateD, Long storageId) {
    }

    private FournisseurProduit createNewFournisseurProduit(OrderLineDTO orderLineDTO, Produit produit)
        throws GenericError {

        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        if (fournisseurProduit == null)
            throw new GenericError(
                "produit", "Ce produit n'a pas de fournisseur principal ", "mainProviderNotFound");
        orderLineDTO.setProvisionalCode(Boolean.TRUE);
        return fournisseurProduitService.createNewFournisseurProduit(
            new FournisseurProduitDTO()
                .setCodeCip(fournisseurProduit.getCodeCip())
                .setCreatedAt(Instant.now())
                .setUpdatedAt(Instant.now())
                .setFournisseurId(orderLineDTO.getCommande().getFournisseur().getId())
                .setPrincipal(false)
                .setPrixAchat(fournisseurProduit.getPrixAchat())
                .setPrixUni(fournisseurProduit.getPrixUni())
                .setProduitId(produit.getId()));
    }

    @Override
    public Optional<OrderLine> findOneById(Long id) {
        if (id == null) return Optional.empty();
        return orderLineRepository.findById(id);
    }

    @Override
    public OrderLine save(OrderLine orderLine) {
        return orderLineRepository.save(orderLine);
    }

    @Override
    public Optional<OrderLine> findOneFromCommande(
        Long produitId, Long commandeId, Long fournisseurId) {
        Optional<FournisseurProduit> fournisseurProduitOptional =
            fournisseurProduitService.findFirstByProduitIdAndFournisseurId(
                produitId, fournisseurId);
        if (fournisseurProduitOptional.isEmpty()) return Optional.empty();
        return orderLineRepository.findFirstByFournisseurProduitIdAndCommandeId(
            fournisseurProduitOptional.get().getId(), commandeId);
    }

    @Override
    public void updateCodeCip(OrderLineDTO orderLineDTO) {
        OrderLine orderLine = findOneById(orderLineDTO.getId()).get();
        fournisseurProduitService.updateCip(
            orderLineDTO.getProduitCip(), orderLine.getFournisseurProduit());
        orderLine.setProvisionalCode(Boolean.FALSE);
        orderLine.setUpdatedAt(Instant.now());
        orderLineRepository.save(orderLine);
    }

    @Override
    public void updateRequestedLineToPassedLine(Set<OrderLine> orderLines) {
        List<Produit> produits = new ArrayList<>();
        orderLines.forEach(
            orderLine -> {
                Produit produit = orderLine.getFournisseurProduit().getProduit();
                //updateProduitParcours(produit, ParcoursProduitStatut.COMMANDE_PASSE);
                produits.add(produit);
            });
        produitRepository.saveAll(produits);
    }

    @Override
    public OrderLine createOrderLine(
        Commande commande, OrderLineDTO orderLineDTO) {
        return buildOrderLine(commande, orderLineDTO);

    }

    @Override
    public Optional<FournisseurProduit> getFournisseurProduitByCriteria(
        String criteria, Long fournisseurId) {
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

    public OrderLine buildOrderLine(
        Commande commande, OrderLineDTO orderLineDTO) {
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
}
