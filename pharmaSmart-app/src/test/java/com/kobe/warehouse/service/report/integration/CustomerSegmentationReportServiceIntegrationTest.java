package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.report.CustomerSegmentationDTO;
import com.kobe.warehouse.service.dto.report.CustomerSegmentationDTO.CustomerClassification;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La segmentation RFM note chaque client sur trois axes — depuis combien de temps il n'est pas venu
 * (<i>recency</i>), combien de fois il est venu dans l'année (<i>frequency</i>), combien il a dépensé
 * (<i>monetary</i>) — puis en déduit une étiquette : champion, fidèle, gros panier, actif, à risque,
 * à réveiller, dormant. C'est de cette étiquette que dépendent les relances commerciales.
 *
 * <p>Le calcul vit dans {@code mv_customer_rfm} et se joue entièrement sur des bornes : 30 / 60 / 90
 * / 180 jours pour la récence, 2 / 5 / 10 / 20 visites pour la fréquence, 50 000 / 100 000 / 200 000
 * / 500 000 pour le montant. Une borne mal placée ne se voit pas — elle range simplement le client
 * dans la mauvaise case, et la relance part au mauvais moment ou pas du tout.
 *
 * <p>C'est la <b>récence</b> qui pèse le plus : elle seule peut faire basculer un client de
 * {@code CHAMPION} à {@code INACTIVE}, et toutes les branches de la classification commencent par
 * elle.
 */
@DisplayName("CustomerSegmentationReportService — segmentation RFM des clients")
class CustomerSegmentationReportServiceIntegrationTest extends AbstractReportIntegrationTest {

    private static final String VUE = "mv_customer_rfm";

    // ===== scores =====

    @Nested
    @DisplayName("Scores RFM")
    class Scores {

        /**
         * La récence est éprouvée sur ses quatre bornes. Un client parti depuis quatre mois doit
         * obtenir 2, pas 5 : c'est cette note qui décide s'il figure dans la liste des clients à
         * relancer, et une note trop généreuse le rend invisible.
         */
        @Test
        @DisplayName("la note de récence décroît avec la durée d'absence")
        void noteDeRecence() {
            visite(client("TRES RECENT", "A", "01"), ilYA(10), 10_000);
            visite(client("RECENT", "B", "02"), ilYA(45), 10_000);
            visite(client("TIEDE", "C", "03"), ilYA(75), 10_000);
            visite(client("FROID", "D", "04"), ilYA(120), 10_000);
            visite(client("GLACE", "E", "05"), ilYA(300), 10_000);
            rafraichir(VUE);

            assertThat(noteRecence("A TRES RECENT")).isEqualTo(5);
            assertThat(noteRecence("B RECENT")).isEqualTo(4);
            assertThat(noteRecence("C TIEDE")).isEqualTo(3);
            assertThat(noteRecence("D FROID")).isEqualTo(2);
            assertThat(noteRecence("E GLACE")).isEqualTo(1);
        }

        @Test
        @DisplayName("la note de fréquence suit le nombre de visites de l'année")
        void noteDeFrequence() {
            visiteurRegulier("UNE FOIS", "A", 1);
            visiteurRegulier("DEUX FOIS", "B", 2);
            visiteurRegulier("CINQ FOIS", "C", 5);
            visiteurRegulier("DIX FOIS", "D", 10);
            rafraichir(VUE);

            assertThat(noteFrequence("A UNE FOIS")).isEqualTo(1);
            assertThat(noteFrequence("B DEUX FOIS")).isEqualTo(2);
            assertThat(noteFrequence("C CINQ FOIS")).isEqualTo(3);
            assertThat(noteFrequence("D DIX FOIS")).isEqualTo(4);
        }

        @Test
        @DisplayName("la note de montant suit la dépense de l'année")
        void noteDeMontant() {
            visite(client("PETIT", "A", "01"), ilYA(10), 10_000);
            visite(client("MOYEN", "B", "02"), ilYA(10), 120_000);
            visite(client("GROS", "C", "03"), ilYA(10), 600_000);
            rafraichir(VUE);

            assertThat(noteMontant("A PETIT")).isEqualTo(1);
            assertThat(noteMontant("B MOYEN")).isEqualTo(3);
            assertThat(noteMontant("C GROS")).isEqualTo(5);
        }

        /** Le segment condense les trois notes en un nombre : c'est la clé de tri de l'écran. */
        @Test
        @DisplayName("le segment condense les trois notes en un seul nombre")
        void segmentCondenseLesTroisNotes() {
            visite(client("CLIENT", "A", "01"), ilYA(10), 600_000);
            rafraichir(VUE);

            CustomerSegmentationDTO client = segmentation("A CLIENT");

            assertThat(client.rfmSegment())
                .isEqualTo(client.recencyScore() * 100 + client.frequencyScore() * 10 + client.monetaryScore());
        }
    }

    // ===== classification =====

    @Nested
    @DisplayName("Classification")
    class Classification {

        @Test
        @DisplayName("un client récent, fréquent et dépensier est un champion")
        void champion() {
            visiteurRegulier("CHAMPION", "A", 12, 50_000);
            rafraichir(VUE);

            assertThat(segmentation("A CHAMPION").customerClassification()).isEqualTo(CustomerClassification.CHAMPION);
            assertThat(services.customerSegmentationReportService.getChampionCustomers())
                .extracting(CustomerSegmentationDTO::customerName)
                .containsExactly("A CHAMPION");
        }

        @Test
        @DisplayName("un client récent et fréquent mais peu dépensier est fidèle")
        void fidele() {
            visiteurRegulier("FIDELE", "A", 6, 2_000);
            rafraichir(VUE);

            assertThat(segmentation("A FIDELE").customerClassification()).isEqualTo(CustomerClassification.LOYAL);
        }

        @Test
        @DisplayName("un client récent et dépensier mais rare est un gros panier")
        void grosPanier() {
            visite(client("GROS PANIER", "A", "01"), ilYA(10), 600_000);
            rafraichir(VUE);

            assertThat(segmentation("A GROS PANIER").customerClassification()).isEqualTo(CustomerClassification.BIG_SPENDER);
        }

        @Test
        @DisplayName("un client récent sans autre qualité est simplement actif")
        void actif() {
            visite(client("ACTIF", "A", "01"), ilYA(10), 10_000);
            rafraichir(VUE);

            assertThat(segmentation("A ACTIF").customerClassification()).isEqualTo(CustomerClassification.ACTIVE);
        }

        /**
         * Entre deux et trois mois d'absence, le client n'est pas encore perdu mais il faut agir :
         * c'est la fenêtre où une relance a encore une chance d'aboutir.
         */
        @Test
        @DisplayName("un client absent depuis deux à trois mois est à risque")
        void aRisque() {
            visite(client("A RISQUE", "A", "01"), ilYA(75), 10_000);
            rafraichir(VUE);

            assertThat(segmentation("A A RISQUE").customerClassification()).isEqualTo(CustomerClassification.AT_RISK);
        }

        /** Un bon client parti depuis longtemps vaut mieux qu'un inconnu : il mérite qu'on le rappelle. */
        @Test
        @DisplayName("un ancien bon client parti depuis longtemps est à réveiller")
        void aReveiller() {
            visiteurRegulier("A REVEILLER", "A", 6, 10_000, 150);
            rafraichir(VUE);

            assertThat(segmentation("A A REVEILLER").customerClassification()).isEqualTo(CustomerClassification.NEED_ATTENTION);
        }

        @Test
        @DisplayName("un client rare et parti depuis longtemps est dormant")
        void dormant() {
            visite(client("DORMANT", "A", "01"), ilYA(200), 10_000);
            rafraichir(VUE);

            assertThat(segmentation("A DORMANT").customerClassification()).isEqualTo(CustomerClassification.INACTIVE);
        }
    }

    // ===== lectures =====

    @Nested
    @DisplayName("Lectures")
    class Lectures {

        @Test
        @DisplayName("le filtre par classification ne rend que cette classe")
        void filtreParClassification() {
            visiteurRegulier("CHAMPION", "A", 12, 50_000);
            visite(client("DORMANT", "B", "02"), ilYA(200), 10_000);
            rafraichir(VUE);

            assertThat(services.customerSegmentationReportService.getCustomersByClassification(CustomerClassification.INACTIVE))
                .extracting(CustomerSegmentationDTO::customerName)
                .containsExactly("B DORMANT");
        }

        /**
         * Les deux classes menacées se lisent ensemble : ce sont les clients qu'on peut encore
         * récupérer, par opposition aux dormants qu'on a déjà perdus.
         */
        @Test
        @DisplayName("les clients menacés regroupent « à risque » et « à réveiller »")
        void clientsMenaces() {
            visite(client("A RISQUE", "A", "01"), ilYA(75), 10_000);
            visiteurRegulier("A REVEILLER", "B", 6, 10_000, 150);
            visite(client("DORMANT", "C", "03"), ilYA(200), 10_000);
            visiteurRegulier("CHAMPION", "D", 12, 50_000);
            rafraichir(VUE);

            assertThat(services.customerSegmentationReportService.getAtRiskCustomers())
                .extracting(CustomerSegmentationDTO::customerName)
                .containsExactlyInAnyOrder("A A RISQUE", "B A REVEILLER");
        }

        @Test
        @DisplayName("les compteurs recensent chaque classe, y compris celles à zéro")
        void compteursParClassification() {
            visite(client("DORMANT A", "A", "01"), ilYA(200), 10_000);
            visite(client("DORMANT B", "B", "02"), ilYA(250), 10_000);
            visite(client("ACTIF", "C", "03"), ilYA(10), 10_000);
            rafraichir(VUE);

            Map<CustomerClassification, Long> compteurs = services.customerSegmentationReportService.getCustomerCountByClassification();

            assertThat(compteurs)
                .containsEntry(CustomerClassification.INACTIVE, 2L)
                .containsEntry(CustomerClassification.ACTIVE, 1L)
                .containsEntry(CustomerClassification.CHAMPION, 0L);
        }

        @Test
        @DisplayName("la fiche d'un client porte son identité et ses coordonnées")
        void ficheClient() {
            UninsuredCustomer client = client("KOUASSI", "Jean", "0102030405");
            visite(client, ilYA(10), 10_000);
            rafraichir(VUE);

            CustomerSegmentationDTO fiche = services.customerSegmentationReportService.getCustomerSegmentation(client.getId());

            assertThat(fiche.customerName()).isEqualTo("Jean KOUASSI");
            assertThat(fiche.phone()).isEqualTo("0102030405");
            assertThat(fiche.lastPurchaseDate()).isEqualTo(ilYA(10));
            assertThat(fiche.nbPurchasesLastYear()).isEqualTo(1);
            assertThat(fiche.totalSpentLastYear()).isEqualTo(10_000);
        }

        @Test
        @DisplayName("un client inconnu de la segmentation ne rend rien plutôt qu'une erreur")
        void clientInconnu() {
            rafraichir(VUE);

            assertThat(services.customerSegmentationReportService.getCustomerSegmentation(999_999)).isNull();
        }

        @Test
        @DisplayName("base vierge : aucun client segmenté, et des compteurs à zéro")
        void baseVierge() {
            rafraichir(VUE);

            assertThat(services.customerSegmentationReportService.getAllCustomerSegmentation()).isEmpty();
            assertThat(services.customerSegmentationReportService.getCustomerCountByClassification())
                .containsEntry(CustomerClassification.CHAMPION, 0L)
                .containsEntry(CustomerClassification.INACTIVE, 0L);
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        @Test
        @DisplayName("une vente de plus d'un an ne compte plus dans la segmentation")
        void venteHorsFenetreExclue() {
            visite(client("ANCIEN", "A", "01"), LocalDate.now().minusMonths(14), 10_000);
            rafraichir(VUE);

            assertThat(services.customerSegmentationReportService.getAllCustomerSegmentation()).isEmpty();
        }

        @Test
        @DisplayName("une vente annulée ne compte pas")
        void venteAnnuleeExclue() {
            UninsuredCustomer client = client("KOUASSI", "Jean", "01");
            venteFermee(ilYA(10), true, CategorieChiffreAffaire.CA, client);
            rafraichir(VUE);

            assertThat(services.customerSegmentationReportService.getAllCustomerSegmentation()).isEmpty();
        }

        @Test
        @DisplayName("une vente anonyme ne crée aucun segment")
        void venteAnonymeExclue() {
            venteFermee(ilYA(10), false, CategorieChiffreAffaire.CA);
            rafraichir(VUE);

            assertThat(services.customerSegmentationReportService.getAllCustomerSegmentation()).isEmpty();
        }
    }

    // ===== fabriques locales =====

    private static LocalDate ilYA(int jours) {
        return LocalDate.now().minusDays(jours);
    }

    private CustomerSegmentationDTO segmentation(String nom) {
        return services.customerSegmentationReportService
            .getAllCustomerSegmentation()
            .stream()
            .filter(c -> nom.equals(c.customerName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("client absent de la segmentation : " + nom));
    }

    private int noteRecence(String nom) {
        return segmentation(nom).recencyScore();
    }

    private int noteFrequence(String nom) {
        return segmentation(nom).frequencyScore();
    }

    private int noteMontant(String nom) {
        return segmentation(nom).monetaryScore();
    }

    private void visite(UninsuredCustomer client, LocalDate date, int montant) {
        var vente = venteFermee(date, false, CategorieChiffreAffaire.CA, client);
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setPayrollAmount(montant);
        em.flush();
    }

    private void visiteurRegulier(String nom, String prenom, int nombreDeVisites) {
        visiteurRegulier(nom, prenom, nombreDeVisites, 10_000);
    }

    private void visiteurRegulier(String nom, String prenom, int nombreDeVisites, int montantParVisite) {
        visiteurRegulier(nom, prenom, nombreDeVisites, montantParVisite, 10);
    }

    /**
     * Un client venu {@code nombreDeVisites} fois, la dernière il y a {@code joursDepuisLaDerniere}
     * jours et les précédentes réparties dans l'année écoulée.
     */
    private void visiteurRegulier(
        String nom,
        String prenom,
        int nombreDeVisites,
        int montantParVisite,
        int joursDepuisLaDerniere
    ) {
        UninsuredCustomer client = client(nom, prenom, "01");
        for (int i = 0; i < nombreDeVisites; i++) {
            visite(client, ilYA(joursDepuisLaDerniere + i * 15), montantParVisite);
        }
    }
}
