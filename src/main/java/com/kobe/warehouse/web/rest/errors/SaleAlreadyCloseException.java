package com.kobe.warehouse.web.rest.errors;

public class SaleAlreadyCloseException extends BadRequestAlertException {
  private static final long serialVersionUID = 1L;

  public SaleAlreadyCloseException(long venteId) {
    super(
        ErrorConstants.SALE_ALREADY_CLOSED,
        String.format("Vente [%s] déjà cloturée", venteId + ""),
        "sales",
        "saleAlreadyClosed");
  }
}
