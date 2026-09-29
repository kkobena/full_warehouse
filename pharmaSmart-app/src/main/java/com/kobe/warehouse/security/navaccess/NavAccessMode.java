package com.kobe.warehouse.security.navaccess;

/** Propriété {@code pharma-smart.security.nav-access.mode}. */
public enum NavAccessMode {
    /** Aucun contrôle. */
    OFF,
    /** Le refus est journalisé, la requête passe. */
    AUDIT,
    /** Le refus est renvoyé en 403. */
    ENFORCE,
}
