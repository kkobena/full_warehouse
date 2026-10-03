package com.kobe.warehouse.service.errors;

public class InvalidPhoneNumberException extends BadRequestAlertException {

    public InvalidPhoneNumberException() {
        super(
            "Le numéro de téléphone n'est pas valide pour la Côte d'Ivoire. Pour un numéro étranger, saisissez-le avec son indicatif (ex. +223 …).",
            "invalidPhoneNumber"
        );
    }
}
