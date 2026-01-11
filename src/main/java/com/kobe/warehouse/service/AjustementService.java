package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.AjustRepository;
import com.kobe.warehouse.repository.AjustementRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.AjustementDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.settings.FileResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

import static java.util.Objects.nonNull;

/**
 * Service Implementation for managing {@link Ajustement}.
 */
@Service
@Transactional
public class AjustementService extends FileResourceService {

    private final Logger log = LoggerFactory.getLogger(AjustementService.class);

    private final AjustementRepository ajustementRepository;
    private final ProduitRepository produitRepository;
    private final AjustRepository ajustRepository;
    private final StorageService storageService;
    private final StockProduitRepository stockProduitRepository;
    private final LogsService logsService;
    private final InventoryTransactionService inventoryTransactionService;
    private final SuggestionReassortService suggestionReassortService;


    private final BiPredicate<Ajustement, String> searchPredicate = (ajustement, s) -> {
        Produit produit = ajustement.getStockProduit().getProduit();
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        return produit.getLibelle().toUpperCase().contains(s.toUpperCase()) || fournisseurProduit.getCodeCip().contains(s);
    };

    public AjustementService(
        AjustementRepository ajustementRepository,
        ProduitRepository produitRepository,
        AjustRepository ajustRepository,
        StorageService storageService,
        StockProduitRepository stockProduitRepository,
        LogsService logsService,
        InventoryTransactionService inventoryTransactionService, SuggestionReassortService suggestionReassortService
    ) {
        this.ajustementRepository = ajustementRepository;
        this.produitRepository = produitRepository;
        this.ajustRepository = ajustRepository;
        this.storageService = storageService;
        this.stockProduitRepository = stockProduitRepository;
        this.logsService = logsService;
        this.inventoryTransactionService = inventoryTransactionService;
        this.suggestionReassortService = suggestionReassortService;
    }

    private AppUser getUser() {
        return storageService.getUser();
    }

    public AjustDTO createAjsut(AjustDTO ajustDto) {
        Ajust ajust = new Ajust();
        ajust.setCommentaire(ajustDto.getCommentaire());
        ajust.setUser(getUser());
        ajust.setDateMtv(LocalDateTime.now());

        ajust = ajustRepository.save(ajust);
        create(ajustDto.getAjustements().getFirst(), ajust);
        return new AjustDTO(ajust);
    }

    private void create(AjustementDTO ajustementDTO, Ajust ajust) {
        Integer storageId = nonNull(ajustementDTO.getStorageId()) ? ajustementDTO.getStorageId() : storageService.getDefaultConnectedUserMainStorage().getId();

        Produit produit = produitRepository.getReferenceById(ajustementDTO.getProduitId());
        StockProduit stockProduit = produit.getStockProduits().stream().filter(stockP -> Objects.equals(stockP.getStorage().getId(), storageId)).findFirst().orElseThrow(() ->
            new GenericError("Le produit " + produit.getLibelle() + " n'a pas de stock dans le magasin selectionné"));
        int stock = stockProduit.getTotalStockQuantity();
        Ajustement ajustement = new Ajustement();
        ajustement.setAjust(ajust);
        ajustement.setMotifAjustement(fromMotifId(ajustementDTO.getMotifAjustementId()));
        ajustement.setStockProduit(stockProduit);
        ajustement.setDateMtv(LocalDateTime.now());
        ajustement.setQtyMvt(ajustementDTO.getQtyMvt());
        ajustement.setStockBefore(stock);
        ajustement.setStockAfter(stock + ajustementDTO.getQtyMvt());
        AjustType ajustType = AjustType.AJUSTEMENT_OUT;
        if (ajustement.getQtyMvt() >= 0) {
            ajustType = AjustType.AJUSTEMENT_IN;
        }
        ajustement.setType(ajustType);
        ajustementRepository.save(ajustement);
    }

    public void deleteAll(List<Integer> ids) {
        ids.forEach(this.ajustementRepository::deleteById);
    }

    public void createOrUpdate(AjustementDTO ajustementDTO) {
        Optional<Ajustement> optionalAjustement = ajustementRepository.findFirstByAjustIdAndStockProduitProduitId(
            ajustementDTO.getAjustId(),
            ajustementDTO.getProduitId()
        );
        Ajust ajust = this.ajustRepository.getReferenceById(ajustementDTO.getAjustId());
        if (optionalAjustement.isEmpty()) {
            create(ajustementDTO, ajust);
        } else {


            Ajustement ajustement = optionalAjustement.get();
            StockProduit stockProduit = ajustement.getStockProduit();
            int stock = stockProduit.getTotalStockQuantity();
            ajustement.setDateMtv(LocalDateTime.now());
            ajustement.setQtyMvt(ajustement.getQtyMvt() + ajustementDTO.getQtyMvt());
            ajustement.setStockBefore(stock);
            ajustement.setStockAfter(stock + ajustement.getQtyMvt());
            ajustementRepository.save(ajustement);
        }
    }

    public void saveAjust(AjustDTO ajustDto) {
        Ajust ajust = ajustRepository.getReferenceById(ajustDto.getId());
        List<Ajustement> ajustements = ajustementRepository.findAllByAjustId(ajust.getId());
        saveItems(ajustements);
        ajust.setCommentaire(ajustDto.getCommentaire());
        ajust.setStatut(AjustementStatut.CLOSED);
    }

    private void saveItems(List<Ajustement> ajustements) {
        for (Ajustement ajustement : ajustements) {
            StockProduit p = ajustement.getStockProduit();
            Produit produit = p.getProduit();
            int initStock = p.getTotalStockQuantity();
            AjustType ajustType = AjustType.AJUSTEMENT_OUT;
            TransactionType transactionType = TransactionType.AJUSTEMENT_OUT;
            if (ajustement.getQtyMvt() >= 0) {
                ajustType = AjustType.AJUSTEMENT_IN;
                transactionType = TransactionType.AJUSTEMENT_IN;
            }
            ajustement.setType(ajustType);
            ajustement.setStockBefore(initStock);
            p.setQtyStock(p.getQtyStock() + ajustement.getQtyMvt());
            p.setQtyVirtual(p.getQtyStock());
            p.setQtyUG(0);
            ajustement.setStockAfter(p.getQtyStock());
            ajustement = this.ajustementRepository.save(ajustement);
          p=  stockProduitRepository.save(p);
            FournisseurProduit fournisseurProduitPrincipal = produit.getFournisseurProduitPrincipal();
            String desc = String.format(
                "Ajustement du produit %s %s quantité initiale %d quantité ajustéé %d quantité finale %d",
                fournisseurProduitPrincipal.getCodeCip(),
                produit.getLibelle(),
                initStock,
                ajustement.getQtyMvt(),
                p.getQtyStock()
            );
            inventoryTransactionService.save(ajustement);
            suggestionReassortService.createRayonSuggestionReassort(p);
            suggestionReassortService.createReserveSuggestionReassort(p);

            logsService.create(transactionType, desc, ajustement.getId().toString());
        }
    }

    public AjustementDTO update(AjustementDTO ajustementDTO) {
        Ajustement ajustement = ajustementRepository.getReferenceById(ajustementDTO.getId());
        StockProduit stockProduit = ajustement.getStockProduit();
        int stock = stockProduit.getQtyStock();
        ajustement.setDateMtv(LocalDateTime.now());
        ajustement.setQtyMvt(ajustementDTO.getQtyMvt());
        ajustement.setStockBefore(stock);
        ajustement.setStockAfter(stock + ajustementDTO.getQtyMvt());
        return new AjustementDTO(ajustementRepository.save(ajustement));
    }

    private MotifAjustement fromMotifId(Integer motifId) {
        if (motifId == null) {
            return null;
        }
        MotifAjustement motifAjustement = new MotifAjustement();
        motifAjustement.setId(motifId);
        return motifAjustement;
    }

    @Transactional(readOnly = true)
    public List<AjustementDTO> findAll(Integer id, String search) {
        log.debug("Request to get all Ajustements");
        Comparator<Ajustement> ajustementComparator = (Comparator.comparing(Ajustement::getDateMtv, Comparator.reverseOrder()));

        if (StringUtils.hasLength(search)) {
            return ajustementRepository
                .findAllByAjustId(id)
                .stream()
                .sorted(ajustementComparator)
                .filter(it -> this.searchPredicate.test(it, search))
                .toList()
                .stream()
                .map(AjustementDTO::new)
                .toList();
        }
        List<Ajustement> ajustements = ajustementRepository.findAllByAjustId(id);
        ajustements.sort(ajustementComparator);
        return ajustements.stream().map(AjustementDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<Ajustement> findAll() {
        log.debug("Request to get all Ajustements");
        return ajustementRepository.findAll();
    }

    public void deleteItem(Integer id) {
        ajustementRepository.deleteById(id);
    }

    public void delete(Integer id) {
        Ajust ajust = this.ajustRepository.getReferenceById(id);
        if (ajust.getStatut() == AjustementStatut.PENDING) {
            this.ajustRepository.deleteById(id);
        }
    }
}
