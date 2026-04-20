package com.kobe.warehouse.constant;

public final class EntityConstant {

    public static final String KEY_MULTI_SITE = "KEY_MULTI_SITE";
    public static final String KEY_MENU_TYPE = "KEY_MENU_TYPE";
    public static final String KEY_MENU_COLOR = "KEY_MENU_COLOR";
    public static final String KEY_THEME = "KEY_THEME";
    public static final String AUTRES_FOURNISSEURS = "AUTRES";
    public static final long SANS_EMPLACEMENT = 1;
    public static final int DEFAULT_STORAGE = 1;
    public static final String DEFAULT_MAIN_STORAGE = "DEFAULT_MAIN_STORAGE";
    public static final int DEFAULT_MAGASIN = 1;
    public static final String APP_GESTION_STOCK = "APP_GESTION_STOCK";
    public static final String APP_MONO_STOCK = "APP_MONO_STOCK";
    public static final String APP_MODE_PAYMENTS = "APP_MODE_PAYMENTS";
    public static final String APP_MODE_PAYMENTS_SANS_CH_VIR = "APP_MODE_PAYMENTS_SANS_CH_VIR";
    public static final long RESERVE_STORAGE = 3L;
    public static final String SANS_EMPLACEMENT_LIBELLE = "SANS EMPLACEMENT";
    public static final String POINT_DE_VENTE_CACHE = "POINT_DE_VENTE_CACHE";

    public static final String CAN_SORCE_STOCK = "CAN_SORCE_STOCK";
    public static final String APP_QTY_MAX = "APP_QTY_MAX";
    public static final String TOUT = "TOUT";
    public static final String VNO = "VNO";
    public static final String VO = "VO";
    public static final String VDE = "VDE";
    public static final String ASSURE = "ASSURE";
    public static final String CARNET = "CARNET";
    public static final String STANDARD = "STANDARD";
    public static final String APP_CASH_FUND = "APP_CASH_FUND";
    public static final String RECEIPT_MAXI_ROW = "RECEIPT_MAXI_ROW";

    public static final String ENTREE_STOCK_SEQUENCE_ID = "ENTREE_STOCK";
    public static final int LEFTPAD_SIZE = 4;
    public static final String APP_DAY_STOCK = "APP_DAY_STOCK";
    public static final String APP_LIMIT_NBR_DAY_REAPPRO = "APP_LIMIT_NBR_DAY_REAPPRO";
    public static final String APP_LAST_DAY_REAPPRO = "APP_LAST_DAY_REAPPRO";
    public static final String APP_DENOMINATEUR_REAPPRO = "APP_DENOMINATEUR_REAPPRO";
    public static final String APP_MODEL_REAPPRO = "APP_MODEL_REAPPRO"; // Modèle de calcul du réapprovisionnement (CLASSIQUE ou SEMOIS)
    public static final int APP_DAY_STOCK_DEFAULT_VALUE = 10;
    public static final int APP_LIMIT_NBR_DAY_REAPPRO_DEFAULT_VALUE = 8;
    public static final int APP_DENOMINATEUR_REAPPRO_DEFAULT_VALUE = 84;
    public static final String CASH_CODE = "CASH";
    public static final String APP_RESET_INVOICE_NUMBER = "APP_RESET_INVOICE_NUMBER"; // Reset invoice number at the beginning of each Year
    public static final String SANS_EMPLACEMENT_CODE = "SANS";
    public static final String APP_SUGGESTION_RETENTION = "APP_SUGGESTION_RETENTION"; // nombre de jours de conservation des suggestions
    public static final String APP_POS_PRINTER_ITEM_COUNT_PER_PAGE = "APP_POS_PRINTER_ITEM_COUNT_PER_PAGE";
    public static final String USER_MAGASIN = "USER_MAGASIN";
    public static final String APP_NOMBRE_JOUR_AVANT_PEREMPTION = "APP_NOMBRE_JOUR_AVANT_PEREMPTION"; // nombre de jour avant la date de peremption pour la vente d'un produit,
    public static final String APP_EXPIRY_ALERT_DAYS_BEFORE = "APP_EXPIRY_ALERT_DAYS_BEFORE"; // nombre de jour avant la date de peremption pour l'alerte d'un produit,
    public static final String APP_GESTION_LOT = "APP_GESTION_LOT"; // nombre de jour avant la date de peremption pour l'alerte d'un produit,
    public static final String APP_BUDGET_MENSUEL_COMMANDE = "APP_BUDGET_MENSUEL_COMMANDE"; // Budget mensuel des commandes fournisseurs (0 = illimité)
    public static final String APP_COUVERTURE_MOIS_CLASSIQUE = "APP_COUVERTURE_MOIS_CLASSIQUE"; // Nb mois de couverture cible pour la formule P2 (défaut: 2)
    public static final String APP_COUVERTURE_MOIS_CLASSIQUE_CACHE = "APP_COUVERTURE_MOIS_CLASSIQUE_CACHE";
    public static final String APP_GESTION_LOT_INVENTAIRE = "APP_GESTION_LOT_INVENTAIRE";
    public static final String APP_GESTION_LOT_INVENTAIRE_CACHE = "APP_GESTION_LOT_INVENTAIRE_CACHE";
    public static final String APP_MODE_SAISIE_LOT_INVENTAIRE = "APP_MODE_SAISIE_LOT_INVENTAIRE";
    public static final String APP_MODE_SAISIE_LOT_INVENTAIRE_CACHE = "APP_MODE_SAISIE_LOT_INVENTAIRE_CACHE";

    public static final String EXCLUDE_FREE_UNIT = "EXCLUDE_FREE_UNIT";
    public static final String USER_STORAGE_CACHE = "USER_STORAGE_CACHE";
    public static final String USER_RESERVE_STORAGE_CACHE = "USER_RESERVE_STORAGE_CACHE";
    public static final String USER_MAIN_STORAGE_CACHE = "USER_MAIN_STORAGE_CACHE";
    public static final String CURRENT_USER_CACHE = "CURRENT_USER_CACHE";
    public static final String CURRENT_USER_MAGASIN_CACHE = "CURRENT_USER_MAGASIN_CACHE";
    public static final String APP_NBRE_JOUR_RETENTION_COMMANDE = "APP_RETENTION_COMMANDE"; // Nombre de jour de retention des suggestions
    public static final String APP_CUSTOMER_DISPLAY = "APP_CUSTOMER_DISPLAY"; // Est-ce que le afficheur client est actif
    public static final String APP_POST_CONFIG = "APP_POST_CONFIG";
    public static final String APP_SCANNER_MODE = "APP_SCANNER_MODE";
    public static final String APP_NTH_MOIS_CONSOMMATION = "APP_NTH_MOIS_CONSOMMATION"; // Nombre de mois de consommation pour les suggestions
    public static final String APP_NTH_MOIS_CONSOMMATION_CACHE = "APP_NTH_MOIS_CONSOMMATION_CACHE";
    public static final String APP_CANCEL_SALE_MAX_DAYS = "APP_CANCEL_SALE_MAX_DAYS"; // Délai maximum (en jours) pour annuler une vente clôturée
    public static final String APP_CANCEL_SALE_MAX_DAYS_CACHE = "APP_CANCEL_SALE_MAX_DAYS_CACHE";
    public static final String APP_RECEPTION_MIN_EXPIRY_DAYS = "APP_RECEPTION_MIN_EXPIRY_DAYS"; // Durée minimale (en jours) de validité d'un lot à la réception
    public static final String APP_RECEPTION_MIN_EXPIRY_DAYS_CACHE = "APP_RECEPTION_MIN_EXPIRY_DAYS_CACHE";
    public static final String APP_SEUIL_VARIATION_PRIX = "APP_SEUIL_VARIATION_PRIX"; // Seuil (%) de variation de prix d'achat déclenchant une alerte à la réception
    public static final String APP_SEUIL_VARIATION_PRIX_CACHE = "APP_SEUIL_VARIATION_PRIX_CACHE";
    public static final String APP_PUTAWAY_MODE = "APP_PUTAWAY_MODE"; // Mode de rangement à la réception (AUTO, MANUAL, ALL_RAYON)
    public static final String APP_PUTAWAY_MODE_CACHE = "APP_PUTAWAY_MODE_CACHE";
    public static final String APP_ACCEPTATION_SUBSTITUTION = "APP_ACCEPTATION_SUBSTITUTION"; // Mode d'acceptation des substitutions PharmaML EP (AUTO | MANUEL)
    public static final String APP_ACCEPTATION_SUBSTITUTION_CACHE = "APP_ACCEPTATION_SUBSTITUTION_CACHE";
    public static final String APP_DELAI_REGLEMENT_FACTURE = "APP_DELAI_REGLEMENT_FACTURE";
    public static final String APP_DELAI_RETOUR_FOURNISSEUR = "APP_DELAI_RETOUR_FOURNISSEUR"; // Délai max (jours) entre réception et retour fournisseur avant avertissement
    public static final String APP_DELAI_RETOUR_FOURNISSEUR_CACHE = "APP_DELAI_RETOUR_FOURNISSEUR_CACHE";

    // ─── Navigation dynamique ─────────────────────────────────────────────────
    /** Cache de l'arbre de navigation par utilisateur. Clé : login. */
    public static final String NAV_TREE_CACHE = "navTree";

    // ─── Dashboard layout ─────────────────────────────────────────────────────
    /**
     * Cache du layout résolu pour l'utilisateur courant.
     * Clé : login. TTL : 24h (changement rare — uniquement lors d'une reconfiguration admin).
     * Eviction : save, update, delete, setAsDefault, setAsDefaultForAuthority.
     */
    public static final String DASHBOARD_LAYOUT_RESOLVED_CACHE = "dashboardLayoutResolved";

}
