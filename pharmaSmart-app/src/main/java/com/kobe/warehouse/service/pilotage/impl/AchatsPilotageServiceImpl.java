package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.repository.PilotageAchatsStockRepository;
import com.kobe.warehouse.repository.PilotageMesuresRepository;
import com.kobe.warehouse.service.dto.pilotage.AchatsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AchatsVentesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneAchatsVentesDTO;
import com.kobe.warehouse.service.dto.pilotage.LignesJourDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantJourDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantsFamilleDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.PointAchatsVentesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.pilotage.AchatsPilotageService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.AchatsPeriode;
import com.kobe.warehouse.service.pilotage.calcul.DecoupagePeriode;
import com.kobe.warehouse.service.pilotage.calcul.LibellesPilotage;
import com.kobe.warehouse.service.pilotage.calcul.Variations;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AchatsPeriode reçus, datés à la réception , au prix d'achat de la commande (TTC, la taxe étant comprise) ;
 * face à eux, le coût d'achat des ventes sur la quantité demandée.
 */
@Service
@Transactional(readOnly = true)
public class AchatsPilotageServiceImpl implements AchatsPilotageService {

    private static final List<OrderStatut> RECUES = List.of(OrderStatut.RECEIVED, OrderStatut.CLOSED);

    private final PeriodeComparaisonService periodeComparaisonService;
    private final PilotageAchatsStockRepository pilotageAchatsStockRepository;
    private final PilotageMesuresRepository pilotageMesuresRepository;

    public AchatsPilotageServiceImpl(
        PeriodeComparaisonService periodeComparaisonService,
        PilotageAchatsStockRepository pilotageAchatsStockRepository,
        PilotageMesuresRepository pilotageMesuresRepository
    ) {
        this.periodeComparaisonService = periodeComparaisonService;
        this.pilotageAchatsStockRepository = pilotageAchatsStockRepository;
        this.pilotageMesuresRepository = pilotageMesuresRepository;
    }

    @Override
    public AchatsPilotageDTO analyserAchats(RequetePilotageDTO requete) {
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        AchatsPeriode periode = lireAchats(comparaison.periode());
        AchatsPeriode reference = comparaison.reference() == null ? null : lireAchats(comparaison.reference());
        return new AchatsPilotageDTO(
            comparaison,
            Variations.cellule(UniteIndicateur.MONTANT, (double) periode.totalTtc(), reference == null ? null : (double) reference.totalTtc()),
            Variations.cellule(UniteIndicateur.MONTANT, (double) periode.totalHt(), reference == null ? null : (double) reference.totalHt()),
            Variations.cellule(UniteIndicateur.NOMBRE, (double) periode.nbBons(), reference == null ? null : (double) reference.nbBons()),
            Variations.cellule(UniteIndicateur.JOURS, periode.delaiMoyen(), reference == null ? null : reference.delaiMoyen()),
            Variations.cellule(UniteIndicateur.POURCENTAGE, periode.conformite(), reference == null ? null : reference.conformite()),
            periode.lignesFournisseurs(reference),
            periode.lignesFamilles(reference)
        );
    }

    @Override
    public AchatsVentesPilotageDTO analyserAchatsVentes(RequetePilotageDTO requete) {
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        PeriodeDTO periode = comparaison.periode();
        List<PeriodeDTO> tranches = DecoupagePeriode.decouper(periode, requete.granularite());
        NavigableMap<LocalDate, Integer> rangs = new TreeMap<>();
        for (int rang = 0; rang < tranches.size(); rang++) {
            rangs.put(tranches.get(rang).du(), rang);
        }
        long[] achats = new long[tranches.size()];
        long[] couts = new long[tranches.size()];
        for (MontantJourDTO jour : pilotageAchatsStockRepository.sommerAchatsHtParJour(periode.du(), periode.au())) {
            achats[rangs.floorEntry(jour.jour()).getValue()] += jour.montant();
        }
        for (LignesJourDTO jour : pilotageMesuresRepository.listerLignesParJour(periode.du(), periode.au(), CategorieChiffreAffaire.officine())) {
            couts[rangs.floorEntry(jour.jour()).getValue()] += jour.coutHt();
        }
        List<PointAchatsVentesDTO> points = new ArrayList<>(tranches.size());
        for (int rang = 0; rang < tranches.size(); rang++) {
            PeriodeDTO tranche = tranches.get(rang);
            points.add(new PointAchatsVentesDTO(tranche.du(), tranche.au(), LibellesPilotage.libellerTranche(tranche, requete.granularite()), achats[rang], couts[rang]));
        }
        return new AchatsVentesPilotageDTO(comparaison, points, comparerParFamille(periode));
    }

    /** AchatsPeriode HT face au coût HT des ventes, famille par famille ; les plus gros écarts d'abord. */
    private List<LigneAchatsVentesDTO> comparerParFamille(PeriodeDTO periode) {
        Map<String, LigneAchatsVentesDTO> parFamille = new LinkedHashMap<>();
        for (MontantsFamilleDTO famille : pilotageAchatsStockRepository.sommerAchatsParFamille(periode.du(), periode.au())) {
            parFamille.put(famille.cle(), ligneAchatsVentes(famille.cle(), famille.libelle(), famille.montantHt(), 0));
        }
        for (MontantsFamilleDTO famille : pilotageAchatsStockRepository.sommerCoutVentesParFamille(periode.du(), periode.au(), CategorieChiffreAffaire.officine())) {
            parFamille.compute(famille.cle(), (k, achats) -> ligneAchatsVentes(famille.cle(), famille.libelle(), achats == null ? 0 : achats.achatsHt(), famille.montantHt()));
        }
        return parFamille.values().stream().sorted(Comparator.comparingLong((LigneAchatsVentesDTO ligne) -> Math.abs(ligne.ecart())).reversed()).toList();
    }

    private static LigneAchatsVentesDTO ligneAchatsVentes(String cle, String libelle, long achats, long couts) {
        return new LigneAchatsVentesDTO(cle, libelle, achats, couts, achats - couts, achats == 0 ? null : (double) couts / achats);
    }

    private AchatsPeriode lireAchats(PeriodeDTO periode) {
        return AchatsPeriode.lire(
            pilotageAchatsStockRepository.sommerAchatsParFournisseur(periode.du(), periode.au(), RECUES),
            pilotageAchatsStockRepository.listerDelaisParFournisseur(periode.du(), periode.au(), RECUES),
            pilotageAchatsStockRepository.sommerAchatsParFamille(periode.du(), periode.au())
        );
    }

}
