package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.repository.PilotageDemarqueRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.DemarqueRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneDemarqueDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantDemarqueDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.pilotage.DemarquePilotageService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.CalculateurIndicateurs;
import com.kobe.warehouse.service.pilotage.calcul.MotifDemarque;
import com.kobe.warehouse.service.pilotage.calcul.Variations;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Démarque : ajustements de sortie clôturés par motif ; rapportée au CA TTC de la même période. */
@Service
@Transactional(readOnly = true)
public class DemarquePilotageServiceImpl implements DemarquePilotageService {

    private static final int PRODUITS_MAX = 10;
    private static final double CENT = 100.0;

    private final PeriodeComparaisonService periodeComparaisonService;
    private final PilotageDemarqueRepository pilotageDemarqueRepository;
    private final LecteurMesuresPilotage lecteurMesuresPilotage;

    public DemarquePilotageServiceImpl(
        PeriodeComparaisonService periodeComparaisonService,
        PilotageDemarqueRepository pilotageDemarqueRepository,
        LecteurMesuresPilotage lecteurMesuresPilotage
    ) {
        this.periodeComparaisonService = periodeComparaisonService;
        this.pilotageDemarqueRepository = pilotageDemarqueRepository;
        this.lecteurMesuresPilotage = lecteurMesuresPilotage;
    }

    @Override
    public DemarqueRentabiliteDTO analyserDemarque(RequetePilotageDTO requete) {
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        boolean avecReference = comparaison.reference() != null;

        Map<String, MotifDemarque> parMotif = new LinkedHashMap<>();
        long valeur = 0;
        for (MontantDemarqueDTO motif : sommerParMotif(comparaison.periode())) {
            parMotif.put(motif.cle(), new MotifDemarque(motif.libelle(), motif.quantite(), motif.valeur(), 0));
            valeur += motif.valeur();
        }
        long valeurReference = 0;
        if (avecReference) {
            for (MontantDemarqueDTO motif : sommerParMotif(comparaison.reference())) {
                parMotif.merge(motif.cle(), new MotifDemarque(motif.libelle(), 0, 0, motif.valeur()), MotifDemarque::plus);
                valeurReference += motif.valeur();
            }
        }
        Double ca = CalculateurIndicateurs.calculer(IndicateurPilotage.CA_TTC, lecteurMesuresPilotage.lireTotal(comparaison.periode()));
        Double caReference = avecReference
            ? CalculateurIndicateurs.calculer(IndicateurPilotage.CA_TTC, lecteurMesuresPilotage.lireTotal(comparaison.reference()))
            : null;

        return new DemarqueRentabiliteDTO(
            comparaison,
            Variations.cellule(UniteIndicateur.MONTANT, (double) valeur, avecReference ? (double) valeurReference : null),
            Variations.cellule(UniteIndicateur.POURCENTAGE, part(valeur, ca), avecReference ? part(valeurReference, caReference) : null),
            parMotif
                .values()
                .stream()
                .sorted(Comparator.comparingLong(MotifDemarque::valeur).reversed())
                .map(motif -> new LigneDemarqueDTO(motif.libelle(), motif.quantite(), Variations.cellule(UniteIndicateur.MONTANT, (double) motif.valeur(), avecReference ? (double) motif.valeurReference() : null)))
                .toList(),
            pilotageDemarqueRepository.listerProduitsLesPlusTouches(
                debut(comparaison.periode()),
                fin(comparaison.periode()),
                AjustementStatut.CLOSED,
                AjustType.AJUSTEMENT_OUT,
                PageRequest.of(0, PRODUITS_MAX)
            )
        );
    }

    private List<MontantDemarqueDTO> sommerParMotif(PeriodeDTO periode) {
        return pilotageDemarqueRepository.sommerParMotif(debut(periode), fin(periode), AjustementStatut.CLOSED, AjustType.AJUSTEMENT_OUT);
    }

    private static LocalDateTime debut(PeriodeDTO periode) {
        return periode.du().atStartOfDay();
    }

    private static LocalDateTime fin(PeriodeDTO periode) {
        return periode.au().plusDays(1).atStartOfDay();
    }

    private static Double part(long valeur, Double ca) {
        return ca == null || ca == 0 ? null : valeur * CENT / ca;
    }

}
