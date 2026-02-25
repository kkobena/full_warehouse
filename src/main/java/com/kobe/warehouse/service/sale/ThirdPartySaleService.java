package com.kobe.warehouse.service.sale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.CashRegisterException;
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
import java.util.List;

public interface ThirdPartySaleService {

    ThirdPartySaleLine clone(ThirdPartySaleLine original, ThirdPartySales copy);

    List<ThirdPartySaleLine> findAllBySaleId(SaleId saleId);

    void copyThirdPartySales(ThirdPartySales sales, ThirdPartySales copy);

    void updateClientTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine);

    void updateTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine);

    SaleLineDTO createOrUpdateSaleLine(SaleLineDTO dto) throws PlafondVenteException;

    void deleteSaleLineById(SaleLineId id);

    ThirdPartySaleDTO createSale(ThirdPartySaleDTO dto) throws GenericError, PlafondVenteException;

    SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO, boolean increment)
        throws StockException, DeconditionnementStockOut, PlafondVenteException;

    SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO) throws PlafondVenteException;

    void cancelSale(SaleId id) throws CashRegisterException;

    ResponseDTO putThirdPartySaleOnHold(ThirdPartySaleDTO dto) throws PlafondVenteException;

    void updateDate(ThirdPartySaleDTO dto) throws PlafondVenteException;

    FinalyseSaleDTO save(ThirdPartySaleDTO dto)
        throws SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, PlafondVenteException;

    SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO) throws PlafondVenteException;

    void deleteSalePrevente(SaleId id);

    void addThirdPartySaleLineToSales(ClientTiersPayantDTO dto, SaleId saleId)
        throws GenericError, NumBonAlreadyUseException, PlafondVenteException;

    void removeThirdPartySaleLineToSales(Integer clientTiersPayantId, SaleId saleId)
        throws PlafondVenteException;

    SaleId changeCashSaleToThirdPartySale(SaleId saleId, NatureVente natureVente)
        throws PlafondVenteException;

    void updateTransformedSale(ThirdPartySaleDTO dto) throws PlafondVenteException;

    void changeCustomer(UpdateSaleInfo updateSaleInfo) throws GenericError, PlafondVenteException;

    FinalyseSaleDTO editSale(ThirdPartySaleDTO dto)
        throws SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, PlafondVenteException;

    void authorizeAction(UtilisationCleSecuriteDTO utilisationCleSecuriteDTO)
        throws PrivilegeException;

    void processDiscount(UpdateSaleInfo updateSaleInfo) throws PlafondVenteException;

    void updateCustomerInformation(UpdateSale updateSale)
        throws InvalidPhoneNumberException, GenericError, JsonProcessingException;

    String computeThirdPartySaleAmounts(ThirdPartySales thirdPartySales)
        throws PlafondVenteException;

    void upddateSaleAmountsOnRemovingItem(ThirdPartySales c) throws PlafondVenteException;

    void savePrevente(ThirdPartySaleDTO dto, boolean transform)
        throws SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, PlafondVenteException;

    void removeDiscount(SaleId saleId) throws PlafondVenteException;

    /**
     * Copie une vente pour l'édition, en s'assurant que les données sont à jour et conformes aux
     * règles métier. Annuler la vente originale pour éviter les conflits de données.
     */
    SaleId copiePourEdition(SaleId saleId)
        throws SaleNotFoundCustomerException, ThirdPartySalesTiersPayantException, PlafondVenteException, CashRegisterException;

    SaleId transformToVenteEncour(SaleId saleId);

    void cloneDevis(SaleId saleId);

    void addAyantDroitToSale(UpdateSaleInfo updateSaleInfo) ;


}
