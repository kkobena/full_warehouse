package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.GrilleRemise;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.records.QuantitySuggestion;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.QuantitySoldException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.id_generator.SaleLineIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public abstract class SalesLineServiceImpl implements SalesLineService {

    private final ProduitRepository produitRepository;
    private final SalesLineRepository salesLineRepository;
    private final StockProduitRepository stockProduitRepository;
    private final LogsService logsService;
    private final SuggestionProduitService suggestionProduitService;
    private final LotService lotService;
    private final InventoryTransactionService inventoryTransactionService;
    private final SaleLineIdGeneratorService saleLineIdGeneratorService;
    private final StockUpdateService stockUpdateService;
    private final StorageService storageService;

    protected SalesLineServiceImpl(
        ProduitRepository produitRepository,
        SalesLineRepository salesLineRepository,
        StockProduitRepository stockProduitRepository,
        LogsService logsService,
        SuggestionProduitService suggestionProduitService,
        LotService lotService,
        InventoryTransactionService inventoryTransactionService,
        SaleLineIdGeneratorService saleLineIdGeneratorService, StockUpdateService stockUpdateService, StorageService storageService
    ) {
        this.produitRepository = produitRepository;
        this.salesLineRepository = salesLineRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.logsService = logsService;
        this.suggestionProduitService = suggestionProduitService;
        this.lotService = lotService;
        this.inventoryTransactionService = inventoryTransactionService;
        this.saleLineIdGeneratorService = saleLineIdGeneratorService;
        this.stockUpdateService = stockUpdateService;
        this.storageService = storageService;
    }

    private SalesLine getNew() {
        SalesLine salesLine = new SalesLine();
        salesLine.setId(saleLineIdGeneratorService.nextId());
        return salesLine;
    }

    protected SalesLine setCommonSaleLine(SaleLineDTO dto, Integer stockageId) throws StockException, DeconditionnementStockOut {
        Produit produit = produitRepository.getReferenceById(dto.getProduitId());
        int currentStockQuantity = getCurrentStockQuantity(produit.getId());

        if (dto.getQuantityRequested() > currentStockQuantity && !dto.isForceStock()) {
            Produit parentProduit = produit.getParent();
            if (parentProduit == null) {
                throw new StockException();
            } else {
                throw new DeconditionnementStockOut(parentProduit.getId().toString());
            }
        }
        Tva tva = produit.getTva();
        SalesLine salesLine = getNew();
        salesLine.setTaxValue(tva.getTaux());
        salesLine.setCreatedAt(LocalDateTime.now());
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.costAmount(produit.getCostAmount());
        salesLine.setProduit(produit);
        salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
        salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantityRequested(dto.getQuantityRequested());
        salesLine.setQuantitySold(calculateQuantitySold(salesLine.getQuantityRequested(), currentStockQuantity));
        salesLine.setSalesAmount(salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice());
        salesLine.setAmountToBeTakenIntoAccount(salesLine.getAmountToBeTakenIntoAccount());
        salesLine.setDiscountAmount(0);
        salesLine.setDiscountUnitPrice(0);
        processUg(salesLine, dto, stockageId);
        return salesLine;
    }


    @Override
    public void updateItemQuantitySold(SalesLine salesLine, SaleLineDTO saleLineDTO, Integer storageId) {
        updateItemQuantitySold(saleLineDTO, salesLine, storageId);
        salesLineRepository.save(salesLine);
    }

    @Override
    public SalesLine getOneById(SaleLineId id) {
        return salesLineRepository.findById(id).orElseThrow();
    }

    /**
     * Met à jour la quantité en unité de gestion (UG) pour le stockage principal.
     */
    @Override
    public void processUg(SalesLine salesLine, SaleLineDTO dto, Integer stockageId) {
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(dto.getProduitId(), stockageId);
        if (stockProduit.getQtyUG() > 0) {
            if (salesLine.getQuantitySold() >= stockProduit.getQtyUG()) {
                salesLine.setQuantityUg(stockProduit.getQtyUG());
            } else {
                salesLine.setQuantityUg(dto.getQuantitySold());
            }
        }
    }

    private Optional<GrilleRemise> getGrilleRemise(CodeRemise codeRemise, RemiseProduit remiseProduit, Sales sales) {
        List<GrilleRemise> grilleRemises = remiseProduit.getGrilles();
        if (CollectionUtils.isEmpty(grilleRemises)) {
            return Optional.empty();
        }
        if (sales instanceof CashSale) {
            return grilleRemises.stream().filter(grilleRemise -> grilleRemise.getCode() == codeRemise.getCodeVno()).findFirst();
        } else {
            return grilleRemises.stream().filter(grilleRemise -> grilleRemise.getCode() == codeRemise.getCodeVo()).findFirst();
        }

    }

    public void processProductDiscount(SalesLine salesLine) {
        Sales sales = salesLine.getSales();
        Remise remise = sales.getRemise();

        RemiseProduit remiseProduit = (RemiseProduit) remise;
        getGrilleRemise(salesLine.getProduit().getCodeRemise(), remiseProduit, sales).ifPresent(grilleRemise -> {
            int discount = (int) Math.ceil(salesLine.getSalesAmount() * grilleRemise.getTauxRemise());
            salesLine.setDiscountAmount(discount);
            salesLine.setTauxRemise(grilleRemise.getTauxRemise());
        });
    }

    @Override
    public void deleteSaleLine(SalesLine salesLine) {
        salesLineRepository.delete(salesLine);
    }

    @Override
    public SalesLine buildSaleLineFromDTO(SaleLineDTO dto) {
        Produit produit = produitRepository.findOneByLibelle(dto.getProduitLibelle().trim()).orElseThrow();
        SalesLine salesLine = getNew();
        salesLine.setCreatedAt(dto.getCreatedAt());
        salesLine.setUpdatedAt(dto.getUpdatedAt());
        salesLine.costAmount(dto.getCostAmount());
        salesLine.setProduit(produit);
        salesLine.setSalesAmount(dto.getSalesAmount());
        salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
        salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
        salesLine.setDiscountAmount(dto.getDiscountAmount());
        salesLine.setDiscountUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantityRequested(dto.getQuantityRequested());
        salesLine.setQuantitySold(calculateQuantitySold(salesLine.getQuantityRequested(), getCurrentStockQuantity(produit.getId())));
        salesLine.setQuantityAvoir(dto.getQuantiyAvoir());
        salesLine.setQuantityUg(dto.getQuantityUg());
        salesLine.setToIgnore(dto.isToIgnore());
        salesLine.setTaxValue(dto.getTaxValue());
        salesLine.setAmountToBeTakenIntoAccount(dto.getAmountToBeTakenIntoAccount());
        salesLine.setEffectiveUpdateDate(dto.getEffectiveUpdateDate());
        return salesLine;
    }

    @Override
    public SalesLine create(SaleLineDTO dto, Integer storageId, Sales sales) {
        SalesLine salesLine = createSaleLineFromDTO(dto, storageId);
        salesLine.setSales(sales);
        salesLine = salesLineRepository.save(salesLine);
        sales.getSalesLines().add(salesLine);
        return salesLine;
    }

    private int getCurrentStockQuantity(Integer produitId) {
        Integer storageId = storageService.getDefaultConnectedUserMainStorage().getId();
        return stockProduitRepository.findPointVenteStock(produitId,
            storageId
        );
    }

    private void updateSalesLine(SalesLine salesLine, SaleLineDTO dto, Integer stockageId) throws StockException {
        int quantityRequested = salesLine.getQuantityRequested() + dto.getQuantityRequested();

        int currentStockQuantity = getCurrentStockQuantity(salesLine.getProduit().getId());
        processItemQuantityRequested(quantityRequested, salesLine, currentStockQuantity);
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.setQuantityRequested(quantityRequested);
        salesLine.setSalesAmount(salesLine.getQuantityRequested() * dto.getRegularUnitPrice());
        salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantitySold(calculateQuantitySold(salesLine.getQuantityRequested(), currentStockQuantity));
        salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());

        processUg(salesLine, dto, stockageId);
        // processProductDiscount(salesLine);
    }

    @Override
    public Optional<SalesLine> findBySalesIdAndProduitId(SaleId salesId, Integer produitId) {
        return salesLineRepository.findBySalesIdAndProduitIdAndSalesSaleDate(salesId.getId(), produitId, salesId.getSaleDate());
    }

    @Override
    public void saveSalesLine(SalesLine salesLine) {
        salesLineRepository.save(salesLine);
    }

    @Override
    public void updateSaleLine(SaleLineDTO dto, SalesLine salesLine, Integer storageId) throws StockException {
        updateSalesLine(salesLine, dto, storageId);
        salesLineRepository.save(salesLine);
    }

    @Override
    public void updateItemRegularPrice(SaleLineDTO saleLineDTO, SalesLine salesLine, Integer storageId) {
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.setRegularUnitPrice(saleLineDTO.getRegularUnitPrice());
        salesLine.setSalesAmount(salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice());
        processUg(salesLine, saleLineDTO, storageId);
        //processProductDiscount(salesLine);
        salesLineRepository.save(salesLine);
    }

    @Override
    public void cloneSalesLine(Set<SalesLine> salesLines, Sales copy, AppUser user, Integer storageId) {
        salesLines.forEach(salesLine -> {
            salesLine.setUpdatedAt(LocalDateTime.now());
            salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
            cloneSalesLine(salesLine, copy, storageId);
        });
    }

    public void saveAll(Set<SalesLine> salesLines) {
        if (!CollectionUtils.isEmpty(salesLines)) {
            salesLineRepository.saveAll(salesLines);
        }
    }

    @Override
    public void createInventory(SalesLine salesLine, AppUser user, Integer storageId) {
        //   InventoryTransaction inventoryTransaction = inventoryTransactionRepository.buildInventoryTransaction(salesLine, user);
        Produit p = salesLine.getProduit();
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(p.getId(), storageId);
        int quantityBefor = stockProduit.getQtyStock() + stockProduit.getQtyUG();
        int quantityAfter = quantityBefor - salesLine.getQuantityRequested();
        //    inventoryTransaction.setQuantityBefor(quantityBefor);
        //   inventoryTransaction.setQuantityAfter(quantityAfter);
        //  inventoryTransactionRepository.save(inventoryTransaction);
        if (quantityBefor < salesLine.getQuantityRequested()) {
            logsService.create(TransactionType.FORCE_STOCK, TransactionType.FORCE_STOCK.getValue(), salesLine.getId().getId().toString());
        }
        FournisseurProduit fournisseurProduitPrincipal = p.getFournisseurProduitPrincipal();
        if (fournisseurProduitPrincipal != null && fournisseurProduitPrincipal.getPrixUni() < salesLine.getRegularUnitPrice()) {
            String desc = String.format(
                "Le prix de vente du produit %s %s a été modifié sur la vente %s prix usuel:  %d prix sur la vente %s",
                fournisseurProduitPrincipal.getCodeCip(),
                p.getLibelle(),
                fournisseurProduitPrincipal.getPrixUni(),
                salesLine.getRegularUnitPrice(),
                salesLine.getSales().getNumberTransaction()
            );
            logsService.create(TransactionType.MODIFICATION_PRIX_PRODUCT_A_LA_VENTE, desc, salesLine.getId().getId().toString());
        }
        stockProduit.setQtyStock(stockProduit.getQtyStock() - (salesLine.getQuantityRequested() - salesLine.getQuantityUg()));
        stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLine.getQuantityUg());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(stockProduit);
    }

    @Async
    @Override
    public void createInventory(Set<SalesLine> salesLines, AppUser user, Integer storageId) {
        if (!CollectionUtils.isEmpty(salesLines)) {
            salesLines.forEach(salesLine -> createInventory(salesLine, user, storageId));
        }
    }

    @Override
    public void save(Set<SalesLine> salesLines, AppUser user, Integer storageId) {
        List<QuantitySuggestion> quantitySuggestions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(salesLines)) {
            salesLines.forEach(salesLine -> {
                Produit p = salesLine.getProduit();
                StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(p.getId(), storageId);
                updateSaleLineLotSold(salesLine);
                save(salesLine, stockProduit);
                this.inventoryTransactionService.save(salesLine);
                quantitySuggestions.add(new QuantitySuggestion(salesLine.getQuantityRequested(), stockProduit, p));
            });
        }
        this.suggestionProduitService.suggerer(quantitySuggestions);
    }

    public Set<SalesLine> cloneSalesLine(Set<SalesLine> salesLines, Sales copy) {
        Set<SalesLine> copySalesLines = new HashSet<>();
        if (CollectionUtils.isEmpty(salesLines)) {
            return copySalesLines;
        }

        for (SalesLine salesLine : salesLines) {
            SalesLine salesLineCopy = (SalesLine) salesLine.clone();
            salesLineCopy.setId(getNextId());
            salesLineCopy.setSaleDate(LocalDate.now());
            salesLineCopy.setCreatedAt(LocalDateTime.now());
            salesLineCopy.setSales(copy);
            salesLineCopy.setUpdatedAt(salesLineCopy.getCreatedAt());
            salesLineCopy.setEffectiveUpdateDate(salesLineCopy.getUpdatedAt());
            copySalesLines.add(salesLineCopy);

        }


        return copySalesLines;

    }

    private void updateSaleLineLotSold(SalesLine salesLine) {
        int quantitySold = salesLine.getQuantitySold();
        if (quantitySold <= 0) {
            return;
        }
        AtomicInteger quantityToUpdate = new AtomicInteger(salesLine.getQuantitySold());
        this.lotService.findByProduitId(salesLine.getProduit().getId()).forEach(lot -> {
            if (quantityToUpdate.get() > 0) {
                if (lot.getCurrentQuantity() >= quantitySold) {
                    //long id, String numLot, int quantity
                    salesLine.getLots().add(new LotSold(lot.getId(), lot.getNumLot(), quantitySold));
                    quantityToUpdate.addAndGet(-quantitySold);
                } else {
                    quantityToUpdate.addAndGet(-lot.getQuantity());
                    salesLine.getLots().add(new LotSold(lot.getId(), lot.getNumLot(), lot.getQuantity()));
                }
            }
        });
        this.lotService.updateLots(salesLine.getLots());
    }

    private void save(SalesLine salesLine, StockProduit stockProduit) {
        StockUpdateService.StockUpdateResult result = stockUpdateService.updateStock(salesLine, stockProduit);
        salesLine.setInitStock(result.quantityBefore());
        salesLine.setAfterStock(result.quantityAfter());
        this.salesLineRepository.save(salesLine);
    }

    private void cloneSalesLine(SalesLine salesLine, Sales copy, Integer storageId) {
        SalesLine salesLineCopy = (SalesLine) salesLine.clone();
        salesLineCopy.setId(getNextId());
        salesLineCopy.setSaleDate(LocalDate.now());
        salesLineCopy.setCreatedAt(LocalDateTime.now());
        salesLineCopy.setSales(copy);
        salesLineCopy.setUpdatedAt(salesLineCopy.getCreatedAt());
        salesLineCopy.setEffectiveUpdateDate(salesLineCopy.getUpdatedAt());
        salesLineCopy.setSalesAmount(salesLineCopy.getSalesAmount() * (-1));
        salesLineCopy.setQuantityAvoir(salesLineCopy.getQuantityAvoir() * (-1));
        salesLineCopy.setQuantitySold(salesLineCopy.getQuantitySold() * (-1));
        salesLineCopy.setQuantityUg(salesLineCopy.getQuantityUg() * (-1));
        salesLineCopy.setQuantityRequested(salesLineCopy.getQuantityRequested() * (-1));
        salesLineCopy.setDiscountAmount(salesLineCopy.getDiscountAmount() * (-1));
        salesLineCopy.setAmountToBeTakenIntoAccount(salesLineCopy.getAmountToBeTakenIntoAccount() * (-1));

        //TODO: une gestion pour les reserve de stock
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(salesLine.getProduit().getId(), storageId);
        int quantityBefor = stockProduit.getQtyStock() + stockProduit.getQtyUG();
        int quantityAfter = quantityBefor - (salesLineCopy.getQuantityRequested() - salesLineCopy.getQuantityUg());
        salesLineCopy.setInitStock(quantityBefor);
        salesLineCopy.setAfterStock(quantityAfter);
        salesLineRepository.save(salesLineCopy);
        salesLineRepository.save(salesLine);
        updateStock(stockProduit, salesLineCopy);
        this.inventoryTransactionService.save(salesLineCopy);
    }

    private void updateStock(StockProduit stockProduit, SalesLine salesLineCopy) {
        stockProduit.setQtyStock(stockProduit.getQtyStock() - (salesLineCopy.getQuantityRequested()/*- salesLineCopy.getQuantityUg()*/));
        stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLineCopy.getQuantityUg());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(stockProduit);
    }

    private void updateItemQuantitySold(SaleLineDTO saleLineDTO, SalesLine salesLine, Integer storageId) {
        processItemQuantitySold(saleLineDTO.getQuantitySold(), salesLine.getQuantityRequested(), getCurrentStockQuantity(salesLine.getProduit().getId()));
        salesLine.setQuantitySold(saleLineDTO.getQuantitySold());
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.setQuantityAvoir(salesLine.getQuantityRequested() - salesLine.getQuantitySold());
        processUg(salesLine, saleLineDTO, storageId);
        //processProductDiscount(salesLine);
    }

    @Override
    public void updateItemQuantityRequested(SaleLineDTO saleLineDTO, SalesLine salesLine, Integer storageId)
        throws StockException, DeconditionnementStockOut {
        int quantity = getCurrentStockQuantity(salesLine.getProduit().getId());
        processItemQuantityRequested(saleLineDTO, salesLine, quantity);
        salesLine.setQuantityRequested(saleLineDTO.getQuantityRequested());
        updateStock(quantity, saleLineDTO, salesLine, storageId);
    }

    @Override
    public void incrementItemQuantityRequested(SaleLineDTO saleLineDTO, SalesLine salesLine, Integer storageId)
        throws StockException, DeconditionnementStockOut {
        int quantity = getCurrentStockQuantity(salesLine.getProduit().getId());
        processItemQuantityRequested(saleLineDTO, salesLine, quantity);
        salesLine.setQuantityRequested(salesLine.getQuantityRequested() + saleLineDTO.getQuantityRequested());
        updateStock(quantity, saleLineDTO, salesLine, storageId);
    }

    private void processItemQuantityRequested(SaleLineDTO saleLineDTO, SalesLine salesLine, int quantity) throws StockException, DeconditionnementStockOut {

        if (saleLineDTO.getQuantityRequested() > quantity && !saleLineDTO.isForceStock()) {
            if (salesLine.getProduit().getParent() == null) {
                throw new StockException();
            } else {
                throw new DeconditionnementStockOut(salesLine.getProduit().getParent().getId().toString());
            }
        }
    }

    private void processItemQuantityRequested(Integer quantityRequested, SalesLine salesLine, int currentStock) throws StockException, DeconditionnementStockOut {

        if (quantityRequested > currentStock) {
            if (salesLine.getProduit().getParent() == null) {
                throw new StockException();
            } else {
                throw new DeconditionnementStockOut(salesLine.getProduit().getParent().getId().toString());
            }
        }
    }

    private void processItemQuantitySold(int quantitySold, int quantityRequested, int quantity) throws StockException, QuantitySoldException {
        if (quantitySold > quantityRequested) {
            throw new QuantitySoldException();
        }
        if (quantitySold > quantity) {
            throw new StockException();
        }
    }


    private void updateStock(int quantity, SaleLineDTO saleLineDTO, SalesLine salesLine, Integer storageId) {

        salesLine.setQuantitySold(calculateQuantitySold(salesLine.getQuantityRequested(), quantity));
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.setSalesAmount(salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice());
        processUg(salesLine, saleLineDTO, storageId);
        //    processProductDiscount(salesLine);
        salesLineRepository.save(salesLine);
    }

    private int calculateQuantitySold(int quantityRequested, int currentStockQuantity) {

        if (currentStockQuantity <= 0) {
            return 0;
        }
        return Math.min(currentStockQuantity, quantityRequested);

    }

    @Override
    public List<SaleLineDTO> findBySalesIdAndSalesSaleDateOrderByProduitLibelle(Long salesId, LocalDate saleDate) {
        return salesLineRepository
            .findBySalesIdAndSalesSaleDateOrderByProduitLibelle(salesId, saleDate)
            .stream()
            .map(SaleLineDTO::new)
            .toList();
    }

    @Override
    public long getNextId() {
        return saleLineIdGeneratorService.nextId();
    }
}
