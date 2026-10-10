package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.CroiseAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneCroiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.pilotage.calcul.CalculateurIndicateurs;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.Variations;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Éléments d'un axe (familles, types de vente) × années, sur l'indicateur ; chaque cellule comparée à l'année précédente. Avec
 * « à date », chaque année s'arrête au même jour que l'année en cours : on compare ce qui est comparable.
 */
@Component
@Transactional(readOnly = true)
class CroiseurAnnees {

    private static final int LIGNES_MAX = 20;

    private final VentilateurPilotage ventilateurPilotage;

    CroiseurAnnees(VentilateurPilotage ventilateurPilotage) {
        this.ventilateurPilotage = ventilateurPilotage;
    }

    /** {@code null} si l'indicateur ne se ventile pas par cet axe (le nombre de ventes par famille…). */
    CroiseAnalyseDTO croiser(
        AxeAnalyse axe,
        IndicateurPilotage indicateur,
        List<Integer> annees,
        LocalDate aujourdhui,
        boolean aDate,
        List<FiltreAnalyseDTO> filtres
    ) {
        List<AxeAnalyse> axes = Stream.concat(Stream.of(axe), filtres.stream().map(FiltreAnalyseDTO::axe)).toList();
        var source = SourceAnalyse.choisir(axes, List.of(indicateur)).filter(candidate -> candidate.getIndicateurs().contains(indicateur));
        if (source.isEmpty()) {
            return null;
        }
        Map<String, MembreAnalyseDTO> membres = new LinkedHashMap<>();
        Map<String, Map<Integer, Double>> valeurs = new HashMap<>();
        for (int annee : annees) {
            PeriodeDTO periode = new PeriodeDTO(LocalDate.of(annee, 1, 1), finDAnnee(annee, aujourdhui, aDate));
            var ventilation = ventilateurPilotage.comparer(source.get(), new ComparaisonPeriodesDTO(periode, null, aDate), axe, null, filtres, Granularite.ANNEE);
            for (MesuresComparees element : ventilation.elements()) {
                membres.putIfAbsent(element.membre().cle(), element.membre());
                valeurs.computeIfAbsent(element.membre().cle(), cle -> new HashMap<>()).put(annee, CalculateurIndicateurs.calculer(indicateur, element.periode()));
            }
        }
        int derniere = annees.getLast();
        List<LigneCroiseeDTO> lignes = membres
            .values()
            .stream()
            .sorted(Comparator.comparing((MembreAnalyseDTO membre) -> valeurs.get(membre.cle()).get(derniere), Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(LIGNES_MAX)
            .map(membre -> new LigneCroiseeDTO(membre.cle(), membre.libelle(), comparerAnnees(indicateur, annees, valeurs.get(membre.cle()))))
            .toList();
        List<MembreAnalyseDTO> colonnes = annees.stream().map(annee -> new MembreAnalyseDTO(String.valueOf(annee), String.valueOf(annee))).toList();
        return new CroiseAnalyseDTO(colonnes, lignes);
    }

    private static List<CelluleAnalyseDTO> comparerAnnees(IndicateurPilotage indicateur, List<Integer> annees, Map<Integer, Double> parAnnee) {
        return IntStream.range(0, annees.size())
            .mapToObj(rang -> Variations.cellule(indicateur, parAnnee.get(annees.get(rang)), rang == 0 ? null : parAnnee.get(annees.get(rang - 1))))
            .toList();
    }

    private static LocalDate finDAnnee(int annee, LocalDate aujourdhui, boolean aDate) {
        LocalDate fin = aDate ? aujourdhui.withYear(annee) : LocalDate.of(annee, 12, 31);
        return fin.isAfter(aujourdhui) ? aujourdhui : fin;
    }
}
