package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.GrilleRemise;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Remise;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.records.QuantitySuggestion;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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

    public SalesLineServiceImpl(
        ProduitRepository produitRepository,
        SalesLineRepository salesLineRepository,
        StockProduitRepository stockProduitRepository,
        LogsService logsService,
        SuggestionProduitService suggestionProduitService,
        LotService lotService,
        InventoryTransactionService inventoryTransactionService
    ) {
        this.produitRepository = produitRepository;
        this.salesLineRepository = salesLineRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.logsService = logsService;
        this.suggestionProduitService = suggestionProduitService;
        this.lotService = lotService;
        this.inventoryTransactionService = inventoryTransactionService;
    }

    protected SalesLine setCommonSaleLine(SaleLineDTO dto, Long stockageId) {
        Produit produit = produitRepository.getReferenceById(dto.getProduitId());
        Tva tva = produit.getTva();
        SalesLine salesLine = new SalesLine();
        salesLine.setTaxValue(tva.getTaux());
        salesLine.setCreatedAt(LocalDateTime.now());
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.costAmount(produit.getCostAmount());
        salesLine.setProduit(produit);
        salesLine.setSalesAmount(dto.getQuantityRequested() * dto.getRegularUnitPrice());
        salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantitySold(dto.getQuantitySold());
        salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantityRequested(dto.getQuantityRequested());
        salesLine.setDiscountAmount(0);
        salesLine.setDiscountUnitPrice(0);
        processUg(salesLine, dto, stockageId);
        return salesLine;
    }

    @Override
    public Sales createSaleLine(SaleLineDTO saleLine, Sales sale, Long stockageId) throws StockException {
        SalesLine salesLine;
        Optional<SalesLine> optionalSalesLine = salesLineRepository.findBySalesIdAndProduitId(
            saleLine.getSaleId(),
            saleLine.getProduitId()
        );
        if (optionalSalesLine.isPresent()) {
            salesLine = optionalSalesLine.get();
            Produit produit = produitRepository.getReferenceById(saleLine.getProduitId());
            if ((salesLine.getQuantitySold() + saleLine.getQuantitySold()) > 0/*produit.getQuantity()*/) {
                throw new StockException();
            } else {
                salesLine.setQuantitySold(salesLine.getQuantitySold() + saleLine.getQuantitySold());
                salesLine.setSalesAmount(salesLine.getQuantitySold() * saleLine.getRegularUnitPrice());
                sale.setCostAmount(sale.getCostAmount() + (salesLine.getQuantitySold() * produit.getCostAmount()));
                sale.setSalesAmount(sale.getSalesAmount() + salesLine.getSalesAmount());
            }
        } else {
            salesLine = createSaleLineFromDTO(saleLine, stockageId);
            sale.addSalesLine(salesLine);
        }
        salesLineRepository.save(salesLine);
        return sale;
    }

    @Override
    public void updateSaleLine(SaleLineDTO dto, SalesLine salesLine) {
        salesLine.setQuantitySold(dto.getQuantitySold());
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setSalesAmount(dto.getQuantitySold() * dto.getRegularUnitPrice());
        salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
        salesLineRepository.save(salesLine);
    }

    @Override
    public void updateItemQuantitySold(SalesLine salesLine, SaleLineDTO saleLineDTO, Long storageId) {
        updateItemQuantitySold(saleLineDTO, salesLine, storageId);
        salesLineRepository.save(salesLine);
    }

    @Override
    public SalesLine getOneById(Long id) {
        return salesLineRepository.getReferenceById(id);
    }

    @Override
    public void processUg(SalesLine salesLine, SaleLineDTO dto, Long stockageId) {
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
        } else if (sales instanceof ThirdPartySales) {
            return grilleRemises.stream().filter(grilleRemise -> grilleRemise.getCode() == codeRemise.getCodeVo()).findFirst();
        }
        return Optional.empty();
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
        SalesLine salesLine = new SalesLine();
        salesLine.setCreatedAt(dto.getCreatedAt());
        salesLine.setUpdatedAt(dto.getUpdatedAt());
        salesLine.costAmount(dto.getCostAmount());
        salesLine.setProduit(produit);
        salesLine.setSalesAmount(dto.getSalesAmount());
        salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
        salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
        salesLine.setDiscountAmount(dto.getDiscountAmount());
        salesLine.setDiscountUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantitySold(dto.getQuantitySold());
        salesLine.setQuantityRequested(dto.getQuantityRequested());
        salesLine.setQuantityAvoir(dto.getQuantiyAvoir());
        salesLine.setQuantityUg(dto.getQuantityUg());
        salesLine.setToIgnore(dto.isToIgnore());
        salesLine.setTaxValue(dto.getTaxValue());
        salesLine.setAmountToBeTakenIntoAccount(dto.getAmountToBeTakenIntoAccount());
        salesLine.setEffectiveUpdateDate(dto.getEffectiveUpdateDate());
        return salesLine;
    }

    @Override
    public SalesLine create(SaleLineDTO dto, Long storageId, Sales sales) {
        SalesLine salesLine = createSaleLineFromDTO(dto, storageId);
        salesLine.setSales(sales);
        return salesLineRepository.save(salesLine);
    }

    private void updateSalesLine(SalesLine salesLine, SaleLineDTO dto, Long stockageId) {
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.setSalesAmount((salesLine.getQuantityRequested() + dto.getQuantityRequested()) * dto.getRegularUnitPrice());
        salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantitySold(salesLine.getQuantitySold() + dto.getQuantitySold());
        salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
        salesLine.setQuantityRequested(salesLine.getQuantityRequested() + dto.getQuantityRequested());
        processUg(salesLine, dto, stockageId);
        // processProductDiscount(salesLine);
    }

    @Override
    public Optional<SalesLine> findBySalesIdAndProduitId(Long salesId, Long produitId) {
        return salesLineRepository.findBySalesIdAndProduitId(salesId, produitId);
    }

    @Override
    public void saveSalesLine(SalesLine salesLine) {
        salesLineRepository.save(salesLine);
    }

    @Override
    public void updateSaleLine(SaleLineDTO dto, SalesLine salesLine, Long storageId) {
        updateSalesLine(salesLine, dto, storageId);
        salesLineRepository.save(salesLine);
    }

    @Override
    public void updateItemRegularPrice(SaleLineDTO saleLineDTO, SalesLine salesLine, Long storageId) {
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.setRegularUnitPrice(saleLineDTO.getRegularUnitPrice());
        salesLine.setSalesAmount(salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice());
        processUg(salesLine, saleLineDTO, storageId);
        //processProductDiscount(salesLine);
        salesLineRepository.save(salesLine);
    }

    @Override
    public void cloneSalesLine(Set<SalesLine> salesLines, Sales copy, User user, Long storageId) {
        salesLines.forEach(salesLine -> {
            salesLine.setUpdatedAt(LocalDateTime.now());
            salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
            cloneSalesLine(salesLine, copy, storageId);
        });
    }

    @Override
    public void createInventory(SalesLine salesLine, User user, Long storageId) {
        //   InventoryTransaction inventoryTransaction = inventoryTransactionRepository.buildInventoryTransaction(salesLine, user);
        Produit p = salesLine.getProduit();
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(p.getId(), storageId);
        int quantityBefor = stockProduit.getQtyStock() + stockProduit.getQtyUG();
        int quantityAfter = quantityBefor - salesLine.getQuantityRequested();
        //    inventoryTransaction.setQuantityBefor(quantityBefor);
        //   inventoryTransaction.setQuantityAfter(quantityAfter);
        //  inventoryTransactionRepository.save(inventoryTransaction);
        if (quantityBefor < salesLine.getQuantityRequested()) {
            logsService.create(TransactionType.FORCE_STOCK, TransactionType.FORCE_STOCK.getValue(), salesLine.getId().toString());
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
            logsService.create(TransactionType.MODIFICATION_PRIX_PRODUCT_A_LA_VENTE, desc, salesLine.getId().toString());
        }
        stockProduit.setQtyStock(stockProduit.getQtyStock() - (salesLine.getQuantityRequested() - salesLine.getQuantityUg()));
        stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLine.getQuantityUg());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(stockProduit);
    }

    @Async
    @Override
    public void createInventory(Set<SalesLine> salesLines, User user, Long storageId) {
        if (!CollectionUtils.isEmpty(salesLines)) {
            salesLines.forEach(salesLine -> createInventory(salesLine, user, storageId));
        }
    }

    @Override
    public void save(Set<SalesLine> salesLines, User user, Long storageId) {
        List<QuantitySuggestion> quantitySuggestions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(salesLines)) {
            salesLines.forEach(salesLine -> {
                Produit p = salesLine.getProduit();
                StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(p.getId(), storageId);
                updateSaleLineLotSold(salesLine);
                save(salesLine, stockProduit, p);
                this.inventoryTransactionService.save(salesLine);
                quantitySuggestions.add(new QuantitySuggestion(salesLine.getQuantityRequested(), stockProduit, p));
            });
        }
        this.suggestionProduitService.suggerer(quantitySuggestions);
    }

    private void updateSaleLineLotSold(SalesLine salesLine) {
        int quantitySold = salesLine.getQuantitySold();
        AtomicInteger quantityToUpdate = new AtomicInteger(salesLine.getQuantitySold());
        this.lotService.findByProduitId(salesLine.getProduit().getId()).forEach(lot -> {
                if (quantityToUpdate.get() > 0) {
                    if (lot.getQuantity() >= quantitySold) {
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

    private void save(SalesLine salesLine, StockProduit stockProduit, Produit p) {
        int quantityBefor = stockProduit.getTotalStockQuantity();
        int quantityAfter = quantityBefor - salesLine.getQuantityRequested();
        salesLine.setInitStock(quantityBefor);
        salesLine.setAfterStock(quantityAfter);

        if (quantityBefor < salesLine.getQuantityRequested()) {
            logsService.create(TransactionType.FORCE_STOCK, TransactionType.FORCE_STOCK.getValue(), salesLine.getId().toString());
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
            logsService.create(TransactionType.MODIFICATION_PRIX_PRODUCT_A_LA_VENTE, desc, salesLine.getId().toString());
        }
        stockProduit.setQtyStock(stockProduit.getQtyStock() - (salesLine.getQuantityRequested() - salesLine.getQuantityUg()));
        stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLine.getQuantityUg());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(stockProduit);
        this.salesLineRepository.save(salesLine);
    }

    private SalesLine cloneSalesLine(SalesLine salesLine, Sales copy, Long storageId) {
        SalesLine salesLineCopy = (SalesLine) salesLine.clone();
        salesLineCopy.setId(null);
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

        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(salesLine.getProduit().getId(), storageId);
        int quantityBefor = stockProduit.getQtyStock() + stockProduit.getQtyUG();
        int quantityAfter = quantityBefor - (salesLineCopy.getQuantityRequested() - salesLineCopy.getQuantityUg());
        salesLineCopy.setInitStock(quantityBefor);
        salesLineCopy.setAfterStock(quantityAfter);
        salesLineRepository.save(salesLineCopy);
        salesLineRepository.save(salesLine);
        updateStock(stockProduit, salesLineCopy);
        return salesLineCopy;
    }

    private void updateStock(StockProduit stockProduit, SalesLine salesLineCopy) {
        stockProduit.setQtyStock(stockProduit.getQtyStock() - (salesLineCopy.getQuantityRequested()/*- salesLineCopy.getQuantityUg()*/));
        stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLineCopy.getQuantityUg());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(stockProduit);
    }

    private void updateItemQuantitySold(SaleLineDTO saleLineDTO, SalesLine salesLine, Long storageId) {
        salesLine.setQuantitySold(saleLineDTO.getQuantitySold());
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.setQuantityAvoir(salesLine.getQuantityRequested() - salesLine.getQuantitySold());
        processUg(salesLine, saleLineDTO, storageId);
        //processProductDiscount(salesLine);
    }

    @Override
    public void updateItemQuantityRequested(SaleLineDTO saleLineDTO, SalesLine salesLine, Long storageId)
        throws StockException, DeconditionnementStockOut {
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(saleLineDTO.getProduitId(), storageId);
        int quantity = stockProduit.getQtyStock();
        if (saleLineDTO.getQuantityRequested() > quantity && !saleLineDTO.isForceStock()) {
            if (salesLine.getProduit().getParent() == null) {
                throw new StockException();
            } else {
                throw new DeconditionnementStockOut(salesLine.getProduit().getParent().getId().toString());
            }
        }
        salesLine.setQuantityRequested(saleLineDTO.getQuantityRequested());
        salesLine.setQuantitySold(salesLine.getQuantityRequested());
        salesLine.setUpdatedAt(LocalDateTime.now());
        salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
        salesLine.setSalesAmount(salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice());
        processUg(salesLine, saleLineDTO, storageId);
        //    processProductDiscount(salesLine);
        salesLineRepository.save(salesLine);
    }
}
