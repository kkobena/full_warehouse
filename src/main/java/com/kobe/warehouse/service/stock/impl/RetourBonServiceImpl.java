package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.MotifRetourProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.RetourBonItem;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.RetourBonItemRepository;
import com.kobe.warehouse.repository.RetourBonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.AvoirFournisseurCommand;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFromBonLignesCommand;
import com.kobe.warehouse.service.dto.RetourBonBatchResultDTO;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonGroupeDTO;
import com.kobe.warehouse.service.dto.RetourBonFromLotRequest;
import com.kobe.warehouse.service.dto.RetourBonFromLotsRequest;
import com.kobe.warehouse.service.dto.RetourBonItemDTO;
import com.kobe.warehouse.service.dto.RetourBonLotResolutionDTO;
import com.kobe.warehouse.service.dto.RetourCompletCommandeRequest;
import com.kobe.warehouse.service.errors.FournisseurIntrouvableException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.MultipleFournisseursException;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.report.pdf.RetourBonPdfReportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.AvoirFournisseurService;
import com.kobe.warehouse.service.stock.RetourBonService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
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
    private final LotRepository lotRepository;
    private final RetourBonPdfReportService retourBonPdfReportService;
    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final FournisseurRepository fournisseurRepository;
    private final AppConfigurationService appConfigurationService;
    private final AvoirFournisseurService avoirFournisseurService;

    public RetourBonServiceImpl(
        RetourBonRepository retourBonRepository,
        RetourBonItemRepository retourBonItemRepository,
        CommandeRepository commandeRepository,
        OrderLineRepository orderLineRepository,
        UserService userService,
        StockProduitRepository stockProduitRepository,
        InventoryTransactionService inventoryTransactionService,
        LotRepository lotRepository,
        RetourBonPdfReportService retourBonPdfReportService,
        FournisseurProduitRepository fournisseurProduitRepository,
        FournisseurRepository fournisseurRepository,
        AppConfigurationService appConfigurationService,
        AvoirFournisseurService avoirFournisseurService
    ) {
        this.retourBonRepository = retourBonRepository;
        this.retourBonItemRepository = retourBonItemRepository;
        this.commandeRepository = commandeRepository;
        this.orderLineRepository = orderLineRepository;
        this.userService = userService;
        this.stockProduitRepository = stockProduitRepository;
        this.inventoryTransactionService = inventoryTransactionService;
        this.lotRepository = lotRepository;
        this.retourBonPdfReportService = retourBonPdfReportService;
        this.fournisseurProduitRepository = fournisseurProduitRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.appConfigurationService = appConfigurationService;
        this.avoirFournisseurService = avoirFournisseurService;
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
        retourBon.setReference(generateReference(retourBon.getId()));
        retourBon = retourBonRepository.save(retourBon);

        if (retourBonDTO.getRetourBonItems() != null && !retourBonDTO.getRetourBonItems().isEmpty()) {
            RetourBon finalRetourBon = retourBon;
            retourBonDTO.getRetourBonItems().forEach(itemDTO -> createRetourBonItem(itemDTO, finalRetourBon, magasinId));
        }

        return toDto(retourBon);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RetourBonDTO> findAll(RetourStatut statut, RetourStatut excludeStatut, LocalDate dtStart, LocalDate dtEnd, String search, Pageable pageable) {
        Specification<RetourBon> spec = buildSpecification(statut, excludeStatut, dtStart, dtEnd, search);
        return retourBonRepository.findAll(spec, pageable).map(this::toDto);
    }

    private Specification<RetourBon> buildSpecification(RetourStatut statut, RetourStatut excludeStatut, LocalDate dtStart, LocalDate dtEnd, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (statut != null) {
                predicates.add(cb.equal(root.get("statut"), statut));
            } else if (excludeStatut != null) {
                predicates.add(cb.notEqual(root.get("statut"), excludeStatut));
            }
            if (dtStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateMtv"), dtStart.atStartOfDay()));
            }
            if (dtEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateMtv"), dtEnd.atTime(23, 59, 59)));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                // Recherche dans commande.fournisseur.libelle (retours avec commande)
                Predicate byCommandeFournisseur = cb.like(
                    cb.lower(root.join("commande", JoinType.LEFT)
                        .join("fournisseur", JoinType.LEFT).get("libelle")), pattern);
                // Recherche dans fournisseur.libelle direct (retours hors commande)
                Predicate byDirectFournisseur = cb.like(
                    cb.lower(root.join("fournisseur", JoinType.LEFT).get("libelle")), pattern);
                // Recherche dans commande.receiptReference
                Predicate byReference = cb.like(
                    cb.lower(root.join("commande", JoinType.LEFT).get("receiptReference")), pattern);
                // Recherche dans retourBon.reference (ex: RET-2026-0042)
                Predicate byRetourReference = cb.like(cb.lower(root.get("reference")), pattern);
                predicates.add(cb.or(byCommandeFournisseur, byDirectFournisseur, byReference, byRetourReference));
            }
            query.orderBy(cb.desc(root.get("dateMtv")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetourBonDTO> findAllByCommande(Integer commandeId, LocalDate orderDate) {
        return retourBonRepository.findAllByCommandeId(commandeId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RetourBonDTO> findOne(Integer id) {
        return retourBonRepository.findById(id).map(this::toDto);
    }

    @Override
    public AvoirFournisseurDTO createSupplierResponse(AvoirFournisseurCommand command) {
        return avoirFournisseurService.create(command);
    }

    @Override
    public RetourBonDTO update(RetourBonDTO retourBonDTO) {
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

        return toDto(retourBonRepository.findById(retourBon.getId()).orElseThrow());
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

    @Override
    public RetourBonDTO markAsProcessing(Integer id) {
        log.debug("Request to mark RetourBon as PROCESSING : {}", id);
        RetourBon retourBon = retourBonRepository.findById(id).orElseThrow(() -> new GenericError("RetourBon not found"));
        if (retourBon.getStatut() != RetourStatut.VALIDATED) {
            throw new GenericError("Seuls les retours en attente peuvent être marqués en cours");
        }
        retourBon.setStatut(RetourStatut.PROCESSING);
        return toDto(retourBonRepository.save(retourBon));
    }

    @Override
    public byte[] export(Integer id) {
       return retourBonPdfReportService.export(new RetourBonDTO(retourBonRepository.findById(id).orElseThrow(() -> new RuntimeException("RetourBon not found"))));
    }

    @Override
    public RetourBonDTO createFromExpiredLot(RetourBonFromLotRequest request) {
        log.debug("Request to create RetourBon from expired lot: {}", request);

        var lot = lotRepository.findById(request.getLotId())
            .orElseThrow(() -> new GenericError("Lot non trouvé: " + request.getLotId()));

        if (request.getQuantity() > lot.getQuantity()) {
            throw new GenericError(
                "La quantité demandée (" + request.getQuantity()
                + ") dépasse le stock disponible dans le lot (" + lot.getQuantity() + ")"
            );
        }

        OrderLine orderLine = lot.getOrderLine();

        if (orderLine != null) {
            // ── Chemin nominal : résolution via orderLine → commande ──────────
            Commande commande = orderLine.getCommande();
            RetourBonDTO retourBonDTO = buildRetourBonDTOFromCommande(commande, request, orderLine, lot);
            return create(retourBonDTO);

        } else if (request.hasCommandeOverride()) {
            // ── Fallback manuel Phase 2 : commande choisie par l'utilisateur ──
            log.debug("Lot {} has no orderLine — using manual commande override: {}/{}", request.getLotId(), request.getCommandeId(), request.getCommandeOrderDate());
            CommandeId commandeKey = new CommandeId(request.getCommandeId(), request.getCommandeOrderDate());
            Commande commande = commandeRepository.findById(commandeKey)
                .orElseThrow(() -> new GenericError("Commande override non trouvée: " + request.getCommandeId()));
            RetourBonDTO retourBonDTO = buildRetourBonDTOFromCommande(commande, request, null, lot);
            return create(retourBonDTO);

        } else {
            // ── Chemin hors commande : résoudre le fournisseur via FournisseurProduit ──
            Fournisseur fournisseur = resolveFournisseurForHorsCommande(lot, request);
            return createHorsCommande(lot, request, fournisseur);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RetourBonLotResolutionDTO resolveLot(Integer lotId) {
        log.debug("Resolving lot {} for retour fournisseur dialog", lotId);
        Lot lot = lotRepository.findById(lotId)
            .orElseThrow(() -> new GenericError("Lot non trouvé: " + lotId));

        OrderLine orderLine = lot.getOrderLine();
        if (orderLine != null) {
            Commande commande = orderLine.getCommande();
            return RetourBonLotResolutionDTO.commandeTrouvee(
                commande.getId().getId(),
                commande.getOrderDate(),
                commande.getReceiptReference()
            );
        }

        // Pas d'orderLine → chercher le fournisseur via la fiche produit
        Produit produit = lot.getProduit();
        FournisseurProduit principal = produit.getFournisseurProduitPrincipal();
        if (principal != null) {
            Fournisseur f = principal.getFournisseur();
            return RetourBonLotResolutionDTO.horsCommandeUnFournisseur(f.getId(), f.getLibelle());
        }

        // Pas de fournisseur principal → chercher tous les fournisseurs du produit
        List<FournisseurProduit> tous = fournisseurProduitRepository.findAllByProduitId(produit.getId());
        if (tous.size() == 1) {
            Fournisseur f = tous.getFirst().getFournisseur();
            return RetourBonLotResolutionDTO.horsCommandeUnFournisseur(f.getId(), f.getLibelle());
        } else if (tous.size() > 1) {
            List<RetourBonLotResolutionDTO.FournisseurSimple> list = tous.stream()
                .map(fp -> new RetourBonLotResolutionDTO.FournisseurSimple(
                    fp.getFournisseur().getId(), fp.getFournisseur().getLibelle()))
                .toList();
            return RetourBonLotResolutionDTO.horsCommandeMultiFournisseurs(list);
        }

        return RetourBonLotResolutionDTO.fournisseurInconnu();
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    /** Résout le fournisseur pour un retour hors commande. Lance une exception si bloquant. */
    private Fournisseur resolveFournisseurForHorsCommande(Lot lot, RetourBonFromLotRequest request) {
        // L'utilisateur a explicitement choisi un fournisseur (cas HORS_COMMANDE_MULTI)
        if (request.hasFournisseurOverride()) {
            return fournisseurRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new GenericError("Fournisseur non trouvé: " + request.getFournisseurId()));
        }
        // Résolution auto via fournisseur principal du produit
        Produit produit = lot.getProduit();
        FournisseurProduit principal = produit.getFournisseurProduitPrincipal();
        if (principal != null) {
            return principal.getFournisseur();
        }
        List<FournisseurProduit> tous = fournisseurProduitRepository.findAllByProduitId(produit.getId());
        if (tous.size() == 1) {
            return tous.getFirst().getFournisseur();
        } else if (tous.size() > 1) {
            List<RetourBonLotResolutionDTO.FournisseurSimple> list = tous.stream()
                .map(fp -> new RetourBonLotResolutionDTO.FournisseurSimple(
                    fp.getFournisseur().getId(), fp.getFournisseur().getLibelle()))
                .toList();
            throw new MultipleFournisseursException(lot.getId(), list);
        }
        throw new FournisseurIntrouvableException(lot.getId());
    }

    /** Crée un RetourBon hors commande (fournisseur direct, commande = null). */
    private RetourBonDTO createHorsCommande(Lot lot, RetourBonFromLotRequest request, Fournisseur fournisseur) {
        AppUser currentUser = userService.getUser();
        int magasinId = currentUser.getMagasin().getId();

        RetourBon retourBon = new RetourBon();
        retourBon.setDateMtv(LocalDateTime.now());
        retourBon.setStatut(RetourStatut.VALIDATED);
        retourBon.setCommentaire(request.getCommentaire());
        retourBon.setUser(currentUser);
        retourBon.setHorsCommande(true);
        retourBon.setFournisseur(fournisseur);
        retourBon = retourBonRepository.save(retourBon);
        retourBon.setReference(generateReference(retourBon.getId()));
        retourBon = retourBonRepository.save(retourBon);

        RetourBonItemDTO itemDTO = new RetourBonItemDTO();
        itemDTO.setProduitId(lot.getProduit().getId());
        itemDTO.setLotId(lot.getId());
        itemDTO.setMotifRetourId(request.getMotifRetourId());
        itemDTO.setQtyMvt(request.getQuantity());
        itemDTO.setPrixAchat(lot.getPrixAchat());

        createRetourBonItem(itemDTO, retourBon, magasinId);
        return toDto(retourBonRepository.findById(retourBon.getId()).orElseThrow());
    }

    /** Construit le DTO commande pour le chemin nominal ou override. */
    private RetourBonDTO buildRetourBonDTOFromCommande(Commande commande, RetourBonFromLotRequest request, OrderLine orderLine, Lot lot) {
        RetourBonDTO dto = new RetourBonDTO();
        dto.setCommandeId(commande.getId().getId());
        dto.setCommandeOrderDate(commande.getId().getOrderDate());
        dto.setCommentaire(request.getCommentaire());

        RetourBonItemDTO itemDTO = new RetourBonItemDTO();
        itemDTO.setProduitId(lot.getProduit().getId());
        if (orderLine != null) {
            itemDTO.setOrderLineId(orderLine.getId().getId());
            itemDTO.setOrderLineOrderDate(orderLine.getId().getOrderDate());
        }
        itemDTO.setLotId(lot.getId());
        itemDTO.setMotifRetourId(request.getMotifRetourId());
        itemDTO.setQtyMvt(request.getQuantity());

        dto.setRetourBonItems(List.of(itemDTO));
        return dto;
    }

    @Override
    public RetourBonBatchResultDTO createFromExpiredLots(RetourBonFromLotsRequest request) {
        log.debug("Request to create RetourBons from expired lots batch: {} lots", request.getLots().size());
        RetourBonBatchResultDTO result = new RetourBonBatchResultDTO();

        for (RetourBonFromLotRequest lotRequest : request.getLots()) {
            try {
                RetourBonDTO created = createFromExpiredLot(lotRequest);
                result.addCreated(created);
            } catch (Exception e) {
                log.warn("Erreur lors du traitement du lot {} dans le batch : {}", lotRequest.getLotId(), e.getMessage());
                // Récupérer le numéro de lot si possible pour un message d'erreur utile
                String lotNumero = null;
                try {
                    lotNumero = lotRepository.findById(lotRequest.getLotId())
                        .map(Lot::getNumLot)
                        .orElse(null);
                } catch (Exception ignored) {}
                result.addError(lotRequest.getLotId(), lotNumero, e.getMessage());
            }
        }

        log.debug("Batch terminé : {} créés, {} erreurs", result.getTotalCreated(), result.getTotalErrors());
        return result;
    }

    private void reverseRetourBonItem(RetourBonItem item, int magasinId) {
        // Résolution du produit : via orderLine si disponible, sinon via lot
        Integer produitId;
        if (item.getOrderLine() != null) {
            produitId = item.getOrderLine().getFournisseurProduit().getProduit().getId();
        } else if (item.getLot() != null) {
            produitId = item.getLot().getProduit().getId();
        } else {
            throw new GenericError("Impossible de retrouver le produit pour annuler l'item du retour: " + item.getId());
        }
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
    public RetourBonDTO closeManually(Integer id) {
        log.debug("Request to close manually RetourBon : {}", id);
        RetourBon retourBon = retourBonRepository.findById(id)
            .orElseThrow(() -> new GenericError("RetourBon not found"));
        if (retourBon.getStatut() != RetourStatut.PARTIALLY_ACCEPTED) {
            throw new GenericError("Seuls les retours partiellement acceptés peuvent être clôturés manuellement");
        }
        retourBon.setStatut(RetourStatut.CLOSED);
        return toDto(retourBonRepository.save(retourBon));
    }

    @Override
    public AvoirFournisseurDTO createFromBonLignes(AvoirFromBonLignesCommand command) {
        log.debug("Request to create RetourBon + AvoirFournisseur from BL lines: commande={}", command.commandeId());

        CommandeId commandeKey = new CommandeId(command.commandeId(), command.commandeOrderDate());
        Commande commande = commandeRepository.findById(commandeKey)
            .orElseThrow(() -> new GenericError("Commande introuvable: " + command.commandeId()));

        AppUser currentUser = userService.getUser();
        int magasinId = currentUser.getMagasin().getId();

        RetourBon retourBon = new RetourBon();
        retourBon.setDateMtv(LocalDateTime.now());
        retourBon.setStatut(RetourStatut.VALIDATED);
        retourBon.setCommentaire(command.commentaire());
        retourBon.setUser(currentUser);
        retourBon.setCommande(commande);
        retourBon = retourBonRepository.save(retourBon);
        retourBon.setReference(generateReference(retourBon.getId()));
        retourBon = retourBonRepository.save(retourBon);

        for (AvoirFromBonLignesCommand.BonLigneItem ligne : command.lignes()) {
            RetourBonItemDTO itemDTO = new RetourBonItemDTO();
            itemDTO.setOrderLineId(ligne.orderLineId());
            itemDTO.setOrderLineOrderDate(ligne.orderLineOrderDate());
            itemDTO.setQtyMvt(ligne.qtyRetour());
            itemDTO.setMotifRetourId(ligne.motifRetourId());
            itemDTO.setProduitId(ligne.produitId());
            itemDTO.setProduitCip(ligne.produitCip());
            itemDTO.setPrixAchat(ligne.prixAchat());
            createRetourBonItem(itemDTO, retourBon, magasinId);
        }

        RetourBon saved = retourBonRepository.findById(retourBon.getId()).orElseThrow();
        List<AvoirFournisseurCommand.AvoirLigneCommand> avoirLignes = retourBonItemRepository
            .findAllByRetourBonId(saved.getId()).stream()
            .map(item -> new AvoirFournisseurCommand.AvoirLigneCommand(item.getId(), item.getQtyMvt(), item.getPrixAchat()))
            .toList();

        return avoirFournisseurService.createFromRetourBon(saved, avoirLignes, command.commentaire());
    }

    @Override
    public RetourBonDTO createRetourCompletFromCommande(RetourCompletCommandeRequest request) {
        log.debug("Request to create retour complet from commande: {}/{}", request.getCommandeId(), request.getCommandeOrderDate());

        CommandeId commandeKey = new CommandeId(request.getCommandeId(), request.getCommandeOrderDate());
        Commande commande = commandeRepository.findById(commandeKey)
            .orElseThrow(() -> new GenericError("Commande introuvable: " + request.getCommandeId()));

        List<OrderLine> orderLines = orderLineRepository
            .findAllByCommandeIdAndCommandeOrderDate(request.getCommandeId(), request.getCommandeOrderDate())
            .stream()
            .filter(ol -> ol.getQuantityReceived() != null && ol.getQuantityReceived() > 0)
            .toList();

        if (orderLines.isEmpty()) {
            throw new GenericError("Aucune ligne reçue trouvée pour cette commande");
        }

        AppUser currentUser = userService.getUser();
        int magasinId = currentUser.getMagasin().getId();

        RetourBon retourBon = new RetourBon();
        retourBon.setDateMtv(LocalDateTime.now());
        retourBon.setStatut(RetourStatut.VALIDATED);
        retourBon.setCommentaire(request.getCommentaire());
        retourBon.setUser(currentUser);
        retourBon.setCommande(commande);
        retourBon = retourBonRepository.save(retourBon);
        retourBon.setReference(generateReference(retourBon.getId()));
        retourBon = retourBonRepository.save(retourBon);

        RetourBon finalRetourBon = retourBon;
        for (OrderLine ol : orderLines) {
            OrderLineId orderLineId = ol.getId();
            int alreadyReturned = retourBonItemRepository.sumQtyMvtByOrderLineId(orderLineId.getId(), orderLineId.getOrderDate(), null, RetourStatut.CLOSED);
            int retournable = ol.getQuantityReceived() - alreadyReturned;
            if (retournable <= 0) continue;

            RetourBonItemDTO itemDTO = new RetourBonItemDTO();
            itemDTO.setOrderLineId(ol.getId().getId());
            itemDTO.setOrderLineOrderDate(ol.getId().getOrderDate());
            itemDTO.setQtyMvt(retournable);
            itemDTO.setMotifRetourId(request.getMotifRetourId());
            if (ol.getFournisseurProduit() != null && ol.getFournisseurProduit().getProduit() != null) {
                itemDTO.setProduitId(ol.getFournisseurProduit().getProduit().getId());
                itemDTO.setProduitCip(ol.getFournisseurProduit().getCodeCip());
            }
            createRetourBonItem(itemDTO, finalRetourBon, magasinId);
        }

        return toDto(retourBonRepository.findById(retourBon.getId()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetourBonGroupeDTO> findAllGroupedByFournisseur() {
        List<RetourBon> open = retourBonRepository.findAll(
            buildSpecification(null, RetourStatut.CLOSED, null, null, null)
        );
        return open.stream()
            .map(this::toDto)
            .filter(dto -> dto.getFournisseurId() != null)
            .collect(java.util.stream.Collectors.groupingBy(RetourBonDTO::getFournisseurId))
            .entrySet().stream()
            .map(e -> {
                List<RetourBonDTO> list = e.getValue();
                String libelle = list.getFirst().getFournisseurLibelle();
                return new com.kobe.warehouse.service.dto.RetourBonGroupeDTO(e.getKey(), libelle, list);
            })
            .sorted(java.util.Comparator.comparing(com.kobe.warehouse.service.dto.RetourBonGroupeDTO::getFournisseurLibelle,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportGroupe(List<Integer> ids) {
        List<RetourBonDTO> retourBons = retourBonRepository.findAllById(ids).stream()
            .map(this::toDto)
            .toList();
        return retourBonPdfReportService.exportGroupe(retourBons);
    }

    private RetourBonDTO toDto(RetourBon retourBon) {
        return new RetourBonDTO(retourBon).withDelayCheck(appConfigurationService.getDelaiRetourFournisseur());
    }

    private String generateReference(Integer id) {
        return "RET-" + Year.now().getValue() + "-" + String.format("%04d", id);
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

        if (itemDTO.getOrderLineId() != null && itemDTO.getOrderLineOrderDate() != null) {
            OrderLineId orderLineId = new OrderLineId(itemDTO.getOrderLineId(), itemDTO.getOrderLineOrderDate());
            OrderLine orderLine = orderLineRepository
                .findById(orderLineId)
                .orElseThrow(() -> new RuntimeException("ligne de commande introuvable"));
            item.setOrderLine(orderLine);
            item.setPrixAchat(orderLine.getOrderCostAmount());
        } else if (itemDTO.getPrixAchat() != null) {
            // Hors commande : prix d'achat vient du lot
            item.setPrixAchat(itemDTO.getPrixAchat());
        }

        if (itemDTO.getLotId() != null) {
            var lot = lotRepository.findById(itemDTO.getLotId()).orElseThrow(() -> new GenericError("Le lot n'existe pas"));
            item.setLot(lot);

            if (itemDTO.getQtyMvt() > lot.getQuantity()) {
                throw new GenericError("Quantité insuffisante dans le lot: " + lot.getNumLot());
            }
            int alreadyReturned = retourBonItemRepository.sumQtyMvtByLotId(lot.getId(), retourBon.getId(), RetourStatut.CLOSED);
            int retournable = lot.getQuantity() - alreadyReturned;
            if (itemDTO.getQtyMvt() > retournable) {
                throw new GenericError(
                    "Lot " + lot.getNumLot() + " : quantité retournable = " + retournable
                    + " (déjà retourné : " + alreadyReturned + ")"
                );
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
