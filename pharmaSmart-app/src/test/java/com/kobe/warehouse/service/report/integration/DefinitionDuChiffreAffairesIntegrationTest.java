package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.repository.ComparativeReportRepository;
import com.kobe.warehouse.repository.PilotageVenteRepository;
import com.kobe.warehouse.service.dto.pilotage.ChiffreAffairesReferenceDTO;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Phase 0 du pilotage (docs/PLAN-PILOTAGE-OFFICINE.md) : le même mois, lu par chaque source actuelle, confronté à la
 * définition de référence du CA de l'officine — ventes clôturées non annulées de catégorie CA, ventes importées du dépôt
 * comprises, ventes au dépôt exclues.
 *
 * <p>Les écarts constatés sont des faits documentés, pas des défauts à corriger ici : chaque assertion dit d'où vient
 * l'écart. Une source qui changerait de définition ferait échouer le test, et le dictionnaire serait à revoir.
 */
@DisplayName("Pilotage — définition du CA confrontée aux sources existantes")
class DefinitionDuChiffreAffairesIntegrationTest extends AbstractReportIntegrationTest {

    // Les partitions de `sales` du conteneur de test ne couvrent que l'année en cours.
    private static final LocalDate JOUR = LocalDate.now().withMonth(1).withDayOfMonth(15);
    private static final LocalDate DEBUT_DU_MOIS = JOUR.withDayOfMonth(1);
    private static final LocalDate FIN_DU_MOIS = JOUR.withDayOfMonth(JOUR.lengthOfMonth());

    private static final int VENTE_COMPTOIR = 10_000;
    private static final int REMISE_COMPTOIR = 1_000;
    private static final int VENTE_IMPORTEE_DU_DEPOT = 4_000;
    private static final int VENTE_AU_DEPOT = 7_000;
    private static final int VENTE_ANNULEE = 3_000;

    private PilotageVenteRepository pilotageVenteRepository;

    @BeforeEach
    void constituerLeMois() {
        pilotageVenteRepository = IntegrationPostgresDatabase.bean(PilotageVenteRepository.class);
        venteDeCa(VENTE_COMPTOIR, REMISE_COMPTOIR, false, false);
        venteDeCa(VENTE_IMPORTEE_DU_DEPOT, 0, true, false);
        venteDeCa(VENTE_ANNULEE, 0, false, true);
        venteDepot(JOUR, VENTE_AU_DEPOT);
    }

    @Test
    @DisplayName("la référence compte le comptoir et les ventes importées du dépôt, pas le dépôt ni l'annulé")
    void laReferenceCompteLeCaDeLOfficine() {
        ChiffreAffairesReferenceDTO reference = pilotageVenteRepository.calculerChiffreAffairesOfficine(DEBUT_DU_MOIS, FIN_DU_MOIS);

        assertThat(reference.nbVentes()).isEqualTo(2);
        assertThat(reference.caTtc()).isEqualTo(VENTE_COMPTOIR + VENTE_IMPORTEE_DU_DEPOT);
        assertThat(reference.remises()).isEqualTo(REMISE_COMPTOIR);
        assertThat(reference.caNet()).isEqualTo(VENTE_COMPTOIR + VENTE_IMPORTEE_DU_DEPOT - REMISE_COMPTOIR);
    }

    @Test
    @DisplayName("mv_dashboard_ca_daily (tableau de bord CA) suit la référence")
    void leTableauDeBordSuitLaReference() {
        rafraichir("mv_dashboard_ca_daily");

        long caTotal = somme("SELECT COALESCE(sum(ca_total), 0) FROM mv_dashboard_ca_daily WHERE sale_date BETWEEN :du AND :au");

        assertThat(caTotal).isEqualTo(reference().caTtc());
    }

    @Test
    @DisplayName("mv_daily_sales_summary (synthèse journalière) écarte les ventes importées du dépôt — laissé tel quel")
    void laSyntheseJournaliereEcarteLesVentesImportees() {
        rafraichir("mv_daily_sales_summary");

        long caCategorieCa = somme(
            "SELECT COALESCE(sum(ca_total), 0) FROM mv_daily_sales_summary WHERE sale_date BETWEEN :du AND :au AND categorie_ca = 'CA'"
        );
        long caDepot = somme(
            "SELECT COALESCE(sum(ca_total), 0) FROM mv_daily_sales_summary WHERE sale_date BETWEEN :du AND :au AND categorie_ca = 'CA_DEPOT'"
        );

        assertThat(caCategorieCa)
            .as("filtre imported = false de la vue : l'écart est exactement la vente importée du dépôt")
            .isEqualTo(reference().caTtc() - VENTE_IMPORTEE_DU_DEPOT);
        assertThat(caDepot).as("le dépôt reste sur sa propre ligne, l'écran le ventile à part").isEqualTo(VENTE_AU_DEPOT);
    }

    @Test
    @DisplayName("le comparatif N / N-1 affiche le CA net de remises, pas le CA TTC")
    void leComparatifAfficheLeCaNet() {
        List<Object[]> mois = new ComparativeReportRepository(em).findMonthlyComparison(JOUR.getYear(), JOUR.getYear() - 1);

        Object[] mai = mois.stream().filter(ligne -> ((Number) ligne[0]).intValue() == JOUR.getMonthValue()).findFirst().orElseThrow();

        assertThat(((Number) mai[1]).longValue())
            .as("même périmètre que la référence, mais remises déduites")
            .isEqualTo(reference().caNet());
    }

    private ChiffreAffairesReferenceDTO reference() {
        return pilotageVenteRepository.calculerChiffreAffairesOfficine(DEBUT_DU_MOIS, FIN_DU_MOIS);
    }

    private void venteDeCa(int montant, int remise, boolean importee, boolean annulee) {
        CashSale vente = venteFermee(JOUR, annulee, CategorieChiffreAffaire.CA);
        vente.setSalesAmount(montant);
        vente.setHtAmount(montant);
        vente.setDiscountAmount(remise);
        vente.setNetAmount(montant - remise);
        vente.setImported(importee);
        em.flush();
    }

    private long somme(String sql) {
        Object valeur = em.createNativeQuery(sql).setParameter("du", DEBUT_DU_MOIS).setParameter("au", FIN_DU_MOIS).getSingleResult();
        return ((Number) valeur).longValue();
    }
}
