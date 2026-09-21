package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.repository.ConcentrationPayersRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La concentration des payeurs se calcule en deux temps dans une même requête : un regroupement par
 * organisme, puis une part de marché rapportée à un total que la requête recalcule à côté.
 *
 * <p>C'est cette architecture en tables communes qui demande une vérification en base. Le total est
 * établi sur <b>tous</b> les organismes, alors que la liste rendue est plafonnée aux premiers : les
 * parts n'ont donc aucune raison de sommer à cent, et ce n'est pas un défaut mais le sens de
 * l'indicateur — la part du premier payeur dans l'ensemble du tiers payant, non dans le palmarès.
 * Un total calculé sur le seul palmarès gonflerait mécaniquement chaque part et ferait passer une
 * officine bien répartie pour dépendante.
 */
@DisplayName("ConcentrationPayersRepository — concentration lue sur PostgreSQL")
class ConcentrationPayersRepositoryIntegrationTest extends AbstractReportIntegrationTest {

    private ConcentrationPayersRepository repository;

    @BeforeEach
    void cablerLeRepository() {
        repository = new ConcentrationPayersRepository(em);
    }

    // ===== regroupement =====

    @Nested
    @DisplayName("Regroupement par organisme")
    class RegroupementParOrganisme {

        @Test
        @DisplayName("les factures d'un même payeur se cumulent sur une ligne")
        void cumulParPayeur() {
            TiersPayant payeur = payeur("CNAM", 45);
            facture(payeur, hier(), 3_000_000, 1_000_000, InvoiceStatut.PARTIALLY_PAID);
            facture(payeur, hier(), 2_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            Object[] ligne = ligneDe(payeur.getName());

            assertThat(toLong(ligne[1])).isEqualTo(5_000_000L);
            assertThat(toLong(ligne[2])).isEqualTo(2L);
            assertThat(toLong(ligne[3])).isEqualTo(45L);
        }

        @Test
        @DisplayName("les organismes sont classés du plus gros chiffre au plus petit")
        void classementParChiffre() {
            TiersPayant petit = payeur("PETIT", 30);
            TiersPayant gros = payeur("GROS", 30);
            facture(petit, hier(), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(gros, hier(), 50_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            List<String> noms = organismes();

            assertThat(noms.indexOf(gros.getName())).isLessThan(noms.indexOf(petit.getName()));
        }

        /** Le plafond sert à ne pas noyer l'écran : c'est un palmarès, pas un annuaire. */
        @Test
        @DisplayName("le palmarès s'arrête au nombre d'organismes demandé")
        void plafondDuPalmares() {
            for (int i = 0; i < 6; i++) {
                facture(payeur("PAYEUR-" + i, 30), hier(), 1_000_000 * (i + 1), 0, InvoiceStatut.NOT_PAID);
            }
            em.flush();

            assertThat(repository.findConcentration(debutDeLAnnee(), LocalDate.now(), 3)).hasSize(3);
        }

        @Test
        @DisplayName("une facture hors période ne compte pas")
        void horsPeriode() {
            TiersPayant payeur = payeur("CNAM", 30);
            facture(payeur, hier(), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            LocalDate demain = LocalDate.now().plusDays(1);

            assertThat(repository.findConcentration(demain, demain.plusDays(1), 10))
                .noneMatch(row -> payeur.getName().equals(row[0]));
        }

        /** Une facture regroupée est portée par sa facture de groupe : la compter doublerait le chiffre. */
        @Test
        @DisplayName("le périmètre exclut les factures rattachées à une facture de groupe")
        void factureRegroupeeExclue() {
            TiersPayant payeur = payeur("CNAM", 30);
            var facture = facture(payeur, hier(), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            var groupe = facture(payeur, hier(), 5_000_000, 0, InvoiceStatut.NOT_PAID);
            facture.setGroupeFactureTiersPayant(groupe);
            em.flush();

            assertThat(toLong(ligneDe(payeur.getName())[1])).isEqualTo(5_000_000L);
        }
    }

    // ===== parts et totaux =====

    @Nested
    @DisplayName("Parts et totaux")
    class PartsEtTotaux {

        @Test
        @DisplayName("la part de chaque organisme se rapporte au total du tiers payant")
        void partDuTotal() {
            TiersPayant premier = payeur("PREMIER", 30);
            TiersPayant second = payeur("SECOND", 30);
            facture(premier, hier(), 7_500_000, 0, InvoiceStatut.NOT_PAID);
            facture(second, hier(), 2_500_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            List<Object[]> lignes = repository.findConcentration(debutDeLAnnee(), LocalDate.now(), 100);
            long total = toLong(lignes.getFirst()[5]);

            assertThat(toDouble(ligneDe(premier.getName())[4]))
                .isEqualTo(arrondiCentieme(7_500_000.0 * 100 / total));
            assertThat(toDouble(ligneDe(second.getName())[4]))
                .isEqualTo(arrondiCentieme(2_500_000.0 * 100 / total));
        }

        @Test
        @DisplayName("le total, le réglé et l'impayé accompagnent chaque ligne")
        void totauxSurChaqueLigne() {
            TiersPayant payeur = payeur("CNAM", 30);
            facture(payeur, hier(), 10_000_000, 4_000_000, InvoiceStatut.PARTIALLY_PAID);
            em.flush();

            List<Object[]> lignes = repository.findConcentration(debutDeLAnnee(), LocalDate.now(), 100);

            assertThat(lignes).allSatisfy(ligne -> {
                assertThat(toLong(ligne[5])).isEqualTo(toLong(lignes.getFirst()[5]));
                assertThat(toLong(ligne[7])).isEqualTo(toLong(ligne[5]) - toLong(ligne[6]));
            });
        }

        /**
         * Le total porte sur tous les organismes, la liste sur les premiers seulement : les parts
         * rendues ne somment donc pas à cent dès qu'un payeur est laissé hors palmarès. C'est voulu —
         * un total réduit au palmarès gonflerait chaque part.
         */
        @Test
        @DisplayName("le total reste celui de tous les organismes, palmarès ou non")
        void totalIndependantDuPlafond() {
            for (int i = 0; i < 5; i++) {
                facture(payeur("PAYEUR-" + i, 30), hier(), 2_000_000, 0, InvoiceStatut.NOT_PAID);
            }
            em.flush();

            long totalSansPlafond = toLong(repository.findConcentration(debutDeLAnnee(), LocalDate.now(), 100).getFirst()[5]);
            List<Object[]> palmares = repository.findConcentration(debutDeLAnnee(), LocalDate.now(), 2);

            assertThat(toLong(palmares.getFirst()[5])).isEqualTo(totalSansPlafond);
            assertThat(palmares.stream().mapToDouble(row -> toDouble(row[4])).sum()).isLessThan(100.0);
        }

        @Test
        @DisplayName("une période sans facture ne rend aucune ligne")
        void periodeSansFacture() {
            LocalDate demain = LocalDate.now().plusDays(1);

            assertThat(repository.findConcentration(demain, demain, 10)).isEmpty();
        }
    }

    // ===== évolution =====

    @Nested
    @DisplayName("Évolution mensuelle")
    class EvolutionMensuelle {

        @Test
        @DisplayName("chaque mois porte le chiffre de chaque organisme du palmarès")
        void chiffreMensuelParOrganisme() {
            TiersPayant payeur = payeur("CNAM", 30);
            LocalDate ceMois = LocalDate.now().withDayOfMonth(1).plusDays(2);
            facture(payeur, ceMois, 3_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            Object[] ligne = repository
                .findEvolution(100)
                .stream()
                .filter(row -> payeur.getName().equals(row[2]) && toLong(row[1]) == LocalDate.now().getMonthValue())
                .findFirst()
                .orElseThrow();

            assertThat(toLong(ligne[3])).isEqualTo(3_000_000L);
        }

        /**
         * Au-delà du palmarès, les organismes sont réunis sous « Autres » plutôt que disparaître :
         * la somme des courbes reste égale au chiffre d'affaires réel du tiers payant.
         */
        @Test
        @DisplayName("les organismes hors palmarès sont réunis sous « Autres »")
        void autresOrganismesRegroupes() {
            for (int i = 0; i < 4; i++) {
                facture(payeur("PAYEUR-" + i, 30), LocalDate.now().withDayOfMonth(1).plusDays(2), 1_000_000 * (i + 1), 0, InvoiceStatut.NOT_PAID);
            }
            em.flush();

            List<Object[]> avecPlafond = repository.findEvolution(1);

            assertThat(avecPlafond).anyMatch(row -> "Autres".equals(row[2]));
        }

        @Test
        @DisplayName("le regroupement ne perd aucun montant")
        void aucunMontantPerdu() {
            long avant = sommeDeLEvolution(1);
            for (int i = 0; i < 4; i++) {
                facture(payeur("PAYEUR-" + i, 30), LocalDate.now().withDayOfMonth(1).plusDays(2), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            }
            em.flush();

            assertThat(sommeDeLEvolution(1) - avant).isEqualTo(4_000_000L);
            assertThat(sommeDeLEvolution(1)).isEqualTo(sommeDeLEvolution(100));
        }

        @Test
        @DisplayName("les mois sont rendus dans l'ordre chronologique")
        void ordreChronologique() {
            TiersPayant payeur = payeur("CNAM", 30);
            facture(payeur, LocalDate.now().withDayOfMonth(1).minusMonths(2), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, LocalDate.now().withDayOfMonth(1), 2_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            assertThat(repository.findEvolution(100)).extracting(row -> toLong(row[0]) * 100 + toLong(row[1])).isSorted();
        }

        private long sommeDeLEvolution(int topN) {
            return repository.findEvolution(topN).stream().mapToLong(row -> toLong(row[3])).sum();
        }
    }

    // ===== utilitaires =====

    private Object[] ligneDe(String organisme) {
        return repository
            .findConcentration(debutDeLAnnee(), LocalDate.now(), 100)
            .stream()
            .filter(row -> organisme.equals(row[0]))
            .findFirst()
            .orElseThrow();
    }

    private List<String> organismes() {
        return repository.findConcentration(debutDeLAnnee(), LocalDate.now(), 100).stream().map(row -> (String) row[0]).toList();
    }

    private TiersPayant payeur(String nom, int delaiReglement) {
        TiersPayant payeur = tiersPayant(nom, groupeTiersPayant("GROUPE-" + nom));
        payeur.setDelaiReglement(delaiReglement);
        em.flush();
        return payeur;
    }

    /** {@code sales} est partitionnée par date : on reste dans l'année en cours. */
    private static LocalDate hier() {
        LocalDate hier = LocalDate.now().minusDays(1);
        LocalDate premierJanvier = LocalDate.now().withDayOfYear(1);
        return hier.isBefore(premierJanvier) ? premierJanvier : hier;
    }

    private static LocalDate debutDeLAnnee() {
        return LocalDate.now().withDayOfYear(1);
    }

    private static double arrondiCentieme(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }

    private static long toLong(Object valeur) {
        return valeur != null ? ((Number) valeur).longValue() : 0L;
    }

    private static double toDouble(Object valeur) {
        return valeur != null ? ((Number) valeur).doubleValue() : 0.0;
    }
}
