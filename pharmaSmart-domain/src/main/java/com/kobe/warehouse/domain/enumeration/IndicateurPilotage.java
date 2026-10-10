package com.kobe.warehouse.domain.enumeration;

import static com.kobe.warehouse.domain.enumeration.DroitPilotage.ACHATS_STOCK;
import static com.kobe.warehouse.domain.enumeration.DroitPilotage.CLIENTS_EQUIPE;
import static com.kobe.warehouse.domain.enumeration.DroitPilotage.PAGE;
import static com.kobe.warehouse.domain.enumeration.DroitPilotage.RENTABILITE_REMISES;
import static com.kobe.warehouse.domain.enumeration.DroitPilotage.TRESORERIE_TIERS_PAYANT;
import static com.kobe.warehouse.domain.enumeration.SensFavorable.BAISSE;
import static com.kobe.warehouse.domain.enumeration.SensFavorable.HAUSSE;
import static com.kobe.warehouse.domain.enumeration.SensFavorable.NEUTRE;
import static com.kobe.warehouse.domain.enumeration.UniteIndicateur.JOURS;
import static com.kobe.warehouse.domain.enumeration.UniteIndicateur.MONTANT;
import static com.kobe.warehouse.domain.enumeration.UniteIndicateur.NOMBRE;
import static com.kobe.warehouse.domain.enumeration.UniteIndicateur.POURCENTAGE;
import static com.kobe.warehouse.domain.enumeration.UniteIndicateur.RATIO;

/**
 * Dictionnaire des indicateurs du pilotage : une seule définition par indicateur, lue par tous les écrans et exports.
 * Décisions du 2026-10-09 : docs/PLAN-PILOTAGE-OFFICINE.md §8.
 *
 * <p>Le « CA de l'officine » : ventes clôturées et non annulées de catégorie {@code CA}, ventes du dépôt importées
 * ({@code imported}) comprises, ventes au dépôt ({@code CA_DEPOT}) exclues.
 */
public enum IndicateurPilotage {
    CA_TTC("Chiffre d'affaires TTC", "Total TTC du CA de l'officine sur la période.", MONTANT, HAUSSE, PAGE),
    CA_HT("Chiffre d'affaires HT", "Total HT du CA de l'officine sur la période.", MONTANT, HAUSSE, PAGE),
    CA_NET("CA net de remises", "CA TTC moins les remises accordées.", MONTANT, HAUSSE, PAGE),
    NB_VENTES("Nombre de ventes", "Ventes comptées dans le CA de l'officine.", NOMBRE, HAUSSE, PAGE),
    FREQUENTATION("Fréquentation", "Nombre de ventes par jour ouvré.", NOMBRE, HAUSSE, PAGE),
    PANIER_MOYEN("Panier moyen", "CA TTC divisé par le nombre de ventes.", MONTANT, HAUSSE, PAGE),
    ARTICLES_PAR_VENTE("Articles par vente", "Quantités vendues divisées par le nombre de ventes.", NOMBRE, HAUSSE, PAGE),
    PRIX_MOYEN_ARTICLE("Prix moyen d'un article", "CA TTC divisé par les quantités vendues.", MONTANT, NEUTRE, PAGE),
    QUANTITES_VENDUES("Quantités vendues", "Unités servies au comptoir.", NOMBRE, HAUSSE, PAGE),
    MARGE_BRUTE(
        "Marge brute",
        "CA HT moins le coût d'achat HT des quantités demandées.",
        MONTANT,
        HAUSSE,
        RENTABILITE_REMISES
    ),
    TAUX_MARGE("Taux de marge", "Marge brute divisée par le CA HT ; sa variation se lit en points.", POURCENTAGE, HAUSSE, RENTABILITE_REMISES),
    COEFFICIENT_MOYEN(
        "Coefficient moyen",
        "CA HT divisé par le coût d'achat HT des quantités demandées.",
        RATIO,
        HAUSSE,
        RENTABILITE_REMISES
    ),
    MARGE_PAR_VENTE("Marge par vente", "Marge brute divisée par le nombre de ventes.", MONTANT, HAUSSE, RENTABILITE_REMISES),
    REMISES(
        "Remises accordées",
        "Total des remises produit accordées au comptoir, par privilège ou après autorisation.",
        MONTANT,
        BAISSE,
        RENTABILITE_REMISES
    ),
    TAUX_REMISE("Taux de remise", "Remises divisées par le CA TTC.", POURCENTAGE, BAISSE, RENTABILITE_REMISES),
    POIDS_REMISES_MARGE(
        "Poids des remises dans la marge",
        "Remises divisées par la marge qu'on aurait faite sans elles (marge brute + remises).",
        POURCENTAGE,
        BAISSE,
        RENTABILITE_REMISES
    ),
    ACHATS_TTC("Achats TTC", "Total TTC des commandes reçues, datées à la réception.", MONTANT, NEUTRE, ACHATS_STOCK),
    RATIO_VENTES_ACHATS("Ratio ventes / achats", "CA TTC divisé par les achats TTC.", RATIO, NEUTRE, ACHATS_STOCK),
    ROTATION_STOCK("Rotation du stock", "Stock moyen divisé par le coût des ventes, en jours.", JOURS, BAISSE, ACHATS_STOCK),
    TAUX_RUPTURE_FOURNISSEUR(
        "Taux de rupture fournisseurs",
        "Lignes commandées non livrées par le fournisseur divisées par les lignes commandées.",
        POURCENTAGE,
        BAISSE,
        ACHATS_STOCK
    ),
    VENTES_MANQUEES(
        "Ventes manquées",
        "Valeur des quantités demandées au comptoir et non servies (avoirs clients).",
        MONTANT,
        BAISSE,
        ACHATS_STOCK
    ),
    ENCAISSEMENTS(
        "Encaissements",
        "Règlements reçus des clients et des organismes (ventes, différés, factures tiers payant), tous modes ; arrondis de caisse à part.",
        MONTANT,
        HAUSSE,
        TRESORERIE_TIERS_PAYANT
    ),
    PART_TIERS_PAYANT("Part tiers payant", "Part du CA TTC à la charge des organismes.", POURCENTAGE, NEUTRE, TRESORERIE_TIERS_PAYANT),
    DELAI_PAIEMENT_TP(
        "Délai de paiement des organismes",
        "Créances tiers payant divisées par le CA tiers payant, en jours.",
        JOURS,
        BAISSE,
        TRESORERIE_TIERS_PAYANT
    ),
    TAUX_ANNULATION("Taux d'annulation", "Ventes annulées divisées par les ventes.", POURCENTAGE, BAISSE, CLIENTS_EQUIPE);

    private final String libelle;
    private final String definition;
    private final UniteIndicateur unite;
    private final SensFavorable sensFavorable;
    private final DroitPilotage droit;

    IndicateurPilotage(String libelle, String definition, UniteIndicateur unite, SensFavorable sensFavorable, DroitPilotage droit) {
        this.libelle = libelle;
        this.definition = definition;
        this.unite = unite;
        this.sensFavorable = sensFavorable;
        this.droit = droit;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getDefinition() {
        return definition;
    }

    public UniteIndicateur getUnite() {
        return unite;
    }

    public SensFavorable getSensFavorable() {
        return sensFavorable;
    }

    public DroitPilotage getDroit() {
        return droit;
    }

    /** Un indicateur additif se somme d'un élément à l'autre : il a une part du total et une contribution à l'écart. */
    public boolean estAdditif() {
        return switch (this) {
            case CA_TTC, CA_HT, CA_NET, NB_VENTES, QUANTITES_VENDUES, MARGE_BRUTE, REMISES, ACHATS_TTC, ENCAISSEMENTS -> true;
            default -> false;
        };
    }
}
