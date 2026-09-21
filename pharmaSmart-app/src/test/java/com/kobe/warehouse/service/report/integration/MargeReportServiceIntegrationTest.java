package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.report.MargeDTO;
import com.kobe.warehouse.service.dto.report.MargeSummaryDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Le rapport de marges répond à la question qui décide du prix de vente : sur quoi gagne-t-on
 * vraiment, et sur quoi travaille-t-on à perte ? Un produit qui tourne beaucoup à marge nulle
 * occupe du rayon et du fonds de roulement sans rien rapporter — c'est précisément ce que le seuil
 * de marge insuffisante sert à faire remonter.
 *
 * <p>Tout se calcule dans {@code mv_marge_produit}. La <b>marge brute</b> y est la différence entre
 * ce qui a été encaissé et ce que la marchandise a coûté, ligne de vente par ligne de vente — donc
 * au coût réel du moment de la vente, pas au prix d'achat courant. Le <b>taux</b> est cette marge
 * rapportée au chiffre d'affaires ; il a déjà été cassé ailleurs par une division entière, et il
 * est ici protégé par des casts explicites.
 *
 * <p>Le reste tient dans la manière d'interroger : un <b>tri</b> reçu de l'écran qui devient une
 * colonne SQL — et dont la liste blanche est la seule protection contre l'injection —, une
 * <b>pagination</b> qui doit rapporter le bon total, et un <b>export</b> qui doit imprimer ce que
 * l'utilisateur vient de lire, pas autre chose.
 */
@DisplayName("MargeReportService — marges produit sur mv_marge_produit")
class MargeReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    private static final String VUE = "mv_marge_produit";

    // ===== calcul de la marge =====

    @Nested
    @DisplayName("Calcul de la marge")
    class CalculDeLaMarge {

        @Test
        @DisplayName("la marge brute est l'écart entre l'encaissé et le coût de la marchandise")
        void margeBrute() {
            venduAvecMarge("DOLIPRANE", 10_000, 6_000, 10);
            rafraichir(VUE);

            MargeDTO ligne = marges().getFirst();

            assertThat(ligne.caTotal()).isEqualTo(100_000L);
            assertThat(ligne.coutAchatTotal()).isEqualTo(60_000L);
            assertThat(ligne.margeBrute()).isEqualTo(40_000L);
        }

        /**
         * Le taux est une division entre deux entiers : sans cast explicite, elle renvoie zéro et le
         * rapport affiche une marge nulle à côté d'une marge brute de plusieurs millions.
         */
        @Test
        @DisplayName("le taux de marge est un pourcentage réel, et non zéro par division entière")
        void tauxDeMarge() {
            venduAvecMarge("DOLIPRANE", 10_000, 6_000, 10);
            rafraichir(VUE);

            assertThat(marges().getFirst().tauxMargePct()).isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("les prix moyens rapportent les montants aux quantités vendues")
        void prixMoyens() {
            venduAvecMarge("DOLIPRANE", 10_000, 6_000, 4);
            rafraichir(VUE);

            MargeDTO ligne = marges().getFirst();

            assertThat(ligne.prixVenteMoyen()).isEqualTo(10_000);
            assertThat(ligne.prixAchatMoyen()).isEqualTo(6_000);
            assertThat(ligne.qteVendue()).isEqualTo(4);
            assertThat(ligne.nbVentes()).isEqualTo(1);
        }

        /** Vendre à perte est possible, et c'est exactement ce que le rapport doit montrer. */
        @Test
        @DisplayName("une marge négative est rendue telle quelle")
        void margeNegative() {
            venduAvecMarge("A PERTE", 5_000, 8_000, 10);
            rafraichir(VUE);

            MargeDTO ligne = marges().getFirst();

            assertThat(ligne.margeBrute()).isEqualTo(-30_000L);
            assertThat(ligne.tauxMargePct()).isEqualByComparingTo("-60.00");
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        /** Un produit jamais vendu n'a pas de marge à montrer : son absence n'est pas un oubli. */
        @Test
        @DisplayName("un produit sans vente ne figure pas au rapport")
        void produitSansVenteAbsent() {
            produitReference("DORMANT", 10_000, 6_000);
            rafraichir(VUE);

            assertThat(marges()).isEmpty();
        }

        @Test
        @DisplayName("une vente annulée ne compte pas dans la marge")
        void venteAnnuleeExclue() {
            Produit produit = produitReference("DOLIPRANE", 10_000, 6_000);
            vendu(produit, 10, LocalDate.now(), true, CategorieChiffreAffaire.CA);
            rafraichir(VUE);

            assertThat(marges()).isEmpty();
        }

        @Test
        @DisplayName("une vente d'il y a plus d'un an ne compte plus")
        void venteHorsFenetreExclue() {
            Produit produit = produitReference("DOLIPRANE", 10_000, 6_000);
            vendu(produit, 10, LocalDate.now().minusMonths(13));
            rafraichir(VUE);

            assertThat(marges()).isEmpty();
        }

        @Test
        @DisplayName("base vierge : aucune ligne et un résumé à zéro")
        void baseVierge() {
            rafraichir(VUE);

            assertThat(marges()).isEmpty();

            MargeSummaryDTO resume = services.margeReportService.getMargeSummary(null, 10, 20);

            assertThat(resume.totalProduits()).isZero();
            assertThat(resume.caTotalGlobal()).isZero();
            assertThat(resume.margeBruteGlobale()).isZero();
            assertThat(resume.tauxMargeMoyen()).isEqualByComparingTo("0");
        }
    }

    // ===== recherche, tri et pagination =====

    @Nested
    @DisplayName("Recherche, tri et pagination")
    class RechercheTriPagination {

        @Test
        @DisplayName("les produits sont classés par marge brute décroissante par défaut")
        void ordreParDefaut() {
            venduAvecMarge("GROSSE MARGE", 10_000, 2_000, 10);
            venduAvecMarge("PETITE MARGE", 10_000, 9_000, 10);
            rafraichir(VUE);

            assertThat(marges()).extracting(MargeDTO::libelle).containsExactly("GROSSE MARGE", "PETITE MARGE");
        }

        @Test
        @DisplayName("un tri demandé par l'écran est appliqué")
        void triDemande() {
            venduAvecMarge("GROSSE MARGE", 10_000, 2_000, 10);
            venduAvecMarge("PETITE MARGE", 10_000, 9_000, 10);
            rafraichir(VUE);

            Page<MargeDTO> page = services.margeReportService.getMarges(
                null,
                null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "tauxMargePct"))
            );

            assertThat(page.getContent()).extracting(MargeDTO::libelle).containsExactly("PETITE MARGE", "GROSSE MARGE");
        }

        /**
         * Le nom de colonne vient de l'écran et finit littéralement dans le SQL. La liste blanche
         * est la seule chose qui s'interpose : une colonne inconnue retombe sur {@code marge_brute}
         * plutôt que d'être recopiée telle quelle. La <b>direction</b>, elle, reste celle demandée —
         * elle ne provient pas d'une chaîne libre.
         */
        @Test
        @DisplayName("un tri sur une colonne inconnue retombe sur la marge brute, sans exécuter la chaîne reçue")
        void triInconnuRetombeSurLaMargeBrute() {
            venduAvecMarge("GROSSE MARGE", 10_000, 2_000, 10);
            venduAvecMarge("PETITE MARGE", 10_000, 9_000, 10);
            rafraichir(VUE);

            String colonneMalveillante = "colonne'; DROP TABLE produit; --";

            assertThat(
                services.margeReportService
                    .getMarges(null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, colonneMalveillante)))
                    .getContent()
            )
                .extracting(MargeDTO::libelle)
                .containsExactly("GROSSE MARGE", "PETITE MARGE");

            assertThat(
                services.margeReportService
                    .getMarges(null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, colonneMalveillante)))
                    .getContent()
            )
                .extracting(MargeDTO::libelle)
                .containsExactly("PETITE MARGE", "GROSSE MARGE");

            // La table est intacte : la chaîne reçue n'a jamais atteint le moteur.
            assertThat(((Number) em.createNativeQuery("SELECT COUNT(*) FROM produit").getSingleResult()).longValue()).isEqualTo(2L);
        }

        @Test
        @DisplayName("la pagination borne les lignes et rapporte le total réel")
        void pagination() {
            venduAvecMarge("A", 10_000, 1_000, 10);
            venduAvecMarge("B", 10_000, 2_000, 10);
            venduAvecMarge("C", 10_000, 3_000, 10);
            rafraichir(VUE);

            Page<MargeDTO> page = services.margeReportService.getMarges(null, null, PageRequest.of(0, 2));

            assertThat(page.getContent()).extracting(MargeDTO::libelle).containsExactly("A", "B");
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(services.margeReportService.getMarges(null, null, PageRequest.of(1, 2)).getContent())
                .extracting(MargeDTO::libelle)
                .containsExactly("C");
        }

        @Test
        @DisplayName("la recherche porte sur le libellé comme sur le code CIP, sans tenir compte de la casse")
        void recherche() {
            Produit doliprane = produitReference("DOLIPRANE 1000", 10_000, 6_000);
            vendu(doliprane, 10, LocalDate.now());
            venduAvecMarge("EFFERALGAN", 10_000, 6_000, 10);
            rafraichir(VUE);

            assertThat(services.margeReportService.getMarges(null, "doliprane", PageRequest.of(0, 10)).getContent())
                .extracting(MargeDTO::libelle)
                .containsExactly("DOLIPRANE 1000");

            String codeCip = doliprane.getFournisseurProduitPrincipal().getCodeCip();
            assertThat(services.margeReportService.getMarges(null, codeCip, PageRequest.of(0, 10)).getContent())
                .extracting(MargeDTO::libelle)
                .containsExactly("DOLIPRANE 1000");
        }

        @Test
        @DisplayName("le filtre par famille retient les produits de cette famille")
        void filtreParFamille() {
            venduAvecMarge("DOLIPRANE", 10_000, 6_000, 10);
            rafraichir(VUE);

            assertThat(services.margeReportService.getMarges(premiereFamille().getId(), null, PageRequest.of(0, 10)).getContent())
                .hasSize(1);
            assertThat(services.margeReportService.getMarges(999_999, null, PageRequest.of(0, 10)).getContent()).isEmpty();
        }

        @Test
        @DisplayName("une recherche sans résultat rend une page vide, pas une erreur")
        void rechercheSansResultat() {
            venduAvecMarge("DOLIPRANE", 10_000, 6_000, 10);
            rafraichir(VUE);

            Page<MargeDTO> page = services.margeReportService.getMarges(null, "INTROUVABLE", PageRequest.of(0, 10));

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }
    }

    // ===== marges insuffisantes et palmarès =====

    @Nested
    @DisplayName("Marges insuffisantes et palmarès")
    class SeuilsEtPalmares {

        /** C'est la liste sur laquelle on renégocie un prix d'achat ou on relève un prix de vente. */
        @Test
        @DisplayName("les produits sous le seuil remontent, du plus faible taux au plus élevé")
        void margesInsuffisantes() {
            venduAvecMarge("CONFORTABLE", 10_000, 5_000, 10); // 50 %
            venduAvecMarge("JUSTE", 10_000, 9_200, 10); // 8 %
            venduAvecMarge("CRITIQUE", 10_000, 9_800, 10); // 2 %
            rafraichir(VUE);

            assertThat(services.margeReportService.getProduitsMargeInsuffisante(10, PageRequest.of(0, 10)).getContent())
                .extracting(MargeDTO::libelle)
                .containsExactly("CRITIQUE", "JUSTE");
        }

        @Test
        @DisplayName("aucun produit sous le seuil rend une page vide")
        void aucuneMargeInsuffisante() {
            venduAvecMarge("CONFORTABLE", 10_000, 5_000, 10);
            rafraichir(VUE);

            assertThat(services.margeReportService.getProduitsMargeInsuffisante(10, PageRequest.of(0, 10)).getContent()).isEmpty();
        }

        @Test
        @DisplayName("le palmarès retient les N meilleures marges et les pagine à l'intérieur")
        void palmaresDesMeilleuresMarges() {
            venduAvecMarge("A", 10_000, 1_000, 10);
            venduAvecMarge("B", 10_000, 2_000, 10);
            venduAvecMarge("C", 10_000, 3_000, 10);
            rafraichir(VUE);

            Page<MargeDTO> palmares = services.margeReportService.getTopProduitsParMarge(2, PageRequest.of(0, 10));

            assertThat(palmares.getContent()).extracting(MargeDTO::libelle).containsExactly("A", "B");
            assertThat(palmares.getTotalElements()).isEqualTo(2);
        }
    }

    // ===== résumé =====

    @Nested
    @DisplayName("Résumé")
    class Resume {

        @Test
        @DisplayName("le résumé totalise le chiffre d'affaires, le coût et la marge de l'ensemble")
        void totaux() {
            venduAvecMarge("A", 10_000, 6_000, 10);
            venduAvecMarge("B", 10_000, 4_000, 10);
            rafraichir(VUE);

            MargeSummaryDTO resume = services.margeReportService.getMargeSummary(null, 10, 20);

            assertThat(resume.totalProduits()).isEqualTo(2);
            assertThat(resume.caTotalGlobal()).isEqualTo(200_000L);
            assertThat(resume.coutAchatGlobal()).isEqualTo(100_000L);
            assertThat(resume.margeBruteGlobale()).isEqualTo(100_000L);
            assertThat(resume.tauxMargeMoyen()).isEqualByComparingTo("50.00");
        }

        /**
         * Les deux seuils découpent le catalogue en trois : ce qui est à renégocier, ce qui va bien,
         * et l'entre-deux qu'on laisse tranquille. Les compteurs disent combien de produits et
         * quelle part du chiffre d'affaires sont concernés.
         */
        @Test
        @DisplayName("les deux seuils comptent les produits et le chiffre d'affaires de part et d'autre")
        void comptagesParSeuil() {
            venduAvecMarge("CRITIQUE", 10_000, 9_800, 10); // 2 %
            venduAvecMarge("MOYEN", 10_000, 8_500, 10); // 15 %
            venduAvecMarge("CONFORTABLE", 10_000, 5_000, 10); // 50 %
            rafraichir(VUE);

            MargeSummaryDTO resume = services.margeReportService.getMargeSummary(null, 10, 20);

            assertThat(resume.nbProduitsMargeInsuffisante()).isEqualTo(1);
            assertThat(resume.caProduitsFaibleMarge()).isEqualTo(100_000L);
            assertThat(resume.nbProduitsMargeConfortable()).isEqualTo(1);
            assertThat(resume.caProduitsBonneMarge()).isEqualTo(100_000L);
        }

        @Test
        @DisplayName("le résumé suit le filtre par famille")
        void resumeFiltre() {
            venduAvecMarge("DOLIPRANE", 10_000, 6_000, 10);
            rafraichir(VUE);

            assertThat(services.margeReportService.getMargeSummary(premiereFamille().getId(), 10, 20).totalProduits()).isEqualTo(1);
            assertThat(services.margeReportService.getMargeSummary(999_999, 10, 20).totalProduits()).isZero();
        }
    }

    // ===== export =====

    @Nested
    @DisplayName("Export")
    class Export {

        /**
         * L'export passait autrefois un résumé nul et une liste vide au gabarit, qui le
         * déréférençait : chaque impression répondait 500. Il doit imprimer ce que l'utilisateur
         * vient de lire — les mêmes lignes, filtrées de la même façon, et leur résumé.
         */
        @Test
        @DisplayName("l'export imprime les lignes filtrées et leur résumé, jamais une liste vide")
        void exportReprendLEcran() {
            venduAvecMarge("DOLIPRANE", 10_000, 6_000, 10);
            venduAvecMarge("EFFERALGAN", 10_000, 4_000, 10);
            rafraichir(VUE);

            when(services.profitabilityPdfReportService.export(any(), anyList())).thenReturn(new byte[] { 1, 2, 3 });

            byte[] pdf = services.margeReportService.export(null, "DOLIPRANE");

            assertThat(pdf).isNotEmpty();

            ArgumentCaptor<MargeSummaryDTO> resume = ArgumentCaptor.forClass(MargeSummaryDTO.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<MargeDTO>> lignes = ArgumentCaptor.forClass(List.class);
            verify(services.profitabilityPdfReportService).export(resume.capture(), lignes.capture());

            assertThat(resume.getValue()).isNotNull();
            assertThat(lignes.getValue()).extracting(MargeDTO::libelle).containsExactly("DOLIPRANE");
        }

        /** L'export est exhaustif par nature : une impression limitée à la première page ne sert à rien. */
        @Test
        @DisplayName("l'export n'est pas borné par la pagination de l'écran")
        void exportNonPagine() {
            for (int i = 1; i <= 25; i++) {
                venduAvecMarge("PRODUIT " + i, 10_000, 6_000, i);
            }
            rafraichir(VUE);

            when(services.profitabilityPdfReportService.export(any(), anyList())).thenReturn(new byte[] { 1 });

            services.margeReportService.export(null, null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<MargeDTO>> lignes = ArgumentCaptor.forClass(List.class);
            verify(services.profitabilityPdfReportService).export(any(), lignes.capture());

            assertThat(lignes.getValue()).hasSize(25);
        }
    }

    // ===== fabriques locales =====

    private List<MargeDTO> marges() {
        return services.margeReportService.getMarges(null, null, PageRequest.of(0, 50)).getContent();
    }

    private Produit produitReference(String libelle, int prixVente, int coutAchat) {
        Produit produit = produit(libelle, TypeProduit.PACKAGE, prixVente, 5);
        produit.setCostAmount(coutAchat);
        produit.setItemCostAmount(coutAchat);
        em.flush();
        referencement(produit, fournisseur("LABOREX " + unique("")));
        return produit;
    }

    /** Un produit vendu une fois, au prix et au coût voulus : c'est l'écart des deux qui fait la marge. */
    private void venduAvecMarge(String libelle, int prixVente, int coutAchat, int quantite) {
        vendu(produitReference(libelle, prixVente, coutAchat), quantite, LocalDate.now());
    }
}
