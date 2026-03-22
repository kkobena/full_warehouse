package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.csv.ExportationCsvService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO;
import com.kobe.warehouse.service.dto.OrderItem;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.CommandeIdGeneratorService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.ImportationEchoueService;
import com.kobe.warehouse.service.stock.csv.CsvImportStrategy;
import com.kobe.warehouse.service.stock.csv.ParsedCsvRecord;
import com.kobe.warehouse.service.utils.FileUtil;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommandServiceImpl implements CommandService {

    private final Logger log = LoggerFactory.getLogger(CommandServiceImpl.class);
    private static final String CSV = "csv";
    private static final String TXT = "txt";
    private static final Map<CommandeModel, CsvImportStrategy> CSV_STRATEGIES = Map.of(
        CommandeModel.LABOREX, CsvImportStrategy.LABOREX,
        CommandeModel.COPHARMED, CsvImportStrategy.COPHARMED,
        CommandeModel.DPCI, CsvImportStrategy.DPCI,
        CommandeModel.TEDIS, CsvImportStrategy.TEDIS,
        CommandeModel.CIP_QTE, CsvImportStrategy.CIP_QTE,
        CommandeModel.CIP_QTE_PA, CsvImportStrategy.CIP_QTE_PA
    );
    private final CommandeRepository commandeRepository;
    private final StorageService storageService;
    private final OrderLineService orderLineService;
    private final ReferenceService referenceService;
    private final ExportationCsvService exportationCsvService;
    private final ImportationEchoueService importationEchoueService;
    private final CommandeIdGeneratorService commandeIdGeneratorService;
    private final SuggestionReassortService suggestionReassortService;
    private final SuggestionRepository suggestionRepository;

    public CommandServiceImpl(
        CommandeRepository commandeRepository,
        StorageService storageService,
        OrderLineService orderLineService,
        ReferenceService referenceService,
        ExportationCsvService exportationCsvService,
        ImportationEchoueService importationEchoueService,
        CommandeIdGeneratorService commandeIdGeneratorService,
        SuggestionReassortService suggestionReassortService,
        SuggestionRepository suggestionRepository
    ) {
        this.commandeRepository = commandeRepository;
        this.storageService = storageService;
        this.orderLineService = orderLineService;
        this.referenceService = referenceService;
        this.exportationCsvService = exportationCsvService;
        this.importationEchoueService = importationEchoueService;
        this.commandeIdGeneratorService = commandeIdGeneratorService;
        this.suggestionReassortService = suggestionReassortService;
        this.suggestionRepository = suggestionRepository;
    }

    private Commande createNewCommande(Commande commande) {
        return commandeRepository.saveAndFlush(commande);
    }

    @Override
    public CommandeLiteDTO createNewCommandeFromCommandeDTO(CommandeDTO commande) {
        return new CommandeLiteDTO(createNewCommande(buildCommandeFromCommandeDTO(commande)));
    }

    private Commande buildCommandeFromCommandeDTO(CommandeDTO commandeDTO) {
        AppUser user = storageService.getUser();
        Commande commande = new Commande();
        commande.setId(this.commandeIdGeneratorService.getNextIdAsInt());
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(commande.getCreatedAt());
        commande.setUser(user);
        commande.setOrderReference(referenceService.buildNumCommande());
        OrderLine orderLine = orderLineService.buildOrderLineFromOrderLineDTO(commandeDTO.getOrderLines().getFirst());
        commande.addOrderLine(orderLine);
        commande.setOrderAmount(orderLine.getOrderUnitPrice() * orderLine.getQuantityRequested());
        commande.setFinalAmount(orderLine.getOrderUnitPrice() * orderLine.getQuantityRequested());
        commande.setGrossAmount(orderLine.getOrderCostAmount() * orderLine.getQuantityRequested());
        commande.setFournisseur(buildFournisseurFromId(commandeDTO.getFournisseurId()));
        return commande;
    }

    private Commande findCommandeById(CommandeId id) {
        return commandeRepository.getReferenceById(id);
    }

    @Override
    public CommandeLiteDTO createOrUpdateOrderLine(OrderLineDTO orderLineDTO) {
        int oldGrossAmount = 0;
        int oldOrderAmount = 0;

        Optional<OrderLine> optionalOrderLine = orderLineService.findOneFromCommande(
            orderLineDTO.getProduitId(),
            orderLineDTO.getCommande().getCommandeId(),
            orderLineDTO.getCommande().getFournisseurId()
        );
        OrderLine orderLine = null;
        if (optionalOrderLine.isPresent()) {
            orderLine = optionalOrderLine.get();
        }

        Commande commande = findCommandeById(orderLineDTO.getCommande().getCommandeId());
        if (orderLine == null) {
            orderLine = orderLineService.buildOrderLineFromOrderLineDTO(orderLineDTO);
            orderLine.setCommande(commande);
            commande.getOrderLines().add(orderLine);
        } else {
            oldGrossAmount = orderLine.getQuantityRequested() * orderLine.getOrderCostAmount();
            oldOrderAmount = orderLine.getQuantityRequested() * orderLine.getOrderUnitPrice();
            orderLine.setQuantityRequested(orderLine.getQuantityRequested() + orderLineDTO.getQuantityRequested());
        }
        updateCommandeAmount(commande, orderLine, oldGrossAmount, oldOrderAmount);
        orderLineService.save(orderLine);
        return new CommandeLiteDTO(commandeRepository.saveAndFlush(commande));
    }

    @Override
    public CommandeLiteDTO updateQuantityRequested(OrderLineDTO orderLineDTO) {
        Pair<OrderLine, OrderLine> orderLineOrderLinePair = orderLineService.updateOrderLineQuantityRequested(orderLineDTO);
        return new CommandeLiteDTO(updateCommande(orderLineOrderLinePair));
    }

    @Override
    public void updateOrderLineQuantityReceived(OrderLineDTO orderLineDTO) {
        orderLineService
            .findOneById(orderLineDTO.getOrderLineId())
            .ifPresentOrElse(
                orderLine -> orderLineService.updateOrderLineQuantityReceived(orderLine, orderLineDTO.getQuantityReceived()),
                () -> {
                    throw new GenericError("Ligne de commande introuvable", "orderLineNotFound");
                }
            );
    }

    @Override
    public void updateOrderLineQuantityUg(OrderLineDTO orderLineDTO) {
        orderLineService
            .findOneById(orderLineDTO.getOrderLineId())
            .ifPresentOrElse(
                orderLine -> orderLineService.updateOrderLineQuantityUG(orderLine.getId(), orderLineDTO.getFreeQty()),
                () -> {
                    throw new GenericError("Ligne de commande introuvable", "orderLineNotFound");
                }
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
    public void deleteOrderLineById(OrderLineId orderLineId) {
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
    public void deleteById(CommandeId id) {
        commandeRepository.deleteById(id);
    }

    @Override
    public void rollback(CommandeId id) {
        Commande commande = findCommandeById(id);
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setUpdatedAt(LocalDateTime.now());
        commandeRepository.save(commande);
    }

    @Override
    public void updateCodeCip(OrderLineDTO orderLineDTO) {
        orderLineService.updateCodeCip(orderLineDTO);
    }

    @Override
    public void deleteOrderLinesByIds(CommandeId commandeId, List<OrderLineId> ids) {
        Commande commande = findCommandeById(commandeId);
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
    public void fusionner(List<CommandeId> ids) {
        ids.sort(Comparator.comparing(CommandeId::getId));
        final CommandeId firstCommandeId = ids.getFirst();
        Commande commande = findCommandeById(firstCommandeId);
        List<Commande> commandesToDelete = new ArrayList<>();
        List<CommandeId> longs = ids.subList(1, ids.size());
        longs.forEach(aLong -> {
            Commande commandeSecond = findCommandeById(aLong);
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
        commande.setUpdatedAt(LocalDateTime.now());
        commandeRepository.save(commande);
        commandeRepository.deleteAll(commandesToDelete);
    }

    @Override
    public void deleteAll(List<CommandeId> ids) {
        ids.forEach(this.commandeRepository::deleteById);
    }

    @Override
    public VerificationResponseCommandeDTO importerReponseCommande(CommandeId commandeId, MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new GenericError("Le nom du fichier importé est obligatoire", "fileNameRequired");
        }
        String extension = fileName.substring(fileName.indexOf(".") + 1);
        if (extension.equalsIgnoreCase(CSV)) {
            return verificationCommandeCsv(multipartFile, findCommandeById(commandeId));
        }
        return verificationCommandeExcel(multipartFile, findCommandeById(commandeId));
    }

    @Override
    public CommandeResponseDTO uploadNewCommande(Integer fournisseurId, CommandeModel commandeModel, MultipartFile multipartFile) {
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
        saveLignesBonEchouees(commandeResponse, commande.getId().getId());
        return commandeResponse;
    }

    @Override
    public void createCommandeFromSuggestion(Suggestion suggestion) {
        buildNew(suggestion);
    }

    @Override
    public void createCommandeFromSelection(
        Suggestion suggestion,
       List<CommanderSelectionDTO.LigneSelection> lignes
    ) {
        Map<Integer, Integer> qteParLigne = lignes.stream()
            .collect(Collectors.toMap(
                CommanderSelectionDTO.LigneSelection::suggestionLineId,
               CommanderSelectionDTO.LigneSelection::quantite
            ));
        AppUser user = storageService.getUser();
        Commande commande = new Commande();
        commande.setId(this.commandeIdGeneratorService.getNextIdAsInt());
        Fournisseur fournisseur = suggestion.getFournisseur();
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(commande.getCreatedAt());
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setUser(user);
        commande.setOrderReference(referenceService.buildNumCommande());
        commande.setReceiptReference(commande.getOrderReference());
        commande.setGrossAmount(0);
        commande.setOrderAmount(0);
        commande.setTaxAmount(0);
        commande.setHtAmount(0);
        commande.setDiscountAmount(0);
        commande.setFournisseur(fournisseur);
        suggestion.getSuggestionLines().stream()
            .filter(sl -> qteParLigne.containsKey(sl.getId()))
            .forEach(suggestionLine -> {
                int qte = qteParLigne.get(suggestionLine.getId());
                if (qte <= 0) return;
                OrderLine orderLine = orderLineService.buildOrderLine(suggestionLine, fournisseur.getId());
                orderLine.setQuantityRequested(qte);
                orderLine.setCommande(commande);
                updateCommandeAmount(commande, orderLine);
                commande.getOrderLines().add(orderLine);
            });
        commandeRepository.save(commande);
    }

    private void saveLignesBonEchouees(CommandeResponseDTO commandeResponse, Integer commandeId) {
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

    private Commande buildCommande(Integer fournisseurId) {
        Commande commande = new Commande();
        commande.setId(this.commandeIdGeneratorService.getNextIdAsInt());
        commande.setTaxAmount(0);
        commande.setHtAmount(0);
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setFournisseur(buildFournisseurFromId(fournisseurId));
        commande.setOrderReference(referenceService.buildNumCommande());
        commande.setReceiptReference(commande.getOrderReference());
        commande.setUser(storageService.getUser());
        commande.setGrossAmount(0);
        commande.setDiscountAmount(0);
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(commande.getCreatedAt());
        commande.setOrderAmount(0);

        return commande;
    }

    private CommandeResponseDTO uploadCSVFormat(Commande commande, CommandeModel commandeModel, MultipartFile multipartFile) {
        List<OrderItem> items = new ArrayList<>();
        Map<Integer, OrderLine> longOrderLineMap = new HashMap<>();
        int fournisseurId = commande.getFournisseur().getId();
        CsvImportStrategy strategy = CSV_STRATEGIES.get(commandeModel);
        CommandeResponseDTO commandeResponseDTO = processCsvWithStrategy(
            commande, multipartFile, items, longOrderLineMap, fournisseurId, strategy
        );
        createRuptureFile(commande.getOrderReference(), commandeModel, commandeResponseDTO.getItems());
        return commandeResponseDTO;
    }

    private CommandeResponseDTO processCsvWithStrategy(
        Commande commande,
        MultipartFile multipartFile,
        List<OrderItem> items,
        Map<Integer, OrderLine> longOrderLineMap,
        int fournisseurId,
        CsvImportStrategy strategy
    ) {
        int totalItemCount = 0;
        int succesCount = 0;
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
                    findInCommandeMap(longOrderLineMap, fournisseurProduit.getId()).ifPresentOrElse(
                        orderLine -> {
                            int oldQty = orderLine.getQuantityReceived();
                            int oldTaxAmount = orderLine.getTaxAmount();
                            updateInRecord(commande, orderLine, rec.quantityReceived(), rec.taxAmount(), oldQty, oldTaxAmount, rec.quantityUg());
                            buildLot(orderLine, rec.quantityReceived(), rec.lotNumber(), rec.expirationDate(), 0, null);
                        },
                        () -> createInRecord(
                            longOrderLineMap, commande, fournisseurProduit,
                            rec.quantityRequested(), rec.quantityReceived(),
                            rec.orderCostAmount(), rec.orderUnitPrice(),
                            rec.quantityUg(), currentStock, rec.taxAmount(),
                            rec.lotNumber(), rec.expirationDate(), 0, null
                        )
                    );
                    succesCount++;
                } else {
                    items.add(strategy.onFailure(csvRecord, rec));
                }
            }
        } catch (IOException e) {
            log.debug("{0}", e);
        }

        if (items.isEmpty()) commande.setOrderStatus(OrderStatut.REQUESTED);
        commandeRepository.save(commande);
        int displayCount = strategy.hasHeader() ? totalItemCount - 1 : totalItemCount;
        return buildCommandeResponseDTO(commande, items, displayCount, succesCount);
    }

    private CommandeResponseDTO uploadTXTFormat(Commande commande, MultipartFile multipartFile) {
        List<OrderItem> items = new ArrayList<>();
        Map<Integer, OrderLine> longOrderLineMap = new HashMap<>();
        Integer fournisseurId = commande.getFournisseur().getId();
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
        Map<Integer, OrderLine> longOrderLineMap,
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
        Map<Integer, OrderLine> longOrderLineMap,
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

    private Optional<OrderLine> findInCommandeMap(Map<Integer, OrderLine> longOrderLineMap, Integer fourniseurProduitId) {
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
        orderLineDTO.setFreeQty(quantityUg);
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

    private Optional<OrderLine> findOrderLineInSetOrderLine(List<OrderLine> orderLines, OrderLine orderLineToCheck) {
        return orderLines
            .stream()
            .filter(orderLine -> orderLine.getFournisseurProduit().equals(orderLineToCheck.getFournisseurProduit()))
            .findFirst();
    }

    private void updateCommandeAmount(Commande commande, OrderLine orderLine, Integer oldGrossAmount, Integer oldOrderAmount) {
        commande.setGrossAmount(
            (orderLine.getQuantityRequested() * orderLine.getOrderCostAmount()) + commande.getGrossAmount() - oldGrossAmount
        );
        commande.setFinalAmount(
            (orderLine.getQuantityRequested() * orderLine.getOrderUnitPrice()) + commande.getFinalAmount() - oldOrderAmount
        );
        commande.setOrderAmount(commande.getFinalAmount());
    }

    private void updateCommandeAmount(Commande commande, Integer grossAmount, Integer orderAmount) {
        commande.setGrossAmount(commande.getGrossAmount() + grossAmount);
        commande.setFinalAmount(commande.getFinalAmount() + orderAmount);
        commande.setOrderAmount(commande.getOrderAmount() + orderAmount);
    }

    private Commande updateCommande(Pair<OrderLine, OrderLine> orderLineOrderLinePair) {
        OrderLine oldOrderLine = orderLineOrderLinePair.getFirst();
        OrderLine orderLine = orderLineOrderLinePair.getSecond();
        Commande commande = orderLine.getCommande();
        updateCommandeAmount(
            commande,
            orderLine,
            oldOrderLine.getQuantityRequested() * oldOrderLine.getOrderCostAmount(),
            oldOrderLine.getQuantityRequested() * oldOrderLine.getOrderUnitPrice()
        );
        return commandeRepository.saveAndFlush(commande);
    }

    private Fournisseur buildFournisseurFromId(Integer id) {
        return new Fournisseur().id(id);
    }

    private void updateCommande(Commande commande, OrderLine orderLine) {
        commande.setGrossAmount(orderLine.getGrossAmount() + commande.getGrossAmount());
    }

    private VerificationResponseCommandeDTO verificationCommandeCsv(MultipartFile multipartFile, Commande commande) {
        List<Pair<String, Integer>> records = new ArrayList<>();
        CSVFormat csvFormat = CSVFormat.EXCEL.builder()
            .setDelimiter(';')
            .get();

        try (Reader reader = new InputStreamReader(multipartFile.getInputStream());
             CSVParser parser = CSVParser.builder()
                 .setReader(reader)
                 .setFormat(csvFormat)
                 .get()
        ) {
            for (CSVRecord record : parser) {
                String code = record.get(0);
                int quantityReceived = Integer.parseInt(record.get(3));
                records.add(Pair.of(code, quantityReceived));
            }
            return processVerificationFileContent(commande, records);
        } catch (IOException e) {
            log.debug("{0}", e);
        }
        return new VerificationResponseCommandeDTO();
    }

    private VerificationResponseCommandeDTO processVerificationFileContent(Commande commande, List<Pair<String, Integer>> records) {
        Set<OrderLine> orderLinesToRemove = new HashSet<>();
        Set<OrderLine> orderLinesToSave = new HashSet<>();
        List<VerificationResponseCommandeDTO.Item> items = new ArrayList<>();
        List<VerificationResponseCommandeDTO.Item> extraItems = new ArrayList<>();
        VerificationResponseCommandeDTO verificationResponseCommandeDTO = new VerificationResponseCommandeDTO();
        for (Pair<String, Integer> record : records) {
            proccessFileRecord(commande, record.getFirst(), items, extraItems, record.getSecond(), orderLinesToSave, orderLinesToRemove);
        }
        updateResponseCommandeEnCours(commande, orderLinesToSave, orderLinesToRemove);
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
        List<Pair<String, Integer>> records = new ArrayList<>();
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
                            } catch (Exception ignored) {
                            }
                            break;
                        default:
                            break;
                    }
                    int quantityReceived = (int) qtyquantityReceivedCell.getNumericCellValue();
                    records.add(Pair.of(code, quantityReceived));
                }
            }
            return processVerificationFileContent(commande, records);
        } catch (IOException e) {
            log.debug("{0}", e);
        }
        return new VerificationResponseCommandeDTO();
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
            .setCodeEan(produit.getCodeEanLaboratoire())
            .setQuantite(orderLine.getQuantityRequested())
            .setQuantitePriseEnCompte(quantityReceived);
    }

    private Optional<OrderLine> getOrderLineInCommandeItems(List<OrderLine> orderLines, final String codeCipOrCodeEan) {
        for (OrderLine orderLine : orderLines) {
            FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
            Produit produit = fournisseurProduit.getProduit();

            String codeCip = fournisseurProduit.getCodeCip();
            String codeEan = fournisseurProduit.getCodeEan();
            String codeEanLab = produit.getCodeEanLaboratoire();
            if (
                Objects.equals(codeCipOrCodeEan, codeCip) ||
                    Objects.equals(codeCipOrCodeEan, codeEan) ||
                    Objects.equals(codeCipOrCodeEan, codeEanLab)
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
            commande.getOrderAmount() +
                (orderLine.getQuantityReceived() * orderLine.getOrderUnitPrice()) -
                (oldQuantityReceived * orderLine.getOrderUnitPrice())
        );
        commande.setTaxAmount(commande.getTaxAmount() + orderLine.getTaxAmount() - oldTaxAmount);
    }

    private void updateOrderLineFromRecord(OrderLine orderLine, int quantityReceived, int quantityUg, int taxAmount) {
        orderLine.setFreeQty(orderLine.getFreeQty() + quantityUg);
        orderLine.setQuantityReceived(orderLine.getQuantityReceived() + quantityReceived);
        orderLine.setQuantityRequested(orderLine.getQuantityReceived());
        orderLine.setTaxAmount(orderLine.getTaxAmount() + taxAmount);
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
                    new Lot()
                        .setOrderLine(orderLine)
                        .setProduit(orderLine.getFournisseurProduit().getProduit())
                        .setNumLot(lotNumber)
                        .setFreeQty(freeQuantity)
                        .setExpiryDate(expirationDate)
                        .setQuantity(quantity)
                        .setManufacturingDate(manufacturingDate)
                );
        }
    }

    private void buildNew(Suggestion suggestion) {
        AppUser user = storageService.getUser();
        Commande commande = new Commande();
        commande.setId(this.commandeIdGeneratorService.getNextIdAsInt());
        Fournisseur fournisseur=suggestion.getFournisseur();
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(commande.getCreatedAt());
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setUser(user);
        commande.setOrderReference(referenceService.buildNumCommande());
        commande.setReceiptReference(commande.getOrderReference());
        commande.setGrossAmount(0);
        commande.setOrderAmount(0);
        commande.setFournisseur(fournisseur);
        suggestion
            .getSuggestionLines()
            .forEach(suggestionLine -> {
                OrderLine orderLine = orderLineService.buildOrderLine(suggestionLine,fournisseur.getId());
                orderLine.setCommande(commande);
                updateCommandeAmount(commande, orderLine);
                commande.getOrderLines().add(orderLine);
            });
        commandeRepository.save(commande);
    }

    @Override
    public void importSuggestionIntoCommande(CommandeId commandeId, Integer suggestionId) {
        Commande commande = findCommandeById(commandeId);
        Integer  fournisseurId = commande.getFournisseur().getId();
        Suggestion suggestion = suggestionRepository.findById(suggestionId).orElseThrow();
        suggestion
            .getSuggestionLines()
            .forEach(suggestionLine -> {
                OrderLine orderLine = orderLineService.buildOrderLine(suggestionLine,fournisseurId);
                Optional<OrderLine> existing = findOrderLineInSetOrderLine(
                    commande.getOrderLines(),
                    orderLine
                );
                if (existing.isPresent()) {

                    orderLine = existing.get();
                    System.err.println("Duplicate order line " + orderLine.getFournisseurProduit().getCodeCip());
                    int oldGross = orderLine.getQuantityRequested() * orderLine.getOrderCostAmount();
                    int oldOrder = orderLine.getQuantityRequested() * orderLine.getOrderUnitPrice();
                    orderLine.setQuantityRequested(orderLine.getQuantityRequested() + suggestionLine.getQuantity());
                    orderLineService.save(orderLine);
                    updateCommandeAmount(commande, orderLine, oldGross, oldOrder);
                } else {
                    System.err.println("New order line " + orderLine.getFournisseurProduit().getCodeCip());
                    orderLine.setCommande(commande);
                    orderLineService.save(orderLine);
                    updateCommandeAmount(commande, orderLine);
                    commande.getOrderLines().add(orderLine);
                }
            });
        commande.setUpdatedAt(LocalDateTime.now());
        commandeRepository.save(commande);
    }

    @Override
    public void changeGrossiste(CommandeDTO commandeDTO) {
        Commande commande = findCommandeById(commandeDTO.getCommandeId());
        Fournisseur fournisseur = new Fournisseur().id(commandeDTO.getFournisseurId());
        commande.setFournisseur(fournisseur);
        commande.setUpdatedAt(LocalDateTime.now());
        commande.setGrossAmount(0);
        commande.setOrderAmount(0);
        commande
            .getOrderLines()
            .forEach(orderLine -> {
                this.orderLineService.changeFournisseurProduit(orderLine, fournisseur.getId());
                updateCommandeAmount(commande, orderLine);
            });
        commandeRepository.save(commande);
    }

    private void updateCommandeAmount(Commande commande, OrderLine orderLine) {
        commande.setGrossAmount(commande.getGrossAmount() + (orderLine.getOrderCostAmount() * orderLine.getQuantityRequested()));
        commande.setOrderAmount(commande.getOrderAmount() + (orderLine.getOrderUnitPrice() * orderLine.getQuantityRequested()));
    }
}
