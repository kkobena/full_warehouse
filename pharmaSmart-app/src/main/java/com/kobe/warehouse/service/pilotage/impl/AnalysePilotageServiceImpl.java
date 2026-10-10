package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AxeAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.CroiseAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ExplicationEcartDTO;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneCroiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.AnalysePilotageService;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.CalculateurIndicateurs;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.MiseEnFormeAnalyse;
import com.kobe.warehouse.service.pilotage.calcul.RangNaturel;
import com.kobe.warehouse.service.pilotage.calcul.Variations;
import com.kobe.warehouse.service.pilotage.calcul.Ventilation;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalysePilotageServiceImpl implements AnalysePilotageService {

    private static final int INDICATEURS_MAX = 3;
    private static final int COLONNES_MAX = 12;

    private final PeriodeComparaisonService periodeComparaisonService;
    private final DictionnaireIndicateursService dictionnaireIndicateursService;
    private final VentilateurPilotage ventilateurPilotage;
    private final ExplicateurEcart explicateurEcart;

    public AnalysePilotageServiceImpl(
        PeriodeComparaisonService periodeComparaisonService,
        DictionnaireIndicateursService dictionnaireIndicateursService,
        VentilateurPilotage ventilateurPilotage,
        ExplicateurEcart explicateurEcart
    ) {
        this.periodeComparaisonService = periodeComparaisonService;
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
        this.ventilateurPilotage = ventilateurPilotage;
        this.explicateurEcart = explicateurEcart;
    }

    @Override
    public AnalysePilotageDTO analyser(RequetePilotageDTO requete, RequeteAnalyseDTO analyse) {
        List<IndicateurPilotage> demandes = dictionnaireIndicateursService.filtrerAutorises(requete.indicateurs());
        if (demandes.isEmpty() || demandes.size() > INDICATEURS_MAX) {
            throw new GenericError("Choisissez de 1 à " + INDICATEURS_MAX + " indicateurs");
        }
        if (analyse.axe() == analyse.axe2()) {
            throw new GenericError("Les deux ventilations doivent être différentes");
        }
        List<FiltreAnalyseDTO> filtres = analyse.lireFiltres();
        List<AxeAnalyse> axes = Stream.concat(Stream.of(analyse.axe(), analyse.axe2()), filtres.stream().map(FiltreAnalyseDTO::axe))
            .filter(Objects::nonNull)
            .toList();
        dictionnaireIndicateursService.verifierAxesAutorises(axes);
        SourceAnalyse source = SourceAnalyse.choisir(axes, demandes).orElseThrow(() ->
            new GenericError("Ces ventilations ne se croisent pas : " + axes.stream().map(AxeAnalyse::getLibelle).collect(Collectors.joining(", ")))
        );
        Map<Boolean, List<IndicateurPilotage>> parCalculabilite = demandes.stream().collect(Collectors.partitioningBy(source.getIndicateurs()::contains));
        List<IndicateurPilotage> indicateurs = parCalculabilite.get(true);
        if (indicateurs.isEmpty()) {
            throw new GenericError("Aucun des indicateurs choisis ne se ventile par " + analyse.axe().getLibelle());
        }

        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        Ventilation ventilation = ventilateurPilotage.comparer(source, comparaison, analyse.axe(), null, filtres, requete.granularite());
        MiseEnFormeAnalyse mise = MiseEnFormeAnalyse.preparer(indicateurs, ventilation.total(), comparaison.reference() != null);

        List<MesuresComparees> classes = ventilation.elements().stream().sorted(ordonner(analyse, indicateurs.getFirst())).toList();
        int top = analyse.axe().estOrdonne() || analyse.top() <= 0 ? classes.size() : Math.min(analyse.top(), classes.size());
        List<MesuresComparees> retenus = classes.subList(0, top);
        List<MesuresComparees> reste = classes.subList(top, classes.size());

        return new AnalysePilotageDTO(
            comparaison,
            source,
            indicateurs.stream().map(IndicateurPilotageDTO::fromIndicateur).toList(),
            parCalculabilite.get(false).stream().map(IndicateurPilotageDTO::fromIndicateur).toList(),
            AxeAnalyseDTO.fromAxe(analyse.axe()),
            analyse.axe2() == null ? null : AxeAnalyseDTO.fromAxe(analyse.axe2()),
            mise.total(),
            retenus.stream().map(element -> mise.element(element.membre(), element)).toList(),
            reste.isEmpty() ? null : mise.element(new MembreAnalyseDTO(null, "Autres (" + reste.size() + ")"), sommer(reste)),
            classes.size(),
            analyse.axe2() == null ? null : croiser(source, comparaison, analyse, filtres, requete, retenus, !reste.isEmpty(), mise)
        );
    }

    @Override
    public ExplicationEcartDTO expliquerEcart(RequetePilotageDTO requete, RequeteAnalyseDTO analyse) {
        return explicateurEcart.expliquer(requete, analyse.lireFiltres());
    }

    /**
     * Les éléments retenus en lignes, les plus forts éléments du second axe en colonnes, sur le premier indicateur. Si le top a
     * écarté des éléments, la lecture se restreint aux retenus.
     */
    private CroiseAnalyseDTO croiser(
        SourceAnalyse source,
        ComparaisonPeriodesDTO comparaison,
        RequeteAnalyseDTO analyse,
        List<FiltreAnalyseDTO> filtres,
        RequetePilotageDTO requete,
        List<MesuresComparees> retenus,
        boolean topApplique,
        MiseEnFormeAnalyse mise
    ) {
        List<FiltreAnalyseDTO> filtresCroises = new ArrayList<>(filtres);
        if (topApplique && analyse.axe().estLuEnBase()) {
            filtresCroises.add(new FiltreAnalyseDTO(analyse.axe(), retenus.stream().map(element -> element.membre().cle()).toList()));
        }
        Map<List<String>, MesuresComparees> parCellule = new LinkedHashMap<>();
        Map<String, MesuresComparees> parColonne = new LinkedHashMap<>();
        var cellules = ventilateurPilotage.comparer(source, comparaison, analyse.axe(), analyse.axe2(), filtresCroises, requete.granularite());
        for (MesuresComparees cellule : cellules.elements()) {
            parCellule.put(List.of(cellule.membre().cle(), cellule.membre2().cle()), cellule);
            parColonne.merge(cellule.membre2().cle(), new MesuresComparees(cellule.membre2(), null, cellule.periode(), cellule.reference()), MesuresComparees::plus);
        }
        RequeteAnalyseDTO parColonnes = new RequeteAnalyseDTO(analyse.axe2(), null, COLONNES_MAX, TriAnalyse.VALEUR, List.of());
        List<MembreAnalyseDTO> colonnes = parColonne
            .values()
            .stream()
            .sorted(ordonner(parColonnes, mise.principal()))
            .limit(analyse.axe2().estOrdonne() ? Long.MAX_VALUE : COLONNES_MAX)
            .map(MesuresComparees::membre)
            .toList();
        List<LigneCroiseeDTO> lignes = retenus
            .stream()
            .map(ligne ->
                new LigneCroiseeDTO(
                    ligne.membre().cle(),
                    ligne.membre().libelle(),
                    colonnes
                        .stream()
                        .map(colonne -> parCellule.getOrDefault(List.of(ligne.membre().cle(), colonne.cle()), MesuresComparees.AUCUNES))
                        .map(cellule -> Variations.cellule(mise.principal(), cellule, mise.avecReference()))
                        .toList()
                )
            )
            .toList();
        return new CroiseAnalyseDTO(colonnes, lignes);
    }

    /** Un axe ordonné (heures, jours, tranches) garde son ordre ; sinon, tri demandé sur le premier indicateur, les vides à la fin. */
    private static Comparator<MesuresComparees> ordonner(RequeteAnalyseDTO analyse, IndicateurPilotage principal) {
        if (analyse.axe().estOrdonne()) {
            return Comparator.comparingInt(element -> RangNaturel.lire(element.membre().cle()));
        }
        Function<MesuresComparees, Double> valeur = element -> CalculateurIndicateurs.calculer(principal, element.periode());
        Function<MesuresComparees, Double> ecart = element ->
            Variations.ecart(valeur.apply(element), CalculateurIndicateurs.calculer(principal, element.reference()));
        return switch (analyse.tri()) {
            case VALEUR -> Comparator.comparing(valeur, Comparator.nullsLast(Comparator.reverseOrder()));
            case ECART_HAUSSE -> Comparator.comparing(ecart, Comparator.nullsLast(Comparator.reverseOrder()));
            case ECART_BAISSE -> Comparator.comparing(ecart, Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }

    private static MesuresComparees sommer(List<MesuresComparees> elements) {
        return elements.stream().reduce(MesuresComparees.AUCUNES, MesuresComparees::plus);
    }

}
