package com.kobe.warehouse.service.report.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.service.stock.dto.RecapProduitVendu;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduRequestParam;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduSummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Les deux récapitulatifs — produits vendus et produits invendus — s'impriment depuis le même écran
 * et par deux services jumeaux. Ils ne recalculent rien : ils reprennent le résumé que le service
 * de données leur remet. Ce qui s'y joue est donc le <b>titre</b>, qui doit porter la période
 * analysée.
 *
 * <p>Un récapitulatif imprimé sans sa période ne veut rien dire : posé sur un bureau à côté d'un
 * autre, plus rien ne les distingue. Le titre l'intègre quand les deux bornes sont fournies, et se
 * contente de l'intitulé sinon — jamais « du null au null ».
 */
@DisplayName("RecapProduitVenduPdfService — récapitulatifs vendus et invendus")
class RecapProduitVenduPdfServiceTest {

    private RecapProduitVenduPdfService vendus;
    private RecapProduitInvenduPdfService invendus;

    @BeforeEach
    void setUp() {
        vendus = new RecapProduitVenduPdfService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits()
        );
        invendus = new RecapProduitInvenduPdfService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits()
        );
    }

    // ===== produits vendus =====

    @Nested
    @DisplayName("Produits vendus")
    class ProduitsVendus {

        @Test
        @DisplayName("le titre porte la période analysée")
        void titreAvecPeriode() {
            vendus.export(page(ligne("DOLIPRANE")), resume(), periode(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 31)));

            assertThat(vendus.getParameters().get("reportTitle"))
                .isEqualTo("Récapitulatif des Produits Vendus du 05/01/2026 au 31/03/2026");
        }

        /** Sans bornes, l'intitulé reste seul — plutôt qu'assorti de dates nulles. */
        @Test
        @DisplayName("sans période, le titre se réduit à son intitulé")
        void titreSansPeriode() {
            vendus.export(page(ligne("DOLIPRANE")), resume(), periode(null, null));

            assertThat(vendus.getParameters().get("reportTitle")).isEqualTo("Récapitulatif des Produits Vendus");
        }

        @Test
        @DisplayName("le résumé est éclaté en variables directement utilisables par le gabarit")
        void resumeEclate() {
            vendus.export(page(ligne("DOLIPRANE")), resume(), periode(null, null));

            Map<String, Object> modele = vendus.getParameters();

            assertThat(modele.get("totalProducts")).isEqualTo(12L);
            assertThat(modele.get("quantitySold")).isEqualTo(340);
            assertThat(modele.get("quantityAvoir")).isEqualTo(5);
            assertThat(modele.get("totalSalesAmount")).isEqualTo(4_500_000L);
            assertThat(modele.get("totalPurchaseAmount")).isEqualTo(2_700_000L);
            assertThat(modele.get("totalStock")).isEqualTo(890L);
        }

        @Test
        @DisplayName("les lignes de la page et les mentions légales accompagnent le document")
        void modeleDuDocument() {
            vendus.export(page(ligne("DOLIPRANE"), ligne("EFFERALGAN")), resume(), periode(null, null));

            Map<String, Object> modele = vendus.getParameters();

            assertThat((List<?>) modele.get("items")).hasSize(2);
            assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
        }
    }

    // ===== produits invendus =====

    @Nested
    @DisplayName("Produits invendus")
    class ProduitsInvendus {

        @Test
        @DisplayName("le titre distingue les invendus et porte la période")
        void titreAvecPeriode() {
            invendus.export(page(ligne("DORMANT")), resume(), periode(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 31)));

            assertThat(invendus.getParameters().get("reportTitle"))
                .isEqualTo("Récapitulatif des Produits Invendus du 05/01/2026 au 31/03/2026");
        }

        /**
         * L'invendu n'a ni quantité vendue ni avoir : son résumé ne porte que ce qui dort en stock,
         * et le gabarit ne reçoit donc pas ces deux variables.
         */
        @Test
        @DisplayName("le résumé des invendus ne porte que le stock immobilisé")
        void resumeDuStockDormant() {
            invendus.export(page(ligne("DORMANT")), resume(), periode(null, null));

            Map<String, Object> modele = invendus.getParameters();

            assertThat(modele.get("totalProducts")).isEqualTo(12L);
            assertThat(modele.get("totalStock")).isEqualTo(890L);
            assertThat(modele.get("totalSalesAmount")).isEqualTo(4_500_000L);
            assertThat(modele).doesNotContainKeys("quantitySold", "quantityAvoir");
        }

        @Test
        @DisplayName("un récapitulatif sans ligne s'imprime tout de même")
        void recapitulatifVide() {
            invendus.export(page(), resume(), periode(null, null));

            assertThat((List<?>) invendus.getParameters().get("items")).isEmpty();
        }
    }

    // ===== fabriques =====

    private static Page<RecapProduitVendu> page(RecapProduitVendu... lignes) {
        return new PageImpl<>(List.of(lignes));
    }

    private static RecapProduitVendu ligne(String libelle) {
        return new RecapProduitVendu(1, libelle, "CIP", "EAN", "COMPTOIR", 10, 0, 100_000, 60_000, 25);
    }

    private static RecapProduitVenduSummary resume() {
        return new RecapProduitVenduSummary(12L, 340, 5, 4_500_000L, 2_700_000L, 890L);
    }

    private static RecapProduitVenduRequestParam periode(LocalDate debut, LocalDate fin) {
        return new RecapProduitVenduRequestParam(
            debut, fin, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false
        );
    }
}
