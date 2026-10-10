package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.PilotageAgregatRepository;
import com.kobe.warehouse.repository.PilotageClientsRepository;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Onglet « Clients & équipe » : chaque requête se lit sur la base ; le CA total est celui des ventes clôturées. */
@DisplayName("Pilotage — clients et équipe sur la base")
class ClientsEquipePilotageIntegrationTest extends AbstractReportIntegrationTest {

    private static final LocalDate JOUR = LocalDate.now().withMonth(1).withDayOfMonth(15);

    @Test
    @DisplayName("clientèle, nouveaux, perdus, revenus, CA par client, annulations et avoirs par vendeur se lisent")
    void chaqueRequeteSeLit() {
        Produit produit = produitAnalysable(unique("CLIENTS"), 1_000);
        referencement(produit, fournisseur(unique("FRS CLIENTS")));
        vendu(produit, 3, JOUR);
        vendu(produit, 1, JOUR, true, CategorieChiffreAffaire.CA);
        em.flush();
        IntegrationPostgresDatabase.bean(PilotageAgregatRepository.class).recalculer(JOUR, JOUR);
        PilotageClientsRepository repository = IntegrationPostgresDatabase.bean(PilotageClientsRepository.class);
        var officine = CategorieChiffreAffaire.officine();
        LocalDate avant = JOUR.minusDays(10);

        assertThat(repository.lireClientele(JOUR, JOUR, SalesStatut.CLOSED, officine).caTotal()).isGreaterThanOrEqualTo(3_000L);
        assertThat(repository.compterNouveaux(JOUR, JOUR, SalesStatut.CLOSED, officine)).isNotNegative();
        assertThat(repository.compterPerdus(avant, avant, JOUR, JOUR, SalesStatut.CLOSED, officine)).isNotNegative();
        assertThat(repository.compterRevenus(avant, avant, JOUR, JOUR, SalesStatut.CLOSED, officine)).isNotNegative();
        assertThat(repository.sommerCaParClient(JOUR, JOUR, SalesStatut.CLOSED, officine)).isNotNull();
        assertThat(repository.compterAnnulationsParVendeur(JOUR, JOUR, officine)).isNotEmpty();
        assertThat(repository.compterAvoirsParVendeur(JOUR.atStartOfDay(), JOUR.plusDays(1).atStartOfDay())).isNotNull();
    }
}
