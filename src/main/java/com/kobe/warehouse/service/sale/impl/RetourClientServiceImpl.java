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
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.RetourClientRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.sale.RetourClientService;
import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientLineDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest.RetourLineRequest;
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

    public RetourClientServiceImpl(
        RetourClientRepository retourClientRepository,
        SalesRepository salesRepository,
        SalesLineRepository salesLineRepository,
        StockProduitRepository stockProduitRepository,
        AvoirClientRepository avoirClientRepository,
        ReferenceService referenceService,
        StorageService storageService, InventoryTransactionService inventoryTransactionService
    ) {
        this.retourClientRepository = retourClientRepository;
        this.salesRepository = salesRepository;
        this.salesLineRepository = salesLineRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.avoirClientRepository = avoirClientRepository;
        this.referenceService = referenceService;
        this.storageService = storageService;
        this.inventoryTransactionService = inventoryTransactionService;
    }

    @Override
    @Transactional(readOnly = true)
    public SaleForRetourDTO findSaleByRef(String numberTransaction) {
        List<Sales> found = salesRepository.findByNumberTransaction(numberTransaction);
        if (found.isEmpty()) {
            throw new GenericError("Vente introuvable : " + numberTransaction);
        }
        Sales sale = found.getFirst();
        List<SalesLine> lines = salesLineRepository.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(
            sale.getId().getId(), sale.getSaleDate());

        String customerName = buildCustomerName(sale.getCustomer());

        List<SaleLineForRetourDTO> lineDTOs = lines.stream()
            .filter(sl -> sl.getQuantitySold() > 0)
            .map(this::toSaleLineForRetourDTO)
            .toList();

        return new SaleForRetourDTO(
            sale.getId().getId(),
            sale.getSaleDate(),
            sale.getNumberTransaction(),
            customerName,
            lineDTOs
        );
    }

    //TODO: ajouter la gestion des lot,LotLocation, la gestion de scan des produits retournés, par le clients
    @Override
    public RetourClientDTO validerRetour(RetourClientRequest request) {
        if (CollectionUtils.isEmpty(request.lines())) {
            throw new GenericError("Au moins une ligne est requise");
        }

        var user = storageService.getUser();
        int storageId = storageService.getDefaultConnectedUserMainStorage().getId();
        LocalDateTime now = LocalDateTime.now();

        RetourClient retour = new RetourClient()
            .setReference(referenceService.buildNumRetourClient())
            .setMotif(request.motif())
            .setModeReglement(request.modeReglement())
            .setCommentaire(request.commentaire())
            .setCreatedBy(user)
            .setValidatedBy(user)
            .setValidatedAt(now);

        int total = 0;
        List<RetourClientLine> retourLines = new ArrayList<>();

        for (RetourLineRequest lineReq : request.lines()) {
            SalesLine sl = salesLineRepository.findById(new SaleLineId(lineReq.salesLineId(), lineReq.salesLineDate()))
                .orElseThrow(() -> new GenericError("Ligne de vente introuvable : " + lineReq.salesLineId()));

            if (lineReq.quantite() <= 0 || lineReq.quantite() > sl.getQuantitySold()) {
                throw new GenericError(
                    "Quantité invalide pour " + sl.getProduit().getLibelle() +
                    " (max retournable : " + sl.getQuantitySold() + ")");
            }
                Sales sale = sl.getSales();
                retour.setOriginalSaleId(sale.getId().getId())
                    .setOriginalSaleDate(sale.getSaleDate())
                    .setOriginalSaleRef(sale.getNumberTransaction())
                    .setCustomer(sale.getCustomer());


            // Restore stock
            StockProduit stock = stockProduitRepository.findOneByProduitIdAndStockageId(
                sl.getProduit().getId(), storageId);

                int stockInit=stock.getQtyStock();
                stock.setQtyStock(stockInit + lineReq.quantite());
                stockProduitRepository.save(stock);


            int montantLigne = lineReq.quantite() * sl.getRegularUnitPrice();
            total += montantLigne;

            RetourClientLine line = new RetourClientLine()
                .setRetourClient(retour)
                .setQuantiteInit(stockInit)
                .setProduit(sl.getProduit())
                .setQuantite(lineReq.quantite())
                .setPrixUnitaire(sl.getRegularUnitPrice())
                .setPrixAchat(sl.getCostAmount())
                .setMontant(montantLigne)
                .setOriginalSalesLineId(sl.getId().getId())
                .setOriginalSalesLineDate(sl.getSaleDate());
            retourLines.add(line);
            inventoryTransactionService.save(line);
        }

        retour.setMontantTotal(total).setLines(retourLines);
        RetourClient saved = retourClientRepository.save(retour);


        if (request.modeReglement() == ModeReglementRetour.AVOIR_CLIENT) {
            createAvoirsFromRetour(saved, user);
        }

        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RetourClientDTO> findAll(String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return retourClientRepository.findAll(
            RetourClientRepository.buildSpec(search, fromDate, toDate), pageable
        ).map(this::toDTO);
    }

    private void createAvoirsFromRetour(RetourClient retour, com.kobe.warehouse.domain.AppUser user) {
        List<AvoirClient> avoirs = retour.getLines().stream()
            .map(line -> new AvoirClient()
                .setReference(referenceService.buildNumAvoirClient())
                .setProduit(line.getProduit())
                .setCustomer(retour.getCustomer())
                .setQuantite(line.getQuantite())
                .setMontant(line.getMontant())
                .setStatut(AvoirClientStatut.OUVERT)
                .setCreatedBy(user))
            .toList();
        avoirClientRepository.saveAll(avoirs);
    }

    private RetourClientDTO toDTO(RetourClient r) {
        String customerName = buildCustomerName(r.getCustomer());
        String createdByName = r.getCreatedBy() != null
            ? r.getCreatedBy().getFirstName() + " " + r.getCreatedBy().getLastName()
            : null;
        List<RetourClientLineDTO> lineDTOs = r.getLines().stream()
            .map(this::toLineDTO)
            .toList();
        return new RetourClientDTO(
            r.getId(), r.getReference(), r.getCreatedAt(),
            r.getMotif(), r.getModeReglement(), r.getCommentaire(),
            r.getMontantTotal(), customerName, r.getOriginalSaleRef(),
            r.getOriginalSaleDate(), createdByName, lineDTOs
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
            line.getMontant()
        );
    }

    private SaleLineForRetourDTO toSaleLineForRetourDTO(SalesLine sl) {
        Produit p = sl.getProduit();
        FournisseurProduit fp = p != null ? p.getFournisseurProduitPrincipal() : null;
        String codeCip = fp != null ? fp.getCodeCip()
            : (p != null ? p.getCodeEanLaboratoire() : null);
        return new SaleLineForRetourDTO(
            sl.getId().getId(),
            sl.getSaleDate(),
            p != null ? p.getLibelle() : null,
            codeCip,
            sl.getQuantitySold(),
            sl.getNetUnitPrice()
        );
    }

    private String buildCustomerName(Customer customer) {
        if (customer == null) return null;
        return (customer.getFirstName() + " " + Objects.requireNonNullElse(customer.getLastName(), "")).strip();
    }
}
