package com.kobe.warehouse.web.rest.errors;

public class NumBonAlreadyUseException extends BadRequestAlertException {
    private static final long serialVersionUID = 1L;

    public NumBonAlreadyUseException(String numBon) {
        super(ErrorConstants.NUM_BON_ALREADY_USE, String.format("Le numéro de bon %s est déjà utilisé par ce client", numBon), "sales", "numBonAlreadyUse");
    }

}
