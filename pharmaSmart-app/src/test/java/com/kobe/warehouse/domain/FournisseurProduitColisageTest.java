package com.kobe.warehouse.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests unitaires pour {@link FournisseurProduit#appliquerColisage(int)}.
 * <p>
 * Règle métier : la quantité commandée doit être un multiple de {@code qteColis}
 * ET supérieure ou égale à {@code qteMinimaleCommande}.
 * <p>
 * Formule : {@code max(1, max(qteMin, ceil(qty / colis) * colis))}
 */
class FournisseurProduitColisageTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FournisseurProduit fp(Integer colis, Integer qteMin) {
        FournisseurProduit fp = new FournisseurProduit();
        fp.setQteColis(colis);
        fp.setQteMinimaleCommande(qteMin);
        return fp;
    }

    // ── Aucune contrainte de colisage ─────────────────────────────────────────

    @Test
    @DisplayName("Colisage=1 (défaut) — pas d'arrondi")
    void appliquerColisage_colisUn_pasDarrondi() {
        assertThat(fp(1, 0).appliquerColisage(5)).isEqualTo(5);
        assertThat(fp(1, 0).appliquerColisage(1)).isEqualTo(1);
        assertThat(fp(1, 0).appliquerColisage(100)).isEqualTo(100);
    }

    @Test
    @DisplayName("Colisage null — traite comme 1 (pas d'arrondi)")
    void appliquerColisage_colisNull_traiteComme1() {
        assertThat(fp(null, null).appliquerColisage(5)).isEqualTo(5);
        assertThat(fp(null, null).appliquerColisage(7)).isEqualTo(7);
    }

    // ── Arrondi au multiple supérieur ─────────────────────────────────────────

    @ParameterizedTest(name = "qty={0}, colis={1} -> attendu={2}")
    @CsvSource({
        "5,  3,  6",
        "6,  3,  6",
        "7,  3,  9",
        "1,  5,  5",
        "4,  5,  5",
        "5,  5,  5",
        "6,  5,  10",
        "10, 6,  12",
        "12, 6,  12",
        "13, 6,  18",
    })
    @DisplayName("Arrondi au multiple superieur du colisage")
    void appliquerColisage_arrondiMultipleSuperieur(int qty, int colis, int attendu) {
        assertThat(fp(colis, 0).appliquerColisage(qty)).isEqualTo(attendu);
    }

    // ── Quantité minimale de commande ─────────────────────────────────────────

    @Test
    @DisplayName("Qte calculee < qteMinimale -> qteMinimale appliquee")
    void appliquerColisage_qteMinimaleAppliquee() {
        // qty=5, min=10, colis=1 → max(5, 10) = 10
        assertThat(fp(1, 10).appliquerColisage(5)).isEqualTo(10);
        // qty=5, min=6, colis=1 → max(5, 6) = 6
        assertThat(fp(1, 6).appliquerColisage(5)).isEqualTo(6);
    }

    @Test
    @DisplayName("Combinaison colisage + qteMinimale : la valeur la plus haute gagne")
    void appliquerColisage_combinaisonColisageEtMinimale() {
        // qty=5, colis=3 → arrondi=6; min=4 → max(6,4) = 6 (colisage l'emporte)
        assertThat(fp(3, 4).appliquerColisage(5)).isEqualTo(6);
        // qty=5, colis=3 → arrondi=6; min=8 → max(6,8) = 8 (min l'emporte) mais 8 n'est pas un multiple de 3
        // → on retourne max(arrondi, min) = max(6,8) = 8
        assertThat(fp(3, 8).appliquerColisage(5)).isEqualTo(8);
        // qty=8, colis=5 → arrondi=10; min=6 → max(10,6) = 10
        assertThat(fp(5, 6).appliquerColisage(8)).isEqualTo(10);
    }

    // ── Cas limites ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("qty=0 → résultat minimum 1 (jamais 0)")
    void appliquerColisage_qtyZero_retourneAuMoins1() {
        assertThat(fp(1, 0).appliquerColisage(0)).isEqualTo(1);
        assertThat(fp(3, 0).appliquerColisage(0)).isEqualTo(1);
        assertThat(fp(null, null).appliquerColisage(0)).isEqualTo(1);
    }

    @Test
    @DisplayName("qty négatif → résultat minimum 1")
    void appliquerColisage_qtyNegatif_retourneAuMoins1() {
        assertThat(fp(1, 0).appliquerColisage(-5)).isEqualTo(1);
        assertThat(fp(3, 0).appliquerColisage(-3)).isEqualTo(1);
    }

    @Test
    @DisplayName("qteMinimale=0 ou null → pas de minimum imposé")
    void appliquerColisage_qteMinimaleZeroOuNull_pasDEffet() {
        assertThat(fp(1, 0).appliquerColisage(5)).isEqualTo(5);
        assertThat(fp(1, null).appliquerColisage(5)).isEqualTo(5);
    }

    @Test
    @DisplayName("Grande quantité — performance et précision")
    void appliquerColisage_grandeQuantite() {
        // qty=1000, colis=6 → ceil(1000/6)*6 = 167*6 = 1002
        assertThat(fp(6, 0).appliquerColisage(1000)).isEqualTo(1002);
        // qty=1200, colis=6 → exact multiple
        assertThat(fp(6, 0).appliquerColisage(1200)).isEqualTo(1200);
    }
}

