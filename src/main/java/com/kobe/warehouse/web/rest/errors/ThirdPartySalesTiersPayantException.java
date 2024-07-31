package com.kobe.warehouse.web.rest.errors;

public class ThirdPartySalesTiersPayantException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public ThirdPartySalesTiersPayantException() {
        super("Il n'existe pas de tiers-payant sur la vente", "noTiersPayant");
    }
}
