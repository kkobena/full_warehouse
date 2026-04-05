package com.kobe.warehouse.service.errors;

import java.io.Serial;

/**
 * Exception levée quand aucun fournisseur n'est associé au produit d'un lot hors commande.
 * L'errorKey "fournisseurIntrouvable" invite l'utilisateur à compléter la fiche produit.
 */
public class FournisseurIntrouvableException extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FournisseurIntrouvableException(Integer lotId) {
        super(
            "Aucun fournisseur associé au produit du lot #" + lotId
            + ". Veuillez compléter la fiche produit avant de créer un retour.",
            "fournisseurIntrouvable"
        );
    }
}

