package com.kobe.warehouse.service.mobile.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.mobile.DailySalesDTO;
import com.kobe.warehouse.service.dto.mobile.ForecastRequestDTO;
import com.kobe.warehouse.service.mobile.MobileForecastService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Ce service prépare la série de ventes journalières que le téléphone donne à manger à son modèle de
 * prévision. Une série est un objet plus exigeant qu'une liste : un modèle de série temporelle
 * suppose un pas de temps régulier, et un jour manquant n'y est pas « rien » mais un décalage de
 * tout ce qui suit.
 *
 * <p>D'où les deux propriétés que ces tests vérifient sur PostgreSQL : la série est <b>continue</b>
 * — un dimanche de fermeture vaut zéro, il ne s'efface pas — et elle ne retient que le <b>chiffre
 * d'affaires réel</b>, à l'exclusion des ventes annulées et des ventes qui n'en relèvent pas.
 */
@DisplayName("MobileForecastService — série de ventes pour la prévision")
class MobileForecastServiceIntegrationTest extends AbstractMobileIntegrationTest {

    private MobileForecastService service;

    @BeforeEach
    void cablerLeService() {
        service = new MobileForecastService();
        ReflectionTestUtils.setField(service, "entityManager", em);
    }

    // ===== continuité de la série =====

    @Nested
    @DisplayName("Continuité de la série")
    class ContinuiteDeLaSerie {

        @Test
        @DisplayName("la série couvre chaque jour de la période, bornes comprises")
        void serieComplete() {
            LocalDate debut = LocalDate.now().minusDays(6);
            LocalDate fin = LocalDate.now();

            List<DailySalesDTO> serie = service.getDailySalesHistory(debut, fin);

            assertThat(serie).hasSize(7);
            assertThat(serie.getFirst().date()).isEqualTo(debut);
            assertThat(serie.getLast().date()).isEqualTo(fin);
        }

        /** Un jour de fermeture est une information : à zéro, pas absent. */
        @Test
        @DisplayName("un jour sans vente vaut zéro et ne disparaît pas de la série")
        void jourSansVente() {
            LocalDate debut = LocalDate.now().minusDays(4);
            venteFermee(debut, 100_000);
            venteFermee(debut.plusDays(2), 200_000);
            em.flush();

            List<DailySalesDTO> serie = service.getDailySalesHistory(debut, debut.plusDays(2));

            assertThat(serie).extracting(DailySalesDTO::date).containsExactly(debut, debut.plusDays(1), debut.plusDays(2));
            assertThat(serie.get(1).salesAmount()).isZero();
            assertThat(serie.get(1).transactionsCount()).isZero();
        }

        @Test
        @DisplayName("la série est ordonnée du plus ancien au plus récent")
        void serieOrdonnee() {
            LocalDate debut = LocalDate.now().minusDays(5);
            venteFermee(debut.plusDays(3), 300_000);
            venteFermee(debut, 100_000);
            em.flush();

            List<DailySalesDTO> serie = service.getDailySalesHistory(debut, LocalDate.now());

            assertThat(serie).extracting(DailySalesDTO::date).isSorted();
        }

        @Test
        @DisplayName("une période d'un seul jour rend un seul point")
        void periodeDUnJour() {
            LocalDate jour = LocalDate.now().minusDays(2);

            assertThat(service.getDailySalesHistory(jour, jour)).hasSize(1);
        }
    }

    // ===== contenu des points =====

    @Nested
    @DisplayName("Contenu des points")
    class ContenuDesPoints {

        @Test
        @DisplayName("un jour agrège le montant et le nombre de ventes")
        void agregationDuJour() {
            LocalDate jour = LocalDate.now().minusDays(3);
            venteFermee(jour, 300_000);
            venteFermee(jour, 200_000);
            em.flush();

            DailySalesDTO point = pointDu(jour);

            assertThat(point.salesAmount()).isEqualTo(500_000L);
            assertThat(point.transactionsCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("le panier moyen se déduit du montant et du nombre de ventes")
        void panierMoyen() {
            LocalDate jour = LocalDate.now().minusDays(3);
            venteFermee(jour, 300_000);
            venteFermee(jour, 200_000);
            em.flush();

            assertThat(pointDu(jour).averageBasket()).isEqualTo(250_000L);
        }

        @Test
        @DisplayName("un jour vide n'a pas de panier moyen plutôt qu'une division par zéro")
        void panierMoyenDUnJourVide() {
            LocalDate jour = LocalDate.now().minusDays(9);

            assertThat(pointDu(jour).averageBasket()).isZero();
        }

        @Test
        @DisplayName("les clients nominatifs distincts sont comptés")
        void clientsDistincts() {
            LocalDate jour = LocalDate.now().minusDays(3);
            var premier = client("DUPONT");
            venteFermee(jour, 100_000, false, CategorieChiffreAffaire.CA, premier);
            venteFermee(jour, 150_000, false, CategorieChiffreAffaire.CA, premier);
            venteFermee(jour, 200_000, false, CategorieChiffreAffaire.CA, client("KONE"));
            em.flush();

            assertThat(pointDu(jour).customersCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("le libellé du point porte sa date")
        void libelleDuPoint() {
            LocalDate jour = LocalDate.now().minusDays(3);

            assertThat(pointDu(jour).dateLabel()).isEqualTo(jour.toString());
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre du chiffre d'affaires")
    class PerimetreDuChiffreAffaires {

        /** Une vente annulée n'a jamais eu lieu : l'apprendre au modèle fausserait la prévision. */
        @Test
        @DisplayName("une vente annulée ne compte pas")
        void venteAnnulee() {
            LocalDate jour = LocalDate.now().minusDays(3);
            venteFermee(jour, 500_000, true, CategorieChiffreAffaire.CA, null);
            em.flush();

            assertThat(pointDu(jour).salesAmount()).isZero();
        }

        @Test
        @DisplayName("une vente hors chiffre d'affaires ne compte pas")
        void venteHorsChiffreAffaires() {
            LocalDate jour = LocalDate.now().minusDays(3);
            venteFermee(jour, 500_000, false, CategorieChiffreAffaire.CA_DEPOT, null);
            em.flush();

            assertThat(pointDu(jour).salesAmount()).isZero();
        }

        @Test
        @DisplayName("une vente hors période ne compte pas")
        void venteHorsPeriode() {
            LocalDate jour = LocalDate.now().minusDays(3);
            venteFermee(jour.minusDays(10), 500_000);
            em.flush();

            assertThat(service.getDailySalesHistory(jour, jour).getFirst().salesAmount()).isZero();
        }
    }

    // ===== demande par nombre de jours =====

    @Nested
    @DisplayName("Profondeur d'historique demandée")
    class ProfondeurDHistorique {

        @Test
        @DisplayName("la profondeur demandée fixe la longueur de la série")
        void profondeurDemandee() {
            assertThat(service.getDailySalesHistory(new ForecastRequestDTO(14, 7))).hasSize(15);
        }

        /** Une demande sans profondeur retombe sur trente jours, la valeur usuelle du modèle. */
        @Test
        @DisplayName("sans profondeur précisée, l'historique porte sur trente jours")
        void profondeurParDefaut() {
            assertThat(service.getDailySalesHistory(new ForecastRequestDTO(null, 7))).hasSize(31);
        }

        @Test
        @DisplayName("l'historique s'arrête au jour courant")
        void historiqueJusquAujourdHui() {
            List<DailySalesDTO> serie = service.getDailySalesHistory(new ForecastRequestDTO(7, 7));

            assertThat(serie.getLast().date()).isEqualTo(LocalDate.now());
        }
    }

    // ===== statistiques de qualité =====

    @Nested
    @DisplayName("Qualité de la série")
    class QualiteDeLaSerie {

        /**
         * Le modèle refuse de prédire sur une série trop courte ou trop erratique. Encore faut-il que
         * la requête qui l'évalue s'exécute : elle référençait un alias absent de sa clause FROM et
         * échouait à chaque appel.
         */
        @Test
        @DisplayName("les statistiques de la série s'obtiennent sans erreur")
        void statistiquesObtenues() {
            LocalDate jour = LocalDate.now().minusDays(3);
            venteFermee(jour, 300_000);
            em.flush();

            MobileForecastService.SalesStatistics statistiques = service.getSalesStatistics(30);

            assertThat(statistiques.daysWithSales()).isPositive();
            assertThat(statistiques.maxDailySales()).isGreaterThanOrEqualTo(statistiques.minDailySales());
        }

        @Test
        @DisplayName("les jours avec vente sont dénombrés")
        void joursAvecVente() {
            int avant = service.getSalesStatistics(30).daysWithSales();
            venteFermee(LocalDate.now().minusDays(3), 300_000);
            venteFermee(LocalDate.now().minusDays(3), 200_000);
            venteFermee(LocalDate.now().minusDays(4), 100_000);
            em.flush();

            assertThat(service.getSalesStatistics(30).daysWithSales() - avant).isEqualTo(2);
        }

        @Test
        @DisplayName("une série trop courte est jugée impropre à la prévision")
        void serieTropCourte() {
            MobileForecastService.SalesStatistics statistiques = new MobileForecastService.SalesStatistics(13, 100_000, 50_000, 150_000, 10_000);

            assertThat(statistiques.isSuitableForForecasting()).isFalse();
        }

        /** Une officine dont le chiffre varie du simple au décuple ne se prédit pas. */
        @Test
        @DisplayName("une série trop erratique est jugée impropre à la prévision")
        void serieErratique() {
            MobileForecastService.SalesStatistics statistiques = new MobileForecastService.SalesStatistics(30, 100_000, 0, 900_000, 250_000);

            assertThat(statistiques.coefficientOfVariation()).isEqualTo(2.5);
            assertThat(statistiques.isSuitableForForecasting()).isFalse();
        }

        @Test
        @DisplayName("une série longue et régulière est jugée exploitable")
        void serieExploitable() {
            MobileForecastService.SalesStatistics statistiques = new MobileForecastService.SalesStatistics(30, 100_000, 80_000, 120_000, 10_000);

            assertThat(statistiques.isSuitableForForecasting()).isTrue();
        }

        @Test
        @DisplayName("une officine sans vente n'a pas de variation plutôt qu'une division par zéro")
        void aucuneVente() {
            MobileForecastService.SalesStatistics statistiques = new MobileForecastService.SalesStatistics(0, 0, 0, 0, 0);

            assertThat(statistiques.coefficientOfVariation()).isZero();
            assertThat(statistiques.isSuitableForForecasting()).isFalse();
        }
    }

    // ===== utilitaires =====

    private DailySalesDTO pointDu(LocalDate jour) {
        return service.getDailySalesHistory(jour, jour).getFirst();
    }
}
