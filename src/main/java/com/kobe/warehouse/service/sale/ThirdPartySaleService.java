package com.kobe.warehouse.service.sale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.KeyValue;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import com.kobe.warehouse.service.errors.NumBonAlreadyUseException;
import com.kobe.warehouse.service.errors.PlafondVenteException;
import com.kobe.warehouse.service.errors.PrivilegeException;
import com.kobe.warehouse.service.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.errors.ThirdPartySalesTiersPayantException;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.sale.dto.UpdateSale;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface ThirdPartySaleService {
    ThirdPartySaleLine clone(ThirdPartySaleLine original, ThirdPartySales copy);

    List<ThirdPartySaleLine> findAllBySaleId(SaleId saleId);

    void copySale(ThirdPartySales sales, ThirdPartySales copy);

    void updateClientTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine);

    void updateTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine);

    int buildConsommationId();

    default int buildConsommationId(@NotNull String s) {
        return Integer.parseInt(s);
    }

    SaleLineDTO createOrUpdateSaleLine(SaleLineDTO dto) throws PlafondVenteException;

    void deleteSaleLineById(SaleLineId id);

    ThirdPartySaleDTO createSale(ThirdPartySaleDTO dto) throws GenericError, PlafondVenteException;

    SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO)
        throws StockException, DeconditionnementStockOut, PlafondVenteException;

    SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) throws PlafondVenteException;

    void cancelSale(SaleId id);

    ResponseDTO putThirdPartySaleOnHold(ThirdPartySaleDTO dto);

    void updateDate(ThirdPartySaleDTO dto);

    FinalyseSaleDTO save(ThirdPartySaleDTO dto)
        throws SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, PlafondVenteException;

    SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) throws PlafondVenteException;

    void deleteSalePrevente(SaleId id);

    void addThirdPartySaleLineToSales(ClientTiersPayantDTO dto, Long saleId)
        throws GenericError, NumBonAlreadyUseException, PlafondVenteException;

    void removeThirdPartySaleLineToSales(Long clientTiersPayantId, SaleId saleId) throws PlafondVenteException;

    SaleId changeCashSaleToThirdPartySale(SaleId saleId, NatureVente natureVente);

    void updateTransformedSale(ThirdPartySaleDTO dto) throws PlafondVenteException;

    void changeCustomer(UpdateSaleInfo updateSaleInfo) throws GenericError, PlafondVenteException;

    FinalyseSaleDTO editSale(ThirdPartySaleDTO dto)
        throws SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, PlafondVenteException;

    void authorizeAction(UtilisationCleSecuriteDTO utilisationCleSecuriteDTO) throws PrivilegeException;

    void processDiscount(UpdateSaleInfo updateSaleInfo);

    void updateCustomerInformation(UpdateSale updateSale) throws InvalidPhoneNumberException, GenericError, JsonProcessingException;
}
