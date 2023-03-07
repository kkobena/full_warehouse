package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.SalesLineService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.web.rest.errors.DeconditionnementStockOut;
import com.kobe.warehouse.web.rest.errors.StockException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class SalesLineServiceImpl implements SalesLineService {
  private final Logger log = LoggerFactory.getLogger(SalesLineServiceImpl.class);
  private final ProduitRepository produitRepository;
  private final SalesLineRepository salesLineRepository;
  private final StockProduitRepository stockProduitRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final LogsService logsService;

  public SalesLineServiceImpl(
      ProduitRepository produitRepository,
      SalesLineRepository salesLineRepository,
      StockProduitRepository stockProduitRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      LogsService logsService) {
    this.produitRepository = produitRepository;
    this.salesLineRepository = salesLineRepository;
    this.stockProduitRepository = stockProduitRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.logsService = logsService;
  }

  @Override
  public Sales createSaleLine(SaleLineDTO saleLine, Sales sale, Long stockageId)
      throws StockException {
    SalesLine salesLine;
    Optional<SalesLine> optionalSalesLine =
        salesLineRepository.findBySalesIdAndProduitId(
            saleLine.getSaleId(), saleLine.getProduitId());
    if (optionalSalesLine.isPresent()) {
      salesLine = optionalSalesLine.get();
      Produit produit = produitRepository.getReferenceById(saleLine.getProduitId());
      if ((salesLine.getQuantitySold() + saleLine.getQuantitySold())
          > 0 /*produit.getQuantity()*/) {
        throw new StockException();
      } else {
        salesLine.setQuantitySold(salesLine.getQuantitySold() + saleLine.getQuantitySold());
        salesLine.setSalesAmount(salesLine.getQuantitySold() * saleLine.getRegularUnitPrice());
        sale.setCostAmount(
            sale.getCostAmount() + (salesLine.getQuantitySold() * produit.getCostAmount()));
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
    salesLine.setUpdatedAt(Instant.now());
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
    StockProduit stockProduit =
        stockProduitRepository.findOneByProduitIdAndStockageId(dto.getProduitId(), stockageId);
    if (stockProduit.getQtyUG() > 0) {
      if (salesLine.getQuantitySold() >= stockProduit.getQtyUG()) {
        salesLine.setQuantityUg(stockProduit.getQtyUG());
      } else {
        salesLine.setQuantityUg(dto.getQuantitySold());
      }
    }
  }

  @Override
  public void processProductDiscount(RemiseProduit remiseProduit, SalesLine salesLine) {
    if (remiseProduit == null) {
      salesLine.setNetAmount(salesLine.getSalesAmount());
    } else if (remiseProduit.isEnable() && remiseProduit.getPeriod().isAfter(LocalDateTime.now())) {
      int discount =
          (int)
              Math.ceil(
                  salesLine.getSalesAmount()
                      * remiseProduit.getRemiseValue()); // getRemiseValue set /100 a la creation;
      salesLine.setDiscountAmount(discount);
      salesLine.setNetAmount(salesLine.getSalesAmount() - discount);
      salesLine.setDiscountAmountHorsUg(discount);
      if (salesLine.getQuantityUg() > 0) {
        int discountHUg =
            (int)
                Math.ceil(
                    ((salesLine.getQuantityRequested() - salesLine.getQuantityUg())
                            * salesLine.getRegularUnitPrice())
                        * remiseProduit.getRemiseValue());
        salesLine.setDiscountAmountHorsUg(discountHUg);
        discountHUg =
            (int)
                Math.ceil(
                    (salesLine.getQuantityUg() * salesLine.getRegularUnitPrice())
                        * remiseProduit.getRemiseValue());
        salesLine.setDiscountAmountUg(discountHUg);
      }
    }
  }

  @Override
  public void deleteSaleLine(SalesLine salesLine) {
    salesLineRepository.delete(salesLine);
  }

  @Override
  public SalesLine buildSaleLineFromDTO(SaleLineDTO dto) {
    Produit produit =
        produitRepository.findOneByLibelle(dto.getProduitLibelle().trim()).orElseThrow();
    SalesLine salesLine = new SalesLine();
    salesLine.setCreatedAt(dto.getCreatedAt());
    salesLine.setUpdatedAt(dto.getUpdatedAt());
    salesLine.costAmount(dto.getCostAmount());
    salesLine.setProduit(produit);
    salesLine.setNetAmount(dto.getNetAmount());
    salesLine.setSalesAmount(dto.getSalesAmount());
    salesLine.setNetAmount(dto.getSalesAmount());
    salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
    salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
    salesLine.setDiscountAmount(dto.getDiscountAmount());
    salesLine.setDiscountUnitPrice(dto.getRegularUnitPrice());
    salesLine.setQuantitySold(dto.getQuantitySold());
    salesLine.setQuantityRequested(dto.getQuantityRequested());
    salesLine.setQuantityAvoir(dto.getQuantiyAvoir());
    salesLine.setQuantityUg(dto.getQuantityUg());
    salesLine.setMontantTvaUg(dto.getMontantTvaUg());
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

  @Override
  public SalesLine createSaleLineFromDTO(SaleLineDTO dto, Long stockageId) {
    Produit produit = produitRepository.getReferenceById(dto.getProduitId());
    Tva tva = produit.getTva();
    SalesLine salesLine = new SalesLine();
    salesLine.setTaxValue(tva.getTaux());
    salesLine.setCreatedAt(Instant.now());
    salesLine.setUpdatedAt(Instant.now());
    salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
    salesLine.costAmount(produit.getCostAmount());
    salesLine.setProduit(produit);
    salesLine.setSalesAmount(dto.getQuantityRequested() * dto.getRegularUnitPrice());
    salesLine.setNetAmount(salesLine.getSalesAmount());
    salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
    salesLine.setQuantitySold(dto.getQuantitySold());
    salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
    salesLine.setQuantityRequested(dto.getQuantityRequested());
    salesLine.setDiscountAmount(0);
    salesLine.setDiscountUnitPrice(0);
    processUg(salesLine, dto, stockageId);
    processProductDiscount(produit.getRemise(), salesLine);
    return salesLine;
  }

  private void updateSalesLine(SalesLine salesLine, SaleLineDTO dto, Long stockageId) {
    salesLine.setUpdatedAt(Instant.now());
    salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
    salesLine.setSalesAmount(
        (salesLine.getQuantityRequested() + dto.getQuantityRequested())
            * dto.getRegularUnitPrice());
    salesLine.setNetAmount(salesLine.getSalesAmount());
    salesLine.setNetUnitPrice(dto.getRegularUnitPrice());
    salesLine.setQuantitySold(salesLine.getQuantitySold() + dto.getQuantitySold());
    salesLine.setRegularUnitPrice(dto.getRegularUnitPrice());
    salesLine.setQuantityRequested(salesLine.getQuantityRequested() + dto.getQuantityRequested());
    processUg(salesLine, dto, stockageId);
    processProductDiscount(salesLine.getProduit().getRemise(), salesLine);
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
    salesLine.setUpdatedAt(Instant.now());
    salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
    salesLine.setRegularUnitPrice(saleLineDTO.getRegularUnitPrice());
    salesLine.setSalesAmount(salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice());
    processUg(salesLine, saleLineDTO, storageId);
    processProductDiscount(salesLine.getProduit().getRemise(), salesLine);
    salesLineRepository.save(salesLine);
  }

  @Override
  public void cloneSalesLine(Set<SalesLine> salesLines, Sales copy, User user, Long storageId) {
    salesLines.forEach(
        salesLine -> {
          salesLine.setUpdatedAt(Instant.now());
          salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
          SalesLine salesLineCopy = cloneSalesLine(salesLine, copy);
          createInventoryAnnulation(salesLineCopy, user, storageId);
        });
  }

  @Override
  public void createInventory(SalesLine salesLine, User user, Long storageId) {
    InventoryTransaction inventoryTransaction =
        inventoryTransactionRepository.buildInventoryTransaction(salesLine, user);
    Produit p = salesLine.getProduit();
    StockProduit stockProduit =
        stockProduitRepository.findOneByProduitIdAndStockageId(p.getId(), storageId);
    int quantityBefor = stockProduit.getQtyStock() + stockProduit.getQtyUG();
    int quantityAfter = quantityBefor - salesLine.getQuantityRequested();
    inventoryTransaction.setQuantityBefor(quantityBefor);
    inventoryTransaction.setQuantityAfter(quantityAfter);
    inventoryTransactionRepository.save(inventoryTransaction);
    if (quantityBefor < salesLine.getQuantityRequested()) {
      logsService.create(
          TransactionType.FORCE_STOCK,
          TransactionType.FORCE_STOCK.getValue(),
          salesLine.getId().toString());
    }
    FournisseurProduit fournisseurProduitPrincipal = p.getFournisseurProduitPrincipal();
    if (fournisseurProduitPrincipal != null
        && fournisseurProduitPrincipal.getPrixUni() < salesLine.getRegularUnitPrice()) {
      String desc =
          String.format(
              "Le prix de vente du produit %s %s a été modifié sur la vente %s prix usuel:  %d prix sur la vente %s",
              fournisseurProduitPrincipal != null ? fournisseurProduitPrincipal.getCodeCip() : "",
              p.getLibelle(),
              fournisseurProduitPrincipal != null ? fournisseurProduitPrincipal.getPrixUni() : null,
              salesLine.getRegularUnitPrice(),
              salesLine.getSales().getNumberTransaction());
      logsService.create(
          TransactionType.MODIFICATION_PRIX_PRODUCT_A_LA_VENTE, desc, salesLine.getId().toString());
    }
    stockProduit.setQtyStock(
        stockProduit.getQtyStock()
            - (salesLine.getQuantityRequested() - salesLine.getQuantityUg()));
    stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLine.getQuantityUg());
    stockProduit.setUpdatedAt(Instant.now());
    stockProduitRepository.save(stockProduit);
  }

  @Async
  @Override
  public void createInventory(Set<SalesLine> salesLines, User user, Long storageId) {
    if (!CollectionUtils.isEmpty(salesLines)) {
      salesLines.forEach(salesLine -> createInventory(salesLine, user, storageId));
    }
  }

  private SalesLine cloneSalesLine(SalesLine salesLine, Sales copy) {
    SalesLine salesLineCopy = (SalesLine) salesLine.clone();
    salesLineCopy.setId(null);
    salesLineCopy.setCreatedAt(Instant.now());
    salesLineCopy.setSales(copy);
    salesLineCopy.setUpdatedAt(salesLineCopy.getCreatedAt());
    salesLineCopy.setEffectiveUpdateDate(salesLineCopy.getUpdatedAt());
    salesLineCopy.setMontantTvaUg(salesLineCopy.getMontantTvaUg() * (-1));
    salesLineCopy.setSalesAmount(salesLineCopy.getSalesAmount() * (-1));
    salesLineCopy.setNetAmount(salesLineCopy.getNetAmount() * (-1));
    salesLineCopy.setQuantityAvoir(salesLineCopy.getQuantityAvoir() * (-1));
    salesLineCopy.setQuantitySold(salesLineCopy.getQuantitySold() * (-1));
    salesLineCopy.setQuantityUg(salesLineCopy.getQuantityUg() * (-1));
    salesLineCopy.setQuantityRequested(salesLineCopy.getQuantityRequested() * (-1));
    salesLineCopy.setDiscountAmountHorsUg(salesLineCopy.getDiscountAmountHorsUg() * (-1));
    salesLineCopy.setDiscountAmount(salesLineCopy.getDiscountAmount() * (-1));
    salesLineCopy.setDiscountAmountUg(salesLineCopy.getDiscountAmountUg() * (-1));
    salesLineCopy.setAmountToBeTakenIntoAccount(
        salesLineCopy.getAmountToBeTakenIntoAccount() * (-1));
    salesLineRepository.save(salesLineCopy);
    salesLineRepository.save(salesLine);
    return salesLineCopy;
  }

  private void createInventoryAnnulation(SalesLine salesLine, User user, Long storageId) {
    InventoryTransaction inventoryTransaction =
        inventoryTransactionRepository.buildInventoryTransaction(
            salesLine, TransactionType.CANCEL_SALE, user);
    Produit p = salesLine.getProduit();
    StockProduit stockProduit =
        stockProduitRepository.findOneByProduitIdAndStockageId(p.getId(), storageId);
    int quantityBefor = stockProduit.getQtyStock() + stockProduit.getQtyUG();
    int quantityAfter = quantityBefor - salesLine.getQuantityRequested();
    inventoryTransaction.setQuantityBefor(quantityBefor);
    inventoryTransaction.setQuantityAfter(quantityAfter);
    inventoryTransactionRepository.save(inventoryTransaction);
    stockProduit.setQtyStock(
        stockProduit.getQtyStock()
            - (salesLine.getQuantityRequested() - salesLine.getQuantityUg()));
    stockProduit.setQtyUG(stockProduit.getQtyUG() - salesLine.getQuantityUg());
    stockProduit.setUpdatedAt(Instant.now());
    stockProduitRepository.save(stockProduit);
  }

  private void updateItemQuantitySold(
      SaleLineDTO saleLineDTO, SalesLine salesLine, Long storageId) {
    salesLine.setQuantitySold(saleLineDTO.getQuantitySold());
    salesLine.setUpdatedAt(Instant.now());
    salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
    salesLine.setQuantityAvoir(salesLine.getQuantityRequested() - salesLine.getQuantitySold());
    processUg(salesLine, saleLineDTO, storageId);
    processProductDiscount(salesLine.getProduit().getRemise(), salesLine);
  }

  @Override
  public void updateItemQuantityRequested(
      SaleLineDTO saleLineDTO, SalesLine salesLine, Long storageId)
      throws StockException, DeconditionnementStockOut {
    StockProduit stockProduit =
        stockProduitRepository.findOneByProduitIdAndStockageId(
            saleLineDTO.getProduitId(), storageId);
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
    salesLine.setUpdatedAt(Instant.now());
    salesLine.setEffectiveUpdateDate(salesLine.getUpdatedAt());
    salesLine.setSalesAmount(salesLine.getQuantityRequested() * salesLine.getRegularUnitPrice());
    processUg(salesLine, saleLineDTO, storageId);
    processProductDiscount(salesLine.getProduit().getRemise(), salesLine);
    salesLineRepository.save(salesLine);
  }
}
