package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.KeyValue;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.web.rest.errors.DeconditionnementStockOut;
import com.kobe.warehouse.web.rest.errors.GenericError;
import com.kobe.warehouse.web.rest.errors.NumBonAlreadyUseException;
import com.kobe.warehouse.web.rest.errors.PaymentAmountException;
import com.kobe.warehouse.web.rest.errors.PlafondVenteException;
import com.kobe.warehouse.web.rest.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.web.rest.errors.StockException;
import com.kobe.warehouse.web.rest.errors.ThirdPartySalesTiersPayantException;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

public interface ThirdPartySaleService {
    void processDiscount(ThirdPartySales thirdPartySales, SalesLine saleLine, SalesLine oldSaleLine);

    void processDiscountWhenRemovingItem(ThirdPartySales thirdPartySales, SalesLine saleLine);

    ThirdPartySaleLine clone(ThirdPartySaleLine original, ThirdPartySales copy);

    List<ThirdPartySaleLine> findAllBySaleId(Long saleId);

    void copySale(ThirdPartySales sales, ThirdPartySales copy);

    void updateClientTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine);

    void updateTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine);

    int buildConsommationId();

    default int buildConsommationId(@NotNull String s) {
        return Integer.parseInt(s);
    }

    String buildTvaData(Set<SalesLine> salesLines);

    SaleLineDTO createOrUpdateSaleLine(SaleLineDTO dto) throws PlafondVenteException;

    void deleteSaleLineById(Long id);

    ThirdPartySaleDTO createSale(ThirdPartySaleDTO dto) throws GenericError, PlafondVenteException;

    SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO)
        throws StockException, DeconditionnementStockOut, PlafondVenteException;

    SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) throws PlafondVenteException;

    void cancelSale(Long id);

    ResponseDTO putThirdPartySaleOnHold(ThirdPartySaleDTO dto);

    FinalyseSaleDTO save(ThirdPartySaleDTO dto)
        throws PaymentAmountException, SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, PlafondVenteException;

    SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) throws PlafondVenteException;

    void deleteSalePrevente(Long id);

    void addThirdPartySaleLineToSales(ClientTiersPayantDTO dto, Long saleId)
        throws GenericError, NumBonAlreadyUseException, PlafondVenteException;

    void removeThirdPartySaleLineToSales(Long clientTiersPayantId, Long saleId) throws PlafondVenteException;

    Long changeCashSaleToThirdPartySale(Long saleId, NatureVente natureVente);

    void updateTransformedSale(ThirdPartySaleDTO dto) throws PlafondVenteException;

    void changeCustomer(KeyValue keyValue) throws GenericError, PlafondVenteException;
}
