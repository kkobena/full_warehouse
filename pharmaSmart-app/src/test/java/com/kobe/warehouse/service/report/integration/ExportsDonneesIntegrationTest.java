package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.ExportDonneesRepository;
import com.kobe.warehouse.repository.PilotageAgregatRepository;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Chaque export du catalogue se lit sur la base, et chaque ligne a exactement les colonnes que l'export annonce. */
@DisplayName("Exports — lecture des données sur la base")
class ExportsDonneesIntegrationTest extends AbstractReportIntegrationTest {

    // Les partitions des tables source du conteneur de test ne couvrent que l'année en cours.
    private static final LocalDate JOUR = LocalDate.now().withMonth(1).withDayOfMonth(15);

    private ExportDonneesRepository exportDonneesRepository;

    @BeforeEach
    void vendreUnProduit() {
        exportDonneesRepository = IntegrationPostgresDatabase.bean(ExportDonneesRepository.class);
        Produit produit = produitAnalysable(unique("EXPORT"), 1_500);
        referencement(produit, fournisseur(unique("FRS EXPORT")));
        vendu(produit, 2, JOUR);
        em.flush();
        IntegrationPostgresDatabase.bean(PilotageAgregatRepository.class).recalculer(JOUR, JOUR);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ExportDonnees.class)
    void chaqueExportSeLit(ExportDonnees export) {
        try (Stream<Object[]> lignes = lire(export)) {
            List<Object[]> lues = lignes.toList();
            assertThat(lues).allSatisfy(ligne -> assertThat(ligne).hasSize(export.getColonnes().size()));
            if (export == ExportDonnees.VENTES || export == ExportDonnees.LIGNES_VENTE || export == ExportDonnees.VENTES_JOUR_PRODUIT) {
                assertThat(lues).isNotEmpty();
            }
        }
    }

    private Stream<Object[]> lire(ExportDonnees export) {
        var officine = CategorieChiffreAffaire.officine();
        return switch (export) {
            case VENTES -> exportDonneesRepository.lireVentes(JOUR, JOUR, SalesStatut.CLOSED, officine);
            case LIGNES_VENTE -> exportDonneesRepository.lireLignesVente(JOUR, JOUR, SalesStatut.CLOSED, officine);
            case ENCAISSEMENTS -> exportDonneesRepository.lireEncaissements(JOUR, JOUR);
            case ACHATS -> exportDonneesRepository.lireAchats(JOUR, JOUR, List.of(OrderStatut.RECEIVED, OrderStatut.CLOSED));
            case MOUVEMENTS_STOCK -> exportDonneesRepository.lireMouvementsStock(JOUR, JOUR);
            case PRODUITS -> exportDonneesRepository.lireProduits();
            case CLIENTS -> exportDonneesRepository.lireClients();
            case VENTES_JOUR_PRODUIT -> exportDonneesRepository.lireVentesJourProduit(JOUR, JOUR, officine);
        };
    }
}
