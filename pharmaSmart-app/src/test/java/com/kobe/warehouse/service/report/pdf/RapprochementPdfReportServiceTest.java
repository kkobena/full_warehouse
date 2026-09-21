package com.kobe.warehouse.service.report.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.service.facturation.dto.EtatRapprochementDto;
import com.kobe.warehouse.service.facturation.dto.RapprochementParams;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * L'état de rapprochement confronte ce qui a été facturé aux tiers payants à ce qu'ils ont
 * réellement réglé. L'écart est la question : c'est ce que l'officine réclame, ou ce qu'elle a
 * encaissé sans savoir à quoi le rattacher.
 *
 * <p>Les trois totaux du document sont recalculés ici, en {@code BigDecimal} — des montants
 * comptables, où un arrondi introduit un écart qu'on passera ensuite du temps à chercher. Le
 * libellé de période a quatre formes, dont une vide : un état non borné ne doit pas afficher
 * « du null au null » sur un document de rapprochement comptable.
 */
@DisplayName("RapprochementPdfReportService — état de rapprochement")
class RapprochementPdfReportServiceTest {

    private RapprochementPdfReportService service;

    @BeforeEach
    void setUp() {
        service = new RapprochementPdfReportService(
            PdfReportTestSupport.proprietes(),
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits()
        );
    }

    // ===== totaux =====

    @Nested
    @DisplayName("Totaux")
    class Totaux {

        @Test
        @DisplayName("les trois totaux additionnent les états de chaque tiers payant")
        void troisTotaux() {
            Map<String, Object> modele = modele(
                etat("MUGEFCI", "1000000.50", "900000.25", "100000.25"),
                etat("CNPS", "500000.50", "400000.75", "99999.75")
            );

            assertThat(modele.get("totalFacture")).isEqualTo(new BigDecimal("1500001.00"));
            assertThat(modele.get("totalRegle")).isEqualTo(new BigDecimal("1300001.00"));
            assertThat(modele.get("ecartTotal")).isEqualTo(new BigDecimal("200000.00"));
        }

        /** Un tiers payant qui a trop réglé produit un écart négatif : il ne doit pas être masqué. */
        @Test
        @DisplayName("un écart négatif se compense avec les autres, sans être escamoté")
        void ecartNegatif() {
            Map<String, Object> modele = modele(
                etat("MUGEFCI", "100000", "90000", "10000"),
                etat("CNPS", "100000", "115000", "-15000")
            );

            assertThat(modele.get("ecartTotal")).isEqualTo(new BigDecimal("-5000"));
        }

        @Test
        @DisplayName("un état sans tiers payant totalise zéro")
        void etatVide() {
            Map<String, Object> modele = modele();

            assertThat(modele.get("totalFacture")).isEqualTo(BigDecimal.ZERO);
            assertThat(modele.get("totalRegle")).isEqualTo(BigDecimal.ZERO);
            assertThat(modele.get("ecartTotal")).isEqualTo(BigDecimal.ZERO);
        }
    }

    // ===== libellé de période =====

    @Nested
    @DisplayName("Libellé de période")
    class LibelleDePeriode {

        @Test
        @DisplayName("une période bornée se lit « du … au … »")
        void periodeBornee() {
            assertThat(modeleAvecPeriode(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 31)).get("periode"))
                .isEqualTo("du 05/01/2026 au 31/03/2026");
        }

        @Test
        @DisplayName("une période ouverte à droite se lit « à partir du … »")
        void periodeOuverteADroite() {
            assertThat(modeleAvecPeriode(LocalDate.of(2026, 1, 5), null).get("periode")).isEqualTo("à partir du 05/01/2026");
        }

        @Test
        @DisplayName("une période ouverte à gauche se lit « jusqu'au … »")
        void periodeOuverteAGauche() {
            assertThat(modeleAvecPeriode(null, LocalDate.of(2026, 3, 31)).get("periode")).isEqualTo("jusqu'au 31/03/2026");
        }

        @Test
        @DisplayName("sans borne, le libellé reste vide plutôt que d'annoncer des dates nulles")
        void aucunePeriode() {
            assertThat(modeleAvecPeriode(null, null).get("periode")).isEqualTo("");
        }
    }

    // ===== modèle et isolement =====

    @Nested
    @DisplayName("Modèle et isolement")
    class ModeleEtIsolement {

        @Test
        @DisplayName("les états et les mentions légales accompagnent le document")
        void modeleDuDocument() {
            Map<String, Object> modele = modele(etat("MUGEFCI", "100000", "90000", "10000"));

            assertThat((List<?>) modele.get("etats")).hasSize(1);
            assertThat(modele.get("reportTitle")).isEqualTo("État de Rapprochement");
            assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
        }

        @Test
        @DisplayName("deux états successifs ne cumulent pas leurs totaux")
        void deuxEtatsSuccessifs() {
            modele(etat("MUGEFCI", "100000", "90000", "10000"));

            Map<String, Object> second = modele(etat("CNPS", "50000", "50000", "0"));

            assertThat(second.get("totalFacture")).isEqualTo(new BigDecimal("50000"));
            assertThat((List<?>) second.get("etats")).hasSize(1);
        }
    }

    // ===== fabriques =====

    private Map<String, Object> modele(EtatRapprochementDto... etats) {
        service.export(List.of(etats), new RapprochementParams(null, null, null, null));
        return service.getParameters();
    }

    private Map<String, Object> modeleAvecPeriode(LocalDate debut, LocalDate fin) {
        service.export(List.of(), new RapprochementParams(null, debut, fin, null));
        return service.getParameters();
    }

    private static EtatRapprochementDto etat(String tiersPayant, String facture, String regle, String ecart) {
        return new EtatRapprochementDto(
            tiersPayant,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
            new BigDecimal(facture),
            new BigDecimal(regle),
            new BigDecimal(ecart),
            List.of()
        );
    }
}
