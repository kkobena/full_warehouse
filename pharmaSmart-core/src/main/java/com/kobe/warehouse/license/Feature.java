package com.kobe.warehouse.license;

/**
 * Modules identifiables d'une licence, portés par le champ {@code features} (cf.
 * PLAN-GESTION-LICENCE §3.6).
 *
 * <p><strong>Règle d'octroi</strong> — deux catégories, et une seule est restrictive :
 *
 * <ul>
 *   <li><em>Couvert d'office</em> ({@code optional = false}) : le périmètre fonctionnel existant au
 *       moment de la mise en place de la licence. Toujours accordé, qu'il figure ou non dans
 *       {@code features}. Une officine qui utilise le logiciel aujourd'hui ne doit rien perdre le
 *       jour où on lui délivre un fichier de licence.
 *   <li><em>Optionnel</em> ({@code optional = true}) : les fonctionnalités spécifiques vendues
 *       séparément — à ce jour les exclusions de chiffre d'affaires. Accordé <strong>uniquement</strong>
 *       s'il est explicitement listé.
 * </ul>
 *
 * <p>Ce choix évite le piège de la liste blanche exhaustive : avec celle-ci, émettre une licence
 * mentionnant deux modules aurait silencieusement retiré tous les autres, et il aurait fallu
 * énumérer l'intégralité du catalogue à chaque émission pour n'activer qu'une option. Une licence
 * ancienne, dépourvue du champ {@code features}, conserve par construction tout le périmètre
 * existant et n'ouvre aucune option — ce qui est exactement le comportement attendu.
 */
public enum Feature {
    /**
     * POS / ventes / caisse.
     */
    CAISSE(false, "Caisse / ventes"),
    /**
     * Tiers-payant, assurances, factures clients.
     */
    FACTURATION(false, "Tiers-payant & facturation"),
    /**
     * Comptabilité, journaux, comptes fournisseurs.
     */
    COMPTABILITE(false, "Comptabilité"),
    /**
     * Inventaire tournant, planification.
     */
    INVENTAIRE_AVANCE(false, "Inventaire"),
    /**
     * Rapports comparatifs et évolutifs.
     */
    REPORTS_AVANCES(false, "Rapports avancés"),
    /**
     * APIs des applications mobiles.
     */
    MOBILE(false, "Applications mobiles"),
    /**
     * Certification des factures normalisées.
     */
    FNE(false, "Facture normalisée (FNE)"),
    /**
     * Gestion de plusieurs {@code Storage} / magasins.
     */
    MULTI_DEPOT(false, "Multi-dépôts"),
    /**
     * Exclusion de certaines ventes du chiffre d'affaires à déclarer.
     */
    CALLEBASSE(true, "Ponction du chiffre d'affaires"),
    /**
     * Exclusion des produits d'un rayon du chiffre d'affaires à déclarer.
     */
    EXCLUSION_RAYON(true, "Exclusion de rayons"),
    /**
     * Exclusion des ventes faites à un tiers-payant du chiffre d'affaires à déclarer.
     */
    EXCLUSION_TP(true, "Exclusion de tiers-payants"),
    EXCLUSION_UG(true, "Exclusion des unités gratuites");

    private final boolean optional;
    private final String libelle;

    Feature(boolean optional, String libelle) {
        this.optional = optional;
        this.libelle = libelle;
    }

    /**
     * Intitulé affiché au client. Porté ici et non recopié côté navigateur : deux référentiels
     * finissent toujours par diverger — celui du front omettait les quatre modules optionnels, ce
     * qui faisait croire à l'application qu'ils étaient tous accordés.
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * {@code true} si le module doit être explicitement souscrit ; {@code false} s'il fait partie
     * du périmètre couvert d'office par toute licence.
     */
    public boolean isOptional() {
        return optional;
    }
}
