package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Les alertes de stock sont la liste de ce qui doit être commandé aujourd'hui. Elles se calculent
 * dans {@code mv_stock_alerts}, une vue matérialisée qui range chaque produit en souffrance dans
 * l'une de trois catégories, et une seule : en <b>rupture</b> quand il n'y a plus rien à vendre, en
 * <b>alerte</b> quand le stock est passé sous le seuil mini, en <b>péremption</b> quand le plus
 * proche lot expire dans moins de trois mois.
 *
 * <p>L'ordre de ces trois cas est ce qui compte, parce qu'ils s'excluent : un produit à zéro est en
 * rupture, pas en alerte, même si zéro est bien en dessous du seuil. Confondre les deux ferait
 * disparaître les vraies ruptures dans la masse des seuils franchis, et c'est la rupture qui fait
 * repartir un client sans son médicament.
 *
 * <p>Le périmètre se vérifie aussi : un produit sain n'a pas à figurer dans une liste d'alertes, et
 * seul le stock du magasin principal est compté.
 */
@DisplayName("StockAlertReportService — alertes de stock sur mv_stock_alerts")
class StockAlertReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    private static final String VUE = "mv_stock_alerts";

    // ===== classification =====

    @Nested
    @DisplayName("Classification des alertes")
    class Classification {

        @Test
        @DisplayName("un produit sans stock est en rupture")
        void stockNulEstUneRupture() {
            produitAvecStock("EN RUPTURE", 10, 0);
            rafraichir(VUE);

            assertThat(alertes()).extracting(StockAlertDTO::alertType).containsExactly(StockAlertType.RUPTURE);
        }

        @Test
        @DisplayName("un produit sous son seuil mini est en alerte")
        void stockSousLeSeuilEstUneAlerte() {
            produitAvecStock("SOUS SEUIL", 10, 3);
            rafraichir(VUE);

            List<StockAlertDTO> alertes = alertes();

            assertThat(alertes).extracting(StockAlertDTO::alertType).containsExactly(StockAlertType.ALERTE);
            assertThat(alertes.getFirst().stockQuantity()).isEqualTo(3);
            assertThat(alertes.getFirst().seuilMin()).isEqualTo(10);
        }

        /**
         * Zéro est à la fois « rien en stock » et « sous le seuil ». La vue tranche en faveur de la
         * rupture : sans cette priorité, une rupture se lirait comme un simple réassort à prévoir.
         */
        @Test
        @DisplayName("la rupture prime sur l'alerte de seuil")
        void ruptureL_emporteSurAlerte() {
            produitAvecStock("EN RUPTURE", 10, 0);
            rafraichir(VUE);

            assertThat(alertes().getFirst().alertType()).isEqualTo(StockAlertType.RUPTURE);
        }

        @Test
        @DisplayName("un produit approvisionné dont un lot périme bientôt est en péremption")
        void lotProcheEstUnePeremption() {
            Produit produit = produitAvecStock("BIENTOT PERIME", 5, 50);
            lot(produit, LocalDate.now().plusMonths(1), 50);
            rafraichir(VUE);

            List<StockAlertDTO> alertes = alertes();

            assertThat(alertes).extracting(StockAlertDTO::alertType).containsExactly(StockAlertType.PEREMPTION);
            assertThat(alertes.getFirst().expiryDate()).isEqualTo(LocalDate.now().plusMonths(1));
        }

        /** Entre deux lots, c'est le plus proche qui commande l'alerte — c'est lui qu'on doit écouler. */
        @Test
        @DisplayName("c'est le lot le plus proche qui date l'alerte")
        void lotLePlusProcheDate() {
            Produit produit = produitAvecStock("BIENTOT PERIME", 5, 50);
            lot(produit, LocalDate.now().plusMonths(2), 20);
            lot(produit, LocalDate.now().plusMonths(1), 30);
            rafraichir(VUE);

            assertThat(alertes().getFirst().expiryDate()).isEqualTo(LocalDate.now().plusMonths(1));
        }

        @Test
        @DisplayName("un lot épuisé ne déclenche pas d'alerte de péremption")
        void lotEpuiseIgnore() {
            Produit produit = produitAvecStock("SAIN", 5, 50);
            lot(produit, LocalDate.now().plusMonths(1), 0);
            rafraichir(VUE);

            assertThat(alertes()).isEmpty();
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        @Test
        @DisplayName("un produit sainement approvisionné n'apparaît pas")
        void produitSainAbsent() {
            produitAvecStock("SAIN", 5, 100);
            rafraichir(VUE);

            assertThat(alertes()).isEmpty();
        }

        @Test
        @DisplayName("un lot à plus de trois mois ne déclenche rien")
        void lotLointainIgnore() {
            Produit produit = produitAvecStock("SAIN", 5, 100);
            lot(produit, LocalDate.now().plusMonths(9), 100);
            rafraichir(VUE);

            assertThat(alertes()).isEmpty();
        }

        @Test
        @DisplayName("base vierge : aucune alerte, et des compteurs à zéro")
        void baseVierge() {
            rafraichir(VUE);

            assertThat(alertes()).isEmpty();

            Map<StockAlertType, Long> compteurs = services.stockAlertReportService.getStockAlertsCount();

            assertThat(compteurs)
                .containsEntry(StockAlertType.RUPTURE, 0L)
                .containsEntry(StockAlertType.ALERTE, 0L)
                .containsEntry(StockAlertType.PEREMPTION, 0L);
        }

        @Test
        @DisplayName("le code CIP du référencement principal accompagne chaque alerte")
        void codeCipReporte() {
            produitAvecStock("EN RUPTURE", 10, 0);
            rafraichir(VUE);

            assertThat(alertes().getFirst().codeCip()).isNotBlank();
        }
    }

    // ===== lecture =====

    @Nested
    @DisplayName("Lecture")
    class Lecture {

        @Test
        @DisplayName("le filtre par type ne rend que les alertes demandées")
        void filtreParType() {
            produitAvecStock("EN RUPTURE", 10, 0);
            produitAvecStock("SOUS SEUIL", 10, 3);
            rafraichir(VUE);

            Page<StockAlertDTO> ruptures = services.stockAlertReportService.getStockAlerts(
                List.of(StockAlertType.RUPTURE),
                Pageable.unpaged()
            );

            assertThat(ruptures.getContent()).extracting(StockAlertDTO::libelle).containsExactly("EN RUPTURE");
        }

        @Test
        @DisplayName("un filtre vide vaut absence de filtre")
        void filtreVideVautTout() {
            produitAvecStock("EN RUPTURE", 10, 0);
            produitAvecStock("SOUS SEUIL", 10, 3);
            rafraichir(VUE);

            assertThat(services.stockAlertReportService.getStockAlerts(List.of(), Pageable.unpaged()).getContent()).hasSize(2);
        }

        @Test
        @DisplayName("la pagination borne les résultats et rapporte le total")
        void pagination() {
            produitAvecStock("EN RUPTURE A", 10, 0);
            produitAvecStock("EN RUPTURE B", 10, 0);
            produitAvecStock("EN RUPTURE C", 10, 0);
            rafraichir(VUE);

            Page<StockAlertDTO> page = services.stockAlertReportService.getStockAlerts(null, PageRequest.of(0, 2));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("les compteurs recensent chaque type, y compris ceux à zéro")
        void compteursParType() {
            produitAvecStock("EN RUPTURE A", 10, 0);
            produitAvecStock("EN RUPTURE B", 10, 0);
            produitAvecStock("SOUS SEUIL", 10, 3);
            rafraichir(VUE);

            Map<StockAlertType, Long> compteurs = services.stockAlertReportService.getStockAlertsCount();

            assertThat(compteurs)
                .containsEntry(StockAlertType.RUPTURE, 2L)
                .containsEntry(StockAlertType.ALERTE, 1L)
                .containsEntry(StockAlertType.PEREMPTION, 0L);
        }
    }

    // ===== fabriques locales =====

    private List<StockAlertDTO> alertes() {
        return services.stockAlertReportService.getStockAlerts(null, Pageable.unpaged()).getContent();
    }

    /**
     * Un produit référencé, doté d'un seuil mini et d'un stock au rayon. La vue ne compte que le
     * stock du magasin principal — un emplacement d'un autre magasin ne l'alimenterait pas.
     */
    private Produit produitAvecStock(String libelle, int seuilMini, int quantite) {
        Produit produit = produit(libelle, TypeProduit.PACKAGE, 10_000, seuilMini);
        referencement(produit, fournisseur("LABOREX " + unique("")));
        stock(produit, quantite);
        return produit;
    }
}
