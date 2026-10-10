package com.kobe.warehouse.service.report.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartitionStockProduitDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * L'historique des répartitions retrace les transferts de stock entre la réserve et le rayon. Il
 * s'imprime pour justifier un écart d'inventaire : c'est la trace de ce qui a bougé sans être
 * vendu.
 *
 * <p>Deux choses le rendent exploitable : le <b>nombre de mouvements</b>, qui doit correspondre aux
 * lignes imprimées — un document qui annonce trente mouvements et en liste vingt-huit ne prouve
 * rien —, et la <b>période</b> dans son titre, sans laquelle il ne se rattache à aucun inventaire.
 */
@DisplayName("RepartitionStockPdfReportService — historique des répartitions de stock")
class RepartitionStockPdfReportServiceTest {

    private RepartitionStockPdfReportService service;

    @BeforeEach
    void setUp() {
        service = new RepartitionStockPdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits()
        );
    }

    @Test
    @DisplayName("le nombre de mouvements correspond aux lignes imprimées")
    void nombreDeMouvements() {
        Map<String, Object> modele = modele(recherche(null, null), mouvement("DOLIPRANE"), mouvement("EFFERALGAN"), mouvement("ZOVIRAX"));

        assertThat(modele.get("totalMouvements")).isEqualTo(3);
        assertThat((List<?>) modele.get("repartitions")).hasSize(3);
    }

    @Test
    @DisplayName("le titre porte la période analysée")
    void titreAvecPeriode() {
        Map<String, Object> modele = modele(recherche(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 31)), mouvement("DOLIPRANE"));

        assertThat(modele.get("reportTitle")).isEqualTo("Historique des Répartitions de Stock du 05-01-2026 au 31-03-2026");
    }

    /** Sans bornes, l'intitulé reste seul — un historique non borné ne se prétend pas daté. */
    @Test
    @DisplayName("sans période, le titre se réduit à son intitulé")
    void titreSansPeriode() {
        Map<String, Object> modele = modele(recherche(null, null), mouvement("DOLIPRANE"));

        assertThat(modele.get("reportTitle")).isEqualTo("Historique des Répartitions de Stock");
    }

    @Test
    @DisplayName("une période incomplète ne produit pas de libellé bancal")
    void periodeIncomplete() {
        assertThat(modele(recherche(LocalDate.of(2026, 1, 5), null), mouvement("A")).get("reportTitle"))
            .isEqualTo("Historique des Répartitions de Stock");
        assertThat(modele(recherche(null, LocalDate.of(2026, 3, 31)), mouvement("A")).get("reportTitle"))
            .isEqualTo("Historique des Répartitions de Stock");
    }

    @Test
    @DisplayName("les mentions légales accompagnent le document")
    void mentionsLegales() {
        Map<String, Object> modele = modele(recherche(null, null), mouvement("DOLIPRANE"));

        assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
    }

    @Test
    @DisplayName("un historique sans mouvement s'imprime avec un compteur nul")
    void historiqueVide() {
        Map<String, Object> modele = modele(recherche(null, null));

        assertThat(modele.get("totalMouvements")).isEqualTo(0);
        assertThat((List<?>) modele.get("repartitions")).isEmpty();
    }

    // ===== fabriques =====

    private Map<String, Object> modele(RepartionSearchQueryDto recherche, RepartitionStockProduitDto... mouvements) {
        service.export(List.of(mouvements), recherche);
        return service.getParameters();
    }

    private static RepartionSearchQueryDto recherche(LocalDate debut, LocalDate fin) {
        return new RepartionSearchQueryDto(null, null, null, debut, fin, null, null);
    }

    private static RepartitionStockProduitDto mouvement(String produit) {
        RepartitionStockProduitDto dto = new RepartitionStockProduitDto();
        dto.setProduitName(produit);
        dto.setMvtQty(10);
        return dto;
    }
}
