package com.kobe.warehouse.service.financiel_transaction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.JacksonConfiguration;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.AvoirFournisseurLineRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.declaration_ca.ModeChiffreAffaireResolver;
import com.kobe.warehouse.service.financiel_transaction.GroupeFournisseurManager;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienAggregator;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienCalculator;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienExportService;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienReportReportService;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienService;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienServiceImpl;
import com.kobe.warehouse.service.financiel_transaction.dto.ModeChiffreAffaire;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.PaymentDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.CommandeDataService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;

/**
 * Le tableau du pharmacien est l'état que le titulaire regarde chaque matin : une ligne par jour, le
 * comptant, le crédit, la remise, le montant net, le nombre de clients, puis les achats par grossiste.
 *
 * <p>Ses chiffres de vente ne sont pas calculés en Java mais par la fonction stockée
 * {@code tableau_pharmacien_report}, qui rend un document JSON relu par Jackson. Le contrat entre les
 * clés de ce document et les champs du DTO n'est écrit nulle part, et la lecture est enveloppée dans
 * un {@code catch} qui rend une liste vide : une clé renommée d'un côté, une erreur SQL, et l'état se
 * présente exactement comme une période sans vente. Rien ne distingue les deux à l'écran.
 *
 * <p>D'où ces tests : sur de vraies ventes, vérifier que chaque montant de la fonction arrive jusqu'au
 * tableau, et que les colonnes se recoupent entre elles.
 */
@DisplayName("TableauPharmacienService — tableau du pharmacien lu sur PostgreSQL")
class TableauPharmacienServiceIntegrationTest extends AbstractFinancialTransactionIntegrationTest {

    private TableauPharmacienService service;
    private CommandeDataService commandeDataService;
    private AppConfigurationService appConfigurationService;

    @BeforeEach
    void cablerLeService() {
        ModeChiffreAffaireResolver resolver = mock(ModeChiffreAffaireResolver.class);
        when(resolver.resoudre(any())).thenReturn(ModeChiffreAffaire.REEL);

        appConfigurationService = mock(AppConfigurationService.class);
        when(appConfigurationService.excludeFreeUnit()).thenReturn(false);

        commandeDataService = mock(CommandeDataService.class);
        when(commandeDataService.fetchReportTableauPharmacienData(any())).thenReturn(List.of());

        AvoirFournisseurLineRepository avoirRepository = mock(AvoirFournisseurLineRepository.class);
        when(avoirRepository.findByDateRange(any(), any())).thenReturn(List.of());
        when(avoirRepository.findByDateRangeGroupByMonth(any(), any())).thenReturn(List.of());

        TableauPharmacienCalculator calculator = new TableauPharmacienCalculator();

        service = new TableauPharmacienServiceImpl(
            IntegrationPostgresDatabase.bean(SalesRepository.class),
            commandeDataService,
            avoirRepository,
            appConfigurationService,
            // L'ObjectMapper de l'application : c'est lui qui porte le module temps de Java. Un
            // mapper nu ne saurait pas lire « mvtDate » et la lecture échouerait pour une raison
            // absente de la production — sans bruit, puisque l'échec rend une liste vide.
            new JacksonConfiguration().objectMapper(),
            new GroupeFournisseurManager(IntegrationPostgresDatabase.bean(FournisseurRepository.class)),
            calculator,
            new TableauPharmacienAggregator(calculator),
            mock(TableauPharmacienExportService.class),
            mock(TableauPharmacienReportReportService.class),
            resolver
        );
    }

    // ===== présence des ventes =====

    @Nested
    @DisplayName("Présence des ventes")
    class PresenceDesVentes {

        /**
         * Le premier enjeu n'est pas la valeur mais l'existence : une lecture en échec rend une liste
         * vide, et la journée se présente comme si l'officine n'avait rien vendu.
         */
        @Test
        @DisplayName("une vente du jour donne une ligne au tableau")
        void venteDuJour() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 0, "CASH");

            TableauPharmacienWrapper tableau = service.getTableauPharmacien(parametre());

            assertThat(tableau.getTableauPharmaciens()).hasSize(1);
            assertThat(tableau.getTableauPharmaciens().getFirst().getMvtDate()).isEqualTo(aujourdHui());
        }

        @Test
        @DisplayName("une période sans vente rend un tableau vide")
        void periodeSansVente() {
            LocalDate plusTard = aujourdHui().plusMonths(1);

            TableauPharmacienWrapper tableau = service.getTableauPharmacien(parametre(plusTard, plusTard.plusDays(1)));

            assertThat(tableau.getTableauPharmaciens()).isEmpty();
            assertThat(tableau.getMontantVenteTtc()).isZero();
        }

        @Test
        @DisplayName("chaque journée garde sa ligne")
        void uneLigneParJour() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui().minusDays(1), produit, 10, 0, "CASH");
            venteEncaissee(aujourdHui(), produit, 5, 0, "CASH");

            TableauPharmacienWrapper tableau = service.getTableauPharmacien(parametre(aujourdHui().minusDays(1), aujourdHui()));

            assertThat(tableau.getTableauPharmaciens())
                .extracting(TableauPharmacienDTO::getMvtDate)
                .containsExactly(aujourdHui().minusDays(1), aujourdHui());
        }

        /** Les lignes sortent triées : le tableau se lit du plus ancien au plus récent. */
        @Test
        @DisplayName("les lignes sont rendues dans l'ordre des dates")
        void lignesTriees() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 5, 0, "CASH");
            venteEncaissee(aujourdHui().minusDays(2), produit, 10, 0, "CASH");

            List<TableauPharmacienDTO> lignes = service
                .getTableauPharmacien(parametre(aujourdHui().minusDays(2), aujourdHui()))
                .getTableauPharmaciens();

            assertThat(lignes).extracting(TableauPharmacienDTO::getMvtDate).isSorted();
        }
    }

    // ===== montants de la journée =====

    @Nested
    @DisplayName("Montants de la journée")
    class MontantsDeLaJournee {

        @Test
        @DisplayName("le TTC est le prix de vente multiplié par la quantité")
        void montantTtc() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 0, "CASH");

            assertThat(ligneDuJour().getMontantTtc()).isEqualTo(10_000);
        }

        @Test
        @DisplayName("le HT se déduit du taux de TVA de la ligne")
        void montantHt() {
            venteEncaissee(aujourdHui(), produit(unique("TAXE"), 1_000, 18), 10, 0, "CASH");

            // Dix mille TTC à dix-huit pour cent.
            assertThat(ligneDuJour().getMontantHt()).isEqualTo(8_475);
        }

        @Test
        @DisplayName("le coût d'achat des produits vendus est repris")
        void montantAchat() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 0, "CASH");

            // Coût unitaire six cents, dix unités.
            assertThat(ligneDuJour().getMontantAchat()).isEqualTo(6_000);
        }

        @Test
        @DisplayName("la remise accordée est reprise")
        void montantRemise() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 1_500, "CASH");

            assertThat(ligneDuJour().getMontantRemise()).isEqualTo(1_500);
        }

        @Test
        @DisplayName("le comptant est ce qui a été encaissé")
        void montantComptant() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 1_500, "CASH");

            assertThat(ligneDuJour().getMontantComptant()).isEqualTo(8_500);
        }

        /**
         * Le défaut que ce test fixe : le montant net ajoutait la remise au TTC au lieu de l'en
         * retrancher. La colonne « Montant Net » du tableau dépassait donc le chiffre d'affaires
         * brut, de deux fois la remise accordée — et les deux ratios V/A et A/V, qui s'appuient sur
         * elle, étaient faux d'autant.
         */
        @Test
        @DisplayName("le montant net est le TTC diminué de la remise")
        void montantNet() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 1_500, "CASH");

            assertThat(ligneDuJour().getMontantNet()).isEqualTo(8_500);
        }

        /**
         * Les colonnes d'une même ligne doivent se recouper : ce que le titulaire lit en « Montant
         * Net » est la somme de ce qu'il a encaissé et de ce qui reste dû.
         */
        @Test
        @DisplayName("le montant net se recoupe avec le comptant et le crédit")
        void netEgalComptantPlusCredit() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 1_500, "CASH");

            TableauPharmacienDTO ligne = ligneDuJour();

            assertThat(ligne.getMontantNet()).isEqualTo(ligne.getMontantComptant() + ligne.getMontantCredit());
        }

        @Test
        @DisplayName("sans remise, le net est le TTC")
        void netSansRemise() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 0, "CASH");

            TableauPharmacienDTO ligne = ligneDuJour();

            assertThat(ligne.getMontantNet()).isEqualTo(10_000);
            assertThat(ligne.getMontantTtc()).isEqualTo(10_000);
        }

        @Test
        @DisplayName("le nombre de clients compte les ventes, non les lignes")
        void nombreVente() {
            Produit premier = produit(unique("DOLIPRANE"), 1_000, 0);
            Produit second = produit(unique("EFFERALGAN"), 2_000, 0);
            CashSale vente = venteFermee(aujourdHui(), 3_000, 0);
            ligneDeVente(vente, premier, 1, 0);
            ligneDeVente(vente, second, 1, 0);
            reglement(vente, caisseOuverte(), "CASH", 3_000);

            assertThat(ligneDuJour().getNombreVente()).isEqualTo(1);
        }

        @Test
        @DisplayName("deux ventes du même jour se cumulent sur une seule ligne")
        void deuxVentesCumulees() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui(), produit, 10, 0, "CASH");
            venteEncaissee(aujourdHui(), produit, 5, 0, "CASH");

            TableauPharmacienDTO ligne = ligneDuJour();

            assertThat(ligne.getMontantTtc()).isEqualTo(15_000);
            assertThat(ligne.getNombreVente()).isEqualTo(2);
        }
    }

    // ===== unités gratuites =====

    /**
     * Les unités gratuites sont vendues au prix normal : {@code quantity_ug} est un sous-ensemble de
     * {@code quantity_requested}, et le client les paie comme le reste. Leur valeur est donc dans le
     * TTC <em>et</em> dans l'encaissement.
     *
     * <p>Une officine peut vouloir les sortir de son chiffre d'affaires — c'est le paramètre
     * {@code EXCLUDE_FREE_UNIT}. Il faut alors les retirer du net et du comptant ensemble, sinon la
     * colonne « Montant Net » cesse d'égaler le comptant plus le crédit.
     */
    @Nested
    @DisplayName("Unités gratuites")
    class UnitesGratuites {

        @Test
        @DisplayName("sans exclusion, les unités gratuites restent dans le chiffre d'affaires")
        void sansExclusion() {
            venteAvecUnitesGratuites();

            TableauPharmacienDTO ligne = ligneDuJour();

            assertThat(ligne.getMontantTtcUg()).isEqualTo(3_000);
            assertThat(ligne.getMontantNet()).isEqualTo(10_000);
            assertThat(ligne.getMontantComptant()).isEqualTo(10_000);
        }

        @Test
        @DisplayName("avec exclusion, les unités gratuites sortent du net et du comptant")
        void avecExclusion() {
            when(appConfigurationService.excludeFreeUnit()).thenReturn(true);
            venteAvecUnitesGratuites();

            TableauPharmacienDTO ligne = ligneDuJour();

            assertThat(ligne.getMontantNet()).isEqualTo(7_000);
            assertThat(ligne.getMontantComptant()).isEqualTo(7_000);
        }

        /** Dans les deux réglages, les colonnes de la ligne doivent continuer de se recouper. */
        @Test
        @DisplayName("l'égalité net = comptant + crédit tient dans les deux réglages")
        void egaliteDansLesDeuxReglages() {
            venteAvecUnitesGratuites();
            TableauPharmacienDTO sansExclusion = ligneDuJour();
            assertThat(sansExclusion.getMontantNet())
                .isEqualTo(sansExclusion.getMontantComptant() + sansExclusion.getMontantCredit());

            when(appConfigurationService.excludeFreeUnit()).thenReturn(true);
            TableauPharmacienDTO avecExclusion = ligneDuJour();
            assertThat(avecExclusion.getMontantNet())
                .isEqualTo(avecExclusion.getMontantComptant() + avecExclusion.getMontantCredit());
        }

        /** Dix unités vendues à mille, dont trois prélevées sur le stock d'unités gratuites. */
        private void venteAvecUnitesGratuites() {
            CashSale vente = venteFermee(aujourdHui(), 10_000, 0);
            ligneDeVente(vente, produit(unique("DOLIPRANE"), 1_000, 0), 10, 0).setQuantityUg(3);
            reglement(vente, caisseOuverte(), "CASH", 10_000);
            em.flush();
        }
    }

    // ===== ventilation des règlements =====

    @Nested
    @DisplayName("Ventilation des règlements")
    class VentilationDesReglements {

        @Test
        @DisplayName("le mode de règlement est rendu avec son code et son libellé")
        void modeDeReglement() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 0, "CASH");

            assertThat(ligneDuJour().getPayments())
                .singleElement()
                .satisfies(paiement -> {
                    assertThat(paiement.code()).isEqualTo("CASH");
                    assertThat(paiement.libelle()).isEqualTo("ESPECE");
                    assertThat(paiement.paidAmount()).isEqualTo(10_000L);
                });
        }

        /** Une vente réglée en deux fois se ventile par mode, et le comptant reste le total. */
        @Test
        @DisplayName("un règlement mixte se ventile par mode")
        void reglementMixte() {
            CashSale vente = venteFermee(aujourdHui(), 10_000, 0);
            ligneDeVente(vente, produit(unique("DOLIPRANE"), 1_000, 0), 10, 0);
            reglement(vente, caisseOuverte(), "CASH", 6_000);
            reglement(vente, caisseOuverte(), "OM", 4_000);

            TableauPharmacienDTO ligne = ligneDuJour();

            assertThat(ligne.getPayments()).extracting(PaymentDTO::code).containsExactlyInAnyOrder("CASH", "OM");
            assertThat(ligne.getMontantComptant()).isEqualTo(10_000);
        }

        /** Les mouvements de caisse ne sont pas des ventes : ils n'ont pas leur place ici. */
        @Test
        @DisplayName("seuls les règlements de vente sont ventilés")
        void seulsLesReglementsDeVente() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 0, "CASH");

            assertThat(ligneDuJour().getPayments()).hasSize(1);
        }
    }

    // ===== totaux de la période =====

    @Nested
    @DisplayName("Totaux de la période")
    class TotauxDeLaPeriode {

        @Test
        @DisplayName("les totaux cumulent les lignes de la période")
        void totauxCumules() {
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(aujourdHui().minusDays(1), produit, 10, 0, "CASH");
            venteEncaissee(aujourdHui(), produit, 5, 0, "CASH");

            TableauPharmacienWrapper tableau = service.getTableauPharmacien(parametre(aujourdHui().minusDays(1), aujourdHui()));

            assertThat(tableau.getMontantVenteTtc()).isEqualTo(15_000);
            assertThat(tableau.getMontantVenteComptant()).isEqualTo(15_000);
            assertThat(tableau.getNumberCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("le total net cumule les nets des lignes")
        void totalNet() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 1_500, "CASH");

            TableauPharmacienWrapper tableau = service.getTableauPharmacien(parametre());

            assertThat(tableau.getMontantVenteNet()).isEqualTo(8_500);
            assertThat(tableau.getMontantVenteRemise()).isEqualTo(1_500);
        }

        /** Sans achat sur la période, les ratios ne se calculent pas : ils valent zéro, non l'infini. */
        @Test
        @DisplayName("sans achat, les ratios valent zéro")
        void ratiosSansAchat() {
            venteEncaissee(aujourdHui(), produit(unique("DOLIPRANE"), 1_000, 0), 10, 0, "CASH");

            TableauPharmacienWrapper tableau = service.getTableauPharmacien(parametre());

            assertThat(tableau.getRatioVenteAchat()).isZero();
            assertThat(tableau.getRatioAchatVente()).isZero();
        }
    }

    // ===== regroupement mensuel =====

    @Nested
    @DisplayName("Regroupement mensuel")
    class RegroupementMensuel {

        /** Le regroupement mensuel passe par une seconde fonction stockée, au contrat distinct. */
        @Test
        @DisplayName("deux journées du même mois tiennent sur une ligne")
        void deuxJoursUneLigne() {
            LocalDate debutDuMois = aujourdHui().withDayOfMonth(1);
            Produit produit = produit(unique("DOLIPRANE"), 1_000, 0);
            venteEncaissee(debutDuMois, produit, 10, 0, "CASH");
            venteEncaissee(debutDuMois.plusDays(1), produit, 5, 0, "CASH");

            MvtParam param = parametre(debutDuMois, debutDuMois.plusDays(1));
            param.setGroupeBy("month");

            TableauPharmacienWrapper tableau = service.getTableauPharmacien(param);

            assertThat(tableau.getTableauPharmaciens()).hasSize(1);
            TableauPharmacienDTO ligne = tableau.getTableauPharmaciens().getFirst();
            assertThat(ligne.getMvtDate()).isEqualTo(debutDuMois);
            assertThat(ligne.getMontantTtc()).isEqualTo(15_000);
            assertThat(ligne.getNombreVente()).isEqualTo(2);
        }

        @Test
        @DisplayName("le regroupement mensuel ventile aussi les règlements")
        void reglementsMensuels() {
            LocalDate debutDuMois = aujourdHui().withDayOfMonth(1);
            venteEncaissee(debutDuMois, produit(unique("DOLIPRANE"), 1_000, 0), 10, 0, "CASH");

            MvtParam param = parametre(debutDuMois, debutDuMois.plusDays(1));
            param.setGroupeBy("month");

            assertThat(service.getTableauPharmacien(param).getTableauPharmaciens().getFirst().getPayments())
                .extracting(PaymentDTO::code)
                .containsExactly("CASH");
        }
    }

    // ===== utilitaires =====

    private TableauPharmacienDTO ligneDuJour() {
        List<TableauPharmacienDTO> lignes = service.getTableauPharmacien(parametre()).getTableauPharmaciens();
        assertThat(lignes).as("la journée doit avoir une ligne").hasSize(1);
        return lignes.getFirst();
    }

    private MvtParam parametre() {
        return parametre(aujourdHui(), aujourdHui());
    }

    private MvtParam parametre(LocalDate debut, LocalDate fin) {
        MvtParam param = new MvtParam();
        param.setFromDate(debut);
        param.setToDate(fin);
        param.setStatuts(java.util.Set.of(SalesStatut.CLOSED));
        return param.build();
    }
}
