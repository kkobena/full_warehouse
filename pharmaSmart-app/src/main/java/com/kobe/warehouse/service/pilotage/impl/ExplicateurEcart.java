package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.AxeAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.AxeExpliqueDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ContributionDTO;
import com.kobe.warehouse.service.dto.pilotage.ExplicationEcartDTO;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.Variations;
import com.kobe.warehouse.service.pilotage.calcul.Ventilation;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * « Expliquer l'écart », méthode simple et déterministe : pour chaque axe encore libre, la part de l'écart que concentrent ses
 * trois principaux éléments allant dans le sens de l'écart. L'axe le plus concentré est le plus explicatif (« 70 % de la baisse
 * des Antalgiques vient de 3 produits »). Un axe à un seul élément n'explique rien et n'est pas rendu.
 */
@Component
@Transactional(readOnly = true)
class ExplicateurEcart {

    private static final int PRINCIPALES = 3;
    private static final double CENT = 100.0;

    private final PeriodeComparaisonService periodeComparaisonService;
    private final DictionnaireIndicateursService dictionnaireIndicateursService;
    private final VentilateurPilotage ventilateurPilotage;

    ExplicateurEcart(
        PeriodeComparaisonService periodeComparaisonService,
        DictionnaireIndicateursService dictionnaireIndicateursService,
        VentilateurPilotage ventilateurPilotage
    ) {
        this.periodeComparaisonService = periodeComparaisonService;
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
        this.ventilateurPilotage = ventilateurPilotage;
    }

    ExplicationEcartDTO expliquer(RequetePilotageDTO requete, List<FiltreAnalyseDTO> filtres) {
        Set<AxeAnalyse> axesFiltres = filtres.stream().map(FiltreAnalyseDTO::axe).collect(Collectors.toSet());
        dictionnaireIndicateursService.verifierAxesAutorises(axesFiltres);
        IndicateurPilotage indicateur = dictionnaireIndicateursService
            .filtrerAutorises(requete.indicateurs())
            .stream()
            .filter(IndicateurPilotage::estAdditif)
            .findFirst()
            .orElse(IndicateurPilotage.CA_TTC);
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        if (comparaison.reference() == null) {
            throw new GenericError("Choisissez une comparaison pour expliquer un écart");
        }
        SourceAnalyse source = choisirSource(axesFiltres, indicateur).orElseThrow(() ->
            new GenericError(indicateur.getLibelle() + " ne se ventile pas sur ce chemin")
        );
        MesuresComparees perimetre = ventilateurPilotage.comparer(source, comparaison, null, null, filtres, Granularite.MOIS).total();
        CelluleAnalyseDTO total = Variations.cellule(indicateur, perimetre, true);

        Set<DroitPilotage> droits = dictionnaireIndicateursService.lireDroitsAccordes();
        List<AxeExpliqueDTO> axes = Arrays.stream(AxeAnalyse.values())
            .filter(axe -> axe.estPropose() && axe.estLuEnBase() && !axesFiltres.contains(axe) && droits.contains(axe.getDroit()))
            .map(axe -> expliquerPar(axe, indicateur, filtres, axesFiltres, comparaison))
            .flatMap(Optional::stream)
            .sorted(Comparator.comparingDouble(AxeExpliqueDTO::part).reversed())
            .toList();
        return new ExplicationEcartDTO(IndicateurPilotageDTO.fromIndicateur(indicateur), total.valeur(), total.valeurReference(), total.ecart(), axes);
    }

    private Optional<AxeExpliqueDTO> expliquerPar(
        AxeAnalyse axe,
        IndicateurPilotage indicateur,
        List<FiltreAnalyseDTO> filtres,
        Set<AxeAnalyse> axesFiltres,
        ComparaisonPeriodesDTO comparaison
    ) {
        Optional<SourceAnalyse> source = choisirSource(Stream.concat(axesFiltres.stream(), Stream.of(axe)).toList(), indicateur);
        if (source.isEmpty()) {
            return Optional.empty();
        }
        Ventilation ventilation = ventilateurPilotage.comparer(source.get(), comparaison, axe, null, filtres, Granularite.MOIS);
        Double ecartTotal = Variations.cellule(indicateur, ventilation.total(), true).ecart();
        if (ventilation.elements().size() < 2 || ecartTotal == null || ecartTotal == 0) {
            return Optional.empty();
        }
        double sens = Math.signum(ecartTotal);
        List<ContributionDTO> principales = ventilation
            .elements()
            .stream()
            .map(element -> element.versContribution(axe, indicateur))
            .filter(contribution -> Math.signum(contribution.ecart()) == sens)
            .sorted(Comparator.comparingLong((ContributionDTO contribution) -> Math.abs(contribution.ecart())).reversed())
            .limit(PRINCIPALES)
            .toList();
        double concentre = principales.stream().mapToLong(ContributionDTO::ecart).sum();
        double part = Math.min(CENT, concentre * CENT / ecartTotal);
        return Optional.of(new AxeExpliqueDTO(AxeAnalyseDTO.fromAxe(axe), part, ventilation.elements().size(), principales));
    }

    private static Optional<SourceAnalyse> choisirSource(Collection<AxeAnalyse> axes, IndicateurPilotage indicateur) {
        return SourceAnalyse.choisir(axes, List.of(indicateur)).filter(source -> source.getIndicateurs().contains(indicateur));
    }
}
