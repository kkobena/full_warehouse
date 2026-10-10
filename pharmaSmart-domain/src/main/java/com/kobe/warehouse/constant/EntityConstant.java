package com.kobe.warehouse.constant;

public final class EntityConstant {

    public static final String AUTRES_FOURNISSEURS = "AUTRES";
    public static final int DEFAULT_STORAGE = 1;
    public static final String DEFAULT_MAIN_STORAGE = "DEFAULT_MAIN_STORAGE";
    public static final int DEFAULT_MAGASIN = 1;
    public static final String APP_GESTION_STOCK = "APP_GESTION_STOCK";
    public static final String APP_MONO_STOCK = "APP_MONO_STOCK";
    public static final String APP_MODE_PAYMENTS = "APP_MODE_PAYMENTS";
    public static final String APP_MODE_PAYMENTS_SANS_CH_VIR = "APP_MODE_PAYMENTS_SANS_CH_VIR";

    public static final String SANS_EMPLACEMENT_LIBELLE = "SANS EMPLACEMENT";

    public static final String TOUT = "TOUT";
    public static final String VNO = "VNO";
    public static final String VO = "VO";
    public static final String ASSURE = "ASSURE";
    public static final String CARNET = "CARNET";
    public static final String STANDARD = "STANDARD";
    public static final String APP_CASH_FUND = "APP_CASH_FUND";

    public static final String APP_DAY_STOCK = "APP_DAY_STOCK";
    public static final String APP_LIMIT_NBR_DAY_REAPPRO = "APP_LIMIT_NBR_DAY_REAPPRO";
    public static final String APP_LAST_DAY_REAPPRO = "APP_LAST_DAY_REAPPRO";
    public static final String APP_DENOMINATEUR_REAPPRO = "APP_DENOMINATEUR_REAPPRO";
    public static final String APP_MODEL_REAPPRO = "APP_MODEL_REAPPRO"; // Modèle de calcul du réapprovisionnement (CLASSIQUE ou SEMOIS)
    public static final int APP_DAY_STOCK_DEFAULT_VALUE = 10;
    public static final int APP_LIMIT_NBR_DAY_REAPPRO_DEFAULT_VALUE = 8;
    public static final int APP_DENOMINATEUR_REAPPRO_DEFAULT_VALUE = 84;
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
    public static final String APP_NTH_MOIS_CONSOMMATION = "APP_NTH_MOIS_CONSOMMATION"; // Nombre de mois de consommation pour les suggestions
    public static final String APP_NTH_MOIS_CONSOMMATION_CACHE = "APP_NTH_MOIS_CONSOMMATION_CACHE";
    /** Contrôle d'ordonnance : par niveau d'alerte, 1 = l'alerte bloque (modale, motif et droit), 0 = simple avertissement. */
    public static final String APP_CONTROLE_ORDONNANCE_BLOQUANT_PREFIXE = "APP_CONTROLE_ORDONNANCE_BLOQUANT_";
    /** Vente d'un produit sur ordonnance : exiger une ordonnance ou un prescripteur à la clôture (1) ou non (0). */
    public static final String APP_VENTE_ORDONNANCE_OBLIGATOIRE = "APP_VENTE_ORDONNANCE_OBLIGATOIRE";
    public static final String APP_CANCEL_SALE_MAX_DAYS = "APP_CANCEL_SALE_MAX_DAYS"; // Délai maximum (en jours) pour annuler une vente clôturée
    public static final String APP_CANCEL_SALE_MAX_DAYS_CACHE = "APP_CANCEL_SALE_MAX_DAYS_CACHE";
    public static final String APP_EXPORT_RETENTION_JOURS = "APP_EXPORT_RETENTION_JOURS"; // Jours de conservation d'un fichier exporté
    public static final String APP_PILOTAGE_SEUIL_STOCK_DORMANT = "APP_PILOTAGE_SEUIL_STOCK_DORMANT"; // Jours sans vente d'un produit dormant
    public static final String APP_PILOTAGE_TRANCHES_REMISE = "APP_PILOTAGE_TRANCHES_REMISE"; // Seuils des tranches de remise du pilotage (%)
    public static final String APP_PILOTAGE_SEUIL_FAIBLE_MARGE = "APP_PILOTAGE_SEUIL_FAIBLE_MARGE"; // Taux de marge (%) d'un produit « à faible marge »
    public static final String APP_PILOTAGE_ALERTE_REMISE_VENDEUR = "APP_PILOTAGE_ALERTE_REMISE_VENDEUR"; // Multiple du taux de remise de l'équipe signalé
    public static final String APP_PILOTAGE_ALERTE_CHUTE_ACTIVITE = "APP_PILOTAGE_ALERTE_CHUTE_ACTIVITE"; // CA des 7 jours sous ce % de N-1
    public static final String APP_PILOTAGE_ALERTE_FAMILLE_RECUL = "APP_PILOTAGE_ALERTE_FAMILLE_RECUL"; // Baisse (%) du CA d'une famille sur le mois
    public static final String APP_PILOTAGE_ALERTE_EROSION_MARGE = "APP_PILOTAGE_ALERTE_EROSION_MARGE"; // Baisse (points) du taux de marge du mois
    public static final String APP_PILOTAGE_ALERTE_OBJECTIF_MENACE = "APP_PILOTAGE_ALERTE_OBJECTIF_MENACE"; // Projection sous ce % de l'objectif
    public static final String APP_PILOTAGE_ALERTE_RATIO_ACHATS = "APP_PILOTAGE_ALERTE_RATIO_ACHATS"; // Ratio ventes / achats sur 30 jours
    public static final String APP_PILOTAGE_ALERTE_DSO_ORGANISME = "APP_PILOTAGE_ALERTE_DSO_ORGANISME"; // Jours de chiffre en encours d'un organisme
    public static final String APP_PONCTION_ANNULATION_MAX_DAYS = "APP_PONCTION_ANNULATION_MAX_DAYS"; // Délai (en jours) pour annuler une ponction validée
    public static final String APP_PONCTION_ANNULATION_MAX_DAYS_CACHE = "APP_PONCTION_ANNULATION_MAX_DAYS_CACHE";
    public static final String APP_PONCTION_PLAFOND_DEFAUT = "APP_PONCTION_PLAFOND_DEFAUT"; // Part maximale d'une vente qu'une ponction peut retirer
    public static final String APP_DEVISE = "APP_DEVISE"; // Devise affichee a la suite des montants
    public static final String APP_PONCTION_PLAFOND_DEFAUT_CACHE = "APP_PONCTION_PLAFOND_DEFAUT_CACHE";
    public static final String APP_DEVISE_CACHE = "APP_DEVISE_CACHE";
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
    public static final String APP_AP_DEFAULT_CREDIT_DAYS = "APP_AP_DEFAULT_CREDIT_DAYS"; // Délai de crédit fournisseur par défaut (jours) pour les comptes fournisseurs AP
    public static final String APP_AP_DEFAULT_CREDIT_DAYS_CACHE = "APP_AP_DEFAULT_CREDIT_DAYS_CACHE";
    public static final String APP_AP_DEFAULT_CRITIQUE_DAYS = "APP_AP_DEFAULT_CRITIQUE_DAYS"; // Délai supplémentaire (jours) après échéance avant statut CRITIQUE
    public static final String APP_AP_DEFAULT_CRITIQUE_DAYS_CACHE = "APP_AP_DEFAULT_CRITIQUE_DAYS_CACHE";
    public static final String APP_NOTIF_AVOIR_EMAIL_ENABLED = "APP_NOTIF_AVOIR_EMAIL_ENABLED"; // Notification email client quand ses produits en avoir sont disponibles (0/1)
    public static final String APP_NOTIF_AVOIR_EMAIL_ENABLED_CACHE = "APP_NOTIF_AVOIR_EMAIL_ENABLED_CACHE";
    public static final String APP_NOTIF_AVOIR_SMS_ENABLED = "APP_NOTIF_AVOIR_SMS_ENABLED"; // Notification SMS client quand ses produits en avoir sont disponibles (0/1)
    public static final String APP_NOTIF_AVOIR_SMS_ENABLED_CACHE = "APP_NOTIF_AVOIR_SMS_ENABLED_CACHE";
    public static final String APP_DELAI_VALIDITE_AVOIR = "APP_DELAI_VALIDITE_AVOIR"; // Délai de validité d'un avoir client (jours, défaut 90)
    public static final String APP_DELAI_VALIDITE_AVOIR_CACHE = "APP_DELAI_VALIDITE_AVOIR_CACHE";
    public static final String APP_DELAI_RETOUR_CLIENT = "APP_DELAI_RETOUR_CLIENT"; // Délai max (jours) entre la vente et le retour client avant avertissement (défaut 30)
    public static final String APP_DELAI_RETOUR_CLIENT_CACHE = "APP_DELAI_RETOUR_CLIENT_CACHE";
    public static final String APP_LIMITE_CREDIT_CLIENT = "APP_LIMITE_CREDIT_CLIENT"; // Encours différé maximal par client (0 = aucune limite)
    public static final String APP_LIMITE_CREDIT_CLIENT_CACHE = "APP_LIMITE_CREDIT_CLIENT_CACHE";
    public static final String APP_RENOUVELLEMENT_EXCEPTIONNEL = "APP_RENOUVELLEMENT_EXCEPTIONNEL"; // Renouvellement exceptionnel d'un traitement chronique après expiration de l'ordonnance (0/1)
    public static final String APP_RENOUVELLEMENT_EXCEPTIONNEL_CACHE = "APP_RENOUVELLEMENT_EXCEPTIONNEL_CACHE";
    public static final String APP_RENOUVELLEMENT_EXCEPTIONNEL_MOIS = "APP_RENOUVELLEMENT_EXCEPTIONNEL_MOIS"; // Durée maximale de ce renouvellement, en mois (défaut 3)
    public static final String APP_RENOUVELLEMENT_EXCEPTIONNEL_MOIS_CACHE = "APP_RENOUVELLEMENT_EXCEPTIONNEL_MOIS_CACHE";
    public static final String APP_RAPPEL_RENOUVELLEMENT_JOURS = "APP_RAPPEL_RENOUVELLEMENT_JOURS"; // Jours avant l'échéance d'un traitement chronique pour signaler le patient (défaut 5)
    public static final String APP_RAPPEL_RENOUVELLEMENT_JOURS_CACHE = "APP_RAPPEL_RENOUVELLEMENT_JOURS_CACHE";

    // ─── Navigation dynamique ─────────────────────────────────────────────────
    /** Cache de l'arbre de navigation par utilisateur. Clé : login. */
    public static final String NAV_TREE_CACHE = "navTree";
    /** Droits nav_item_role fusionnés par ensemble de rôles, pour le contrôle des endpoints. */
    public static final String NAV_ACCESS_CACHE = "navAccess";

    // ─── Dashboard layout ─────────────────────────────────────────────────────
    /**
     * Cache du layout résolu pour l'utilisateur courant.
     * Clé : login. TTL : 24h (changement rare — uniquement lors d'une reconfiguration admin).
     * Eviction : save, update, delete, setAsDefault, setAsDefaultForAuthority.
     */
    public static final String DASHBOARD_LAYOUT_RESOLVED_CACHE = "dashboardLayoutResolved";

}
