package com.kobe.warehouse.service;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.GammeProduit;
import com.kobe.warehouse.domain.Importation;
import com.kobe.warehouse.domain.Laboratoire;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tableau;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.TypeEtiquette;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.domain.enumeration.ImportationStatus;
import com.kobe.warehouse.domain.enumeration.ImportationType;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.repository.FamilleProduitRepository;
import com.kobe.warehouse.repository.FormProduitRepository;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.GammeProduitRepository;
import com.kobe.warehouse.repository.ImportationRepository;
import com.kobe.warehouse.repository.LaboratoireRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.TableauRepository;
import com.kobe.warehouse.repository.TvaRepository;
import com.kobe.warehouse.repository.TypeEtiquetteRepository;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.InstallationDataDTO;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.TypeImportationProduit;
import com.kobe.warehouse.service.errors.FileStorageException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

@Service
public class ImportationProduitService {

    private final Logger log = LoggerFactory.getLogger(ImportationProduitService.class);
    private final FamilleProduitRepository familleProduitRepository;
    private final RayonRepository rayonRepository;
    private final TypeEtiquetteRepository typeEtiquetteRepository;
    private final TvaRepository tvaRepository;
    private final FournisseurRepository fournisseurRepository;
    private final GammeProduitRepository gammeProduitRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final FormProduitRepository formProduitRepository;
    private final StorageService storageService;
    private final ProduitRepository produitRepository;
    private final TransactionTemplate transactionTemplate;
    private final StockProduitRepository stockProduitRepository;
    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final ImportationRepository importationRepository;
    private final TableauRepository tableauRepository;

    private final String DEFAULT_CODE_FAMILLE = "1000";
    private final Path fileStorageLocation;

    public ImportationProduitService(
        TransactionTemplate transactionTemplate,
        FamilleProduitRepository familleProduitRepository,
        RayonRepository rayonRepository,
        TypeEtiquetteRepository typeEtiquetteRepository,
        TvaRepository tvaRepository,
        FournisseurRepository fournisseurRepository,
        GammeProduitRepository gammeProduitRepository,
        LaboratoireRepository laboratoireRepository,
        FormProduitRepository formProduitRepository,
        ProduitRepository produitRepository,
        StorageService storageService,
        StockProduitRepository stockProduitRepository,
        FournisseurProduitRepository fournisseurProduitRepository,
        ImportationRepository importationRepository,
        TableauRepository tableauRepository,
        FileStorageProperties fileStorageProperties
    ) {
        this.familleProduitRepository = familleProduitRepository;
        this.rayonRepository = rayonRepository;
        this.typeEtiquetteRepository = typeEtiquetteRepository;
        this.tvaRepository = tvaRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.gammeProduitRepository = gammeProduitRepository;
        this.laboratoireRepository = laboratoireRepository;
        this.formProduitRepository = formProduitRepository;
        this.produitRepository = produitRepository;
        this.transactionTemplate = transactionTemplate;
        this.stockProduitRepository = stockProduitRepository;
        this.fournisseurProduitRepository = fournisseurProduitRepository;
        this.importationRepository = importationRepository;
        this.storageService = storageService;
        this.tableauRepository = tableauRepository;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getReportsDir() + "/installation/rejets").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Async
    public void updateStocFromJSON(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Storage storage = storageService.getDefaultMagasinMainStorage();
        AtomicInteger errorSize = new AtomicInteger(0);
        AtomicInteger size = new AtomicInteger(0);
        List<ProduitDTO> list = mapper.readValue(input, new TypeReference<>() {});
        int totalSize = list.size();
        log.info("size===>> {}", list.size());
        transactionTemplate.setPropagationBehavior(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        Importation importation = importation(storageService.getUserFormImport());
        importation.setTotalZise(totalSize);
        saveImportation(importation);
        for (ProduitDTO p : list) {
            try {
                processImportation(p, storage, errorSize, size);
                updateImportation(errorSize.get(), size.get());
            } catch (Exception e) {
                log.debug("updateStocFromJSON ===>> {}", e);
            }
        }
        updateImportation(errorSize.get(), size.get());
    }

    void processImportation(final ProduitDTO p, Storage storage, AtomicInteger errorSize, AtomicInteger size) {
        transactionTemplate.execute(
            new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        StockProduit stockProduit = buidStockProduit(p, storage);
                        Produit produit = buildProduit(p);
                        produit.setRayonProduits(Set.of(fromRayonCode(p.getRayonLibelle(), storage).setProduit(produit)));
                        produit = produitRepository.save(produit);
                        stockProduit.setProduit(produit);
                        stockProduitRepository.save(stockProduit);
                        for (FournisseurProduitDTO d : p.getFournisseurProduits()) {
                            FournisseurProduit fournisseurProduit = buildFournisseurProduit(d);
                            fournisseurProduit.setProduit(produit);
                            produit.addFournisseurProduit(fournisseurProduit);
                            fournisseurProduitRepository.save(fournisseurProduit);
                        }
                        if (!p.getProduits().isEmpty()) {
                            ProduitDTO detail = p.getProduits().getFirst();
                            Produit produitDetail = buildDeatilProduit(detail, produit);
                            produitDetail.setRayonProduits(Set.of(fromRayonCode(p.getRayonLibelle(), storage).setProduit(produitDetail)));
                            StockProduit stockProduitDetail = buidStockProduit(detail, storage);
                            produitDetail = produitRepository.save(produitDetail);
                            stockProduitDetail.setProduit(produitDetail);
                            stockProduitRepository.save(stockProduitDetail);
                            for (FournisseurProduitDTO d : detail.getFournisseurProduits()) {
                                FournisseurProduit fournisseurProduitDetail = buildFournisseurProduit(d);
                                fournisseurProduitDetail.setProduit(produitDetail);
                                fournisseurProduitRepository.save(fournisseurProduitDetail);
                            }
                        }
                        size.incrementAndGet();
                    } catch (Exception e) {
                        log.debug("processImportation ===>> {}", e);
                        errorSize.incrementAndGet();
                    }
                }
            }
        );
    }

    private Produit buildDeatilProduit(ProduitDTO produitDTO, Produit parent) {
        Produit produit = new Produit();
        produit.setTypeProduit(TypeProduit.DETAIL);
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setNetUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCreatedAt(LocalDateTime.now());
        produit.setParent(parent);
        produit.setUpdatedAt(produit.getCreatedAt());
        produit.setCostAmount(produitDTO.getCostAmount());
        produit.setItemCostAmount(produitDTO.getCostAmount());
        produit.setItemQty(1);
        produit.setItemRegularUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCodeEan(produitDTO.getCodeEan());
        produit.setCheckExpiryDate(produitDTO.getDateperemption());
        produit.setDeconditionnable(false);
        produit.setQtyAppro(0);
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());
        produit.setPerimeAt(produitDTO.getPerimeAt());
        produit.setTva(parent.getTva());
        produit.setLaboratoire(parent.getLaboratoire());
        produit.setFamille(parent.getFamille());
        produit.setGamme(produit.getGamme());
        produit.setForme(parent.getForme());
        return produit;
    }

    private RayonProduit fromRayonCode(String code, Storage storage) {
        Optional<Rayon> optionalRayon = rayonRepository.findFirstByCodeAndStorageId(code, storage.getId());
        return new RayonProduit()
            .setRayon(
                optionalRayon.orElse(
                    rayonRepository.findFirstByLibelleAndStorageId(EntityConstant.SANS_EMPLACEMENT_LIBELLE, storage.getId()).orElse(null)
                )
            );
    }

    private Produit buildProduit(ProduitDTO produitDTO) {
        Produit produit = new Produit();
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setNetUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setTypeProduit(TypeProduit.PACKAGE);
        produit.setCreatedAt(LocalDateTime.now());
        produit.setUpdatedAt(produit.getCreatedAt());
        produit.setCostAmount(produitDTO.getCostAmount());
        produit.setItemCostAmount(produitDTO.getItemCostAmount());
        produit.setItemQty(produitDTO.getItemQty());
        produit.setItemRegularUnitPrice(produitDTO.getItemRegularUnitPrice());
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCodeEan(produitDTO.getCodeEan());
        produit.setCheckExpiryDate(produitDTO.getDateperemption());
        produit.setDeconditionnable(produitDTO.getDeconditionnable());
        produit.setQtyAppro(produitDTO.getQtyAppro());
        produit.setQtySeuilMini(produitDTO.getQtySeuilMini());
        produit.setPerimeAt(produitDTO.getPerimeAt());
        produit.setCmuAmount(produitDTO.getCmuAmount());
        if (ObjectUtils.isNotEmpty(produitDTO.getTvaTaux())) {
            tvaRepository.findFirstByTauxEquals(produitDTO.getTvaTaux()).ifPresent(produit::setTva);
        }
        if (StringUtils.isNotEmpty(produitDTO.getLaboratoireLibelle())) {
            laboratoireRepository.findFirstByLibelleEquals(produitDTO.getLaboratoireLibelle()).ifPresent(produit::setLaboratoire);
        }
        if (StringUtils.isNotEmpty(produitDTO.getFamilleLibelle())) {
            familleProduitRepository.findFirstByLibelleEquals(produitDTO.getFamilleLibelle()).ifPresent(produit::setFamille);
        }
        if (StringUtils.isNotEmpty(produitDTO.getGammeLibelle())) {
            gammeProduitRepository.findFirstByLibelleEquals(produitDTO.getGammeLibelle()).ifPresent(produit::setGamme);
        }

        if (StringUtils.isNotEmpty(produitDTO.getFormeLibelle())) {
            formProduitRepository.findFirstByLibelleEquals(produitDTO.getFormeLibelle()).ifPresent(produit::setForme);
        }

        return produit;
    }

    private StockProduit buidStockProduit(ProduitDTO p, Storage storage) {
        return new StockProduit()
            .qtyStock(p.getTotalQuantity())
            .qtyUG(p.getQtyUG())
            .qtyVirtual(p.getTotalQuantity())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .setStorage(storage);
    }

    private FournisseurProduit buildFournisseurProduit(FournisseurProduitDTO p) {
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setFournisseur(fournisseurRepository.findFirstByLibelleEquals(p.getFournisseurLibelle()).orElse(null));
        fournisseurProduit.setPrixUni(p.getPrixUni());
        fournisseurProduit.setPrixAchat(p.getPrixAchat());
        fournisseurProduit.setPrincipal(p.isPrincipal());
        fournisseurProduit.setCodeCip(p.getCodeCip());
        return fournisseurProduit;
    }

    private Importation importation(User user) {
        Importation importation = new Importation();
        importation.setImportationStatus(ImportationStatus.PROCESSING);
        importation.setImportationType(ImportationType.STOCK_PRODUIT);
        importation.setCreated(LocalDateTime.now());
        importation.setUser(user);
        return importation;
    }

    private void saveImportation(Importation importation) {
        transactionTemplate.execute(
            new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        importationRepository.save(importation);
                    } catch (Exception e) {
                        log.debug("saveImportation ===>> {}", e);
                    }
                }
            }
        );
    }

    private void updateImportation(final int errorSize, final int size) {
        transactionTemplate.execute(
            new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        Importation importation = importationRepository.findFirstByImportationTypeOrderByCreatedDesc(
                            ImportationType.STOCK_PRODUIT
                        );
                        if (importation != null) {
                            importation.setUpdated(LocalDateTime.now());
                            importation.setSize(size);
                            importation.setErrorSize(errorSize);
                            importation.setImportationStatus(
                                errorSize > 0 ? ImportationStatus.COMPLETED_ERRORS : ImportationStatus.COMPLETED
                            );
                            importationRepository.save(importation);
                        }
                    } catch (Exception e) {
                        log.debug("saveImportation ===>> {}", e);
                    }
                }
            }
        );
    }

    public Importation current(ImportationType importationType) {
        return importationRepository.findFirstByImportationTypeOrderByCreatedDesc(importationType);
    }

    @Transactional
    public ResponseDTO installNewOfficine(InputStream inputStream, InstallationDataDTO installationData) {
        Fournisseur fournisseur = fournisseurRepository.findById(installationData.getFournisseurId()).orElse(null);
        return switch (installationData.getTypeImportation()) {
            case NOUVELLE_INSTALLATION -> faireNouvelleInstallation(inputStream, fournisseur);
            case BASCULEMENT -> faireBasculement(inputStream, fournisseur);
            case BASCULEMENT_PRESTIGE -> faireBasculementPrestige(inputStream, fournisseur);
        };
    }

    private ResponseDTO faireNouvelleInstallation(InputStream inputStream, Fournisseur fournisseur) {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        Storage storage = storageService.getDefaultMagasinMainStorage();
        Rayon rayon = rayonRepository.findFirstByCodeAndStorageId(EntityConstant.SANS_EMPLACEMENT_CODE, storage.getId()).orElseThrow();
        AtomicInteger count = new AtomicInteger();
        List<Record> errorList = new ArrayList<>();
        AtomicInteger errorSize = new AtomicInteger();
        FamilleProduit familleProduit = findFamilleProduit(DEFAULT_CODE_FAMILLE);
        TypeEtiquette typeEtiquette = typeEtiquetteRepository.getReferenceById(EntityConstant.DEFAULT_TYPE_ETIQUETTES);
        Map<String, Long> tableauCode =
            this.tableauRepository.findAll().stream().collect(HashMap::new, (map, t) -> map.put(t.getCode(), t.getId()), HashMap::putAll);
        ResponseDTO response = new ResponseDTO();
        Map<Integer, Long> codeTvaMap =
            this.tvaRepository.findAll().stream().collect(HashMap::new, (map, t) -> map.put(t.getTaux(), t.getId()), HashMap::putAll);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').build().parse(br);
            records.forEach(record -> {
                transactionTemplate.execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            Record produitRecord = null;
                            try {
                                var codeTva = record.get(5);
                                var cip = record.get(0);

                                Long tvaId = null;
                                if (org.springframework.util.StringUtils.hasText(codeTva)) {
                                    tvaId = switch (codeTva) {
                                        case "1" -> codeTvaMap.get(18);
                                        case "4" -> codeTvaMap.get(9);
                                        default -> codeTvaMap.get(0);
                                    };
                                }
                                var tab = record.get(6);
                                produitRecord = new Record(
                                    org.springframework.util.StringUtils.hasText(tab) ? tableauCode.get(tab) : null,
                                    cip,
                                    cip.length() > 8 ? cip : null,
                                    record.get(1),
                                    familleProduit.getId(),
                                    rayon.getId(),
                                    null,
                                    fournisseur.getId(),
                                    Integer.parseInt(record.get(3)),
                                    Integer.parseInt(record.get(2)),
                                    Integer.parseInt(record.get(4)),
                                    tvaId,
                                    0,
                                    0,
                                    null,
                                    null,
                                    typeEtiquette.getId(),
                                    null,
                                    null,
                                    0,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    TypeImportationProduit.NOUVELLE_INSTALLATION
                                );
                                saveRecord(produitRecord, storage);
                                count.incrementAndGet();
                            } catch (Exception e) {
                                errorList.add(produitRecord);
                                errorSize.incrementAndGet();
                                log.error("saveproduit ===>> {}", e);
                            }
                        }
                    }
                );
            });
            response.setErrorSize(errorSize.get()).size(count.get());
            exportLigneRejeteesToCsv(response, errorList);
        } catch (IOException e) {
            log.error("importation : {0}", e);
        }

        return response;
    }

    private ResponseDTO faireBasculement(InputStream inputStream, Fournisseur fournisseur) {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        Storage storage = storageService.getDefaultMagasinMainStorage();
        Rayon rayon = rayonRepository.findFirstByCodeAndStorageId(EntityConstant.SANS_EMPLACEMENT_CODE, storage.getId()).orElseThrow();
        ResponseDTO response = new ResponseDTO();
        List<Record> errorList = new ArrayList<>();
        AtomicInteger errorSize = new AtomicInteger();
        Long defaultRayonId = rayon.getId();
        Map<String, Long> rayonCodes =
            this.rayonRepository.findAll().stream().collect(HashMap::new, (map, t) -> map.put(t.getCode(), t.getId()), HashMap::putAll);
        AtomicInteger count = new AtomicInteger();
        TypeEtiquette typeEtiquette = typeEtiquetteRepository.getReferenceById(EntityConstant.DEFAULT_TYPE_ETIQUETTES);
        Map<String, Long> familleCode =
            this.familleProduitRepository.findAll()
                .stream()
                .collect(HashMap::new, (map, t) -> map.put(t.getCode(), t.getId()), HashMap::putAll);
        Map<Integer, Long> codeTvaMap =
            this.tvaRepository.findAll().stream().collect(HashMap::new, (map, t) -> map.put(t.getTaux(), t.getId()), HashMap::putAll);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').build().parse(br);
            records.forEach(record -> {
                transactionTemplate.execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            Record produitRecord = null;
                            try {
                                var codeTva = record.get(5);
                                Long tvaId = null;
                                if (org.springframework.util.StringUtils.hasText(codeTva)) {
                                    tvaId = switch (codeTva) {
                                        case "1" -> codeTvaMap.get(18);
                                        case "4" -> codeTvaMap.get(9);
                                        default -> codeTvaMap.get(0);
                                    };
                                }

                                var codeFamille = record.get(6);
                                Long familleId = null;
                                if (org.springframework.util.StringUtils.hasText(codeFamille)) {
                                    familleId = switch (codeFamille) {
                                        case "2" -> familleCode.get("2000");
                                        case "3" -> familleCode.get("3000");
                                        case "4" -> familleCode.get("4000");
                                        case "5" -> familleCode.get("5000");
                                        case "6" -> familleCode.get("6000");
                                        case "7" -> familleCode.get("7000");
                                        case "8" -> familleCode.get("8000");
                                        case "9" -> familleCode.get("9000");
                                        default -> familleCode.get("1000");
                                    };
                                }
                                var rayonCode = record.get(7);
                                var cip = record.get(0);
                                Long rayonId = defaultRayonId;
                                if (rayonCodes.containsKey(rayonCode)) {
                                    rayonId = rayonCodes.get(rayonCode);
                                }
                                produitRecord = new Record(
                                    null,
                                    cip,
                                    cip.length() > 8 ? cip : null,
                                    record.get(1),
                                    familleId,
                                    rayonId,
                                    record.get(4),
                                    fournisseur.getId(),
                                    Integer.parseInt(record.get(3)),
                                    Integer.parseInt(record.get(2)),
                                    Integer.parseInt(record.get(8)),
                                    tvaId,
                                    Integer.parseInt(record.get(9)),
                                    Integer.parseInt(record.get(10)),
                                    null,
                                    null,
                                    typeEtiquette.getId(),
                                    null,
                                    null,
                                    0,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    TypeImportationProduit.BASCULEMENT
                                );
                                saveRecord(produitRecord, storage);
                                count.incrementAndGet();
                            } catch (Exception e) {
                                errorList.add(produitRecord);
                                errorSize.incrementAndGet();
                                log.error("processImportation ===>> {}", e);
                            }
                        }
                    }
                );
            });
            response.setErrorSize(errorSize.get()).size(count.get());
            exportLigneRejeteesToCsv(response, errorList);
        } catch (IOException e) {
            log.error("importation : {0}", e);
        }

        return response;
    }

    private ResponseDTO faireBasculementPrestige(InputStream inputStream, Fournisseur fournisseur) {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        Storage storage = storageService.getDefaultMagasinMainStorage();
        Rayon rayon = rayonRepository.findFirstByCodeAndStorageId(EntityConstant.SANS_EMPLACEMENT_CODE, storage.getId()).orElseThrow();
        ResponseDTO response = new ResponseDTO();
        List<Record> errorList = new ArrayList<>();
        Map<String, Long> tableauCode =
            this.tableauRepository.findAll().stream().collect(HashMap::new, (map, t) -> map.put(t.getCode(), t.getId()), HashMap::putAll);

        Long defaultRayonId = rayon.getId();
        Map<String, Long> rayonCodes =
            this.rayonRepository.findAll().stream().collect(HashMap::new, (map, t) -> map.put(t.getCode(), t.getId()), HashMap::putAll);
        AtomicInteger count = new AtomicInteger();
        AtomicInteger errorSize = new AtomicInteger();
        TypeEtiquette typeEtiquette = typeEtiquetteRepository.getReferenceById(EntityConstant.DEFAULT_TYPE_ETIQUETTES);
        Map<String, Long> familleCode =
            this.familleProduitRepository.findAll()
                .stream()
                .collect(HashMap::new, (map, t) -> map.put(t.getCode(), t.getId()), HashMap::putAll);
        Map<Integer, Long> codeTvaMap =
            this.tvaRepository.findAll().stream().collect(HashMap::new, (map, t) -> map.put(t.getTaux(), t.getId()), HashMap::putAll);
        Map<String, Long> fournisseurCodes =
            this.fournisseurRepository.findAll()
                .stream()
                .collect(HashMap::new, (map, t) -> map.put(t.getCode(), t.getId()), HashMap::putAll);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').build().parse(br);
            records.forEach(record -> {
                var index = count.get();
                if (index == 0) {
                    count.incrementAndGet();
                    return;
                }
                transactionTemplate.execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            Record produitRecord = null;
                            try {
                                //2175198
                                var codeTva = record.get(11);
                                Long tvaId;
                                if (
                                    org.springframework.util.StringUtils.hasText(codeTva) &&
                                    codeTvaMap.containsKey(NumberUtil.parseInt(codeTva))
                                ) {
                                    tvaId = codeTvaMap.get(NumberUtil.parseInt(codeTva));
                                } else {
                                    tvaId = codeTvaMap.get(0);
                                }
                                var codeFamille = record.get(15);
                                Long familleId;
                                if (familleCode.containsKey(codeFamille)) {
                                    familleId = familleCode.get(codeFamille);
                                } else {
                                    familleId = familleCode.get(DEFAULT_CODE_FAMILLE);
                                }
                                var rayonCode = record.get(14);
                                Long rayonId = defaultRayonId;
                                if (rayonCodes.containsKey(rayonCode)) {
                                    rayonId = rayonCodes.get(rayonCode);
                                }
                                Long fournisseurId = fournisseurCodes.get(record.get(16));
                                if (fournisseurId == null) {
                                    fournisseurId = fournisseur.getId();
                                }

                                var tab = record.get(13);
                                var labo = record.get(19);
                                var gamme = record.get(20);
                                var chiffre = record.get(8);
                                var cmu = record.get(10);
                                var produitAvecOrdance = record.get(9);
                                var checkExpiryDate = record.get(17);
                                var perimeAt = record.get(18);
                                var itemNumber = NumberUtil.parseInt(record.get(21));
                                var detailIndex = record.get(22);
                                var deconQty = NumberUtil.parseInt(detailIndex);
                                var prixUniDetail = NumberUtil.parseInt(record.get(23));
                                var prixAchatDetail = NumberUtil.parseInt(record.get(24));
                                produitRecord = new Record(
                                    org.springframework.util.StringUtils.hasText(tab) ? tableauCode.get(tab) : null,
                                    record.get(0),
                                    record.get(1),
                                    record.get(2),
                                    familleId,
                                    rayonId,
                                    record.get(12),
                                    fournisseurId,
                                    NumberUtil.parseInt(record.get(3)),
                                    NumberUtil.parseInt(record.get(4)),
                                    NumberUtil.parseInt(record.get(5)),
                                    tvaId,
                                    NumberUtil.parseInt(record.get(6)),
                                    NumberUtil.parseInt(record.get(7)),
                                    org.springframework.util.StringUtils.hasText(labo) ? tableauCode.get(labo) : null,
                                    org.springframework.util.StringUtils.hasText(gamme) ? tableauCode.get(gamme) : null,
                                    typeEtiquette.getId(),
                                    org.springframework.util.StringUtils.hasText(chiffre) ? Integer.parseInt(chiffre) == 1 : null,
                                    org.springframework.util.StringUtils.hasText(produitAvecOrdance)
                                        ? NumberUtil.parseInt(produitAvecOrdance) == 1
                                        : null,
                                    org.springframework.util.StringUtils.hasText(cmu) ? Integer.parseInt(cmu) : 0,
                                    org.springframework.util.StringUtils.hasText(checkExpiryDate)
                                        ? NumberUtil.parseInt(checkExpiryDate) == 1
                                        : null,
                                    org.springframework.util.StringUtils.hasText(perimeAt) ? LocalDate.parse(perimeAt) : null,
                                    itemNumber > 0 ? itemNumber : null,
                                    deconQty,
                                    prixUniDetail,
                                    prixAchatDetail,
                                    TypeImportationProduit.BASCULEMENT_PRESTIGE
                                );

                                Produit produit = saveRecord(produitRecord, storage);

                                if (org.springframework.util.StringUtils.hasText(detailIndex)) {
                                    saveRecordDetail(produitRecord, produit, storage);
                                }

                                count.incrementAndGet();
                            } catch (Exception e) {
                                errorSize.incrementAndGet();
                                errorList.add(produitRecord);
                                log.error("save produit ===>> {}", e);
                            }
                        }
                    }
                );
            });
            response.setErrorSize(errorSize.get()).size(count.get());
            exportLigneRejeteesToCsv(response, errorList);
        } catch (IOException e) {
            log.error("importation : {0}", e);
        }

        return response;
    }

    private FamilleProduit findFamilleProduit(String code) {
        return familleProduitRepository.findByCodeEquals(code);
    }

    private Produit saveRecord(Record produitRecord, Storage storage) {
        StockProduit stockProduit = buidStockProduit(produitRecord, storage);
        Produit produit = buildProduit(produitRecord);
        produit.setRayonProduits(Set.of(new RayonProduit().setRayon(new Rayon().id(produitRecord.rayonId())).setProduit(produit)));
        produit = produitRepository.save(produit);
        stockProduit.setProduit(produit);
        stockProduitRepository.save(stockProduit);
        FournisseurProduit fournisseurProduit = buildFournisseurProduit(produitRecord);
        fournisseurProduit.setProduit(produit);
        this.fournisseurProduitRepository.save(fournisseurProduit);
        return produit;
    }

    private void saveRecordDetail(Record v, Produit parent, Storage storage) {
        StockProduit stockProduit = buidStockProduit(v, storage);
        Produit produit = buildDeatilProduit(v, parent);
        produit.setRayonProduits(Set.of(new RayonProduit().setRayon(new Rayon().id(v.rayonId())).setProduit(produit)));
        produit = produitRepository.save(produit);
        stockProduit.setProduit(produit);
        stockProduitRepository.save(stockProduit);
        FournisseurProduit fournisseurProduit = buildFournisseurProduit(v);
        fournisseurProduit.setProduit(produit);
        fournisseurProduit.setCodeCip(v.cip().concat("D"));
        fournisseurProduit.setPrixUni(v.prixUniDetail());
        fournisseurProduit.setPrixAchat(v.prixAchatDetail());
        fournisseurProduitRepository.save(fournisseurProduit);
    }

    private StockProduit buidStockProduit(Record record, Storage storage) {
        return new StockProduit()
            .qtyStock(record.qty())
            .qtyUG(0)
            .qtyVirtual(record.qty())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .setStorage(storage);
    }

    private FournisseurProduit buildFournisseurProduit(Record record) {
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setFournisseur(this.fournisseurRepository.getReferenceById(record.fournisseurId()));
        fournisseurProduit.setPrixUni(record.prixVente());
        fournisseurProduit.setPrixAchat(record.prixAchat());
        fournisseurProduit.setPrincipal(true);
        fournisseurProduit.setCodeCip(record.cip());
        return fournisseurProduit;
    }

    private Produit buildProduit(Record record) {
        Produit produit = buildCommonProduit(record);
        produit.setDeconditionnable(true);
        produit.setTypeProduit(TypeProduit.PACKAGE);

        produit.setTva(new Tva().setId(record.tvaId()));
        produit.setFamille(new FamilleProduit().setId(record.familleId()));
        if (nonNull(record.tableauId())) {
            produit.setTableau(new Tableau().setId(record.tableauId()));
        }
        if (nonNull(record.laboratoireId())) {
            produit.setLaboratoire(new Laboratoire().setId(record.laboratoireId()));
        }
        if (nonNull(record.gammeId())) {
            produit.setGamme(new GammeProduit().setId(record.gammeId()));
        }

        if (nonNull(record.itemNumber())) {
            produit.setItemQty(record.itemNumber());
        }

        return produit;
    }

    private Produit buildCommonProduit(Record record) {
        Produit produit = new Produit();
        produit.setLibelle(record.nom().trim().toUpperCase());
        produit.setNetUnitPrice(record.prixVente());
        produit.setCreatedAt(LocalDateTime.now());
        produit.setUpdatedAt(produit.getCreatedAt());
        produit.setCostAmount(record.prixAchat());
        produit.setItemCostAmount(0);
        produit.setItemQty(0);
        produit.setItemRegularUnitPrice(0);
        produit.setRegularUnitPrice(record.prixVente());
        produit.setCheckExpiryDate(false);
        produit.setQtyAppro(record.qtyReappro());
        produit.setQtySeuilMini(record.seuil());
        produit.setCmuAmount(record.cmu());

        if (org.springframework.util.StringUtils.hasText(record.codeRemise())) {
            produit.setCodeRemise(CodeRemise.fromValue(record.codeRemise()));
        }

        if (nonNull(record.checkExpiryDate())) {
            produit.setCheckExpiryDate(record.checkExpiryDate());
        }
        if (nonNull(record.perimeAt())) {
            produit.setPerimeAt(record.perimeAt());
        }
        if (nonNull(record.chiffre())) {
            produit.setChiffre(record.chiffre());
        }
        if (nonNull(record.scheduled())) {
            produit.setScheduled(record.scheduled());
        }
        if (org.springframework.util.StringUtils.hasText(record.codeEan())) {
            produit.setCodeEan(record.codeEan());
        }

        return produit;
    }

    private Produit buildDeatilProduit(Record record, Produit parent) {
        Produit produit = buildCommonProduit(record);
        produit.setLibelle(record.nom().concat("-DET"));
        produit.setTypeProduit(TypeProduit.DETAIL);
        produit.setParent(parent);
        produit.setItemQty(1);
        produit.setDeconditionnable(false);
        produit.setQtyAppro(0);
        produit.setQtySeuilMini(record.seuil());
        produit.setTva(parent.getTva());
        produit.setLaboratoire(parent.getLaboratoire());
        produit.setFamille(parent.getFamille());
        produit.setGamme(produit.getGamme());
        produit.setForme(parent.getForme());
        return produit;
    }

    private void exportLigneRejeteesToCsv(ResponseDTO response, List<Record> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        log.info("Writing data to the csv printer");
        String filename = "importation_produit_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) + ".csv";
        String p = this.fileStorageLocation.resolve(filename).toFile().getAbsolutePath();
        try (
            final FileWriter writer = new FileWriter(p);
            final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setDelimiter(";").build())
        ) {
            printer.printRecord(
                "Type Importation",
                "Tableau",
                "Cip",
                "Code Ean",
                "Nom",
                "Famille id",
                "Rayon id",
                "Code Remise",
                "Fournisseur id",
                "Prix Achat",
                "Prix Vente",
                "Stock",
                "Tva",
                "Seuil",
                "Qté Reappro",
                "Laboratoire id",
                "Gamme id",
                "Type Etiquette id",
                "Chiffre",
                "Produit ordonnancé",
                "Cmu",
                "Date péremtion",
                "Date péremption",
                "Nbre detail",
                "Qte detail",
                "Prix unitaire detail",
                "Prix achat detail"
            );
            records
                .stream()
                .filter(Objects::nonNull)
                .forEach(record -> {
                    try {
                        printer.printRecord(
                            record.typeImportationProduit(),
                            record.tableauId(),
                            record.cip(),
                            record.codeEan(),
                            record.nom(),
                            record.familleId(),
                            record.rayonId(),
                            record.codeRemise(),
                            record.fournisseurId(),
                            record.prixAchat(),
                            record.prixVente(),
                            record.qty(),
                            record.tvaId(),
                            record.seuil(),
                            record.qtyReappro(),
                            record.laboratoireId(),
                            record.gammeId(),
                            record.typeEtiquetteId(),
                            record.chiffre(),
                            record.scheduled(),
                            record.cmu(),
                            record.checkExpiryDate(),
                            record.perimeAt(),
                            record.itemNumber(),
                            record.itmQty(),
                            record.prixUniDetail(),
                            record.prixAchatDetail()
                        );
                    } catch (IOException e) {
                        log.error("Error writing data to the csv printer", e);
                    }
                });

            printer.flush();
        } catch (final IOException e) {
            throw new RuntimeException("Csv writing error: " + e.getMessage());
        }
        response.setRejectFileUrl(filename);
    }

    public Resource getRejets(String nomFichier) {
        Path path = this.fileStorageLocation.toAbsolutePath().resolve(nomFichier).normalize();

        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new GenericError("Le fichier n'existe pas ", "duplicateProvider");
        }
    }

    private record Record(
        Long tableauId,
        String cip,
        String codeEan,
        String nom,
        Long familleId,
        Long rayonId,
        String codeRemise,
        Long fournisseurId,
        int prixAchat,
        int prixVente,
        int qty,
        Long tvaId,
        int seuil,
        int qtyReappro,
        Long laboratoireId,
        Long gammeId,
        Long typeEtiquetteId,
        Boolean chiffre,
        Boolean scheduled,
        int cmu,
        Boolean checkExpiryDate,
        LocalDate perimeAt,
        Integer itemNumber,
        Integer itmQty,
        Integer prixUniDetail,
        Integer prixAchatDetail,
        TypeImportationProduit typeImportationProduit
    ) {}
}
