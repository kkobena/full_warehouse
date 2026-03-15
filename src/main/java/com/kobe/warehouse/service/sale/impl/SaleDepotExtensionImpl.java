package com.kobe.warehouse.service.sale.impl;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.RemiseRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.repository.VenteDepotRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.CashRegisterException;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.sale.SaleDepotExtensionService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.SalesManager;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.sale.dto.VenteDepotTransactionRecord;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SaleDepotExtensionImpl extends SaleCommonService implements SaleDepotExtensionService {

    private final StorageService storageService;
    private final VenteDepotRepository venteDepotRepository;
    private final SalesLineService salesLineService;
    private final RemiseRepository remiseRepository;
    private final CashRegisterService cashRegisterService;
    private final StockUpdateService stockUpdateService;
    private final InventoryTransactionService inventoryTransactionService;
    private final SalesManager salesManager;

    public SaleDepotExtensionImpl(
        RemiseRepository remiseRepository,
        ReferenceService referenceService,
        StorageService storageService,
        UserRepository userRepository,
        SaleLineServiceFactory saleLineServiceFactory,
        CashRegisterService cashRegisterService,
        PosteRepository posteRepository,
        CustomerDisplayService afficheurPosService,
        SaleIdGeneratorService idGeneratorService,
        VenteDepotRepository venteDepotRepository,
        StockUpdateService stockUpdateService,
        InventoryTransactionService inventoryTransactionService,
        ObjectMapper objectMapper,
        SalesManager salesManager, AppConfigurationService appConfigurationService
    ) {
        super(
            referenceService,
            storageService,
            userRepository,
            saleLineServiceFactory,
            cashRegisterService,
            posteRepository,
            afficheurPosService,
            idGeneratorService, objectMapper, appConfigurationService
        );
        this.salesLineService = saleLineServiceFactory.getService(TypeVente.VenteDepot);
        this.storageService = storageService;
        this.venteDepotRepository = venteDepotRepository;
        this.remiseRepository = remiseRepository;
        this.cashRegisterService = cashRegisterService;
        this.stockUpdateService = stockUpdateService;
        this.inventoryTransactionService = inventoryTransactionService;
        this.salesManager = salesManager;
    }

    @Override
    public DepotExtensionSaleDTO create(DepotExtensionSaleDTO dto) {
        Magasin magasin = new Magasin();
        magasin.setId(dto.getMagasin().getId());
        VenteDepot venteDepot = new VenteDepot();
        this.intSale(dto, venteDepot);
        venteDepot.setCategorieChiffreAffaire(CategorieChiffreAffaire.CA_DEPOT);
        venteDepot.setDepot(magasin);
        venteDepot.setNatureVente(NatureVente.ASSURANCE); //TODO a supprimer

        SalesLine saleLine = salesLineService.createSaleLineFromDTO(
            dto.getSalesLines().getFirst(),
            storageService.getDefaultConnectedUserMainStorage().getId()
        );
        venteDepot.getSalesLines().add(saleLine);
        upddateAmounts(venteDepot);

        var venteDepote = venteDepotRepository.save(venteDepot);
        saleLine.setSales(venteDepote);

        salesLineService.saveSalesLine(saleLine);
        // this.displayNet(venteDepot.getNetAmount());
        return new DepotExtensionSaleDTO(venteDepote);
    }

    @Override
    public SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO, boolean increment)
        throws StockException, DeconditionnementStockOut {
        return salesManager.updateItemQuantityRequested(saleLineDTO,
            findOne(saleLineDTO.getSaleCompositeId()), increment);
    }

    @Override
    public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) {
        return salesManager.updateItemQuantitySold(saleLineDTO,
            findOne(saleLineDTO.getSaleCompositeId()));
    }

    @Override
    public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) {
        return salesManager.updateItemRegularPrice(saleLineDTO,
            findOne(saleLineDTO.getSaleCompositeId()));
    }


    @Override
    public SaleLineDTO addOrUpdateSaleLine(SaleLineDTO dto) {
        return salesManager.addOrUpdateSaleLine(dto, findOne(dto.getSaleCompositeId()));
    }


    private VenteDepot findOne(SaleId id) {
        return this.venteDepotRepository.getReferenceById(id);
    }


    @Override
    public FinalyseSaleDTO save(DepotExtensionSaleDTO dto)
        throws PaymentAmountException, SaleNotFoundCustomerException, CashRegisterException {
        VenteDepot venteDepot = venteDepotRepository
            .findOneWithEagerSalesLines(dto.getSaleId().getId(), dto.getSaleId().getSaleDate())
            .orElseThrow();
        if (isNull(venteDepot.getDepot())) {
            throw new GenericError("Le dépôt de la vente n'est pas défini.");
        }

        prevalideSale(venteDepot);
        finalizeSale(venteDepot, dto);
        venteDepot.setTvaEmbeded(buildTvaData(venteDepot.getSalesLines()));

        venteDepotRepository.save(venteDepot);

        return new FinalyseSaleDTO(venteDepot.getId(), true);
    }

    @Override
    public void deleteSaleLineById(SaleLineId id) {
        salesManager.deleteSaleLineById(salesLineService.getOneById(id));
    }

    @Override
    public void deleteSalePrevente(SaleId id) {
        VenteDepot venteDepot = venteDepotRepository.getReferenceById(id);
        venteDepot.getSalesLines().forEach(salesLineService::deleteSaleLine);
        venteDepotRepository.delete(venteDepot);
    }

    @Override
    public void cancel(SaleId id) {
        AppUser user = storageService.getUser();
        VenteDepot venteDepot = venteDepotRepository.getReferenceById(id);
        VenteDepot copy = (VenteDepot) venteDepot.clone();
        copySale(venteDepot, copy);
        setId(copy);
        copy.setSaleDate(LocalDate.now());
        venteDepot.setEffectiveUpdateDate(LocalDateTime.now());
        venteDepot.setCanceled(true);
        copy.setCanceled(true);
        venteDepotRepository.save(venteDepot);
        venteDepotRepository.save(copy);

        salesLineService.cloneSalesLine(
            venteDepot.getSalesLines(),
            copy,
            user,
            storageService.getDefaultConnectedUserMainStorage().getId()
        );
    }


    @Override
    public void processDiscount(UpdateSaleInfo keyValue) {
        venteDepotRepository
            .findById(keyValue.id())
            .ifPresent(venteDepot -> remiseRepository
                .findById(keyValue.value())
                .ifPresent(remise -> {
                    if (venteDepot.getRemise() != null) {
                        this.removeRemise(venteDepot);
                    }
                    this.applyRemiseProduit(venteDepot, (RemiseProduit) remise);
                    computeAmountToPaid(venteDepot);
                    arrondirMontantCaisse(venteDepot);
                    this.venteDepotRepository.save(venteDepot);
                }));
    }

    @Override
    public void removeRemiseFromSale(SaleId saleId) {
        VenteDepot sales = findOne(saleId);
        this.removeRemise(sales);
        this.venteDepotRepository.save(sales);
        //  this.displayNet(sales.getNetAmount());
    }


    @Override
    public void changeDepot(SaleId saleId, Integer depotId) {
        venteDepotRepository
            .findById(saleId)
            .ifPresentOrElse(
                s -> {
                    Magasin depot = new Magasin();
                    depot.setId(depotId);
                    s.setDepot(depot);
                    venteDepotRepository.save(s);
                },
                () -> {
                    throw new GenericError("Vente introuvable");
                }
            );
    }

    private void upddateAmounts(VenteDepot venteDepot) {
        computeSaleEagerAmount(venteDepot);
        this.proccessDiscount(venteDepot);
        computeAmountToPaid(venteDepot);
        arrondirMontantCaisse(venteDepot);
    }

    private void computeAmountToPaid(VenteDepot c) {
        c.setAmountToBePaid(c.getNetAmount());
        c.setRestToPay(c.getAmountToBePaid());
        c.setAmountToBeTakenIntoAccount(0);
    }

    private void finalizeSale(VenteDepot c, DepotExtensionSaleDTO dto) {
        AppUser user = storageService.getUser();
        c.setUser(user);
        CashRegister cashRegister = cashRegisterService.getLastOpiningUserCashRegisterByUser(user);
        if (Objects.isNull(cashRegister)) {
            cashRegister = cashRegisterService.openCashRegister(user, user);
        }
        c.setCashRegister(cashRegister);
        int id = storageService.getDefaultConnectedUserMainStorage().getId();
        salesLineService.save(c.getSalesLines(), user, id);
        updateDepotStockOnSaleFinalization(c);
        c.setStatut(SalesStatut.CLOSED);
        c.setPayrollAmount(dto.getPayrollAmount());
        c.setRestToPay(dto.getRestToPay());
        c.setUpdatedAt(LocalDateTime.now());
        c.setEffectiveUpdateDate(c.getUpdatedAt());
        c.setPaymentStatus(PaymentStatus.IMPAYE);
        c.setRestToPay(c.getAmountToBePaid());
        this.buildReference(c);
    }

    private void updateDepotStockOnSaleFinalization(VenteDepot venteDepot) {
        Magasin depot = venteDepot.getDepot();
        Storage storage = depot.getPrimaryStorage();
        List<VenteDepotTransactionRecord> venteDepotTransactionRecords = new ArrayList<>();
        Set<SalesLine> salesLines = venteDepot.getSalesLines();
        for (SalesLine salesLine : salesLines) {
            StockUpdateService.StockUpdateResult result = stockUpdateService.updateStockDepot(
                salesLine, storage);
            venteDepotTransactionRecords.add(
                new VenteDepotTransactionRecord(result.quantityBefore(), result.quantityAfter(),
                    salesLine)
            );
        }
        inventoryTransactionService.saveVenteDepotExtensionInventoryTransactions(depot,
            venteDepotTransactionRecords);
    }
}
