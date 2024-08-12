package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.DeliveryReceipt;
import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.ProductState;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.WarehouseSequence;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.DeliveryReceiptItemRepository;
import com.kobe.warehouse.repository.DeliveryReceiptRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.WarehouseSequenceRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.ProductStateService;
import com.kobe.warehouse.service.ProduitService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.WarehouseCalendarService;
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
import com.kobe.warehouse.service.utils.ServiceUtil;
import com.kobe.warehouse.web.rest.errors.GenericError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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

    private final DeliveryReceiptItemRepository deliveryReceiptItemRepository;
    private final LotService lotService;
    private final StorageService storageService;
    private final DeliveryReceiptRepository deliveryReceiptRepository;
    private final FournisseurProduitService fournisseurProduitService;
    private final LogsService logsService;
    private final WarehouseSequenceRepository warehouseSequenceRepository;
    private final FournisseurRepository fournisseurRepository;
    private final OrderLineService orderLineService;
    private final ProductStateService productStateService;
    private final WarehouseCalendarService warehouseCalendarService;
    private final Predicate<OrderLine> isNotEntreeStockIsAuthorize = orderLine -> {
        if (Objects.nonNull(orderLine.getReceiptDate()) && Objects.nonNull(orderLine.getQuantityReceived())) {
            return (
                (orderLine.getQuantityReceived() < orderLine.getQuantityRequested()) &&
                orderLine.getFournisseurProduit().getProduit().getCheckExpiryDate()
            );
        }
        return false;
    };
    private final Predicate<DeliveryReceiptItem> canEntreeStockIsAuthorize2 = deliveryReceiptItem -> {
        if (BooleanUtils.isTrue(deliveryReceiptItem.getUpdated()) && Objects.nonNull(deliveryReceiptItem.getQuantityReceived())) {
            return (deliveryReceiptItem.getQuantityReceived().compareTo(deliveryReceiptItem.getQuantityRequested()) == 0);
        }
        return true;
    };
    private final Predicate<DeliveryReceiptItem> lotPredicate = deliveryReceiptItem -> {
        if (BooleanUtils.isTrue(deliveryReceiptItem.getFournisseurProduit().getProduit().getCheckExpiryDate())) {
            return (
                !CollectionUtils.isEmpty(deliveryReceiptItem.getLots()) &&
                deliveryReceiptItem.getLots().stream().map(Lot::getExpiryDate).allMatch(Objects::nonNull) &&
                deliveryReceiptItem.getLots().stream().mapToInt(Lot::getQuantityReceived).sum() == deliveryReceiptItem.getQuantityReceived()
            );
        }
        return true;
    };
    private final Predicate<DeliveryReceiptItem> cipNotSet = deliveryReceiptItem ->
        org.springframework.util.StringUtils.hasLength(deliveryReceiptItem.getFournisseurProduit().getCodeCip());
    private final BiPredicate<OrderLine, List<LotJsonValue>> cannotContinue = (orderLine, lotJsonValueList) -> {
        if (lotJsonValueList.isEmpty()) {
            return false;
        }
        return (
            orderLine.getQuantityRequested() < lotJsonValueList.stream().map(LotJsonValue::getQuantityReceived).reduce(Integer::sum).get()
        );
    };

    public StockEntryServiceImpl(
        CommandeRepository commandeRepository,
        ProduitService produitService,
        DeliveryReceiptItemRepository deliveryReceiptItemRepository,
        LotService lotService,
        StorageService storageService,
        DeliveryReceiptRepository deliveryReceiptRepository,
        FournisseurProduitService fournisseurProduitService,
        LogsService logsService,
        WarehouseSequenceRepository warehouseSequenceRepository,
        FournisseurRepository fournisseurRepository,
        OrderLineService orderLineService,
        ProductStateService productStateService,
        WarehouseCalendarService warehouseCalendarService
    ) {
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
        this.productStateService = productStateService;
        this.warehouseCalendarService = warehouseCalendarService;
    }

    private Commande updateCommande(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        Optional<Commande> commandeOp = commandeRepository.getFirstByOrderRefernce(deliveryReceiptLite.getOrderReference());
        return commandeOp
            .map(
                commande ->
                    commande
                        .setReceiptDate(deliveryReceiptLite.getReceiptDate())
                        .setReceiptAmount(deliveryReceiptLite.getReceiptAmount())
                        .taxAmount(deliveryReceiptLite.getTaxAmount())
                        .setReceiptRefernce(deliveryReceiptLite.getReceiptRefernce())
                        .setSequenceBon(deliveryReceiptLite.getSequenceBon())
                        .setLastUserEdit(storageService.getUser())
                        .orderStatus(OrderStatut.CLOSED)
                        .updatedAt(LocalDateTime.now())
            )
            .orElse(null);
    }

    @Override
    public void finalizeSaisieEntreeStock(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        Commande commande = updateCommande(deliveryReceiptLite);
        DeliveryReceipt deliveryReceipt = finalizeSaisie(deliveryReceiptLite);

        if (Objects.nonNull(commande)) {
            deliveryReceipt.setOrderReference(commande.getOrderRefernce());
            this.commandeRepository.saveAndFlush(commande);
        }
        this.deliveryReceiptRepository.saveAndFlush(deliveryReceipt);
    }

    private DeliveryReceipt finalizeSaisie(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        DeliveryReceipt deliveryReceipt = this.deliveryReceiptRepository.getReferenceById(deliveryReceiptLite.getId());
        // TODO: liste des vente en avoir pour envoi possible de notif et de mail
        deliveryReceipt
            .getReceiptItems()
            .forEach(deliveryReceiptItem -> {
                FournisseurProduit fournisseurProduit = deliveryReceiptItem.getFournisseurProduit();
                Produit produit = fournisseurProduit.getProduit();
                if (!cipNotSet.test(deliveryReceiptItem)) {
                    throw new GenericError(
                        String.format(
                            "%s [%s %s ]",
                            "Code cip non renseigné pour ce produit ",
                            produit.getLibelle(),
                            Optional.ofNullable(produit.getCodeEan()).orElse("")
                        ),
                        "codeCipManquant"
                    );
                }
                if (!canEntreeStockIsAuthorize2.test(deliveryReceiptItem)) {
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
                if (!lotPredicate.test(deliveryReceiptItem)) {
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
                updateFournisseurProduit(deliveryReceiptItem, fournisseurProduit, produit);
                saveItem(deliveryReceiptItem);

                StockProduit stockProduit = produitService.updateTotalStock(
                    produit,
                    deliveryReceiptItem.getQuantityReceived(),
                    deliveryReceiptItem.getUgQuantity()
                );

                produit.setPrixMnp(
                    produitService.calculPrixMoyenPondereReception(
                        deliveryReceiptItem.getInitStock(),
                        deliveryReceiptItem.getCostAmount(),
                        getTotalStockQuantity(stockProduit),
                        deliveryReceiptItem.getOrderCostAmount()
                    )
                );
                produit.setUpdatedAt(LocalDateTime.now());
                updateProductState(produit);
                produitService.update(produit);
            });
        logsService.create(
            TransactionType.ENTREE_STOCK,
            "order.entry",
            new Object[] { deliveryReceipt.getReceiptRefernce() },
            deliveryReceipt.getId().toString()
        );
        deliveryReceipt.setReceiptStatut(ReceiptStatut.CLOSE);
        deliveryReceipt.setModifiedUser(storageService.getUser());
        deliveryReceipt.setModifiedDate(LocalDateTime.now());

        return deliveryReceipt;
    }

    @Override
    public DeliveryReceiptLiteDTO createBon(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        DeliveryReceipt deliveryReceipt = new DeliveryReceipt();
        deliveryReceipt.setCalendar(warehouseCalendarService.initCalendar());
        deliveryReceipt.setCreatedDate(LocalDateTime.now());
        deliveryReceipt.setCreatedUser(storageService.getUser());

        Commande commande = commandeRepository.getFirstByOrderRefernce(deliveryReceiptLite.getOrderReference()).orElseThrow();
        commande.orderStatus(OrderStatut.RECEIVED);
        deliveryReceipt.setOrderReference(deliveryReceiptLite.getOrderReference());
        deliveryReceipt.setFournisseur(commande.getFournisseur());
        deliveryReceipt.setReceiptStatut(ReceiptStatut.PENDING);
        deliveryReceipt.setNumberTransaction(buildDeliveryReceiptNumberTransaction());
        DeliveryReceiptLiteDTO response = fromEntity(
            deliveryReceiptRepository.save(buildDeliveryReceipt(deliveryReceiptLite, deliveryReceipt))
        );
        commande.getOrderLines().forEach(orderLine -> addItem(orderLine, deliveryReceipt));
        commandeRepository.saveAndFlush(commande);
        return response;
    }

    @Override
    public DeliveryReceiptLiteDTO updateBon(DeliveryReceiptLiteDTO deliveryReceiptLite) {
        DeliveryReceipt deliveryReceipt = this.deliveryReceiptRepository.getReferenceById(deliveryReceiptLite.getId());
        return fromEntity(deliveryReceiptRepository.save(buildDeliveryReceipt(deliveryReceiptLite, deliveryReceipt)));
    }

    @Override
    public CommandeResponseDTO importNewBon(UploadDeleiveryReceiptDTO uploadDeleiveryReceipt, MultipartFile multipartFile)
        throws IOException {
        String extension = FileUtil.getFileExtension(multipartFile.getOriginalFilename());

        DeliveryReceipt deliveryReceipt = importNewBon(uploadDeleiveryReceipt);
        return switch (extension) {
            case FileUtil.CSV -> uploadCSVFormat(deliveryReceipt, uploadDeleiveryReceipt.getModel(), multipartFile);
            case FileUtil.TXT -> uploadTXTFormat(deliveryReceipt, multipartFile);
            default -> throw new GenericError(
                String.format(
                    "Le modèle ===> %s d'importation de commande n'est pas pris en charche",
                    uploadDeleiveryReceipt.getModel().name()
                ),
                "modelimportation"
            );
        };
    }

    @Override
    public void updateQuantityUG(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        DeliveryReceiptItem receiptItem = this.deliveryReceiptItemRepository.getReferenceById(deliveryReceiptItem.getId());
        receiptItem.setUgQuantity(deliveryReceiptItem.getQuantityUG());
        receiptItem.setUpdated(true);
        receiptItem.setUpdatedDate(LocalDateTime.now());
        this.deliveryReceiptItemRepository.save(receiptItem);
    }

    @Override
    public void updateQuantityReceived(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        DeliveryReceiptItem receiptItem = this.deliveryReceiptItemRepository.getReferenceById(deliveryReceiptItem.getId());
        receiptItem.setQuantityReceived(deliveryReceiptItem.getQuantityReceivedTmp());
        receiptItem.setUpdated(true);
        receiptItem.setUpdatedDate(LocalDateTime.now());
        this.deliveryReceiptItemRepository.save(receiptItem);
    }

    @Override
    public void updateOrderUnitPrice(DeliveryReceiptItemLiteDTO deliveryReceiptItem) {
        DeliveryReceiptItem receiptItem = this.deliveryReceiptItemRepository.getReferenceById(deliveryReceiptItem.getId());
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
            Objects.nonNull(orderLine.getQuantityReceived()) ? orderLine.getQuantityReceived() : orderLine.getQuantityRequested()
        );
        /*  receiptItem.setInitStock(
    produitService.getProductTotalStock(fournisseurProduit.getProduit().getId()));*/
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
        setProductState(fournisseurProduit.getProduit());
        return receiptItem;
    }

    private void setProductState(List<Produit> produits) {
        produits.forEach(this::setProductState);
    }

    private void setProductState(Produit produit) {
        List<ProductState> productStates = this.productStateService.fetchByProduitAndState(produit, ProductStateEnum.COMMANDE_PASSE);
        if (!CollectionUtils.isEmpty(productStates)) {
            if (productStates.size() == 1) {
                productStates.forEach(this.productStateService::remove);
            } else {
                this.productStateService.remove(productStates.getFirst());
            }
        }
        this.productStateService.addState(produit, ProductStateEnum.ENTREE);
    }

    private List<LotJsonValue> getLotByOrderLine(OrderLine orderLine, Commande commande) {
        if (CollectionUtils.isEmpty(commande.getLots())) {
            return Collections.emptyList();
        }
        return commande.getLots().stream().filter(lotJsonValue -> lotJsonValue.getReceiptItem().compareTo(orderLine.getId()) == 0).toList();
    }

    private String buildDeliveryReceiptNumberTransaction() {
        WarehouseSequence warehouseSequence = warehouseSequenceRepository.getReferenceById(EntityConstant.ENTREE_STOCK_SEQUENCE_ID);
        String num = StringUtils.leftPad(String.valueOf(warehouseSequence.getValue()), EntityConstant.LEFTPAD_SIZE, '0');
        warehouseSequence.setValue(warehouseSequence.getValue() + warehouseSequence.getIncrement());
        warehouseSequenceRepository.save(warehouseSequence);
        return num;
    }

    private FournisseurProduit updateFournisseurProduit(DeliveryReceiptItem deliveryReceiptItem) {
        FournisseurProduit fournisseurProduit = deliveryReceiptItem.getFournisseurProduit();
        Produit produit = fournisseurProduit.getProduit();
        int montantAdditionel = produit.getTableau() != null ? produit.getTableau().getValue() : 0;
        fournisseurProduit.setUpdatedAt(LocalDateTime.now());
        fournisseurProduit.setPrixAchat(deliveryReceiptItem.getOrderCostAmount());
        fournisseurProduit.setPrixUni(deliveryReceiptItem.getOrderUnitPrice() + montantAdditionel);
        return fournisseurProduitService.update(fournisseurProduit);
    }

    private int getTotalStockQuantity(StockProduit stockProduit) {
        return stockProduit.getQtyStock() + (Objects.nonNull(stockProduit.getQtyUG()) ? stockProduit.getQtyStock() : 0);
    }

    private DeliveryReceipt buildDeliveryReceipt(DeliveryReceiptLiteDTO deliveryReceiptLite, DeliveryReceipt deliveryReceipt) {
        deliveryReceipt.setReceiptDate(deliveryReceiptLite.getReceiptFullDate().toLocalDate());
        deliveryReceipt.setModifiedDate(LocalDateTime.now());
        deliveryReceipt.setModifiedUser(storageService.getUser());
        deliveryReceipt.setReceiptAmount(deliveryReceiptLite.getReceiptAmount());
        deliveryReceipt.setDiscountAmount(0);
        deliveryReceipt.setTaxAmount(deliveryReceiptLite.getTaxAmount());
        deliveryReceipt.setReceiptRefernce(deliveryReceiptLite.getReceiptRefernce());
        deliveryReceipt.setSequenceBon(deliveryReceiptLite.getSequenceBon());
        deliveryReceipt.setNetAmount(ServiceUtil.computeHtaxe(deliveryReceipt.getReceiptAmount(), deliveryReceipt.getTaxAmount()));
        return deliveryReceipt;
    }

    private DeliveryReceiptLiteDTO fromEntity(DeliveryReceipt deliveryReceipt) {
        return new DeliveryReceiptLiteDTO()
            .setId(deliveryReceipt.getId())
            .setReceiptAmount(deliveryReceipt.getReceiptAmount())
            .setReceiptDate(deliveryReceipt.getReceiptDate())
            .setReceiptFullDate(deliveryReceipt.getReceiptDate().atStartOfDay())
            .setOrderReference(deliveryReceipt.getOrderReference())
            .setSequenceBon(deliveryReceipt.getSequenceBon())
            .setReceiptRefernce(deliveryReceipt.getReceiptRefernce())
            .setTaxAmount(deliveryReceipt.getTaxAmount());
    }

    private DeliveryReceipt importNewBon(UploadDeleiveryReceiptDTO uploadDeleiveryReceipt) {
        DeliveryReceipt deliveryReceipt = new DeliveryReceipt();
        deliveryReceipt.setType(TypeDeliveryReceipt.DIRECT);
        deliveryReceipt.setCreatedDate(LocalDateTime.now());
        deliveryReceipt.setCreatedUser(storageService.getUser());
        deliveryReceipt.setFournisseur(this.fournisseurRepository.getReferenceById(uploadDeleiveryReceipt.getFournisseurId()));
        deliveryReceipt.setReceiptStatut(ReceiptStatut.PENDING);
        deliveryReceipt.setNumberTransaction(buildDeliveryReceiptNumberTransaction());
        buildDeliveryReceipt(uploadDeleiveryReceipt.getDeliveryReceipt(), deliveryReceipt);
        deliveryReceipt.setOrderReference(deliveryReceipt.getReceiptRefernce());
        deliveryReceiptRepository.save(deliveryReceipt);
        return deliveryReceipt;
    }

    private DeliveryReceiptItem createItemFromFile(OrderLine orderLine, DeliveryReceipt deliveryReceipt) {
        FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
        DeliveryReceiptItem receiptItem = new DeliveryReceiptItem();
        receiptItem.setDeliveryReceipt(deliveryReceipt);
        receiptItem.setCreatedDate(deliveryReceipt.getCreatedDate());
        receiptItem.setQuantityReceived(
            Objects.nonNull(orderLine.getQuantityReceived()) ? orderLine.getQuantityReceived() : orderLine.getQuantityRequested()
        );
        receiptItem.setInitStock(produitService.getProductTotalStock(fournisseurProduit.getProduit().getId()));
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

    private Optional<DeliveryReceiptItem> findInMap(Map<Long, DeliveryReceiptItem> longOrderLineMap, Long fourniseurProduitId) {
        if (longOrderLineMap.containsKey(fourniseurProduitId)) {
            return Optional.of(longOrderLineMap.get(fourniseurProduitId));
        }

        return Optional.empty();
    }

    private void updateReceiptItemFromRecord(DeliveryReceiptItem receiptItem, int quantityReceived, int quantityUg, int taxAmount) {
        receiptItem.setUgQuantity(receiptItem.getUgQuantity() + quantityUg);
        receiptItem.setQuantityReceived(receiptItem.getQuantityReceived() + quantityReceived);
        receiptItem.setQuantityRequested(receiptItem.getQuantityReceived());
        receiptItem.setTaxAmount(receiptItem.getTaxAmount() + taxAmount);
    }

    private void updateInRecord(
        /* DeliveryReceipt deliveryReceipt,*/
        DeliveryReceiptItem orderLine,
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

    private DeliveryReceiptItem buildDeliveryReceiptItemFromRecord(
        FournisseurProduit fournisseurProduit,
        int quantityRequested,
        int quantityReceived,
        int orderCostAmount,
        int orderUnitPrice,
        int quantityUg,
        int stock,
        int taxeAmount,
        DeliveryReceipt deliveryReceipt
    ) {
        DeliveryReceiptItem receiptItem = new DeliveryReceiptItem();
        receiptItem.setUgQuantity(quantityUg);
        receiptItem.setCreatedDate(deliveryReceipt.getCreatedDate());
        receiptItem.setQuantityReceived(quantityReceived);
        receiptItem.setQuantityRequested(quantityRequested);
        receiptItem.setOrderUnitPrice(orderUnitPrice > 0 ? orderUnitPrice : fournisseurProduit.getPrixUni());
        receiptItem.setOrderCostAmount(orderCostAmount > 0 ? orderCostAmount : fournisseurProduit.getPrixAchat());
        receiptItem.setCostAmount(fournisseurProduit.getPrixAchat());
        receiptItem.setRegularUnitPrice(fournisseurProduit.getPrixUni());
        receiptItem.setInitStock(stock);
        receiptItem.setFournisseurProduit(fournisseurProduit);
        receiptItem.setTaxAmount(taxeAmount);
        deliveryReceipt.addReceiptItem(receiptItem);

        return receiptItem;
    }

    private void createInRecord(
        Map<Long, DeliveryReceiptItem> longOrderLineMap,
        DeliveryReceipt deliveryReceipt,
        FournisseurProduit fournisseurProduit,
        int quantityRequested,
        int quantityReceived,
        int orderCostAmount,
        int orderUnitPrice,
        int quantityUg,
        int currentStock,
        int taxAmount
    ) {
        DeliveryReceiptItem orderLineNew =
            this.deliveryReceiptItemRepository.save(
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
        setProductState(fournisseurProduit.getProduit());
    }

    private void updateDeliveryReceiptAmountDuringUploading(
        DeliveryReceipt deliveryReceipt,
        DeliveryReceiptItem receiptItem,
        Integer oldQuantityReceived,
        int oldTaxAmount
    ) {
        deliveryReceipt.setReceiptAmount(
            deliveryReceipt.getReceiptAmount() +
            (receiptItem.getQuantityReceived() * receiptItem.getOrderCostAmount()) -
            (oldQuantityReceived * receiptItem.getOrderCostAmount())
        );

        deliveryReceipt.setTaxAmount(deliveryReceipt.getTaxAmount() + receiptItem.getTaxAmount() - oldTaxAmount);
    }

    private CommandeResponseDTO uploadLaborexModelCSVFormat(
        DeliveryReceipt deliveryReceipt,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, DeliveryReceiptItem> longOrderLineMap
    ) throws IOException {
        int totalItemCount = 0;
        int succesCount = 0;
        Long fournisseurId = deliveryReceipt.getFournisseur().getId();
        try (
            CSVParser parser = new CSVParser(
                new InputStreamReader(multipartFile.getInputStream()),
                CSVFormat.EXCEL.builder().setDelimiter(';').build()
            )
        ) {
            for (CSVRecord record : parser) {
                if (totalItemCount > 0) {
                    String codeProduit = record.get(3);
                    // String lgFamilleId, int qty, int intPafDetail, int pu, int ug
                    //int quantityReceived = Integer.parseInt(record.get(5));
                    int quantityReceived = Integer.parseInt(record.get(7));
                    // int orderCostAmount = (int) Double.parseDouble(record.get(6));
                    int orderCostAmount = (int) Double.parseDouble(record.get(8));
                    //  int orderUnitPrice = (int) Double.parseDouble(record.get(7));
                    int orderUnitPrice = (int) Double.parseDouble(record.get(9));
                    int taxAmount = (int) Double.parseDouble(record.get(11));
                    int quantityUg = Integer.parseInt(record.get(6));

                    Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
                        codeProduit,
                        fournisseurId
                    );
                    if (fournisseurProduitOptional.isPresent()) {
                        FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
                        int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());

                        findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                            orderLine -> {
                                // int oldQty = orderLine.getQuantityReceived();
                                // int oldTaxAmount = orderLine.getTaxAmount();
                                updateInRecord(orderLine, quantityReceived, taxAmount, quantityUg);
                            },
                            () ->
                                createInRecord(
                                    longOrderLineMap,
                                    deliveryReceipt,
                                    fournisseurProduit,
                                    quantityReceived,
                                    quantityReceived,
                                    orderCostAmount,
                                    orderUnitPrice,
                                    quantityUg,
                                    currentStock,
                                    taxAmount
                                )
                        );
                        succesCount++;
                    } else {
                        CommandServiceImpl.addModelLaborexLigneExistant(
                            items,
                            record,
                            codeProduit,
                            quantityReceived,
                            quantityUg,
                            orderUnitPrice,
                            orderCostAmount
                        );
                    }
                }
                totalItemCount++;
            }
        } catch (IOException e) {
            log.debug("{0}", e);
            throw e;
        }
        return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount - 1, succesCount);
    }

    private CommandeResponseDTO uploadCOPHARMEDCSVFormat(
        DeliveryReceipt deliveryReceipt,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, DeliveryReceiptItem> longOrderLineMap
    ) throws IOException {
        int totalItemCount = 0;
        int succesCount = 0;
        Long fournisseurId = deliveryReceipt.getFournisseur().getId();
        try (
            CSVParser parser = new CSVParser(
                new InputStreamReader(multipartFile.getInputStream()),
                CSVFormat.EXCEL.builder().setDelimiter(';').build()
            )
        ) {
            for (CSVRecord record : parser) {
                if (totalItemCount > 0) {
                    String codeProduit = record.get(4);

                    int quantityReceived = Integer.parseInt(record.get(9));
                    int orderCostAmount = (int) Double.parseDouble(record.get(11));
                    int orderUnitPrice = (int) Double.parseDouble(record.get(13));
                    int quantityUg = Integer.parseInt(record.get(10));
                    int quantityRequested = Integer.parseInt(record.get(8));
                    Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
                        codeProduit,
                        fournisseurId
                    );
                    if (fournisseurProduitOptional.isPresent()) {
                        FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
                        int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());
                        findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                            orderLine -> updateInRecord(orderLine, quantityReceived, 0, quantityUg),
                            () ->
                                createInRecord(
                                    longOrderLineMap,
                                    deliveryReceipt,
                                    fournisseurProduit,
                                    quantityRequested,
                                    quantityReceived,
                                    orderCostAmount,
                                    orderUnitPrice,
                                    quantityUg,
                                    currentStock,
                                    0
                                )
                        );
                        succesCount++;
                    } else {
                        items.add(
                            new OrderItem()
                                .setFacture(record.get(1))
                                .setDateBonLivraison(record.get(0))
                                .setUg(quantityUg)
                                .setLigne(Integer.parseInt(record.get(2)))
                                .setProduitCip(codeProduit)
                                .setProduitLibelle(record.get(6))
                                .setQuantityRequested(quantityRequested)
                                .setQuantityReceived(quantityReceived)
                                .setPrixUn(orderUnitPrice)
                                .setPrixAchat(orderCostAmount)
                        );
                    }
                }
                totalItemCount++;
            }
        } catch (IOException e) {
            log.debug("{0}", e);
            throw e;
        }

        return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount - 1, succesCount);
    }

    private CommandeResponseDTO uploadDPCICSVFormat(
        DeliveryReceipt deliveryReceipt,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, DeliveryReceiptItem> longOrderLineMap
    ) throws IOException {
        int totalItemCount = 0;
        int succesCount = 0;
        Long fournisseurId = deliveryReceipt.getFournisseur().getId();
        try (
            CSVParser parser = new CSVParser(
                new InputStreamReader(multipartFile.getInputStream()),
                CSVFormat.EXCEL.builder().setDelimiter(';').build()
            )
        ) {
            for (CSVRecord record : parser) {
                String codeProduit = record.get(2);
                totalItemCount++;
                int quantityReceived = Integer.parseInt(record.get(6));
                int orderCostAmount = (int) Double.parseDouble(record.get(3));
                int orderUnitPrice = (int) Double.parseDouble(record.get(4));
                int quantityRequested = Integer.parseInt(record.get(7));
                double taxAmount = Double.parseDouble(record.get(5));
                Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
                    codeProduit,
                    fournisseurId
                );
                if (fournisseurProduitOptional.isPresent()) {
                    FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
                    int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());
                    findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        orderLine -> {
                            updateInRecord(orderLine, quantityReceived, (int) taxAmount, 0);
                        },
                        () ->
                            createInRecord(
                                longOrderLineMap,
                                deliveryReceipt,
                                fournisseurProduit,
                                quantityRequested,
                                quantityReceived,
                                orderCostAmount,
                                orderUnitPrice,
                                0,
                                currentStock,
                                (int) taxAmount
                            )
                    );
                    succesCount++;
                } else {
                    items.add(
                        new OrderItem()
                            .setReferenceBonLivraison(record.get(8))
                            .setTva(taxAmount)
                            .setLigne(Integer.parseInt(record.get(0)))
                            .setProduitCip(codeProduit)
                            .setProduitLibelle(record.get(1))
                            .setQuantityRequested(quantityRequested)
                            .setQuantityReceived(quantityReceived)
                            .setPrixUn(orderUnitPrice)
                            .setPrixAchat(orderCostAmount)
                    );
                }
            }
        } catch (IOException e) {
            log.debug("{0}", e);
            throw e;
        }

        return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount, succesCount);
    }

    private CommandeResponseDTO uploadTEDISCSVFormat(
        DeliveryReceipt deliveryReceipt,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, DeliveryReceiptItem> longOrderLineMap
    ) throws IOException {
        int totalItemCount = 0;
        int succesCount = 0;
        Long fournisseurId = deliveryReceipt.getFournisseur().getId();
        try (
            CSVParser parser = new CSVParser(
                new InputStreamReader(multipartFile.getInputStream()),
                CSVFormat.EXCEL.builder().setDelimiter(';').build()
            )
        ) {
            for (CSVRecord record : parser) {
                String codeProduit = record.get(1);
                totalItemCount++;
                int quantityReceived = new BigDecimal(record.get(3)).intValue();
                int orderCostAmount = new BigDecimal(record.get(2)).intValue();
                int orderUnitPrice = new BigDecimal(record.get(5)).intValue();

                Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
                    codeProduit,
                    fournisseurId
                );
                if (fournisseurProduitOptional.isPresent()) {
                    FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
                    //   orderUnitPrice = orderUnitPrice==0? fournisseurProduit.getPrixUni():orderUnitPrice;
                    int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());
                    findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        orderLine -> updateInRecord(orderLine, quantityReceived, 0, 0),
                        () -> {
                            int unitPrice = orderUnitPrice == 0 ? fournisseurProduit.getPrixUni() : orderUnitPrice;
                            createInRecord(
                                longOrderLineMap,
                                deliveryReceipt,
                                fournisseurProduit,
                                quantityReceived,
                                quantityReceived,
                                orderCostAmount,
                                unitPrice,
                                0,
                                currentStock,
                                0
                            );
                        }
                    );
                    succesCount++;
                } else {
                    items.add(
                        new OrderItem()
                            .setLigne(Integer.parseInt(record.get(0)))
                            .setProduitCip(codeProduit)
                            .setProduitEan(codeProduit)
                            .setPrixUn(orderUnitPrice)
                            .setQuantityReceived(quantityReceived)
                            .setPrixAchat(orderCostAmount)
                    );
                }
            }
        } catch (IOException e) {
            log.debug("{0}", e);
            throw e;
        }
        return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount, succesCount);
    }

    private CommandeResponseDTO uploadCipQteFormat(
        DeliveryReceipt deliveryReceipt,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, DeliveryReceiptItem> longOrderLineMap
    ) {
        int totalItemCount = 0;
        int succesCount = 0;
        int isFirstLigne;
        long fournisseurId = deliveryReceipt.getFournisseur().getId();
        try (
            CSVParser parser = new CSVParser(
                new InputStreamReader(multipartFile.getInputStream()),
                CSVFormat.EXCEL.builder().setDelimiter(';').build()
            )
        ) {
            for (CSVRecord record : parser) {
                isFirstLigne = skipFirstLigne(record, totalItemCount);
                if (isFirstLigne < 0) {
                    continue;
                }
                String codeProduit = record.get(0);
                int quantityReceived = Integer.parseInt(record.get(1));
                Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
                    codeProduit,
                    fournisseurId
                );
                if (fournisseurProduitOptional.isPresent()) {
                    FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
                    int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());

                    findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        item -> updateInRecord(item, quantityReceived, 0, 0),
                        () ->
                            createInRecord(
                                longOrderLineMap,
                                deliveryReceipt,
                                fournisseurProduit,
                                quantityReceived,
                                quantityReceived,
                                fournisseurProduit.getPrixAchat(),
                                fournisseurProduit.getPrixUni(),
                                0,
                                currentStock,
                                0
                            )
                    );
                    succesCount++;
                } else {
                    items.add(new OrderItem().setProduitCip(codeProduit).setQuantityReceived(quantityReceived));
                }

                totalItemCount++;
            }
        } catch (IOException e) {
            log.debug("{0}", e);
        }

        return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount - 1, succesCount);
    }

    private CommandeResponseDTO uploadCipQtePrixAchatFormat(
        DeliveryReceipt deliveryReceipt,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, DeliveryReceiptItem> longOrderLineMap
    ) {
        int totalItemCount = 0;
        int succesCount = 0;
        int isFirstLigne;
        long fournisseurId = deliveryReceipt.getFournisseur().getId();
        try (
            CSVParser parser = new CSVParser(
                new InputStreamReader(multipartFile.getInputStream()),
                CSVFormat.EXCEL.builder().setDelimiter(';').build()
            )
        ) {
            for (CSVRecord record : parser) {
                isFirstLigne = skipFirstLigne(record, totalItemCount);
                if (isFirstLigne < 0) {
                    continue;
                }
                String codeProduit = record.get(0);
                int quantityReceived = Integer.parseInt(record.get(3));
                int prixAchat = Integer.parseInt(record.get(4));
                Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
                    codeProduit,
                    fournisseurId
                );
                if (fournisseurProduitOptional.isPresent()) {
                    FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
                    int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());

                    findInMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        item -> updateInRecord(item, quantityReceived, 0, 0),
                        () -> {
                            int prixA = prixAchat == 0 ? fournisseurProduit.getPrixAchat() : prixAchat;
                            createInRecord(
                                longOrderLineMap,
                                deliveryReceipt,
                                fournisseurProduit,
                                quantityReceived,
                                quantityReceived,
                                prixA,
                                fournisseurProduit.getPrixUni(),
                                0,
                                currentStock,
                                0
                            );
                        }
                    );
                    succesCount++;
                } else {
                    items.add(
                        new OrderItem()
                            .setQuantityRequested(Integer.parseInt(record.get(1)))
                            .setPrixAchat(prixAchat)
                            .setProduitCip(codeProduit)
                            .setQuantityReceived(quantityReceived)
                    );
                }

                totalItemCount++;
            }
        } catch (IOException e) {
            log.debug("{0}", e);
        }
        return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount - 1, succesCount);
    }

    private CommandeResponseDTO uploadTXTFormat(DeliveryReceipt deliveryReceipt, MultipartFile multipartFile) throws IOException {
        List<OrderItem> items = new ArrayList<>();
        Map<Long, DeliveryReceiptItem> longOrderLineMap = new HashMap<>();
        Long fournisseurId = deliveryReceipt.getFournisseur().getId();
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
                                deliveryReceipt,
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

        return buildCommandeResponseDTO(deliveryReceipt, items, totalItemCount, succesCount);
    }

    private CommandeResponseDTO buildCommandeResponseDTO(
        DeliveryReceipt deliveryReceipt,
        List<OrderItem> items,
        int totalItemCount,
        int succesCount
    ) {
        return new CommandeResponseDTO()
            .setFailureCount(items.size())
            .setItems(items)
            .setReference(deliveryReceipt.getReceiptRefernce())
            .setSuccesCount(succesCount)
            .setTotalItemCount(totalItemCount);
    }

    private CommandeResponseDTO uploadCSVFormat(DeliveryReceipt deliveryReceipt, CommandeModel commandeModel, MultipartFile multipartFile)
        throws IOException {
        CommandeResponseDTO commandeResponseDTO;
        List<OrderItem> items = new ArrayList<>();
        Map<Long, DeliveryReceiptItem> longOrderLineMap = new HashMap<>();

        commandeResponseDTO = switch (commandeModel) {
            case LABOREX -> uploadLaborexModelCSVFormat(deliveryReceipt, multipartFile, items, longOrderLineMap);
            case COPHARMED -> uploadCOPHARMEDCSVFormat(deliveryReceipt, multipartFile, items, longOrderLineMap);
            case DPCI -> uploadDPCICSVFormat(deliveryReceipt, multipartFile, items, longOrderLineMap);
            case TEDIS -> uploadTEDISCSVFormat(deliveryReceipt, multipartFile, items, longOrderLineMap);
            case CIP_QTE_PA -> uploadCipQtePrixAchatFormat(deliveryReceipt, multipartFile, items, longOrderLineMap);
            case CIP_QTE -> uploadCipQteFormat(deliveryReceipt, multipartFile, items, longOrderLineMap);
        };
        assert commandeResponseDTO != null;
        return commandeResponseDTO.setEntity(fromEntity(this.deliveryReceiptRepository.save(deliveryReceipt)));
    }

    private void finalyseSaisieStock(Commande commande, DeliveryReceiptLiteDTO deliveryReceiptLite) {
        Set<OrderLine> orderLineSet = commande.getOrderLines();
        DeliveryReceipt deliveryReceipt = this.deliveryReceiptRepository.getReferenceById(deliveryReceiptLite.getId());
        // TODO: liste des vente en avoir pour envoi possible de notif et de mail
        orderLineSet.forEach(orderLine -> {
            if (isNotEntreeStockIsAuthorize.test(orderLine) && cannotContinue.test(orderLine, getLotByOrderLine(orderLine, commande))) {
                throw new GenericError(
                    "La reception de certains produits n'a pas ete faite. Veuillez verifier la saisie",
                    "commandeManquante"
                );
            }

            DeliveryReceiptItem deliveryReceiptItem = addItem(orderLine, deliveryReceipt);
            getLotByOrderLine(orderLine, commande).forEach(
                lotJsonValue -> lotService.addLot(lotJsonValue, deliveryReceiptItem, deliveryReceipt.getReceiptRefernce())
            );
            FournisseurProduit fournisseurProduit = updateFournisseurProduit(deliveryReceiptItem);
            StockProduit stockProduit = produitService.updateTotalStock(
                fournisseurProduit.getProduit(),
                deliveryReceiptItem.getQuantityReceived(),
                deliveryReceiptItem.getUgQuantity()
            );
            Produit produit = stockProduit.getProduit();

            produit.setPrixMnp(
                produitService.calculPrixMoyenPondereReception(
                    deliveryReceiptItem.getInitStock(),
                    orderLine.getGrossAmount(),
                    getTotalStockQuantity(stockProduit),
                    deliveryReceiptItem.getOrderCostAmount()
                )
            );
            produitService.update(produit);
        });
        logsService.create(
            TransactionType.ENTREE_STOCK,
            "order.entry",
            new Object[] { deliveryReceipt.getReceiptRefernce() },
            deliveryReceipt.getId().toString()
        );
        deliveryReceipt.setReceiptStatut(ReceiptStatut.CLOSE);
        this.deliveryReceiptRepository.saveAndFlush(deliveryReceipt);
        commandeRepository.saveAndFlush(commande);
    }

    private void saveItem(DeliveryReceiptItem receiptItem) {
        FournisseurProduit fournisseurProduit = receiptItem.getFournisseurProduit();
        receiptItem.setUpdatedDate(LocalDateTime.now());
        receiptItem.setQuantityReceived(
            Objects.nonNull(receiptItem.getQuantityReceived()) ? receiptItem.getQuantityReceived() : receiptItem.getQuantityRequested()
        );
        receiptItem.setInitStock(produitService.getProductTotalStock(fournisseurProduit.getProduit().getId()));
        receiptItem.setCostAmount(fournisseurProduit.getPrixAchat());
        receiptItem.setAfterStock(receiptItem.getInitStock() + receiptItem.getQuantityReceived() + receiptItem.getUgQuantity());
        receiptItem.setDiscountAmount(0);
        receiptItem.setRegularUnitPrice(fournisseurProduit.getPrixUni());
        receiptItem.setOrderUnitPrice(receiptItem.getOrderUnitPrice());

        receiptItem.setNetAmount(0);
        receiptItem.setTaxAmount(0);
        receiptItem.setQuantityReturned(0);

        deliveryReceiptItemRepository.saveAndFlush(receiptItem);
    }

    private void updateFournisseurProduit(DeliveryReceiptItem deliveryReceiptItem, FournisseurProduit fournisseurProduit, Produit produit) {
        int montantAdditionel = produit.getTableau() != null ? produit.getTableau().getValue() : 0;
        fournisseurProduit.setUpdatedAt(LocalDateTime.now());
        if (deliveryReceiptItem.getOrderCostAmount().compareTo(fournisseurProduit.getPrixAchat()) != 0) {
            fournisseurProduit.setPrixAchat(deliveryReceiptItem.getOrderCostAmount());
        }
        if ((deliveryReceiptItem.getOrderUnitPrice() + montantAdditionel) != (fournisseurProduit.getPrixUni() + montantAdditionel)) {
            fournisseurProduit.setPrixUni(deliveryReceiptItem.getOrderCostAmount());
        }

        fournisseurProduitService.update(fournisseurProduit);
    }

    private void updateProductState(Produit produit) {
        List<ProductState> productStates = this.productStateService.fetchByProduit(produit);
        if (!CollectionUtils.isEmpty(productStates)) {
            if (productStates.size() == 1) {
                productStates.forEach(this.productStateService::remove);
            } else {
                productStates
                    .stream()
                    .filter(s -> s.getState() == ProductStateEnum.ENTREE)
                    .limit(1)
                    .forEach(this.productStateService::remove);
            }
        }
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
