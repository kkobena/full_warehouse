package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ContributionDTO;
import com.kobe.warehouse.service.dto.pilotage.EcartsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.EcartsPilotageService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.EffetsEcart;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CA = ventes × articles par vente × prix moyen d'un article. L'écart se décompose en trois effets successifs dont la somme vaut
 * l'écart exactement : fréquentation (ventes en plus ou en moins, au panier de référence), articles (à fréquentation nouvelle),
 * prix (au nouveau nombre d'articles).
 */
@Service
@Transactional(readOnly = true)
public class EcartsPilotageServiceImpl implements EcartsPilotageService {

    private static final int CONTRIBUTIONS_MAX = 20;
    private static final List<AxeAnalyse> AXES_EXPLICATIFS = List.of(AxeAnalyse.FAMILLE, AxeAnalyse.PRODUIT, AxeAnalyse.VENDEUR, AxeAnalyse.NATURE_VENTE);

    private final PeriodeComparaisonService periodeComparaisonService;
    private final LecteurMesuresPilotage lecteurMesuresPilotage;
    private final VentilateurPilotage ventilateurPilotage;
    private final DictionnaireIndicateursService dictionnaireIndicateursService;

    public EcartsPilotageServiceImpl(
        PeriodeComparaisonService periodeComparaisonService,
        LecteurMesuresPilotage lecteurMesuresPilotage,
        VentilateurPilotage ventilateurPilotage,
        DictionnaireIndicateursService dictionnaireIndicateursService
    ) {
        this.periodeComparaisonService = periodeComparaisonService;
        this.lecteurMesuresPilotage = lecteurMesuresPilotage;
        this.ventilateurPilotage = ventilateurPilotage;
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
    }

    @Override
    public EcartsPilotageDTO expliquerEcarts(RequetePilotageDTO requete, int nombreContributions) {
        if (nombreContributions < 1 || nombreContributions > CONTRIBUTIONS_MAX) {
            throw new GenericError("Le nombre de contributions va de 1 à " + CONTRIBUTIONS_MAX);
        }
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        MesuresPilotage periode = lecteurMesuresPilotage.lireTotal(comparaison.periode());
        if (comparaison.reference() == null) {
            return new EcartsPilotageDTO(comparaison, periode.caLignesTtc(), 0, null, null, null, List.of(), List.of());
        }
        MesuresPilotage reference = lecteurMesuresPilotage.lireTotal(comparaison.reference());
        EffetsEcart effets = EffetsEcart.calculer(periode, reference);
        Map<Boolean, List<ContributionDTO>> parSens = listerContributions(comparaison)
            .stream()
            .filter(contribution -> contribution.ecart() != 0)
            .collect(Collectors.partitioningBy(contribution -> contribution.ecart() > 0));
        return new EcartsPilotageDTO(
            comparaison,
            periode.caLignesTtc(),
            reference.caLignesTtc(),
            effets.frequentation(),
            effets.articles(),
            effets.prix(),
            premieres(parSens.get(true), Comparator.comparingLong(ContributionDTO::ecart).reversed(), nombreContributions),
            premieres(parSens.get(false), Comparator.comparingLong(ContributionDTO::ecart), nombreContributions)
        );
    }

    /** Un axe sans droit (les vendeurs, sans « Clients & équipe ») n'est pas lu. */
    private List<ContributionDTO> listerContributions(ComparaisonPeriodesDTO comparaison) {
        Set<DroitPilotage> droits = dictionnaireIndicateursService.lireDroitsAccordes();
        return AXES_EXPLICATIFS.stream()
            .filter(axe -> droits.contains(axe.getDroit()))
            .flatMap(axe ->
                ventilateurPilotage
                    .comparer(SourceAnalyse.LIGNES, comparaison, axe, null, List.of(), Granularite.MOIS)
                    .elements()
                    .stream()
                    .map(element -> element.versContribution(axe, IndicateurPilotage.CA_TTC))
            )
            .toList();
    }

    private static List<ContributionDTO> premieres(List<ContributionDTO> contributions, Comparator<ContributionDTO> ordre, int nombre) {
        return contributions.stream().sorted(ordre).limit(nombre).toList();
    }

}
