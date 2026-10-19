package com.kobe.warehouse.domain.enumeration;

import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import com.kobe.warehouse.service.dto.exports.OptionFiltreDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.ModeChiffreAffaire;
import java.util.ArrayList;
import java.util.List;

/**
 * D'où viennent les valeurs qu'un filtre d'export propose : d'un référentiel de l'officine (familles, fournisseurs…), lu en base
 * au chargement du catalogue, ou d'une liste fermée portée ici (type de vente, type de mouvement…).
 *
 * <p>Les valeurs circulent toujours en texte, de l'écran jusqu'à l'historique ; {@code convertir} les ramène au type attendu par
 * la requête de l'export.
 */
public enum SourceOptionsExport {
    FAMILLE(TypeValeurFiltre.ENTIER),
    LABORATOIRE(TypeValeurFiltre.ENTIER),
    FOURNISSEUR(TypeValeurFiltre.ENTIER),
    FORME(TypeValeurFiltre.ENTIER),
    GAMME(TypeValeurFiltre.ENTIER),
    MAGASIN(TypeValeurFiltre.ENTIER),
    UTILISATEUR(TypeValeurFiltre.ENTIER),
    MODE_PAIEMENT(TypeValeurFiltre.TEXTE),
    NATURE_VENTE(
        NatureVente.class,
        List.of(
            option(NatureVente.COMPTANT, "Comptant"),
            option(NatureVente.ASSURANCE, "Assurance"),
            option(NatureVente.CARNET, "Carnet")
        )
    ),
    TYPE_PRESCRIPTION(
        TypePrescription.class,
        List.of(
            option(TypePrescription.PRESCRIPTION, "Ordonnance"),
            option(TypePrescription.CONSEIL, "Conseil"),
            option(TypePrescription.DEPOT, "Dépôt")
        )
    ),
    TYPE_TRANSACTION(TypeFinancialTransaction.class, optionsTransaction()),
    TYPE_MOUVEMENT(MouvementProduit.class, optionsMouvement()),
    SEXE(TypeValeurFiltre.TEXTE, List.of(new OptionFiltreDTO("M", "Masculin"), new OptionFiltreDTO("F", "Féminin"))),
    STATUT_CLIENT(
        Status.class,
        List.of(option(Status.ENABLE, "Actif"), option(Status.DISABLE, "Inactif"), option(Status.CLOSED, "Clôturé"))
    ),
    SECTIONS_COMPTABLES(SectionExportComptable.class, optionsSectionsComptables()),
    GRANULARITE(
        Granularite.class,
        List.of(
            option(Granularite.JOUR, "Jour"),
            option(Granularite.SEMAINE, "Semaine"),
            option(Granularite.MOIS, "Mois"),
            option(Granularite.TRIMESTRE, "Trimestre"),
            option(Granularite.ANNEE, "Année")
        )
    ),
    /** Seules les comparaisons qui se décrivent sans réglage supplémentaire : les autres demandent une année ou des dates. */
    COMPARAISON(
        TypeComparaison.class,
        List.of(
            option(TypeComparaison.MEME_PERIODE_N_1, "Même période l'an passé"),
            option(TypeComparaison.PERIODE_PRECEDENTE, "Période précédente"),
            option(TypeComparaison.OBJECTIF, "Objectifs du mois"),
            option(TypeComparaison.AUCUNE, "Sans comparaison")
        )
    ),
    INDICATEUR(IndicateurPilotage.class, optionsIndicateurs()),
    AXE_ANALYSE(AxeAnalyse.class, optionsAxes()),
    MODE_ANNEES(
        ModeAnnees.class,
        List.of(
            option(ModeAnnees.MENSUEL, "Le mois seul"),
            option(ModeAnnees.CUMULE, "Cumul depuis janvier"),
            option(ModeAnnees.GLISSANT, "Douze mois glissants")
        )
    ),
    NOMBRE_ANNEES(
        TypeValeurFiltre.ENTIER,
        List.of(
            new OptionFiltreDTO("2", "2 ans"),
            new OptionFiltreDTO("3", "3 ans"),
            new OptionFiltreDTO("4", "4 ans"),
            new OptionFiltreDTO("5", "5 ans"),
            new OptionFiltreDTO("6", "6 ans")
        )
    ),
    /** Les mêmes choix que le sélecteur « Éléments » de l'onglet Analyser ; « Tout » vaut 0. */
    NOMBRE_ELEMENTS(
        TypeValeurFiltre.ENTIER,
        List.of(
            new OptionFiltreDTO("10", "Top 10"),
            new OptionFiltreDTO("20", "Top 20"),
            new OptionFiltreDTO("50", "Top 50"),
            new OptionFiltreDTO("0", "Tout")
        )
    ),
    TRI_ANALYSE(
        TriAnalyse.class,
        List.of(
            option(TriAnalyse.VALEUR, "Valeur"),
            option(TriAnalyse.ECART_HAUSSE, "Plus fortes hausses"),
            option(TriAnalyse.ECART_BAISSE, "Plus fortes baisses")
        )
    ),
    /** Les deux chiffres d'affaires des états comptables : l'encaissé, ou le déclaré après exclusions et ponctions. */
    MODE_CA(
        ModeChiffreAffaire.class,
        List.of(option(ModeChiffreAffaire.REEL, "CA réel (encaissé)"), option(ModeChiffreAffaire.DECLARE, "CA déclaré (retraité)"))
    ),
    /** Le découpage du tableau du pharmacien, écrit comme l'attend {@code MvtParam.groupeBy}. */
    DECOUPAGE_TABLEAU(
        TypeValeurFiltre.TEXTE,
        List.of(new OptionFiltreDTO("daily", "Journalier"), new OptionFiltreDTO("month", "Mensuel"))
    ),
    /** Le regroupement du rapport de TVA, écrit comme l'attend {@code MvtParam.groupeBy}. */
    GROUPEMENT_TVA(TypeValeurFiltre.TEXTE, List.of(new OptionFiltreDTO("codeTva", "Par période"), new OptionFiltreDTO("daily", "Journalier"))),
    TYPE_FACTURE(
        TypeFacture.class,
        List.of(option(TypeFacture.INDIVIDUAL, "Factures individuelles"), option(TypeFacture.GROUPED, "Factures groupées"))
    ),
    /** Le découpage du comparatif des ventes, écrit comme l'attend {@code ComparativePdfReportService}. */
    DECOUPAGE_COMPARATIF(
        TypeValeurFiltre.TEXTE,
        List.of(
            new OptionFiltreDTO("MONTHLY", "Mois par mois"),
            new OptionFiltreDTO("QUARTERLY", "Par trimestre"),
            new OptionFiltreDTO("YEARLY", "Année par année")
        )
    );

    private final TypeValeurFiltre type;
    private final Class<? extends Enum<?>> classe;
    private final List<OptionFiltreDTO> options;

    SourceOptionsExport(TypeValeurFiltre type) {
        this(type, null, List.of());
    }

    SourceOptionsExport(TypeValeurFiltre type, List<OptionFiltreDTO> options) {
        this(type, null, options);
    }

    SourceOptionsExport(Class<? extends Enum<?>> classe, List<OptionFiltreDTO> options) {
        this(TypeValeurFiltre.ENUM, classe, options);
    }

    SourceOptionsExport(TypeValeurFiltre type, Class<? extends Enum<?>> classe, List<OptionFiltreDTO> options) {
        this.type = type;
        this.classe = classe;
        this.options = options;
    }

    /** Vide : les options se lisent en base, l'écran les reçoit avec le catalogue. */
    public List<OptionFiltreDTO> getOptions() {
        return options;
    }

    /** Les options viennent d'un référentiel de l'officine, à lire en base. */
    public boolean estReferentiel() {
        return options.isEmpty();
    }

    /** La valeur reçue de l'écran, au type qu'attend la requête. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object convertir(String valeur) {
        return switch (type) {
            case ENTIER -> Integer.valueOf(valeur);
            case TEXTE -> valeur;
            case ENUM -> Enum.valueOf((Class) classe, valeur);
        };
    }

    private static OptionFiltreDTO option(Enum<?> constante, String libelle) {
        return new OptionFiltreDTO(constante.name(), libelle);
    }

    private static List<OptionFiltreDTO> optionsSectionsComptables() {
        List<OptionFiltreDTO> options = new ArrayList<>();
        for (SectionExportComptable section : SectionExportComptable.values()) {
            options.add(option(section, section.getLibelle()));
        }
        return List.copyOf(options);
    }

    /** Tous les indicateurs du pilotage ; le service n'en gardera que ceux que les droits de l'utilisateur ouvrent. */
    private static List<OptionFiltreDTO> optionsIndicateurs() {
        List<OptionFiltreDTO> options = new ArrayList<>();
        for (IndicateurPilotage indicateur : IndicateurPilotage.values()) {
            options.add(option(indicateur, indicateur.getLibelle()));
        }
        return List.copyOf(options);
    }

    private static List<OptionFiltreDTO> optionsAxes() {
        List<OptionFiltreDTO> options = new ArrayList<>();
        for (AxeAnalyse axe : AxeAnalyse.values()) {
            if (axe.estPropose()) {
                options.add(option(axe, axe.getLibelle()));
            }
        }
        return List.copyOf(options);
    }

    private static List<OptionFiltreDTO> optionsTransaction() {
        return List.of(
            option(TypeFinancialTransaction.CASH_SALE, "Vente comptant"),
            option(TypeFinancialTransaction.CREDIT_SALE, "Vente à crédit"),
            option(TypeFinancialTransaction.VENTES_DEPOTS, "Vente dépôt"),
            option(TypeFinancialTransaction.REGLEMENT_DIFFERE, "Règlement différé"),
            option(TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT, "Règlement tiers payant"),
            option(TypeFinancialTransaction.ENTREE_CAISSE, "Entrée de caisse"),
            option(TypeFinancialTransaction.SORTIE_CAISSE, "Sortie de caisse"),
            option(TypeFinancialTransaction.FONDS_CAISSE, "Fonds de caisse"),
            option(TypeFinancialTransaction.REGLMENT_FOURNISSEUR, "Règlement fournisseur"),
            option(TypeFinancialTransaction.CAUTION, "Caution")
        );
    }

    private static List<OptionFiltreDTO> optionsMouvement() {
        return List.of(
            option(MouvementProduit.SALE, MouvementProduit.SALE.getValue()),
            option(MouvementProduit.CANCEL_SALE, MouvementProduit.CANCEL_SALE.getValue()),
            option(MouvementProduit.COMMANDE, MouvementProduit.COMMANDE.getValue()),
            option(MouvementProduit.ENTREE_STOCK, MouvementProduit.ENTREE_STOCK.getValue()),
            option(MouvementProduit.AJUSTEMENT_IN, MouvementProduit.AJUSTEMENT_IN.getValue()),
            option(MouvementProduit.AJUSTEMENT_OUT, MouvementProduit.AJUSTEMENT_OUT.getValue()),
            option(MouvementProduit.INVENTAIRE, MouvementProduit.INVENTAIRE.getValue()),
            option(MouvementProduit.MOUVEMENT_STOCK_IN, MouvementProduit.MOUVEMENT_STOCK_IN.getValue()),
            option(MouvementProduit.MOUVEMENT_STOCK_OUT, MouvementProduit.MOUVEMENT_STOCK_OUT.getValue()),
            option(MouvementProduit.DECONDTION_IN, MouvementProduit.DECONDTION_IN.getValue()),
            option(MouvementProduit.DECONDTION_OUT, MouvementProduit.DECONDTION_OUT.getValue()),
            option(MouvementProduit.RETRAIT_PERIME, MouvementProduit.RETRAIT_PERIME.getValue()),
            option(MouvementProduit.RETOUR_CLIENT, MouvementProduit.RETOUR_CLIENT.getValue()),
            option(MouvementProduit.RETOUR_FOURNISSEUR, MouvementProduit.RETOUR_FOURNISSEUR.getValue()),
            option(MouvementProduit.RETOUR_DEPOT, MouvementProduit.RETOUR_DEPOT.getValue()),
            option(MouvementProduit.DESTRUCTION, MouvementProduit.DESTRUCTION.getValue())
        );
    }
}
