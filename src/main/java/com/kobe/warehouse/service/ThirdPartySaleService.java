package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleLineDTO;
import com.kobe.warehouse.web.rest.errors.DeconditionnementStockOut;
import com.kobe.warehouse.web.rest.errors.GenericError;
import com.kobe.warehouse.web.rest.errors.NumBonAlreadyUseException;
import com.kobe.warehouse.web.rest.errors.PaymentAmountException;
import com.kobe.warehouse.web.rest.errors.SaleNotFoundCustomerException;
import com.kobe.warehouse.web.rest.errors.StockException;
import com.kobe.warehouse.web.rest.errors.ThirdPartySalesTiersPayantException;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

public interface ThirdPartySaleService {

  ThirdPartySales computeCarnetAmounts(
      ThirdPartySaleDTO thirdPartySaleDTO, ThirdPartySales thirdPartySales)
      throws GenericError, NumBonAlreadyUseException;

  void processDiscount(ThirdPartySales thirdPartySales, SalesLine saleLine, SalesLine oldSaleLine);

  void processDiscountWhenRemovingItem(ThirdPartySales thirdPartySales, SalesLine saleLine);

  void computeAllAmounts(ThirdPartySales thirdPartySales);

  ThirdPartySaleLine clone(ThirdPartySaleLine original, ThirdPartySales copy);

  List<ThirdPartySaleLine> findAllBySaleId(Long saleId);

  void copySale(ThirdPartySales sales, ThirdPartySales copy);

  void updateClientTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine);

  void updateTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine);

  int buildConsommationId();

  default int buildConsommationId(@NotNull String s) {
    return Integer.valueOf(s);
  }

  String buildTvaData(Set<SalesLine> salesLines);

  SaleLineDTO createOrUpdateSaleLine(SaleLineDTO dto);

  void deleteSaleLineById(Long id);

  ThirdPartySaleDTO createSale(ThirdPartySaleDTO dto) throws GenericError;

  SaleLineDTO updateItemQuantityRequested(SaleLineDTO saleLineDTO)
      throws StockException, DeconditionnementStockOut;

  SaleLineDTO updateItemRegularPrice(SaleLineDTO saleLineDTO);

  void cancelSale(Long id);

  ResponseDTO putThirdPartySaleOnHold(ThirdPartySaleDTO dto);

  ResponseDTO save(ThirdPartySaleDTO dto)
      throws PaymentAmountException, SaleNotFoundCustomerException,
          ThirdPartySalesTiersPayantException;

  SaleLineDTO updateItemQuantitySold(SaleLineDTO saleLineDTO);

  void deleteSalePrevente(Long id);

  ThirdPartySaleDTO addThirdPartySaleLineToSales(ClientTiersPayantDTO dto, Long saleId)
      throws GenericError, NumBonAlreadyUseException;

  void removeThirdPartySaleLineToSales(Long clientTiersPayantId, Long saleId);
}
