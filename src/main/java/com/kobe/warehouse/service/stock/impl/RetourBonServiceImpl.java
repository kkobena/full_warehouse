package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.MotifRetourProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.ReponseRetourBon;
import com.kobe.warehouse.domain.ReponseRetourBonItem;
import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.RetourBonItem;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ReponseRetourBonItemRepository;
import com.kobe.warehouse.repository.ReponseRetourBonRepository;
import com.kobe.warehouse.repository.RetourBonItemRepository;
import com.kobe.warehouse.repository.RetourBonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.ReponseRetourBonDTO;
import com.kobe.warehouse.service.dto.ReponseRetourBonItemDTO;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonItemDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.report.pdf.RetourBonPdfReportService;
import com.kobe.warehouse.service.stock.RetourBonService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link RetourBon}.
 */
@Service
@Transactional
public class RetourBonServiceImpl implements RetourBonService {

    private final Logger log = LoggerFactory.getLogger(RetourBonServiceImpl.class);

    private final RetourBonRepository retourBonRepository;
    private final RetourBonItemRepository retourBonItemRepository;
    private final CommandeRepository commandeRepository;
    private final OrderLineRepository orderLineRepository;
    private final UserService userService;
    private final StockProduitRepository stockProduitRepository;
    private final InventoryTransactionService inventoryTransactionService;
    private final ReponseRetourBonRepository reponseRetourBonRepository;
    private final ReponseRetourBonItemRepository reponseRetourBonItemRepository;
    private final LotRepository lotRepository;
    private final RetourBonPdfReportService retourBonPdfReportService;

    public RetourBonServiceImpl(
        RetourBonRepository retourBonRepository,
        RetourBonItemRepository retourBonItemRepository,
        CommandeRepository commandeRepository,
        OrderLineRepository orderLineRepository,
        UserService userService,
        StockProduitRepository stockProduitRepository,
        InventoryTransactionService inventoryTransactionService,
        ReponseRetourBonRepository reponseRetourBonRepository,
        ReponseRetourBonItemRepository reponseRetourBonItemRepository,
        LotRepository lotRepository, RetourBonPdfReportService retourBonPdfReportService
    ) {
        this.retourBonRepository = retourBonRepository;
        this.retourBonItemRepository = retourBonItemRepository;
        this.commandeRepository = commandeRepository;
        this.orderLineRepository = orderLineRepository;
        this.userService = userService;
        this.stockProduitRepository = stockProduitRepository;
        this.inventoryTransactionService = inventoryTransactionService;
        this.reponseRetourBonRepository = reponseRetourBonRepository;
        this.reponseRetourBonItemRepository = reponseRetourBonItemRepository;
        this.lotRepository = lotRepository;
        this.retourBonPdfReportService = retourBonPdfReportService;
    }

    @Override
    public RetourBonDTO create(RetourBonDTO retourBonDTO) {
        log.debug("Request to create RetourBon : {}", retourBonDTO);

        RetourBon retourBon = new RetourBon();
        retourBon.setDateMtv(LocalDateTime.now());
        retourBon.setStatut(RetourStatut.VALIDATED);
        retourBon.setCommentaire(retourBonDTO.getCommentaire());
        AppUser currentUser = userService.getUser();
        Magasin magasin = currentUser.getMagasin();
        int magasinId = magasin.getId();
        retourBon.setUser(currentUser);
        CommandeId commandeId = new CommandeId(retourBonDTO.getCommandeId(), retourBonDTO.getCommandeOrderDate());
        Commande commande = commandeRepository.findById(commandeId).orElseThrow(() -> new RuntimeException("Commande not found"));
        retourBon.setCommande(commande);
        retourBon = retourBonRepository.save(retourBon);

        if (retourBonDTO.getRetourBonItems() != null && !retourBonDTO.getRetourBonItems().isEmpty()) {
            RetourBon finalRetourBon = retourBon;
            retourBonDTO.getRetourBonItems().forEach(itemDTO -> createRetourBonItem(itemDTO, finalRetourBon, magasinId));
        }

        return new RetourBonDTO(retourBon);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RetourBonDTO> findAll(RetourStatut statut, LocalDate dtStart, LocalDate dtEnd, String search, Pageable pageable) {
        log.debug("Request to get all RetourBons with filters: statut={}, dtStart={}, dtEnd={}, search={}", statut, dtStart, dtEnd, search);
        Specification<RetourBon> spec = buildSpecification(statut, dtStart, dtEnd, search);
        return retourBonRepository.findAll(spec, pageable).map(RetourBonDTO::new);
    }

    private Specification<RetourBon> buildSpecification(RetourStatut statut, LocalDate dtStart, LocalDate dtEnd, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (statut != null) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }
            if (dtStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateMtv"), dtStart.atStartOfDay()));
            }
            if (dtEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateMtv"), dtEnd.atTime(23, 59, 59)));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate byFournisseur = cb.like(cb.lower(root.get("commande").get("fournisseur").get("libelle")), pattern);
                Predicate byReference = cb.like(cb.lower(root.get("commande").get("receiptReference")), pattern);
                predicates.add(cb.or(byFournisseur, byReference));
            }
            query.orderBy(cb.desc(root.get("dateMtv")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetourBonDTO> findAllByCommande(Integer commandeId, LocalDate orderDate) {
        log.debug("Request to get all RetourBons by commande : {}, {}", commandeId, orderDate);
        return retourBonRepository.findAllByCommandeId(commandeId).stream().map(RetourBonDTO::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RetourBonDTO> findOne(Integer id) {
        log.debug("Request to get RetourBon : {}", id);
        return retourBonRepository.findById(id).map(RetourBonDTO::new);
    }

    @Override
    public ReponseRetourBonDTO createSupplierResponse(ReponseRetourBonDTO reponseRetourBonDTO) {
        AppUser currentUser = userService.getUser();
        RetourBon retourBon = retourBonRepository
            .findById(reponseRetourBonDTO.getRetourBonId())
            .orElseThrow(() -> new GenericError("RetourBon not found"));

        if (retourBon.getStatut() != RetourStatut.VALIDATED) {
            throw new GenericError("Ce retour est déjà traité ");
        }

        ReponseRetourBon reponseRetourBon = new ReponseRetourBon();
        reponseRetourBon.setDateMtv(LocalDateTime.now());
        reponseRetourBon.setUser(currentUser);
        reponseRetourBon.setRetourBon(retourBon);
        reponseRetourBon = reponseRetourBonRepository.save(reponseRetourBon);
        boolean allItemsAccepted = true;

        // Create the response items
        if (reponseRetourBonDTO.getReponseRetourBonItems() != null && !reponseRetourBonDTO.getReponseRetourBonItems().isEmpty()) {
            ReponseRetourBon finalReponseRetourBon = reponseRetourBon;
            for (ReponseRetourBonItemDTO itemDTO : reponseRetourBonDTO.getReponseRetourBonItems()) {
                var allAccepted = createReponseRetourBonItem(itemDTO, finalReponseRetourBon);
                if (!allAccepted) {
                    allItemsAccepted = false;
                }
            }
        }
        if (allItemsAccepted) {
            retourBon.setStatut(RetourStatut.CLOSED);
        }
        retourBonRepository.save(retourBon);

        return new ReponseRetourBonDTO(reponseRetourBon);
    }

    @Override
    public RetourBonDTO update(RetourBonDTO retourBonDTO) {
        log.debug("Request to update RetourBon : {}", retourBonDTO.getId());
        RetourBon retourBon = retourBonRepository
            .findById(retourBonDTO.getId())
            .orElseThrow(() -> new GenericError("RetourBon not found"));
        if (retourBon.getStatut() != RetourStatut.VALIDATED) {
            throw new GenericError("Seuls les retours en attente peuvent être modifiés");
        }
        int magasinId = userService.getUser().getMagasin().getId();

        // Reverse all existing items' stock movements then delete them
        retourBonItemRepository.findAllByRetourBonId(retourBon.getId())
            .forEach(item -> reverseRetourBonItem(item, magasinId));
        retourBonItemRepository.deleteAllByRetourBonId(retourBon.getId());

        retourBon.setCommentaire(retourBonDTO.getCommentaire());
        retourBon.setDateMtv(LocalDateTime.now());
        retourBonRepository.save(retourBon);

        if (retourBonDTO.getRetourBonItems() != null && !retourBonDTO.getRetourBonItems().isEmpty()) {
            retourBonDTO.getRetourBonItems().forEach(itemDTO -> createRetourBonItem(itemDTO, retourBon, magasinId));
        }

        return new RetourBonDTO(retourBonRepository.findById(retourBon.getId()).orElseThrow());
    }

    @Override
    public void delete(Integer id) {
        log.debug("Request to delete RetourBon : {}", id);
        RetourBon retourBon = retourBonRepository.findById(id).orElseThrow(() -> new GenericError("RetourBon not found"));
        if (retourBon.getStatut() != RetourStatut.VALIDATED) {
            throw new GenericError("Seuls les retours en attente peuvent être supprimés");
        }
        int magasinId = userService.getUser().getMagasin().getId();
        retourBonItemRepository.findAllByRetourBonId(id).forEach(item -> reverseRetourBonItem(item, magasinId));
        retourBonItemRepository.deleteAllByRetourBonId(id);
        retourBonRepository.deleteById(id);
    }

    private void reverseRetourBonItem(RetourBonItem item, int magasinId) {
        Integer produitId = item.getOrderLine().getFournisseurProduit().getProduit().getId();
        List<StockProduit> stockProduits = stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(magasinId, produitId);
        var stockProduit = stockProduits.stream()
            .filter(st -> st.getStorage().getStorageType() == StorageType.PRINCIPAL)
            .findFirst()
            .orElse(stockProduits.getFirst());
        stockProduit.setQtyStock(stockProduit.getQtyStock() + item.getQtyMvt());
        stockProduitRepository.save(stockProduit);
        if (item.getLot() != null) {
            var lot = item.getLot();
            lot.setQuantity(lot.getQuantity() + item.getQtyMvt());
            lotRepository.save(lot);
        }
    }

    @Override
    public RetourBonDTO markAsProcessing(Integer id) {
        log.debug("Request to mark RetourBon as PROCESSING : {}", id);
        RetourBon retourBon = retourBonRepository.findById(id).orElseThrow(() -> new GenericError("RetourBon not found"));
        if (retourBon.getStatut() != RetourStatut.VALIDATED) {
            throw new GenericError("Seuls les retours en attente peuvent être marqués en cours");
        }
        retourBon.setStatut(RetourStatut.PROCESSING);
        return new RetourBonDTO(retourBonRepository.save(retourBon));
    }

    @Override
    public byte[] export(Integer id) {
       return retourBonPdfReportService.export(new RetourBonDTO(retourBonRepository.findById(id).orElseThrow(() -> new RuntimeException("RetourBon not found"))));
    }

    private boolean createReponseRetourBonItem(ReponseRetourBonItemDTO itemDTO, ReponseRetourBon reponseRetourBon) {
        // Find the RetourBonItem
        RetourBonItem retourBonItem = retourBonItemRepository
            .findById(itemDTO.getRetourBonItemId())
            .orElseThrow(() -> new GenericError("RetourBonItem not found"));
        if (retourBonItem.getAcceptedQty() != null && retourBonItem.getAcceptedQty().compareTo(retourBonItem.getQtyMvt()) == 0) {
            return true;
        }
        // Validate quantity
        if (itemDTO.getQtyMvt() < 0) {
            throw new GenericError("Accepted quantity cannot be negative");
        }

        if (itemDTO.getQtyMvt() > retourBonItem.getQtyMvt()) {
            throw new GenericError("Accepted quantity cannot exceed requested quantity");
        }

        // Create the response item

        int acceptedQty = Objects.requireNonNullElse(retourBonItem.getAcceptedQty(), 0);
        ReponseRetourBonItem responseItem = new ReponseRetourBonItem();
        responseItem.setDateMtv(LocalDateTime.now());
        responseItem.setReponseRetourBon(reponseRetourBon);
        responseItem.setRetourBonItem(retourBonItem);
        responseItem.setQtyMvt(itemDTO.getQtyMvt() - acceptedQty);
        responseItem.setPrixAchat(retourBonItem.getPrixAchat());
        reponseRetourBonItemRepository.save(responseItem);
        retourBonItem.setAcceptedQty(Objects.requireNonNullElse(retourBonItem.getAcceptedQty(), 0) + responseItem.getQtyMvt());
        retourBonItemRepository.save(retourBonItem);
        return itemDTO.getQtyMvt().compareTo(retourBonItem.getQtyMvt()) == 0;
    }

    private void createRetourBonItem(RetourBonItemDTO itemDTO, RetourBon retourBon, int magasinId) {
        List<StockProduit> stockProduits = stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(
            magasinId,
            itemDTO.getProduitId()
        );
        StockProduit stockProduit = stockProduits
            .stream()
            .filter(st -> st.getStorage().getStorageType() == StorageType.PRINCIPAL)
            .findFirst()
            .orElse(stockProduits.getFirst());
        int initStock = stockProduits.stream().mapToInt(StockProduit::getTotalStockQuantity).sum();
        if (itemDTO.getQtyMvt() > initStock) {
            throw new GenericError("Stock insuffisant pour le produit: " + itemDTO.getProduitCip());
        }
        int finalAfterStock = initStock - itemDTO.getQtyMvt();
        RetourBonItem item = new RetourBonItem();
        item.setDateMtv(LocalDateTime.now());
        item.setRetourBon(retourBon);
        item.setQtyMvt(itemDTO.getQtyMvt());
        item.setInitStock(initStock);
        item.setAfterStock(finalAfterStock);
        var motifRetourProduit = new MotifRetourProduit();
        motifRetourProduit.setId(itemDTO.getMotifRetourId());
        item.setMotifRetour(motifRetourProduit);

        // Set order line
        if (itemDTO.getOrderLineId() != null && itemDTO.getOrderLineOrderDate() != null) {
            OrderLineId orderLineId = new OrderLineId(itemDTO.getOrderLineId(), itemDTO.getOrderLineOrderDate());
            OrderLine orderLine = orderLineRepository
                .findById(orderLineId)
                .orElseThrow(() -> new RuntimeException("ligne de commande introuvable"));
            item.setOrderLine(orderLine);
            item.setPrixAchat(orderLine.getOrderCostAmount());
        }
        if (itemDTO.getLotId() != null) {
            var lot = lotRepository.findById(itemDTO.getLotId()).orElseThrow(() -> new GenericError("Le lot n'existe pas"));
            item.setLot(lot);

            if (itemDTO.getQtyMvt() > lot.getQuantity()) {
                throw new GenericError("Quantité insuffisante dans le lot: " + lot.getNumLot());
            }
            lot.setQuantity(lot.getQuantity() - itemDTO.getQtyMvt());
            lotRepository.save(lot);
        }

        item = retourBonItemRepository.save(item);
        stockProduit.setQtyStock(finalAfterStock);
        stockProduitRepository.save(stockProduit);
        inventoryTransactionService.save(item);
    }
}
