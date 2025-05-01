package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.WarehouseCalendarService;
import com.kobe.warehouse.service.csv.ExportationCsvService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.LotJsonValue;
import com.kobe.warehouse.service.dto.OrderItem;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.ImportationEchoueService;
import com.kobe.warehouse.service.utils.DateUtil;
import com.kobe.warehouse.service.utils.FileUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class CommandServiceImpl implements CommandService {

    private static final String CSV = "csv";
    private static final String TXT = "txt";
    private final Logger log = LoggerFactory.getLogger(CommandServiceImpl.class);
    private final CommandeRepository commandeRepository;
    private final StorageService storageService;
    private final OrderLineService orderLineService;
    private final ReferenceService referenceService;
    private final ExportationCsvService exportationCsvService;
    private final WarehouseCalendarService warehouseCalendarService;
    private final ImportationEchoueService importationEchoueService;

    public CommandServiceImpl(
        CommandeRepository commandeRepository,
        StorageService storageService,
        OrderLineService orderLineService,
        ReferenceService referenceService,
        ExportationCsvService exportationCsvService,
        WarehouseCalendarService warehouseCalendarService,
        ImportationEchoueService importationEchoueService
    ) {
        this.commandeRepository = commandeRepository;
        this.storageService = storageService;
        this.orderLineService = orderLineService;
        this.referenceService = referenceService;
        this.exportationCsvService = exportationCsvService;
        this.warehouseCalendarService = warehouseCalendarService;
        this.importationEchoueService = importationEchoueService;
    }

    static void addModelLaborexLigneExistant(
        List<OrderItem> items,
        CSVRecord record,
        String codeProduit,
        int quantityReceived,
        int quantityUg,
        int orderUnitPrice,
        int orderCostAmount
    ) {
        items.add(
            new OrderItem()
                .setEtablissement(record.get(0))
                .setFacture(record.get(1))
                .setLigne(Integer.parseInt(record.get(2)))
                .setProduitCip(codeProduit)
                .setProduitLibelle(record.get(4))
                .setQuantityRequested(Integer.parseInt(record.get(5)))
                .setQuantityReceived(quantityReceived)
                .setMontant((double) orderCostAmount)
                .setPrixAchat(orderCostAmount)
                .setPrixUn(orderUnitPrice)
                .setReferenceBonLivraison(record.get(10))
                .setUg(quantityUg)
                .setTva(Double.parseDouble(record.get(11)))
        );
    }

    @Override
    public Commande createNewCommande(Commande commande) {
        commande.setCalendar(warehouseCalendarService.initCalendar());
        return commandeRepository.saveAndFlush(commande);
    }

    @Override
    public Commande createNewCommandeFromCommandeDTO(CommandeDTO commande) {
        return createNewCommande(buildCommandeFromCommandeDTO(commande));
    }

    private Commande buildCommandeFromCommandeDTO(CommandeDTO commandeDTO) {
        User user = storageService.getUser();
        Commande commande = new Commande();
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(commande.getCreatedAt());
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setUser(user);
        commande.setLastUserEdit(user);
        commande.setMagasin(user.getMagasin());
        commande.setOrderReference(referenceService.buildNumCommande());
        OrderLine orderLine = orderLineService.buildOrderLineFromOrderLineDTO(commandeDTO.getOrderLines().getFirst());
        commande.addOrderLine(orderLine);
        commande.setGrossAmount(orderLine.getGrossAmount());
        commande.setOrderAmount(orderLine.getOrderAmount());
        commande.setFournisseur(buildFournisseurFromId(commandeDTO.getFournisseurId()));
        return commande;
    }

    @Override
    public Commande createOrUpdateOrderLine(OrderLineDTO orderLineDTO) {
        int oldGrossAmount = 0;
        int oldOrderAmount = 0;
        Optional<OrderLine> optionalOrderLine = orderLineService.findOneFromCommande(
            orderLineDTO.getProduitId(),
            orderLineDTO.getCommande().getId(),
            orderLineDTO.getCommande().getFournisseurId()
        );
        OrderLine orderLine = null;
        if (optionalOrderLine.isPresent()) {
            orderLine = optionalOrderLine.get();
        }

        Commande commande = commandeRepository.getReferenceById(orderLineDTO.getCommande().getId());
        if (orderLine == null) {
            orderLine = orderLineService.buildOrderLineFromOrderLineDTO(orderLineDTO);
            orderLine.setCommande(commande);
            commande.getOrderLines().add(orderLine);
        } else {
            oldGrossAmount = orderLine.getGrossAmount();
            oldOrderAmount = orderLine.getOrderCostAmount();
            orderLine.setQuantityRequested(orderLine.getQuantityRequested() + orderLineDTO.getQuantityRequested());
            orderLine.setOrderAmount(orderLine.getOrderUnitPrice() * orderLine.getQuantityRequested());
            orderLine.setGrossAmount(orderLine.getOrderCostAmount() * orderLine.getQuantityRequested());
        }
        updateCommandeAmount(commande, orderLine, oldGrossAmount, oldOrderAmount);
        orderLineService.save(orderLine);
        return commandeRepository.saveAndFlush(commande);
    }

    @Override
    public Commande updateQuantityRequested(OrderLineDTO orderLineDTO) {
        Pair<OrderLine, OrderLine> orderLineOrderLinePair = orderLineService.updateOrderLineQuantityRequested(orderLineDTO);
        return updateCommande(orderLineOrderLinePair);
    }

    @Override
    public void updateOrderLineQuantityReceived(OrderLineDTO orderLineDTO) {
        orderLineService
            .findOneById(orderLineDTO.getId())
            .ifPresentOrElse(
                orderLine -> orderLineService.updateOrderLineQuantityReceived(orderLine, orderLineDTO.getQuantityReceived()),
                NullPointerException::new
            );
    }

    @Override
    public void updateOrderLineQuantityUg(OrderLineDTO orderLineDTO) {
        orderLineService
            .findOneById(orderLineDTO.getId())
            .ifPresentOrElse(
                orderLine -> orderLineService.updateOrderLineQuantityUG(orderLineDTO.getId(), orderLineDTO.getUgQuantity()),
                NullPointerException::new
            );
    }

    @Override
    public Commande updateOrderCostAmount(OrderLineDTO orderLineDTO) {
        Pair<OrderLine, OrderLine> orderLineOrderLinePair = orderLineService.updateOrderLineCostAmount(orderLineDTO);

        return updateCommande(orderLineOrderLinePair);
    }

    @Override
    public Commande updateOrderUnitPrice(OrderLineDTO orderLineDTO) {
        Pair<OrderLine, OrderLine> orderLineOrderLinePair = orderLineService.updateOrderLineUnitPrice(orderLineDTO);

        return updateCommande(orderLineOrderLinePair);
    }

    @Override
    public void deleteOrderLineById(Long orderLineId) {
        orderLineService
            .findOneById(orderLineId)
            .ifPresent(orderLine -> {
                Commande commande = orderLine.getCommande();
                commande.removeOrderLine(orderLine);
                updateCommandeAmount(commande, orderLine.getGrossAmount() * (-1), orderLine.getOrderAmount() * (-1));
                orderLineService.deleteOrderLine(orderLine);
                commandeRepository.save(commande);
            });
    }

    @Override
    public void deleteById(Long id) {
        commandeRepository.deleteById(id);
    }

    @Override
    public void rollback(Long id) {
        Commande commande = commandeRepository.getReferenceById(id);
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setUpdatedAt(LocalDateTime.now());
        commande.setLastUserEdit(storageService.getUser());
        commandeRepository.save(commande);
    }

    @Override
    public void updateCodeCip(OrderLineDTO orderLineDTO) {
        orderLineService.updateCodeCip(orderLineDTO);
    }

    @Override
    public void deleteOrderLinesByIds(Long commandeId, List<Long> ids) {
        Commande commande = commandeRepository.getReferenceById(commandeId);
        ids.forEach(orderLineId ->
            orderLineService
                .findOneById(orderLineId)
                .ifPresent(orderLine -> {
                    commande.removeOrderLine(orderLine);
                    updateCommandeAmount(commande, orderLine.getGrossAmount() * (-1), orderLine.getOrderAmount() * (-1));
                    orderLineService.deleteOrderLine(orderLine);
                })
        );
        commandeRepository.save(commande);
    }

    @Override
    public void closeCommandeEnCours(Long commandeId) {
        Commande commande = commandeRepository.getReferenceById(commandeId);
        commande.setOrderStatus(OrderStatut.PASSED);
        commande.setUpdatedAt(LocalDateTime.now());
        commande.setLastUserEdit(storageService.getUser());
        commandeRepository.save(commande);
    }

    @Override
    public void fusionner(List<Long> ids) {
        Collections.sort(ids);
        final Long firstCommandeId = ids.getFirst();
        Commande commande = commandeRepository.getReferenceById(firstCommandeId);
        List<Commande> commandesToDelete = new ArrayList<>();
        List<Long> longs = ids.subList(1, ids.size());
        longs.forEach(aLong -> {
            Commande commandeSecond = commandeRepository.getReferenceById(aLong);
            commandeSecond
                .getOrderLines()
                .forEach(orderLine ->
                    findOrderLineInSetOrderLine(commande.getOrderLines(), orderLine).ifPresentOrElse(
                        orderLine1 -> {
                            orderLineService.updateOrderLine(orderLine1, orderLine.getQuantityRequested());
                            orderLineService.save(orderLine1);
                        },
                        () -> {
                            commande.addOrderLine(orderLine);
                            orderLineService.save(orderLine);
                            updateCommande(commande, orderLine);
                        }
                    )
                );
            commandesToDelete.add(commandeSecond);
        });
        commande.setLastUserEdit(storageService.getUser());
        commande.setUpdatedAt(LocalDateTime.now());
        commandeRepository.save(commande);
        commandeRepository.deleteAll(commandesToDelete);
    }

    @Override
    public void deleteAll(List<Long> ids) {
        ids.forEach(this.commandeRepository::deleteById);
    }

    @Override
    public VerificationResponseCommandeDTO importerReponseCommande(Long commandeId, MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();
        assert fileName != null;
        String extension = fileName.substring(fileName.indexOf(".") + 1);
        if (extension.equalsIgnoreCase(CSV)) {
            return verificationCommandeCsv(multipartFile, commandeRepository.getReferenceById(commandeId));
        }
        return verificationCommandeExcel(multipartFile, commandeRepository.getReferenceById(commandeId));
    }

    @Override
    public CommandeResponseDTO uploadNewCommande(Long fournisseurId, CommandeModel commandeModel, MultipartFile multipartFile) {
        String extension = FileUtil.getFileExtension(multipartFile.getOriginalFilename());
        Commande commande = buildCommande(fournisseurId);
        CommandeResponseDTO commandeResponse =
            switch (extension) {
                case CSV -> uploadCSVFormat(commande, commandeModel, multipartFile);
                case TXT -> uploadTXTFormat(commande, multipartFile);
                default -> throw new GenericError(
                    String.format("Le modèle ===> %s d'importation de commande n'est pas pris en charche", commandeModel.name()),
                    "modelimportation"
                );
            };
        saveLignesBonEchouees(commandeResponse, commande.getId());
        return commandeResponse;
    }

    @Override
    public void createCommandeFromSuggestion(Suggestion suggestion) {
        buildNew(suggestion);
    }

    private void saveLignesBonEchouees(CommandeResponseDTO commandeResponse, Long commandeId) {
        if (Objects.isNull(commandeResponse) || commandeResponse.getItems().isEmpty()) {
            return;
        }
        this.importationEchoueService.save(commandeId, true, commandeResponse.getItems());
    }

    public void createRuptureFile(String commandeReference, CommandeModel commandeModel, List<OrderItem> items) {
        if (!CollectionUtils.isEmpty(items)) {
            exportationCsvService.createRuptureFile(commandeReference, items, commandeModel);
        }
    }

    private Commande buildCommande(Long fournisseurId) {
        Commande commande = new Commande();
        commande.setCalendar(warehouseCalendarService.initCalendar());
        commande.setTaxAmount(0);
        commande.setNetAmount(0);
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setFournisseur(buildFournisseurFromId(fournisseurId));
        commande.setOrderReference(referenceService.buildNumCommande());
        commande.setLastUserEdit(storageService.getUser());
        commande.setUser(commande.getLastUserEdit());
        commande.setOrderAmount(0);
        commande.setGrossAmount(0);
        commande.setDiscountAmount(0);
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(commande.getCreatedAt());
        commande.setMagasin(commande.getUser().getMagasin());
        return commande;
    }

    private CommandeResponseDTO uploadCSVFormat(Commande commande, CommandeModel commandeModel, MultipartFile multipartFile) {
        CommandeResponseDTO commandeResponseDTO;
        List<OrderItem> items = new ArrayList<>();
        Map<Long, OrderLine> longOrderLineMap = new HashMap<>();
        long fournisseurId = commande.getFournisseur().getId();
        commandeResponseDTO = switch (commandeModel) {
            case LABOREX -> uploadLaborexCSVFormat(commande, multipartFile, items, longOrderLineMap, fournisseurId);
            case COPHARMED -> uploadCOPHARMEDCSVFormat(commande, multipartFile, items, longOrderLineMap, fournisseurId);
            case DPCI -> uploadDPCICSVFormat(commande, multipartFile, items, longOrderLineMap, fournisseurId);
            case TEDIS -> uploadTEDISCSVFormat(commande, multipartFile, items, longOrderLineMap, fournisseurId);
            case CIP_QTE_PA -> uploadCipQtePrixAchatFormat(commande, multipartFile, items, longOrderLineMap, fournisseurId);
            case CIP_QTE -> uploadCipQteFormat(commande, multipartFile, items, longOrderLineMap, fournisseurId);
        };
        createRuptureFile(commande.getOrderReference(), commandeModel, commandeResponseDTO.getItems());
        return commandeResponseDTO;
    }

    private CommandeResponseDTO uploadTXTFormat(Commande commande, MultipartFile multipartFile) {
        List<OrderItem> items = new ArrayList<>();
        Map<Long, OrderLine> longOrderLineMap = new HashMap<>();
        long fournisseurId = commande.getFournisseur().getId();
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
                    succesCount = getSuccesCount(
                        commande,
                        longOrderLineMap,
                        succesCount,
                        quantityReceived,
                        orderCostAmount,
                        orderUnitPrice,
                        fournisseurProduit,
                        currentStock,
                        null,
                        null
                    );
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
            log.error(e.getLocalizedMessage());
        }
        commandeRepository.save(commande);
        return buildCommandeResponseDTO(commande, items, totalItemCount, succesCount);
    }

    private int getSuccesCount(
        Commande commande,
        Map<Long, OrderLine> longOrderLineMap,
        int succesCount,
        int quantityReceived,
        int orderCostAmount,
        int orderUnitPrice,
        FournisseurProduit fournisseurProduit,
        int currentStock,
        String lotNumber,
        LocalDate expirationDate
    ) {
        findInCommandeMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
            orderLine -> {
                int oldQty = orderLine.getQuantityReceived();
                int oldTaxAmount = orderLine.getTaxAmount();
                updateInRecord(commande, orderLine, quantityReceived, 0, oldQty, oldTaxAmount, 0);
                buildLot(orderLine, quantityReceived, lotNumber, expirationDate, 0, null);
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
                    0,
                    lotNumber,
                    expirationDate,
                    0,
                    null
                )
        );
        succesCount++;
        return succesCount;
    }

    private CommandeResponseDTO uploadLaborexCSVFormat(
        Commande commande,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, OrderLine> longOrderLineMap,
        long fournisseurId
    ) {
        int totalItemCount = 0;
        int succesCount = 0;

        try (
            CSVParser parser = new CSVParser(
                new InputStreamReader(multipartFile.getInputStream()),
                CSVFormat.EXCEL.builder().setDelimiter(';').build()
            )
        ) {
            for (CSVRecord record : parser) {
                if (totalItemCount > 0) {
                    // String codeProduit = record.get(2);
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

                        findInCommandeMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                            orderLine -> {
                                int oldQty = orderLine.getQuantityReceived();
                                int oldTaxAmount = orderLine.getTaxAmount();
                                updateInRecord(commande, orderLine, quantityReceived, taxAmount, oldQty, oldTaxAmount, quantityUg);
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
                                    quantityUg,
                                    currentStock,
                                    taxAmount,
                                    null,
                                    null,
                                    0,
                                    null
                                )
                        );
                        succesCount++;
                    } else {
                        addModelLaborexLigneExistant(
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
        }
        if (items.isEmpty()) {
            commande.setOrderStatus(OrderStatut.PASSED);
        }
        commandeRepository.save(commande);
        return buildCommandeResponseDTO(commande, items, totalItemCount - 1, succesCount);
    }

    private CommandeResponseDTO uploadCOPHARMEDCSVFormat(
        Commande commande,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, OrderLine> longOrderLineMap,
        long fournisseurId
    ) {
        int totalItemCount = 0;
        int succesCount = 0;

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
                        findInCommandeMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                            orderLine -> {
                                int oldQty = orderLine.getQuantityReceived();
                                int oldTaxAmount = orderLine.getTaxAmount();
                                updateInRecord(commande, orderLine, quantityReceived, 0, oldQty, oldTaxAmount, quantityUg);
                            },
                            () ->
                                createInRecord(
                                    longOrderLineMap,
                                    commande,
                                    fournisseurProduit,
                                    quantityRequested,
                                    quantityReceived,
                                    orderCostAmount,
                                    orderUnitPrice,
                                    quantityUg,
                                    currentStock,
                                    0,
                                    null,
                                    null,
                                    0,
                                    null
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
        }
        if (items.isEmpty()) {
            commande.setOrderStatus(OrderStatut.PASSED);
        }
        commandeRepository.save(commande);
        return buildCommandeResponseDTO(commande, items, totalItemCount - 1, succesCount);
    }

    private CommandeResponseDTO uploadTEDISCSVFormat(
        Commande commande,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, OrderLine> longOrderLineMap,
        long fournisseurId
    ) {
        int totalItemCount = 0;
        int succesCount = 0;

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
                String lotNumero = record.get(6);
                String dateP = record.get(7);
                LocalDate datePeremption = DateUtil.fromYyyyMmDd(dateP);

                Optional<FournisseurProduit> fournisseurProduitOptional = orderLineService.getFournisseurProduitByCriteria(
                    codeProduit,
                    fournisseurId
                );
                if (fournisseurProduitOptional.isPresent()) {
                    FournisseurProduit fournisseurProduit = fournisseurProduitOptional.get();
                    orderUnitPrice = orderUnitPrice == 0 ? fournisseurProduit.getPrixUni() : orderUnitPrice;
                    int currentStock = orderLineService.produitTotalStockWithQantitUg(fournisseurProduit.getProduit());
                    succesCount = getSuccesCount(
                        commande,
                        longOrderLineMap,
                        succesCount,
                        quantityReceived,
                        orderCostAmount,
                        orderUnitPrice,
                        fournisseurProduit,
                        currentStock,
                        lotNumero,
                        datePeremption
                    );
                } else {
                    items.add(
                        new OrderItem()
                            .setLigne(Integer.parseInt(record.get(0)))
                            .setProduitCip(codeProduit)
                            .setProduitEan(codeProduit)
                            .setPrixUn(orderUnitPrice)
                            .setQuantityReceived(quantityReceived)
                            .setPrixAchat(orderCostAmount)
                            .setLotNumber(lotNumero)
                            .setDatePeremption(dateP)
                    );
                }
            }
        } catch (IOException e) {
            log.debug("{0}", e);
        }
        if (items.isEmpty()) {
            commande.setOrderStatus(OrderStatut.PASSED);
        }
        commandeRepository.save(commande);
        return buildCommandeResponseDTO(commande, items, totalItemCount, succesCount);
    }

    private CommandeResponseDTO uploadDPCICSVFormat(
        Commande commande,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, OrderLine> longOrderLineMap,
        long fournisseurId
    ) {
        int totalItemCount = 0;
        int succesCount = 0;

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
                    findInCommandeMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        orderLine -> {
                            int oldQty = orderLine.getQuantityReceived();
                            int oldTaxAmount = orderLine.getTaxAmount();
                            updateInRecord(commande, orderLine, quantityReceived, (int) taxAmount, oldQty, oldTaxAmount, 0);
                        },
                        () ->
                            createInRecord(
                                longOrderLineMap,
                                commande,
                                fournisseurProduit,
                                quantityRequested,
                                quantityReceived,
                                orderCostAmount,
                                orderUnitPrice,
                                0,
                                currentStock,
                                (int) taxAmount,
                                null,
                                null,
                                0,
                                null
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
        }
        if (items.isEmpty()) {
            commande.setOrderStatus(OrderStatut.PASSED);
        }
        commandeRepository.save(commande);
        return buildCommandeResponseDTO(commande, items, totalItemCount, succesCount);
    }

    private CommandeResponseDTO buildCommandeResponseDTO(Commande commande, List<OrderItem> items, int totalItemCount, int succesCount) {
        return new CommandeResponseDTO()
            .setFailureCount(items.size())
            .setItems(items)
            .setReference(commande.getOrderReference())
            .setSuccesCount(succesCount)
            .setTotalItemCount(totalItemCount);
    }

    private void updateInRecord(
        Commande commande,
        OrderLine orderLine,
        int quantityReceived,
        int taxAmount,
        int oldQty,
        int oldTaxAmount,
        int quantitUg
    ) {
        updateOrderLineFromRecord(orderLine, quantityReceived, quantitUg, taxAmount);

        /*  commande.removeOrderLine(orderLine);
    commande.addOrderLine(orderLine);*/
        updateCommandeAmountDuringUploading(commande, orderLine, oldQty, oldTaxAmount);
    }

    private void createInRecord(
        Map<Long, OrderLine> longOrderLineMap,
        Commande commande,
        FournisseurProduit fournisseurProduit,
        int quantityRequested,
        int quantityReceived,
        int orderCostAmount,
        int orderUnitPrice,
        int quantityUg,
        int currentStock,
        int taxAmount,
        String lotNumber,
        LocalDate expirationDate,
        int lotNumberUg,
        LocalDate manufactureDate
    ) {
        OrderLine orderLineNew = orderLineService.createOrderLine(
            commande,
            buildOrderLineDTOFromRecord(
                fournisseurProduit,
                quantityRequested,
                quantityReceived,
                orderCostAmount,
                orderUnitPrice,
                quantityUg,
                currentStock,
                taxAmount
            )
        );
        orderLineNew.setFournisseurProduit(fournisseurProduit);
        buildLot(orderLineNew, quantityReceived, lotNumber, expirationDate, lotNumberUg, manufactureDate);
        commande.addOrderLine(orderLineNew);
        updateCommandeAmountDuringUploading(commande, orderLineNew, 0, 0);
        longOrderLineMap.put(fournisseurProduit.getId(), orderLineNew);
    }

    private Optional<OrderLine> findInCommandeMap(Map<Long, OrderLine> longOrderLineMap, Long fourniseurProduitId) {
        if (longOrderLineMap.containsKey(fourniseurProduitId)) {
            return Optional.of(longOrderLineMap.get(fourniseurProduitId));
        }

        return Optional.empty();
    }

    private OrderLineDTO buildOrderLineDTOFromRecord(
        FournisseurProduit fournisseurProduit,
        int quantityRequested,
        int quantityReceived,
        int orderCostAmount,
        int orderUnitPrice,
        int quantityUg,
        int stock,
        int taxeAmount
    ) {
        OrderLineDTO orderLineDTO = new OrderLineDTO();
        orderLineDTO.setProvisionalCode(false);
        orderLineDTO.setQuantityUg(quantityUg);
        orderLineDTO.setQuantityReceived(quantityReceived);
        orderLineDTO.setQuantityRequested(quantityRequested);
        orderLineDTO.setOrderAmount(fournisseurProduit.getPrixUni() * quantityRequested);
        orderLineDTO.setGrossAmount(fournisseurProduit.getPrixAchat() * quantityRequested);
        orderLineDTO.setOrderUnitPrice(orderUnitPrice > 0 ? orderUnitPrice : fournisseurProduit.getPrixUni());
        orderLineDTO.setOrderCostAmount(orderCostAmount > 0 ? orderCostAmount : fournisseurProduit.getPrixAchat());
        orderLineDTO.setCostAmount(fournisseurProduit.getPrixAchat());
        orderLineDTO.setRegularUnitPrice(fournisseurProduit.getPrixUni());
        orderLineDTO.setInitStock(stock);
        orderLineDTO.setTaxAmount(taxeAmount);
        return orderLineDTO;
    }

    private Optional<OrderLine> findOrderLineInSetOrderLine(Set<OrderLine> orderLines, OrderLine orderLineToCheck) {
        return orderLines
            .stream()
            .filter(orderLine -> orderLine.getFournisseurProduit().equals(orderLineToCheck.getFournisseurProduit()))
            .findFirst();
    }

    private void updateCommandeAmount(Commande commande, OrderLine orderLine, Integer oldGrossAmount, Integer oldOrderAmount) {
        commande.setLastUserEdit(storageService.getUser());
        commande.setGrossAmount(orderLine.getGrossAmount() + commande.getGrossAmount() - oldGrossAmount);
        commande.setOrderAmount(orderLine.getOrderAmount() + commande.getOrderAmount() - oldOrderAmount);
    }

    private void updateCommandeAmount(Commande commande, Integer grossAmount, Integer orderAmount) {
        commande.setLastUserEdit(storageService.getUser());
        commande.setGrossAmount(commande.getGrossAmount() + grossAmount);
        commande.setOrderAmount(commande.getOrderAmount() + orderAmount);
    }

    private Commande updateCommande(Pair<OrderLine, OrderLine> orderLineOrderLinePair) {
        OrderLine oldOrderLine = orderLineOrderLinePair.getFirst();
        OrderLine orderLine = orderLineOrderLinePair.getSecond();
        Commande commande = orderLine.getCommande();
        updateCommandeAmount(commande, orderLine, oldOrderLine.getGrossAmount(), oldOrderLine.getOrderAmount());
        return commandeRepository.saveAndFlush(commande);
    }

    private Fournisseur buildFournisseurFromId(Long id) {
        return new Fournisseur().id(id);
    }

    private void updateCommande(Commande commande, OrderLine orderLine) {
        commande.setGrossAmount(orderLine.getGrossAmount() + commande.getGrossAmount());
        commande.setOrderAmount(orderLine.getOrderAmount() + commande.getOrderAmount());
    }

    private VerificationResponseCommandeDTO verificationCommandeCsv(MultipartFile multipartFile, Commande commande) {
        Set<OrderLine> orderLinesToRemove = new HashSet<>();
        Set<OrderLine> orderLinesToSave = new HashSet<>();
        List<VerificationResponseCommandeDTO.Item> items = new ArrayList<>();
        List<VerificationResponseCommandeDTO.Item> extraItems = new ArrayList<>();
        VerificationResponseCommandeDTO verificationResponseCommandeDTO = new VerificationResponseCommandeDTO();
        try (
            CSVParser parser = new CSVParser(
                new InputStreamReader(multipartFile.getInputStream()),
                CSVFormat.EXCEL.builder().setDelimiter(';').build()
            )
        ) {
            for (CSVRecord record : parser) {
                String code = record.get(0);
                int quantityReceived = Integer.parseInt(record.get(3));
                proccessFileRecord(commande, code, items, extraItems, quantityReceived, orderLinesToSave, orderLinesToRemove);
            }
            updateResponseCommandeEnCours(commande, orderLinesToSave, orderLinesToRemove);
        } catch (IOException e) {
            log.debug("{0}", e);
        }
        verificationResponseCommandeDTO.setItems(items).setExtraItems(extraItems);
        return verificationResponseCommandeDTO;
    }

    private void updateResponseCommandeEnCours(Commande commande, Set<OrderLine> orderLinesToSave, Set<OrderLine> orderLinesToRemove) {
        if (orderLinesToSave.isEmpty()) {
            commandeRepository.delete(commande);
        } else {
            saveOrderLines(orderLinesToSave);
            removeOrderLines(commande, orderLinesToRemove);
            commandeRepository.save(commande);
        }
        if (!orderLinesToRemove.isEmpty()) {
            orderLineService.deleteAll(orderLinesToRemove);
        }
    }

    private void proccessFileRecord(
        Commande commande,
        String code,
        List<VerificationResponseCommandeDTO.Item> items,
        List<VerificationResponseCommandeDTO.Item> extraItems,
        int quantityReceived,
        Set<OrderLine> orderLinesToSave,
        Set<OrderLine> orderLinesToRemove
    ) {
        getOrderLineInCommandeItems(commande.getOrderLines(), code).ifPresentOrElse(
            orderLine -> {
                log.info(
                    "orderLine {} ==>> code {} quantityReceived {} qut {}",
                    orderLine.getFournisseurProduit().getCodeCip(),
                    code,
                    quantityReceived,
                    orderLine.getQuantityRequested()
                );
                VerificationResponseCommandeDTO.Item item = updateOrderItemQtyFromResponse(orderLine, quantityReceived);
                if (quantityReceived > 0) {
                    orderLinesToSave.add(orderLine);
                } else {
                    orderLinesToRemove.add(orderLine);
                }
                items.add(item);
            },
            () -> extraItems.add(new VerificationResponseCommandeDTO.Item().setCodeCip(code).setQuantitePriseEnCompte(quantityReceived))
        );
    }

    private VerificationResponseCommandeDTO verificationCommandeExcel(MultipartFile multipartFile, Commande commande) {
        Set<OrderLine> orderLinesToRemove = new HashSet<>();
        Set<OrderLine> orderLinesToSave = new HashSet<>();
        List<VerificationResponseCommandeDTO.Item> items = new ArrayList<>();
        List<VerificationResponseCommandeDTO.Item> extraItems = new ArrayList<>();
        VerificationResponseCommandeDTO verificationResponseCommandeDTO = new VerificationResponseCommandeDTO();

        try (Workbook workbook = WorkbookFactory.create(multipartFile.getInputStream())) {
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    String code = "";
                    Cell codeCell = row.getCell(0);
                    Cell qtyquantityReceivedCell = row.getCell(3);
                    switch (codeCell.getCellType()) {
                        case STRING:
                            code = codeCell.getStringCellValue();
                            break;
                        case NUMERIC:
                            try {
                                code = String.valueOf(codeCell.getNumericCellValue());
                            } catch (Exception ignored) {}
                            break;
                        default:
                            break;
                    }
                    int quantityReceived = (int) qtyquantityReceivedCell.getNumericCellValue();
                    proccessFileRecord(commande, code, items, extraItems, quantityReceived, orderLinesToSave, orderLinesToRemove);
                }
            }
            updateResponseCommandeEnCours(commande, orderLinesToSave, orderLinesToRemove);
        } catch (IOException e) {
            log.debug("{0}", e);
        }
        return verificationResponseCommandeDTO.setItems(items).setExtraItems(extraItems);
    }

    private void saveOrderLines(Set<OrderLine> orderLines) {
        if (!orderLines.isEmpty()) {
            orderLineService.saveAll(orderLines);
        }
    }

    private void removeOrderLines(Commande commande, Set<OrderLine> orderLines) {
        int grossAmount = 0;
        int orderAmount = 0;
        for (OrderLine orderLine : orderLines) {
            grossAmount += orderLine.getGrossAmount();
            orderAmount += orderLine.getOrderAmount();
            commande.removeOrderLine(orderLine);
        }

        updateCommandeAmount(commande, grossAmount * (-1), orderAmount * (-1));
    }

    private VerificationResponseCommandeDTO.Item updateOrderItemQtyFromResponse(OrderLine orderLine, int quantityReceived) {
        FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
        Produit produit = fournisseurProduit.getProduit();

        if (quantityReceived > 0) {
            orderLineService.updateOrderLineQuantityReceived(orderLine, quantityReceived);
        }

        return new VerificationResponseCommandeDTO.Item()
            .setCodeCip(fournisseurProduit.getCodeCip())
            .setProduitLibelle(produit.getLibelle())
            .setCodeEan(produit.getCodeEan())
            .setQuantite(orderLine.getQuantityRequested())
            .setQuantitePriseEnCompte(quantityReceived);
    }

    private Optional<OrderLine> getOrderLineInCommandeItems(Set<OrderLine> orderLines, final String codeCipOrCodeEan) {
        for (OrderLine orderLine : orderLines) {
            FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
            if (
                fournisseurProduit.getCodeCip().contains(codeCipOrCodeEan) ||
                fournisseurProduit.getProduit().getCodeEan().contains(codeCipOrCodeEan)
            ) {
                return Optional.of(orderLine);
            }
        }
        return Optional.empty();
    }

    private void updateCommandeAmountDuringUploading(
        Commande commande,
        OrderLine orderLine,
        Integer oldQuantityReceived,
        int oldTaxAmount
    ) {
        commande.setGrossAmount(
            commande.getGrossAmount() +
            (orderLine.getQuantityReceived() * orderLine.getOrderCostAmount()) -
            (oldQuantityReceived * orderLine.getOrderCostAmount())
        );
        commande.setOrderAmount(
            orderLine.getOrderAmount() +
            (orderLine.getQuantityReceived() * orderLine.getOrderUnitPrice()) -
            (oldQuantityReceived * orderLine.getOrderUnitPrice())
        );
        commande.setTaxAmount(commande.getTaxAmount() + orderLine.getTaxAmount() - oldTaxAmount);
        commande.setTaxAmount(commande.getTaxAmount() + orderLine.getTaxAmount() - oldTaxAmount);
    }

    private void updateOrderLineFromRecord(OrderLine orderLine, int quantityReceived, int quantityUg, int taxAmount) {
        orderLine.setQuantityUg(orderLine.getQuantityUg() + quantityUg);
        orderLine.setQuantityReceived(orderLine.getQuantityReceived() + quantityReceived);
        orderLine.setQuantityRequested(orderLine.getQuantityReceived());
        orderLine.setOrderAmount(orderLine.getRegularUnitPrice() * orderLine.getQuantityReceived());
        orderLine.setGrossAmount(orderLine.getCostAmount() * orderLine.getQuantityReceived());
        orderLine.setTaxAmount(orderLine.getTaxAmount() + taxAmount);
    }

    private CommandeResponseDTO uploadCipQteFormat(
        Commande commande,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, OrderLine> longOrderLineMap,
        long fournisseurId
    ) {
        int totalItemCount = 0;
        int succesCount = 0;
        int isFirstLigne;
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

                    findInCommandeMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        orderLine -> {
                            int oldQty = orderLine.getQuantityReceived();
                            updateInRecord(commande, orderLine, quantityReceived, 0, oldQty, 0, 0);
                        },
                        () ->
                            createInRecord(
                                longOrderLineMap,
                                commande,
                                fournisseurProduit,
                                quantityReceived,
                                quantityReceived,
                                fournisseurProduit.getPrixAchat(),
                                fournisseurProduit.getPrixUni(),
                                0,
                                currentStock,
                                0,
                                null,
                                null,
                                0,
                                null
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
        if (items.isEmpty()) {
            commande.setOrderStatus(OrderStatut.PASSED);
        }
        commandeRepository.save(commande);
        return buildCommandeResponseDTO(commande, items, totalItemCount - 1, succesCount);
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

    private CommandeResponseDTO uploadCipQtePrixAchatFormat(
        Commande commande,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Long, OrderLine> longOrderLineMap,
        long fournisseurId
    ) {
        int totalItemCount = 0;
        int succesCount = 0;
        int isFirstLigne;
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

                    findInCommandeMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        orderLine -> {
                            int oldQty = orderLine.getQuantityReceived();
                            updateInRecord(commande, orderLine, quantityReceived, 0, oldQty, 0, 0);
                        },
                        () ->
                            createInRecord(
                                longOrderLineMap,
                                commande,
                                fournisseurProduit,
                                quantityReceived,
                                quantityReceived,
                                prixAchat,
                                fournisseurProduit.getPrixUni(),
                                0,
                                currentStock,
                                0,
                                null,
                                null,
                                0,
                                null
                            )
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
        if (items.isEmpty()) {
            commande.setOrderStatus(OrderStatut.PASSED);
        }
        commandeRepository.save(commande);
        return buildCommandeResponseDTO(commande, items, totalItemCount - 1, succesCount);
    }

    private void buildLot(
        OrderLine orderLine,
        int quantity,
        String lotNumber,
        LocalDate expirationDate,
        int freeQuantity,
        LocalDate manufacturingDate
    ) {
        if (StringUtils.hasLength(lotNumber)) {
            orderLine
                .getLots()
                .add(
                    new LotJsonValue()
                        .setNumLot(lotNumber)
                        .setFreeQuantity(freeQuantity)
                        .setExpiryDate(expirationDate)
                        .setQuantity(quantity)
                        .setManufacturingDate(manufacturingDate)
                );
        }
    }

    private Commande buildNew(Suggestion suggestion) {
        User user = storageService.getUser();
        Commande commande = new Commande();
        commande.setCalendar(warehouseCalendarService.initCalendar());
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(commande.getCreatedAt());
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setUser(user);
        commande.setLastUserEdit(user);
        commande.setMagasin(user.getMagasin());
        commande.setOrderReference(referenceService.buildNumCommande());
        commande.setGrossAmount(0);
        commande.setOrderAmount(0); //getGrossAmount
        commande.setFournisseur(suggestion.getFournisseur());
        suggestion
            .getSuggestionLines()
            .forEach(suggestionLine -> {
                OrderLine orderLine = this.orderLineService.buildOrderLine(suggestionLine);
                orderLine.setCommande(commande);
                commande.setGrossAmount(commande.getGrossAmount() + orderLine.getGrossAmount());
                commande.setOrderAmount(commande.getOrderAmount() + orderLine.getOrderAmount());
                commande.getOrderLines().add(orderLine);
            });
        return commandeRepository.save(commande);
    }
}
