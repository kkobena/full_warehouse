package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
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
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.records.QuantitySuggestion;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.QuantitySoldException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.errors.StockInReserveException;
import com.kobe.warehouse.service.id_generator.SaleLineIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
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
    private final SuggestionProduitService suggestionProduitService;
    private final LotService lotService;
    private final InventoryTransactionService inventoryTransactionService;
    private final SaleLineIdGeneratorService saleLineIdGeneratorService;
    private final StockUpdateService stockUpdateService;
    private final StorageService storageService;
    private final RepartitionStockService repartitionStockService;

    protected SalesLineServiceImpl(
        ProduitRepository produitRepository,
        SalesLineRepository salesLineRepository,
        StockProduitRepository stockProduitRepository,
        SuggestionProduitService suggestionProduitService,
        LotService lotService,
        InventoryTransactionService inventoryTransactionService,
        SaleLineIdGeneratorService saleLineIdGeneratorService,
        StockUpdateService stockUpdateService,
        StorageService storageService,
        RepartitionStockService repartitionStockService
    ) {
        this.produitRepository = produitRepository;
        this.salesLineRepository = salesLineRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.suggestionProduitService = suggestionProduitService;
        this.lotService = lotService;
        this.inventoryTransactionService = inventoryTransactionService;
        this.saleLineIdGeneratorService = saleLineIdGeneratorService;
        this.stockUpdateService = stockUpdateService;
        this.storageService = storageService;
        this.repartitionStockService = repartitionStockService;
    }

    private SalesLine getNew() {
        SalesLine salesLine = new SalesLine();
        salesLine.setId(saleLineIdGeneratorService.nextId());
        return salesLine;
    }

    protected SalesLine setCommonSaleLine(SaleLineDTO dto, Integer stockageId) throws StockException, DeconditionnementStockOut {
        Produit produit = produitRepository.getReferenceById(dto.getProduitId());
        int currentStockQuantity = getCurrentStockQuantity(produit.getId(), dto.getQuantityRequested(), dto.isForceStock());

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
        // buildSaleLineFromDTO est utilisé pour l'import de données : pas de forceStock
        salesLine.setQuantitySold(calculateQuantitySold(salesLine.getQuantityRequested(), getCurrentStockQuantity(produit.getId(), salesLine.getQuantityRequested(), false)));
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

    /**
     * Retourne le stock rayon utilisable pour la vente, en gérant les trois scénarios :
     *
     * <ol>
     *   <li><b>Rayon ≥ demande</b> → retourne rayonStock, vente normale.</li>
     *   <li><b>Rayon &lt; demande ET réserve &gt; 0</b> :
     *     <ul>
     *       <li>{@code forceStock=false} → lève {@link StockInReserveException} :
     *           le frontend choisit entre transfert explicite ou vente urgente.</li>
     *       <li>{@code forceStock=true} → transfert implicite atomique réserve→rayon
     *           de {@code min(reserveStock, demande - rayon)} unités, puis retourne
     *           le nouveau stock rayon (rayon + transféré). Si le total reste insuffisant,
     *           {@code calculateQuantitySold} plafonnera à ce total et l'écart partira en avoir.</li>
     *     </ul>
     *   </li>
     *   <li><b>Rayon &lt; demande ET réserve = 0</b> → retourne rayonStock ;
     *       l'appelant gère via {@code forceStock} (vente en avoir si force, StockException sinon).</li>
     * </ol>
     */
    private int getCurrentStockQuantity(Integer produitId, int quantityRequested, boolean forceStock) {
        var mainStorage = storageService.getDefaultConnectedUserMainStorage();
        int rayonStock = stockProduitRepository.findPointVenteStock(produitId, mainStorage.getId());

        if (rayonStock < quantityRequested) {
            var reserveStorage = storageService.getDefaultConnectedUserReserveStorage();
            if (reserveStorage != null) {
                int reserveStock = stockProduitRepository.findReserveStock(produitId, reserveStorage.getId());
                if (reserveStock > 0) {
                    if (!forceStock) {
                        throw new StockInReserveException(rayonStock, reserveStock);
                    }
                    // Option B : transfert implicite atomique, journalisé via RepartitionStockProduit
                    int toTransfer = Math.min(reserveStock, quantityRequested - rayonStock);
                    repartitionStockService.transfertImpliciteReserveVersRayon(
                        produitId, mainStorage.getId(), reserveStorage.getId(), toTransfer);
                    return rayonStock + toTransfer;
                }
            }
        }
        return rayonStock;
    }

    private void updateSalesLine(SalesLine salesLine, SaleLineDTO dto, Integer stockageId) throws StockException {
        int quantityRequested = salesLine.getQuantityRequested() + dto.getQuantityRequested();

        int currentStockQuantity = getCurrentStockQuantity(salesLine.getProduit().getId(), quantityRequested, dto.isForceStock());
        processItemQuantityRequested(quantityRequested, salesLine, currentStockQuantity, dto.isForceStock());
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
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(
            salesLine.getProduit().getId(), storageId);
        updateSaleLineLotSold(salesLine);
        save(salesLine, stockProduit);
        this.inventoryTransactionService.save(salesLine);
    }

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
        AtomicInteger remaining = new AtomicInteger(quantitySold);
        this.lotService.findByProduitId(salesLine.getProduit().getId()).forEach(lot -> {
            if (remaining.get() > 0) {
                // FEFO garanti par le tri ORDER BY expiryDate ASC de la requête
                int toTake = Math.min(remaining.get(), lot.getCurrentQuantity());
                if (toTake > 0) {
                    salesLine.getLots().add(new LotSold(lot.getId(), lot.getNumLot(), toTake, lot.getExpiryDate()));
                    remaining.addAndGet(-toTake);
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

        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(salesLine.getProduit().getId(), storageId);
        int quantityBefor = stockProduit.getQtyStock() + stockProduit.getQtyUG();
        int quantityAfter = quantityBefor - (salesLineCopy.getQuantityRequested() - salesLineCopy.getQuantityUg());
        salesLineCopy.setInitStock(quantityBefor);
        salesLineCopy.setAfterStock(quantityAfter);
        salesLineRepository.save(salesLineCopy);
        salesLineRepository.save(salesLine);
        // Restaure le stock rayon et crée une suggestion réassort réserve si rayon > stockMaxi
        stockUpdateService.updateStockOnCancellation(salesLineCopy, stockProduit);
        this.lotService.restoreLots(salesLine.getLots());
        this.inventoryTransactionService.save(salesLineCopy);
    }

    private void updateItemQuantitySold(SaleLineDTO saleLineDTO, SalesLine salesLine, Integer storageId) {
        // Ajustement de quantité vendue : pas de transfert implicite ici
        processItemQuantitySold(saleLineDTO.getQuantitySold(), salesLine.getQuantityRequested(), getCurrentStockQuantity(salesLine.getProduit().getId(), saleLineDTO.getQuantitySold(), false));
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
        int quantity = getCurrentStockQuantity(salesLine.getProduit().getId(), saleLineDTO.getQuantityRequested(), saleLineDTO.isForceStock());
        processItemQuantityRequested(saleLineDTO, salesLine, quantity);
        salesLine.setQuantityRequested(saleLineDTO.getQuantityRequested());
        updateStock(quantity, saleLineDTO, salesLine, storageId);
    }

    @Override
    public void incrementItemQuantityRequested(SaleLineDTO saleLineDTO, SalesLine salesLine, Integer storageId)
        throws StockException, DeconditionnementStockOut {
        int totalRequested = salesLine.getQuantityRequested() + saleLineDTO.getQuantityRequested();
        int quantity = getCurrentStockQuantity(salesLine.getProduit().getId(), totalRequested, saleLineDTO.isForceStock());
        processItemQuantityRequested(saleLineDTO, salesLine, quantity);
        salesLine.setQuantityRequested(totalRequested);
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

    private void processItemQuantityRequested(Integer quantityRequested, SalesLine salesLine, int currentStock, boolean forceStock) throws StockException, DeconditionnementStockOut {

        if (quantityRequested > currentStock && !forceStock) {
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
