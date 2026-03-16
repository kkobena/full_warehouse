package com.kobe.warehouse.service.stock.impl;

import static java.util.Objects.nonNull;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.WarehouseSequence;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.WarehouseSequenceRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptItemLiteDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptLiteDTO;
import com.kobe.warehouse.service.dto.OrderItem;
import com.kobe.warehouse.service.dto.UploadDeleiveryReceiptDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.CommandeIdGeneratorService;
import com.kobe.warehouse.service.id_generator.OrderLineIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.domain.FournisseurProduitPriceHistory;
import com.kobe.warehouse.domain.LotReception;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.repository.FournisseurProduitPriceHistoryRepository;
import com.kobe.warehouse.repository.LotReceptionRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.RetourBonRepository;
import com.kobe.warehouse.service.dto.StockEntryResultDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.ImportationEchoueService;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.StockEntryService;
import com.kobe.warehouse.service.stock.csv.CsvImportStrategy;
import com.kobe.warehouse.service.stock.csv.ParsedCsvRecord;
import com.kobe.warehouse.service.utils.FileUtil;
import com.kobe.warehouse.service.utils.ServiceUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
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
    private final ReferenceService referenceService;
    private final StorageService storageService;
    private final FournisseurProduitService fournisseurProduitService;
    private final LogsService logsService;
    private final WarehouseSequenceRepository warehouseSequenceRepository;
    private final FournisseurRepository fournisseurRepository;
    private final OrderLineService orderLineService;
    private final CommandeIdGeneratorService commandeIdGeneratorService;
    private final OrderLineIdGeneratorService orderLineIdGeneratorService;

    private final ImportationEchoueService importationEchoueService;
    private final InventoryTransactionService inventoryTransactionService;
    private final LotRepository lotRepository;
    private final LotReceptionRepository lotReceptionRepository;
    private final AppConfigurationService appConfigurationService ;
    private final RetourBonRepository retourBonRepository;
    private final FournisseurProduitPriceHistoryRepository priceHistoryRepository;

    private final Predicate<OrderLine> canEntreeStockIsAuthorize2 = orderLine -> {
        if (!BooleanUtils.isTrue(orderLine.getUpdated())) {
            return true;
        }
        // updated=true : quantityReceived doit être renseignée et égale à quantityRequested
        return nonNull(orderLine.getQuantityReceived()) &&
            orderLine.getQuantityReceived().compareTo(orderLine.getQuantityRequested()) == 0;
    };

    private final Predicate<OrderLine> lotPredicate = orderLine -> {
        if (BooleanUtils.isTrue(orderLine.getFournisseurProduit().getProduit().getCheckExpiryDate())) {
            return (
                !CollectionUtils.isEmpty(orderLine.getLots()) &&
                orderLine.getLots().stream().map(Lot::getExpiryDate).allMatch(Objects::nonNull) &&
                orderLine.getLots().stream().mapToInt(Lot::getQuantity).sum() >= orderLine.getQuantityReceived()
            );
        }
        return true;
    };

    private final Predicate<OrderLine> cipNotSet = orderLine ->
        org.springframework.util.StringUtils.hasLength(orderLine.getFournisseurProduit().getCodeCip());


    public StockEntryServiceImpl(
        CommandeRepository commandeRepository,
        ProduitService produitService,
        ReferenceService referenceService,
        StorageService storageService,
        FournisseurProduitService fournisseurProduitService,
        LogsService logsService,
        WarehouseSequenceRepository warehouseSequenceRepository,
        FournisseurRepository fournisseurRepository,
        OrderLineService orderLineService,
        CommandeIdGeneratorService commandeIdGeneratorService,
        OrderLineIdGeneratorService orderLineIdGeneratorService,
        ImportationEchoueService importationEchoueService,
        InventoryTransactionService inventoryTransactionService,
        LotRepository lotRepository,
        LotReceptionRepository lotReceptionRepository,
        AppConfigurationService appConfigurationService,
        RetourBonRepository retourBonRepository,
        FournisseurProduitPriceHistoryRepository priceHistoryRepository
    ) {
        this.commandeRepository = commandeRepository;
        this.produitService = produitService;
        this.referenceService = referenceService;
        this.storageService = storageService;
        this.fournisseurProduitService = fournisseurProduitService;
        this.logsService = logsService;
        this.warehouseSequenceRepository = warehouseSequenceRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.orderLineService = orderLineService;
        this.commandeIdGeneratorService = commandeIdGeneratorService;
        this.orderLineIdGeneratorService = orderLineIdGeneratorService;
        this.importationEchoueService = importationEchoueService;
        this.inventoryTransactionService = inventoryTransactionService;
        this.lotRepository = lotRepository;
        this.lotReceptionRepository = lotReceptionRepository;
        this.appConfigurationService = appConfigurationService;
        this.retourBonRepository = retourBonRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Override
    public StockEntryResultDTO finalizeSaisieEntreeStock(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        return finalizeSaisie(deliveryReceiptLite);
    }

    private Commande getReferenceById(CommandeId id) {
        return commandeRepository.getReferenceById(id);
    }

    private StockEntryResultDTO finalizeSaisie(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        record Pair(Commande origin, Commande cloned) {}
        Commande deliveryReceipt = getReferenceById(deliveryReceiptLite.getCommandeId());
        Pair pair = null;
        if (!deliveryReceipt.getOrderDate().isEqual(LocalDate.now())) {
            pair = new Pair(deliveryReceipt, cloneCommande(deliveryReceipt));
            deliveryReceipt = pair.cloned();
        }
        boolean hasCloned = pair != null;
        LocalDate receiptDate = deliveryReceipt.getReceiptDate();
        // TODO: liste des vente en avoir pour envoi possible de notif et de mail
        deliveryReceipt
            .getOrderLines()
            .forEach(orderLine -> {
                FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
                Produit produit = fournisseurProduit.getProduit();
                if (!cipNotSet.test(orderLine)) {
                    throw new GenericError(
                        String.format(
                            "%s [%s %s ]",
                            "Code cip non renseigné pour ce produit ",
                            produit.getLibelle(),
                            Optional.ofNullable(fournisseurProduit.getCodeCip()).orElse("")
                        ),
                        "codeCipManquant"
                    );
                }
                if (!canEntreeStockIsAuthorize2.test(orderLine)) {
                    throw new GenericError(
                        String.format(
                            "%s produit [%s %s]",
                            "La reception de certains produits n'a pas ete faite. Veuillez verifier la saisie produit ",
                            produit.getLibelle(),
                            fournisseurProduit.getCodeCip()
                        ),
                        "commandeManquante"
                    );
                }
                if (!lotPredicate.test(orderLine)) {
                    throw new GenericError(
                        String.format(
                            "%s [%s %s ]",
                            "Tous les lots ne sont renseignés pour la ligne ",
                            produit.getLibelle(),
                            fournisseurProduit.getCodeCip()
                        ),
                        "lotManquant"
                    );
                }
                if (!isLotMinExpiryValid(orderLine)) {
                    throw new GenericError(
                        String.format(
                            "Un ou plusieurs lots du produit [%s %s] ont une date de péremption inférieure au seuil minimum de %d jours",
                            produit.getLibelle(),
                            fournisseurProduit.getCodeCip(),
                            appConfigurationService.getReceptionMinExpiryDays()
                        ),
                        "lotExpirationTropProche"
                    );
                }
                mergeLots(orderLine, produit, receiptDate);
                updateFournisseurProduit(orderLine, fournisseurProduit, produit);
                saveItem(orderLine);

                StockProduit stockProduit = produitService.updateTotalStock(
                    produit,
                    orderLine.getQuantityReceived(),
                    orderLine.getFreeQty()
                );

                produit.setPrixMnp(
                    produitService.calculPrixMoyenPondereReception(
                        orderLine.getInitStock(),
                        fournisseurProduit.getPrixAchat(),
                        getTotalStockQuantity(stockProduit),
                        orderLine.getOrderCostAmount()
                    )
                );
                produit.setUpdatedAt(LocalDateTime.now());
                produitService.update(produit);
            });
        logsService.create(
            TransactionType.ENTREE_STOCK,
            "order.entry",
            new Object[] { deliveryReceipt.getReceiptReference() },
            deliveryReceipt.getId().getId().toString()
        );
        deliveryReceipt.setOrderStatus(OrderStatut.CLOSED);
        deliveryReceipt.setUpdatedAt(LocalDateTime.now());
        //  Archiver l'original au lieu de le supprimer
        // Évite la FK violation via RetourBonItem.order_line → order_line (cascade delete)
        if (hasCloned) {
            pair.origin.setOrderStatus(OrderStatut.ARCHIVED);
            pair.origin.setUpdatedAt(LocalDateTime.now());
            commandeRepository.save(pair.origin);
        }
        deliveryReceipt = this.commandeRepository.save(deliveryReceipt);
        if (!hasCloned) {
            orderLineService.saveAll(deliveryReceipt.getOrderLines());
        }
        saveLotReceptions(deliveryReceipt, receiptDate);
        inventoryTransactionService.saveAll(deliveryReceipt.getOrderLines());
        // Retours fournisseur en attente liés à cette commande (ou l'originale si clonée)
        Integer sourceCommandeId = hasCloned
            ? pair.origin.getId().getId()
            : deliveryReceipt.getId().getId();
        List<StockEntryResultDTO.PendingRetourBon> pendingRetourBons = retourBonRepository
            .findAllByCommandeId(sourceCommandeId)
            .stream()
            .filter(rb -> rb.getStatut() == RetourStatut.VALIDATED)
            .map(rb -> new StockEntryResultDTO.PendingRetourBon(
                rb.getId(),
                rb.getDateMtv(),
                rb.getCommentaire(),
                rb.getRetourBonItems().size()
            ))
            .toList();
        return new StockEntryResultDTO(deliveryReceipt.getId(), pendingRetourBons);
    }

    private Commande cloneCommande(Commande commande) {
        Commande cloned = new Commande();
        cloned.setOrderDate(LocalDate.now());
        cloned.setId(commandeIdGeneratorService.getNextIdAsInt());
        cloned.setCreatedAt(LocalDateTime.now());
        cloned.setUpdatedAt(cloned.getCreatedAt());
        cloned.setUser(storageService.getUser());
        cloned.setFournisseur(commande.getFournisseur());
        cloned.setOrderReference(commande.getOrderReference());
        cloned.setReceiptDate(commande.getReceiptDate());
        cloned.setGrossAmount(commande.getGrossAmount());
        cloned.setDiscountAmount(commande.getDiscountAmount());
        cloned.setTaxAmount(commande.getTaxAmount());
        cloned.setReceiptReference(commande.getReceiptReference());
        cloned.setHtAmount(commande.getHtAmount());
        cloned.setFinalAmount(commande.getFinalAmount());
        cloned.setOrderStatus(commande.getOrderStatus());
        cloned.setType(commande.getType());
        cloned.setOriginalCommandeId(commande.getId().getId());
        commande.getOrderLines().forEach(orderLine -> cloneOrderLine(orderLine, cloned));
        return cloned;
    }

    private void cloneOrderLine(OrderLine orderLine, Commande commande) {
        OrderLine cloned = new OrderLine();
        cloned.setId(orderLineIdGeneratorService.getNextIdAsInt());
        cloned.setOrderDate(LocalDate.now());
        cloned.setCreatedAt(LocalDateTime.now());
        cloned.setUpdatedAt(cloned.getCreatedAt());
        cloned.setFournisseurProduit(orderLine.getFournisseurProduit());
        cloned.setQuantityRequested(orderLine.getQuantityRequested());
        cloned.setQuantityReceived(orderLine.getQuantityReceived());
        cloned.setOrderCostAmount(orderLine.getOrderCostAmount());
        cloned.setOrderUnitPrice(orderLine.getOrderUnitPrice());
        cloned.setFreeQty(orderLine.getFreeQty());
        cloned.setCommande(commande);
        cloned.setInitStock(orderLine.getInitStock());
        cloned.setFinalStock(orderLine.getFinalStock());
        cloned.setTva(orderLine.getTva());
        cloned.setTaxAmount(orderLine.getTaxAmount());
        orderLine.getLots().forEach(lot -> cloneLot(lot, cloned));
        commande.getOrderLines().add(cloned);
    }

    private void cloneLot(Lot lot, OrderLine orderLine) {
        Lot cloned = new Lot();
        cloned.setCreatedDate(LocalDateTime.now());
        cloned.setUpdated(cloned.getCreatedDate());
        cloned.setNumLot(lot.getNumLot());
        cloned.setQuantity(lot.getQuantity());
        cloned.setFreeQty(lot.getFreeQty());
        cloned.setManufacturingDate(lot.getManufacturingDate());
        cloned.setExpiryDate(lot.getExpiryDate());
        cloned.setPrixAchat(lot.getPrixAchat());
        cloned.setPrixUnit(lot.getPrixUnit());
        cloned.setOrderLine(orderLine);
        orderLine.getLots().add(cloned);
    }

    @Override
    public DeliveryReceiptLiteDTO createBon(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        Commande commande = getReferenceById(deliveryReceiptLite.getCommandeId());
        commande.setUpdatedAt(LocalDateTime.now());
        commande.setUser(storageService.getUser());
        commande.orderStatus(OrderStatut.RECEIVED);
        commande.setReceiptReference(deliveryReceiptLite.getReceiptReference());

        buildDeliveryReceipt(deliveryReceiptLite, commande);
        List<OrderLine> orderLines = commande.getOrderLines();
        orderLines.forEach(this::updateReceivedQty);
        this.orderLineService.saveAll(orderLines);
        return fromEntity(commandeRepository.saveAndFlush(commande));
    }

    @Override
    public DeliveryReceiptLiteDTO updateBon(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        Commande commande = getReferenceById(deliveryReceiptLite.getCommandeId());
        return fromEntity(commandeRepository.save(buildDeliveryReceipt(deliveryReceiptLite, commande)));
    }

    @Override
    public CommandeResponseDTO importNewBon(UploadDeleiveryReceiptDTO uploadDeleiveryReceipt, MultipartFile multipartFile)
        throws IOException {
        String extension = FileUtil.getFileExtension(multipartFile.getOriginalFilename());

        Commande commande = importNewBon(uploadDeleiveryReceipt);
        CommandeResponseDTO commandeResponse =
            switch (extension) {
                case FileUtil.CSV -> uploadCSVFormat(commande, uploadDeleiveryReceipt.getModel(), multipartFile);
                case FileUtil.TXT -> uploadTXTFormat(commande, multipartFile);
                default -> throw new GenericError(
                    String.format(
                        "Le modèle ===> %s d'importation de commande n'est pas pris en charche",
                        uploadDeleiveryReceipt.getModel().name()
                    ),
                    "modelimportation"
                );
            };
        saveLignesBonEchouees(commandeResponse, commande.getId().getId());
        return commandeResponse;
    }

    @Override
    public void updateQuantityUG(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        OrderLine orderLine = getOrderLine(deliveryReceiptItem.getOrderLineId());
        orderLine.setFreeQty(deliveryReceiptItem.getQuantityUG());
        updateItem(orderLine);
    }

    private void updateItem(OrderLine orderLine) {
        orderLine.setUpdated(true);
        orderLine.setUpdatedAt(LocalDateTime.now());
        this.orderLineService.save(orderLine);
    }

    @Override
    public void updateQuantityReceived(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        OrderLine orderLine = getOrderLine(deliveryReceiptItem.getOrderLineId());
        orderLine.setQuantityReceived(deliveryReceiptItem.getQuantityReceivedTmp());
        updateItem(orderLine);
    }

    private OrderLine getOrderLine(OrderLineId id) {
        return this.orderLineService.findOneById(id).orElse(null);
    }

    @Override
    public void updateOrderUnitPrice(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        OrderLine orderLine = getOrderLine(deliveryReceiptItem.getOrderLineId());
        orderLine.setOrderUnitPrice(deliveryReceiptItem.getOrderUnitPrice());
        updateItem(orderLine);
    }

    @Override
    public void updateTva(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        OrderLine orderLine = getOrderLine(deliveryReceiptItem.getOrderLineId());
        orderLine.setTva(new Tva().setId(deliveryReceiptItem.getTvaId()));
        updateItem(orderLine);
    }

    @Override
    public void updateDatePeremption(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        OrderLine orderLine = getOrderLine(deliveryReceiptItem.getOrderLineId());
        //orderLine.setDatePeremption(deliveryReceiptItem.getDatePeremptionTmp());
        updateItem(orderLine);
    }

    private void updateReceivedQty(OrderLine orderLine) {
        orderLine.setQuantityReceived(
            nonNull(orderLine.getQuantityReceived()) ? orderLine.getQuantityReceived() : orderLine.getQuantityRequested()
        );
    }

    private String buildDeliveryReceiptNumberTransaction() {
        WarehouseSequence warehouseSequence = warehouseSequenceRepository.getReferenceById(EntityConstant.ENTREE_STOCK_SEQUENCE_ID);
        String num = StringUtils.leftPad(String.valueOf(warehouseSequence.getValue()), EntityConstant.LEFTPAD_SIZE, '0');
        warehouseSequence.setValue(warehouseSequence.getValue() + warehouseSequence.getIncrement());
        warehouseSequenceRepository.save(warehouseSequence);
        return num;
    }

    private int getTotalStockQuantity(StockProduit stockProduit) {
        return stockProduit.getQtyStock() + Objects.requireNonNullElse(stockProduit.getQtyUG(),0);
    }

    private Commande buildDeliveryReceipt(DeliveryReceiptLiteDTO deliveryReceiptLite, Commande commande) {
        commande.setReceiptDate(deliveryReceiptLite.getReceiptDate());
        commande.setUpdatedAt(LocalDateTime.now());
        commande.setGrossAmount(deliveryReceiptLite.getReceiptAmount());
        commande.setDiscountAmount(0);
        commande.setTaxAmount(deliveryReceiptLite.getTaxAmount());
        commande.setReceiptReference(deliveryReceiptLite.getReceiptReference());
        commande.setHtAmount(deliveryReceiptLite.getReceiptAmount());
        commande.setFinalAmount(commande.getHtAmount());
        return commande;
    }

    private DeliveryReceiptLiteDTO fromEntity(Commande commande) {
        return new DeliveryReceiptLiteDTO()
            .setCommandeId(commande.getId())
            .setId(commande.getId().getId())
            .setHtAmount(commande.getHtAmount())
            .setReceiptAmount(commande.getGrossAmount())
            .setFinalAmount(commande.getFinalAmount())
            .setReceiptDate(commande.getReceiptDate())
            .setReceiptReference(commande.getReceiptReference())
            .setTaxAmount(commande.getTaxAmount());
    }

    private Commande importNewBon(UploadDeleiveryReceiptDTO uploadDeleiveryReceipt) {
        DeliveryReceiptLiteDTO deliveryReceipt = uploadDeleiveryReceipt.getDeliveryReceipt();
        Commande commande = new Commande();
        commande.setId(commandeIdGeneratorService.getNextIdAsInt());
        commande.setOrderDate(LocalDate.now());
        commande.setType(TypeDeliveryReceipt.DIRECT);
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(commande.getCreatedAt());
        commande.setUser(storageService.getUser());
        commande.setFournisseur(this.fournisseurRepository.getReferenceById(uploadDeleiveryReceipt.getFournisseurId()));
        commande.setOrderReference(referenceService.buildNumCommande());
        commande.setReceiptDate(deliveryReceipt.getReceiptDate());
        commande.setUpdatedAt(LocalDateTime.now());
        commande.setGrossAmount(deliveryReceipt.getReceiptAmount());
        commande.setDiscountAmount(0);
        commande.setTaxAmount(deliveryReceipt.getTaxAmount());
        commande.setReceiptReference(deliveryReceipt.getReceiptReference());
        commande.setHtAmount(ServiceUtil.computeHtaxe(commande.getGrossAmount(), commande.getTaxAmount()));
        // deliveryReceipt.setOrderReference(deliveryReceipt.getReceiptReference());
        commandeRepository.save(commande);
        return commande;
    }

    private Optional<OrderLine> findInMap(Map<Integer, OrderLine> longOrderLineMap, Integer fourniseurProduitId) {
        if (longOrderLineMap.containsKey(fourniseurProduitId)) {
            return Optional.of(longOrderLineMap.get(fourniseurProduitId));
        }
        return Optional.empty();
    }

    private void updateReceiptItemFromRecord(OrderLine orderLine, int quantityReceived, int quantityUg, int taxAmount) {
        orderLine.setFreeQty(orderLine.getFreeQty() + quantityUg);
        orderLine.setQuantityReceived(orderLine.getQuantityReceived() + quantityReceived);
        orderLine.setQuantityRequested(orderLine.getQuantityReceived());
        orderLine.setTaxAmount(orderLine.getTaxAmount() + taxAmount);
    }

    private void updateInRecord(
        /* DeliveryReceipt deliveryReceipt,*/
        OrderLine orderLine,
        int quantityReceived,
        int taxAmount,
        /* int oldQty,
      int oldTaxAmount,*/
        int quantitUg
    ) {
        updateReceiptItemFromRecord(orderLine, quantityReceived, quantitUg, taxAmount);
        /*  updateDeliveryReceiptAmountDuringUploading(deliveryReceipt, orderLine, oldQty,
    oldTaxAmount);*/
    }

    private OrderLine buildDeliveryReceiptItemFromRecord(
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
        OrderLine orderLine =
            this.orderLineService.buildDeliveryReceiptItemFromRecord(
                    fournisseurProduit,
                    quantityRequested,
                    quantityReceived,
                    orderCostAmount,
                    orderUnitPrice,
                    quantityUg,
                    stock,
                    taxeAmount,
                    commande
                );

        commande.getOrderLines().add(orderLine);

        return orderLine;
    }

    private void createInRecord(
        Map<Integer, OrderLine> longOrderLineMap,
        Commande deliveryReceipt,
        FournisseurProduit fournisseurProduit,
        int quantityRequested,
        int quantityReceived,
        int orderCostAmount,
        int orderUnitPrice,
        int quantityUg,
        int currentStock,
        int taxAmount
    ) {
        createInRecord(longOrderLineMap, deliveryReceipt, fournisseurProduit,
            quantityRequested, quantityReceived, orderCostAmount, orderUnitPrice,
            quantityUg, currentStock, taxAmount, null, null);
    }

    private void createInRecord(
        Map<Integer, OrderLine> longOrderLineMap,
        Commande deliveryReceipt,
        FournisseurProduit fournisseurProduit,
        int quantityRequested,
        int quantityReceived,
        int orderCostAmount,
        int orderUnitPrice,
        int quantityUg,
        int currentStock,
        int taxAmount,
        String lotNumber,
        LocalDate expirationDate
    ) {
        OrderLine orderLineNew =
            this.orderLineService.save(
                buildDeliveryReceiptItemFromRecord(
                    fournisseurProduit,
                    quantityRequested,
                    quantityReceived,
                    orderCostAmount,
                    orderUnitPrice,
                    quantityUg,
                    currentStock,
                    taxAmount,
                    deliveryReceipt
                )
            );
        buildLot(orderLineNew, quantityReceived, lotNumber, expirationDate, quantityUg);
        longOrderLineMap.put(fournisseurProduit.getId(), orderLineNew);
    }

    private void buildLot(OrderLine orderLine, int quantity, String lotNumber, LocalDate expirationDate, int freeQuantity) {
        if (StringUtils.isNotBlank(lotNumber)) {
            orderLine.getLots().add(
                new Lot()
                    .setOrderLine(orderLine)
                    .setProduit(orderLine.getFournisseurProduit().getProduit())
                    .setNumLot(lotNumber)
                    .setFreeQty(freeQuantity)
                    .setExpiryDate(expirationDate)
                    .setQuantity(quantity)
            );
        }
    }

    private CommandeResponseDTO uploadCSVFormat(Commande commande, CommandeModel commandeModel, MultipartFile multipartFile)
        throws IOException {
        List<OrderItem> items = new ArrayList<>();
        Map<Integer, OrderLine> longOrderLineMap = new HashMap<>();
        CsvImportStrategy strategy = switch (commandeModel) {
            case LABOREX -> CsvImportStrategy.LABOREX;
            case COPHARMED -> CsvImportStrategy.COPHARMED;
            case DPCI -> CsvImportStrategy.DPCI;
            case TEDIS -> CsvImportStrategy.TEDIS;
            case CIP_QTE_PA -> CsvImportStrategy.CIP_QTE_PA;
            case CIP_QTE -> CsvImportStrategy.CIP_QTE;
        };
        CommandeResponseDTO commandeResponseDTO = processCsvUpload(commande, multipartFile, items, longOrderLineMap, strategy);
        return commandeResponseDTO.setEntity(fromEntity(this.commandeRepository.save(commande)));
    }

    private CommandeResponseDTO uploadTXTFormat(Commande commande, MultipartFile multipartFile) throws IOException {
        List<OrderItem> items = new ArrayList<>();
        Map<Integer, OrderLine> longOrderLineMap = new HashMap<>();
        int fournisseurId = commande.getFournisseur().getId();
        int totalItemCount = 0;
        int succesCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(multipartFile.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] re = line.split("\t");
                String codeProduit = re[0];
                totalItemCount++;
                int quantityReceived = Integer.parseInt(re[3]);
                int orderCostAmount = Integer.parseInt(re[2]);
                int orderUnitPrice = Integer.parseInt(re[5]);
                Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
                    codeProduit,
                    fournisseurId
                );
                if (fournisseurProduitOptional.isPresent()) {
                    FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();

                    int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());
                    findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        orderLine -> updateInRecord(orderLine, quantityReceived, 0, 0),
                        () ->
                            createInRecord(
                                longOrderLineMap,
                                commande,
                                fournisseurProduit,
                                quantityReceived,
                                quantityReceived,
                                orderCostAmount,
                                orderUnitPrice,
                                0,
                                currentStock,
                                0
                            )
                    );
                    succesCount++;
                } else {
                    items.add(
                        new OrderItem()
                            .setProduitCip(codeProduit)
                            .setProduitEan(codeProduit)
                            .setQuantityRequested(quantityReceived)
                            .setQuantityReceived(quantityReceived)
                            .setMontant((double) orderUnitPrice)
                    );
                }
            }
        } catch (IOException e) {
            log.debug("{0}", e);
            throw e;
        }

        return buildCommandeResponseDTO(commande, items, totalItemCount, succesCount);
    }

    private CommandeResponseDTO buildCommandeResponseDTO(Commande commande, List<OrderItem> items, int totalItemCount, int succesCount) {
        return new CommandeResponseDTO()
            .setFailureCount(items.size())
            .setItems(items)
            .setReference(commande.getReceiptReference())
            .setSuccesCount(succesCount)
            .setTotalItemCount(totalItemCount);
    }

    private CommandeResponseDTO processCsvUpload(
        Commande commande,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Integer, OrderLine> longOrderLineMap,
        CsvImportStrategy strategy
    ) throws IOException {
        int totalItemCount = 0;
        int succesCount = 0;
        int fournisseurId = commande.getFournisseur().getId();
        CSVFormat csvFormat = CSVFormat.EXCEL.builder().setDelimiter(';').get();

        try (
            Reader reader = new InputStreamReader(multipartFile.getInputStream());
            CSVParser parser = CSVParser.builder().setReader(reader).setFormat(csvFormat).get()
        ) {
            for (CSVRecord csvRecord : parser) {
                Optional<ParsedCsvRecord> parsedOpt = strategy.extract(csvRecord, totalItemCount++);
                if (parsedOpt.isEmpty()) continue;
                ParsedCsvRecord rec = parsedOpt.get();

                Optional<FournisseurProduit> fpOpt = orderLineService.getFournisseurProduitByCriteria(
                    rec.codeProduit(), fournisseurId
                );
                if (fpOpt.isPresent()) {
                    FournisseurProduit fournisseurProduit = fpOpt.get();
                    int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());
                    int orderUnitPrice = rec.orderUnitPrice() > 0 ? rec.orderUnitPrice() : fournisseurProduit.getPrixUni();
                    int orderCostAmount = rec.orderCostAmount() > 0 ? rec.orderCostAmount() : fournisseurProduit.getPrixAchat();

                    findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        orderLine -> {
                            updateInRecord(orderLine, rec.quantityReceived(), rec.taxAmount(), rec.quantityUg());
                            buildLot(orderLine, rec.quantityReceived(), rec.lotNumber(), rec.expirationDate(), rec.quantityUg());
                        },
                        () -> createInRecord(
                            longOrderLineMap, commande, fournisseurProduit,
                            rec.quantityRequested(), rec.quantityReceived(),
                            orderCostAmount, orderUnitPrice,
                            rec.quantityUg(), currentStock, rec.taxAmount(),
                            rec.lotNumber(), rec.expirationDate()
                        )
                    );
                    succesCount++;
                } else {
                    items.add(strategy.onFailure(csvRecord, rec));
                }
            }
        } catch (IOException e) {
            log.debug("{0}", e);
            throw e;
        }
        return buildCommandeResponseDTO(commande, items, succesCount + items.size(), succesCount);
    }

    private void saveLignesBonEchouees(CommandeResponseDTO commandeResponse, Integer deliveryReceiptId) {
        if (Objects.isNull(commandeResponse) || commandeResponse.getItems().isEmpty()) {
            return;
        }
        this.importationEchoueService.save(deliveryReceiptId, false, commandeResponse.getItems());
    }

    private void saveItem(OrderLine orderLine) {
        FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLine.setQuantityReceived(
            nonNull(orderLine.getQuantityReceived()) ? orderLine.getQuantityReceived() : orderLine.getQuantityRequested()
        );
        orderLine.setInitStock(produitService.getProductTotalStock(fournisseurProduit.getProduit().getId()));
        orderLine.setFinalStock(orderLine.getInitStock() + (orderLine.getQuantityReceived() + orderLine.getFreeQty()));
        Produit produit = fournisseurProduit.getProduit();
        if (orderLine.getTva() != null) {
            produit.setTva(orderLine.getTva());
            produitService.update(produit);
        }
        //  orderLineService.save(orderLine);
    }

    private void mergeLots(OrderLine orderLine, Produit produit, LocalDate receiptDate) {
        if (CollectionUtils.isEmpty(orderLine.getLots())) {
            return;
        }
        List<Lot> lotsToRemove = new ArrayList<>();
        orderLine.getLots().forEach(lot ->
            lotRepository.findByNumLotAndProduitId(lot.getNumLot(), produit.getId())
                .ifPresent(existingLot -> {
                    existingLot.setQuantity(existingLot.getQuantity() + lot.getQuantity());
                    existingLot.setCurrentQuantity(existingLot.getCurrentQuantity() + lot.getQuantity());
                    existingLot.setFreeQty(existingLot.getFreeQty() + lot.getFreeQty());
                    existingLot.setStatut(StatutLot.AVAILABLE);
                    existingLot.setUpdated(LocalDateTime.now());
                    lotRepository.save(existingLot);
                    lotReceptionRepository.save(buildLotReception(existingLot, orderLine, lot.getQuantity(), lot.getFreeQty(), lot.getPrixAchat(), receiptDate));
                    lotsToRemove.add(lot);
                })
        );
        orderLine.getLots().removeAll(lotsToRemove);
    }

    /**
     * Sauvegarde les LotReception pour les lots nouvellement créés (après flush en cascade).
     * Les lots restants dans orderLine.lots sont des nouveaux lots qui viennent d'être persistés
     * et ont maintenant un ID généré par la base.
     */
    private void saveLotReceptions(Commande commande, LocalDate receiptDate) {
        commande.getOrderLines().forEach(orderLine ->
            orderLine.getLots().forEach(lot ->
                lotReceptionRepository.save(
                    buildLotReception(lot, orderLine, lot.getQuantity(), lot.getFreeQty(), lot.getPrixAchat(), receiptDate)
                )
            )
        );
    }

    private boolean isLotMinExpiryValid(OrderLine orderLine) {
        if (!BooleanUtils.isTrue(orderLine.getFournisseurProduit().getProduit().getCheckExpiryDate())) {
            return true;
        }
        if (CollectionUtils.isEmpty(orderLine.getLots())) {
            return true;
        }
        LocalDate minAcceptableDate = LocalDate.now().plusDays(appConfigurationService.getReceptionMinExpiryDays());
        return orderLine.getLots().stream()
            .filter(lot -> lot.getExpiryDate() != null)
            .noneMatch(lot -> lot.getExpiryDate().isBefore(minAcceptableDate));
    }

    private LotReception buildLotReception(Lot lot, OrderLine orderLine, int quantityReceived, int freeQty, Integer prixAchat, LocalDate receiptDate) {
        return new LotReception()
            .setLot(lot)
            .setOrderLine(orderLine)
            .setQuantityReceived(quantityReceived)
            .setFreeQty(freeQty)
            .setPrixAchat(prixAchat != null ? prixAchat : 0)
            .setReceiptDate(receiptDate)
            .setCreatedAt(LocalDateTime.now());
    }

    private void updateFournisseurProduit(OrderLine orderLine, FournisseurProduit fournisseurProduit, Produit produit) {
        int montantAdditionel = produit.getTableau() != null ? produit.getTableau().getValue() : 0;
        boolean prixAchatChanged = orderLine.getOrderCostAmount().compareTo(fournisseurProduit.getPrixAchat()) != 0;
        boolean prixUniChanged = (orderLine.getOrderUnitPrice() + montantAdditionel) != (fournisseurProduit.getPrixUni() + montantAdditionel);

        if (prixAchatChanged || prixUniChanged) {
            // : Enregistrer l'historique avant modification
            priceHistoryRepository.save(
                new FournisseurProduitPriceHistory()
                    .setFournisseurProduit(fournisseurProduit)
                    .setOldPrixAchat(fournisseurProduit.getPrixAchat())
                    .setNewPrixAchat(prixAchatChanged ? orderLine.getOrderCostAmount() : fournisseurProduit.getPrixAchat())
                    .setOldPrixUni(fournisseurProduit.getPrixUni())
                    .setNewPrixUni(prixUniChanged ? orderLine.getOrderUnitPrice() : fournisseurProduit.getPrixUni())
                    .setChangedAt(LocalDateTime.now())
                    .setChangedBy(storageService.getUser())
                    .setCommandeId(orderLine.getCommande().getId().getId())
                    .setReceiptReference(orderLine.getCommande().getReceiptReference())
            );
            if (prixAchatChanged) {
                fournisseurProduit.setPrixAchat(orderLine.getOrderCostAmount());
            }
            if (prixUniChanged) {
                fournisseurProduit.setPrixUni(orderLine.getOrderUnitPrice());
            }
        }

        fournisseurProduitService.update(fournisseurProduit);
    }


}
