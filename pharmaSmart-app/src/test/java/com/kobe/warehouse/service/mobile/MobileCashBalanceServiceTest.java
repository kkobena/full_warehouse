package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;
import com.kobe.warehouse.service.dto.mobile.CashMovementDTO;
import com.kobe.warehouse.service.dto.mobile.CategoryBalanceDTO;
import com.kobe.warehouse.service.dto.mobile.MobileCashBalanceDTO;
import com.kobe.warehouse.service.dto.mobile.PaymentModeBreakdownDTO;
import com.kobe.warehouse.service.dto.records.Tuple;
import com.kobe.warehouse.service.financiel_transaction.BalanceCaisseService;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * La balance de caisse mobile rejoue le calcul du poste et le remet en forme pour un écran étroit :
 * une part par mode de règlement, une ligne par catégorie de vente, une liste de mouvements.
 *
 * <p>Le point sensible est le sens des mouvements. Le calcul de la balance somme des règlements —
 * des montants toujours positifs, sortie de caisse comprise — et laisse la clé du mouvement dire
 * s'il s'agit d'une entrée ou d'une sortie. Déduire ce sens du signe du montant, comme le faisait
 * cette transformation, affichait toute sortie de caisse comme une rentrée d'argent.
 */
@DisplayName("MobileCashBalanceService — balance de caisse")
class MobileCashBalanceServiceTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 3, 10);

    // Doublure créée à l'initialisation du champ, et non dans un @BeforeEach : le délai de quinze
    // secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final BalanceCaisseService balanceCaisseService = mock(BalanceCaisseService.class);

    private final MobileCashBalanceService service = new MobileCashBalanceService(balanceCaisseService);

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre interrogé")
    class PerimetreInterroge {

        @Test
        @DisplayName("une date de fin absente ramène la période à la seule date de début")
        void dateDeFinAbsente() {
            donneBalance(new BalanceCaisseWrapper());

            MobileCashBalanceDTO resultat = service.getCashBalance(JOUR, null);

            assertThat(parametreTransmis().getToDate()).isEqualTo(JOUR);
            assertThat(resultat.toDate()).isEqualTo(JOUR);
        }

        /** Seules les ventes clôturées entrent en balance : un panier en cours n'est pas encaissé. */
        @Test
        @DisplayName("seules les ventes clôturées sont prises en compte")
        void seulementLesVentesCloturees() {
            donneBalance(new BalanceCaisseWrapper());

            service.getCashBalance(JOUR, JOUR);

            assertThat(parametreTransmis().getStatuts()).containsExactly(SalesStatut.CLOSED);
        }

        @Test
        @DisplayName("une balance introuvable rend un récapitulatif vide mais daté")
        void balanceIntrouvable() {
            when(balanceCaisseService.getBalanceCaisse(any())).thenReturn(null);

            MobileCashBalanceDTO resultat = service.getCashBalance(JOUR, JOUR);

            assertThat(resultat.isEmpty()).isTrue();
            assertThat(resultat.fromDate()).isEqualTo(JOUR);
            assertThat(resultat.periodLabel()).isEqualTo("10/03/2026");
            assertThat(resultat.paymentBreakdown()).isEmpty();
            assertThat(resultat.categoryBalances()).isEmpty();
            assertThat(resultat.cashMovements()).isEmpty();
        }

        @Test
        @DisplayName("les totaux de la balance sont repris tels quels")
        void totauxRepris() {
            donneBalance(
                new BalanceCaisseWrapper()
                    .setCount(42)
                    .setMontantTtc(1_000_000L)
                    .setMontantHt(900_000L)
                    .setMontantNet(950_000L)
                    .setMontantDiscount(50_000L)
                    .setMontantTaxe(100_000L)
                    .setPanierMoyen(23_809L)
                    .setMontantAchat(600_000L)
                    .setMontantMarge(400_000L)
                    .setRatioVenteAchat(1.67f)
                    .setRatioAchatVente(0.6f)
            );

            MobileCashBalanceDTO resultat = service.getCashBalance(JOUR, JOUR);

            assertThat(resultat.transactionsCount()).isEqualTo(42);
            assertThat(resultat.montantTtc()).isEqualTo(1_000_000L);
            assertThat(resultat.montantHt()).isEqualTo(900_000L);
            assertThat(resultat.montantNet()).isEqualTo(950_000L);
            assertThat(resultat.montantRemise()).isEqualTo(50_000L);
            assertThat(resultat.montantTva()).isEqualTo(100_000L);
            assertThat(resultat.panierMoyen()).isEqualTo(23_809L);
            assertThat(resultat.montantAchats()).isEqualTo(600_000L);
            assertThat(resultat.montantMarge()).isEqualTo(400_000L);
            assertThat(resultat.ratioVenteAchat()).isEqualTo(1.67f);
            assertThat(resultat.ratioAchatVente()).isEqualTo(0.6f);
        }
    }

    // ===== répartition par mode =====

    @Nested
    @DisplayName("Répartition par mode de règlement")
    class RepartitionParMode {

        @Test
        @DisplayName("chaque mode alimenté donne une part, avec son pourcentage et sa couleur")
        void partParMode() {
            donneBalance(
                new BalanceCaisseWrapper()
                    .setMontantTtc(1_000_000L)
                    .setMontantCash(500_000L)
                    .setMontantCard(200_000L)
                    .setMontantMobileMoney(150_000L)
                    .setMontantCheck(100_000L)
                    .setMontantVirement(50_000L)
            );

            List<PaymentModeBreakdownDTO> parts = service.getCashBalance(JOUR, JOUR).paymentBreakdown();

            assertThat(parts).extracting(PaymentModeBreakdownDTO::code)
                .containsExactly("ESPECES", "CARTE", "MOBILE_MONEY", "CHEQUE", "VIREMENT");
            assertThat(parts).extracting(PaymentModeBreakdownDTO::percent)
                .containsExactly(50.0, 20.0, 15.0, 10.0, 5.0);
            assertThat(parts.getFirst().color()).isEqualTo("#4CAF50");
        }

        @Test
        @DisplayName("un mode non utilisé n'encombre pas la répartition")
        void modeInutiliseAbsent() {
            donneBalance(new BalanceCaisseWrapper().setMontantTtc(500_000L).setMontantCash(500_000L));

            assertThat(service.getCashBalance(JOUR, JOUR).paymentBreakdown())
                .extracting(PaymentModeBreakdownDTO::code)
                .containsExactly("ESPECES");
        }

        /**
         * Crédit, différé et part tiers payant décrivent tous le même fait : de la marchandise
         * sortie sans encaissement. Ils sont réunis sur une seule part, sans quoi le camembert
         * multiplierait les tranches minuscules.
         */
        @Test
        @DisplayName("crédit, différé et tiers payant forment une seule part")
        void creditRegroupe() {
            donneBalance(
                new BalanceCaisseWrapper()
                    .setMontantTtc(1_000_000L)
                    .setMontantCredit(30_000L)
                    .setMontantDiffere(20_000L)
                    .setPartTiersPayant(50_000L)
            );

            List<PaymentModeBreakdownDTO> parts = service.getCashBalance(JOUR, JOUR).paymentBreakdown();

            assertThat(parts).hasSize(1);
            assertThat(parts.getFirst().code()).isEqualTo("CREDIT");
            assertThat(parts.getFirst().montant()).isEqualTo(100_000L);
            assertThat(parts.getFirst().percent()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("le pourcentage est arrondi au dixième")
        void pourcentageArrondi() {
            donneBalance(new BalanceCaisseWrapper().setMontantTtc(1_000_000L).setMontantCash(333_333L));

            assertThat(service.getCashBalance(JOUR, JOUR).paymentBreakdown().getFirst().percent()).isEqualTo(33.3);
        }

        /** Un chiffre d'affaires nul ne doit pas faire diviser par zéro. */
        @Test
        @DisplayName("un chiffre d'affaires nul rend des pourcentages nuls")
        void chiffreDAffairesNul() {
            donneBalance(new BalanceCaisseWrapper().setMontantTtc(0L).setMontantCash(1_000L));

            assertThat(service.getCashBalance(JOUR, JOUR).paymentBreakdown().getFirst().percent()).isZero();
        }

        @Test
        @DisplayName("une journée sans encaissement ne rend aucune part")
        void journeeSansEncaissement() {
            donneBalance(new BalanceCaisseWrapper());

            assertThat(service.getCashBalance(JOUR, JOUR).paymentBreakdown()).isEmpty();
        }
    }

    // ===== catégories =====

    @Nested
    @DisplayName("Balance par catégorie de vente")
    class BalanceParCategorie {

        @Test
        @DisplayName("chaque catégorie porte son code et son libellé d'officine")
        void codeEtLibelle() {
            donneBalance(
                new BalanceCaisseWrapper()
                    .setBalanceCaisses(
                        List.of(
                            categorie(TypeVenteDTO.CashSale),
                            categorie(TypeVenteDTO.ThirdPartySales),
                            categorie(TypeVenteDTO.VenteDepot)
                        )
                    )
            );

            assertThat(service.getCashBalance(JOUR, JOUR).categoryBalances())
                .extracting(CategoryBalanceDTO::categoryLabel)
                .containsExactly("VNO", "VO", "Ventes Dépôts");
        }

        @Test
        @DisplayName("une catégorie sans type se range dans « Autre »")
        void categorieSansType() {
            donneBalance(new BalanceCaisseWrapper().setBalanceCaisses(List.of(new BalanceCaisseDTO())));

            CategoryBalanceDTO categorie = service.getCashBalance(JOUR, JOUR).categoryBalances().getFirst();

            assertThat(categorie.categoryCode()).isEqualTo("AUTRE");
            assertThat(categorie.categoryLabel()).isEqualTo("Autre");
        }

        @Test
        @DisplayName("les montants de la catégorie sont repris ligne à ligne")
        void montantsRepris() {
            BalanceCaisseDTO vno = categorie(TypeVenteDTO.CashSale);
            vno.setCount(40L);
            vno.setMontantTtc(800_000L).setMontantHt(700_000L).setMontantNet(750_000L);
            vno.setMontantDiscount(50_000);
            vno.setMontantAchat(500_000L).setMontantMarge(300_000L).setPanierMoyen(20_000L);
            vno.setMontantCash(400_000L).setMontantCard(200_000L).setMontantCheck(50_000L);
            vno.setMontantVirement(30_000L).setMontantMobileMoney(80_000L).setMontantCredit(40_000L);
            vno.setMontantDiffere(10_000);
            vno.setPartTiersPayant(20_000L);
            donneBalance(new BalanceCaisseWrapper().setBalanceCaisses(List.of(vno)));

            CategoryBalanceDTO categorie = service.getCashBalance(JOUR, JOUR).categoryBalances().getFirst();

            assertThat(categorie.montantTtc()).isEqualTo(800_000L);
            assertThat(categorie.montantHt()).isEqualTo(700_000L);
            assertThat(categorie.montantNet()).isEqualTo(750_000L);
            assertThat(categorie.montantRemise()).isEqualTo(50_000L);
            // La taxe se déduit du TTC et du HT : elle n'est pas portée par la catégorie.
            assertThat(categorie.montantTva()).isEqualTo(100_000L);
            assertThat(categorie.montantAchat()).isEqualTo(500_000L);
            // Marge et panier moyen se déduisent eux aussi : net − achat, et TTC ÷ nombre de ventes.
            assertThat(categorie.montantMarge()).isEqualTo(250_000L);
            assertThat(categorie.panierMoyen()).isEqualTo(20_000L);
            assertThat(categorie.montantCash()).isEqualTo(400_000L);
            assertThat(categorie.montantCard()).isEqualTo(200_000L);
            assertThat(categorie.montantCheque()).isEqualTo(50_000L);
            assertThat(categorie.montantVirement()).isEqualTo(30_000L);
            assertThat(categorie.montantMobileMoney()).isEqualTo(80_000L);
            assertThat(categorie.montantCredit()).isEqualTo(40_000L);
            assertThat(categorie.montantDiffere()).isEqualTo(10_000L);
            assertThat(categorie.montantTiersPayant()).isEqualTo(20_000L);
        }

        @Test
        @DisplayName("aucune catégorie rend une liste vide")
        void aucuneCategorie() {
            donneBalance(new BalanceCaisseWrapper().setBalanceCaisses(List.of()));

            assertThat(service.getCashBalance(JOUR, JOUR).categoryBalances()).isEmpty();
        }
    }

    // ===== mouvements de caisse =====

    @Nested
    @DisplayName("Mouvements de caisse")
    class MouvementsDeCaisse {

        /**
         * Le défaut que ces tests fixent : le montant d'une sortie de caisse remonte positif, comme
         * tous les autres. En déduire le sens du signe faisait passer chaque sortie pour une entrée.
         */
        @Test
        @DisplayName("une sortie de caisse reste une sortie malgré son montant positif")
        void sortieDeCaisse() {
            donneBalance(mouvements(mouvement(TransactionTypeAffichage.SORTIE_CAISSE, 75_000L)));

            CashMovementDTO mouvement = service.getCashBalance(JOUR, JOUR).cashMovements().getFirst();

            assertThat(mouvement.isSortie()).isTrue();
            assertThat(mouvement.montant()).isEqualTo(75_000L);
            assertThat(mouvement.libelle()).isEqualTo("Sortie");
        }

        /**
         * Un règlement fournisseur et un fonds de caisse sortent aussi de la caisse : le domaine les
         * range en sortie, et le rapport d'activité les compte déjà ainsi. La balance s'aligne.
         */
        @Test
        @DisplayName("un règlement fournisseur est aussi une sortie")
        void reglementFournisseur() {
            donneBalance(mouvements(mouvement(TransactionTypeAffichage.REGLEMENT_FOURNISSEUR, 400_000L)));

            assertThat(service.getCashBalance(JOUR, JOUR).cashMovements().getFirst().isSortie()).isTrue();
        }

        @Test
        @DisplayName("les autres mouvements sont des entrées")
        void autresMouvements() {
            donneBalance(
                mouvements(
                    mouvement(TransactionTypeAffichage.ENTREE_CAISSE, 30_000L),
                    mouvement(TransactionTypeAffichage.VNO, 500_000L),
                    mouvement(TransactionTypeAffichage.REGLEMENT_DIFFERE, 20_000L)
                )
            );

            assertThat(service.getCashBalance(JOUR, JOUR).cashMovements())
                .allMatch(CashMovementDTO::isEntree);
        }

        @Test
        @DisplayName("un montant négatif reste une sortie, quelle que soit la clé")
        void montantNegatif() {
            donneBalance(mouvements(mouvement(TransactionTypeAffichage.ENTREE_CAISSE, -12_000L)));

            CashMovementDTO mouvement = service.getCashBalance(JOUR, JOUR).cashMovements().getFirst();

            assertThat(mouvement.isSortie()).isTrue();
            assertThat(mouvement.montant()).isEqualTo(12_000L);
        }

        @Test
        @DisplayName("le montant est lu quel que soit le type numérique porté")
        void typesNumeriques() {
            donneBalance(
                mouvements(
                    new Tuple("VNO", "VNO", 1_000),
                    new Tuple("VO", "VO", 2_000L),
                    new Tuple("DEPOT_CAUTION", "Caution", 3_000.75d),
                    new Tuple("ENTREE_CAISSE", "Entrée", BigDecimal.valueOf(4_000))
                )
            );

            assertThat(service.getCashBalance(JOUR, JOUR).cashMovements())
                .extracting(CashMovementDTO::montant)
                .containsExactly(1_000L, 2_000L, 3_000L, 4_000L);
        }

        @Test
        @DisplayName("un montant absent ou illisible vaut zéro plutôt que d'interrompre le rapport")
        void montantIllisible() {
            donneBalance(mouvements(new Tuple("VNO", "VNO", null), new Tuple("VO", "VO", "n/a")));

            assertThat(service.getCashBalance(JOUR, JOUR).cashMovements())
                .extracting(CashMovementDTO::montant)
                .containsExactly(0L, 0L);
        }

        @Test
        @DisplayName("une journée sans mouvement rend une liste vide")
        void aucunMouvement() {
            donneBalance(new BalanceCaisseWrapper());

            assertThat(service.getCashBalance(JOUR, JOUR).cashMovements()).isEmpty();
        }
    }

    // ===== libellé de période =====

    @Nested
    @DisplayName("Libellé de période")
    class LibelleDePeriode {

        @Test
        @DisplayName("la journée en cours se nomme « Aujourd'hui »")
        void aujourdHui() {
            assertThat(libellePour(LocalDate.now(), null)).isEqualTo("Aujourd'hui");
        }

        @Test
        @DisplayName("la veille se nomme « Hier »")
        void hier() {
            assertThat(libellePour(LocalDate.now().minusDays(1), null)).isEqualTo("Hier");
        }

        @Test
        @DisplayName("toute autre journée s'affiche en clair")
        void autreJournee() {
            assertThat(libellePour(JOUR, null)).isEqualTo("10/03/2026");
        }

        @Test
        @DisplayName("une période encadrée affiche ses deux bornes")
        void periodeEncadree() {
            assertThat(libellePour(JOUR, LocalDate.of(2026, 3, 12))).isEqualTo("10/03/2026 - 12/03/2026");
        }

        private String libellePour(LocalDate debut, LocalDate fin) {
            donneBalance(new BalanceCaisseWrapper());
            return service.getCashBalance(debut, fin).periodLabel();
        }
    }

    // ===== utilitaires =====

    private void donneBalance(BalanceCaisseWrapper wrapper) {
        when(balanceCaisseService.getBalanceCaisse(any())).thenReturn(wrapper);
    }

    private MvtParam parametreTransmis() {
        ArgumentCaptor<MvtParam> captor = ArgumentCaptor.forClass(MvtParam.class);
        verify(balanceCaisseService).getBalanceCaisse(captor.capture());
        return captor.getValue();
    }

    private static BalanceCaisseWrapper mouvements(Tuple... tuples) {
        BalanceCaisseWrapper wrapper = new BalanceCaisseWrapper();
        wrapper.getMvtCaisses().addAll(List.of(tuples));
        return wrapper;
    }

    private static Tuple mouvement(TransactionTypeAffichage type, long montant) {
        return new Tuple(type.name(), type.getValueCourt(), montant);
    }

    private static BalanceCaisseDTO categorie(TypeVenteDTO type) {
        BalanceCaisseDTO dto = new BalanceCaisseDTO();
        dto.setTypeSale(type);
        dto.setCount(1L);
        return dto;
    }
}
