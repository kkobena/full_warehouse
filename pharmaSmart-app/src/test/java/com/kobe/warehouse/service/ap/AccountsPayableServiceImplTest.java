package com.kobe.warehouse.service.ap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.PaymentFournisseur;
import com.kobe.warehouse.domain.enumeration.PaimentStatut;
import com.kobe.warehouse.domain.enumeration.StatutLigneFournisseurAP;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.PaymentFournisseurRepository;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.CompteFournisseurAPDTO;
import com.kobe.warehouse.service.dto.FournisseurAPSummaryDTO;
import com.kobe.warehouse.service.dto.LigneFournisseurAPDTO;
import com.kobe.warehouse.service.dto.ReglementFournisseurAPCommand;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.report.pdf.AccountsPayableApPdfExportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Le compte fournisseur suit ce que l'officine doit à ses grossistes : bon de livraison par bon de
 * livraison, ce qui a été réglé et ce qui reste dû, avec l'échéance de chacun.
 *
 * <p>L'échéance n'est pas stockée : elle se déduit d'une date de réception et d'un nombre de jours de
 * crédit qui peut venir du fournisseur, de sa maison mère, ou d'un réglage général. Trois sources en
 * cascade pour une date qui décide du statut affiché — et donc de qui l'officine paie en premier.
 *
 * <p>L'imputation d'un règlement est l'autre point sensible : le montant versé se répartit sur
 * plusieurs bons, du plus ancien au plus récent, et chaque bon entièrement couvert doit basculer en
 * réglé. Une imputation qui déborde ou qui oublie de solder laisse le compte faux.
 */
@DisplayName("AccountsPayableService — comptes fournisseurs")
class AccountsPayableServiceImplTest {

    private static final int JOURS_CREDIT_PAR_DEFAUT = 30;
    private static final int JOURS_CRITIQUE_PAR_DEFAUT = 15;

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final CommandeRepository commandeRepository = mock(CommandeRepository.class);
    private final PaymentFournisseurRepository paymentFournisseurRepository = mock(PaymentFournisseurRepository.class);
    private final CashRegisterService cashRegisterService = mock(CashRegisterService.class);
    private final TransactionIdGeneratorService idGenerator = mock(TransactionIdGeneratorService.class);
    private final AppConfigurationService appConfigurationService = mock(AppConfigurationService.class);
    private final AccountsPayableApPdfExportService pdfExportService = mock(AccountsPayableApPdfExportService.class);

    private final AccountsPayableService service = new AccountsPayableServiceImpl(
        commandeRepository,
        paymentFournisseurRepository,
        cashRegisterService,
        idGenerator,
        appConfigurationService,
        pdfExportService
    );

    @BeforeEach
    void aucuneDette() {
        when(commandeRepository.findUnpaidCommandesApByPeriod(any(), any(), any(), any())).thenReturn(List.of());
        when(commandeRepository.findUnpaidCommandesApByFournisseurAndPeriod(any(), any(), anyInt(), any(), any()))
            .thenReturn(List.of());
        when(paymentFournisseurRepository.sumPaidAmountsByCommandeIds(any())).thenReturn(List.of());
        when(appConfigurationService.getApDefaultCreditDays()).thenReturn(JOURS_CREDIT_PAR_DEFAUT);
        when(appConfigurationService.getApDefaultCritiqueDays()).thenReturn(JOURS_CRITIQUE_PAR_DEFAUT);
        when(idGenerator.nextId()).thenReturn(1L);
    }

    // ===== échéance =====

    @Nested
    @DisplayName("Calcul de l'échéance")
    class CalculDeLEcheance {

        /** L'échéance court depuis la réception : c'est la livraison qui déclenche le crédit. */
        @Test
        @DisplayName("l'échéance court depuis la réception, plus les jours de crédit")
        void depuisLaReception() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 45, null, null);
            donneCommandes(commande(1, laborex, jour(60), jour(50), 1_000_000));

            assertThat(ligneUnique().dateEcheance()).isEqualTo(jour(50).plusDays(45).toString());
        }

        /** Un bon jamais réceptionné garde la date de commande comme point de départ. */
        @Test
        @DisplayName("à défaut de réception, l'échéance court depuis la commande")
        void aDefautDepuisLaCommande() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 45, null, null);
            Commande commande = commande(1, laborex, jour(60), null, 1_000_000);
            donneCommandes(commande);

            assertThat(ligneUnique().dateEcheance()).isEqualTo(jour(60).plusDays(45).toString());
        }

        @Test
        @DisplayName("les jours de crédit du fournisseur priment")
        void joursDuFournisseur() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 60, null, null);
            donneCommandes(commande(1, laborex, jour(10), jour(10), 1_000_000));

            assertThat(ligneUnique().dateEcheance()).isEqualTo(jour(10).plusDays(60).toString());
        }

        /** Un dépôt régional hérite des conditions négociées avec sa maison mère. */
        @Test
        @DisplayName("à défaut, ceux de la maison mère s'appliquent")
        void joursDeLaMaisonMere() {
            Fournisseur groupe = fournisseur(1, "GROUPE", 90, null, null);
            Fournisseur depot = fournisseur(3, "DEPOT", null, null, groupe);
            donneCommandes(commande(1, depot, jour(10), jour(10), 1_000_000));

            assertThat(ligneUnique().dateEcheance()).isEqualTo(jour(10).plusDays(90).toString());
        }

        @Test
        @DisplayName("à défaut encore, le réglage général s'applique")
        void joursDuReglageGeneral() {
            Fournisseur laborex = fournisseur(3, "LABOREX", null, null, null);
            donneCommandes(commande(1, laborex, jour(10), jour(10), 1_000_000));

            assertThat(ligneUnique().dateEcheance()).isEqualTo(jour(10).plusDays(JOURS_CREDIT_PAR_DEFAUT).toString());
        }
    }

    // ===== statut d'une ligne =====

    @Nested
    @DisplayName("Statut d'un bon de livraison")
    class StatutDUnBon {

        @Test
        @DisplayName("un bon soldé est réglé")
        void bonSolde() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            Commande commande = commande(1, laborex, jour(60), jour(60), 1_000_000);
            commande.setPaimentStatut(PaimentStatut.PAID);
            donneCommandes(commande);

            assertThat(ligneUnique().statut()).isEqualTo(StatutLigneFournisseurAP.REGLE.name());
        }

        /** Un acompte suffit à faire passer le bon en partiel, même échéance dépassée. */
        @Test
        @DisplayName("un acompte fait passer le bon en partiel")
        void bonPartiel() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(commande(1, laborex, jour(90), jour(90), 1_000_000));
            donneReglements(1, 400_000L);

            LigneFournisseurAPDTO ligne = ligneUnique();

            assertThat(ligne.statut()).isEqualTo(StatutLigneFournisseurAP.PARTIEL.name());
            assertThat(ligne.montantRegle()).isEqualTo(400_000L);
            assertThat(ligne.restantDu()).isEqualTo(600_000L);
        }

        @Test
        @DisplayName("un bon impayé dont l'échéance est passée est en retard")
        void bonEnRetard() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(commande(1, laborex, jour(90), jour(90), 1_000_000));

            assertThat(ligneUnique().statut()).isEqualTo(StatutLigneFournisseurAP.EN_RETARD.name());
        }

        @Test
        @DisplayName("un bon impayé dont l'échéance vient est en attente")
        void bonEnAttente() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(commande(1, laborex, jour(5), jour(5), 1_000_000));

            assertThat(ligneUnique().statut()).isEqualTo(StatutLigneFournisseurAP.EN_ATTENTE.name());
        }

        @Test
        @DisplayName("le bon porte la référence de réception, à défaut celle de commande")
        void referenceDuBon() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            Commande avecReception = commande(1, laborex, jour(10), jour(10), 1_000_000);
            avecReception.setReceiptReference("BL-001");
            Commande sansReception = commande(2, laborex, jour(10), jour(10), 500_000);
            donneCommandes(avecReception, sansReception);

            assertThat(service.getLignes(3, null, PageRequest.of(0, 20)).getContent())
                .extracting(LigneFournisseurAPDTO::numBon)
                .containsExactlyInAnyOrder("BL-001", sansReception.getOrderReference());
        }
    }

    // ===== liste des bons =====

    @Nested
    @DisplayName("Liste des bons")
    class ListeDesBons {

        @Test
        @DisplayName("les bons sont classés du plus ancien au plus récent")
        void ordreChronologique() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(
                commande(1, laborex, jour(10), jour(10), 100_000),
                commande(2, laborex, jour(60), jour(60), 200_000),
                commande(3, laborex, jour(30), jour(30), 300_000)
            );

            assertThat(service.getLignes(3, null, PageRequest.of(0, 20)).getContent())
                .extracting(LigneFournisseurAPDTO::dateCommande)
                .isSorted();
        }

        @Test
        @DisplayName("le filtre par statut ne retient que les bons concernés")
        void filtreParStatut() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(
                commande(1, laborex, jour(90), jour(90), 100_000),
                commande(2, laborex, jour(5), jour(5), 200_000)
            );

            Page<LigneFournisseurAPDTO> enRetard = service.getLignes(
                3,
                StatutLigneFournisseurAP.EN_RETARD,
                PageRequest.of(0, 20)
            );

            assertThat(enRetard.getContent()).hasSize(1);
            assertThat(enRetard.getContent().getFirst().statut()).isEqualTo(StatutLigneFournisseurAP.EN_RETARD.name());
        }

        @Test
        @DisplayName("le total annoncé est celui d'après filtrage")
        void totalApresFiltrage() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(
                commande(1, laborex, jour(90), jour(90), 100_000),
                commande(2, laborex, jour(5), jour(5), 200_000)
            );

            assertThat(service.getLignes(3, StatutLigneFournisseurAP.EN_RETARD, PageRequest.of(0, 20)).getTotalElements())
                .isEqualTo(1L);
        }

        @Test
        @DisplayName("les pages se suivent sans recouvrement")
        void pagesDistinctes() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(
                commande(1, laborex, jour(50), jour(50), 100_000),
                commande(2, laborex, jour(40), jour(40), 200_000),
                commande(3, laborex, jour(30), jour(30), 300_000),
                commande(4, laborex, jour(20), jour(20), 400_000)
            );

            List<LigneFournisseurAPDTO> premiere = service.getLignes(3, null, PageRequest.of(0, 2)).getContent();
            List<LigneFournisseurAPDTO> seconde = service.getLignes(3, null, PageRequest.of(1, 2)).getContent();

            assertThat(premiere).hasSize(2);
            assertThat(seconde).hasSize(2);
            assertThat(premiere)
                .extracting(LigneFournisseurAPDTO::commandeId)
                .doesNotContainAnyElementsOf(seconde.stream().map(LigneFournisseurAPDTO::commandeId).toList());
        }

        /** Le téléphone peut demander une page au-delà de la fin : elle doit être vide, pas fatale. */
        @Test
        @DisplayName("une page au-delà de la fin est vide")
        void pageAuDela() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(commande(1, laborex, jour(10), jour(10), 100_000));

            assertThat(service.getLignes(3, null, PageRequest.of(9, 20)).getContent()).isEmpty();
        }
    }

    // ===== comptes =====

    @Nested
    @DisplayName("Comptes par fournisseur")
    class ComptesParFournisseur {

        @Test
        @DisplayName("les bons se cumulent par fournisseur, avec son solde")
        void cumulParFournisseur() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(
                commande(1, laborex, jour(10), jour(10), 1_000_000),
                commande(2, laborex, jour(20), jour(20), 500_000)
            );
            donneReglements(1, 400_000L);

            CompteFournisseurAPDTO compte = service.getComptes().getFirst();

            assertThat(compte.fournisseurId()).isEqualTo(3);
            assertThat(compte.totalCommande()).isEqualTo(1_500_000L);
            assertThat(compte.totalRegle()).isEqualTo(400_000L);
            assertThat(compte.solde()).isEqualTo(1_100_000L);
            assertThat(compte.nbCommandesEnAttente()).isEqualTo(2L);
        }

        @Test
        @DisplayName("les fournisseurs sont classés du plus gros solde au plus petit")
        void classementParSolde() {
            Fournisseur gros = fournisseur(3, "GROS", 30, null, null);
            Fournisseur petit = fournisseur(4, "PETIT", 30, null, null);
            donneCommandes(
                commande(1, petit, jour(10), jour(10), 100_000),
                commande(2, gros, jour(10), jour(10), 9_000_000)
            );

            assertThat(service.getComptes())
                .extracting(CompteFournisseurAPDTO::fournisseurName)
                .containsExactly("GROS", "PETIT");
        }

        @Test
        @DisplayName("la prochaine échéance est la plus proche des bons en attente")
        void prochaineEcheance() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(
                commande(1, laborex, jour(10), jour(10), 100_000),
                commande(2, laborex, jour(40), jour(40), 200_000)
            );

            // Le bon le plus ancien échoit le premier.
            assertThat(service.getComptes().getFirst().prochaineEcheance())
                .isEqualTo(jour(40).plusDays(30).toString());
        }

        @Test
        @DisplayName("un compte sans retard est à jour")
        void compteAJour() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(commande(1, laborex, jour(5), jour(5), 100_000));

            assertThat(service.getComptes().getFirst().statut()).isEqualTo("A_JOUR");
        }

        @Test
        @DisplayName("un compte dont une échéance est passée est en retard")
        void compteEnRetard() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(commande(1, laborex, jour(40), jour(40), 100_000));

            assertThat(service.getComptes().getFirst().statut()).isEqualTo("EN_RETARD");
        }

        /** Au-delà du délai critique après l'échéance, le compte devient critique. */
        @Test
        @DisplayName("un retard dépassant le délai critique rend le compte critique")
        void compteCritique() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, 10, null);
            donneCommandes(commande(1, laborex, jour(60), jour(60), 100_000));

            assertThat(service.getComptes().getFirst().statut()).isEqualTo("CRITIQUE");
        }

        @Test
        @DisplayName("un bon soldé ne compte pas parmi ceux en attente")
        void bonSoldeExclu() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            Commande solde = commande(1, laborex, jour(10), jour(10), 100_000);
            solde.setPaimentStatut(PaimentStatut.PAID);
            donneCommandes(solde);

            CompteFournisseurAPDTO compte = service.getComptes().getFirst();

            assertThat(compte.nbCommandesEnAttente()).isZero();
            assertThat(compte.prochaineEcheance()).isNull();
        }

        @Test
        @DisplayName("aucune dette rend une liste vide")
        void aucuneDette() {
            assertThat(service.getComptes()).isEmpty();
        }
    }

    // ===== synthèse =====

    @Nested
    @DisplayName("Synthèse")
    class Synthese {

        @Test
        @DisplayName("le total dû additionne les restes à payer")
        void totalDu() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(
                commande(1, laborex, jour(10), jour(10), 1_000_000),
                commande(2, laborex, jour(20), jour(20), 500_000)
            );
            donneReglements(1, 400_000L);

            assertThat(service.getSummary().totalDu()).isEqualTo(1_100_000L);
        }

        /** Un bon trop-payé ne doit pas venir en déduction du total dû des autres. */
        @Test
        @DisplayName("un bon trop-payé ne réduit pas le total dû")
        void bonTropPaye() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(
                commande(1, laborex, jour(10), jour(10), 100_000),
                commande(2, laborex, jour(20), jour(20), 500_000)
            );
            donneReglements(1, 150_000L);

            assertThat(service.getSummary().totalDu()).isEqualTo(500_000L);
        }

        @Test
        @DisplayName("les fournisseurs dont une échéance est passée sont dénombrés")
        void echeancesDepassees() {
            Fournisseur enRetard = fournisseur(3, "RETARD", 30, null, null);
            Fournisseur aJour = fournisseur(4, "A JOUR", 30, null, null);
            donneCommandes(
                commande(1, enRetard, jour(90), jour(90), 100_000),
                commande(2, aJour, jour(1), jour(1), 200_000)
            );

            FournisseurAPSummaryDTO synthese = service.getSummary();

            assertThat(synthese.echeancesDepassees()).isEqualTo(1L);
            assertThat(synthese.nbFournisseursActifs()).isEqualTo(2L);
        }

        /** À sept jours, l'échéance est « prochaine » : c'est la fenêtre de préparation du virement. */
        @Test
        @DisplayName("les échéances des sept prochains jours sont signalées")
        void echeancesProchaines() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(commande(1, laborex, jour(27), jour(27), 100_000));

            assertThat(service.getSummary().echeancesProchaines()).isEqualTo(1L);
        }

        @Test
        @DisplayName("une échéance au-delà de sept jours n'est pas encore signalée")
        void echeanceLointaine() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(commande(1, laborex, jour(10), jour(10), 100_000));

            FournisseurAPSummaryDTO synthese = service.getSummary();

            assertThat(synthese.echeancesProchaines()).isZero();
            assertThat(synthese.echeancesDepassees()).isZero();
        }

        @Test
        @DisplayName("aucune dette rend une synthèse à zéro")
        void aucuneDette() {
            FournisseurAPSummaryDTO synthese = service.getSummary();

            assertThat(synthese.totalDu()).isZero();
            assertThat(synthese.nbFournisseursActifs()).isZero();
        }
    }

    // ===== imputation d'un règlement =====

    @Nested
    @DisplayName("Imputation d'un règlement")
    class ImputationDUnReglement {

        @Test
        @DisplayName("un règlement couvrant un bon le solde")
        void bonSolde() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            Commande commande = commande(1, laborex, jour(10), jour(10), 500_000);
            donneCommandes(commande);

            service.enregistrerReglement(3, reglement(500_000, null));

            assertThat(commande.getPaimentStatut()).isEqualTo(PaimentStatut.PAID);
            assertThat(paiementsEnregistres()).hasSize(1);
            assertThat(paiementsEnregistres().getFirst().getPaidAmount()).isEqualTo(500_000);
        }

        @Test
        @DisplayName("un acompte n'entraîne pas le solde du bon")
        void acompte() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            Commande commande = commande(1, laborex, jour(10), jour(10), 500_000);
            donneCommandes(commande);

            service.enregistrerReglement(3, reglement(200_000, null));

            assertThat(commande.getPaimentStatut()).isNotEqualTo(PaimentStatut.PAID);
            verify(commandeRepository, never()).save(any());
        }

        /** Le versement se répartit du plus ancien au plus récent : c'est la dette la plus vieille qui s'éteint. */
        @Test
        @DisplayName("le versement se répartit du bon le plus ancien au plus récent")
        void repartitionDuPlusAncien() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            Commande ancien = commande(1, laborex, jour(60), jour(60), 300_000);
            Commande recent = commande(2, laborex, jour(10), jour(10), 400_000);
            donneCommandes(ancien, recent);

            service.enregistrerReglement(3, reglement(500_000, null));

            assertThat(paiementsEnregistres())
                .extracting(PaymentFournisseur::getPaidAmount)
                .containsExactly(300_000, 200_000);
            assertThat(ancien.getPaimentStatut()).isEqualTo(PaimentStatut.PAID);
            assertThat(recent.getPaimentStatut()).isNotEqualTo(PaimentStatut.PAID);
        }

        /** Un bon désigné est servi d'abord, même s'il n'est pas le plus ancien. */
        @Test
        @DisplayName("un bon désigné est servi avant les autres")
        void bonDesignePrioritaire() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            Commande ancien = commande(1, laborex, jour(60), jour(60), 300_000);
            Commande designe = commande(2, laborex, jour(10), jour(10), 400_000);
            donneCommandes(ancien, designe);

            service.enregistrerReglement(3, reglement(400_000, 2));

            assertThat(paiementsEnregistres().getFirst().getPaidAmount()).isEqualTo(400_000);
            assertThat(designe.getPaimentStatut()).isEqualTo(PaimentStatut.PAID);
            assertThat(ancien.getPaimentStatut()).isNotEqualTo(PaimentStatut.PAID);
        }

        @Test
        @DisplayName("le reliquat d'un bon désigné se répartit sur les autres")
        void reliquatApresBonDesigne() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            Commande ancien = commande(1, laborex, jour(60), jour(60), 300_000);
            Commande designe = commande(2, laborex, jour(10), jour(10), 400_000);
            donneCommandes(ancien, designe);

            service.enregistrerReglement(3, reglement(600_000, 2));

            assertThat(paiementsEnregistres())
                .extracting(PaymentFournisseur::getPaidAmount)
                .containsExactly(400_000, 200_000);
        }

        @Test
        @DisplayName("un bon déjà réglé n'absorbe rien")
        void bonDejaRegle() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            Commande deja = commande(1, laborex, jour(60), jour(60), 300_000);
            Commande autre = commande(2, laborex, jour(10), jour(10), 400_000);
            donneCommandes(deja, autre);
            donneReglements(1, 300_000L);

            service.enregistrerReglement(3, reglement(400_000, null));

            assertThat(paiementsEnregistres()).hasSize(1);
            assertThat(paiementsEnregistres().getFirst().getPaidAmount()).isEqualTo(400_000);
        }

        /**
         * Un versement supérieur à la dette totale : le surplus n'est rattaché à aucun bon et
         * disparaît sans trace. C'est le comportement actuel, consigné ici tel quel.
         */
        @Test
        @DisplayName("un versement supérieur à la dette n'impute que ce qui est dû")
        void versementSuperieurALaDette() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(commande(1, laborex, jour(10), jour(10), 300_000));

            service.enregistrerReglement(3, reglement(500_000, null));

            assertThat(paiementsEnregistres()).hasSize(1);
            assertThat(paiementsEnregistres().getFirst().getPaidAmount()).isEqualTo(300_000);
        }

        @Test
        @DisplayName("le règlement porte sa date, sa référence, son mode et le montant versé")
        void tracabiliteDuReglement() {
            Fournisseur laborex = fournisseur(3, "LABOREX", 30, null, null);
            donneCommandes(commande(1, laborex, jour(10), jour(10), 300_000));

            service.enregistrerReglement(3, reglement(300_000, null));

            PaymentFournisseur paiement = paiementsEnregistres().getFirst();

            assertThat(paiement.getTransactionDate()).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(paiement.getTransactionNumber()).isEqualTo("VIR-001");
            assertThat(paiement.getPaymentMode().getCode()).isEqualTo("VIREMENT");
            assertThat(paiement.getMontantVerse()).isEqualTo(300_000);
            assertThat(paiement.getCommentaire()).isEqualTo("règlement mensuel");
        }

        @Test
        @DisplayName("aucune dette n'enregistre aucun règlement")
        void aucuneDette() {
            service.enregistrerReglement(3, reglement(500_000, null));

            assertThat(paiementsEnregistres()).isEmpty();
        }
    }

    // ===== utilitaires =====

    private LigneFournisseurAPDTO ligneUnique() {
        List<LigneFournisseurAPDTO> lignes = service.getLignes(3, null, PageRequest.of(0, 20)).getContent();
        assertThat(lignes).hasSize(1);
        return lignes.getFirst();
    }

    @SuppressWarnings("unchecked")
    private List<PaymentFournisseur> paiementsEnregistres() {
        ArgumentCaptor<List<PaymentFournisseur>> captor = ArgumentCaptor.forClass(List.class);
        verify(paymentFournisseurRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private void donneCommandes(Commande... commandes) {
        when(commandeRepository.findUnpaidCommandesApByPeriod(any(), any(), any(), any())).thenReturn(List.of(commandes));
        when(commandeRepository.findUnpaidCommandesApByFournisseurAndPeriod(any(), any(), anyInt(), any(), any()))
            .thenReturn(List.of(commandes));
    }

    private void donneReglements(int commandeId, long montant) {
        when(paymentFournisseurRepository.sumPaidAmountsByCommandeIds(any()))
            .thenReturn(List.<Object[]>of(new Object[] { commandeId, montant }));
    }

    private static ReglementFournisseurAPCommand reglement(int montant, Integer commandeId) {
        return new ReglementFournisseurAPCommand(montant, "2026-03-10", "VIR-001", "VIREMENT", "règlement mensuel", commandeId);
    }

    /** Une date située {@code anciennete} jours en arrière. */
    private static LocalDate jour(int anciennete) {
        return LocalDate.now().minusDays(anciennete);
    }

    private static Fournisseur fournisseur(int id, String libelle, Integer joursCredit, Integer joursCritique, Fournisseur parent) {
        Fournisseur fournisseur = new Fournisseur();
        ReflectionTestUtils.setField(fournisseur, "id", id);
        fournisseur.setLibelle(libelle);
        fournisseur.setCode("F" + id);
        fournisseur.setJoursCredit(joursCredit);
        fournisseur.setJoursCritique(joursCritique);
        fournisseur.setParent(parent);
        return fournisseur;
    }

    private static Commande commande(int id, Fournisseur fournisseur, LocalDate dateCommande, LocalDate dateReception, int montant) {
        Commande commande = new Commande();
        commande.setId(id);
        commande.setOrderDate(dateCommande);
        commande.setReceiptDate(dateReception);
        commande.setOrderReference("CMD-" + id);
        commande.setFinalAmount(montant);
        commande.setFournisseur(fournisseur);
        return commande;
    }
}
