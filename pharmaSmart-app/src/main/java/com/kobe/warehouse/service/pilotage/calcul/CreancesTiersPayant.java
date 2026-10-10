package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.service.dto.pilotage.DelaiObserveDTO;
import com.kobe.warehouse.service.dto.pilotage.FactureEncoursDTO;
import com.kobe.warehouse.service.dto.pilotage.FactureOrganismeDTO;
import com.kobe.warehouse.service.dto.pilotage.OrganismeTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.TrancheMontantDTO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Créances tiers payant à date, rangées en un passage : par organisme, par ancienneté, par mois d'échéance. */
public record CreancesTiersPayant(Map<String, OrganismeCreances> organismes, long[] parAge, Map<Integer, Long> parEcheance, long enRetard, long encours, double ageMontant) {
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final int MOIS_ECHEANCIER = 6;

    public static CreancesTiersPayant ranger(List<FactureEncoursDTO> factures, Map<String, DelaiObserveDTO> observes, int delaiDefaut, LocalDate aujourdhui) {
        Map<String, OrganismeCreances> organismes = new LinkedHashMap<>();
        long[] parAge = new long[4];
        Map<Integer, Long> parEcheance = new TreeMap<>();
        long enRetard = 0;
        long encours = 0;
        double ageMontant = 0;
        for (FactureEncoursDTO facture : factures) {
            OrganismeCreances organisme = organismes.computeIfAbsent(facture.cle(), cle -> OrganismeCreances.retenir(facture, observes.get(cle), delaiDefaut));
            long age = Math.max(0, ChronoUnit.DAYS.between(facture.invoiceDate(), aujourdhui));
            LocalDate echeance = facture.invoiceDate().plusDays(organisme.delai());
            boolean enRetardDePaiement = echeance.isBefore(aujourdhui);
            organisme.ajouter(facture.reste(), age, enRetardDePaiement);
            parAge[age <= 30 ? 0 : age <= 60 ? 1 : age <= 90 ? 2 : 3] += facture.reste();
            if (enRetardDePaiement) {
                enRetard += facture.reste();
            } else {
                int mois = (int) ChronoUnit.MONTHS.between(aujourdhui.withDayOfMonth(1), echeance.withDayOfMonth(1));
                parEcheance.merge(Math.min(mois, MOIS_ECHEANCIER), facture.reste(), Long::sum);
            }
            encours += facture.reste();
            ageMontant += (double) age * facture.reste();
        }
        return new CreancesTiersPayant(organismes, parAge, parEcheance, enRetard, encours, ageMontant);
    }

    /** Âge moyen de l'encours, pondéré par les montants (même calcul que le vieillissement des créances). */
    public Integer dso() {
        return encours == 0 ? null : (int) Math.round(ageMontant / encours);
    }

    public List<TrancheMontantDTO> vieillissement() {
        return List.of(
            new TrancheMontantDTO("0 à 30 jours", parAge[0]),
            new TrancheMontantDTO("31 à 60 jours", parAge[1]),
            new TrancheMontantDTO("61 à 90 jours", parAge[2]),
            new TrancheMontantDTO("Plus de 90 jours", parAge[3])
        );
    }

    public List<TrancheMontantDTO> echeancier(LocalDate aujourdhui) {
        List<TrancheMontantDTO> echeances = new ArrayList<>();
        echeances.add(new TrancheMontantDTO("En retard", enRetard));
        for (int mois = 0; mois < MOIS_ECHEANCIER; mois++) {
            echeances.add(new TrancheMontantDTO(aujourdhui.plusMonths(mois).format(MOIS), parEcheance.getOrDefault(mois, 0L)));
        }
        echeances.add(new TrancheMontantDTO("Plus tard", parEcheance.getOrDefault(MOIS_ECHEANCIER, 0L)));
        return echeances;
    }

    /** Organismes ayant facturé sur la période ou portant de l'encours, classés par encours décroissant. */
    public List<OrganismeTresorerieDTO> lister(Map<String, FactureOrganismeDTO> facturation) {
        Map<String, OrganismeTresorerieDTO> lignes = new LinkedHashMap<>();
        for (Map.Entry<String, OrganismeCreances> entree : organismes.entrySet()) {
            lignes.put(entree.getKey(), entree.getValue().versLigne(entree.getKey(), facturation.get(entree.getKey()), encours));
        }
        for (FactureOrganismeDTO factures : facturation.values()) {
            lignes.computeIfAbsent(factures.cle(), cle -> OrganismeCreances.sansEncours(factures));
        }
        return lignes.values().stream().sorted(Comparator.comparingLong(OrganismeTresorerieDTO::encours).reversed()).toList();
    }
}
