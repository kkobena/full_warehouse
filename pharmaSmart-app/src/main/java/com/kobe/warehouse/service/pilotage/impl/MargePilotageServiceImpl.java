package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.repository.PilotageVentesDetailRepository;
import com.kobe.warehouse.service.dto.pilotage.AxeAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneMargeDTO;
import com.kobe.warehouse.service.dto.pilotage.MargeRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.VenteMargeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.MargePilotageService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.MargeElement;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MargeElement brute HT sur la quantité demandée (décision du 2026-10-09). Écart de marge d'un élément = effet volume (CA HT en plus ou
 * en moins, au taux de référence) + effet taux (au nouveau CA HT). Variation du taux global = effet mix (poids des éléments) +
 * effet taux (taux de chaque élément) ; les deux décompositions sont exactes.
 */
@Service
@Transactional(readOnly = true)
public class MargePilotageServiceImpl implements MargePilotageService {

    private static final int FAIBLES_MARGES_MAX = 20;
    private static final int VENTES_MAX = 50;
    private static final double CENT = 100.0;

    private final PeriodeComparaisonService periodeComparaisonService;
    private final DictionnaireIndicateursService dictionnaireIndicateursService;
    private final VentilateurPilotage ventilateurPilotage;
    private final PilotageVentesDetailRepository pilotageVentesDetailRepository;
    private final AppConfigurationService appConfigurationService;

    public MargePilotageServiceImpl(
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
    public MargeRentabiliteDTO analyserMarge(RequetePilotageDTO requete, AxeAnalyse axe) {
        if (!axe.estLuEnBase() || !axe.getSources().contains(SourceAnalyse.LIGNES)) {
            throw new GenericError("La marge ne se ventile pas par " + axe.getLibelle().toLowerCase());
        }
        dictionnaireIndicateursService.verifierAxesAutorises(List.of(axe));
        ComparaisonPeriodesDTO comparaison = periodeComparaisonService.comparer(requete, LocalDate.now());
        var ventilation = ventilateurPilotage.comparer(SourceAnalyse.LIGNES, comparaison, axe, null, List.of(), requete.granularite());
        boolean avecReference = comparaison.reference() != null;
        MargeElement total = MargeElement.lire(ventilation.total());

        List<LigneMargeDTO> lignes = new ArrayList<>(ventilation.elements().size());
        double effetMix = 0;
        double effetTaux = 0;
        for (MesuresComparees element : ventilation.elements()) {
            MargeElement marge = MargeElement.lire(element);
            lignes.add(marge.versLigne(element, avecReference));
            effetMix += (marge.poids(total.caHt()) - marge.poidsReference(total.caHtReference())) * marge.tauxDeReference();
            effetTaux += marge.poids(total.caHt()) * (marge.tauxDeLaPeriode() - marge.tauxDeReference());
        }
        lignes.sort(Comparator.comparingLong(LigneMargeDTO::marge).reversed());

        int seuil = appConfigurationService.getSeuilFaibleMargePilotage();
        return new MargeRentabiliteDTO(
            comparaison,
            AxeAnalyseDTO.fromAxe(axe),
            total.taux(),
            avecReference ? total.tauxReference() : null,
            avecReference ? effetMix * CENT : null,
            avecReference ? effetTaux * CENT : null,
            lignes,
            seuil,
            listerFaiblesMarges(comparaison, requete, axe == AxeAnalyse.PRODUIT ? lignes : null, seuil),
            listerVentesAMargeNegative(comparaison)
        );
    }

    /** Produits vendus sous le seuil, les plus gros CA d'abord : c'est là que la marge se perd. */
    private List<LigneMargeDTO> listerFaiblesMarges(ComparaisonPeriodesDTO comparaison, RequetePilotageDTO requete, List<LigneMargeDTO> produits, int seuil) {
        List<LigneMargeDTO> parProduit = produits != null
            ? produits
            : ventilateurPilotage
                .comparer(SourceAnalyse.LIGNES, new ComparaisonPeriodesDTO(comparaison.periode(), null, comparaison.aDate()), AxeAnalyse.PRODUIT, null, List.of(), requete.granularite())
                .elements()
                .stream()
                .map(element -> MargeElement.lire(element).versLigne(element, false))
                .toList();
        return parProduit
            .stream()
            .filter(ligne -> ligne.caHt() > 0 && ligne.taux() != null && ligne.taux() < seuil)
            .sorted(Comparator.comparingLong(LigneMargeDTO::caHt).reversed())
            .limit(FAIBLES_MARGES_MAX)
            .toList();
    }

    private List<VenteMargeDTO> listerVentesAMargeNegative(ComparaisonPeriodesDTO comparaison) {
        boolean voitLEquipe = dictionnaireIndicateursService.lireDroitsAccordes().contains(DroitPilotage.CLIENTS_EQUIPE);
        return pilotageVentesDetailRepository
            .listerVentesAMargeNegative(
                comparaison.periode().du(),
                comparaison.periode().au(),
                SalesStatut.CLOSED,
                CategorieChiffreAffaire.officine(),
                PageRequest.of(0, VENTES_MAX)
            )
            .stream()
            .map(vente -> voitLEquipe ? vente : vente.masquerVendeur())
            .toList();
    }

}
