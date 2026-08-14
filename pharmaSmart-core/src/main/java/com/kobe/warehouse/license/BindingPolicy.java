package com.kobe.warehouse.license;

/**
 * Politique de liaison de la licence à une installation (cf. PLAN-GESTION-LICENCE §3.4).
 *
 * <p>Elle est portée par la licence elle-même : elle est donc ajustable client par client sans
 * recompilation ni modification de la configuration du poste.
 */
public enum BindingPolicy {
    /** Aucune liaison — installable partout (démos, licences partenaires). */
    NONE,
    /** Liaison au nom de l'officine ({@code Magasin.name}). */
    MAGASIN,
    /** Liaison au nom de l'officine <em>et</em> à l'empreinte matérielle du poste serveur. */
    MAGASIN_AND_HARDWARE;

    public boolean checksMagasin() {
        return this != NONE;
    }

    public boolean checksHardware() {
        return this == MAGASIN_AND_HARDWARE;
    }
}
