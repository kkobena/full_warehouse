package com.kobe.warehouse.license;

/**
 * Nature commerciale de la licence — champ signé, donc infalsifiable (cf. PLAN-GESTION-LICENCE §3.5).
 *
 * <p>La distinction structurante est : <em>les données saisies ont-elles vocation à devenir des
 * données de production ?</em> Non ⇒ {@link #DEMO}, oui ⇒ {@link #TRIAL}.
 */
public enum LicenseType {
    /** Démonstration commerciale : bannière permanente, filigrane PDF, FNE désactivée, quotas. */
    DEMO,
    /** Essai chez un prospect réel : techniquement identique à un abonnement, mais court. */
    TRIAL,
    /** Abonnement payant. */
    SUBSCRIPTION,
    /** Revendeur / support interne. */
    PARTNER;

    /** Une démo ne doit jamais produire de document ni de flux exploitable légalement. */
    public boolean isDemo() {
        return this == DEMO;
    }
}
