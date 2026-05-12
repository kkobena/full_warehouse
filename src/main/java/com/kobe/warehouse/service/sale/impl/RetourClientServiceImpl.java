package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RetourClient;
import com.kobe.warehouse.domain.RetourClientLine;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.ModeReglementRetour;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.StatutLegal;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.RetourClientRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.sale.RetourClientService;
import com.kobe.warehouse.service.sale.dto.EchangeContextDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientLineDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest.RetourLineRequest;
import com.kobe.warehouse.service.sale.dto.RetourClientResultDTO;
import com.kobe.warehouse.service.sale.dto.RetourLigneRejeteeDTO;
import com.kobe.warehouse.service.sale.dto.SaleForRetourDTO;
import com.kobe.warehouse.service.sale.dto.SaleLineForRetourDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class RetourClientServiceImpl implements RetourClientService {

    private final RetourClientRepository retourClientRepository;
    private final SalesRepository salesRepository;
    private final SalesLineRepository salesLineRepository;
    private final StockProduitRepository stockProduitRepository;
    private final AvoirClientRepository avoirClientRepository;
    private final ReferenceService referenceService;
    private final StorageService storageService;
    private final InventoryTransactionService inventoryTransactionService;
    private final AppConfigurationService appConfigurationService;

    public RetourClientServiceImpl(
        RetourClientRepository retourClientRepository,
        SalesRepository salesRepository,
        SalesLineRepository salesLineRepository,
        StockProduitRepository stockProduitRepository,
        AvoirClientRepository avoirClientRepository,
        ReferenceService referenceService,
        StorageService storageService,
        InventoryTransactionService inventoryTransactionService,
        AppConfigurationService appConfigurationService
    ) {
        this.retourClientRepository = retourClientRepository;
        this.salesRepository = salesRepository;
        this.salesLineRepository = salesLineRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.avoirClientRepository = avoirClientRepository;
        this.referenceService = referenceService;
        this.storageService = storageService;
        this.inventoryTransactionService = inventoryTransactionService;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    @Transactional(readOnly = true)
    public SaleForRetourDTO findSaleByRef(String numberTransaction) {
        List<Sales> found = salesRepository.findByNumberTransaction(numberTransaction);
        if (found.isEmpty()) {
            throw new GenericError("Vente introuvable : " + numberTransaction);
        }
        return buildSaleForRetourDTO(found.getFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public SaleForRetourDTO findSaleById(Long id, LocalDate saleDate) {
        Sales sale = salesRepository.findById(new com.kobe.warehouse.domain.SaleId(id, saleDate))
            .orElseThrow(() -> new GenericError("Vente introuvable : id=" + id + ", date=" + saleDate));
        return buildSaleForRetourDTO(sale);
    }

    private SaleForRetourDTO buildSaleForRetourDTO(Sales sale) {
        List<SalesLine> lines = salesLineRepository.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(
            sale.getId().getId(), sale.getSaleDate());
        String customerName = buildCustomerName(sale.getCustomer());
        double tauxPartAssure = computeTauxPartAssure(sale);
        boolean hasTiersPayant = sale.getNatureVente() != NatureVente.COMPTANT;
        List<SaleLineForRetourDTO> lineDTOs = lines.stream()
            .filter(sl -> sl.getQuantitySold() > 0)
            .map(sl -> toSaleLineForRetourDTO(sl, tauxPartAssure))
            .toList();
        long ancienneteJours = java.time.temporal.ChronoUnit.DAYS.between(sale.getSaleDate(), LocalDate.now());
        boolean depasseDelai = ancienneteJours > appConfigurationService.getDelaiRetourClient();
        return new SaleForRetourDTO(
            sale.getId().getId(),
            sale.getSaleDate(),
            sale.getNumberTransaction(),
            customerName,
            sale.getNatureVente(),
            hasTiersPayant,
            lineDTOs,
            ancienneteJours,
            depasseDelai
        );
    }

    //TODO: ajouter la gestion des lot,LotLocation, la gestion de scan des produits retournés, par le clients
    @Override
    public RetourClientResultDTO validerRetour(RetourClientRequest request) {
        if (CollectionUtils.isEmpty(request.lines())) {
            throw new GenericError("Au moins une ligne est requise");
        }

        var user = storageService.getUser();
        int storageId = storageService.getDefaultConnectedUserMainStorage().getId();
        LocalDateTime now = LocalDateTime.now();

        ModeReglementRetour modeReglement = request.avecEchange()
            ? ModeReglementRetour.AVOIR_CLIENT
            : request.modeReglement();

        RetourClient retour = new RetourClient()
            .setReference(referenceService.buildNumRetourClient())
            .setMotif(request.motif())
            .setModeReglement(modeReglement)
            .setCommentaire(request.commentaire())
            .setAvecEchange(request.avecEchange())
            .setCreatedBy(user)
            .setValidatedBy(user)
            .setValidatedAt(now);

        int total = 0;
        int totalTp = 0;
        List<RetourClientLine> retourLines = new ArrayList<>();
        List<RetourLigneRejeteeDTO> lignesRejetees = new ArrayList<>();
        List<RetourLigneRejeteeDTO> lignesNonRestockees = new ArrayList<>();
        double tauxPartAssure = -1; // calculé une fois sur la première ligne traitée

        for (RetourLineRequest lineReq : request.lines()) {
            SalesLine sl = salesLineRepository.findById(new SaleLineId(lineReq.salesLineId(), lineReq.salesLineDate()))
                .orElseThrow(() -> new GenericError("Ligne de vente introuvable : " + lineReq.salesLineId()));

            Produit produit = sl.getProduit();
            StatutLegal statutLegal = produit != null && produit.getStatutLegal() != null
                ? produit.getStatutLegal()
                : StatutLegal.SANS_LISTE;

            if (statutLegal.isRetourInterdit()) {
                FournisseurProduit fp = produit != null ? produit.getFournisseurProduitPrincipal() : null;
                String codeCip = fp != null ? fp.getCodeCip()
                    : (produit != null ? produit.getCodeEanLaboratoire() : null);
                lignesRejetees.add(new RetourLigneRejeteeDTO(
                    produit != null ? produit.getLibelle() : String.valueOf(lineReq.salesLineId()),
                    codeCip,
                    lineReq.quantite(),
                    statutLegal,
                    statutLegal.getDescription().split("\\.")[0]
                        + ". Ce produit ne peut pas être repris — orienter vers le circuit de destruction réglementaire."
                ));
                continue;
            }

            if (lineReq.quantite() <= 0 || lineReq.quantite() > sl.getQuantitySold()) {
                throw new GenericError(
                    "Quantité invalide pour " + (produit != null ? produit.getLibelle() : lineReq.salesLineId()) +
                    " (max retournable : " + sl.getQuantitySold() + ")");
            }

            Sales sale = sl.getSales();
            retour.setOriginalSaleId(sale.getId().getId())
                .setOriginalSaleDate(sale.getSaleDate())
                .setOriginalSaleRef(sale.getNumberTransaction())
                .setCustomer(sale.getCustomer());

            if (tauxPartAssure < 0) {
                tauxPartAssure = computeTauxPartAssure(sale);
            }

            StockProduit stock = stockProduitRepository.findOneByProduitIdAndStockageId(
                produit.getId(), storageId);
            int stockInit = stock.getQtyStock();

            boolean thermosensible = produit != null && Boolean.TRUE.equals(produit.getThermosensible());
            boolean etatOk = lineReq.etatProduitOk();
            boolean restockable = !thermosensible && etatOk;

            if (!restockable) {
                FournisseurProduit fp = produit != null ? produit.getFournisseurProduitPrincipal() : null;
                String motif = thermosensible
                    ? "Produit thermosensible — remise en stock impossible. Le produit sera enregistré en destruction."
                    : "État du produit non conforme (emballage, lot ou péremption) — envoyé en quarantaine.";
                lignesNonRestockees.add(new RetourLigneRejeteeDTO(
                    produit != null ? produit.getLibelle() : String.valueOf(lineReq.salesLineId()),
                    fp != null ? fp.getCodeCip() : (produit != null ? produit.getCodeEanLaboratoire() : null),
                    lineReq.quantite(),
                    null,
                    motif
                ));
            } else {
                stock.setQtyStock(stockInit + lineReq.quantite());
                stockProduitRepository.save(stock);
            }

            int montantBrut = lineReq.quantite() * sl.getRegularUnitPrice();
            int montantRemboursableClient = (int) Math.round(montantBrut * tauxPartAssure);
            int montantTp = montantBrut - montantRemboursableClient;
            total += montantRemboursableClient;
            totalTp += montantTp;

            RetourClientLine line = new RetourClientLine()
                .setRetourClient(retour)
                .setQuantiteInit(stockInit)
                .setStockRestitue(restockable)
                .setEmballageIntact(lineReq.emballageIntact() == null || lineReq.emballageIntact())
                .setNumLotLisible(lineReq.numLotLisible() == null || lineReq.numLotLisible())
                .setDatePeremptionValide(lineReq.datePeremptionValide() == null || lineReq.datePeremptionValide())
                .setProduit(produit)
                .setQuantite(lineReq.quantite())
                .setPrixUnitaire(sl.getRegularUnitPrice())
                .setPrixAchat(sl.getCostAmount())
                .setMontant(montantRemboursableClient)
                .setMontantTp(montantTp)
                .setOriginalSalesLineId(sl.getId().getId())
                .setOriginalSalesLineDate(sl.getSaleDate());
            retourLines.add(line);
            inventoryTransactionService.save(line);
        }

        if (retourLines.isEmpty()) {
            throw new GenericError("Aucune ligne retournable dans cette demande. "
                + "Tous les produits sélectionnés sont soumis à une restriction de retour.");
        }

        retour.setMontantTotal(total).setLines(retourLines);
        RetourClient saved = retourClientRepository.save(retour);

        List<AvoirClient> avoirs = List.of();
        if (modeReglement == ModeReglementRetour.AVOIR_CLIENT) {
            avoirs = createAvoirsFromRetour(saved, user);
        }

        RetourClientDTO retourDTO = toDTO(saved);
        boolean hasAnomalies = !lignesRejetees.isEmpty() || !lignesNonRestockees.isEmpty();

        if (request.avecEchange()) {
            EchangeContextDTO ctx = buildEchangeContext(saved, avoirs);
            return hasAnomalies
                ? RetourClientResultDTO.partielAvecEchange(retourDTO, lignesRejetees, lignesNonRestockees, ctx)
                : RetourClientResultDTO.totalAvecEchange(retourDTO, ctx);
        }
        return hasAnomalies
            ? RetourClientResultDTO.partiel(retourDTO, lignesRejetees, lignesNonRestockees)
            : RetourClientResultDTO.total(retourDTO);
    }

    @Override
    public RetourClientDTO lierVenteEchange(Integer retourId, String saleRef) {
        RetourClient retour = retourClientRepository.findById(retourId)
            .orElseThrow(() -> new GenericError("Retour introuvable : " + retourId));
        if (!retour.isAvecEchange()) {
            throw new GenericError("Ce retour n'est pas un retour avec échange");
        }
        retour.setEchangeSaleRef(saleRef);
        return toDTO(retourClientRepository.save(retour));
    }

    @Override
    @Transactional(readOnly = true)
    public RetourClientDTO findById(Integer id) {
        RetourClient retour = retourClientRepository.findById(id)
            .orElseThrow(() -> new GenericError("Retour introuvable : " + id));
        return toDTO(retour);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RetourClientDTO> findAll(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return retourClientRepository.findAll(
            RetourClientRepository.buildSpec(search, fromDate, toDate), pageable
        ).map(this::toDTO);
    }

    private List<AvoirClient> createAvoirsFromRetour(RetourClient retour, com.kobe.warehouse.domain.AppUser user) {
        LocalDate expiration = LocalDate.now().plusDays(appConfigurationService.getDelaiValiditeAvoir());
        List<AvoirClient> avoirs = retour.getLines().stream()
            .map(line -> new AvoirClient()
                .setReference(referenceService.buildNumAvoirClient())
                .setProduit(line.getProduit())
                .setCustomer(retour.getCustomer())
                .setQuantite(line.getQuantite())
                .setMontant(line.getMontant())
                .setStatut(AvoirClientStatut.OUVERT)
                .setDateExpiration(expiration)
                .setCreatedBy(user))
            .toList();
        return avoirClientRepository.saveAll(avoirs);
    }

    private EchangeContextDTO buildEchangeContext(RetourClient retour, List<AvoirClient> avoirs) {
        Customer customer = retour.getCustomer();
        String customerName = buildCustomerName(customer);
        List<String> avoirRefs = avoirs.stream().map(AvoirClient::getReference).toList();
        return new EchangeContextDTO(
            customer != null ? customer.getId() : null,
            customerName,
            retour.getMontantTotal(),
            retour.getId(),
            retour.getReference(),
            avoirRefs
        );
    }

    private RetourClientDTO toDTO(RetourClient r) {
        String customerName = buildCustomerName(r.getCustomer());
        String createdByName = r.getCreatedBy() != null
            ? r.getCreatedBy().getFirstName() + " " + r.getCreatedBy().getLastName()
            : null;
        List<RetourClientLineDTO> lineDTOs = r.getLines().stream()
            .map(this::toLineDTO)
            .toList();
        int montantTpTotal = lineDTOs.stream().mapToInt(RetourClientLineDTO::montantTp).sum();
        return new RetourClientDTO(
            r.getId(), r.getReference(), r.getCreatedAt(),
            r.getMotif(), r.getModeReglement(), r.getCommentaire(),
            r.getMontantTotal(), montantTpTotal, customerName, r.getOriginalSaleRef(),
            r.getOriginalSaleDate(), createdByName, lineDTOs,
            r.isAvecEchange(), r.getEchangeSaleRef()
        );
    }

    private RetourClientLineDTO toLineDTO(RetourClientLine line) {
        Produit p = line.getProduit();
        FournisseurProduit fp = p != null ? p.getFournisseurProduitPrincipal() : null;
        String codeCip = fp != null ? fp.getCodeCip()
            : (p != null ? p.getCodeEanLaboratoire() : null);
        return new RetourClientLineDTO(
            line.getId(),
            p != null ? p.getLibelle() : null,
            codeCip,
            line.getQuantite(),
            line.getPrixUnitaire(),
            line.getMontant(),
            line.getMontantTp()
        );
    }

    private SaleLineForRetourDTO toSaleLineForRetourDTO(SalesLine sl, double tauxPartAssure) {
        Produit p = sl.getProduit();
        FournisseurProduit fp = p != null ? p.getFournisseurProduitPrincipal() : null;
        String codeCip = fp != null ? fp.getCodeCip()
            : (p != null ? p.getCodeEanLaboratoire() : null);
        var statutLegal = p != null && p.getStatutLegal() != null
            ? p.getStatutLegal()
            : StatutLegal.SANS_LISTE;
        int montantBrut = sl.getQuantitySold() * sl.getRegularUnitPrice();
        int montantRemboursableClient = (int) Math.round(montantBrut * tauxPartAssure);
        return new SaleLineForRetourDTO(
            sl.getId().getId(),
            sl.getSaleDate(),
            p != null ? p.getLibelle() : null,
            codeCip,
            sl.getQuantitySold(),
            sl.getNetUnitPrice(),
            statutLegal,
            statutLegal.isRetourInterdit(),
            p != null && Boolean.TRUE.equals(p.getThermosensible()),
            montantRemboursableClient,
            montantBrut - montantRemboursableClient
        );
    }

    private double computeTauxPartAssure(Sales sale) {
        int salesAmount = sale.getSalesAmount() != null ? sale.getSalesAmount() : 0;
        if (salesAmount == 0) {
            return 1.0;
        }
        int partAssure = sale.getAmountToBePaid() != null ? sale.getAmountToBePaid() : salesAmount;
        return Math.min(1.0, (double) partAssure / salesAmount);
    }

    private String buildCustomerName(Customer customer) {
        if (customer == null) return null;
        return (customer.getFirstName() + " " + Objects.requireNonNullElse(customer.getLastName(), "")).strip();
    }
}
