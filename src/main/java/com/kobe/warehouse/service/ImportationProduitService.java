package com.kobe.warehouse.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Importation;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.User;
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
import com.kobe.warehouse.repository.RemiseProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.TvaRepository;
import com.kobe.warehouse.repository.TypeEtiquetteRepository;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.ProduitDTO;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final RemiseProduitRepository remiseProduitRepository;
    private final StorageService storageService;
    private final ProduitRepository produitRepository;
    private final TransactionTemplate transactionTemplate;
    private final StockProduitRepository stockProduitRepository;
    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final ImportationRepository importationRepository;

    public ImportationProduitService(TransactionTemplate transactionTemplate,
        FamilleProduitRepository familleProduitRepository,
        RayonRepository rayonRepository,
        TypeEtiquetteRepository typeEtiquetteRepository,
        TvaRepository tvaRepository,
        FournisseurRepository fournisseurRepository,
        GammeProduitRepository gammeProduitRepository,
        LaboratoireRepository laboratoireRepository,
        FormProduitRepository formProduitRepository,
        RemiseProduitRepository remiseProduitRepository,
        ProduitRepository produitRepository,
        StorageService storageService,
        StockProduitRepository stockProduitRepository,
        FournisseurProduitRepository fournisseurProduitRepository,
        ImportationRepository importationRepository
    ) {
        this.familleProduitRepository = familleProduitRepository;
        this.rayonRepository = rayonRepository;
        this.typeEtiquetteRepository = typeEtiquetteRepository;
        this.tvaRepository = tvaRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.gammeProduitRepository = gammeProduitRepository;
        this.laboratoireRepository = laboratoireRepository;
        this.formProduitRepository = formProduitRepository;
        this.remiseProduitRepository = remiseProduitRepository;
        this.produitRepository = produitRepository;
        this.transactionTemplate = transactionTemplate;
        this.stockProduitRepository = stockProduitRepository;
        this.fournisseurProduitRepository = fournisseurProduitRepository;
        this.importationRepository = importationRepository;
        this.storageService = storageService;
    }

    @Async
    public void updateStocFromJSON(InputStream input, User user) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Storage storage = storageService.getDefaultMagasinMainStorage();
        if (storage == null) {
            storage = storageService.getDefaultMagasinMainStorage();
        }
        AtomicInteger errorSize = new AtomicInteger(0);
        AtomicInteger size = new AtomicInteger(0);
        List<ProduitDTO> list = mapper.readValue(input, new TypeReference<>() {
        });
        int totalSize = list.size();
        log.info("size===>> {}", list.size());
        transactionTemplate.setPropagationBehavior(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        Importation importation = importation(user);
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

    void processImportation(final ProduitDTO p, Storage storage, AtomicInteger errorSize,
        AtomicInteger size) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    StockProduit stockProduit = buidStockProduit(p, storage);
                    Produit produit = buildProduit(p);
                    produit.setRayonProduits(
                        Set.of(fromRayonLibelle(p.getRayonLibelle(), storage).setProduit(produit)));
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
                        ProduitDTO detail = p.getProduits().stream().findFirst().get();
                        Produit produitDetail = buildDeatilProduit(detail, produit);
                        produitDetail.setRayonProduits(Set.of(
                            fromRayonLibelle(p.getRayonLibelle(), storage).setProduit(
                                produitDetail)));
                        StockProduit stockProduitDetail = buidStockProduit(detail, storage);
                        produitDetail = produitRepository.save(produitDetail);
                        stockProduitDetail.setProduit(produitDetail);
                        stockProduitRepository.save(stockProduitDetail);

                        for (FournisseurProduitDTO d : detail.getFournisseurProduits()) {
                            FournisseurProduit fournisseurProduitDetail = buildFournisseurProduit(
                                d);
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
        });


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
        if (ObjectUtils.isNotEmpty(produitDTO.getTauxRemise())) {
            remiseProduitRepository.findFirstByRemiseValueEquals(produitDTO.getTauxRemise())
                .ifPresent(r -> {
                    produit.setRemise(r);
                });
        }
        produit.setTva(parent.getTva());
        produit.setLaboratoire(parent.getLaboratoire());
        produit.setFamille(parent.getFamille());
        produit.setGamme(produit.getGamme());
        produit.setTypeEtyquette(parent.getTypeEtyquette());
        produit.setForme(parent.getForme());
        return produit;
    }

    private RayonProduit fromRayonLibelle(String libelle, Storage storage) {
        Optional<Rayon> optionalRayon = rayonRepository.findFirstByLibelleAndStorageId(libelle,
            storage.getId());
        Rayon rayon;
        if (optionalRayon.isEmpty()) {
            rayon = rayonRepository.findFirstByLibelleAndStorageId(
                EntityConstant.SANS_EMPLACEMENT_LIBELLE, storage.getId()).get();
        } else {
            rayon = optionalRayon.get();
        }
        return new RayonProduit().setRayon(rayon);
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
        if (ObjectUtils.isNotEmpty(produitDTO.getTauxRemise())) {
            remiseProduitRepository.findFirstByRemiseValueEquals(produitDTO.getTauxRemise())
                .ifPresent(r -> {
                    produit.setRemise(r);
                });
        }
        if (ObjectUtils.isNotEmpty(produitDTO.getTvaTaux())) {
            tvaRepository.findFirstByTauxEquals(produitDTO.getTvaTaux())
                .ifPresent(t -> {
                    produit.setTva(t);
                });
        }
        if (StringUtils.isNotEmpty(produitDTO.getLaboratoireLibelle())) {
            laboratoireRepository.findFirstByLibelleEquals(produitDTO.getLaboratoireLibelle())
                .ifPresent(l -> {
                    produit.setLaboratoire(l);
                });
        }
        if (StringUtils.isNotEmpty(produitDTO.getFamilleLibelle())) {
            familleProduitRepository.findFirstByLibelleEquals(produitDTO.getFamilleLibelle())
                .ifPresent(f -> {
                    produit.setFamille(f);
                });
        }
        if (StringUtils.isNotEmpty(produitDTO.getGammeLibelle())) {
            gammeProduitRepository.findFirstByLibelleEquals(produitDTO.getGammeLibelle())
                .ifPresent(d -> {
                    produit.setGamme(d);
                });
        }
        if (StringUtils.isNotEmpty(produitDTO.getTypeEtiquetteLibelle())) {
            typeEtiquetteRepository.findFirstByLibelleEquals(produitDTO.getTypeEtiquetteLibelle())
                .ifPresent(t -> {
                    produit.setTypeEtyquette(t);
                });
        }
        if (StringUtils.isNotEmpty(produitDTO.getFormeLibelle())) {
            formProduitRepository.findFirstByLibelleEquals(produitDTO.getFormeLibelle())
                .ifPresent(f ->
                    produit.setForme(f)
                );

        }

        return produit;

    }

    private StockProduit buidStockProduit(ProduitDTO p, Storage storage) {
        return new StockProduit()
            .qtyStock(p.getTotalQuantity())
            .qtyUG(p.getQtyUG()).
            qtyVirtual(p.getTotalQuantity())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .setStorage(storage);
    }


    private FournisseurProduit buildFournisseurProduit(FournisseurProduitDTO p) {
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setFournisseur(
            fournisseurRepository.findFirstByLibelleEquals(p.getFournisseurLibelle()).get());
        fournisseurProduit.setPrixUni(p.getPrixUni());
        fournisseurProduit.setPrixAchat(p.getPrixAchat());
        fournisseurProduit.setPrincipal(p.isPrincipal());
        fournisseurProduit.setCodeCip(p.getCodeCip());
        fournisseurProduit.setCreatedAt(LocalDateTime.now());
        fournisseurProduit.setUpdatedAt(LocalDateTime.now());
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
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    importationRepository.save(importation);
                } catch (Exception e) {
                    log.debug("saveImportation ===>> {}", e);

                }
            }
        });
    }


    private void updateImportation(final int errorSize, final int size) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    Importation importation = importationRepository.findFirstByImportationTypeOrderByCreatedDesc(
                        ImportationType.STOCK_PRODUIT);
                    if (importation != null) {
                        importation.setUpdated(LocalDateTime.now());
                        importation.setSize(size);
                        importation.setErrorSize(errorSize);
                        importation.setImportationStatus(
                            errorSize > 0 ? ImportationStatus.COMPLETED_ERRORS
                                : ImportationStatus.COMPLETED);
                        importationRepository.save(importation);
                    }
                } catch (Exception e) {
                    log.debug("saveImportation ===>> {}", e);

                }
            }
        });
    }

    public Importation current(ImportationType importationType) {
        return importationRepository.findFirstByImportationTypeOrderByCreatedDesc(importationType);
    }
}
