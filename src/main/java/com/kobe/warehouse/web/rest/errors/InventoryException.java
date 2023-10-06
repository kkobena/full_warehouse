package com.kobe.warehouse.web.rest.errors;

public class InventoryException extends BadRequestAlertException {
  private static final long serialVersionUID = 1L;

  public InventoryException() {
    super(
        ErrorConstants.ALL_INVENTORY_LINE_NOT_UPDATED,
        "Toutes les lignes ne sont pas renseignées",
        "storeInventory",
        "someInventoryLineNotUpdated");
  }
}
