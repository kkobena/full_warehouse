package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.repository.PilotageAchatsStockRepository;
import com.kobe.warehouse.repository.PilotageRupturesRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.MoisStockDTO;
import com.kobe.warehouse.service.dto.pilotage.PeremptionMoisDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.PointStockDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.RupturesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.StockPilotageDTO;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.StockPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.IndicateursStock;
import com.kobe.warehouse.service.pilotage.calcul.RupturesPeriode;
import com.kobe.warehouse.service.pilotage.calcul.StockParFamille;
import com.kobe.warehouse.service.pilotage.calcul.Variations;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stock lu sur les photographies de fin de mois (au prix d'achat TTC du dernier mouvement), rapporté au coût TTC des ventes des
 * douze mois qui se terminent ce mois-là. RupturesPeriode fournisseurs et ventes manquées au comptoir restent deux mesures distinctes.
 */
@Service
@Transactional(readOnly = true)
public class StockPilotageServiceImpl implements StockPilotageService {

    private static final int MOIS_COURBE = 12;
    private static final int LISTE_MAX = 20;
    private static final int MOIS_PEREMPTIONS = 6;
    private static final int MOIS_A_PERIMER = 3;
    private static final double CENT = 100.0;

    private final PeriodeComparaisonService periodeComparaisonService;
    private final PilotageAchatsStockRepository pilotageAchatsStockRepository;
    private final PilotageRupturesRepository pilotageRupturesRepository;
    private final AppConfigurationService appConfigurationService;

    public StockPilotageServiceImpl(
        PeriodeComparaisonService periodeComparaisonService,
        PilotageAchatsStockRepository pilotageAchatsStockRepository,
        PilotageRupturesRepository pilotageRupturesRepository,
        AppConfigurationService appConfigurationService
    ) {
        this.periodeComparaisonService = periodeComparaisonService;
        this.pilotageAchatsStockRepository = pilotageAchatsStockRepository;
        this.pilotageRupturesRepository = pilotageRupturesRepository;
        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public StockPilotageDTO analyserStock(RequetePilotageDTO requete) {
        LocalDate aujourdhui = LocalDate.now();
        PeriodeDTO periode = periodeComparaisonService.comparer(requete, aujourdhui).periode();
        LocalDate mois = finDeMois(periode.au());
        LocalDate moisN1 = finDeMois(mois.minusYears(1));
        Map<LocalDate, Long> parMois = new HashMap<>();
        for (MoisStockDTO photo : pilotageAchatsStockRepository.sommerStockParMois(finDeMois(mois.minusMonths(MOIS_COURBE * 2L - 1)), mois)) {
            parMois.put(photo.mois(), photo.valeur());
        }
        long valeur = parMois.getOrDefault(mois, 0L);
        LocalDate debutDouzeMois = mois.minusYears(1).plusDays(1);
        var officine = CategorieChiffreAffaire.officine();
        long coutDouzeMois = pilotageAchatsStockRepository.sommerCoutVentesTtc(debutDouzeMois, mois, officine);

        int seuil = appConfigurationService.getSeuilStockDormantPilotage();
        LocalDate depuis = (mois.isAfter(aujourdhui) ? aujourdhui : mois).minusDays(seuil);
        StockParFamille familles = StockParFamille.lire(
            pilotageAchatsStockRepository.sommerStockParFamille(mois),
            pilotageAchatsStockRepository.sommerCoutVentesParFamille(debutDouzeMois, mois, officine),
            pilotageAchatsStockRepository.sommerDormantsParFamille(mois, depuis)
        );
        return new StockPilotageDTO(
            mois,
            Variations.cellule(UniteIndicateur.MONTANT, parMois.containsKey(mois) ? (double) valeur : null, parMois.containsKey(moisN1) ? (double) parMois.get(moisN1) : null),
            IndicateursStock.rotation(coutDouzeMois, valeur),
            IndicateursStock.couverture(valeur, coutDouzeMois),
            familles.nbDormants(),
            familles.valeurDormante(),
            seuil,
            tracerCourbe(mois, parMois),
            familles.lignes(valeur),
            pilotageAchatsStockRepository.listerDormants(mois, depuis, PageRequest.of(0, LISTE_MAX))
        );
    }

    @Override
    public RupturesPilotageDTO analyserRuptures(RequetePilotageDTO requete) {
        LocalDate aujourdhui = LocalDate.now();
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, aujourdhui);
        RupturesPeriode periode = lireRuptures(comparaison.periode());
        RupturesPeriode reference = comparaison.reference() == null ? null : lireRuptures(comparaison.reference());
        PeriodeDTO lue = comparaison.periode();
        LocalDateTime debut = lue.du().atStartOfDay();
        LocalDateTime fin = lue.au().plusDays(1).atStartOfDay();
        List<PeremptionMoisDTO> peremptions = pilotageRupturesRepository.sommerPeremptionsParMois(aujourdhui, finDeMois(aujourdhui.plusMonths(MOIS_PEREMPTIONS - 1L)));
        LocalDate limiteAPerimer = aujourdhui.plusMonths(MOIS_A_PERIMER);
        long aPerimer = 0;
        for (PeremptionMoisDTO peremption : peremptions) {
            if (!LocalDate.of(peremption.annee(), peremption.mois(), 1).isAfter(limiteAPerimer)) {
                aPerimer += peremption.valeur();
            }
        }
        return new RupturesPilotageDTO(
            comparaison,
            Variations.cellule(UniteIndicateur.POURCENTAGE, periode.tauxRupture(), reference == null ? null : reference.tauxRupture()),
            Variations.cellule(UniteIndicateur.MONTANT, (double) periode.ventesManquees(), reference == null ? null : (double) reference.ventesManquees()),
            Variations.cellule(UniteIndicateur.POURCENTAGE, periode.tauxVentesManquees(), reference == null ? null : reference.tauxVentesManquees()),
            Variations.cellule(UniteIndicateur.MONTANT, (double) periode.valeurPerimee(), reference == null ? null : (double) reference.valeurPerimee()),
            aPerimer,
            pilotageRupturesRepository.sommerRupturesParFournisseur(debut, fin),
            pilotageRupturesRepository.sommerRupturesParProduit(debut, fin, PageRequest.of(0, LISTE_MAX)),
            pilotageRupturesRepository.sommerVentesManqueesParProduit(debut, fin, PageRequest.of(0, LISTE_MAX)),
            peremptions
        );
    }

    private RupturesPeriode lireRuptures(PeriodeDTO periode) {
        LocalDateTime debut = periode.du().atStartOfDay();
        LocalDateTime fin = periode.au().plusDays(1).atStartOfDay();
        long lignesCommandees = pilotageRupturesRepository.compterLignesCommandees(periode.du(), periode.au());
        long ruptures = pilotageRupturesRepository.compterRuptures(debut, fin);
        ComptageDTO avoirs = pilotageRupturesRepository.sommerVentesManquees(debut, fin);
        ComptageDTO quantites = pilotageRupturesRepository.sommerQuantitesEnAvoir(periode.du(), periode.au(), CategorieChiffreAffaire.officine());
        long perimee = 0;
        for (PeremptionMoisDTO peremption : pilotageRupturesRepository.sommerPeremptionsParMois(periode.du(), periode.au())) {
            perimee += peremption.valeur();
        }
        return new RupturesPeriode(
            lignesCommandees == 0 ? null : ruptures * CENT / lignesCommandees,
            avoirs.montant(),
            quantites.nombre() == 0 ? null : quantites.quantite() * CENT / quantites.nombre(),
            perimee
        );
    }

    /** Douze mois jusqu'au mois de fin de période, chacun face au même mois de l'année précédente ; un mois sans photo reste vide. */
    private static List<PointStockDTO> tracerCourbe(LocalDate mois, Map<LocalDate, Long> parMois) {
        List<PointStockDTO> points = new ArrayList<>(MOIS_COURBE);
        for (int recul = MOIS_COURBE - 1; recul >= 0; recul--) {
            LocalDate point = finDeMois(mois.minusMonths(recul));
            points.add(new PointStockDTO(point, parMois.get(point), parMois.get(finDeMois(point.minusYears(1)))));
        }
        return points;
    }

    private static LocalDate finDeMois(LocalDate jour) {
        return jour.with(TemporalAdjusters.lastDayOfMonth());
    }

}
