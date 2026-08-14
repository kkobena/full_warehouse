package com.kobe.warehouse.service.license;

import com.kobe.warehouse.license.LicenseViolationException;

/**
 * Refus d'un fichier de licence déposé depuis l'écran d'activation.
 *
 * <p>Le message porté est destiné à être <strong>lu par l'utilisateur</strong> : il doit indiquer la
 * cause et, si possible, la marche à suivre. Un « licence invalide » sans explication laisserait une
 * officine bloquée sans savoir qui appeler.
 *
 * <p>Hérite de {@link LicenseViolationException} pour être traduite en HTTP 402 par le
 * {@code ExceptionTranslator} déjà en place : un échec d'activation reste un problème de licence, et
 * répondre 401/403 déconnecterait l'administrateur en train de renouveler.
 */
public class LicenseActivationException extends LicenseViolationException {

    public LicenseActivationException(String message, String errorKey) {
        super(message, errorKey);
    }
}
