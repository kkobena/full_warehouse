package com.kobe.warehouse.service.errors;

import java.io.Serial;

/**
 * Exception levée quand un lot périmé n'a pas d'OrderLine associée,
 * empêchant la résolution automatique de la Commande source.
 * L'errorKey "commandeNotFound" permet au frontend de proposer la sélection manuelle.
 */
public class RetourBonCommandeNotFoundException extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RetourBonCommandeNotFoundException() {
        super(
            "Commande source introuvable pour ce lot. Ce lot n'est pas rattaché à une commande de réception.",
            "commandeNotFound"
        );
    }

    public RetourBonCommandeNotFoundException(Integer lotId) {
        super(
            "Commande source introuvable pour le lot #" + lotId + ". Ce lot n'est pas rattaché à une commande de réception.",
            "commandeNotFound"
        );
    }
}

