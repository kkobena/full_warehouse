package com.kobe.warehouse.service.errors;

public class PrivilegeException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public PrivilegeException() {
        super("Vous n'avez pas les autorisations. Veuillez contacter l'administrateur ", "privilegeException");
    }
}
