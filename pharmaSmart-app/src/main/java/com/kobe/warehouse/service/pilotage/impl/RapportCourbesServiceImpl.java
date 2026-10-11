package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.PointSerieDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO.StyleSerie;
import com.kobe.warehouse.service.pilotage.GraphiquePilotageService;
import com.kobe.warehouse.service.pilotage.RapportCourbesService;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.HorizonCourbe;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RapportCourbesServiceImpl implements RapportCourbesService {

    private final SeriesPilotageService seriesPilotageService;
    private final GraphiquePilotageService graphiquePilotageService;

    public RapportCourbesServiceImpl(SeriesPilotageService seriesPilotageService, GraphiquePilotageService graphiquePilotageService) {
        this.seriesPilotageService = seriesPilotageService;
        this.graphiquePilotageService = graphiquePilotageService;
    }

    /** La courbe remonte 13 tranches : la tendance, pas la seule période des tuiles. */
    @Override
    public String dessinerHistorique(RequetePilotageDTO requete, IndicateurPilotage indicateur, boolean contreObjectif, FormatGraphe format) {
        return dessiner(HorizonCourbe.elargirAuxTreizeTranches(requete, indicateur), requete, contreObjectif, format);
    }

    @Override
    public String dessinerPeriode(RequetePilotageDTO requete, IndicateurPilotage indicateur, FormatGraphe format) {
        return dessiner(requete.lireAvecIndicateurs(List.of(indicateur)), requete, false, format);
    }

    /** @param requete la requête de la barre, qui nomme la référence */
    private String dessiner(RequetePilotageDTO tracee, RequetePilotageDTO requete, boolean contreObjectif, FormatGraphe format) {
        List<SerieIndicateurDTO> historique = seriesPilotageService.calculerSeries(tracee).series();
        if (historique.isEmpty() || historique.getFirst().points().isEmpty()) {
            return null;
        }
        SerieIndicateurDTO serie = historique.getFirst();
        int tranches = serie.points().size();
        List<String> libelles = new ArrayList<>(tranches);
        List<Double> realise = new ArrayList<>(tranches);
        List<Double> reference = new ArrayList<>(tranches);
        List<Double> objectif = new ArrayList<>(tranches);
        boolean avecReference = false;
        boolean avecObjectif = false;
        for (PointSerieDTO point : serie.points()) {
            libelles.add(point.libelle());
            realise.add(point.valeur());
            reference.add(point.valeurReference());
            objectif.add(point.objectif());
            avecReference |= point.valeurReference() != null;
            avecObjectif |= point.objectif() != null;
        }
        List<SerieGraphiqueDTO> series = new ArrayList<>(3);
        series.add(new SerieGraphiqueDTO("Réalisé", realise, StyleSerie.REALISE));
        if (avecReference) {
            series.add(new SerieGraphiqueDTO(HorizonCourbe.libellerReference(requete), reference, StyleSerie.REFERENCE));
        }
        if (avecObjectif && !contreObjectif) {
            series.add(new SerieGraphiqueDTO("Objectif", objectif, StyleSerie.OBJECTIF));
        }
        return graphiquePilotageService.dessinerCourbes(libelles, series, serie.indicateur().unite(), format);
    }
}
