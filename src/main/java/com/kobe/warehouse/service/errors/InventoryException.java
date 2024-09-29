package com.kobe.warehouse.service.errors;

public class InventoryException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public InventoryException() {
        super("Toutes les lignes ne sont pas renseignées", "someInventoryLineNotUpdated");
    }
}
