package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.repository.PilotageMesuresRepository;
import com.kobe.warehouse.service.dto.pilotage.LignesJourDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantJourDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.VentesJourDTO;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Lit les mesures journalières d'une période et les range dans ses tranches : un seul passage par série lue. */
@Component
@Transactional(readOnly = true)
class LecteurMesuresPilotage {

    /** Recettes : règlements de ventes, de différés et de factures tiers payant (ni paiements fournisseurs, ni caisse diverse). */
    static final Set<String> TYPES_ENCAISSEMENT = Set.of("SalePayment", "DifferePayment", "InvoicePayment");

    private final PilotageMesuresRepository pilotageMesuresRepository;

    LecteurMesuresPilotage(PilotageMesuresRepository pilotageMesuresRepository) {
        this.pilotageMesuresRepository = pilotageMesuresRepository;
    }

    MesuresPilotage lireTotal(PeriodeDTO periode) {
        return lireParTranche(periode, List.of(periode)).getFirst();
    }

    /** Le même mois N-1, coupé au même jour : [début, même jour] puis le reste du mois s'il en reste ; sert aux projections. */
    List<MesuresPilotage> lireMemeMoisN1(LocalDate aujourdhui) {
        LocalDate memeJour = aujourdhui.minusYears(1);
        LocalDate debut = memeJour.withDayOfMonth(1);
        LocalDate fin = memeJour.withDayOfMonth(memeJour.lengthOfMonth());
        List<PeriodeDTO> tranches = memeJour.isBefore(fin)
            ? List.of(new PeriodeDTO(debut, memeJour), new PeriodeDTO(memeJour.plusDays(1), fin))
            : List.of(new PeriodeDTO(debut, fin));
        return lireParTranche(new PeriodeDTO(debut, fin), tranches);
    }

    List<MesuresPilotage> lireParTranche(PeriodeDTO periode, List<PeriodeDTO> tranches) {
        NavigableMap<LocalDate, Integer> rangParDebut = new TreeMap<>();
        for (int rang = 0; rang < tranches.size(); rang++) {
            rangParDebut.put(tranches.get(rang).du(), rang);
        }
        List<MesuresPilotage> mesures = new ArrayList<>(Collections.nCopies(tranches.size(), MesuresPilotage.AUCUNE));
        Set<CategorieChiffreAffaire> officine = CategorieChiffreAffaire.officine();
        LocalDate du = periode.du();
        LocalDate au = periode.au();

        ajouter(mesures, rangParDebut, pilotageMesuresRepository.listerVentesParJour(du, au, officine), VentesJourDTO::jour, MesuresPilotage::deVentes);
        ajouter(mesures, rangParDebut, pilotageMesuresRepository.listerLignesParJour(du, au, officine), LignesJourDTO::jour, MesuresPilotage::deLignes);
        ajouter(mesures, rangParDebut, pilotageMesuresRepository.listerAchatsParJour(du, au), MontantJourDTO::jour, a -> MesuresPilotage.dAchats(a.montant()));
        ajouter(
            mesures,
            rangParDebut,
            pilotageMesuresRepository.listerEncaissementsParJour(du, au, TYPES_ENCAISSEMENT),
            MontantJourDTO::jour,
            e -> MesuresPilotage.dEncaissements(e.montant())
        );
        return mesures;
    }

    private static <T> void ajouter(
        List<MesuresPilotage> mesures,
        NavigableMap<LocalDate, Integer> rangParDebut,
        List<T> lignes,
        Function<T, LocalDate> jour,
        Function<T, MesuresPilotage> versMesures
    ) {
        for (T ligne : lignes) {
            int rang = rangParDebut.floorEntry(jour.apply(ligne)).getValue();
            mesures.set(rang, mesures.get(rang).plus(versMesures.apply(ligne)));
        }
    }
}
