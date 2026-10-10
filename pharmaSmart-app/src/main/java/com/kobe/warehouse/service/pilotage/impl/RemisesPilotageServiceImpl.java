package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.OctroiRemise;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.repository.PilotageVentesDetailRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneRemiseDTO;
import com.kobe.warehouse.service.dto.pilotage.RemisesRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.VenteRemiseeDTO;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.RemisesPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.CalculateurIndicateurs;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.RangNaturel;
import com.kobe.warehouse.service.pilotage.calcul.Variations;
import com.kobe.warehouse.service.pilotage.calcul.Ventilation;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remises au comptoir : une seule remise, la remise produit, accordée par le vendeur qui détient le privilège ou autorisée par
 * clé de sécurité. Ventes, CA et remises se lisent sur les en-têtes ; la marge, sur les lignes.
 */
@Service
@Transactional(readOnly = true)
public class RemisesPilotageServiceImpl implements RemisesPilotageService {

    private static final String PRIVILEGE_REMISE = "PR_AJOUTER_REMISE_VENTE";
    private static final int VENTES_MAX = 20;
    private static final double CENT = 100.0;

    private final PeriodeComparaisonService periodeComparaisonService;
    private final DictionnaireIndicateursService dictionnaireIndicateursService;
    private final VentilateurPilotage ventilateurPilotage;
    private final PilotageVentesDetailRepository pilotageVentesDetailRepository;
    private final AppConfigurationService appConfigurationService;

    public RemisesPilotageServiceImpl(
        PeriodeComparaisonService periodeComparaisonService,
        DictionnaireIndicateursService dictionnaireIndicateursService,
        VentilateurPilotage ventilateurPilotage,
        PilotageVentesDetailRepository pilotageVentesDetailRepository,
        AppConfigurationService appConfigurationService
    ) {
        this.periodeComparaisonService = periodeComparaisonService;
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
        this.ventilateurPilotage = ventilateurPilotage;
        this.pilotageVentesDetailRepository = pilotageVentesDetailRepository;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public RemisesRentabiliteDTO analyserRemises(RequetePilotageDTO requete) {
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        boolean avecReference = comparaison.reference() != null;
        boolean voitLEquipe = dictionnaireIndicateursService.lireDroitsAccordes().contains(DroitPilotage.CLIENTS_EQUIPE);

        var octrois = lireEntetes(comparaison, AxeAnalyse.OCTROI_REMISE, requete);
        MesuresComparees total = octrois.total();
        Map<Boolean, List<MesuresComparees>> parRemise = octrois
            .elements()
            .stream()
            .collect(Collectors.partitioningBy(element -> !OctroiRemise.AUCUNE.name().equals(element.membre().cle())));
        MesuresComparees sansRemise = parRemise.get(false).stream().findFirst().orElse(MesuresComparees.AUCUNES);
        MesuresComparees remisees = new MesuresComparees(null, null, total.periode().moins(sansRemise.periode()), total.reference().moins(sansRemise.reference()));
        MesuresComparees marge = ventilateurPilotage.comparer(SourceAnalyse.LIGNES, comparaison, null, null, List.of(), requete.granularite()).total();
        double multiple = appConfigurationService.getAlerteRemiseVendeurPilotage();

        return new RemisesRentabiliteDTO(
            comparaison,
            Variations.cellule(IndicateurPilotage.REMISES, total, avecReference),
            Variations.cellule(IndicateurPilotage.TAUX_REMISE, total, avecReference),
            Variations.cellule(
                UniteIndicateur.POURCENTAGE,
                pourcentage(remisees.periode().nbVentes(), total.periode().nbVentes()),
                avecReference ? pourcentage(remisees.reference().nbVentes(), total.reference().nbVentes()) : null
            ),
            Variations.cellule(
                UniteIndicateur.MONTANT,
                ratio(remisees.periode().remises(), remisees.periode().nbVentes()),
                avecReference ? ratio(remisees.reference().remises(), remisees.reference().nbVentes()) : null
            ),
            Variations.cellule(
                UniteIndicateur.POURCENTAGE,
                pourcentage(total.periode().remises(), marge.periode().margeBrute() + total.periode().remises()),
                avecReference ? pourcentage(total.reference().remises(), marge.reference().margeBrute() + total.reference().remises()) : null
            ),
            parRemise.get(true).stream().map(element -> versLigne(element, null, avecReference, false)).toList(),
            listerTranches(comparaison, requete, avecReference),
            voitLEquipe ? listerVendeurs(comparaison, requete, avecReference, multiple) : null,
            multiple,
            listerPlusFortesRemises(comparaison, voitLEquipe)
        );
    }

    /** Nombre de ventes par tranche (en-têtes), CA, remises et marge par tranche (lignes), dans l'ordre des tranches. */
    private List<LigneRemiseDTO> listerTranches(ComparaisonPeriodesDTO comparaison, RequetePilotageDTO requete, boolean avecReference) {
        Map<String, MesuresComparees> ventesParTranche = lireEntetes(comparaison, AxeAnalyse.REMISE, requete)
            .elements()
            .stream()
            .collect(Collectors.toMap(element -> element.membre().cle(), Function.identity()));
        return ventilateurPilotage
            .comparer(SourceAnalyse.LIGNES, comparaison, AxeAnalyse.REMISE, null, List.of(), requete.granularite())
            .elements()
            .stream()
            .sorted(Comparator.comparingInt(element -> RangNaturel.lire(element.membre().cle())))
            .map(element -> versLigne(element, ventesParTranche.get(element.membre().cle()), avecReference, false))
            .toList();
    }

    /** Taux de remise de chaque vendeur face à celui de l'équipe ; au-delà du multiple fixé, signalé. */
    private List<LigneRemiseDTO> listerVendeurs(ComparaisonPeriodesDTO comparaison, RequetePilotageDTO requete, boolean avecReference, double multiple) {
        var vendeurs = lireEntetes(comparaison, AxeAnalyse.VENDEUR, requete);
        Double tauxEquipe = CalculateurIndicateurs.calculer(IndicateurPilotage.TAUX_REMISE, vendeurs.total().periode());
        return vendeurs
            .elements()
            .stream()
            .sorted(Comparator.comparingLong((MesuresComparees element) -> element.periode().remises()).reversed())
            .map(element -> {
                Double taux = CalculateurIndicateurs.calculer(IndicateurPilotage.TAUX_REMISE, element.periode());
                boolean alerte = taux != null && tauxEquipe != null && tauxEquipe > 0 && taux > multiple * tauxEquipe;
                return versLigne(element, null, avecReference, alerte);
            })
            .toList();
    }

    private List<VenteRemiseeDTO> listerPlusFortesRemises(ComparaisonPeriodesDTO comparaison, boolean voitLEquipe) {
        return pilotageVentesDetailRepository
            .listerPlusFortesRemises(
                comparaison.periode().du(),
                comparaison.periode().au(),
                SalesStatut.CLOSED,
                CategorieChiffreAffaire.officine(),
                PRIVILEGE_REMISE,
                PageRequest.of(0, VENTES_MAX)
            )
            .stream()
            .map(vente -> voitLEquipe ? vente : vente.masquerPersonnes())
            .toList();
    }

    private Ventilation lireEntetes(ComparaisonPeriodesDTO comparaison, AxeAnalyse axe, RequetePilotageDTO requete) {
        return ventilateurPilotage.comparer(SourceAnalyse.ENTETES, comparaison, axe, null, List.of(), requete.granularite());
    }

    /**
     * @param ventes les en-têtes de l'élément quand {@code mesures} vient des lignes (qui ne comptent pas les ventes) ; sinon
     *     {@code null}, {@code mesures} étant lui-même lu sur les en-têtes
     */
    private static LigneRemiseDTO versLigne(MesuresComparees mesures, MesuresComparees ventes, boolean avecReference, boolean alerte) {
        MesuresComparees entetes = ventes == null ? mesures : ventes;
        boolean lignes = ventes != null;
        return new LigneRemiseDTO(
            mesures.membre().cle(),
            mesures.membre().libelle(),
            Variations.cellule(IndicateurPilotage.NB_VENTES, entetes, avecReference),
            Variations.cellule(IndicateurPilotage.CA_TTC, mesures, avecReference),
            Variations.cellule(IndicateurPilotage.REMISES, mesures, avecReference),
            Variations.cellule(IndicateurPilotage.TAUX_REMISE, mesures, avecReference),
            lignes ? Variations.cellule(IndicateurPilotage.TAUX_MARGE, mesures, avecReference) : null,
            alerte
        );
    }

    private static Double pourcentage(long numerateur, long denominateur) {
        return denominateur == 0 ? null : numerateur * CENT / denominateur;
    }

    private static Double ratio(long numerateur, long denominateur) {
        return denominateur == 0 ? null : (double) numerateur / denominateur;
    }
}
