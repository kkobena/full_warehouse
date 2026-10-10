package com.kobe.warehouse.domain.enumeration;

import static com.kobe.warehouse.domain.enumeration.SourceAnalyse.ENTETES;
import static com.kobe.warehouse.domain.enumeration.SourceAnalyse.LIGNES;

import java.util.EnumSet;
import java.util.Set;

/**
 * Axes selon lesquels le pilotage ventile ses indicateurs, avec le droit qu'exige chacun et les agrégats qui le connaissent.
 * Les axes temporels (jour de semaine, période) se déduisent du jour, en Java : ils ne servent pas de filtre.
 */
public enum AxeAnalyse {
    FAMILLE("Famille", "Sans famille", DroitPilotage.PAGE, EnumSet.of(LIGNES)),
    PRODUIT("Produit", "Produit inconnu", DroitPilotage.PAGE, EnumSet.of(LIGNES)),
    LABORATOIRE("Laboratoire", "Sans laboratoire", DroitPilotage.PAGE, EnumSet.of(LIGNES)),
    FOURNISSEUR("Grossiste", "Sans fournisseur", DroitPilotage.PAGE, EnumSet.of(LIGNES)),
    FORME("Forme", "Sans forme", DroitPilotage.PAGE, EnumSet.of(LIGNES)),
    GAMME("Gamme", "Sans gamme", DroitPilotage.PAGE, EnumSet.of(LIGNES)),
    DCI("DCI", "Sans DCI", DroitPilotage.PAGE, EnumSet.of(LIGNES)),
    TVA("Taux de TVA", "Sans TVA", DroitPilotage.PAGE, EnumSet.of(LIGNES)),
    NATURE_VENTE("Type de vente", "Non renseigné", DroitPilotage.PAGE, EnumSet.of(LIGNES, ENTETES)),
    TYPE_PRESCRIPTION("Ordonnance / conseil", "Non renseigné", DroitPilotage.PAGE, EnumSet.of(LIGNES, ENTETES)),
    VENDEUR("Vendeur", "Sans vendeur", DroitPilotage.CLIENTS_EQUIPE, EnumSet.of(LIGNES, ENTETES)),
    /** Tranche de taux de remise de la vente (« 5 à 10 % ») : le seul axe de remise proposé à la ventilation. */
    REMISE("Remise", "Sans remise", DroitPilotage.RENTABILITE_REMISES, EnumSet.of(LIGNES, ENTETES)),
    /** Interne à l'onglet Remises (« Comment elles sont accordées ») : ni proposé à la ventilation, ni à l'explication d'un écart. */
    OCTROI_REMISE("Octroi de la remise", "Sans remise", DroitPilotage.RENTABILITE_REMISES, EnumSet.of(LIGNES, ENTETES)),
    CAISSIER("Caissier", "Sans caissier", DroitPilotage.CLIENTS_EQUIPE, EnumSet.of(ENTETES)),
    HEURE("Heure", "—", DroitPilotage.PAGE, EnumSet.of(ENTETES)),
    JOUR_SEMAINE("Jour de la semaine", "—", DroitPilotage.PAGE, EnumSet.of(LIGNES, ENTETES)),
    PERIODE("Période", "—", DroitPilotage.PAGE, EnumSet.of(LIGNES, ENTETES));

    private final String libelle;
    private final String libelleAbsent;
    private final DroitPilotage droit;
    private final Set<SourceAnalyse> sources;

    AxeAnalyse(String libelle, String libelleAbsent, DroitPilotage droit, Set<SourceAnalyse> sources) {
        this.libelle = libelle;
        this.libelleAbsent = libelleAbsent;
        this.droit = droit;
        this.sources = sources;
    }

    public String getLibelle() {
        return libelle;
    }

    public DroitPilotage getDroit() {
        return droit;
    }

    public Set<SourceAnalyse> getSources() {
        return sources;
    }

    /** Proposé dans « Ventiler par » et à l'explication d'un écart ; un axe interne ne sert qu'à un onglet. */
    public boolean estPropose() {
        return this != OCTROI_REMISE;
    }

    /** Lu en base (et donc filtrable) ; les axes temporels se calculent à partir du jour. */
    public boolean estLuEnBase() {
        return this != JOUR_SEMAINE && this != PERIODE;
    }

    /**
     * Ses éléments ont un ordre propre (heures, jours, tranches) : ni tri par valeur, ni « autres ». Leur clé commence par leur
     * rang (« 5-9 » pour une tranche).
     */
    public boolean estOrdonne() {
        return this == HEURE || this == JOUR_SEMAINE || this == PERIODE || this == REMISE;
    }

    /** Axe proposé à la descente sur un élément ; {@code null} : on descend aux ventes (produit) ou on s'arrête. */
    public AxeAnalyse suivant() {
        return switch (this) {
            case FAMILLE, LABORATOIRE, FOURNISSEUR, FORME, GAMME, DCI, TVA -> PRODUIT;
            case NATURE_VENTE, TYPE_PRESCRIPTION, VENDEUR -> FAMILLE;
            case CAISSIER -> NATURE_VENTE;
            case REMISE, OCTROI_REMISE -> VENDEUR;
            case PRODUIT, HEURE, JOUR_SEMAINE, PERIODE -> null;
        };
    }

    /** Libellé affiché d'un élément, à partir de ce que la base renvoie. */
    public String formaterLibelle(String brut) {
        if (brut == null || brut.isBlank()) {
            return libelleAbsent;
        }
        return switch (this) {
            case TVA -> brut + " %";
            case HEURE -> brut + " h";
            case NATURE_VENTE -> brut.charAt(0) + brut.substring(1).toLowerCase();
            case OCTROI_REMISE -> switch (brut) {
                case "PRIVILEGE" -> "Privilège du vendeur";
                case "AUTORISEE" -> "Autorisée (clé de sécurité)";
                default -> libelleAbsent;
            };
            case TYPE_PRESCRIPTION -> switch (brut) {
                case "PRESCRIPTION" -> "Ordonnance";
                case "CONSEIL" -> "Conseil";
                case "DEPOT" -> "Dépôt";
                default -> brut;
            };
            default -> brut;
        };
    }
}
