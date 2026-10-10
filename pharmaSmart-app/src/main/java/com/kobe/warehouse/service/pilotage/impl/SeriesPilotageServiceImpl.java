package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.repository.ObjectifPilotageRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.PointSerieDTO;
import com.kobe.warehouse.service.dto.pilotage.ProjectionObjectifDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.SeriesPilotageDTO;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.CalculateurIndicateurs;
import com.kobe.warehouse.service.pilotage.calcul.ContexteSeries;
import com.kobe.warehouse.service.pilotage.calcul.DecoupagePeriode;
import com.kobe.warehouse.service.pilotage.calcul.LibellesPilotage;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import com.kobe.warehouse.service.pilotage.calcul.ObjectifsPeriode;
import com.kobe.warehouse.service.pilotage.calcul.SuiviObjectifs;
import com.kobe.warehouse.service.pilotage.calcul.TranchesSerie;
import com.kobe.warehouse.service.pilotage.calcul.Variations;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SeriesPilotageServiceImpl implements SeriesPilotageService {

    private final PeriodeComparaisonService periodeComparaisonService;
    private final LecteurMesuresPilotage lecteurMesuresPilotage;
    private final DictionnaireIndicateursService dictionnaireIndicateursService;
    private final ObjectifPilotageRepository objectifPilotageRepository;

    public SeriesPilotageServiceImpl(
        PeriodeComparaisonService periodeComparaisonService,
        LecteurMesuresPilotage lecteurMesuresPilotage,
        DictionnaireIndicateursService dictionnaireIndicateursService,
        ObjectifPilotageRepository objectifPilotageRepository
    ) {
        this.periodeComparaisonService = periodeComparaisonService;
        this.lecteurMesuresPilotage = lecteurMesuresPilotage;
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
        this.objectifPilotageRepository = objectifPilotageRepository;
    }

    @Override
    public SeriesPilotageDTO calculerSeries(RequetePilotageDTO requete) {
        LocalDate aujourdhui = LocalDate.now();
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, aujourdhui);
        TranchesSerie periode = lireTranches(comparaison.periode(), requete);
        TranchesSerie reference = comparaison.reference() == null ? TranchesSerie.AUCUNE : lireTranches(comparaison.reference(), requete);
        ContexteSeries contexte = new ContexteSeries(
            periode,
            reference,
            requete.granularite(),
            lireObjectifs(comparaison.periode()),
            requete.comparaison() == TypeComparaison.OBJECTIF,
            estMoisEnCoursADate(comparaison.periode(), aujourdhui) ? lecteurMesuresPilotage.lireMemeMoisN1(aujourdhui) : List.of(),
            aujourdhui
        );

        List<SerieIndicateurDTO> series = dictionnaireIndicateursService
            .filtrerAutorises(requete.indicateurs())
            .stream()
            .map(indicateur -> construireSerie(indicateur, contexte))
            .toList();
        return new SeriesPilotageDTO(comparaison, periode.total().joursOuvres(), reference.total().joursOuvres(), series);
    }

    private TranchesSerie lireTranches(PeriodeDTO periode, RequetePilotageDTO requete) {
        List<PeriodeDTO> decoupage = DecoupagePeriode.decouper(periode, requete.granularite());
        List<MesuresPilotage> mesures = lecteurMesuresPilotage.lireParTranche(periode, decoupage);
        MesuresPilotage total = mesures.stream().reduce(MesuresPilotage.AUCUNE, MesuresPilotage::plus);
        return new TranchesSerie(decoupage, mesures, total);
    }

    /** Les objectifs ne se montrent qu'avec le droit de l'onglet Objectifs. */
    private ObjectifsPeriode lireObjectifs(PeriodeDTO periode) {
        if (!dictionnaireIndicateursService.lireDroitsAccordes().contains(DroitPilotage.OBJECTIFS)) {
            return ObjectifsPeriode.AUCUN;
        }
        return ObjectifsPeriode.ranger(objectifPilotageRepository.findAllByAnneeBetween(periode.du().getYear(), periode.au().getYear()));
    }

    private static boolean estMoisEnCoursADate(PeriodeDTO periode, LocalDate aujourdhui) {
        return periode.au().equals(aujourdhui) && periode.du().equals(aujourdhui.withDayOfMonth(1));
    }

    /** Contre l'objectif, la référence est l'objectif ; sinon la période de référence. */
    private static SerieIndicateurDTO construireSerie(IndicateurPilotage indicateur, ContexteSeries contexte) {
        TranchesSerie periode = contexte.periode();
        Double valeur = CalculateurIndicateurs.calculer(indicateur, periode.total());
        Double objectif = contexte.objectifs().lireObjectif(indicateur, new PeriodeDTO(periode.decoupage().getFirst().du(), periode.decoupage().getLast().au()));
        Double valeurReference = contexte.contreObjectif()
            ? objectif
            : contexte.reference().estVide() ? null : CalculateurIndicateurs.calculer(indicateur, contexte.reference().total());
        return new SerieIndicateurDTO(
            IndicateurPilotageDTO.fromIndicateur(indicateur),
            valeur,
            valeurReference,
            Variations.ecart(valeur, valeurReference),
            Variations.ecartPct(indicateur, valeur, valeurReference),
            construirePoints(indicateur, contexte),
            objectif,
            projeter(indicateur, contexte)
        );
    }

    private static ProjectionObjectifDTO projeter(IndicateurPilotage indicateur, ContexteSeries contexte) {
        List<MesuresPilotage> moisN1 = contexte.moisN1();
        if (moisN1.isEmpty() || !indicateur.estAdditif()) {
            return null;
        }
        return SuiviObjectifs.projeter(
            indicateur,
            contexte.objectifs().lireObjectifDuMois(indicateur, contexte.aujourdhui()),
            contexte.periode().total(),
            moisN1.getFirst(),
            moisN1.size() > 1 ? moisN1.get(1) : MesuresPilotage.AUCUNE,
            contexte.aujourdhui()
        );
    }

    private static List<PointSerieDTO> construirePoints(IndicateurPilotage indicateur, ContexteSeries contexte) {
        TranchesSerie periode = contexte.periode();
        TranchesSerie reference = contexte.reference();
        List<PointSerieDTO> points = new ArrayList<>(periode.decoupage().size());
        for (int rang = 0; rang < periode.decoupage().size(); rang++) {
            PeriodeDTO tranche = periode.decoupage().get(rang);
            Double valeur = CalculateurIndicateurs.calculer(indicateur, periode.mesures().get(rang));
            Double objectif = contexte.objectifs().lireObjectif(indicateur, tranche);
            Double valeurReference = contexte.contreObjectif()
                ? objectif
                : rang < reference.mesures().size() ? CalculateurIndicateurs.calculer(indicateur, reference.mesures().get(rang)) : null;
            points.add(
                new PointSerieDTO(
                    tranche.du(),
                    tranche.au(),
                    LibellesPilotage.libellerTranche(tranche, contexte.granularite()),
                    valeur,
                    valeurReference,
                    Variations.ecart(valeur, valeurReference),
                    Variations.ecartPct(indicateur, valeur, valeurReference),
                    objectif
                )
            );
        }
        return points;
    }
}
