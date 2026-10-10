package com.kobe.warehouse.service.report.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La suggestion de commande s'imprime pour être relue avant d'être envoyée au grossiste : c'est le
 * dernier point où l'on peut retirer une ligne ou corriger une quantité. Son <b>montant total</b>
 * est l'engagement financier que l'officine s'apprête à prendre, et il est calculé ici, ligne à
 * ligne, au prix d'achat.
 *
 * <p>Ce produit quantité × prix se fait sur des entiers dont le produit peut dépasser la capacité
 * d'un {@code int} — un millier d'unités à cinquante mille francs y suffit. Le calcul doit donc
 * passer en {@code long} avant la multiplication, et non après.
 */
@DisplayName("SuggestionPdfReportService — bon de suggestion de commande")
class SuggestionPdfReportServiceTest {

    private SuggestionPdfReportService service;

    @BeforeEach
    void setUp() {
        service = new SuggestionPdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits()
        );
    }

    @Test
    @DisplayName("le total des quantités additionne les lignes")
    void totalDesQuantites() {
        Map<String, Object> modele = modele(ligne(10, 6_000), ligne(25, 4_000), ligne(5, 100_000));

        assertThat(modele.get("totalQuantite")).isEqualTo(40L);
    }

    @Test
    @DisplayName("le montant total valorise chaque ligne au prix d'achat")
    void montantTotal() {
        Map<String, Object> modele = modele(ligne(10, 6_000), ligne(25, 4_000));

        assertThat(modele.get("totalMontant")).isEqualTo(160_000L); // 60 000 + 100 000
    }

    /**
     * Mille unités à cinquante mille francs font cinquante millions, et deux lignes de ce calibre
     * dépassent déjà ce qu'un {@code int} peut porter. Le montant d'une commande de gros ne doit pas
     * repasser en négatif au moment de l'impression.
     */
    @Test
    @DisplayName("un montant dépassant la capacité d'un entier reste juste")
    void montantHorsCapaciteEntiere() {
        Map<String, Object> modele = modele(ligne(50_000, 50_000), ligne(50_000, 50_000));

        assertThat(modele.get("totalMontant")).isEqualTo(5_000_000_000L);
    }

    @Test
    @DisplayName("le titre et la référence nomment la suggestion et son fournisseur")
    void identiteDeLaSuggestion() {
        Map<String, Object> modele = modele(ligne(10, 6_000));

        assertThat(modele.get("suggestionReference")).isEqualTo("SUG-2026-001");
        assertThat(modele.get("fournisseurLibelle")).isEqualTo("LABOREX");
        assertThat(modele.get("reportTitle")).isEqualTo("Suggestion de commande — LABOREX");
        assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
    }

    @Test
    @DisplayName("une suggestion vide s'imprime avec des totaux nuls")
    void suggestionVide() {
        Map<String, Object> modele = modele();

        assertThat(modele.get("totalQuantite")).isEqualTo(0L);
        assertThat(modele.get("totalMontant")).isEqualTo(0L);
    }

    // ===== fabriques =====

    private Map<String, Object> modele(SuggestionLineDTO... lignes) {
        service.export("SUG-2026-001", "LABOREX", List.of(lignes));
        return service.getParameters();
    }

    private static SuggestionLineDTO ligne(int quantite, int prixAchat) {
        return new SuggestionLineDTO(
            1,
            quantite,
            null,
            null,
            "PRODUIT",
            "CIP",
            "EAN",
            1,
            1,
            0,
            null,
            prixAchat,
            prixAchat * 2,
            Map.of(),
            null,
            null,
            null,
            false,
            1,
            0
        );
    }
}
