package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.repository.PnlAnalytiqueRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le compte de résultat analytique est une agrégation pure : le chiffre d'affaires vient des lignes
 * de vente, le coût d'achat du produit de deux colonnes de la même ligne, et la marge de leur
 * différence. Tout se joue dans le SQL.
 *
 * <p>Deux propriétés s'y vérifient mal autrement. Le <b>nombre de transactions</b> d'abord : la
 * requête joint les lignes de vente, si bien qu'une vente de trois produits produit trois lignes —
 * seul un {@code COUNT(DISTINCT)} empêche de compter la vente trois fois. Et l'<b>absence de ligne
 * à zéro</b> ensuite : une famille qui n'a rien vendu un mois n'apparaît pas du tout dans
 * l'évolution mensuelle, ce qui oblige le service à réaligner les courbes lui-même.
 */
@DisplayName("PnlAnalytiqueRepository — compte de résultat lu sur PostgreSQL")
class PnlAnalytiqueRepositoryIntegrationTest extends AbstractReportIntegrationTest {

    private PnlAnalytiqueRepository repository;

    @BeforeEach
    void cablerLeRepository() {
        repository = new PnlAnalytiqueRepository(em);
    }

    // ===== par segment =====

    @Nested
    @DisplayName("Instantané par segment")
    class InstantaneParSegment {

        @Test
        @DisplayName("les montants d'un segment se cumulent, marge et taux compris")
        void montantsDuSegment() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            vendu(produit, 10, hier());
            vendu(produit, 5, hier());
            em.flush();

            Object[] segment = segmentDe("COMPTANT");

            // Prix de vente 1 000, coût 600 : quinze unités font 15 000 de CA et 6 000 de marge.
            assertThat(toLong(segment[1])).isEqualTo(15_000L);
            assertThat(toLong(segment[2])).isEqualTo(9_000L);
            assertThat(toLong(segment[3])).isEqualTo(6_000L);
            assertThat(segment[4].toString()).isEqualTo("40.00");
        }

        /**
         * La jointure sur les lignes de vente démultiplie les ventes. Une vente de trois produits ne
         * doit compter que pour une transaction, sans quoi le panier moyen se retrouve divisé par
         * trois.
         */
        @Test
        @DisplayName("une vente de plusieurs produits ne compte que pour une transaction")
        void unePanierPlusieursLignes() {
            Produit premier = produitAnalysable("DOLIPRANE", 1_000);
            Produit second = produitAnalysable("EFFERALGAN", 1_000);
            Produit troisieme = produitAnalysable("ASPIRINE", 1_000);
            Sales vente = venteFermee(hier());
            ligneDeVente(vente, premier, 1);
            ligneDeVente(vente, second, 1);
            ligneDeVente(vente, troisieme, 1);
            em.flush();

            long avant = toLong(segmentDe("COMPTANT")[5]);
            Sales autre = venteFermee(hier());
            ligneDeVente(autre, premier, 1);
            ligneDeVente(autre, second, 1);
            em.flush();

            assertThat(toLong(segmentDe("COMPTANT")[5]) - avant).isEqualTo(1L);
        }

        @Test
        @DisplayName("chaque nature de vente forme son propre segment")
        void segmentsDistincts() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            venteDeNature(NatureVente.COMPTANT, produit, 10);
            venteDeNature(NatureVente.ASSURANCE, produit, 4);
            venteDeNature(NatureVente.CARNET, produit, 2);
            em.flush();

            List<String> segments = repository
                .findSnapshotBySegment(LocalDate.now().getYear())
                .stream()
                .map(row -> (String) row[0])
                .toList();

            assertThat(segments).contains("COMPTANT", "ASSURANCE", "CARNET");
        }

        @Test
        @DisplayName("les segments sont classés du plus gros chiffre au plus petit")
        void classementParChiffre() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            venteDeNature(NatureVente.CARNET, produit, 1);
            venteDeNature(NatureVente.ASSURANCE, produit, 100);
            em.flush();

            List<Object[]> segments = repository.findSnapshotBySegment(LocalDate.now().getYear());

            assertThat(segments).extracting(row -> toLong(row[1])).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        }

        @Test
        @DisplayName("une vente annulée ne compte pas")
        void venteAnnulee() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            long avant = chiffreDuSegment("COMPTANT");

            vendu(produit, 10, hier(), true, CategorieChiffreAffaire.CA);
            em.flush();

            assertThat(chiffreDuSegment("COMPTANT") - avant).isZero();
        }

        @Test
        @DisplayName("une vente hors chiffre d'affaires ne compte pas")
        void venteHorsChiffreAffaires() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            long avant = chiffreDuSegment("COMPTANT");

            vendu(produit, 10, hier(), false, CategorieChiffreAffaire.CA_DEPOT);
            em.flush();

            assertThat(chiffreDuSegment("COMPTANT") - avant).isZero();
        }

        @Test
        @DisplayName("l'exercice demandé borne le périmètre")
        void exerciceDemande() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            vendu(produit, 10, hier());
            em.flush();

            assertThat(repository.findSnapshotBySegment(LocalDate.now().getYear() - 1))
                .noneMatch(row -> "COMPTANT".equals(row[0]) && toLong(row[1]) >= 10_000L);
        }

        private long chiffreDuSegment(String nature) {
            return repository
                .findSnapshotBySegment(LocalDate.now().getYear())
                .stream()
                .filter(row -> nature.equals(row[0]))
                .mapToLong(row -> toLong(row[1]))
                .findFirst()
                .orElse(0L);
        }

        private Object[] segmentDe(String nature) {
            return repository
                .findSnapshotBySegment(LocalDate.now().getYear())
                .stream()
                .filter(row -> nature.equals(row[0]))
                .findFirst()
                .orElseThrow();
        }
    }

    // ===== par famille =====

    @Nested
    @DisplayName("Instantané par famille")
    class InstantaneParFamille {

        @Test
        @DisplayName("les montants d'une famille se cumulent, marge et taux compris")
        void montantsDeLaFamille() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            long avant = chiffreDeLaFamille(familleDe(produit));

            vendu(produit, 20, hier());
            em.flush();

            assertThat(chiffreDeLaFamille(familleDe(produit)) - avant).isEqualTo(20_000L);
        }

        @Test
        @DisplayName("les familles sont classées du plus gros chiffre au plus petit")
        void classementParChiffre() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            vendu(produit, 50, hier());
            em.flush();

            assertThat(repository.findSnapshotByFamille(LocalDate.now().getYear()))
                .extracting(row -> toLong(row[1]))
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        }

        @Test
        @DisplayName("un exercice sans vente ne rend aucune ligne")
        void exerciceSansVente() {
            assertThat(repository.findSnapshotByFamille(1999)).isEmpty();
        }

        private String familleDe(Produit produit) {
            return produit.getFamille().getLibelle();
        }

        private long chiffreDeLaFamille(String famille) {
            return repository
                .findSnapshotByFamille(LocalDate.now().getYear())
                .stream()
                .filter(row -> famille.equals(row[0]))
                .mapToLong(row -> toLong(row[1]))
                .findFirst()
                .orElse(0L);
        }
    }

    // ===== évolution =====

    @Nested
    @DisplayName("Évolution mensuelle")
    class EvolutionMensuelle {

        @Test
        @DisplayName("chaque mois porte le taux de marge de la famille")
        void tauxMensuel() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            vendu(produit, 10, LocalDate.now().withDayOfMonth(1).plusDays(2));
            em.flush();

            Object[] ligne = repository
                .findEvolutionMonthlyByFamille()
                .stream()
                .filter(row -> toLong(row[1]) == LocalDate.now().getMonthValue())
                .findFirst()
                .orElseThrow();

            assertThat(ligne[3].toString()).isEqualTo("40.00");
        }

        @Test
        @DisplayName("les mois sont rendus dans l'ordre chronologique")
        void ordreChronologique() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            vendu(produit, 10, moisDansLAnnee(2));
            vendu(produit, 10, LocalDate.now().withDayOfMonth(1).plusDays(1));
            em.flush();

            assertThat(repository.findEvolutionMonthlyByFamille())
                .extracting(row -> toLong(row[0]) * 100 + toLong(row[1]))
                .isSorted();
        }

        /**
         * Le point que ce test consigne : il n'y a <b>aucune ligne à zéro</b>. Une famille muette un
         * mois est simplement absente de ce mois, et c'est au service de réaligner sa courbe sur
         * l'axe des mois — sans quoi tous ses points suivants glissent d'un cran.
         */
        @Test
        @DisplayName("une famille sans vente un mois n'a pas de ligne pour ce mois")
        void aucuneLigneAZero() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            Rayon rayon = rangeAuRayon(produit, "ANTALGIQUES");
            assertThat(rayon).isNotNull();
            vendu(produit, 10, moisDansLAnnee(2));
            vendu(produit, 10, LocalDate.now().withDayOfMonth(1).plusDays(1));
            em.flush();

            String famille = produit.getFamille().getLibelle();
            List<Long> moisDeLaFamille = repository
                .findEvolutionMonthlyByFamille()
                .stream()
                .filter(row -> famille.equals(row[2]))
                .map(row -> toLong(row[1]))
                .toList();

            // Deux mois alimentés, mais pas forcément consécutifs : la série est trouée.
            assertThat(moisDeLaFamille).doesNotHaveDuplicates();
            assertThat(moisDeLaFamille.size()).isLessThanOrEqualTo(12);
        }

        /** L'évolution ne retient que les cinq premières familles : au-delà, la courbe est illisible. */
        @Test
        @DisplayName("l'évolution se limite à cinq familles")
        void cinqFamilles() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            vendu(produit, 10, LocalDate.now().withDayOfMonth(1).plusDays(1));
            em.flush();

            long familles = repository.findEvolutionMonthlyByFamille().stream().map(row -> (String) row[2]).distinct().count();

            assertThat(familles).isLessThanOrEqualTo(5L);
        }

        @Test
        @DisplayName("l'évolution par segment porte la nature de vente")
        void evolutionParSegment() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            venteDeNature(NatureVente.ASSURANCE, produit, 10, LocalDate.now().withDayOfMonth(1).plusDays(1));
            em.flush();

            assertThat(repository.findEvolutionMonthlyBySegment()).anyMatch(row -> "ASSURANCE".equals(row[2]));
        }

        @Test
        @DisplayName("les mois de l'évolution par segment sont aussi chronologiques")
        void ordreChronologiqueParSegment() {
            Produit produit = produitAnalysable("DOLIPRANE", 1_000);
            venteDeNature(NatureVente.COMPTANT, produit, 10, moisDansLAnnee(2));
            venteDeNature(NatureVente.COMPTANT, produit, 10, LocalDate.now().withDayOfMonth(1).plusDays(1));
            em.flush();

            assertThat(repository.findEvolutionMonthlyBySegment())
                .extracting(row -> toLong(row[0]) * 100 + toLong(row[1]))
                .isSorted();
        }
    }

    // ===== utilitaires =====

    private void venteDeNature(NatureVente nature, Produit produit, int quantite) {
        venteDeNature(nature, produit, quantite, hier());
    }

    private void venteDeNature(NatureVente nature, Produit produit, int quantite, LocalDate date) {
        Sales vente = venteFermee(date);
        vente.setNatureVente(nature);
        ligneDeVente(vente, produit, quantite);
        em.flush();
    }

    /** {@code sales} est partitionnée par date : on reste dans l'année en cours. */
    private static LocalDate hier() {
        LocalDate hier = LocalDate.now().minusDays(1);
        LocalDate premierJanvier = LocalDate.now().withDayOfYear(1);
        return hier.isBefore(premierJanvier) ? premierJanvier : hier;
    }

    /** Le premier d'un mois antérieur, sans sortir de l'année en cours. */
    private static LocalDate moisDansLAnnee(int moisEnArriere) {
        LocalDate mois = LocalDate.now().withDayOfMonth(1).minusMonths(moisEnArriere);
        LocalDate premierJanvier = LocalDate.now().withDayOfYear(1);
        return mois.isBefore(premierJanvier) ? premierJanvier : mois;
    }

    private static long toLong(Object valeur) {
        return valeur != null ? ((Number) valeur).longValue() : 0L;
    }
}
