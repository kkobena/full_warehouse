package com.kobe.warehouse.web.rest.errors;

public class DefaultFournisseurException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public DefaultFournisseurException() {
        super("Il existe déjà un fournisseur principal. Désactiver pour continuer ", "principal");
    }
}
