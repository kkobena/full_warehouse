package com.kobe.warehouse.domain.enumeration;

/**
 * Réglages qu'une entrée du catalogue des exports propose, les mêmes que l'écran d'où les données viennent : le pharmacien
 * obtient depuis le menu Exports ce qu'il aurait filtré là-bas, sans y aller.
 *
 * <p>Les filtres d'un export de données portent un prédicat JPQL, écrit sur les alias que posent les requêtes des exports
 * ({@code RequeteExport}) ; {@code {v}} désigne la racine de la vente, qui diffère d'un export à l'autre. Les réglages d'une
 * édition ({@link RapportExport}) n'en ont pas : ils alimentent la requête du service qui la bâtit. Chaque entrée n'annonce que
 * les réglages que sa requête ou son service sait honorer ({@link SujetExport#getFiltres()}).
 */
public enum FiltreExport {
    FAMILLE("Famille", SourceOptionsExport.FAMILLE, true, "fam.id IN :familles", "familles"),
    LABORATOIRE("Laboratoire", SourceOptionsExport.LABORATOIRE, true, "lab.id IN :laboratoires", "laboratoires"),
    FOURNISSEUR("Fournisseur", SourceOptionsExport.FOURNISSEUR, true, "four.id IN :fournisseurs", "fournisseurs"),
    FORME("Forme", SourceOptionsExport.FORME, true, "forme.id IN :formes", "formes"),
    GAMME("Gamme", SourceOptionsExport.GAMME, true, "gam.id IN :gammes", "gammes"),
    MAGASIN("Magasin", SourceOptionsExport.MAGASIN, true, "mag.id IN :magasins", "magasins"),
    VENDEUR("Vendeur", SourceOptionsExport.UTILISATEUR, true, "vend.id IN :vendeurs", "vendeurs"),
    CAISSIER("Caissier", SourceOptionsExport.UTILISATEUR, true, "caiss.id IN :caissiers", "caissiers"),
    UTILISATEUR("Utilisateur", SourceOptionsExport.UTILISATEUR, true, "util.id IN :utilisateurs", "utilisateurs"),
    MODE_PAIEMENT("Mode de paiement", SourceOptionsExport.MODE_PAIEMENT, true, "pm.code IN :modesPaiement", "modesPaiement"),
    NATURE_VENTE("Type de vente", SourceOptionsExport.NATURE_VENTE, true, "{v}.natureVente IN :naturesVente", "naturesVente"),
    TYPE_PRESCRIPTION(
        "Ordonnance / conseil",
        SourceOptionsExport.TYPE_PRESCRIPTION,
        true,
        "{v}.typePrescription IN :typesPrescription",
        "typesPrescription"
    ),
    TYPE_TRANSACTION(
        "Type de transaction",
        SourceOptionsExport.TYPE_TRANSACTION,
        true,
        "t.typeFinancialTransaction IN :typesTransaction",
        "typesTransaction"
    ),
    TYPE_MOUVEMENT("Type de mouvement", SourceOptionsExport.TYPE_MOUVEMENT, true, "t.mouvementType IN :typesMouvement", "typesMouvement"),
    SEXE("Sexe", SourceOptionsExport.SEXE, true, "c.sexe IN :sexes", "sexes"),
    STATUT_CLIENT("Statut du client", SourceOptionsExport.STATUT_CLIENT, true, "c.status IN :statutsClient", "statutsClient"),

    /** Réglages des éditions : honorés par le service qui bâtit l'édition, pas par une requête d'export. */
    SECTIONS_COMPTABLES("Feuilles", SourceOptionsExport.SECTIONS_COMPTABLES, true),
    GRANULARITE("Découpage", SourceOptionsExport.GRANULARITE, false),
    COMPARAISON("Comparaison", SourceOptionsExport.COMPARAISON, false),
    INDICATEURS("Indicateurs", SourceOptionsExport.INDICATEUR, true),
    INDICATEUR_COMPARE("Indicateur comparé", SourceOptionsExport.INDICATEUR, false),
    AXE_ANALYSE("Ventiler par", SourceOptionsExport.AXE_ANALYSE, false),
    NOMBRE_ELEMENTS("Éléments", SourceOptionsExport.NOMBRE_ELEMENTS, false),
    TRI_ANALYSE("Trier par", SourceOptionsExport.TRI_ANALYSE, false),
    MODE_ANNEES("Lecture du mois", SourceOptionsExport.MODE_ANNEES, false),
    NOMBRE_ANNEES("Années comparées", SourceOptionsExport.NOMBRE_ANNEES, false),
    MODE_CA("Chiffre d'affaires", SourceOptionsExport.MODE_CA, false),
    /** Les services de rapport n'acceptent qu'une famille : le choix est donc unique, là où l'export de données en prend plusieurs. */
    FAMILLE_UNIQUE("Famille", SourceOptionsExport.FAMILLE, false),
    DECOUPAGE_COMPARATIF("Comparer par", SourceOptionsExport.DECOUPAGE_COMPARATIF, false),
    TYPE_FACTURE("Type de facture", SourceOptionsExport.TYPE_FACTURE, false),
    DECOUPAGE_TABLEAU("Découpage", SourceOptionsExport.DECOUPAGE_TABLEAU, false),
    GROUPEMENT_TVA("Regroupement", SourceOptionsExport.GROUPEMENT_TVA, false);

    private final String libelle;
    private final SourceOptionsExport source;
    private final boolean multiple;
    private final String predicat;
    private final String parametre;

    FiltreExport(String libelle, SourceOptionsExport source, boolean multiple) {
        this(libelle, source, multiple, null, null);
    }

    FiltreExport(String libelle, SourceOptionsExport source, boolean multiple, String predicat, String parametre) {
        this.libelle = libelle;
        this.source = source;
        this.multiple = multiple;
        this.predicat = predicat;
        this.parametre = parametre;
    }

    public String getLibelle() {
        return libelle;
    }

    public SourceOptionsExport getSource() {
        return source;
    }

    /** Plusieurs valeurs retenues à la fois ; sinon, l'écran n'en propose qu'une et le service lit la première. */
    public boolean estMultiple() {
        return multiple;
    }

    /** Prédicat JPQL du filtre ; {@code {v}} reste à remplacer par la racine de la vente de l'export. Vide pour une édition. */
    public String getPredicat() {
        return predicat;
    }

    /** Nom du paramètre nommé du prédicat, sans les deux-points. */
    public String getParametre() {
        return parametre;
    }
}
