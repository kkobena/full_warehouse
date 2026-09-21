package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.report.DemarqueByMotifDTO;
import com.kobe.warehouse.service.dto.report.DemarqueKpiDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La démarque est ce qui a disparu du stock sans être vendu : périmés détruits, casse, vol. C'est
 * une perte sèche, valorisée au prix d'achat — ce que la marchandise a coûté, non ce qu'elle aurait
 * rapporté. Le rapport la chiffre et la ventile par motif, parce que les remèdes diffèrent : un taux
 * de périmés élevé se traite en révisant les commandes, la casse en revoyant le rangement, le vol
 * en revoyant l'agencement du comptoir.
 *
 * <p>Le périmètre est ce qui se vérifie le moins à l'œil nu. Seuls les bons <b>clôturés</b> comptent
 * — un ajustement resté en brouillon n'a rien retiré du stock —, et seules les lignes <b>sortantes</b>
 * — un ajustement entrant est une correction à la hausse, l'inverse d'une perte. Compter les unes
 * pour les autres transformerait une régularisation en sinistre.
 */
@DisplayName("DemarqueReportService — pertes de stock par motif")
class DemarqueReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    // ===== indicateurs =====

    @Nested
    @DisplayName("Indicateurs")
    class Indicateurs {

        @Test
        @DisplayName("totalise les bons, les quantités perdues et leur valeur d'achat")
        void totaux() {
            MotifAjustement casse = motif("CASSE " + unique(""));
            demarque(hier(), produitCoutant("DOLIPRANE", 6_000), 3, casse);
            demarque(hier(), produitCoutant("EFFERALGAN", 4_000), 5, casse);
            viderLeCache();

            DemarqueKpiDTO kpi = services.demarqueReportService.getKpi(ilYA(7), LocalDate.now());

            assertThat(kpi.nbAjustements()).isEqualTo(2);
            assertThat(kpi.totalQtyPerdue()).isEqualTo(8L);
            assertThat(kpi.valeurPerdue()).isEqualTo(38_000L); // 3×6 000 + 5×4 000
        }

        /** La sortie est enregistrée en négatif ; la perte, elle, se compte en positif. */
        @Test
        @DisplayName("la quantité perdue est rendue en valeur absolue")
        void quantiteEnValeurAbsolue() {
            demarque(hier(), produitCoutant("DOLIPRANE", 6_000), 10, motif("CASSE " + unique("")));
            viderLeCache();

            DemarqueKpiDTO kpi = services.demarqueReportService.getKpi(ilYA(7), LocalDate.now());

            assertThat(kpi.totalQtyPerdue()).isEqualTo(10L);
            assertThat(kpi.valeurPerdue()).isPositive();
        }

        @Test
        @DisplayName("la période borne les bons pris en compte")
        void filtreParPeriode() {
            MotifAjustement casse = motif("CASSE " + unique(""));
            demarque(ilYA(30).atStartOfDay(), produitCoutant("ANCIEN", 6_000), 10, casse);
            demarque(hier(), produitCoutant("RECENT", 6_000), 3, casse);
            viderLeCache();

            DemarqueKpiDTO kpi = services.demarqueReportService.getKpi(ilYA(7), LocalDate.now());

            assertThat(kpi.nbAjustements()).isEqualTo(1);
            assertThat(kpi.totalQtyPerdue()).isEqualTo(3L);
        }

        @Test
        @DisplayName("les bornes de la période sont incluses")
        void bornesIncluses() {
            MotifAjustement casse = motif("CASSE " + unique(""));
            demarque(ilYA(7).atTime(23, 59), produitCoutant("PREMIER JOUR", 6_000), 1, casse);
            demarque(LocalDate.now().atTime(0, 1), produitCoutant("DERNIER JOUR", 6_000), 1, casse);
            viderLeCache();

            assertThat(services.demarqueReportService.getKpi(ilYA(7), LocalDate.now()).nbAjustements()).isEqualTo(2);
        }

        @Test
        @DisplayName("base vierge : aucune perte")
        void baseVierge() {
            DemarqueKpiDTO kpi = services.demarqueReportService.getKpi(ilYA(30), LocalDate.now());

            assertThat(kpi.nbAjustements()).isZero();
            assertThat(kpi.totalQtyPerdue()).isZero();
            assertThat(kpi.valeurPerdue()).isZero();
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        /** Un bon en brouillon n'a rien retiré du stock : le compter inventerait une perte. */
        @Test
        @DisplayName("un bon en brouillon n'est pas une perte")
        void bonEnBrouillonExclu() {
            ajustement(
                hier(),
                produitCoutant("DOLIPRANE", 6_000),
                10,
                motif("CASSE " + unique("")),
                AjustType.AJUSTEMENT_OUT,
                AjustementStatut.PENDING
            );
            viderLeCache();

            assertThat(services.demarqueReportService.getKpi(ilYA(7), LocalDate.now()).nbAjustements()).isZero();
        }

        /** Un ajustement entrant corrige le stock à la hausse : c'est l'inverse d'une démarque. */
        @Test
        @DisplayName("un ajustement entrant n'est pas une perte")
        void ajustementEntrantExclu() {
            ajustement(
                hier(),
                produitCoutant("DOLIPRANE", 6_000),
                10,
                motif("REGULARISATION " + unique("")),
                AjustType.AJUSTEMENT_IN,
                AjustementStatut.CLOSED
            );
            viderLeCache();

            assertThat(services.demarqueReportService.getKpi(ilYA(7), LocalDate.now()).nbAjustements()).isZero();
            assertThat(services.demarqueReportService.getByMotif(ilYA(7), LocalDate.now())).isEmpty();
        }
    }

    // ===== ventilation par motif =====

    @Nested
    @DisplayName("Ventilation par motif")
    class ParMotif {

        /** Les motifs les plus coûteux en tête : c'est par là qu'on commence à corriger. */
        @Test
        @DisplayName("les motifs sont classés par valeur perdue décroissante")
        void ordreParValeur() {
            MotifAjustement casse = motif("CASSE " + unique(""));
            MotifAjustement perime = motif("PERIME " + unique(""));

            demarque(hier(), produitCoutant("PETIT", 1_000), 2, casse);
            demarque(hier(), produitCoutant("GROS", 50_000), 4, perime);
            viderLeCache();

            List<DemarqueByMotifDTO> ventilation = services.demarqueReportService.getByMotif(ilYA(7), LocalDate.now());

            assertThat(ventilation).extracting(DemarqueByMotifDTO::motif).containsExactly(perime.getLibelle(), casse.getLibelle());
            assertThat(ventilation.getFirst().valeur()).isEqualTo(200_000L);
            assertThat(ventilation.get(1).valeur()).isEqualTo(2_000L);
        }

        @Test
        @DisplayName("les pertes d'un même motif sont regroupées")
        void regroupementParMotif() {
            MotifAjustement casse = motif("CASSE " + unique(""));
            demarque(hier(), produitCoutant("A", 6_000), 3, casse);
            demarque(hier(), produitCoutant("B", 6_000), 7, casse);
            viderLeCache();

            List<DemarqueByMotifDTO> ventilation = services.demarqueReportService.getByMotif(ilYA(7), LocalDate.now());

            assertThat(ventilation).hasSize(1);
            assertThat(ventilation.getFirst().nbLignes()).isEqualTo(2);
            assertThat(ventilation.getFirst().totalQty()).isEqualTo(10L);
            assertThat(ventilation.getFirst().valeur()).isEqualTo(60_000L);
        }

        /** Une perte sans motif reste une perte : elle ne doit pas disparaître du rapport. */
        @Test
        @DisplayName("les pertes sans motif sont regroupées sous une étiquette explicite")
        void pertesSansMotif() {
            demarque(hier(), produitCoutant("SANS MOTIF A", 6_000), 3, null);
            demarque(hier(), produitCoutant("SANS MOTIF B", 6_000), 2, null);
            viderLeCache();

            List<DemarqueByMotifDTO> ventilation = services.demarqueReportService.getByMotif(ilYA(7), LocalDate.now());

            assertThat(ventilation).hasSize(1);
            assertThat(ventilation.getFirst().motif()).isEqualTo("Sans motif");
            assertThat(ventilation.getFirst().totalQty()).isEqualTo(5L);
        }

        @Test
        @DisplayName("la ventilation s'accorde avec le total des indicateurs")
        void coherenceAvecLesIndicateurs() {
            demarque(hier(), produitCoutant("A", 6_000), 3, motif("CASSE " + unique("")));
            demarque(hier(), produitCoutant("B", 4_000), 5, motif("PERIME " + unique("")));
            viderLeCache();

            DemarqueKpiDTO kpi = services.demarqueReportService.getKpi(ilYA(7), LocalDate.now());
            List<DemarqueByMotifDTO> ventilation = services.demarqueReportService.getByMotif(ilYA(7), LocalDate.now());

            assertThat(ventilation.stream().mapToLong(DemarqueByMotifDTO::valeur).sum()).isEqualTo(kpi.valeurPerdue());
            assertThat(ventilation.stream().mapToLong(DemarqueByMotifDTO::totalQty).sum()).isEqualTo(kpi.totalQtyPerdue());
        }

        @Test
        @DisplayName("base vierge : aucune ventilation")
        void baseVierge() {
            assertThat(services.demarqueReportService.getByMotif(ilYA(30), LocalDate.now())).isEmpty();
        }
    }

    // ===== fabriques locales =====

    private static LocalDate ilYA(int jours) {
        return LocalDate.now().minusDays(jours);
    }

    private static java.time.LocalDateTime hier() {
        return LocalDate.now().minusDays(1).atTime(10, 0);
    }

    /** Un produit stocké, dont le coût d'achat est ce qui valorise la perte. */
    private Produit produitCoutant(String libelle, int coutAchat) {
        Produit produit = produit(libelle, TypeProduit.PACKAGE, coutAchat * 10 / 6, 5);
        produit.setCostAmount(coutAchat);
        produit.setItemCostAmount(coutAchat);
        em.flush();
        stock(produit, 100);
        return produit;
    }
}
