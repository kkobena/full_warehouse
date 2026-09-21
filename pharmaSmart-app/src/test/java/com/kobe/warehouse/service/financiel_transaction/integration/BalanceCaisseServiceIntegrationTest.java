package com.kobe.warehouse.service.financiel_transaction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.config.JacksonConfiguration;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.declaration_ca.ModeChiffreAffaireResolver;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;
import com.kobe.warehouse.service.financiel_transaction.BalanceCaisseServiceImpl;
import com.kobe.warehouse.service.financiel_transaction.BalanceReportReportService;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.ModeChiffreAffaire;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La balance de caisse n'est pas calculée en Java : une fonction stockée agrège les ventes et rend un
 * document JSON, que le service relit avec Jackson avant de le totaliser.
 *
 * <p>Ce détour crée un contrat que rien ne garde. Les clés que la fonction émet doivent correspondre
 * aux propriétés du DTO, et Jackson est configuré pour <b>ignorer ce qu'il ne reconnaît pas</b> : une
 * clé renommée d'un côté sans l'autre ne lève aucune erreur, elle fait disparaître un montant de
 * l'état. Le service enveloppe par ailleurs la lecture dans un {@code catch} qui rend une liste vide,
 * si bien qu'un échec de désérialisation se présente comme une journée sans vente.
 *
 * <p>Seule une exécution contre un vrai PostgreSQL, sur des ventes réelles, permet de le voir.
 */
@DisplayName("BalanceCaisseService — balance lue sur PostgreSQL")
class BalanceCaisseServiceIntegrationTest extends AbstractFinancialTransactionIntegrationTest {

    private BalanceCaisseServiceImpl service;

    @BeforeEach
    void cablerLeService() {
        // Les repositories viennent du contexte d'intégration : les construire à la main obligerait
        // à redériver toutes les requêtes déduites des noms de méthodes.
        SalesRepository salesRepository = IntegrationPostgresDatabase.bean(SalesRepository.class);
        PaymentTransactionRepository paymentTransactionRepository = IntegrationPostgresDatabase.bean(PaymentTransactionRepository.class);

        ModeChiffreAffaireResolver resolver = mock(ModeChiffreAffaireResolver.class);
        when(resolver.resoudre(any())).thenReturn(ModeChiffreAffaire.REEL);

        // L'ObjectMapper de l'application, et non un mapper nu : c'est lui qui porte le module
        // temps de Java et l'indulgence aux clés inconnues. Un mapper reconstruit à la main ne lit
        // pas les dates et ferait échouer la désérialisation pour une raison absente de la production.
        ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

        service = new BalanceCaisseServiceImpl(
            mock(BalanceReportReportService.class),
            salesRepository,
            paymentTransactionRepository,
            objectMapper,
            resolver
        );
    }

    // ===== lecture du document JSON =====

    @Nested
    @DisplayName("Lecture de la fonction stockée")
    class LectureDeLaFonctionStockee {

        /**
         * Le premier enjeu n'est pas la valeur mais l'existence : si la désérialisation échoue, le
         * service rend une liste vide et l'écran affiche une journée blanche, sans erreur.
         */
        @Test
        @DisplayName("une vente comptant apparaît dans la balance")
        void venteComptantPresente() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 0, "CASH");

            BalanceCaisseWrapper balance = service.getBalanceCaisse(parametre());

            assertThat(balance.getBalanceCaisses()).isNotEmpty();
            assertThat(balance.getMontantTtc()).isGreaterThanOrEqualTo(10_000L);
        }

        @Test
        @DisplayName("le nombre de ventes et le chiffre d'affaires sont repris")
        void nombreEtChiffre() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 0, "CASH");
            venteEncaissee(aujourdHui(), produit, 5, 0, "CASH");

            BalanceCaisseWrapper balance = service.getBalanceCaisse(parametre());

            assertThat(balance.getCount()).isGreaterThanOrEqualTo(2L);
            assertThat(balance.getMontantTtc()).isGreaterThanOrEqualTo(15_000L);
        }

        @Test
        @DisplayName("la vente comptant se range dans sa catégorie")
        void categorieDeLaVente() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 0, "CASH");

            assertThat(service.getBalanceCaisse(parametre()).getBalanceCaisses())
                .extracting(BalanceCaisseDTO::getTypeSale)
                .contains(TypeVenteDTO.CashSale);
        }

        @Test
        @DisplayName("le coût d'achat des produits vendus est repris")
        void coutDAchat() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 0, "CASH");

            // Coût unitaire six cents, dix unités.
            assertThat(service.getBalanceCaisse(parametre()).getMontantAchat()).isGreaterThanOrEqualTo(6_000L);
        }

        /** Une journée sans vente doit rendre une balance à zéro, non une balance absente. */
        @Test
        @DisplayName("une journée sans vente rend une balance nulle sans échouer")
        void journeeSansVente() {
            LocalDate demain = LocalDate.now().plusDays(1);

            BalanceCaisseWrapper balance = service.getBalanceCaisse(parametre(demain, demain.plusDays(1)));

            assertThat(balance.getMontantTtc()).isZero();
            assertThat(balance.getCount()).isZero();
            assertThat(balance.getBalanceCaisses()).isEmpty();
        }
    }

    // ===== remises =====

    @Nested
    @DisplayName("Remises accordées")
    class RemisesAccordees {

        /**
         * La remise est ce qui distingue le chiffre d'affaires brut du net. La fonction stockée
         * l'émet, le service la totalise : encore faut-il qu'elle survive au passage par le JSON.
         */
        @Test
        @DisplayName("la remise accordée sur une vente se retrouve dans la balance")
        void remiseReportee() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 1_500, "CASH");

            BalanceCaisseWrapper balance = service.getBalanceCaisse(parametre());

            assertThat(balance.getMontantDiscount()).isEqualTo(1_500L);
        }

        @Test
        @DisplayName("le chiffre net est le brut diminué de la remise")
        void netApresRemise() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 1_500, "CASH");

            BalanceCaisseWrapper balance = service.getBalanceCaisse(parametre());

            assertThat(balance.getMontantNet()).isEqualTo(balance.getMontantTtc() - balance.getMontantDiscount());
        }

        @Test
        @DisplayName("une vente sans remise ne crée pas de remise")
        void venteSansRemise() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 0, "CASH");

            assertThat(service.getBalanceCaisse(parametre()).getMontantDiscount()).isZero();
        }
    }

    // ===== règlements =====

    @Nested
    @DisplayName("Ventilation des règlements")
    class VentilationDesReglements {

        @Test
        @DisplayName("un règlement en espèces alimente la ligne des espèces")
        void reglementEspeces() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 0, "CASH");

            assertThat(service.getBalanceCaisse(parametre()).getMontantCash()).isGreaterThanOrEqualTo(10_000L);
        }

        @Test
        @DisplayName("un règlement par carte alimente la ligne des cartes")
        void reglementCarte() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 0, "CB");

            assertThat(service.getBalanceCaisse(parametre()).getMontantCard()).isGreaterThanOrEqualTo(10_000L);
        }

        /** Les mouvements de caisse portent la clé de leur type : le gabarit y distingue les sorties. */
        @Test
        @DisplayName("les mouvements de caisse portent leur type en clé")
        void mouvementsDeCaisse() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 0, "CASH");

            assertThat(service.getBalanceCaisse(parametre()).getMvtCaisses())
                .allSatisfy(mvt -> {
                    assertThat(mvt.key()).isNotBlank();
                    assertThat(mvt.libelle()).isNotBlank();
                });
        }
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre retenu")
    class PerimetreRetenu {

        @Test
        @DisplayName("une vente hors période ne compte pas")
        void venteHorsPeriode() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 0, "CASH");

            LocalDate demain = LocalDate.now().plusDays(1);

            assertThat(service.getBalanceCaisse(parametre(demain, demain.plusDays(1))).getMontantTtc()).isZero();
        }

        /** La balance retient les ventes closes et les annulées : les annulations doivent se voir. */
        @Test
        @DisplayName("la balance retient les ventes closes et les annulées")
        void statutsRetenus() {
            MvtParam param = parametre();

            service.getBalanceCaisse(param);

            assertThat(param.getStatuts()).containsExactlyInAnyOrder(SalesStatut.CLOSED, SalesStatut.CANCELED);
        }

        @Test
        @DisplayName("la période analysée est reportée en clair sur la balance")
        void periodeReportee() {
            LocalDate jour = aujourdHui();

            assertThat(service.getBalanceCaisse(parametre(jour, jour)).getPeriode()).contains("Du ").contains(" au ");
        }
    }

    // ===== utilitaires =====

    private MvtParam parametre() {
        return parametre(aujourdHui(), aujourdHui());
    }

    private MvtParam parametre(LocalDate debut, LocalDate fin) {
        MvtParam param = new MvtParam();
        param.setFromDate(debut);
        param.setToDate(fin);
        param.setStatuts(Set.of(SalesStatut.CLOSED));
        return param.build();
    }
}
