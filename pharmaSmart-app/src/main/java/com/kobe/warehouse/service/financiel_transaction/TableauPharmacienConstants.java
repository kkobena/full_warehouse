package com.kobe.warehouse.service.financiel_transaction;

/**
 * Constants for TableauPharmacien service
 */
public final class TableauPharmacienConstants {

    public static final int GROUP_OTHER_ID = -1;
    public static final String GROUP_OTHER_LABEL = "Autres";
    public static final int GROUP_OTHER_ORDER = 1_000_100;
    public static final int MAX_DISPLAYED_GROUPS = 4;

    public static final String GROUPING_DAILY = "daily";
    public static final String GROUPING_MONTHLY = "month";

    // Excel export columns
    public static final String COL_DATE = "Date";
    public static final String COL_COMPTANT = "Comptant";
    public static final String COL_CREDIT = "Crédit";
    public static final String COL_REMISE = "Remise";
    public static final String COL_MONTANT_NET = "Montant Net";
    public static final String COL_NOMBRE_CLIENTS = "Nbre de Clients";
    public static final String COL_AVOIRS = "Avoirs";
    public static final String COL_ACHATS_NETS = "Achats Nets";
    public static final String COL_RATIO_VA = "Ratios V/A";
    public static final String COL_RATIO_AV = "Ratios A/V";

    public static final String EXCEL_SHEET_NAME = "Tableau pharmacien";
    public static final String EXCEL_FILE_NAME = "tableau_pharmacien";

    private TableauPharmacienConstants() {}
}
