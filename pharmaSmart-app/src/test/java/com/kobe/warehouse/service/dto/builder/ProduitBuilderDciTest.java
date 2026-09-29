package com.kobe.warehouse.service.dto.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.ProduitDci;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.ProduitDciDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Molécules d'un produit : modèle, écriture et lecture — PLAN-PRODUIT-DCI-N-N. */
class ProduitBuilderDciTest {

    private static Dci dci(int id, String libelle) {
        Dci dci = new Dci();
        dci.setId(id);
        dci.setCode("C" + id);
        dci.setLibelle(libelle);
        return dci;
    }

    private static List<Integer> ids(Produit produit) {
        return produit.getDcis().stream().map(Dci::getId).toList();
    }

    private static ProduitDciDTO molecule(int id) {
        return new ProduitDciDTO(id, null, null, null, null, null);
    }

    @Nested
    @DisplayName("Produit.remplacerDcis")
    class Remplacer {

        @Test
        void ordreDonneEtPrincipaleAlignee() {
            Produit produit = new Produit();
            produit.remplacerDcis(List.of(dci(1, "A"), dci(2, "B")));

            assertThat(ids(produit)).containsExactly(1, 2);
            assertThat(produit.getProduitDcis()).extracting(ProduitDci::getRang).containsExactly(1, 2);
            assertThat(produit.getDci().getId()).isEqualTo(1);
        }

        @Test
        void lesLiensConservesSontMisAJourSurPlace() {
            Produit produit = new Produit();
            produit.remplacerDcis(List.of(dci(1, "A"), dci(2, "B")));
            ProduitDci lienB = produit.getProduitDcis().get(1);

            produit.remplacerDcis(List.of(dci(2, "B"), dci(3, "C")));

            assertThat(ids(produit)).containsExactly(2, 3);
            assertThat(produit.getProduitDcis().getFirst()).as("même instance : pas de suppression-recréation").isSameAs(lienB);
            assertThat(lienB.getRang()).isEqualTo(1);
        }

        @Test
        void doublonsEtNulsIgnores() {
            Produit produit = new Produit();
            produit.remplacerDcis(java.util.Arrays.asList(dci(1, "A"), null, dci(1, "A"), dci(2, "B")));

            assertThat(ids(produit)).containsExactly(1, 2);
        }

        @Test
        void listeVideRetireTout() {
            Produit produit = new Produit();
            produit.remplacerDcis(List.of(dci(1, "A")));
            produit.remplacerDcis(List.of());

            assertThat(produit.getProduitDcis()).isEmpty();
            assertThat(produit.getDci()).isNull();
        }
    }

    @Nested
    @DisplayName("ProduitBuilder.appliquerDcis")
    class Appliquer {

        @Test
        void laListeFaitFoi() {
            Produit produit = new Produit();
            produit.remplacerDcis(List.of(dci(9, "Z")));

            ProduitBuilder.appliquerDcis(produit, new ProduitDTO().setDcis(List.of(molecule(1), molecule(2))));

            assertThat(ids(produit)).containsExactly(1, 2);
        }

        @Test
        void unAncienClientRemplaceUnProduitAUneMolecule() {
            Produit produit = new Produit();
            produit.remplacerDcis(List.of(dci(9, "Z")));

            ProduitBuilder.appliquerDcis(produit, new ProduitDTO().setDciId(4));

            assertThat(ids(produit)).containsExactly(4);
        }

        @Test
        void unAncienClientNeChangeQueLaPrincipaleDUneAssociation() {
            Produit produit = new Produit();
            produit.remplacerDcis(List.of(dci(1, "A"), dci(2, "B")));

            ProduitBuilder.appliquerDcis(produit, new ProduitDTO().setDciId(2));
            assertThat(ids(produit)).containsExactly(2, 1);

            ProduitBuilder.appliquerDcis(produit, new ProduitDTO().setDciId(7));
            assertThat(ids(produit)).containsExactly(7, 2, 1);
        }

        @Test
        void unAncienClientSansDciNEffacePasUneAssociation() {
            Produit produit = new Produit();
            produit.remplacerDcis(List.of(dci(1, "A"), dci(2, "B")));

            ProduitBuilder.appliquerDcis(produit, new ProduitDTO());

            assertThat(ids(produit)).containsExactly(1, 2);
        }

        @Test
        void unAncienClientSansDciVideUnProduitAUneMolecule() {
            Produit produit = new Produit();
            produit.remplacerDcis(List.of(dci(1, "A")));

            ProduitBuilder.appliquerDcis(produit, new ProduitDTO());

            assertThat(produit.getProduitDcis()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ProduitBuilder.updateDci (lecture)")
    class Lecture {

        @Test
        void champsHistoriquesCalculesDepuisLaListe() {
            Produit produit = new Produit();
            produit.remplacerDcis(List.of(dci(1, "PARACETAMOL"), dci(2, "CODEINE")));
            ProduitDTO dto = new ProduitDTO();

            ProduitBuilder.updateDci(dto, produit);

            assertThat(dto.getDcis()).extracting(ProduitDciDTO::dciId).containsExactly(1, 2);
            assertThat(dto.getDciId()).isEqualTo(1);
            assertThat(dto.getDciCode()).isEqualTo("C1");
            assertThat(dto.getDciLibelle()).isEqualTo("PARACETAMOL + CODEINE");
        }

        @Test
        void sansMoleculeLaListeEstVide() {
            ProduitDTO dto = new ProduitDTO();

            ProduitBuilder.updateDci(dto, new Produit());

            assertThat(dto.getDcis()).isEmpty();
            assertThat(dto.getDciId()).isNull();
        }
    }
}
