package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneVendeurDTO;

/** Mesures d'un vendeur : en-têtes, lignes, ventes sur ordonnance, et compteurs [période, référence]. */
public final class MesuresVendeur {
    private static final double CENT = 100.0;

    private final String libelle;
    private final MesuresComparees entetes;
    private MesuresComparees lignes = MesuresComparees.AUCUNES;
    private MesuresComparees ordonnance = MesuresComparees.AUCUNES;
    private final long[] annulations = new long[2];
    private final long[] avoirs = new long[2];

    public MesuresVendeur(String libelle, MesuresComparees entetes) {
        this.libelle = libelle;
        this.entetes = entetes;
    }

    /** Mesures lues sur les lignes de vente (marge, articles). */
    public MesuresVendeur definirLignes(MesuresComparees mesures) {
        this.lignes = mesures;
        return this;
    }

    /** Ventes sur ordonnance, cumulées. */
    public void ajouterOrdonnance(MesuresComparees mesures) {
        this.ordonnance = this.ordonnance.plus(mesures);
    }

    public void ajouter(boolean annulation, int rang, long nombre) {
        (annulation ? annulations : avoirs)[rang] += nombre;
    }

    public LigneVendeurDTO versLigne(String cle, boolean avecReference) {
        // Lignes et en-têtes réunis : les articles par vente rapprochent les quantités (lignes) du nombre de ventes (en-têtes).
        MesuresComparees reunies = new MesuresComparees(null, null, entetes.periode().plus(lignes.periode()), entetes.reference().plus(lignes.reference()));
        return new LigneVendeurDTO(
            cle,
            libelle,
            Variations.cellule(IndicateurPilotage.NB_VENTES, entetes, avecReference),
            Variations.cellule(IndicateurPilotage.CA_TTC, entetes, avecReference),
            Variations.cellule(IndicateurPilotage.PANIER_MOYEN, entetes, avecReference),
            Variations.cellule(IndicateurPilotage.ARTICLES_PAR_VENTE, reunies, avecReference),
            Variations.cellule(IndicateurPilotage.TAUX_MARGE, lignes, avecReference),
            Variations.cellule(IndicateurPilotage.TAUX_REMISE, entetes, avecReference),
            compteur(annulations, avecReference),
            compteur(avoirs, avecReference),
            Variations.cellule(
                UniteIndicateur.POURCENTAGE,
                part(ordonnance.periode().nbVentes(), entetes.periode().nbVentes()),
                avecReference ? part(ordonnance.reference().nbVentes(), entetes.reference().nbVentes()) : null
            )
        );
    }

    private static CelluleAnalyseDTO compteur(long[] compteurs, boolean avecReference) {
        return Variations.cellule(UniteIndicateur.NOMBRE, (double) compteurs[0], avecReference ? (double) compteurs[1] : null);
    }

    private static Double part(long numerateur, long denominateur) {
        return denominateur == 0 ? null : numerateur * CENT / denominateur;
    }
}
