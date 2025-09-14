package com.kobe.warehouse.service.stock.impl;

import static java.util.Objects.nonNull;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.WarehouseSequence;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.WarehouseSequenceRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptItemLiteDTO;
import com.kobe.warehouse.service.dto.DeliveryReceiptLiteDTO;
import com.kobe.warehouse.service.dto.OrderItem;
import com.kobe.warehouse.service.dto.UploadDeleiveryReceiptDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.stock.ImportationEchoueService;
import com.kobe.warehouse.service.stock.StockEntryService;
import com.kobe.warehouse.service.utils.FileUtil;
import com.kobe.warehouse.service.utils.ServiceUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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

    private final ImportationEchoueService importationEchoueService;
    private final InventoryTransactionService inventoryTransactionService;

    private final Predicate<OrderLine> canEntreeStockIsAuthorize2 = orderLine -> {
        if (BooleanUtils.isTrue(orderLine.getUpdated()) && nonNull(orderLine.getQuantityReceived())) {
            return (orderLine.getQuantityReceived().compareTo(orderLine.getQuantityRequested()) == 0);
        }
        return true;
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
        ImportationEchoueService importationEchoueService,
        InventoryTransactionService inventoryTransactionService
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
        this.importationEchoueService = importationEchoueService;
        this.inventoryTransactionService = inventoryTransactionService;
    }

    @Override
    public void finalizeSaisieEntreeStock(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        this.commandeRepository.saveAndFlush(finalizeSaisie(deliveryReceiptLite));
    }

    private Commande getReferenceById(Long id) {
        return commandeRepository.findCommandeById(id);
    }

    private Commande finalizeSaisie(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        Commande deliveryReceipt = getReferenceById(deliveryReceiptLite.getId());
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
                inventoryTransactionService.save(orderLine);
            });
        logsService.create(
            TransactionType.ENTREE_STOCK,
            "order.entry",
            new Object[] { deliveryReceipt.getReceiptReference() },
            deliveryReceipt.getId().toString()
        );
        deliveryReceipt.setOrderStatus(OrderStatut.CLOSED);

        deliveryReceipt.setUpdatedAt(LocalDateTime.now());

        return deliveryReceipt;
    }

    @Override
    public DeliveryReceiptLiteDTO createBon(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        Commande commande = getReferenceById(deliveryReceiptLite.getId());
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
        Commande commande = getReferenceById(deliveryReceiptLite.getId());
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
        OrderLine orderLine = getOrderLine(deliveryReceiptItem.getId());
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
        OrderLine orderLine = getOrderLine(deliveryReceiptItem.getId());
        orderLine.setQuantityReceived(deliveryReceiptItem.getQuantityReceivedTmp());
        updateItem(orderLine);
    }

    private OrderLine getOrderLine(Long id) {
        return this.orderLineService.findOneById(id).orElse(null);
    }

    @Override
    public void updateOrderUnitPrice(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        OrderLine orderLine = getOrderLine(deliveryReceiptItem.getId());
        orderLine.setOrderUnitPrice(deliveryReceiptItem.getOrderUnitPrice());
        updateItem(orderLine);
    }

    @Override
    public void updateTva(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        OrderLine orderLine = getOrderLine(deliveryReceiptItem.getId());
        orderLine.setTva(new Tva().setId(deliveryReceiptItem.getTvaId()));
        updateItem(orderLine);
    }

    @Override
    public void updateDatePeremption(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        OrderLine orderLine = getOrderLine(deliveryReceiptItem.getId());
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
        return stockProduit.getQtyStock() + (nonNull(stockProduit.getQtyUG()) ? stockProduit.getQtyStock() : 0);
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

    private Optional<OrderLine> findInMap(Map<Long, OrderLine> longOrderLineMap, Long fourniseurProduitId) {
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
        Map<Long, OrderLine> longOrderLineMap,
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
        longOrderLineMap.put(fournisseurProduit.getId(), orderLineNew);
    }

    private CommandeResponseDTO uploadCSVFormat(Commande commande, CommandeModel commandeModel, MultipartFile multipartFile)
        throws IOException {
        CommandeResponseDTO commandeResponseDTO;
        List<OrderItem> items = new ArrayList<>();
        Map<Long, OrderLine> longOrderLineMap = new HashMap<>();

        commandeResponseDTO = switch (commandeModel) {
            case LABOREX -> processCsvUpload(commande, multipartFile, items, longOrderLineMap, this::parseLaborexRecord);
            case COPHARMED -> processCsvUpload(commande, multipartFile, items, longOrderLineMap, this::parseCopharmedRecord);
            case DPCI -> processCsvUpload(commande, multipartFile, items, longOrderLineMap, this::parseDpciRecord);
            case TEDIS -> processCsvUpload(commande, multipartFile, items, longOrderLineMap, this::parseTedisRecord);
            case CIP_QTE_PA -> processCsvUpload(commande, multipartFile, items, longOrderLineMap, this::parseCipQtePrixAchatRecord);
            case CIP_QTE -> processCsvUpload(commande, multipartFile, items, longOrderLineMap, this::parseCipQteRecord);
        };
        assert commandeResponseDTO != null;
        return commandeResponseDTO.setEntity(fromEntity(this.commandeRepository.save(commande)));
    }

    private CommandeResponseDTO uploadTXTFormat(Commande commande, MultipartFile multipartFile) throws IOException {
        List<OrderItem> items = new ArrayList<>();
        Map<Long, OrderLine> longOrderLineMap = new HashMap<>();
        Long fournisseurId = commande.getFournisseur().getId();
        int totalItemCount = 0;
        int succesCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(multipartFile.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] record = line.split("\t");
                String codeProduit = record[0];
                totalItemCount++;
                int quantityReceived = Integer.parseInt(record[3]);
                int orderCostAmount = Integer.parseInt(record[2]);
                int orderUnitPrice = Integer.parseInt(record[5]);
                Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
                    codeProduit,
                    fournisseurId
                );
                if (fournisseurProduitOptional.isPresent()) {
                    FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();

                    int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());
                    findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        orderLine -> {
                            updateInRecord(orderLine, quantityReceived, 0, 0);
                        },
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
        Map<Long, OrderLine> longOrderLineMap,
        java.util.function.BiFunction<CSVRecord, Integer, OrderItem> recordParser
    ) throws IOException {
        int totalItemCount = 0;
        int succesCount = 0;
        long fournisseurId = commande.getFournisseur().getId();

        try (
            CSVParser parser = new CSVParser(
                new InputStreamReader(multipartFile.getInputStream()),
                CSVFormat.EXCEL.builder().setDelimiter(';').build()
            )
        ) {
            for (CSVRecord record : parser) {
                OrderItem orderItem = recordParser.apply(record, totalItemCount);
                totalItemCount++;
                if (orderItem == null) { // Skip header
                    continue;
                }

                Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
                    orderItem.getProduitCip(),
                    fournisseurId
                );

                if (fournisseurProduitOptional.isPresent()) {
                    FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
                    int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());

                    findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        orderLine ->
                            updateInRecord(
                                orderLine,
                                orderItem.getQuantityReceived(),
                                nonNull(orderItem.getTva()) ? orderItem.getTva().intValue() : 0,
                                orderItem.getUg()
                            ),
                        () -> {
                            int orderUnitPrice = orderItem.getPrixUn() > 0 ? (int) orderItem.getPrixUn() : fournisseurProduit.getPrixUni();
                            int orderCostAmount = orderItem.getPrixAchat() > 0
                                ? orderItem.getPrixAchat()
                                : fournisseurProduit.getPrixAchat();

                            createInRecord(
                                longOrderLineMap,
                                commande,
                                fournisseurProduit,
                                orderItem.getQuantityRequested(),
                                orderItem.getQuantityReceived(),
                                orderCostAmount,
                                orderUnitPrice,
                                orderItem.getUg(),
                                currentStock,
                                nonNull(orderItem.getTva()) ? orderItem.getTva().intValue() : 0
                            );
                        }
                    );
                    succesCount++;
                } else {
                    items.add(orderItem);
                }
            }
        } catch (IOException e) {
            log.debug("{0}", e);
            throw e;
        }
        return buildCommandeResponseDTO(commande, items, succesCount + items.size(), succesCount);
    }

    private OrderItem parseLaborexRecord(CSVRecord record, int index) {
        if (index == 0) {
            return null;
        }
        String codeProduit = record.get(3);
        int quantityReceived = Integer.parseInt(record.get(7));
        int orderCostAmount = (int) Double.parseDouble(record.get(8));
        int orderUnitPrice = (int) Double.parseDouble(record.get(9));
        int taxAmount = (int) Double.parseDouble(record.get(11));
        int quantityUg = Integer.parseInt(record.get(6));
        OrderItem item = new OrderItem()
            .setProduitCip(codeProduit)
            .setQuantityReceived(quantityReceived)
            .setQuantityRequested(quantityReceived)
            .setPrixAchat(orderCostAmount)
            .setPrixUn(orderUnitPrice)
            .setTva((double) taxAmount)
            .setUg(quantityUg);
        item
            .setEtablissement(record.get(0))
            .setFacture(record.get(1))
            .setLigne(Integer.parseInt(record.get(2)))
            .setProduitLibelle(record.get(4))
            .setMontant((double) orderCostAmount)
            .setReferenceBonLivraison(record.get(10))
            .setTva(Double.parseDouble(record.get(11)));
        return item;
    }

    private OrderItem parseCopharmedRecord(CSVRecord record, int index) {
        if (index == 0) {
            return null;
        }
        String codeProduit = record.get(4);
        int quantityReceived = Integer.parseInt(record.get(9));
        int orderCostAmount = (int) Double.parseDouble(record.get(11));
        int orderUnitPrice = (int) Double.parseDouble(record.get(13));
        int quantityUg = Integer.parseInt(record.get(10));
        int quantityRequested = Integer.parseInt(record.get(8));
        OrderItem item = new OrderItem()
            .setProduitCip(codeProduit)
            .setQuantityReceived(quantityReceived)
            .setQuantityRequested(quantityRequested)
            .setPrixAchat(orderCostAmount)
            .setPrixUn(orderUnitPrice)
            .setUg(quantityUg);
        item
            .setFacture(record.get(1))
            .setDateBonLivraison(record.get(0))
            .setLigne(Integer.parseInt(record.get(2)))
            .setProduitLibelle(record.get(6));
        return item;
    }

    private OrderItem parseDpciRecord(CSVRecord record, int index) {
        String codeProduit = record.get(2);
        int quantityReceived = Integer.parseInt(record.get(6));
        int orderCostAmount = (int) Double.parseDouble(record.get(3));
        int orderUnitPrice = (int) Double.parseDouble(record.get(4));
        int quantityRequested = Integer.parseInt(record.get(7));
        double taxAmount = Double.parseDouble(record.get(5));
        OrderItem item = new OrderItem()
            .setProduitCip(codeProduit)
            .setQuantityReceived(quantityReceived)
            .setQuantityRequested(quantityRequested)
            .setPrixAchat(orderCostAmount)
            .setPrixUn(orderUnitPrice)
            .setTva(taxAmount);
        item
            .setReferenceBonLivraison(record.get(8))
            .setTva(taxAmount)
            .setLigne(Integer.parseInt(record.get(0)))
            .setProduitLibelle(record.get(1));
        return item;
    }

    private OrderItem parseTedisRecord(CSVRecord record, int index) {
        String codeProduit = record.get(1);
        int quantityReceived = new BigDecimal(record.get(3)).intValue();
        int orderCostAmount = new BigDecimal(record.get(2)).intValue();
        int orderUnitPrice = new BigDecimal(record.get(5)).intValue();
        OrderItem item = new OrderItem()
            .setProduitCip(codeProduit)
            .setQuantityReceived(quantityReceived)
            .setQuantityRequested(quantityReceived)
            .setPrixAchat(orderCostAmount)
            .setPrixUn(orderUnitPrice);
        item.setLigne(Integer.parseInt(record.get(0))).setProduitEan(codeProduit);
        return item;
    }

    private OrderItem parseCipQteRecord(CSVRecord record, int index) {
        if (index == 0) {
            try {
                Integer.parseInt(record.get(1));
            } catch (Exception e) {
                return null; // Skip header
            }
        }
        String codeProduit = record.get(0);
        int quantityReceived = Integer.parseInt(record.get(1));
        return new OrderItem().setProduitCip(codeProduit).setQuantityReceived(quantityReceived).setQuantityRequested(quantityReceived);
    }

    private OrderItem parseCipQtePrixAchatRecord(CSVRecord record, int index) {
        if (index == 0) {
            try {
                Integer.parseInt(record.get(1));
            } catch (Exception e) {
                return null; // Skip header
            }
        }
        String codeProduit = record.get(0);
        int quantityReceived = Integer.parseInt(record.get(3));
        int prixAchat = Integer.parseInt(record.get(4));
        OrderItem item = new OrderItem()
            .setProduitCip(codeProduit)
            .setQuantityReceived(quantityReceived)
            .setQuantityRequested(quantityReceived)
            .setPrixAchat(prixAchat);
        item.setQuantityRequested(Integer.parseInt(record.get(1)));
        return item;
    }

    private void saveLignesBonEchouees(CommandeResponseDTO commandeResponse, Long deliveryReceiptId) {
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

        orderLineService.save(orderLine);
    }

    private void updateFournisseurProduit(OrderLine orderLine, FournisseurProduit fournisseurProduit, Produit produit) {
        int montantAdditionel = produit.getTableau() != null ? produit.getTableau().getValue() : 0;
        if (orderLine.getOrderCostAmount().compareTo(fournisseurProduit.getPrixAchat()) != 0) {
            fournisseurProduit.setPrixAchat(orderLine.getOrderCostAmount());
        }
        if ((orderLine.getOrderUnitPrice() + montantAdditionel) != (fournisseurProduit.getPrixUni() + montantAdditionel)) {
            fournisseurProduit.setPrixUni(orderLine.getOrderUnitPrice());
        }

        fournisseurProduitService.update(fournisseurProduit);
    }

    private int skipFirstLigne(CSVRecord cSVRecord, int index) {
        if (index < 1) {
            try {
                return Integer.parseInt(cSVRecord.get(1));
            } catch (Exception e) {
                return -1;
            }
        }
        return 0;
    }
}
