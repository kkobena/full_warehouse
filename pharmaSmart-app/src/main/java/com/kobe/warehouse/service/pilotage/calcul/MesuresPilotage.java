package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.LignesJourDTO;
import com.kobe.warehouse.service.dto.pilotage.MesuresVentileesDTO;
import com.kobe.warehouse.service.dto.pilotage.VentesJourDTO;

/** Sommes lues dans les agrégats sur un jour ou une tranche : tout indicateur du dictionnaire s'en déduit. */
public record MesuresPilotage(
    long nbVentes,
    long nbVentesAnnulees,
    long caTtc,
    long caHt,
    long remises,
    long partTiersPayant,
    long caLignesTtc,
    long caLignesHt,
    long coutHt,
    long quantiteServie,
    long achatsTtc,
    long encaissements,
    long joursOuvres
) {
    public static final MesuresPilotage AUCUNE = new MesuresPilotage(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    public static MesuresPilotage deVentes(VentesJourDTO v) {
        long jourOuvre = v.nbVentes() > 0 ? 1 : 0;
        return new MesuresPilotage(v.nbVentes(), v.nbVentesAnnulees(), v.caTtc(), v.caHt(), v.remises(), v.partTiersPayant(), 0, 0, 0, 0, 0, 0, jourOuvre);
    }

    public static MesuresPilotage deLignes(LignesJourDTO l) {
        return new MesuresPilotage(0, 0, 0, 0, 0, 0, l.caTtc(), l.caHt(), l.coutHt(), l.quantiteServie(), 0, 0, 0);
    }

    /** Les lignes donnent CA, remises, coût et quantités ; les en-têtes, CA, remises, ventes et part tiers payant. */
    public static MesuresPilotage deVentilation(SourceAnalyse source, MesuresVentileesDTO v) {
        long caLignesTtc = source == SourceAnalyse.LIGNES ? v.caTtc() : 0;
        long caLignesHt = source == SourceAnalyse.LIGNES ? v.caHt() : 0;
        return new MesuresPilotage(v.nbVentes(), 0, v.caTtc(), v.caHt(), v.remise(), v.partTiersPayant(), caLignesTtc, caLignesHt, v.coutHt(), v.quantite(), 0, 0, 0);
    }

    public static MesuresPilotage dAchats(long montant) {
        return new MesuresPilotage(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, montant, 0, 0);
    }

    public static MesuresPilotage dEncaissements(long montant) {
        return new MesuresPilotage(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, montant, 0);
    }

    public MesuresPilotage plus(MesuresPilotage autre) {
        return new MesuresPilotage(
            nbVentes + autre.nbVentes,
            nbVentesAnnulees + autre.nbVentesAnnulees,
            caTtc + autre.caTtc,
            caHt + autre.caHt,
            remises + autre.remises,
            partTiersPayant + autre.partTiersPayant,
            caLignesTtc + autre.caLignesTtc,
            caLignesHt + autre.caLignesHt,
            coutHt + autre.coutHt,
            quantiteServie + autre.quantiteServie,
            achatsTtc + autre.achatsTtc,
            encaissements + autre.encaissements,
            joursOuvres + autre.joursOuvres
        );
    }

    /** Différence de deux cumuls : les sommes d'une fenêtre lues sur des sommes préfixées. */
    public MesuresPilotage moins(MesuresPilotage autre) {
        return new MesuresPilotage(
            nbVentes - autre.nbVentes,
            nbVentesAnnulees - autre.nbVentesAnnulees,
            caTtc - autre.caTtc,
            caHt - autre.caHt,
            remises - autre.remises,
            partTiersPayant - autre.partTiersPayant,
            caLignesTtc - autre.caLignesTtc,
            caLignesHt - autre.caLignesHt,
            coutHt - autre.coutHt,
            quantiteServie - autre.quantiteServie,
            achatsTtc - autre.achatsTtc,
            encaissements - autre.encaissements,
            joursOuvres - autre.joursOuvres
        );
    }

    /** Les mêmes sommes, sur les jours ouvrés de l'officine (un périmètre filtré ne les compte pas lui-même). */
    public MesuresPilotage avecJoursOuvres(long jours) {
        return new MesuresPilotage(
            nbVentes, nbVentesAnnulees, caTtc, caHt, remises, partTiersPayant, caLignesTtc, caLignesHt, coutHt, quantiteServie, achatsTtc, encaissements, jours
        );
    }

    public long margeBrute() {
        return caLignesHt - coutHt;
    }
}
