package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.PilotageTresorerieRepository;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/** Onglet « Trésorerie & tiers payant » : chaque requête se lit sur la base. */
@DisplayName("Pilotage — trésorerie sur la base")
class TresoreriePilotageIntegrationTest extends AbstractReportIntegrationTest {

    private static final LocalDate JOUR = LocalDate.now().withMonth(1).withDayOfMonth(15);
    private static final Set<String> ENCAISSEMENTS = Set.of("SalePayment", "DifferePayment", "InvoicePayment");

    @Test
    @DisplayName("encaissements, sessions, facturation, encours, délais observés, différés et avoirs se lisent")
    void chaqueRequeteSeLit() {
        PilotageTresorerieRepository repository = IntegrationPostgresDatabase.bean(PilotageTresorerieRepository.class);
        var debut = JOUR.atStartOfDay();
        var fin = JOUR.plusDays(1).atStartOfDay();
        var officine = CategorieChiffreAffaire.officine();

        assertThat(repository.sommerEncaissementsParJourEtMode(JOUR, JOUR, ENCAISSEMENTS)).isNotNull();
        assertThat(repository.sommerEncaissementsParCaissier(JOUR, JOUR, ENCAISSEMENTS)).isNotNull();
        assertThat(repository.compterSessionsParCaissier(debut, fin)).isNotNull();
        assertThat(repository.sommerFacturationParOrganisme(JOUR, JOUR)).isNotNull();
        assertThat(repository.listerFacturesNonSoldees(List.of(InvoiceStatut.NOT_PAID, InvoiceStatut.PARTIALLY_PAID))).isNotNull();
        assertThat(repository.listerDelaisObserves(InvoiceStatut.PAID)).isNotNull();
        assertThat(repository.sommerDifferesParClient(SalesStatut.CLOSED, officine, PageRequest.of(0, 5))).isNotNull();
        assertThat(repository.sommerDifferesParAge(JOUR.minusDays(30), JOUR.minusDays(60), JOUR.minusDays(90), SalesStatut.CLOSED, officine).moins30()).isNotNegative();
        assertThat(repository.sommerAvoirsEmis(debut, fin).nombre()).isNotNegative();
        assertThat(repository.sommerAvoirsRembourses(debut, fin, List.of(ModeClotureAvoir.REMBOURSEMENT_ESPECES)).nombre()).isNotNegative();
    }
}
