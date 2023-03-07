package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.DeliveryReceipt;
import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.WarehouseSequence;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.DeliveryReceiptItemRepository;
import com.kobe.warehouse.repository.DeliveryReceiptRepository;
import com.kobe.warehouse.repository.WarehouseSequenceRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.InventoryTransactionService;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.ProduitService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.LotJsonValue;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.StockEntryService;
import com.kobe.warehouse.web.rest.errors.GenericError;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class StockEntryServiceImpl implements StockEntryService {
  private final Logger log = LoggerFactory.getLogger(StockEntryServiceImpl.class);
  private final CommandeRepository commandeRepository;

  private final ProduitService produitService;

  private final DeliveryReceiptItemRepository deliveryReceiptItemRepository;
  private final LotService lotService;
  private final StorageService storageService;
  private final DeliveryReceiptRepository deliveryReceiptRepository;
  private final FournisseurProduitService fournisseurProduitService;
  private final LogsService logsService;
  private final WarehouseSequenceRepository warehouseSequenceRepository;
  private final InventoryTransactionService inventoryTransactionService;

  private final Predicate<OrderLine> isNotEntreeStockIsAuthorize =
      orderLine -> {
        if (Objects.nonNull(orderLine.getReceiptDate())
            && Objects.nonNull(orderLine.getQuantityReceived())) {
          return (orderLine.getQuantityReceived() < orderLine.getQuantityRequested())
              && orderLine.getFournisseurProduit().getProduit().getCheckExpiryDate();
        }
        return false;
      };

  private final BiPredicate<OrderLine, List<LotJsonValue>> cannotContinue =
      (orderLine, lotJsonValueList) -> {
        if (lotJsonValueList.isEmpty()) return false;
        return orderLine.getQuantityRequested()
            < lotJsonValueList.stream()
                .map(LotJsonValue::getQuantityReceived)
                .reduce(Integer::sum)
                .get();
      };

  public StockEntryServiceImpl(
      CommandeRepository commandeRepository,
      ProduitService produitService,
      InventoryTransactionService inventoryTransactionService,
      DeliveryReceiptItemRepository deliveryReceiptItemRepository,
      LotService lotService,
      StorageService storageService,
      DeliveryReceiptRepository deliveryReceiptRepository,
      FournisseurProduitService fournisseurProduitService,
      LogsService logsService,
      WarehouseSequenceRepository warehouseSequenceRepository,
      InventoryTransactionService inventoryTransactionService1) {
    this.commandeRepository = commandeRepository;
    this.produitService = produitService;
    this.deliveryReceiptItemRepository = deliveryReceiptItemRepository;
    this.lotService = lotService;
    this.storageService = storageService;
    this.deliveryReceiptRepository = deliveryReceiptRepository;
    this.fournisseurProduitService = fournisseurProduitService;
    this.logsService = logsService;
    this.warehouseSequenceRepository = warehouseSequenceRepository;
    this.inventoryTransactionService = inventoryTransactionService1;
  }

  @Override
  public Commande saveSaisieEntreeStock(CommandeDTO commandeDTO) {
    return commandeRepository.saveAndFlush(updateCommande(commandeDTO));
  }

  private Commande updateCommande(CommandeDTO commandeDTO) {
    Commande commande = commandeRepository.getReferenceById(commandeDTO.getId());
    return commande
        .setReceiptDate(commandeDTO.getReceiptDate())
        .setReceiptAmount(commandeDTO.getReceiptAmount())
        .taxAmount(commandeDTO.getTaxAmount())
        .setReceiptRefernce(commandeDTO.getReceiptRefernce())
        .setSequenceBon(commandeDTO.getSequenceBon())
        .setLastUserEdit(storageService.getUser())
        .updatedAt(Instant.now());
  }

  @Override
  public void finalizeSaisieEntreeStock(CommandeDTO commandeDTO) {
    Commande commande = updateCommande(commandeDTO);
    commande.orderStatus(OrderStatut.CLOSED);
    Set<OrderLine> orderLineSet = commande.getOrderLines();
    DeliveryReceipt deliveryReceipt = createDeliveryReceipt(commande);
    // TODO liste des vente en avoir pour envoi possible de notif et de mail
    orderLineSet.forEach(
        orderLine -> {
          if (isNotEntreeStockIsAuthorize.test(orderLine)
              && cannotContinue.test(orderLine, getLotByOrderLine(orderLine, commande)))
            throw new GenericError(
                "La reception de certains produits n'a pas ete faite. Veuillez verifier la saisie",
                "commande",
                "commandeManquante");

          DeliveryReceiptItem deliveryReceiptItem = addItem(orderLine, deliveryReceipt);
          getLotByOrderLine(orderLine, commande)
              .forEach(
                  lotJsonValue ->
                      lotService.addLot(
                          lotJsonValue, deliveryReceiptItem, deliveryReceipt.getReceiptRefernce()));
          FournisseurProduit fournisseurProduit = updateFournisseurProduit(deliveryReceiptItem);
          StockProduit stockProduit =
              produitService.updateTotalStock(
                  fournisseurProduit.getProduit(),
                  deliveryReceiptItem.getQuantityReceived(),
                  deliveryReceiptItem.getUgQuantity());
          Produit produit = stockProduit.getProduit();

          produit.setPrixMnp(
              produitService.calculPrixMoyenPondereReception(
                  deliveryReceiptItem.getInitStock(),
                  orderLine.getGrossAmount(),
                  getTotalStockQuantity(stockProduit),
                  deliveryReceiptItem.getOrderCostAmount()));
          produitService.update(produit);
        });
    logsService.create(
        TransactionType.ENTREE_STOCK,
        "order.entry",
        new Object[] {deliveryReceipt.getReceiptRefernce()},
        deliveryReceipt.getId().toString());
    commandeRepository.saveAndFlush(commande);
  }

  private DeliveryReceiptItem addItem(OrderLine orderLine, DeliveryReceipt deliveryReceipt) {
    FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
    DeliveryReceiptItem receiptItem = new DeliveryReceiptItem();
    receiptItem.setDeliveryReceipt(deliveryReceipt);
    receiptItem.setCreatedDate(deliveryReceipt.getCreatedDate());
    receiptItem.setQuantityReceived(
        Objects.nonNull(orderLine.getQuantityReceived())
            ? orderLine.getQuantityReceived()
            : orderLine.getQuantityRequested());
    receiptItem.setInitStock(
        produitService.getProductTotalStock(fournisseurProduit.getProduit().getId()));
    receiptItem.setDiscountAmount(0);
    receiptItem.setUgQuantity(orderLine.getQuantityUg() != null ? orderLine.getQuantityUg() : 0);
    receiptItem.setOrderCostAmount(orderLine.getOrderCostAmount());
    receiptItem.setRegularUnitPrice(orderLine.getRegularUnitPrice());
    receiptItem.setOrderUnitPrice(orderLine.getOrderUnitPrice());
    receiptItem.setQuantityRequested(orderLine.getQuantityRequested());
    receiptItem.setFournisseurProduit(fournisseurProduit);
    receiptItem.setNetAmount(0);
    receiptItem.setTaxAmount(0);
    receiptItem.setQuantityReturned(0);
    receiptItem = deliveryReceiptItemRepository.save(receiptItem);
    inventoryTransactionService.saveInventoryTransaction(receiptItem, storageService.getUser());
    return receiptItem;
  }

  private DeliveryReceipt createDeliveryReceipt(Commande commande) {
    DeliveryReceipt deliveryReceipt = new DeliveryReceipt();
    deliveryReceipt.setReceiptStatut(ReceiptStatut.UNPAID);
    deliveryReceipt.setCreatedDate(LocalDateTime.now());
    deliveryReceipt.setReceiptDate(commande.getReceiptDate());
    deliveryReceipt.setCreatedUser(storageService.getUser());
    deliveryReceipt.setModifiedDate(deliveryReceipt.getCreatedDate());
    deliveryReceipt.setModifiedUser(deliveryReceipt.getCreatedUser());
    deliveryReceipt.setReceiptAmount(commande.getReceiptAmount());
    deliveryReceipt.setDiscountAmount(0);
    deliveryReceipt.setNetAmount(0);
    deliveryReceipt.setTaxAmount(commande.getTaxAmount());
    deliveryReceipt.setReceiptRefernce(commande.getReceiptRefernce());
    deliveryReceipt.setReceiptRefernce(commande.getReceiptRefernce());
    deliveryReceipt.setSequenceBon(commande.getSequenceBon());
    deliveryReceipt.setFournisseur(commande.getFournisseur());
    deliveryReceipt.setNumberTransaction(buildDeliveryReceiptNumberTransaction());
    return deliveryReceiptRepository.save(deliveryReceipt);
  }

  private List<LotJsonValue> getLotByOrderLine(OrderLine orderLine, Commande commande) {
    if (CollectionUtils.isEmpty(commande.getLots())) return Collections.emptyList();
    return commande.getLots().stream()
        .filter(lotJsonValue -> lotJsonValue.getReceiptItem().compareTo(orderLine.getId()) == 0)
        .toList();
  }

  private String buildDeliveryReceiptNumberTransaction() {
    WarehouseSequence warehouseSequence =
        warehouseSequenceRepository.getReferenceById(EntityConstant.ENTREE_STOCK_SEQUENCE_ID);
    String num =
        StringUtils.leftPad(warehouseSequence.getValue() + "", EntityConstant.LEFTPAD_SIZE, '0');
    warehouseSequence.setValue(warehouseSequence.getValue() + warehouseSequence.getIncrement());
    warehouseSequenceRepository.save(warehouseSequence);
    return num;
  }

  private FournisseurProduit updateFournisseurProduit(DeliveryReceiptItem deliveryReceiptItem) {
    FournisseurProduit fournisseurProduit = deliveryReceiptItem.getFournisseurProduit();
    Produit produit = fournisseurProduit.getProduit();
    int montantAdditionel = produit.getTableau() != null ? produit.getTableau().getValue() : 0;
    fournisseurProduit.setUpdatedAt(Instant.now());
    fournisseurProduit.setPrixAchat(deliveryReceiptItem.getOrderCostAmount());
    fournisseurProduit.setPrixUni(deliveryReceiptItem.getOrderUnitPrice() + montantAdditionel);
    return fournisseurProduitService.update(fournisseurProduit);
  }

  private int getTotalStockQuantity(StockProduit stockProduit) {
    return stockProduit.getQtyStock()
        + (Objects.nonNull(stockProduit.getQtyUG()) ? stockProduit.getQtyStock() : 0);
  }
}
