package com.kobe.warehouse.service.report;

import java.time.format.DateTimeFormatter;

public final class Constant {

    public static final String IS_LAST_PAGE = "isLastPage";
    public static final String MAGASIN = "magasin";
    public static final String SALE = "sale";
    public static final String SIZE = "size";
    public static final String SALE_ITEMS = "sale_items";
    public static final String COMMANDE = "commande";
    public static final String COMMANDE_ITEMS = "commande_items";
    public static final String COMMANDE_EN_COURS_TEMPLATE_FILE = "commande/commande-en-cours";
    public static final String DELIVERY_TEMPLATE_FILE = "delivery/main";
    public static final String AJUSTEMENT_TEMPLATE_FILE = "ajustement/main";
    public static final String MVT_CAISSE_TEMPLATE_FILE = "mvtcaisse/main";
    public static final String INVENTAIRE_TEMPLATE_FILE = "inventaire/main";
    public static final String FACTURATION_TEMPLATE_FILE = "facturation/main";
    public static final String FACTURATION_GROUPE_TEMPLATE_FILE = "facturation/group/main";
    public static final String CURRENT_DATE = "currentDate";
    public static final String DTO_DATE = "dto_date";
    public static final String ITEM_SIZE = "item_size";
    public static final String FOOTER = "footer";
    public static final String DEVISE = "devise";
    public static final String DEVISE_CONSTANT = "CFA";
    public static final int COMMANDE_PAGE_SIZE = 55;
    public static final int PAGE_SIZE = 73;
    public static final String PAGE_COUNT = "page_count";
    public static final String ITEMS = "items";
    public static final String ENTITY = "entity";
    public static final String IS_START_STORAGE = "isStartStorage";
    public static final String IS_START_RAYON = "isStartRayon";
    public static final String IS_END_STORAGE = "isEndStorage";
    public static final String IS_END_RAYON = "isEndRayon";
    public static final int GROUP_PAGE_SIZE = 40;
    public static final String REPORT_TITLE = "reportTitle";
    public static final String REPORT_SUMMARY = "reportSummary";
    public static final String FACTURE_TOTAL = "grandTotal";
    public static final String FACTURE_TOTAL_LETTERS = "invoiceTotalAmountLetters";
    public static final String TVA_GROUP_DATE = "tvaGroupDate";
    public static final String TVA_TEMPLATE_FILE = "tva/main";
    public static final String BALANCE_TEMPLATE_FILE = "balance/main";
    public static final String TABLEAU_PHARMACIEN_TEMPLATE_FILE = "tableaupharmacien/main";
    public static final String TABLEAU_PHARMACIEN_GROUP_FOURNISSEUR = "groupeFournisseur";
    public static final String TABLEAU_PHARMACIEN_GROUP_MONTH = "tableauGroupMonth";
    public static final String COLSPAN = "colspan";
    public static final String DATE_FORMATTER_PATTERN = "dd/MM/yyyy";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN);
    public static final String SUIVI_ARTICLE_TEMPLATE_FILE = "suiviarticle/main";
    public static final String ETIQUETES_TEMPLATE_FILE = "etiquetes/main";
    public static final String ETIQUETES_BEGIN = "begin";
    public static final String INVOICE_TEMPLATE_FILE = "facture/saleInvoice";
    public static final String REGLEMENT_RECEIPT_TEMPLATE_FILE = "reglement/receipt/main";
    public static final String REGLEMENT_SINGLE_TEMPLATE_FILE = "reglement/pdf/single/main";
    public static final String REGLEMENT_GROUP_TEMPLATE_FILE = "reglement/pdf/group/main";
    public static final String REGLEMENT_COUNT = "totalDossier";
    public static final String REGLEMENT_PAID_AMOUNT = "paidAmount";
    public static final String PERIODE = "periode";
    public static final String ACTIVITY_SUMMARY = "activity/main";
    public static final String HISTORIQUE_VENTE_DAILY_ARTICLE_TEMPLATE_FILE = "historique/vente/daily/main";
    public static final String HISTORIQUE_VENTE_YEARLY_ARTICLE_TEMPLATE_FILE = "historique/vente/yearly/main";
    public static final String HISTORIQUE_ACHAT_DAILY_ARTICLE_TEMPLATE_FILE = "historique/achat/daily/main";
    public static final String HISTORIQUE_ACHAT_YEARLY_ARTICLE_TEMPLATE_FILE = "historique/achat/yearly/main";
    public static final String REGLEMENT_DIFFERE_RECEIPT_TEMPLATE_FILE = "differe/receipt/main";
    public static final String LIST_DIFFERE_PDF_TEMPLATE_FILE = "differe/pdf/list/main";
    public static final String REGLEMENT_DIFFERE_PDF_TEMPLATE_FILE = "differe/pdf/reglement/main";
    public static final String TICKET_Z_TEMPLATE_FILE = "ticketz/main";

    private Constant() {}
}
