package com.kobe.warehouse.domain.enumeration;

/**
 * Exports existants, rattachés au catalogue : ils restent dans leur écran, le catalogue y conduit. Chacun n'apparaît qu'à qui a
 * le droit d'ouvrir cet écran ({@code droit}).
 *
 * @see ExportDonnees les exports de données brutes, générés par le menu Exports
 */
public enum LienExport {
    COMPTABILITE("Comptabilité", "Balance, rapport TVA, tableau du pharmacien", "Écran Comptabilité : états PDF et Excel.", "/comptabilite", null, "comptabilite"),
    EXPORT_COMPTABLE("Comptabilité", "Export comptable", "Ventes, encaissements et TVA pour le logiciel du comptable.", "/mvt-caisse", null, "mvt-caisse.export-comptable"),
    DECLARATION_TVA("Déclarations", "Déclaration de TVA", "Écran Mouvements de caisse, onglet Déclaration TVA.", "/mvt-caisse", null, "mvt-caisse.declaration-tva"),
    DECLARATION_CA("Déclarations", "Retraitement du CA", "CA déclaré : exclusions, ponctions, tableau et TVA sur CA encaissé.", "/declaration-ca", null, "declaration-ca"),
    PILOTAGE_TABLEAU_DE_BORD("Pilotage", "Tableau de bord", "Le détail par période, tel qu'affiché, en CSV.", "/pilotage", "tableau-de-bord", "pilotage.tableau-de-bord"),
    PILOTAGE_ANALYSER("Pilotage", "Analyser", "Toute ventilation, telle qu'affichée, en CSV.", "/pilotage", "analyser", "pilotage.analyser"),
    PILOTAGE_ANNEES("Pilotage", "Comparer les années", "Mois × années, en CSV.", "/pilotage", "comparer-annees", "pilotage.comparer-annees");

    private final String rubrique;
    private final String libelle;
    private final String description;
    private final String route;
    private final String onglet;
    private final String droit;

    LienExport(String rubrique, String libelle, String description, String route, String onglet, String droit) {
        this.rubrique = rubrique;
        this.libelle = libelle;
        this.description = description;
        this.route = route;
        this.onglet = onglet;
        this.droit = droit;
    }

    public String getRubrique() {
        return rubrique;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getDescription() {
        return description;
    }

    public String getRoute() {
        return route;
    }

    /** Onglet à ouvrir ({@code ?onglet=}), pour une page à onglets. */
    public String getOnglet() {
        return onglet;
    }

    public String getDroit() {
        return droit;
    }
}
