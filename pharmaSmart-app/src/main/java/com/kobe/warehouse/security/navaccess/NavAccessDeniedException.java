package com.kobe.warehouse.security.navaccess;

import com.kobe.warehouse.service.errors.ForbiddenOperationException;

/** Refus d'un endpoint faute de droit {@code nav_item_role} ; traduit en 403. */
public class NavAccessDeniedException extends ForbiddenOperationException {

    public NavAccessDeniedException(String libelle) {
        super("Accès refusé : " + libelle, "accessDenied");
    }
}
