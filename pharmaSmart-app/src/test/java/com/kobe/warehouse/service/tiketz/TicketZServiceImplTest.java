package com.kobe.warehouse.service.tiketz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.SalePaymentRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.service.PaymentModeService;
import com.kobe.warehouse.service.receipt.service.TicketZPrinterService;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZCreditProjection;
import com.kobe.warehouse.service.tiketz.dto.TicketZData;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;
import com.kobe.warehouse.service.tiketz.dto.TicketZProjection;
import com.kobe.warehouse.service.tiketz.dto.TicketZRecap;
import com.kobe.warehouse.service.tiketz.service.TicketZReportService;
import com.kobe.warehouse.service.tiketz.service.TicketZServiceImpl;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le ticket Z est le document de clôture de caisse : il arrête, caissier par caissier, ce qui a été
 * encaissé et par quel moyen. C'est la pièce que le titulaire signe et archive, et celle contre
 * laquelle il recompte son tiroir.
 *
 * <p>Sa construction assemble deux natures de lignes que la base rend séparément : les règlements,
 * groupés par caissier <b>et par sens</b> — la requête produit une ligne de règlement et une ligne
 * de crédit pour un même mode —, et les ventes à crédit, qui n'ont pas de règlement du tout. Un
 * cumul mal placé dans cette double boucle ne produit aucune erreur : il gonfle un sous-total que
 * personne ne recoupe, puisque c'est justement le document de référence.
 */
@DisplayName("TicketZService — clôture de caisse")
class TicketZServiceImplTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 3, 10);

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final SalePaymentRepository salePaymentRepository = mock(SalePaymentRepository.class);
    private final PaymentTransactionRepository paymentTransactionRepository = mock(PaymentTransactionRepository.class);
    private final SalesRepository salesRepository = mock(SalesRepository.class);
    private final ThirdPartySaleRepository thirdPartySaleRepository = mock(ThirdPartySaleRepository.class);
    private final PaymentModeService paymentModeService = mock(PaymentModeService.class);
    private final TicketZPrinterService printerService = mock(TicketZPrinterService.class);
    private final TicketZReportService reportService = mock(TicketZReportService.class);

    private final TicketZServiceImpl service = new TicketZServiceImpl(
        salePaymentRepository,
        paymentTransactionRepository,
        salesRepository,
        thirdPartySaleRepository,
        paymentModeService,
        printerService,
        reportService
    );

    @BeforeEach
    void caisseVide() {
        when(paymentTransactionRepository.fetchAllMvts(any())).thenReturn(List.of());
        when(salePaymentRepository.fetchSalesPayment(any())).thenReturn(List.of());
        when(salesRepository.getTicketZDifferes(any())).thenReturn(new java.util.ArrayList<>());
        when(thirdPartySaleRepository.getTicketZCreditProjection(any())).thenReturn(new java.util.ArrayList<>());
        when(paymentModeService.fetchAll()).thenReturn(List.of());
    }

    // ===== récapitulatif par caissier =====

    @Nested
    @DisplayName("Récapitulatif par caissier")
    class RecapitulatifParCaissier {

        @Test
        @DisplayName("chaque caissier a son récapitulatif, nommé par son initiale et son nom")
        void unRecapParCaissier() {
            donneReglements(
                reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 300_000, false),
                reglement(2, "Jean", "DUPONT", ModePaimentCode.CASH, 200_000, false)
            );

            TicketZ ticket = service.getTicketZ(parametre());

            assertThat(ticket.datas()).extracting(TicketZRecap::userName).containsExactly("A. KONE", "J. DUPONT");
        }

        @Test
        @DisplayName("les caissiers sont classés par nom")
        void classementParNom() {
            donneReglements(
                reglement(1, "Zoe", "ZOBO", ModePaimentCode.CASH, 100_000, false),
                reglement(2, "Awa", "KONE", ModePaimentCode.CASH, 100_000, false)
            );

            assertThat(service.getTicketZ(parametre()).datas())
                .extracting(TicketZRecap::userName)
                .isSorted();
        }

        @Test
        @DisplayName("les modes d'un caissier se cumulent mode par mode")
        void cumulParMode() {
            donneReglements(
                reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 300_000, false),
                reglement(1, "Awa", "KONE", ModePaimentCode.CB, 120_000, false)
            );

            assertThat(lignesDuCaissier(0))
                .extracting(TicketZData::modePaimentCode, TicketZData::value)
                .containsExactly(
                    org.assertj.core.api.Assertions.tuple(ModePaimentCode.CASH, 300_000L),
                    org.assertj.core.api.Assertions.tuple(ModePaimentCode.CB, 120_000L)
                );
        }

        /**
         * La requête rend une ligne par sens : le règlement d'un côté, le crédit de l'autre. Le crédit
         * se retranche, puisqu'il n'est pas entré dans le tiroir.
         */
        @Test
        @DisplayName("la part à crédit d'un mode se retranche de son règlement")
        void creditRetranche() {
            donneReglements(
                reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 300_000, false),
                reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 50_000, true)
            );

            assertThat(lignesDuCaissier(0).getFirst().value()).isEqualTo(250_000L);
        }

        @Test
        @DisplayName("les lignes du caissier sont rangées dans l'ordre d'affichage des modes")
        void ordreDesModes() {
            donneReglements(
                reglement(1, "Awa", "KONE", ModePaimentCode.CH, 10_000, false),
                reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 20_000, false),
                reglement(1, "Awa", "KONE", ModePaimentCode.CB, 30_000, false)
            );

            assertThat(lignesDuCaissier(0)).extracting(TicketZData::sortOrder).isSorted();
        }

        @Test
        @DisplayName("une caisse sans mouvement rend un ticket vide")
        void caisseSansMouvement() {
            TicketZ ticket = service.getTicketZ(parametre());

            assertThat(ticket.datas()).isEmpty();
            assertThat(ticket.summaries()).isEmpty();
        }
    }

    // ===== sous-total mobile =====

    @Nested
    @DisplayName("Sous-total du règlement mobile")
    class SousTotalMobile {

        /**
         * Le défaut que ce test fixe : le cumul mobile se faisait <b>à chaque ligne</b> de la boucle,
         * en ajoutant le sous-total courant du mode et non le montant de la ligne. Un mode mobile
         * portant un règlement et un crédit comptait donc deux fois son règlement.
         */
        @Test
        @DisplayName("un mode mobile portant un crédit ne compte pas deux fois son règlement")
        void creditNeDoublePasLeReglement() {
            donneReglements(
                reglement(1, "Awa", "KONE", ModePaimentCode.OM, 100_000, false),
                reglement(1, "Awa", "KONE", ModePaimentCode.OM, 30_000, true),
                reglement(1, "Awa", "KONE", ModePaimentCode.WAVE, 50_000, false)
            );

            // Orange Money net : 100 000 − 30 000. Plus Wave : 120 000 au total.
            assertThat(sousTotalMobileDuCaissier(0)).isEqualTo(120_000L);
        }

        /**
         * Le sous-total n'a d'intérêt qu'à partir de deux opérateurs. Compter les lignes et non les
         * opérateurs le faisait apparaître pour un seul opérateur réglé en deux fois, où il ne faisait
         * que répéter la ligne au-dessus.
         */
        @Test
        @DisplayName("un seul opérateur mobile ne produit pas de sous-total")
        void unSeulOperateur() {
            donneReglements(
                reglement(1, "Awa", "KONE", ModePaimentCode.OM, 100_000, false),
                reglement(1, "Awa", "KONE", ModePaimentCode.OM, 30_000, true)
            );

            assertThat(service.getTicketZ(parametre()).datas().getFirst().summary()).isEmpty();
        }

        @Test
        @DisplayName("deux opérateurs mobiles produisent leur sous-total")
        void deuxOperateurs() {
            donneReglements(
                reglement(1, "Awa", "KONE", ModePaimentCode.OM, 100_000, false),
                reglement(1, "Awa", "KONE", ModePaimentCode.MTN, 60_000, false)
            );

            assertThat(sousTotalMobileDuCaissier(0)).isEqualTo(160_000L);
        }

        /** Le sous-total global suit la même règle, tous caissiers confondus. */
        @Test
        @DisplayName("le récapitulatif global porte son propre sous-total mobile")
        void sousTotalGlobal() {
            donneReglements(
                reglement(1, "Awa", "KONE", ModePaimentCode.OM, 100_000, false),
                reglement(2, "Jean", "DUPONT", ModePaimentCode.WAVE, 60_000, false)
            );

            assertThat(ligneGlobale("Total Mobile").value()).isEqualTo(160_000L);
        }

        @Test
        @DisplayName("une caisse sans règlement mobile n'a pas de sous-total mobile")
        void aucunMobile() {
            donneReglements(reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 300_000, false));

            assertThat(service.getTicketZ(parametre()).summaries())
                .extracting(TicketZData::libelle)
                .doesNotContain("Total Mobile");
        }
    }

    // ===== ventes à crédit =====

    @Nested
    @DisplayName("Ventes à crédit")
    class VentesACredit {

        @Test
        @DisplayName("le crédit du caissier apparaît sur son récapitulatif")
        void creditDuCaissier() {
            donneReglements(reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 300_000, false));
            donneCredits(credit(1, "Awa", "KONE", 75_000));

            assertThat(lignesDuCaissier(0))
                .extracting(TicketZData::libelle)
                .contains("Crédit(vno/vo)");
        }

        /**
         * Un caissier peut n'avoir fait que des ventes à crédit : il n'a alors aucun règlement, donc
         * aucune ligne de mode. Son récapitulatif doit exister quand même.
         */
        @Test
        @DisplayName("un caissier n'ayant fait que du crédit a tout de même son récapitulatif")
        void caissierSansReglement() {
            donneCredits(credit(7, "Awa", "KONE", 75_000));

            TicketZ ticket = service.getTicketZ(parametre());

            assertThat(ticket.datas()).hasSize(1);
            assertThat(ticket.datas().getFirst().userName()).isEqualTo("A. KONE");
            assertThat(ticket.datas().getFirst().datas()).extracting(TicketZData::value).containsExactly(75_000L);
        }

        @Test
        @DisplayName("les crédits d'un même caissier se cumulent")
        void cumulDesCredits() {
            donneCredits(credit(7, "Awa", "KONE", 50_000), credit(7, "Awa", "KONE", 25_000));

            assertThat(service.getTicketZ(parametre()).datas().getFirst().datas().getFirst().value()).isEqualTo(75_000L);
        }

        @Test
        @DisplayName("le récapitulatif global porte le crédit total")
        void creditGlobal() {
            donneReglements(reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 300_000, false));
            donneCredits(credit(1, "Awa", "KONE", 75_000));

            assertThat(ligneGlobale("Crédit(vno/vo)").value()).isEqualTo(75_000L);
        }

        @Test
        @DisplayName("sans crédit, aucune ligne de crédit au global")
        void aucunCredit() {
            donneReglements(reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 300_000, false));

            assertThat(service.getTicketZ(parametre()).summaries())
                .extracting(TicketZData::libelle)
                .doesNotContain("Crédit(vno/vo)");
        }
    }

    // ===== récapitulatif global =====

    @Nested
    @DisplayName("Récapitulatif global")
    class RecapitulatifGlobal {

        @Test
        @DisplayName("les modes de tous les caissiers se cumulent au global")
        void cumulTousCaissiers() {
            donneReglements(
                reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 300_000, false),
                reglement(2, "Jean", "DUPONT", ModePaimentCode.CASH, 200_000, false)
            );

            assertThat(ligneGlobaleDuMode(ModePaimentCode.CASH).value()).isEqualTo(500_000L);
        }

        @Test
        @DisplayName("les lignes globales sont rangées dans l'ordre d'affichage des modes")
        void ordreDesLignes() {
            donneReglements(
                reglement(1, "Awa", "KONE", ModePaimentCode.CH, 10_000, false),
                reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 20_000, false)
            );

            assertThat(service.getTicketZ(parametre()).summaries()).extracting(TicketZData::sortOrder).isSorted();
        }

        /** Le libellé vient du référentiel des modes de règlement, avec repli sur le code. */
        @Test
        @DisplayName("le libellé du mode vient du référentiel")
        void libelleDuReferentiel() {
            PaymentMode mode = new PaymentMode();
            mode.setCode("CASH");
            mode.setLibelle("Espèces");
            // fetchAll() et non fetch() : le service nomme des modes deja encaisses, pas des modes
            // a proposer, et fetch() ecarte VIREMENT et CH (cf. TicketZServiceImpl).
            when(paymentModeService.fetchAll()).thenReturn(List.of(mode));
            donneReglements(reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 300_000, false));

            assertThat(ligneGlobaleDuMode(ModePaimentCode.CASH).libelle()).isEqualTo("Espèces");
        }

        @Test
        @DisplayName("un mode absent du référentiel s'affiche sous son code")
        void modeHorsReferentiel() {
            donneReglements(reglement(1, "Awa", "KONE", ModePaimentCode.CASH, 300_000, false));

            assertThat(ligneGlobaleDuMode(ModePaimentCode.CASH).libelle()).isEqualTo("CASH");
        }
    }

    // ===== périmètre interrogé =====

    @Nested
    @DisplayName("Périmètre interrogé")
    class PerimetreInterroge {

        /** « Ventes seules » change la source : les règlements de vente, non tous les mouvements. */
        @Test
        @DisplayName("le filtre « ventes seules » change la source interrogée")
        void ventesSeules() {
            service.getTicketZ(new TicketZParam(Set.of(), true, JOUR, JOUR, null, null));

            verify(salePaymentRepository).fetchSalesPayment(any());
            verify(paymentTransactionRepository, never()).fetchAllMvts(any());
        }

        @Test
        @DisplayName("sans ce filtre, tous les mouvements de caisse sont retenus")
        void tousLesMouvements() {
            service.getTicketZ(parametre());

            verify(paymentTransactionRepository).fetchAllMvts(any());
            verify(salePaymentRepository, never()).fetchSalesPayment(any());
        }

        @Test
        @DisplayName("les ventes à crédit et les différés sont tous deux réunis")
        void creditsEtDifferes() {
            service.getTicketZ(parametre());

            verify(thirdPartySaleRepository).getTicketZCreditProjection(any());
            verify(salesRepository).getTicketZDifferes(any());
        }
    }

    // ===== utilitaires =====

    private TicketZParam parametre() {
        return new TicketZParam(Set.of(), false, JOUR, JOUR, LocalTime.MIN, LocalTime.MAX);
    }

    private void donneReglements(TicketZProjection... projections) {
        when(paymentTransactionRepository.fetchAllMvts(any())).thenReturn(List.of(projections));
    }

    private void donneCredits(TicketZCreditProjection... projections) {
        when(thirdPartySaleRepository.getTicketZCreditProjection(any()))
            .thenReturn(new java.util.ArrayList<>(List.of(projections)));
    }

    private List<TicketZData> lignesDuCaissier(int index) {
        return service.getTicketZ(parametre()).datas().get(index).datas();
    }

    private long sousTotalMobileDuCaissier(int index) {
        List<TicketZData> sousTotaux = service.getTicketZ(parametre()).datas().get(index).summary();
        assertThat(sousTotaux).hasSize(1);
        return sousTotaux.getFirst().value();
    }

    private TicketZData ligneGlobale(String libelle) {
        return service
            .getTicketZ(parametre())
            .summaries()
            .stream()
            .filter(data -> libelle.equals(data.libelle()))
            .findFirst()
            .orElseThrow();
    }

    private TicketZData ligneGlobaleDuMode(ModePaimentCode code) {
        return service
            .getTicketZ(parametre())
            .summaries()
            .stream()
            .filter(data -> code == data.modePaimentCode())
            .findFirst()
            .orElseThrow();
    }

    private static TicketZProjection reglement(
        int userId,
        String prenom,
        String nom,
        ModePaimentCode mode,
        long montant,
        boolean credit
    ) {
        return new TicketZProjection(mode.name(), mode.name(), userId, prenom, nom, montant, montant, credit);
    }

    private static TicketZCreditProjection credit(int userId, String prenom, String nom, long montant) {
        return new TicketZCreditProjection(userId, prenom, nom, montant);
    }
}
