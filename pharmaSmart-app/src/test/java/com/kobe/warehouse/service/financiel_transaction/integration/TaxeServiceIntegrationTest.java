package com.kobe.warehouse.service.financiel_transaction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.config.JacksonConfiguration;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.declaration_ca.ModeChiffreAffaireResolver;
import com.kobe.warehouse.service.financiel_transaction.DeclarationTvaPdfReportService;
import com.kobe.warehouse.service.financiel_transaction.TaxeService;
import com.kobe.warehouse.service.financiel_transaction.TaxeServiceImpl;
import com.kobe.warehouse.service.financiel_transaction.TvaReportReportService;
import com.kobe.warehouse.service.financiel_transaction.dto.ModeChiffreAffaire;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * L'état de TVA est une pièce fiscale : il ventile le chiffre d'affaires par taux, et la déclaration
 * en déduit la TVA collectée, la TVA déductible sur achats, et le solde à verser.
 *
 * <p>Comme la balance de caisse, il ne se calcule pas en Java : une fonction stockée agrège les
 * ventes et rend un document JSON que le service relit avec Jackson, clés ignorées quand elles ne
 * correspondent à rien, lecture enveloppée dans un {@code catch} qui rend une liste vide. Une
 * ventilation de TVA muette et un exercice sans vente se présentent donc de la même façon.
 *
 * <p>C'est la raison d'être de ces tests : vérifier sur de vraies ventes, à des taux différents, que
 * chaque montant de l'état arrive bien jusqu'au DTO.
 */
@DisplayName("TaxeService — état de TVA lu sur PostgreSQL")
class TaxeServiceIntegrationTest extends AbstractFinancialTransactionIntegrationTest {

    private TaxeService service;

    @BeforeEach
    void cablerLeService() {
        ModeChiffreAffaireResolver resolver = mock(ModeChiffreAffaireResolver.class);
        when(resolver.resoudre(any())).thenReturn(ModeChiffreAffaire.REEL);

        // L'ObjectMapper de l'application, et non un mapper nu : c'est lui qui porte le module
        // temps de Java et l'indulgence aux clés inconnues. Un mapper reconstruit à la main ne lit
        // pas les dates et ferait échouer la désérialisation pour une raison absente de la production.
        ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

        service = new TaxeServiceImpl(
            mock(TvaReportReportService.class),
            mock(DeclarationTvaPdfReportService.class),
            IntegrationPostgresDatabase.bean(SalesRepository.class),
            mock(AppConfigurationService.class),
            objectMapper,
            resolver
        );
    }

    // ===== ventilation par taux =====

    @Nested
    @DisplayName("Ventilation par taux")
    class VentilationParTaux {

        /**
         * Le premier enjeu n'est pas la valeur mais l'existence : une désérialisation en échec rend
         * une liste vide, et l'état se présente comme un exercice sans vente.
         */
        @Test
        @DisplayName("une vente apparaît dans l'état de TVA")
        void ventePresente() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 0, "CASH");

            TaxeWrapperDTO etat = service.fetchTaxe(parametre(), false);

            assertThat(etat).isNotNull();
            assertThat(etat.getTaxes()).isNotEmpty();
            assertThat(etat.getMontantTtc()).isGreaterThanOrEqualTo(10_000L);
        }

        @Test
        @DisplayName("chaque taux forme sa propre ligne")
        void uneligneParTaux() {
            venteEncaissee(aujourdHui(), produit(unique("EXONERE"), 1_000, 0), 10, 0, "CASH");
            venteEncaissee(aujourdHui(), produit(unique("TAXE"), 1_000, 18), 10, 0, "CASH");

            assertThat(service.fetchTaxe(parametre(), false).getTaxes())
                .extracting(TaxeDTO::getCodeTva)
                .contains(0, 18);
        }

        /** La taxe est l'écart entre le TTC et le HT : c'est elle que l'officine collecte. */
        @Test
        @DisplayName("la taxe collectée est l'écart entre le TTC et le HT")
        void taxeCollectee() {
            venteEncaissee(aujourdHui(), produit(unique("TAXE"), 1_000, 18), 10, 0, "CASH");

            TaxeWrapperDTO etat = service.fetchTaxe(parametre(), false);

            assertThat(etat.getMontantTaxe()).isEqualTo(etat.getMontantTtc() - etat.getMontantHt());
        }

        @Test
        @DisplayName("le coût d'achat des produits vendus est repris")
        void coutDAchat() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 18), 10, 0, "CASH");

            // Coût unitaire six cents, dix unités.
            assertThat(service.fetchTaxe(parametre(), false).getMontantAchat()).isGreaterThanOrEqualTo(6_000L);
        }

        @Test
        @DisplayName("la remise accordée est reprise")
        void remiseReprise() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 18), 10, 1_500, "CASH");

            assertThat(service.fetchTaxe(parametre(), false).getMontantRemise()).isGreaterThanOrEqualTo(1_500L);
        }

        /** Sans vente, il n'y a pas d'état : le service le dit en rendant rien. */
        @Test
        @DisplayName("un exercice sans vente ne rend pas d'état")
        void exerciceSansVente() {
            LocalDate demain = LocalDate.now().plusDays(1);

            assertThat(service.fetchTaxe(parametre(demain, demain.plusDays(1)), false)).isNull();
        }

        @Test
        @DisplayName("une vente hors période ne compte pas")
        void venteHorsPeriode() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 18), 10, 0, "CASH");

            LocalDate demain = LocalDate.now().plusDays(1);

            assertThat(service.fetchTaxe(parametre(demain, demain.plusDays(1)), false)).isNull();
        }
    }

    // ===== graphique =====

    @Nested
    @DisplayName("Graphique de répartition")
    class GraphiqueDeRepartition {

        /** L'export PDF n'a pas besoin du graphique : il ne le demande pas, et ne doit pas le payer. */
        @Test
        @DisplayName("l'état d'écran porte son graphique, l'export non")
        void graphiqueSelonLUsage() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 18), 10, 0, "CASH");

            assertThat(service.fetchTaxe(parametre(), false).getChart()).isNotNull();
            assertThat(service.fetchTaxe(parametre(), true).getChart()).isNull();
        }

        @Test
        @DisplayName("le graphique porte un libellé et un montant par taux")
        void contenuDuGraphique() {
            venteEncaissee(aujourdHui(), produit(unique("EXONERE"), 1_000, 0), 10, 0, "CASH");
            venteEncaissee(aujourdHui(), produit(unique("TAXE"), 1_000, 18), 10, 0, "CASH");

            var graphique = service.fetchTaxe(parametre(), false).getChart();

            assertThat(graphique.labeles()).hasSameSizeAs(graphique.data());
            assertThat(graphique.labeles()).contains("0", "18");
        }
    }

    // ===== déclaration =====

    @Nested
    @DisplayName("Déclaration de TVA")
    class DeclarationDeTva {

        /**
         * La TVA déductible s'obtient en appliquant à chaque ligne d'achat le taux de sa ligne de
         * vente. C'est le calcul qui décide du montant versé à l'administration.
         */
        @Test
        @DisplayName("la TVA déductible applique le taux de la ligne au coût d'achat")
        void tvaDeductible() {
            venteEncaissee(aujourdHui(), produit(unique("TAXE"), 1_000, 18), 10, 0, "CASH");

            TaxeWrapperDTO declaration = service.fetchDeclarationTva(parametre());

            // Dix-huit pour cent de six mille d'achat.
            assertThat(declaration.getTvaDeductible()).isGreaterThanOrEqualTo(1_080L);
        }

        @Test
        @DisplayName("la TVA nette est la collectée diminuée de la déductible")
        void tvaNette() {
            venteEncaissee(aujourdHui(), produit(unique("TAXE"), 1_000, 18), 10, 0, "CASH");

            TaxeWrapperDTO declaration = service.fetchDeclarationTva(parametre());

            assertThat(declaration.getTvaNette()).isEqualTo(declaration.getMontantTaxe() - declaration.getTvaDeductible());
        }

        /** Un produit exonéré ne donne aucune TVA, ni collectée ni déductible. */
        @Test
        @DisplayName("un produit exonéré ne produit aucune TVA déductible")
        void produitExonere() {
            venteEncaissee(aujourdHui(), produit(unique("EXONERE"), 1_000, 0), 10, 0, "CASH");

            assertThat(service.fetchDeclarationTva(parametre()).getTvaDeductible()).isZero();
        }

        /**
         * Une déclaration doit toujours exister, même sur un exercice vide : l'officine doit pouvoir
         * déposer un état à zéro plutôt que rien du tout.
         */
        @Test
        @DisplayName("un exercice sans vente rend une déclaration à zéro, non rien")
        void exerciceSansVente() {
            LocalDate demain = LocalDate.now().plusDays(1);

            TaxeWrapperDTO declaration = service.fetchDeclarationTva(parametre(demain, demain.plusDays(1)));

            assertThat(declaration).isNotNull();
            assertThat(declaration.getMontantTaxe()).isZero();
            assertThat(declaration.getTvaDeductible()).isZero();
            assertThat(declaration.getTvaNette()).isZero();
        }
    }

    // ===== regroupement journalier =====

    @Nested
    @DisplayName("Regroupement journalier")
    class RegroupementJournalier {

        /** L'état journalier sert au rapprochement quotidien : chaque journée garde sa ligne. */
        @Test
        @DisplayName("l'état journalier date chacune de ses lignes")
        void lignesDatees() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 18), 10, 0, "CASH");

            MvtParam param = parametre();
            param.setGroupeBy("daily");

            TaxeWrapperDTO etat = service.fetchTaxe(param, false);

            assertThat(etat.isGroupDate()).isTrue();
            assertThat(etat.getTaxes()).allSatisfy(taxe -> assertThat(taxe.getMvtDate()).isNotNull());
        }

        @Test
        @DisplayName("l'état d'ensemble ne date pas ses lignes")
        void lignesNonDatees() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 18), 10, 0, "CASH");

            assertThat(service.fetchTaxe(parametre(), false).isGroupDate()).isFalse();
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
