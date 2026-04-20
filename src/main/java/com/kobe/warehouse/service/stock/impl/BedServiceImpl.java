package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.MotifBed;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.BedDTO;
import com.kobe.warehouse.service.dto.BedImportLigneDTO;
import com.kobe.warehouse.service.dto.BedLigneDTO;
import com.kobe.warehouse.service.dto.BedSummaryDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.CommandeIdGeneratorService;
import com.kobe.warehouse.service.id_generator.OrderLineIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.stock.BedService;
import com.kobe.warehouse.service.stock.ProduitService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BedServiceImpl implements BedService {

    private final CommandeRepository commandeRepository;
    private final OrderLineRepository orderLineRepository;
    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final FournisseurRepository fournisseurRepository;
    private final StorageService storageService;
    private final LogsService logsService;
    private final ProduitService produitService;
    private final CommandeIdGeneratorService commandeIdGeneratorService;
    private final OrderLineIdGeneratorService orderLineIdGeneratorService;
    private final ReferenceService referenceService;
    private final InventoryTransactionService inventoryTransactionService;

    public BedServiceImpl(
        CommandeRepository commandeRepository,
        OrderLineRepository orderLineRepository,
        FournisseurProduitRepository fournisseurProduitRepository,
        FournisseurRepository fournisseurRepository,
        StorageService storageService,
        LogsService logsService,
        ProduitService produitService,
        CommandeIdGeneratorService commandeIdGeneratorService,
        OrderLineIdGeneratorService orderLineIdGeneratorService,
        ReferenceService referenceService, InventoryTransactionService inventoryTransactionService
    ) {
        this.commandeRepository = commandeRepository;
        this.orderLineRepository = orderLineRepository;
        this.fournisseurProduitRepository = fournisseurProduitRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.storageService = storageService;
        this.logsService = logsService;
        this.produitService = produitService;
        this.commandeIdGeneratorService = commandeIdGeneratorService;
        this.orderLineIdGeneratorService = orderLineIdGeneratorService;
        this.referenceService = referenceService;
        this.inventoryTransactionService = inventoryTransactionService;
    }

    @Override
    public BedDTO createBed(BedDTO dto) {
        LocalDate today = LocalDate.now();
        Commande commande = new Commande();
        commande.setId(commandeIdGeneratorService.getNextIdAsInt());
        commande.setOrderDate(today);
        commande.setType(TypeDeliveryReceipt.DIRECT);
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setMotifBed(dto.getMotifBed());
        commande.setCommentaireBed(dto.getCommentaireBed());
        commande.setReceiptReference(generateBedReference(today));
        commande.setOrderReference(referenceService.buildNumCommande());
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(commande.getCreatedAt());
        commande.setUser(storageService.getUser());
        commande.setGrossAmount(0);
        commande.setDiscountAmount(0);
        commande.setTaxAmount(0);
        commande.setHtAmount(0);
        if (dto.getFournisseurId() != null) {
            commande.setFournisseur(fournisseurRepository.getReferenceById(dto.getFournisseurId()));
        }
        commandeRepository.save(commande);
        return new BedDTO(commande);
    }

    @Override
    @Transactional(readOnly = true)
    public BedDTO findById(Integer bedId, LocalDate orderDate) {
        Commande commande = loadBed(bedId, orderDate);
        BedDTO dto = new BedDTO(commande);
        List<BedLigneDTO> lignes = commande.getOrderLines()
            .stream()
            .map(BedLigneDTO::new)
            .toList();
        dto.setLignes(lignes);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BedSummaryDTO> findAll(
        String search,
        MotifBed motifBed,
        OrderStatut orderStatus,
        LocalDate fromDate,
        LocalDate toDate,
        Pageable pageable
    ) {
        Specification<Commande> spec = commandeRepository.byReceiptType(TypeDeliveryReceipt.DIRECT);

        if (motifBed != null) {
            spec = spec.and(commandeRepository.byMotifBed(motifBed));
        }
        if (orderStatus != null) {
            spec = spec.and(commandeRepository.hasOrderStatut(orderStatus));
        }
        if (fromDate != null && toDate != null) {
            spec = spec.and(commandeRepository.between(fromDate, toDate));
        } else if (fromDate != null) {
            spec = spec.and(commandeRepository.between(fromDate, LocalDate.now()));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(commandeRepository.byBedSearchRef(search));
        }

        return commandeRepository.findAll(spec, pageable)
            .map(BedSummaryDTO::new);
    }

    @Override
    public BedDTO addLigne(Integer bedId, LocalDate orderDate, BedLigneDTO ligneDTO) {
        Commande commande = loadBed(bedId, orderDate);
        if (commande.getOrderStatus() != OrderStatut.REQUESTED) {
            throw new GenericError("Ce BED ne peut plus être modifié", "bedNonModifiable");
        }
        FournisseurProduit fp = fournisseurProduitRepository.getReferenceById(ligneDTO.getFournisseurProduitId());

        OrderLine line = new OrderLine();
        line.setId(orderLineIdGeneratorService.getNextIdAsInt());
        line.setOrderDate(commande.getOrderDate());
        line.setCommande(commande);
        line.setFournisseurProduit(fp);
        line.setQuantityRequested(ligneDTO.getQuantite());
        line.setQuantityReceived(ligneDTO.getQuantite());
        line.setOrderCostAmount(ligneDTO.getPrixAchat());
        line.setOrderUnitPrice(fp.getPrixUni() > 0 ? fp.getPrixUni() : ligneDTO.getPrixVente());
        line.setInitStock(0);
        line.setFinalStock(0);
        line.setDiscountAmount(0);
        line.setCreatedAt(LocalDateTime.now());
        line.setUpdatedAt(line.getCreatedAt());
        line.setUpdated(Boolean.FALSE);
        orderLineRepository.save(line);

        commande.getOrderLines().add(line);
        int newGross = commande.getGrossAmount() != null ? commande.getGrossAmount() : 0;
        newGross += ligneDTO.getQuantite() * ligneDTO.getPrixAchat();
        commande.setGrossAmount(newGross);
        commande.setUpdatedAt(LocalDateTime.now());
        commandeRepository.save(commande);

        return findById(bedId, orderDate);
    }

    @Override
    public BedDTO updateLigne(Integer bedId, LocalDate orderDate, Integer ligneId, LocalDate ligneDate, BedLigneDTO dto) {
        Commande commande = loadBed(bedId, orderDate);
        if (commande.getOrderStatus() != OrderStatut.REQUESTED) {
            throw new GenericError("Ce BED ne peut plus être modifié", "bedNonModifiable");
        }
        OrderLine line = orderLineRepository.getReferenceById(new OrderLineId(ligneId, ligneDate));
        int oldAmount = line.getQuantityRequested() * line.getOrderCostAmount();

        line.setQuantityRequested(dto.getQuantite());
        line.setQuantityReceived(dto.getQuantite());
        line.setOrderCostAmount(dto.getPrixAchat());
        line.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.save(line);

        int gross = commande.getGrossAmount() != null ? commande.getGrossAmount() : 0;
        gross = gross - oldAmount + (dto.getQuantite() * dto.getPrixAchat());
        commande.setGrossAmount(Math.max(0, gross));
        commande.setUpdatedAt(LocalDateTime.now());
        commandeRepository.save(commande);

        return findById(bedId, orderDate);
    }

    @Override
    public void removeLigne(Integer bedId, LocalDate orderDate, Integer ligneId, LocalDate ligneDate) {
        Commande commande = loadBed(bedId, orderDate);
        if (commande.getOrderStatus() != OrderStatut.REQUESTED) {
            throw new GenericError("Ce BED ne peut plus être modifié", "bedNonModifiable");
        }
        OrderLine line = orderLineRepository.getReferenceById(new OrderLineId(ligneId, ligneDate));
        int gross = commande.getGrossAmount() != null ? commande.getGrossAmount() : 0;
        gross -= line.getQuantityRequested() * line.getOrderCostAmount();
        commande.setGrossAmount(Math.max(0, gross));
        commande.setUpdatedAt(LocalDateTime.now());
        orderLineRepository.delete(line);
        commandeRepository.save(commande);
    }

    @Override
    public BedDTO validateBed(Integer bedId, LocalDate orderDate, MotifBed motif, Integer fournisseurId, String commentaire) {
        Commande commande = loadBed(bedId, orderDate);
        if (commande.getOrderStatus() != OrderStatut.REQUESTED) {
            throw new GenericError("Ce BED est déjà validé ou annulé", "bedDejaValide");
        }
        if (commande.getOrderLines().isEmpty()) {
            throw new GenericError("Le BED doit avoir au moins une ligne", "aucuneLigne");
        }
        MotifBed effectifMotif = motif != null ? motif : commande.getMotifBed();
        if (effectifMotif == null) {
            throw new GenericError("Le motif est obligatoire pour valider un BED", "motifObligatoire");
        }
        commande.setMotifBed(effectifMotif);
        if (fournisseurId != null && commande.getFournisseur() == null) {
            commande.setFournisseur(fournisseurRepository.getReferenceById(fournisseurId));
        }
        if (commentaire != null && !commentaire.isBlank()) {
            commande.setCommentaireBed(commentaire);
        }

        LocalDate today = LocalDate.now();
        commande.setReceiptDate(today);

        commande.getOrderLines().forEach(line -> {
            FournisseurProduit fp = line.getFournisseurProduit();
            Produit produit = fp.getProduit();
            int initStock = produitService.getProductTotalStock(produit.getId());
            line.setInitStock(initStock);
            line.setFinalStock(initStock + line.getQuantityReceived());
            line.setUpdatedAt(LocalDateTime.now());
            orderLineRepository.save(line);
            produitService.updateTotalStock(produit, line.getQuantityReceived(), 0);
        });

        logsService.create(
            TransactionType.ENTREE_STOCK,
            "bed.entry",
            new Object[]{commande.getReceiptReference()},
            commande.getId().getId().toString()
        );

        commande.setOrderStatus(OrderStatut.CLOSED);
        commande.setUpdatedAt(LocalDateTime.now());
        commandeRepository.save(commande);
        return findById(bedId, commande.getOrderDate());
    }

    @Override
    public void deleteBed(Integer bedId, LocalDate orderDate) {
        Commande commande = loadBed(bedId, orderDate);
        if (commande.getOrderStatus() != OrderStatut.REQUESTED) {
            throw new GenericError("Seul un BED en brouillon peut être supprimé", "bedNonSupprimable");
        }
        commandeRepository.delete(commande);
    }

    private Commande loadBed(Integer bedId, LocalDate orderDate) {
        return commandeRepository.findById(new CommandeId(bedId, orderDate))
            .filter(c -> c.getType() == TypeDeliveryReceipt.DIRECT)
            .orElseThrow(() -> new GenericError("BED introuvable", "bedIntrouvable"));
    }

    @Override
    public String createBedFromImport(MotifBed motif, Integer fournisseurId, List<BedImportLigneDTO> lignes) {
        if (lignes.isEmpty()) return null;

        LocalDate today = LocalDate.now();
        Commande commande = new Commande();
        commande.setId(commandeIdGeneratorService.getNextIdAsInt());
        commande.setOrderDate(today);
        commande.setReceiptDate(today);
        commande.setType(TypeDeliveryReceipt.DIRECT);
        commande.setOrderStatus(OrderStatut.CLOSED);
        commande.setMotifBed(motif);
        commande.setReceiptReference(generateBedReference(today));
        commande.setOrderReference(referenceService.buildNumCommande());
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(commande.getCreatedAt());
        commande.setUser(storageService.getUser());
        if (fournisseurId != null) {
            commande.setFournisseur(fournisseurRepository.getReferenceById(fournisseurId));
        }

        int totalGross = 0;
        for (BedImportLigneDTO dto : lignes) {
            FournisseurProduit fp = fournisseurProduitRepository.getReferenceById(dto.fournisseurProduitId());
            OrderLine line = new OrderLine();
            line.setId(orderLineIdGeneratorService.getNextIdAsInt());
            line.setOrderDate(today);
            line.setCommande(commande);
            line.setFournisseurProduit(fp);
            line.setQuantityRequested(dto.quantite());
            line.setQuantityReceived(dto.quantite());
            line.setOrderCostAmount(dto.prixAchat());
            line.setOrderUnitPrice(dto.prixVente());
            line.setInitStock(0);
            line.setFinalStock(dto.quantite());
            line.setDiscountAmount(0);
            line.setUpdated(Boolean.FALSE);
            line.setCreatedAt(LocalDateTime.now());
            line.setUpdatedAt(line.getCreatedAt());
            commande.getOrderLines().add(line);
            totalGross +=  dto.quantite() * dto.prixAchat();
        }
        commande.setGrossAmount(totalGross);
        commande.setDiscountAmount(0);
        commande.setTaxAmount(0);
        commande.setHtAmount(0);

        commandeRepository.save(commande);
        orderLineRepository.saveAll(commande.getOrderLines());
        inventoryTransactionService.saveAll(commande.getOrderLines());

        logsService.create(
            TransactionType.ENTREE_STOCK,
            "bed.import",
            new Object[]{commande.getReceiptReference(), motif.name()},
            commande.getId().getId().toString()
        );

        return commande.getReceiptReference();
    }

    private String generateBedReference(LocalDate date) {
        String prefix = "BED-" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = commandeRepository.countByTypeAndOrderDate(TypeDeliveryReceipt.DIRECT, date);
        return prefix + String.format("%03d", count + 1);
    }
}
