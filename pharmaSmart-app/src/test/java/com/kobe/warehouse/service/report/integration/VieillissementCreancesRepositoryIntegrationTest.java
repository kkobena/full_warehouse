package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.repository.VieillissementCreancesRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le vieillissement des créances est calculé entièrement en SQL : les tranches d'ancienneté, le
 * nombre de factures en retard et le délai moyen de règlement pondéré sortent d'une seule requête.
 *
 * <p>Rien de tout cela n'est vérifiable hors base. Les bornes des tranches sont écrites en
 * {@code BETWEEN}, qui est inclusif des deux côtés — une facture de soixante jours appartient à la
 * deuxième tranche, celle de soixante-et-un à la troisième. Le retard se juge contre un délai de
 * règlement résolu par {@code COALESCE(payeur, groupe, 30)}, et le délai moyen est une moyenne
 * pondérée par les montants : un calcul qu'aucune relecture ne garantit et que seule une vraie base
 * peut confirmer.
 */
@DisplayName("VieillissementCreancesRepository — créances lues sur PostgreSQL")
class VieillissementCreancesRepositoryIntegrationTest extends AbstractReportIntegrationTest {

    private VieillissementCreancesRepository repository;

    @BeforeEach
    void cablerLeRepository() {
        repository = new VieillissementCreancesRepository(em);
    }

    // ===== tranches d'ancienneté =====

    @Nested
    @DisplayName("Tranches d'ancienneté")
    class TranchesDAnciennete {

        @Test
        @DisplayName("chaque facture tombe dans la tranche de son ancienneté")
        void repartitionParAnciennete() {
            TiersPayant payeur = payeur("CNAM", 30);
            Object[] avant = repository.findAgingGlobal();

            facture(payeur, jourDeLAnnee(10), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(45), 2_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(75), 3_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(120), 4_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            Object[] apres = repository.findAgingGlobal();

            assertThat(ecart(avant, apres, 0)).isEqualTo(1_000_000L);
            assertThat(ecart(avant, apres, 1)).isEqualTo(2_000_000L);
            assertThat(ecart(avant, apres, 2)).isEqualTo(3_000_000L);
            assertThat(ecart(avant, apres, 3)).isEqualTo(4_000_000L);
        }

        /** Les bornes sont inclusives : trente jours relèvent encore de la première tranche. */
        @Test
        @DisplayName("les bornes des tranches appartiennent à la tranche la plus jeune")
        void bornesInclusives() {
            TiersPayant payeur = payeur("CNAM", 30);
            Object[] avant = repository.findAgingGlobal();

            facture(payeur, jourDeLAnnee(30), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(31), 2_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(60), 4_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(61), 8_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(90), 16_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(91), 32_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            Object[] apres = repository.findAgingGlobal();

            assertThat(ecart(avant, apres, 0)).isEqualTo(1_000_000L);
            assertThat(ecart(avant, apres, 1)).isEqualTo(6_000_000L);
            assertThat(ecart(avant, apres, 2)).isEqualTo(24_000_000L);
            assertThat(ecart(avant, apres, 3)).isEqualTo(32_000_000L);
        }

        @Test
        @DisplayName("l'encours total est la somme des quatre tranches")
        void encoursTotal() {
            TiersPayant payeur = payeur("CNAM", 30);
            Object[] avant = repository.findAgingGlobal();

            facture(payeur, jourDeLAnnee(10), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(120), 4_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            Object[] apres = repository.findAgingGlobal();

            assertThat(ecart(avant, apres, 4)).isEqualTo(5_000_000L);
            assertThat(ecart(avant, apres, 4))
                .isEqualTo(ecart(avant, apres, 0) + ecart(avant, apres, 1) + ecart(avant, apres, 2) + ecart(avant, apres, 3));
        }

        /** Seul le reste dû entre en créance : la part déjà réglée n'est plus à recouvrer. */
        @Test
        @DisplayName("seul le reste dû est compté, non le montant facturé")
        void seulLeResteDu() {
            TiersPayant payeur = payeur("CNAM", 30);
            Object[] avant = repository.findAgingGlobal();

            facture(payeur, jourDeLAnnee(10), 1_000_000, 600_000, InvoiceStatut.PARTIALLY_PAID);
            em.flush();

            assertThat(ecart(avant, repository.findAgingGlobal(), 4)).isEqualTo(400_000L);
        }

        /** Un règlement supérieur au facturé ne doit pas rendre une créance négative. */
        @Test
        @DisplayName("un trop-perçu ne rend pas une créance négative")
        void tropPercu() {
            TiersPayant payeur = payeur("CNAM", 30);
            Object[] avant = repository.findAgingGlobal();

            facture(payeur, jourDeLAnnee(10), 1_000_000, 1_200_000, InvoiceStatut.PARTIALLY_PAID);
            em.flush();

            assertThat(ecart(avant, repository.findAgingGlobal(), 4)).isZero();
        }

        @Test
        @DisplayName("une facture soldée sort du périmètre des créances")
        void factureSoldee() {
            TiersPayant payeur = payeur("CNAM", 30);
            Object[] avant = repository.findAgingGlobal();

            facture(payeur, jourDeLAnnee(10), 1_000_000, 1_000_000, InvoiceStatut.PAID);
            em.flush();

            Object[] apres = repository.findAgingGlobal();
            assertThat(ecart(avant, apres, 4)).isZero();
            assertThat(ecart(avant, apres, 5)).isZero();
        }
    }

    // ===== retard =====

    @Nested
    @DisplayName("Factures en retard")
    class FacturesEnRetard {

        @Test
        @DisplayName("une facture dépassant le délai du payeur est en retard")
        void depassementDuDelai() {
            TiersPayant payeur = payeur("CNAM", 45);
            Object[] avant = repository.findAgingGlobal();

            facture(payeur, jourDeLAnnee(46), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            assertThat(ecart(avant, repository.findAgingGlobal(), 6)).isEqualTo(1L);
        }

        @Test
        @DisplayName("une facture juste dans le délai n'est pas en retard")
        void dansLeDelai() {
            TiersPayant payeur = payeur("CNAM", 45);
            Object[] avant = repository.findAgingGlobal();

            facture(payeur, jourDeLAnnee(45), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            Object[] apres = repository.findAgingGlobal();
            assertThat(ecart(avant, apres, 6)).isZero();
            assertThat(ecart(avant, apres, 5)).isEqualTo(1L);
        }

        /**
         * Le délai se résout par {@code COALESCE(payeur, groupe, 30)}. La colonne du payeur étant
         * <b>non nulle</b> en base, c'est toujours la sienne qui décide dès qu'une facture lui est
         * rattachée : le repli sur le groupe ne concerne que les factures sans payeur individuel.
         */
        @Test
        @DisplayName("le délai du payeur prime sur celui de son groupe")
        void delaiDuPayeurPrime() {
            GroupeTiersPayant groupe = groupeTiersPayant("GROUPE");
            groupe.setDelaiReglement(90);
            TiersPayant payeur = tiersPayant("MUGEF", groupe);
            payeur.setDelaiReglement(30);
            em.flush();

            Object[] avant = repository.findAgingGlobal();
            facture(payeur, jourDeLAnnee(60), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            // Soixante jours pour un délai de trente : en retard, malgré les quatre-vingt-dix du groupe.
            assertThat(ecart(avant, repository.findAgingGlobal(), 6)).isEqualTo(1L);
        }

        @Test
        @DisplayName("le délai du payeur figure sur sa ligne, tel qu'il est renseigné")
        void delaiRenseigne() {
            GroupeTiersPayant groupe = groupeTiersPayant("GROUPE");
            groupe.setDelaiReglement(90);
            TiersPayant payeur = tiersPayant("CMU", groupe);
            payeur.setDelaiReglement(60);
            em.flush();

            facture(payeur, jourDeLAnnee(10), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            Object[] ligne = repository
                .findAgingByOrganisme(0, 1000)
                .stream()
                .filter(row -> payeur.getName().equals(row[0]))
                .findFirst()
                .orElseThrow();

            assertThat(toLong(ligne[1])).isEqualTo(60L);
        }
    }

    // ===== par organisme =====

    @Nested
    @DisplayName("Détail par organisme")
    class DetailParOrganisme {

        @Test
        @DisplayName("l'organisme porte son encours, ses tranches et son délai")
        void contenuDeLaLigne() {
            TiersPayant payeur = payeur("CNAM", 45);
            facture(payeur, jourDeLAnnee(10), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(120), 4_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            Object[] ligne = ligneDe(payeur.getName());

            assertThat(toLong(ligne[2])).isEqualTo(5_000_000L);
            assertThat(toLong(ligne[3])).isEqualTo(1_000_000L);
            assertThat(toLong(ligne[6])).isEqualTo(4_000_000L);
            assertThat(toLong(ligne[1])).isEqualTo(45L);
            assertThat(toLong(ligne[7])).isEqualTo(2L);
        }

        /**
         * Le délai moyen de règlement est pondéré par les montants : une grosse facture ancienne
         * pèse plus qu'une petite facture récente. Sans pondération, l'indicateur se laisserait
         * berner par une multitude de petites factures fraîches.
         */
        @Test
        @DisplayName("le délai moyen est pondéré par les montants restant dus")
        void delaiMoyenPondere() {
            TiersPayant payeur = payeur("CNAM", 30);
            facture(payeur, jourDeLAnnee(10), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, jourDeLAnnee(110), 9_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            // (10 × 1M + 110 × 9M) / 10M = 100 jours
            assertThat(toLong(ligneDe(payeur.getName())[9])).isEqualTo(100L);
        }

        @Test
        @DisplayName("un organisme sans encours n'a pas de délai moyen plutôt qu'une division par zéro")
        void encoursNul() {
            TiersPayant payeur = payeur("CNAM", 30);
            facture(payeur, jourDeLAnnee(10), 1_000_000, 1_000_000, InvoiceStatut.PARTIALLY_PAID);
            em.flush();

            assertThat(toLong(ligneDe(payeur.getName())[9])).isZero();
        }

        @Test
        @DisplayName("les organismes sont classés du plus gros encours au plus petit")
        void classementParEncours() {
            TiersPayant petit = payeur("PETIT", 30);
            TiersPayant gros = payeur("GROS", 30);
            facture(petit, jourDeLAnnee(10), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(gros, jourDeLAnnee(10), 50_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            List<String> noms = repository.findAgingByOrganisme(0, 100).stream().map(row -> (String) row[0]).toList();

            assertThat(noms.indexOf(gros.getName())).isLessThan(noms.indexOf(petit.getName()));
        }

        /** Le compteur sert à paginer la liste : il doit en annoncer exactement la taille. */
        @Test
        @DisplayName("le compteur annonce exactement ce que la liste contient")
        void compteurEtListeConcordent() {
            facture(payeur("CNAM", 30), jourDeLAnnee(10), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur("MUGEF", 45), jourDeLAnnee(50), 2_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            assertThat(repository.countAgingByOrganisme()).isEqualTo(repository.findAgingByOrganisme(0, 1000).size());
        }

        @Test
        @DisplayName("la pagination découpe la liste sans la recomposer")
        void pagination() {
            facture(payeur("A-CNAM", 30), jourDeLAnnee(10), 3_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur("B-MUGEF", 30), jourDeLAnnee(10), 2_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur("C-CMU", 30), jourDeLAnnee(10), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            List<Object[]> tout = repository.findAgingByOrganisme(0, 1000);
            List<Object[]> premiere = repository.findAgingByOrganisme(0, 2);
            List<Object[]> seconde = repository.findAgingByOrganisme(2, 2);

            assertThat(premiere).hasSize(2);
            assertThat((String) premiere.getFirst()[0]).isEqualTo((String) tout.getFirst()[0]);
            assertThat((String) seconde.getFirst()[0]).isEqualTo((String) tout.get(2)[0]);
        }

        private Object[] ligneDe(String organisme) {
            return repository
                .findAgingByOrganisme(0, 1000)
                .stream()
                .filter(row -> organisme.equals(row[0]))
                .findFirst()
                .orElseThrow();
        }
    }

    // ===== encours mensuel =====

    @Nested
    @DisplayName("Évolution mensuelle de l'encours")
    class EvolutionMensuelle {

        @Test
        @DisplayName("chaque mois porte son facturé et ce qui reste dû")
        void contenuDuMois() {
            TiersPayant payeur = payeur("CNAM", 30);
            LocalDate mois = LocalDate.now().withDayOfMonth(1).plusDays(5);
            facture(payeur, mois, 5_000_000, 2_000_000, InvoiceStatut.PARTIALLY_PAID);
            em.flush();

            Object[] ligne = moisDe(LocalDate.now().getYear(), LocalDate.now().getMonthValue());

            assertThat(toLong(ligne[2])).isGreaterThanOrEqualTo(5_000_000L);
            assertThat(toLong(ligne[3])).isGreaterThanOrEqualTo(3_000_000L);
        }

        @Test
        @DisplayName("les mois sont rendus dans l'ordre chronologique")
        void ordreChronologique() {
            TiersPayant payeur = payeur("CNAM", 30);
            facture(payeur, LocalDate.now().withDayOfMonth(1).minusMonths(2), 1_000_000, 0, InvoiceStatut.NOT_PAID);
            facture(payeur, LocalDate.now().withDayOfMonth(1), 2_000_000, 0, InvoiceStatut.NOT_PAID);
            em.flush();

            List<Object[]> rows = repository.findEncoursMensuelEvolution();

            assertThat(rows)
                .extracting(row -> toLong(row[0]) * 100 + toLong(row[1]))
                .isSorted();
        }

        /** Une facture soldée reste au facturé du mois : c'est bien du chiffre d'affaires émis. */
        @Test
        @DisplayName("une facture soldée compte au facturé mais plus à l'encours")
        void factureSoldee() {
            TiersPayant payeur = payeur("CNAM", 30);
            LocalDate mois = LocalDate.now().withDayOfMonth(1).plusDays(3);
            Object[] avant = moisDe(LocalDate.now().getYear(), LocalDate.now().getMonthValue());
            long factureAvant = avant != null ? toLong(avant[2]) : 0L;
            long encoursAvant = avant != null ? toLong(avant[3]) : 0L;

            facture(payeur, mois, 4_000_000, 4_000_000, InvoiceStatut.PAID);
            em.flush();

            Object[] apres = moisDe(LocalDate.now().getYear(), LocalDate.now().getMonthValue());

            assertThat(toLong(apres[2]) - factureAvant).isEqualTo(4_000_000L);
            assertThat(toLong(apres[3]) - encoursAvant).isZero();
        }

        private Object[] moisDe(int annee, int mois) {
            return repository
                .findEncoursMensuelEvolution()
                .stream()
                .filter(row -> toLong(row[0]) == annee && toLong(row[1]) == mois)
                .findFirst()
                .orElse(null);
        }
    }

    // ===== utilitaires =====

    private TiersPayant payeur(String nom, int delaiReglement) {
        TiersPayant payeur = tiersPayant(nom, groupeTiersPayant("GROUPE-" + nom));
        payeur.setDelaiReglement(delaiReglement);
        em.flush();
        return payeur;
    }

    /** Une ancienneté en jours, bornée à l'année en cours : {@code sales} est partitionnée par date. */
    private static LocalDate jourDeLAnnee(int anciennete) {
        LocalDate jour = LocalDate.now().minusDays(anciennete);
        LocalDate premierJanvier = LocalDate.now().withDayOfYear(1);
        return jour.isBefore(premierJanvier) ? premierJanvier : jour;
    }

    private static long ecart(Object[] avant, Object[] apres, int colonne) {
        return toLong(apres[colonne]) - toLong(avant[colonne]);
    }

    private static long toLong(Object valeur) {
        return valeur != null ? ((Number) valeur).longValue() : 0L;
    }
}
