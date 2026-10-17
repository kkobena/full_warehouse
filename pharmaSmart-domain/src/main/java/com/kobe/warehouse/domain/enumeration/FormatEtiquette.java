package com.kobe.warehouse.domain.enumeration;

import java.util.ArrayList;
import java.util.List;

/**
 * Formats d'étiquettes : planches A4 autocollantes et rouleaux d'imprimante dédiée.
 *
 * <p>Toutes les cotes sont en millimètres. Sur une planche, le <b>pas</b> (étiquette + gouttière)
 * est ce qui positionne les cases : une gouttière fausse décale l'impression et les étiquettes
 * sortent à côté de la découpe. {@code FormatEtiquetteTest} vérifie que chaque grille tient dans
 * 210 × 297.
 *
 * <p>{@link #A4_65_HISTORIQUE} reproduit <b>exactement</b> le calage en place avant le catalogue,
 * mesuré sur le PDF : pas de 40 mm et non de 38, parce que la table du gabarit occupait toute la
 * largeur de page et répartissait ses cinq colonnes sur 200 mm. Les étiquettes déjà imprimées dans
 * l'officine sortent avec ce calage ; le corriger en silence les décalerait. Le vrai Avery L7651
 * est offert à côté, dans {@link #A4_65_L7651}, à qui veut le calage de la planche du commerce.
 *
 * <p>Un {@link SupportEtiquette#ROULEAU} n'a ni grille, ni marges de planche, ni case de départ :
 * ses colonnes et rangées valent 1, ses marges 0, et le gabarit fixe la page à la taille de
 * l'étiquette. Les rouleaux die-cut et les imprimantes Zebra / TSC / Godex sont disponibles
 * localement, là où les planches Avery sont un consommable importé.
 */
public enum FormatEtiquette {
    /**
     * Calage historique de l'application, mesuré sur le PDF : marge de 5 mm, étiquettes de 38 mm
     * espacées de 40. C'est le format par défaut — rien ne bouge pour qui imprimait déjà.
     */
    A4_65_HISTORIQUE("65 étiquettes — 38 × 21,2 mm (calage historique)", "calage maison", 38.0, 21.2, 5, 13, 5.0, 5.0, 2.0, 0.0),

    /** Le vrai Avery L7651 : pas de 38,1 mm sans gouttière, marges de la planche du commerce. */
    A4_65_L7651("65 étiquettes — 38,1 × 21,2 mm", "Avery L7651, Herma 4333", 38.1, 21.2, 5, 13, 9.75, 10.7, 0.0, 0.0),

    /** Avery L7636. Marges déduites de la grille, à confronter à la planche réelle. */
    A4_48_45X21("48 étiquettes — 45,7 × 21,2 mm", "Avery L7636", 45.7, 21.2, 4, 12, 9.85, 21.3, 2.5, 0.0),

    /** Avery L7654. Marges déduites de la grille, à confronter à la planche réelle. */
    A4_40_45X25("40 étiquettes — 45,7 × 25,4 mm", "Avery L7654", 45.7, 25.4, 4, 10, 9.85, 21.5, 2.5, 0.0),

    /** Avery L7159. Assez haute pour porter lot et péremption confortablement. */
    A4_24_63X34("24 étiquettes — 63,5 × 33,9 mm", "Avery L7159", 63.5, 33.9, 3, 8, 7.25, 12.9, 2.5, 0.0),

    /** Avery L7160. Le format d'adresse, courant et facile à trouver. */
    A4_21_63X38("21 étiquettes — 63,5 × 38,1 mm", "Avery L7160", 63.5, 38.1, 3, 7, 7.25, 15.15, 2.5, 0.0),

    /** Avery L7163. Pour une étiquette de préparation ou de posologie. */
    A4_14_99X38("14 étiquettes — 99,1 × 38,1 mm", "Avery L7163", 99.1, 38.1, 2, 7, 5.9, 15.15, 0.0, 0.0),

    /** Le die-cut le plus répandu sur le marché ivoirien : prix et code sur la boîte. */
    ROULEAU_40X30("Rouleau — 40 × 30 mm", "die-cut 40 × 30", 40.0, 30.0),

    /** Die-cut 50 × 30 : la même chose, au large, quand le libellé est long. */
    ROULEAU_50X30("Rouleau — 50 × 30 mm", "die-cut 50 × 30", 50.0, 30.0),

    /** Die-cut 50 × 25. */
    ROULEAU_50X25("Rouleau — 50 × 25 mm", "die-cut 50 × 25", 50.0, 25.0),

    /** Die-cut 40 × 20 : petit article, prix et code seuls. */
    ROULEAU_40X20("Rouleau — 40 × 20 mm", "die-cut 40 × 20", 40.0, 20.0),

    /** Die-cut 30 × 20 : le plus petit qui porte encore un DataMatrix de 11 mm lisible. */
    ROULEAU_30X20("Rouleau — 30 × 20 mm", "die-cut 30 × 20", 30.0, 20.0),

    /** Die-cut 58 × 40 : lot et péremption au large, sur rouleau. */
    ROULEAU_58X40("Rouleau — 58 × 40 mm", "die-cut 58 × 40", 58.0, 40.0),

    /** Dymo LabelWriter, référence 11354 — pilotée par son pilote Windows. */
    ROULEAU_DYMO_57X32("Rouleau Dymo — 57 × 32 mm", "Dymo LabelWriter 11354", 57.0, 32.0),

    /** Brother QL, référence DK-11209, posée dans le sens de la laize. */
    ROULEAU_BROTHER_62X29("Rouleau Brother — 62 × 29 mm", "Brother QL DK-11209", 62.0, 29.0);

    /** Largeur de la page A4, en millimètres. */
    public static final double LARGEUR_A4_MM = 210.0;

    /** Hauteur de la page A4, en millimètres. */
    public static final double HAUTEUR_A4_MM = 297.0;

    /** Retrait interne d'une case : zone de silence à l'intérieur de la découpe. */
    private static final double RETRAIT_MM = 1.0;

    /** Part de la hauteur d'une étiquette de rouleau laissée au Code 39 ; le texte prend le reste. */
    private static final double PART_CODE_BARRES_ROULEAU = 0.4;

    /** Plancher de hauteur du Code 39 : en deçà, les barres ne se lisent plus. */
    private static final double HAUTEUR_CODE_BARRES_MINI_MM = 8.0;

    private final String libelle;
    private final String reference;
    private final SupportEtiquette support;
    private final double largeurMm;
    private final double hauteurMm;
    private final int colonnes;
    private final int rangees;
    private final double margeCoteMm;
    private final double margeHautMm;
    private final double gouttiereHorizontaleMm;
    private final double gouttiereVerticaleMm;

    FormatEtiquette(
        String libelle,
        String reference,
        double largeurMm,
        double hauteurMm,
        int colonnes,
        int rangees,
        double margeCoteMm,
        double margeHautMm,
        double gouttiereHorizontaleMm,
        double gouttiereVerticaleMm
    ) {
        this.libelle = libelle;
        this.reference = reference;
        this.support = SupportEtiquette.PLANCHE_A4;
        this.largeurMm = largeurMm;
        this.hauteurMm = hauteurMm;
        this.colonnes = colonnes;
        this.rangees = rangees;
        this.margeCoteMm = margeCoteMm;
        this.margeHautMm = margeHautMm;
        this.gouttiereHorizontaleMm = gouttiereHorizontaleMm;
        this.gouttiereVerticaleMm = gouttiereVerticaleMm;
    }

    /** Un rouleau : une étiquette par page, à la taille de l'étiquette, sans marge de planche. */
    FormatEtiquette(String libelle, String reference, double largeurMm, double hauteurMm) {
        this.libelle = libelle;
        this.reference = reference;
        this.support = SupportEtiquette.ROULEAU;
        this.largeurMm = largeurMm;
        this.hauteurMm = hauteurMm;
        this.colonnes = 1;
        this.rangees = 1;
        this.margeCoteMm = 0.0;
        this.margeHautMm = 0.0;
        this.gouttiereHorizontaleMm = 0.0;
        this.gouttiereVerticaleMm = 0.0;
    }

    /** Format retenu quand l'appelant n'en précise pas : celui d'avant le catalogue. */
    public static FormatEtiquette parDefaut() {
        return A4_65_HISTORIQUE;
    }

    /** Les planches A4 du catalogue, dans l'ordre de déclaration. */
    public static List<FormatEtiquette> listerPlanches() {
        return listerParSupport(SupportEtiquette.PLANCHE_A4);
    }

    /** Les rouleaux du catalogue, dans l'ordre de déclaration. */
    public static List<FormatEtiquette> listerRouleaux() {
        return listerParSupport(SupportEtiquette.ROULEAU);
    }

    private static List<FormatEtiquette> listerParSupport(SupportEtiquette support) {
        List<FormatEtiquette> formats = new ArrayList<>();
        for (FormatEtiquette format : values()) {
            if (format.support == support) {
                formats.add(format);
            }
        }
        return formats;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getReference() {
        return reference;
    }

    public SupportEtiquette getSupport() {
        return support;
    }

    public boolean isRouleau() {
        return support == SupportEtiquette.ROULEAU;
    }

    public double getLargeurMm() {
        return largeurMm;
    }

    public double getHauteurMm() {
        return hauteurMm;
    }

    public int getColonnes() {
        return colonnes;
    }

    public int getRangees() {
        return rangees;
    }

    public double getMargeCoteMm() {
        return margeCoteMm;
    }

    public double getMargeHautMm() {
        return margeHautMm;
    }

    public double getGouttiereHorizontaleMm() {
        return gouttiereHorizontaleMm;
    }

    public double getGouttiereVerticaleMm() {
        return gouttiereVerticaleMm;
    }

    /** Pas horizontal : largeur de la case porteuse, gouttière comprise. */
    public double getPasHorizontalMm() {
        return largeurMm + gouttiereHorizontaleMm;
    }

    /** Pas vertical : hauteur de la case porteuse, gouttière comprise. */
    public double getPasVerticalMm() {
        return hauteurMm + gouttiereVerticaleMm;
    }

    /** Nombre d'étiquettes d'une planche, donc borne de la case de départ. Toujours 1 en rouleau. */
    public int getCapacite() {
        return colonnes * rangees;
    }

    /**
     * Case de départ ramenée dans les bornes du support.
     *
     * <p>Un rouleau n'a pas de planche entamée à reprendre : une case de départ autre que la
     * première y ferait dérouler des étiquettes blanches, donc du consommable perdu.
     */
    public int normaliserCaseDeDepart(int startAt) {
        if (isRouleau() || startAt < 1) {
            return 1;
        }
        return Math.min(startAt, getCapacite());
    }

    /**
     * Largeur de la grille.
     *
     * <p>Le gabarit en a besoin : sans largeur explicite, la table occupe toute la page et
     * répartit ses colonnes à parts égales, ce qui écrase les cotes déclarées. C'est précisément
     * ce qui faisait imprimer l'ancien gabarit à 40 mm au lieu de 38.
     *
     * <p>La gouttière de la dernière colonne est gardée quand elle tient dans la marge — c'est le
     * cas du calage historique, dont les cinq cases de 40 mm remplissent exactement les 200 mm de
     * page. Sinon la dernière colonne est rognée à la largeur de l'étiquette.
     */
    public double getLargeurGrilleMm() {
        return derniereColonneRognee() ? (colonnes - 1) * getPasHorizontalMm() + largeurMm : colonnes * getPasHorizontalMm();
    }

    /**
     * Largeur de CONTENU d'une case, retrait compris.
     *
     * <p>Flying Saucer traite {@code width} comme la boîte de contenu : le retrait s'y ajoute.
     * Déclarer le pas ferait donc des cases de pas + 2 mm — mesuré à 42 mm pour un pas de 40.
     */
    public double getLargeurCaseContenuMm() {
        return getPasHorizontalMm() - 2 * RETRAIT_MM;
    }

    /** Idem pour la dernière colonne quand elle est rognée de sa gouttière. */
    public double getLargeurDerniereCaseContenuMm() {
        return largeurMm - 2 * RETRAIT_MM;
    }

    /** Largeur du code-barres : la case de contenu, moins une zone de silence d'un millimètre. */
    public double getLargeurCodeBarresMm() {
        return (derniereColonneRognee() ? largeurMm : getPasHorizontalMm()) - 3 * RETRAIT_MM;
    }

    /**
     * Hauteur du Code 39 sur un rouleau.
     *
     * <p>Sur une planche, le code-barres se contente de la hauteur que lui laisse le flux. Une
     * étiquette de rouleau est seule sur sa page : sans hauteur déclarée, le symbole prend toute
     * la place et chasse le prix hors de l'étiquette.
     */
    public double getHauteurCodeBarresMm() {
        return Math.max(HAUTEUR_CODE_BARRES_MINI_MM, hauteurMm * PART_CODE_BARRES_ROULEAU);
    }

    /** La gouttière de la dernière colonne sortirait-elle de la page ? */
    public boolean isDerniereColonneRognee() {
        return derniereColonneRognee();
    }

    private boolean derniereColonneRognee() {
        return margeCoteMm + colonnes * getPasHorizontalMm() > LARGEUR_A4_MM - margeCoteMm + 0.1;
    }
}
