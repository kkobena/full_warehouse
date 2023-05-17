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
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.WarehouseSequenceRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.ProduitService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptItemLiteDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptLiteDTO;
import com.kobe.warehouse.service.dto.LotJsonValue;
import com.kobe.warehouse.service.dto.OrderItem;
import com.kobe.warehouse.service.dto.UploadDeleiveryReceiptDTO;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.StockEntryService;
import com.kobe.warehouse.service.utils.FileUtil;
import com.kobe.warehouse.web.rest.errors.GenericError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

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
  private final FournisseurRepository fournisseurRepository;
  private final OrderLineService orderLineService;

  private final Predicate<OrderLine> isNotEntreeStockIsAuthorize = orderLine -> {
    if (Objects.nonNull(orderLine.getReceiptDate()) && Objects.nonNull(
        orderLine.getQuantityReceived())) {
      return (orderLine.getQuantityReceived() < orderLine.getQuantityRequested())
          && orderLine.getFournisseurProduit().getProduit().getCheckExpiryDate();
    }
    return false;
  };

  private final BiPredicate<OrderLine, List<LotJsonValue>> cannotContinue = (orderLine, lotJsonValueList) -> {
    if (lotJsonValueList.isEmpty()) {
      return false;
    }
    return orderLine.getQuantityRequested() < lotJsonValueList.stream()
        .map(LotJsonValue::getQuantityReceived).reduce(Integer::sum).get();
  };

  public StockEntryServiceImpl(CommandeRepository commandeRepository, ProduitService produitService,
      DeliveryReceiptItemRepository deliveryReceiptItemRepository, LotService lotService,
      StorageService storageService, DeliveryReceiptRepository deliveryReceiptRepository,
      FournisseurProduitService fournisseurProduitService, LogsService logsService,
      WarehouseSequenceRepository warehouseSequenceRepository,
      FournisseurRepository fournisseurRepository, OrderLineService orderLineService) {
    this.commandeRepository = commandeRepository;
    this.produitService = produitService;
    this.deliveryReceiptItemRepository = deliveryReceiptItemRepository;
    this.lotService = lotService;
    this.storageService = storageService;
    this.deliveryReceiptRepository = deliveryReceiptRepository;
    this.fournisseurProduitService = fournisseurProduitService;
    this.logsService = logsService;
    this.warehouseSequenceRepository = warehouseSequenceRepository;

    this.fournisseurRepository = fournisseurRepository;
    this.orderLineService = orderLineService;
  }


  private Commande updateCommande(DeliveryReceiptLiteDTO deliveryReceiptLite) {
    Commande commande = commandeRepository.getFirstByOrderRefernce(
        deliveryReceiptLite.getOrderReference()).orElseThrow();
    return commande.setReceiptDate(deliveryReceiptLite.getReceiptDate())
        .setReceiptAmount(deliveryReceiptLite.getReceiptAmount())
        .taxAmount(deliveryReceiptLite.getTaxAmount())
        .setReceiptRefernce(deliveryReceiptLite.getReceiptRefernce())
        .setSequenceBon(deliveryReceiptLite.getSequenceBon())
        .setLastUserEdit(storageService.getUser()).orderStatus(OrderStatut.CLOSED)
        .updatedAt(Instant.now());
  }

  @Override
  public void finalizeSaisieEntreeStock(DeliveryReceiptLiteDTO deliveryReceiptLite) {
    Commande commande = updateCommande(deliveryReceiptLite);
    Set<OrderLine> orderLineSet = commande.getOrderLines();
    DeliveryReceipt deliveryReceipt = this.deliveryReceiptRepository.getReferenceById(
        deliveryReceiptLite.getId());
    // TODO liste des vente en avoir pour envoi possible de notif et de mail
    orderLineSet.forEach(orderLine -> {
      if (isNotEntreeStockIsAuthorize.test(orderLine) && cannotContinue.test(orderLine,
          getLotByOrderLine(orderLine, commande))) {
        throw new GenericError(
            "La reception de certains produits n'a pas ete faite. Veuillez verifier la saisie",
            "commande", "commandeManquante");
      }

      DeliveryReceiptItem deliveryReceiptItem = addItem(orderLine, deliveryReceipt);
      getLotByOrderLine(orderLine, commande).forEach(
          lotJsonValue -> lotService.addLot(lotJsonValue, deliveryReceiptItem,
              deliveryReceipt.getReceiptRefernce()));
      FournisseurProduit fournisseurProduit = updateFournisseurProduit(deliveryReceiptItem);
      StockProduit stockProduit = produitService.updateTotalStock(fournisseurProduit.getProduit(),
          deliveryReceiptItem.getQuantityReceived(), deliveryReceiptItem.getUgQuantity());
      Produit produit = stockProduit.getProduit();

      produit.setPrixMnp(
          produitService.calculPrixMoyenPondereReception(deliveryReceiptItem.getInitStock(),
              orderLine.getGrossAmount(), getTotalStockQuantity(stockProduit),
              deliveryReceiptItem.getOrderCostAmount()));
      produitService.update(produit);
    });
    logsService.create(TransactionType.ENTREE_STOCK, "order.entry",
        new Object[]{deliveryReceipt.getReceiptRefernce()}, deliveryReceipt.getId().toString());
    deliveryReceipt.setReceiptStatut(ReceiptStatut.UNPAID);
    this.deliveryReceiptRepository.saveAndFlush(deliveryReceipt);
    commandeRepository.saveAndFlush(commande);
  }

  @Override
  public DeliveryReceiptLiteDTO createBon(DeliveryReceiptLiteDTO deliveryReceiptLite) {
    DeliveryReceipt deliveryReceipt = new DeliveryReceipt();
    deliveryReceipt.setCreatedDate(LocalDateTime.now());
    deliveryReceipt.setCreatedUser(storageService.getUser());
    Commande commande = commandeRepository.getFirstByOrderRefernce(
        deliveryReceiptLite.getOrderReference()).orElseThrow();
    commande.orderStatus(OrderStatut.RECEIVED);
    deliveryReceipt.setOrderReference(deliveryReceiptLite.getOrderReference());
    deliveryReceipt.setFournisseur(commande.getFournisseur());
    deliveryReceipt.setReceiptStatut(ReceiptStatut.PENDING);
    deliveryReceipt.setNumberTransaction(buildDeliveryReceiptNumberTransaction());
    DeliveryReceiptLiteDTO response = fromEntity(
        createDeliveryReceipt(deliveryReceiptLite, deliveryReceipt));
    commande.getOrderLines().forEach(orderLine -> addItem(orderLine, deliveryReceipt));
    commandeRepository.saveAndFlush(commande);
    return response;
  }

  @Override
  public DeliveryReceiptLiteDTO updateBon(DeliveryReceiptLiteDTO deliveryReceiptLite) {
    DeliveryReceipt deliveryReceipt = this.deliveryReceiptRepository.getReferenceById(
        deliveryReceiptLite.getId());
    return fromEntity(createDeliveryReceipt(deliveryReceiptLite, deliveryReceipt));
  }

  @Override
  public CommandeResponseDTO importNewBon(UploadDeleiveryReceiptDTO uploadDeleiveryReceipt,
      MultipartFile multipartFile) throws IOException {
    String extension = FileUtil.getFileExtension(multipartFile.getOriginalFilename());

    DeliveryReceipt deliveryReceipt = importNewBon(uploadDeleiveryReceipt);
    switch (extension) {
      case FileUtil.CSV:
        return uploadCSVFormat(deliveryReceipt, uploadDeleiveryReceipt.getModel(), multipartFile);

      case FileUtil.TXT:
        return uploadTXTFormat(deliveryReceipt, multipartFile);

      default:
        throw new GenericError(
            String.format("Le modèle ===> %s d'importation de commande n'est pas pris en charche",
                uploadDeleiveryReceipt.getModel().name()), "commande", "modelimportation");
    }

  }

  @Override
  public void updateQuantityUG(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {

    DeliveryReceiptItem receiptItem = this.deliveryReceiptItemRepository.getReferenceById(
        deliveryReceiptItem.getId());
    receiptItem.setUgQuantity(deliveryReceiptItem.getQuantityUG());
    receiptItem.setUpdated(true);
    receiptItem.setUpdatedDate(LocalDateTime.now());
    this.deliveryReceiptItemRepository.save(receiptItem);
  }

  @Override
  public void updateQuantityReceived(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
    DeliveryReceiptItem receiptItem = this.deliveryReceiptItemRepository.getReferenceById(
        deliveryReceiptItem.getId());
    receiptItem.setQuantityReceived(deliveryReceiptItem.getQuantityReceivedTmp());
    receiptItem.setUpdated(true);
    receiptItem.setUpdatedDate(LocalDateTime.now());
    this.deliveryReceiptItemRepository.save(receiptItem);
  }

  @Override
  public void updateOrderUnitPrice(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
    DeliveryReceiptItem receiptItem = this.deliveryReceiptItemRepository.getReferenceById(
        deliveryReceiptItem.getId());
    receiptItem.setOrderUnitPrice(deliveryReceiptItem.getOrderUnitPrice());
    receiptItem.setUpdated(true);
    receiptItem.setUpdatedDate(LocalDateTime.now());
    this.deliveryReceiptItemRepository.save(receiptItem);
  }

  private DeliveryReceiptItem addItem(OrderLine orderLine, DeliveryReceipt deliveryReceipt) {
    FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
    DeliveryReceiptItem receiptItem = new DeliveryReceiptItem();
    receiptItem.setDeliveryReceipt(deliveryReceipt);
    receiptItem.setCreatedDate(deliveryReceipt.getCreatedDate());
    receiptItem.setQuantityReceived(
        Objects.nonNull(orderLine.getQuantityReceived()) ? orderLine.getQuantityReceived()
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
    receiptItem.setCostAmount(orderLine.getCostAmount());
    receiptItem = deliveryReceiptItemRepository.save(receiptItem);
    return receiptItem;
  }

  private List<LotJsonValue> getLotByOrderLine(OrderLine orderLine, Commande commande) {
    if (CollectionUtils.isEmpty(commande.getLots())) {
      return Collections.emptyList();
    }
    return commande.getLots().stream()
        .filter(lotJsonValue -> lotJsonValue.getReceiptItem().compareTo(orderLine.getId()) == 0)
        .toList();
  }

  private String buildDeliveryReceiptNumberTransaction() {
    WarehouseSequence warehouseSequence = warehouseSequenceRepository.getReferenceById(
        EntityConstant.ENTREE_STOCK_SEQUENCE_ID);
    String num = StringUtils.leftPad(warehouseSequence.getValue() + "", EntityConstant.LEFTPAD_SIZE,
        '0');
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
    return stockProduit.getQtyStock() + (Objects.nonNull(stockProduit.getQtyUG())
        ? stockProduit.getQtyStock() : 0);
  }

  private DeliveryReceipt createDeliveryReceipt(DeliveryReceiptLiteDTO deliveryReceiptLite,
      DeliveryReceipt deliveryReceipt) {
    deliveryReceipt.setReceiptDate(deliveryReceiptLite.getReceiptFullDate().toLocalDate());
    deliveryReceipt.setModifiedDate(LocalDateTime.now());
    deliveryReceipt.setModifiedUser(storageService.getUser());
    deliveryReceipt.setReceiptAmount(deliveryReceiptLite.getReceiptAmount());
    deliveryReceipt.setDiscountAmount(0);
    deliveryReceipt.setNetAmount(0);
    deliveryReceipt.setTaxAmount(deliveryReceiptLite.getTaxAmount());
    deliveryReceipt.setReceiptRefernce(deliveryReceiptLite.getReceiptRefernce());
    deliveryReceipt.setSequenceBon(deliveryReceiptLite.getSequenceBon());
    return deliveryReceiptRepository.save(deliveryReceipt);
  }

  private DeliveryReceiptLiteDTO fromEntity(DeliveryReceipt deliveryReceipt) {
    return DeliveryReceiptLiteDTO.builder().id(deliveryReceipt.getId())
        .receiptAmount(deliveryReceipt.getReceiptAmount())
        .receiptDate(deliveryReceipt.getReceiptDate())
        .receiptFullDate(deliveryReceipt.getReceiptDate().atStartOfDay())
        .orderReference(deliveryReceipt.getOrderReference())
        .sequenceBon(deliveryReceipt.getSequenceBon())
        .receiptRefernce(deliveryReceipt.getReceiptRefernce())
        .taxAmount(deliveryReceipt.getTaxAmount()).build();
  }

  private DeliveryReceipt importNewBon(UploadDeleiveryReceiptDTO uploadDeleiveryReceipt) {
    DeliveryReceipt deliveryReceipt = new DeliveryReceipt();
    deliveryReceipt.setCreatedDate(LocalDateTime.now());
    deliveryReceipt.setCreatedUser(storageService.getUser());
    deliveryReceipt.setFournisseur(
        this.fournisseurRepository.getReferenceById(uploadDeleiveryReceipt.getFournisseurId()));
    deliveryReceipt.setReceiptStatut(ReceiptStatut.PENDING);
    deliveryReceipt.setNumberTransaction(buildDeliveryReceiptNumberTransaction());
    createDeliveryReceipt(uploadDeleiveryReceipt.getDeliveryReceipt(), deliveryReceipt);
    return deliveryReceipt;
  }

  private DeliveryReceiptItem createItemFromFile(OrderLine orderLine,
      DeliveryReceipt deliveryReceipt) {
    FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
    DeliveryReceiptItem receiptItem = new DeliveryReceiptItem();
    receiptItem.setDeliveryReceipt(deliveryReceipt);
    receiptItem.setCreatedDate(deliveryReceipt.getCreatedDate());
    receiptItem.setQuantityReceived(
        Objects.nonNull(orderLine.getQuantityReceived()) ? orderLine.getQuantityReceived()
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
    receiptItem.setCostAmount(orderLine.getCostAmount());
    receiptItem = deliveryReceiptItemRepository.save(receiptItem);
    return receiptItem;
  }

  private Optional<DeliveryReceiptItem> findInMap(Map<Long, DeliveryReceiptItem> longOrderLineMap,
      Long fourniseurProduitId) {
    if (longOrderLineMap.containsKey(fourniseurProduitId)) {
      return Optional.of(longOrderLineMap.get(fourniseurProduitId));
    }

    return Optional.empty();
  }

  private void updateReceiptItemFromRecord(DeliveryReceiptItem receiptItem, int quantityReceived,
      int quantityUg, int taxAmount) {
    receiptItem.setUgQuantity(receiptItem.getUgQuantity() + quantityUg);
    receiptItem.setQuantityReceived(receiptItem.getQuantityReceived() + quantityReceived);
    receiptItem.setQuantityRequested(receiptItem.getQuantityReceived());
    receiptItem.setTaxAmount(receiptItem.getTaxAmount() + taxAmount);
  }

  private void updateInRecord(
      /* DeliveryReceipt deliveryReceipt,*/
      DeliveryReceiptItem orderLine, int quantityReceived, int taxAmount,
       /* int oldQty,
        int oldTaxAmount,*/
      int quantitUg) {

    updateReceiptItemFromRecord(orderLine, quantityReceived, quantitUg, taxAmount);

      /*  updateDeliveryReceiptAmountDuringUploading(deliveryReceipt, orderLine, oldQty,
            oldTaxAmount);*/
  }

  private DeliveryReceiptItem buildDeliveryReceiptItemFromRecord(
      FournisseurProduit fournisseurProduit, int quantityRequested, int quantityReceived,
      int orderCostAmount, int orderUnitPrice, int quantityUg, int stock, int taxeAmount,
      DeliveryReceipt deliveryReceipt) {
    DeliveryReceiptItem receiptItem = new DeliveryReceiptItem();
    receiptItem.setUgQuantity(quantityUg);
    receiptItem.setQuantityReceived(quantityReceived);
    receiptItem.setQuantityRequested(quantityRequested);
    receiptItem.setOrderUnitPrice(
        orderUnitPrice > 0 ? orderUnitPrice : fournisseurProduit.getPrixUni());
    receiptItem.setOrderCostAmount(
        orderCostAmount > 0 ? orderCostAmount : fournisseurProduit.getPrixAchat());
    receiptItem.setCostAmount(fournisseurProduit.getPrixAchat());
    receiptItem.setRegularUnitPrice(fournisseurProduit.getPrixUni());
    receiptItem.setInitStock(stock);
    receiptItem.setFournisseurProduit(fournisseurProduit);
    receiptItem.setTaxAmount(taxeAmount);
    deliveryReceipt.addReceiptItem(receiptItem);

    return receiptItem;
  }

  private void createInRecord(Map<Long, DeliveryReceiptItem> longOrderLineMap,
      DeliveryReceipt deliveryReceipt, FournisseurProduit fournisseurProduit, int quantityRequested,
      int quantityReceived, int orderCostAmount, int orderUnitPrice, int quantityUg,
      int currentStock, int taxAmount) {
    try {
      DeliveryReceiptItem orderLineNew = this.deliveryReceiptItemRepository.save(
          buildDeliveryReceiptItemFromRecord(fournisseurProduit, quantityRequested,
              quantityReceived, orderCostAmount, orderUnitPrice, quantityUg, currentStock,
              taxAmount, deliveryReceipt));
      longOrderLineMap.put(fournisseurProduit.getId(), orderLineNew);
    } catch (Exception e) {
      throw e;
    }

  }

  private void updateDeliveryReceiptAmountDuringUploading(DeliveryReceipt deliveryReceipt,
      DeliveryReceiptItem receiptItem, Integer oldQuantityReceived, int oldTaxAmount) {
    deliveryReceipt.setReceiptAmount(
        deliveryReceipt.getReceiptAmount() + (receiptItem.getQuantityReceived()
            * receiptItem.getOrderCostAmount()) - (oldQuantityReceived
            * receiptItem.getOrderCostAmount()));

    deliveryReceipt.setTaxAmount(
        deliveryReceipt.getTaxAmount() + receiptItem.getTaxAmount() - oldTaxAmount);
  }

  private CommandeResponseDTO uploadLaborexModelCSVFormat(DeliveryReceipt deliveryReceipt,
      MultipartFile multipartFile, List<OrderItem> items,
      Map<Long, DeliveryReceiptItem> longOrderLineMap) throws IOException {
    int totalItemCount = 0;
    int succesCount = 0;
    Long fournisseurId = deliveryReceipt.getFournisseur().getId();
    try (CSVParser parser = new CSVParser(new InputStreamReader(multipartFile.getInputStream()),
        CSVFormat.EXCEL.withDelimiter(';'))) {
      for (CSVRecord record : parser) {
        if (totalItemCount > 0) {

          String codeProduit = record.get(2);

          int quantityReceived = Integer.parseInt(record.get(5));
          int orderCostAmount = (int) Double.parseDouble(record.get(6));
          int orderUnitPrice = (int) Double.parseDouble(record.get(7));
          int taxAmount = (int) Double.parseDouble(record.get(9));
          Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
              codeProduit, fournisseurId);
          if (fournisseurProduitOptional.isPresent()) {
            FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
            int currentStock = orderLineService.produitTotalStockWithQantitUg(
                fournisseurProduit.getProduit());

            findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(orderLine -> {
              // int oldQty = orderLine.getQuantityReceived();
              // int oldTaxAmount = orderLine.getTaxAmount();
              updateInRecord(orderLine, quantityReceived, taxAmount, 0);
            }, () -> createInRecord(longOrderLineMap, deliveryReceipt, fournisseurProduit,
                quantityReceived, quantityReceived, orderCostAmount, orderUnitPrice, 0,
                currentStock, taxAmount));
            succesCount++;
          } else {
            items.add(
                new OrderItem().setFacture(record.get(0)).setLigne(Integer.parseInt(record.get(1)))
                    .setProduitCip(codeProduit).setProduitLibelle(record.get(3))
                    .setQuantityRequested(quantityReceived).setQuantityReceived(quantityReceived)
                    .setMontant(Double.parseDouble(record.get(6)))
                    .setPrixUn(Double.parseDouble(record.get(7)))
                    .setReferenceBonLivraison(record.get(8))
                    .setTva(Double.parseDouble(record.get(9))));
          }
        }
        totalItemCount++;
      }

    } catch (IOException e) {
      log.debug("{}", e);
      throw e;
    }
    return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount - 1, succesCount);
  }

  private CommandeResponseDTO uploadCOPHARMEDCSVFormat(DeliveryReceipt deliveryReceipt,
      MultipartFile multipartFile, List<OrderItem> items,
      Map<Long, DeliveryReceiptItem> longOrderLineMap) throws IOException {
    int totalItemCount = 0;
    int succesCount = 0;
    Long fournisseurId = deliveryReceipt.getFournisseur().getId();
    try (CSVParser parser = new CSVParser(new InputStreamReader(multipartFile.getInputStream()),
        CSVFormat.EXCEL.withDelimiter(';'))) {
      for (CSVRecord record : parser) {
        if (totalItemCount > 0) {
          String codeProduit = record.get(4);

          int quantityReceived = Integer.parseInt(record.get(9));
          int orderCostAmount = (int) Double.parseDouble(record.get(11));
          int orderUnitPrice = (int) Double.parseDouble(record.get(13));
          int quantityUg = Integer.parseInt(record.get(10));
          int quantityRequested = Integer.parseInt(record.get(8));
          Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
              codeProduit, fournisseurId);
          if (fournisseurProduitOptional.isPresent()) {
            FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
            int currentStock = orderLineService.produitTotalStockWithQantitUg(
                fournisseurProduit.getProduit());
            findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(orderLine -> {
              updateInRecord(orderLine, quantityReceived, 0, quantityUg);
            }, () -> createInRecord(longOrderLineMap, deliveryReceipt, fournisseurProduit,
                quantityRequested, quantityReceived, orderCostAmount, orderUnitPrice, quantityUg,
                currentStock, 0));
            succesCount++;
          } else {
            items.add(new OrderItem().setFacture(record.get(1)).setDateBonLivraison(record.get(0))
                .setUg(quantityUg).setLigne(Integer.parseInt(record.get(2)))
                .setProduitCip(codeProduit).setProduitLibelle(record.get(6))
                .setQuantityRequested(quantityRequested).setQuantityReceived(quantityReceived)
                .setPrixUn(orderUnitPrice).setPrixAchat(orderCostAmount));
          }
        }
        totalItemCount++;
      }

    } catch (IOException e) {
      log.debug("{}", e);
      throw e;
    }

    return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount - 1, succesCount);
  }

  private CommandeResponseDTO uploadDPCICSVFormat(DeliveryReceipt deliveryReceipt,
      MultipartFile multipartFile, List<OrderItem> items,
      Map<Long, DeliveryReceiptItem> longOrderLineMap) throws IOException {
    int totalItemCount = 0;
    int succesCount = 0;
    Long fournisseurId = deliveryReceipt.getFournisseur().getId();
    try (CSVParser parser = new CSVParser(new InputStreamReader(multipartFile.getInputStream()),
        CSVFormat.EXCEL.withDelimiter(';'))) {
      for (CSVRecord record : parser) {
        String codeProduit = record.get(2);
        totalItemCount++;
        int quantityReceived = Integer.parseInt(record.get(6));
        int orderCostAmount = (int) Double.parseDouble(record.get(3));
        int orderUnitPrice = (int) Double.parseDouble(record.get(4));
        int quantityRequested = Integer.parseInt(record.get(7));
        double taxAmount = Double.valueOf(record.get(5));
        Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
            codeProduit, fournisseurId);
        if (fournisseurProduitOptional.isPresent()) {
          FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
          int currentStock = orderLineService.produitTotalStockWithQantitUg(
              fournisseurProduit.getProduit());
          findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(orderLine -> {

            updateInRecord(

                orderLine, quantityReceived, (int) taxAmount,

                0);
          }, () -> createInRecord(longOrderLineMap, deliveryReceipt, fournisseurProduit,
              quantityRequested, quantityReceived, orderCostAmount, orderUnitPrice, 0, currentStock,
              (int) taxAmount));
          succesCount++;
        } else {
          items.add(new OrderItem().setDateBonLivraison(record.get(8)).setTva(taxAmount)
              .setLigne(Integer.parseInt(record.get(0))).setProduitCip(codeProduit)
              .setProduitLibelle(record.get(1)).setQuantityRequested(quantityRequested)
              .setQuantityReceived(quantityReceived).setPrixUn(orderUnitPrice)
              .setPrixAchat(orderCostAmount));
        }
      }

    } catch (IOException e) {
      log.debug("{}", e);
      throw e;
    }

    return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount, succesCount);
  }

  private CommandeResponseDTO uploadTEDISCSVFormat(DeliveryReceipt deliveryReceipt,
      MultipartFile multipartFile, List<OrderItem> items,
      Map<Long, DeliveryReceiptItem> longOrderLineMap) throws IOException {
    int totalItemCount = 0;
    int succesCount = 0;
    Long fournisseurId = deliveryReceipt.getFournisseur().getId();
    try (CSVParser parser = new CSVParser(new InputStreamReader(multipartFile.getInputStream()),
        CSVFormat.EXCEL.withDelimiter(';'))) {
      for (CSVRecord record : parser) {
        String codeProduit = record.get(0);
        totalItemCount++;
        int quantityReceived = Integer.parseInt(record.get(3));
        int orderCostAmount = Integer.parseInt(record.get(4));

        Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
            codeProduit, fournisseurId);
        if (fournisseurProduitOptional.isPresent()) {
          FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
          int orderUnitPrice = fournisseurProduit.getPrixUni();
          int currentStock = orderLineService.produitTotalStockWithQantitUg(
              fournisseurProduit.getProduit());
          findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(orderLine -> {

            updateInRecord(orderLine, quantityReceived, 0, 0);
          }, () -> createInRecord(longOrderLineMap, deliveryReceipt, fournisseurProduit,
              quantityReceived, quantityReceived, orderCostAmount, orderUnitPrice, 0, currentStock,
              0));
          succesCount++;
        } else {
          items.add(new OrderItem().setDateBonLivraison(record.get(0)).setProduitCip(codeProduit)
              .setProduitEan(codeProduit).setQuantityRequested(Integer.parseInt(record.get(3)))
              .setQuantityReceived(quantityReceived).setMontant(Double.parseDouble(record.get(4))));
        }
      }

    } catch (IOException e) {
      log.debug("{}", e);
      throw e;
    }
    return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount, succesCount);
  }

  private CommandeResponseDTO uploadTXTFormat(DeliveryReceipt deliveryReceipt,
      MultipartFile multipartFile) throws IOException {
    List<OrderItem> items = new ArrayList<>();
    Map<Long, DeliveryReceiptItem> longOrderLineMap = new HashMap<>();
    Long fournisseurId = deliveryReceipt.getFournisseur().getId();
    int totalItemCount = 0;
    int succesCount = 0;
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(multipartFile.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] record = line.split("\t");
        String codeProduit = record[0];
        totalItemCount++;
        int quantityReceived = Integer.parseInt(record[3]);
        int orderCostAmount = Integer.parseInt(record[2]);
        int orderUnitPrice = Integer.parseInt(record[5]);
        Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
            codeProduit, fournisseurId);
        if (fournisseurProduitOptional.isPresent()) {
          FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();

          int currentStock = orderLineService.produitTotalStockWithQantitUg(
              fournisseurProduit.getProduit());
          findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(orderLine -> {

            updateInRecord(orderLine, quantityReceived, 0, 0);
          }, () -> createInRecord(longOrderLineMap, deliveryReceipt, fournisseurProduit,
              quantityReceived, quantityReceived, orderCostAmount, orderUnitPrice, 0, currentStock,
              0));
          succesCount++;
        } else {
          items.add(new OrderItem().setProduitCip(codeProduit).setProduitEan(codeProduit)
              .setQuantityRequested(quantityReceived).setQuantityReceived(quantityReceived)
              .setMontant(Double.valueOf(orderUnitPrice)));
        }
      }

    } catch (IOException e) {
      log.debug("{}", e);
      throw e;
    }

    return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount, succesCount);
  }

  private CommandeResponseDTO buildCommandeResponseDTO(DeliveryReceipt deliveryReceipt,
      List<OrderItem> items, int totalItemCount, int succesCount) {
    return new CommandeResponseDTO().setFailureCount(items.size()).setItems(items)
        .setReference(deliveryReceipt.getReceiptRefernce()).setSuccesCount(succesCount)
        .setTotalItemCount(totalItemCount);
  }

  private CommandeResponseDTO uploadCSVFormat(DeliveryReceipt deliveryReceipt,
      CommandeModel commandeModel, MultipartFile multipartFile) throws IOException {
    CommandeResponseDTO commandeResponseDTO;
    List<OrderItem> items = new ArrayList<>();
    Map<Long, DeliveryReceiptItem> longOrderLineMap = new HashMap<>();

    switch (commandeModel) {
      case LABOREX:
        commandeResponseDTO = uploadLaborexModelCSVFormat(deliveryReceipt, multipartFile, items,
            longOrderLineMap);
        break;
      case COPHARMED:
        commandeResponseDTO = uploadCOPHARMEDCSVFormat(deliveryReceipt, multipartFile, items,
            longOrderLineMap);
        break;
      case DPCI:
        commandeResponseDTO = uploadDPCICSVFormat(deliveryReceipt, multipartFile, items,
            longOrderLineMap);
        break;
      case TEDIS:
        commandeResponseDTO = uploadTEDISCSVFormat(deliveryReceipt, multipartFile, items,
            longOrderLineMap);
        break;
      default:
        throw new GenericError(
            String.format("Le modèle ===> %s d'importation de commande n'est pas pris en charche",
                commandeModel.name()), "commande", "modelimportation");
    }
    return commandeResponseDTO.setEntity(
        fromEntity(this.deliveryReceiptRepository.save(deliveryReceipt)));

  }
}
