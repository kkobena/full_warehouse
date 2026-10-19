package com.kobe.warehouse.domain.enumeration;

import java.util.List;

/**
 * Éditions déjà bâties par le service de leur écran — export comptable, tableaux du pilotage, états comptables, déclaration de
 * TVA — que le menu Exports produit sur place au lieu d'y renvoyer : rien n'oblige à ouvrir l'écran d'origine. Le bouton de cet
 * écran reste ; celui-ci est un chemin de plus.
 *
 * <p>Les états comptables se lisent sur deux chiffres d'affaires, l'encaissé et le déclaré après exclusions et ponctions : c'est
 * le réglage {@link FiltreExport#MODE_CA}, le même paramètre {@code mode} que leurs écrans passent à leur service.
 *
 * <p>Chacune garde le droit de son écran ({@code droits}) : qui n'a pas le droit d'exporter le pilotage ne voit pas ses
 * éditions ici. Et aucune n'est programmable : son service la bâtit avec les droits de qui la demande — une exécution de nuit
 * n'aurait personne derrière elle.
 */
public enum RapportExport implements SujetExport {
    EXPORT_COMPTABLE(
        "Comptabilité",
        "Export comptable",
        "Ventes, achats, caisse, tiers payants, différés et TVA sur la période, pour le logiciel du comptable. En PDF, c'est la déclaration de TVA seule.",
        true,
        List.of(FormatExport.XLSX, FormatExport.CSV, FormatExport.PDF),
        List.of(FiltreExport.SECTIONS_COMPTABLES),
        List.of("mvt-caisse.export-comptable", "comptabilite")
    ),
    PILOTAGE_TABLEAU_DE_BORD(
        "Pilotage",
        "Pilotage — détail par période",
        "Les indicateurs du tableau de bord, une ligne par tranche de la période, avec leur référence de comparaison.",
        true,
        List.of(FormatExport.CSV),
        List.of(FiltreExport.GRANULARITE, FiltreExport.COMPARAISON, FiltreExport.INDICATEURS),
        List.of("pilotage.tableau-de-bord")
    ),
    PILOTAGE_ANALYSER(
        "Pilotage",
        "Pilotage — ventilation",
        "Les indicateurs ventilés selon l'axe choisi, comme à l'écran : les premiers éléments, le reste en « autres », et le total.",
        true,
        List.of(FormatExport.CSV),
        List.of(
            FiltreExport.AXE_ANALYSE,
            FiltreExport.NOMBRE_ELEMENTS,
            FiltreExport.TRI_ANALYSE,
            FiltreExport.COMPARAISON,
            FiltreExport.INDICATEURS
        ),
        List.of("pilotage.analyser")
    ),
    PILOTAGE_ANNEES(
        "Pilotage",
        "Pilotage — comparaison des années",
        "Un indicateur mois par mois sur plusieurs années civiles ; indépendant de la période, comme l'écran.",
        false,
        List.of(FormatExport.CSV),
        List.of(FiltreExport.INDICATEUR_COMPARE, FiltreExport.MODE_ANNEES, FiltreExport.NOMBRE_ANNEES),
        List.of("pilotage.comparer-annees")
    ),
    BALANCE_CAISSE(
        "Comptabilité",
        "Balance des ventes",
        "La balance de la période : ventes, encaissements et modes de paiement. Au choix sur le CA encaissé ou sur le CA déclaré.",
        true,
        List.of(FormatExport.PDF),
        List.of(FiltreExport.MODE_CA),
        List.of("comptabilite.balance", "mvt-caisse.balance", "declaration-ca.balance-reelle")
    ),
    TABLEAU_PHARMACIEN(
        "Comptabilité",
        "Tableau du pharmacien",
        "Le tableau de bord du pharmacien sur la période, par jour ou par mois, sur le CA encaissé ou déclaré.",
        true,
        List.of(FormatExport.PDF, FormatExport.XLSX),
        List.of(FiltreExport.MODE_CA, FiltreExport.DECOUPAGE_TABLEAU),
        List.of("comptabilite.tableau-pharmacien", "mvt-caisse.tableau-pharmacien", "declaration-ca.tableau-pharmacien-reel")
    ),
    RAPPORT_TVA(
        "Comptabilité",
        "Rapport de TVA",
        "La TVA collectée sur la période, par code de TVA ou jour par jour, sur le CA encaissé ou déclaré.",
        true,
        List.of(FormatExport.PDF),
        List.of(FiltreExport.MODE_CA, FiltreExport.GROUPEMENT_TVA),
        List.of("comptabilite.taxe-report", "mvt-caisse.taxe-report", "declaration-ca.taxe-report-reel")
    ),
    DECLARATION_TVA(
        "Déclarations",
        "Déclaration de TVA",
        "L'imprimé de déclaration de la période, tel que l'écran Mouvements de caisse le produit. Il ne se règle pas : la période suffit.",
        true,
        List.of(FormatExport.PDF),
        List.of(),
        List.of("mvt-caisse.declaration-tva", "comptabilite.taxe-report", "mvt-caisse.taxe-report")
    ),
    RECAPITULATIF_CAISSE(
        "Caisse",
        "Récapitulatif de caisse",
        "Le récapitulatif d'une journée de caisse : ouvertures, encaissements par mode de paiement, écarts. Il porte sur un jour — c'est le début de la période qui le désigne.",
        true,
        List.of(FormatExport.PDF),
        List.of(),
        List.of("mvt-caisse.recapitulatif-caisse", "comptabilite.recapitulatif-caisse", "mvt-caisse.gestion-caisse")
    ),
    RAPPORT_ACTIVITE(
        "Comptabilité",
        "Rapport d'activité",
        "L'activité de la période d'un seul tenant : chiffre d'affaires, recettes, mouvements de caisse, achats et tiers payants.",
        true,
        List.of(FormatExport.PDF),
        List.of(),
        List.of("comptabilite.raport-activite", "mvt-caisse.raport-activite")
    ),
    TABLEAU_DE_BORD_CA(
        "Ventes",
        "Tableau de bord du chiffre d'affaires",
        "Le chiffre d'affaires de la période jour par jour, avec ses indicateurs de synthèse.",
        true,
        List.of(FormatExport.PDF, FormatExport.XLSX, FormatExport.CSV),
        List.of(),
        List.of("rapport-ventes.dashboard-ca")
    ),
    MEILLEURES_VENTES(
        "Ventes",
        "Meilleures ventes",
        "Les produits les plus vendus de la période, en quantité et en valeur. En tableur seulement, comme à l'écran.",
        true,
        List.of(FormatExport.XLSX, FormatExport.CSV),
        List.of(),
        List.of("rapport-ventes.dashboard-ca")
    ),
    COMPARATIF_VENTES(
        "Ventes",
        "Comparatif des ventes",
        "Les ventes de l'année en cours comparées à l'année précédente, au mois, au trimestre ou à l'année.",
        false,
        List.of(FormatExport.PDF),
        List.of(FiltreExport.DECOUPAGE_COMPARATIF),
        List.of("rapport-ventes.comparative")
    ),
    RENTABILITE_PRODUITS(
        "Ventes",
        "Rentabilité par produit",
        "Marge et rentabilité des produits, au choix sur une famille. État du jour, comme à l'écran.",
        false,
        List.of(FormatExport.PDF),
        List.of(FiltreExport.FAMILLE_UNIQUE),
        List.of("rapport-ventes.profitability", "rapport-finance.profitability")
    ),
    SEGMENTATION_CLIENTS(
        "Clients",
        "Segmentation des clients",
        "Les clients répartis par comportement d'achat : état du jour.",
        false,
        List.of(FormatExport.PDF),
        List.of(),
        List.of("rapport-partners.customer-segmentation")
    ),
    ALERTES_STOCK(
        "Stock",
        "Alertes de stock",
        "Les produits en rupture et sous leur seuil d'alerte, à l'instant où l'export est demandé.",
        false,
        List.of(FormatExport.PDF, FormatExport.XLSX, FormatExport.CSV),
        List.of(),
        List.of("rapport-stock.stock-alerts")
    ),
    ROTATION_STOCK(
        "Stock",
        "Rotation du stock",
        "La vitesse d'écoulement du stock produit par produit : état du jour.",
        false,
        List.of(FormatExport.PDF),
        List.of(),
        List.of("rapport-stock.stock-rotation")
    ),
    VALORISATION_STOCK(
        "Stock",
        "Valorisation du stock",
        "La valeur du stock détenu, au choix sur une famille. État du jour, comme à l'écran.",
        false,
        List.of(FormatExport.PDF),
        List.of(FiltreExport.FAMILLE_UNIQUE),
        List.of("rapport-stock.stock-valuation")
    ),
    ABC_PARETO(
        "Stock",
        "Analyse ABC / Pareto",
        "Les produits classés A, B, C selon leur poids dans le chiffre d'affaires : état du jour.",
        false,
        List.of(FormatExport.PDF),
        List.of(),
        List.of("rapport-stock.abc-pareto", "rapport-stock.stock-abc")
    ),
    COMPTES_FOURNISSEURS(
        "Fournisseurs",
        "Comptes fournisseurs",
        "Les comptes fournisseurs de la période : factures, règlements et solde par fournisseur.",
        true,
        List.of(FormatExport.PDF),
        List.of(),
        List.of("facturation.comptes-fournisseurs")
    ),
    PERFORMANCE_FOURNISSEURS(
        "Fournisseurs",
        "Performance des fournisseurs",
        "Délais, taux de service et écarts de prix par fournisseur : état du jour.",
        false,
        List.of(FormatExport.PDF),
        List.of(),
        List.of("rapport-partners.supplier-performance")
    ),
    CREANCES_TIERS_PAYANTS(
        "Tiers payants",
        "Créances tiers payants",
        "Ce que les tiers payants doivent à l'officine, par tiers et par ancienneté : état du jour.",
        false,
        List.of(FormatExport.PDF),
        List.of(),
        List.of("rapport-finance.creances", "rapport-finance.situation-creances")
    ),
    RECAPITULATIF_FACTURES(
        "Tiers payants",
        "Récapitulatif des factures",
        "Le récapitulatif mensuel des factures tiers payants. Il porte sur un mois : c'est celui du début de la période qui est retenu.",
        true,
        List.of(FormatExport.PDF, FormatExport.XLSX),
        List.of(FiltreExport.TYPE_FACTURE),
        List.of("facturation.recapitulatif", "factures")
    ),
    RAPPROCHEMENT_TIERS_PAYANTS(
        "Tiers payants",
        "État de rapprochement",
        "Les factures de la période face aux règlements reçus, tous tiers payants et tous statuts confondus.",
        true,
        List.of(FormatExport.PDF, FormatExport.XLSX),
        List.of(),
        List.of("facturation.rapprochement", "factures")
    ),
    DIFFERES_IMPAYES(
        "Différés",
        "Différés impayés",
        "Ce que les clients doivent encore sur leurs ventes à crédit, tous clients confondus. État du jour.",
        false,
        List.of(FormatExport.PDF, FormatExport.XLSX),
        List.of(),
        List.of("differes.differes", "differes", "differes.historique")
    ),
    MOUVEMENTS_CAISSE_ETAT(
        "Caisse",
        "État des mouvements de caisse",
        "Les mouvements de caisse de la période mis en page pour l'impression, au choix par type de transaction ou mode de paiement.",
        true,
        List.of(FormatExport.PDF),
        List.of(FiltreExport.TYPE_TRANSACTION, FiltreExport.MODE_PAIEMENT),
        List.of("mvt-caisse.mvt-caisse", "mvt-caisse", "mvt-caisse.gestion-caisse")
    ),
    PRODUITS_A_DETRUIRE(
        "Stock",
        "Produits à détruire",
        "Les produits périmés ou retirés sur la période, prêts pour le procès-verbal de destruction.",
        true,
        List.of(FormatExport.PDF, FormatExport.XLSX, FormatExport.CSV),
        List.of(),
        List.of("peremptions.lot-a-detruire", "peremptions")
    );

    private final String libelleRubrique;
    private final String libelle;
    private final String description;
    private final boolean periodique;
    private final List<FormatExport> formats;
    private final List<FiltreExport> filtres;
    private final List<String> droits;

    RapportExport(
        String libelleRubrique,
        String libelle,
        String description,
        boolean periodique,
        List<FormatExport> formats,
        List<FiltreExport> filtres,
        List<String> droits
    ) {
        this.libelleRubrique = libelleRubrique;
        this.libelle = libelle;
        this.description = description;
        this.periodique = periodique;
        this.formats = formats;
        this.filtres = filtres;
        this.droits = droits;
    }

    @Override
    public String getLibelleRubrique() {
        return libelleRubrique;
    }

    @Override
    public String getLibelle() {
        return libelle;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isPeriodique() {
        return periodique;
    }

    @Override
    public List<FormatExport> getFormats() {
        return formats;
    }

    @Override
    public List<FiltreExport> getFiltres() {
        return filtres;
    }

    @Override
    public List<String> getDroits() {
        return droits;
    }

    @Override
    public boolean estProgrammable() {
        return false;
    }
}
