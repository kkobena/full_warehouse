package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.ComparaisonAnneesService;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.calcul.CalendrierMensuel;
import com.kobe.warehouse.service.pilotage.calcul.CroissanceAnnuelle;
import com.kobe.warehouse.service.pilotage.calcul.LectureAnnees;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Années civiles comparées mois par mois. Une seule lecture mensuelle couvre toutes les années, plus l'année qui précède la
 * première (sa variation, et les douze mois glissants de janvier) ; l'année en cours s'arrête à aujourd'hui et son total se
 * compare à la même date de l'année précédente.
 */
@Service
@Transactional(readOnly = true)
public class ComparaisonAnneesServiceImpl implements ComparaisonAnneesService {

    private static final int ANNEES_MIN = 2;
    private static final int ANNEES_MAX = 6;

    private final DictionnaireIndicateursService dictionnaireIndicateursService;
    private final LecteurFiltrePilotage lecteurFiltrePilotage;
    private final CroiseurAnnees croiseurAnnees;

    public ComparaisonAnneesServiceImpl(
        DictionnaireIndicateursService dictionnaireIndicateursService,
        LecteurFiltrePilotage lecteurFiltrePilotage,
        CroiseurAnnees croiseurAnnees
    ) {
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
        this.lecteurFiltrePilotage = lecteurFiltrePilotage;
        this.croiseurAnnees = croiseurAnnees;
    }

    @Override
    public ComparaisonAnneesDTO comparerAnnees(RequeteAnneesDTO requete, LocalDate aujourdhui) {
        if (requete.annees() < ANNEES_MIN || requete.annees() > ANNEES_MAX) {
            throw new GenericError("On compare de " + ANNEES_MIN + " à " + ANNEES_MAX + " années");
        }
        IndicateurPilotage indicateur = requete.indicateur();
        if (dictionnaireIndicateursService.filtrerAutorises(List.of(indicateur)).isEmpty()) {
            throw new ForbiddenOperationException("Indicateur non autorisé : " + indicateur.getLibelle(), "pilotageIndicateurInterdit");
        }
        List<FiltreAnalyseDTO> filtres = requete.lireFiltres();
        dictionnaireIndicateursService.verifierAxesAutorises(filtres.stream().map(FiltreAnalyseDTO::axe).toList());

        int derniere = aujourdhui.getYear();
        List<Integer> annees = IntStream.rangeClosed(derniere - requete.annees() + 1, derniere).boxed().toList();
        int anneeDeDepart = annees.getFirst() - 1;
        CalendrierMensuel calendrier = CalendrierMensuel.ranger(
            anneeDeDepart,
            lecteurFiltrePilotage.lire(new PeriodeDTO(LocalDate.of(anneeDeDepart, 1, 1), aujourdhui), Granularite.MOIS, filtres, indicateur)
        );
        MesuresPilotage anneePrecedenteADate = lecteurFiltrePilotage
            .lire(new PeriodeDTO(LocalDate.of(derniere - 1, 1, 1), aujourdhui.minusYears(1)), Granularite.ANNEE, filtres, indicateur)
            .getFirst();
        LocalDate memeJourN1 = aujourdhui.minusYears(1);
        MesuresPilotage moisPrecedentADate = lecteurFiltrePilotage
            .lire(new PeriodeDTO(memeJourN1.withDayOfMonth(1), memeJourN1), Granularite.ANNEE, filtres, indicateur)
            .getFirst();
        LectureAnnees lecture = new LectureAnnees(
            indicateur,
            requete.mode(),
            requete.parJourOuvre() && indicateur.estAdditif(),
            calendrier,
            derniere,
            anneePrecedenteADate,
            aujourdhui.getMonthValue(),
            moisPrecedentADate
        );

        List<Integer> anneesCloses = annees.stream().filter(annee -> annee < derniere).toList();
        boolean additif = indicateur.estAdditif();
        CroissanceAnnuelle croissance = additif ? CroissanceAnnuelle.calculer(anneesCloses, lecture) : CroissanceAnnuelle.AUCUNE;
        boolean voitLaMarge = dictionnaireIndicateursService.lireDroitsAccordes().contains(DroitPilotage.RENTABILITE_REMISES);
        return new ComparaisonAnneesDTO(
            IndicateurPilotageDTO.fromIndicateur(indicateur),
            requete.mode(),
            lecture.parJourOuvre(),
            aujourdhui,
            annees.stream().map(lecture::comparer).toList(),
            additif ? lecture.calculerSaisonnalite(anneesCloses) : List.of(),
            croissance.taux(),
            croissance.depuis(),
            croissance.jusqua(),
            lecture.trouverMoisMaximum(annees),
            annees.stream().map(annee -> lecture.synthetiser(annee, voitLaMarge)).toList(),
            croiseurAnnees.croiser(AxeAnalyse.FAMILLE, indicateur, annees, aujourdhui, requete.aDate(), filtres),
            croiseurAnnees.croiser(AxeAnalyse.NATURE_VENTE, indicateur, annees, aujourdhui, requete.aDate(), filtres)
        );
    }

}
