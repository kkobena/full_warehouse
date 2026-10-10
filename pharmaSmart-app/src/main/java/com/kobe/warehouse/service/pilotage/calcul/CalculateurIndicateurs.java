package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;

/**
 * Valeur d'un indicateur du dictionnaire à partir des mesures. {@code null} : non calculable (dénominateur nul) ou pas encore
 * alimenté par les agrégats (rotation, délais, ruptures, ventes manquées : onglets à venir).
 */
public final class CalculateurIndicateurs {

    private static final double CENT = 100.0;

    private CalculateurIndicateurs() {}

    public static Double calculer(IndicateurPilotage indicateur, MesuresPilotage m) {
        return switch (indicateur) {
            case CA_TTC -> (double) m.caTtc();
            case CA_HT -> (double) m.caHt();
            case CA_NET -> (double) (m.caTtc() - m.remises());
            case NB_VENTES -> (double) m.nbVentes();
            case FREQUENTATION -> ratio(m.nbVentes(), m.joursOuvres());
            case PANIER_MOYEN -> ratio(m.caTtc(), m.nbVentes());
            case ARTICLES_PAR_VENTE -> ratio(m.quantiteServie(), m.nbVentes());
            case PRIX_MOYEN_ARTICLE -> ratio(m.caLignesTtc(), m.quantiteServie());
            case QUANTITES_VENDUES -> (double) m.quantiteServie();
            case MARGE_BRUTE -> (double) m.margeBrute();
            case TAUX_MARGE -> pourcentage(m.margeBrute(), m.caLignesHt());
            case COEFFICIENT_MOYEN -> ratio(m.caLignesHt(), m.coutHt());
            case MARGE_PAR_VENTE -> ratio(m.margeBrute(), m.nbVentes());
            case REMISES -> (double) m.remises();
            case TAUX_REMISE -> pourcentage(m.remises(), m.caTtc());
            case POIDS_REMISES_MARGE -> pourcentage(m.remises(), m.margeBrute() + m.remises());
            case ACHATS_TTC -> (double) m.achatsTtc();
            case RATIO_VENTES_ACHATS -> ratio(m.caTtc(), m.achatsTtc());
            case ENCAISSEMENTS -> (double) m.encaissements();
            case PART_TIERS_PAYANT -> pourcentage(m.partTiersPayant(), m.caTtc());
            case TAUX_ANNULATION -> pourcentage(m.nbVentesAnnulees(), m.nbVentes() + m.nbVentesAnnulees());
            case ROTATION_STOCK, TAUX_RUPTURE_FOURNISSEUR, VENTES_MANQUEES, DELAI_PAIEMENT_TP -> null;
        };
    }

    private static Double ratio(long numerateur, long denominateur) {
        return denominateur == 0 ? null : (double) numerateur / denominateur;
    }

    private static Double pourcentage(long numerateur, long denominateur) {
        return denominateur == 0 ? null : numerateur * CENT / denominateur;
    }
}
