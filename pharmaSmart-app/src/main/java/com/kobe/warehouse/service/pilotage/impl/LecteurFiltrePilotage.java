package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.calcul.DecoupagePeriode;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mesures d'une période par tranche, sur tout le CA de l'officine ou sur un périmètre filtré (une famille, un type de vente).
 * Filtrées, les mesures gardent les jours ouvrés de l'officine : un jour ouvré l'est pour toute la pharmacie.
 */
@Component
@Transactional(readOnly = true)
class LecteurFiltrePilotage {

    private final LecteurMesuresPilotage lecteurMesuresPilotage;
    private final VentilateurPilotage ventilateurPilotage;

    LecteurFiltrePilotage(LecteurMesuresPilotage lecteurMesuresPilotage, VentilateurPilotage ventilateurPilotage) {
        this.lecteurMesuresPilotage = lecteurMesuresPilotage;
        this.ventilateurPilotage = ventilateurPilotage;
    }

    List<MesuresPilotage> lire(PeriodeDTO periode, Granularite granularite, List<FiltreAnalyseDTO> filtres, IndicateurPilotage indicateur) {
        List<MesuresPilotage> officine = lecteurMesuresPilotage.lireParTranche(periode, DecoupagePeriode.decouper(periode, granularite));
        if (filtres.isEmpty()) {
            return officine;
        }
        List<AxeAnalyse> axes = filtres.stream().map(FiltreAnalyseDTO::axe).toList();
        SourceAnalyse source = SourceAnalyse.choisir(axes, List.of(indicateur))
            .filter(candidate -> candidate.getIndicateurs().contains(indicateur))
            .orElseThrow(() -> new GenericError(indicateur.getLibelle() + " ne se lit pas sur ce périmètre"));
        List<MesuresPilotage> filtrees = new ArrayList<>(officine.stream().map(mesures -> MesuresPilotage.AUCUNE.avecJoursOuvres(mesures.joursOuvres())).toList());
        var ventilation = ventilateurPilotage.comparer(source, new ComparaisonPeriodesDTO(periode, null, false), AxeAnalyse.PERIODE, null, filtres, granularite);
        for (MesuresComparees tranche : ventilation.elements()) {
            int rang = Integer.parseInt(tranche.membre().cle());
            filtrees.set(rang, tranche.periode().avecJoursOuvres(officine.get(rang).joursOuvres()));
        }
        return filtrees;
    }
}
