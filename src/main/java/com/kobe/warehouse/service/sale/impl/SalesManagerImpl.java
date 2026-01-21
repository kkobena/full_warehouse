package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.repository.VenteDepotRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.PlafondVenteException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.SalesManager;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class SalesManagerImpl implements SalesManager {

    private final SalesLineService salesLineService;
    private final StorageService storageService;
    private final CustomerDisplayService customerDisplayService;
    private final CashSaleRepository cashSaleRepository;
    private final VenteDepotRepository venteDepotRepository;
    private final SaleService saleService;
    private final ThirdPartySaleService thirdPartySaleService;
    private final SaleCommonService saleCommonService;

    public SalesManagerImpl(
        SaleLineServiceFactory saleLineServiceFactory,
        StorageService storageService,
        CustomerDisplayService customerDisplayService,
        CashSaleRepository cashSaleRepository,
        VenteDepotRepository venteDepotRepository,
        @Lazy SaleService saleService,
        @Lazy ThirdPartySaleService thirdPartySaleService,
        SaleCommonService saleCommonService
    ) {
        this.salesLineService = saleLineServiceFactory.getService(null);
        this.storageService = storageService;
        this.customerDisplayService = customerDisplayService;
        this.cashSaleRepository = cashSaleRepository;
        this.venteDepotRepository = venteDepotRepository;
        this.saleService = saleService;
        this.thirdPartySaleService = thirdPartySaleService;
        this.saleCommonService = saleCommonService;
    }

    @Override
    public SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO, Sales sales)
        throws StockException, DeconditionnementStockOut {
        SalesLine salesLine = getOneSalesLine(saleLineDTO);
        salesLineService.updateItemQuantityRequested(
            saleLineDTO,
            salesLine,
            storageService.getDefaultConnectedUserMainStorage().getId()
        );
        finalizeSaleUpdate(sales);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO, Sales sales) {
        SalesLine salesLine = getOneSalesLine(saleLineDTO);
        salesLineService.updateItemQuantitySold(salesLine, saleLineDTO, storageService.getDefaultConnectedUserMainStorage().getId());
        finalizeSaleUpdate(sales);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO, Sales sales) {
        SalesLine salesLine = getOneSalesLine(saleLineDTO);
        salesLineService.updateItemRegularPrice(saleLineDTO, salesLine, storageService.getDefaultConnectedUserMainStorage().getId());
        finalizeSaleUpdate(sales);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public SaleLineDTO addOrUpdateSaleLine(SaleLineDTO dto, Sales sales) {
        Optional<SalesLine> salesLineOp = salesLineService.findBySalesIdAndProduitId(dto.getSaleCompositeId(), dto.getProduitId());
        int storageId = storageService.getDefaultConnectedUserMainStorage().getId();
        SalesLine salesLine;
        if (salesLineOp.isPresent()) {
            salesLine = salesLineOp.get();
            salesLineService.updateSaleLine(dto, salesLine, storageId);
        } else {
            salesLine = salesLineService.create(dto, storageId, sales);
            salesLine.setSales(sales);
            sales.getSalesLines().add(salesLine);
        }
        finalizeSaleUpdate(sales);
        return new SaleLineDTO(salesLine);
    }

    @Override
    public void deleteSaleLineById(SalesLine salesLine) {
        Sales sales = salesLine.getSales();
        sales.removeSalesLine(salesLine);
        sales.setUpdatedAt(LocalDateTime.now());
        sales.setEffectiveUpdateDate(sales.getUpdatedAt());
        salesLineService.deleteSaleLine(salesLine);

        switch (sales) {
            case CashSale cashSale -> {
                upddateCashSaleAmountsOnRemovingItem(cashSale, salesLine);
                cashSaleRepository.save(cashSale);
                this.displayNet(cashSale.getNetAmount());
            }
            case ThirdPartySales thirdPartySales -> {
                upddateThirdPartySaleAmountsOnRemovingItem(thirdPartySales);
                this.displayNet(thirdPartySales.getPartAssure());
            }
            case VenteDepot venteDepot -> {
                upddateDepotSaleAmountsOnRemovingItem(venteDepot, salesLine);
                venteDepotRepository.save(venteDepot);
                this.displayNet(venteDepot.getNetAmount());
            }

            default -> throw new IllegalStateException("Unexpected value: " + sales);
        }
    }

    private SalesLine getOneSalesLine(SaleLineDTO saleLineDTO) {
        return salesLineService.getOneById(saleLineDTO.getSaleLineId());
    }

    private void finalizeSaleUpdate(Sales sales) {
        if (sales instanceof CashSale cashSale) {
            saleService.upddateCashSaleAmounts(cashSale);
            cashSaleRepository.saveAndFlush(cashSale);
            this.displayNet(cashSale.getNetAmount());
        } else if (sales instanceof ThirdPartySales thirdPartySales) {
            var message = thirdPartySaleService.computeThirdPartySaleAmounts(thirdPartySales);
            this.displayNet(thirdPartySales.getPartAssure());
            if (StringUtils.hasLength(message)) {
                throw new PlafondVenteException(new ThirdPartySaleDTO(thirdPartySales), message);
            }
        } else if (sales instanceof VenteDepot venteDepot) {
            upddateDepotSaleAmounts(venteDepot);
            venteDepotRepository.saveAndFlush(venteDepot);
            this.displayNet(venteDepot.getNetAmount());
        }
    }

    private void upddateCashSaleAmountsOnRemovingItem(CashSale c, SalesLine saleLine) {
        saleService.upddateCashSaleAmountsOnRemovingItem(c, saleLine);
    }

    private void upddateThirdPartySaleAmountsOnRemovingItem(ThirdPartySales thirdPartySales) {
        thirdPartySaleService.upddateSaleAmountsOnRemovingItem(thirdPartySales);
    }

    private void displayNet(Integer net) {
        customerDisplayService.displaySaleTotal(net);
    }

    private void upddateDepotSaleAmountsOnRemovingItem(VenteDepot venteDepot, SalesLine saleLine) {
        saleCommonService.computeSaleEagerAmountOnRemovingItem(venteDepot, saleLine);
        saleCommonService.proccessDiscount(venteDepot);
        computeDepotAmountToPaid(venteDepot);
        saleCommonService.computeSaleLazyAmountOnRemovingItem(venteDepot, saleLine);
        saleCommonService.computeTvaAmountOnRemovingItem(venteDepot, saleLine);
    }

    private void upddateDepotSaleAmounts(VenteDepot venteDepot) {
        saleCommonService.computeSaleEagerAmount(venteDepot);
        saleCommonService.proccessDiscount(venteDepot);
        computeDepotAmountToPaid(venteDepot);
        saleCommonService.arrondirMontantCaisse(venteDepot);
    }

    private void computeDepotAmountToPaid(VenteDepot venteDepot) {
        venteDepot.setAmountToBePaid(venteDepot.getNetAmount());
        venteDepot.setRestToPay(venteDepot.getAmountToBePaid());
        venteDepot.setAmountToBeTakenIntoAccount(0);
    }
}
