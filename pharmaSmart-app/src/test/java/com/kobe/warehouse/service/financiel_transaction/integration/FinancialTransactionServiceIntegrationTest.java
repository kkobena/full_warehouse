package com.kobe.warehouse.service.financiel_transaction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.DefaultTransactionRepository;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionService;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionServiceImpl;
import com.kobe.warehouse.service.financiel_transaction.MvtCaisseReportReportService;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseWrapper;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.receipt.service.MouvementCaisseReceiptService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Les mouvements de caisse consignent ce qui entre et sort du tiroir en dehors des ventes : une
 * entrée de fonds, une sortie pour la banque, un règlement fournisseur, un dépôt de caution.
 *
 * <p>Le point sensible est le <b>sens</b> du mouvement. Il ne se déduit pas du signe du montant —
 * tous les montants sont positifs — mais d'un drapeau {@code credit} posé à l'enregistrement, que le
 * ticket Z lit pour décider s'il ajoute la somme au tiroir ou l'en retranche. Un type oublié à cet
 * endroit fait passer une sortie pour une recette, et le rapprochement du soir ne tombe plus juste
 * sans qu'on sache pourquoi.
 *
 * <p>Ces mouvements s'écrivent dans une table partitionnée par date, avec un identifiant composite et
 * explicite : le genre de détail qui ne se vérifie qu'en écrivant réellement en base.
 */
@DisplayName("FinancialTransactionService — mouvements de caisse sur PostgreSQL")
class FinancialTransactionServiceIntegrationTest extends AbstractFinancialTransactionIntegrationTest {

    private FinancialTransactionService service;
    private CashRegister caisse;
    private CashRegisterService cashRegisterService;

    @BeforeEach
    void cablerLeService() {
        caisse = caisseOuverte();

        UserService userService = mock(UserService.class);
        when(userService.getUser()).thenReturn(utilisateur);

        cashRegisterService = mock(CashRegisterService.class);
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(any())).thenReturn(caisse);

        ReferenceService referenceService = mock(ReferenceService.class);
        when(referenceService.buildNumTransaction()).thenReturn("MVT0001");

        service = new FinancialTransactionServiceImpl(
            IntegrationPostgresDatabase.bean(PaymentTransactionRepository.class),
            userService,
            IntegrationPostgresDatabase.bean(SalesRepository.class),
            cashRegisterService,
            em,
            mock(MvtCaisseReportReportService.class),
            IntegrationPostgresDatabase.bean(DefaultTransactionRepository.class),
            new TransactionIdGeneratorService(em),
            mock(MouvementCaisseReceiptService.class),
            referenceService
        );
    }

    // ===== enregistrement =====

    @Nested
    @DisplayName("Enregistrement d'un mouvement")
    class EnregistrementDUnMouvement {

        @Test
        @DisplayName("un mouvement s'écrit en base et rend son identifiant composite")
        void mouvementEcrit() {
            PaymentId id = service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            em.flush();

            assertThat(id).isNotNull();
            assertThat(id.getId()).isPositive();
            assertThat(id.getTransactionDate()).isEqualTo(aujourdHui());
        }

        @Test
        @DisplayName("le montant est porté par les trois colonnes de montant")
        void troisMontants() {
            PaymentId id = service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            viderLeCache();

            assertThat(colonnes(id, "expected_amount", "paid_amount", "reel_amount")).containsExactly(50_000, 50_000, 50_000);
        }

        @Test
        @DisplayName("le mouvement porte sa référence et la caisse ouverte du caissier")
        void referenceEtCaisse() {
            PaymentId id = service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            viderLeCache();

            Object[] ligne = colonnes(id, "transaction_number", "cash_register_id");

            assertThat(ligne[0]).isEqualTo("MVT0001");
            assertThat(((Number) ligne[1]).intValue()).isEqualTo(caisse.getId());
        }

        /**
         * Aucune caisse ouverte n'empêche la saisie : le service en ouvre une. Sans cela le caissier
         * serait renvoyé vers un autre écran au moment où il a de l'argent en main.
         */
        @Test
        @DisplayName("sans caisse ouverte, une caisse est ouverte pour le mouvement")
        void caisseOuverteAuBesoin() throws Exception {
            CashRegister autreCaisse = caisseOuverte();
            when(cashRegisterService.getLastOpiningUserCashRegisterByUser(any())).thenReturn(null);
            when(cashRegisterService.openCashRegister(any(AppUser.class), any(AppUser.class))).thenReturn(autreCaisse);

            PaymentId id = service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            viderLeCache();

            assertThat(((Number) colonnes(id, "cash_register_id")[0]).intValue()).isEqualTo(autreCaisse.getId());
        }

        @Test
        @DisplayName("la date de transaction demandée est retenue")
        void dateRetenue() {
            LocalDate hier = aujourdHui().minusDays(1);
            FinancialTransactionDTO dto = mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000);
            dto.setTransactionDate(hier);

            PaymentId id = service.create(dto);
            em.flush();

            assertThat(id.getTransactionDate()).isEqualTo(hier);
        }

        /**
         * Le défaut que ce test fixe : la saisie propose « Ajouter un commentaire (optionnel) »,
         * l'écran l'envoie, et il n'était recopié ni à l'enregistrement ni à la relecture. La seule
         * trace du motif d'une sortie de caisse disparaissait ainsi entre le formulaire et la base.
         */
        @Test
        @DisplayName("le commentaire du caissier est conservé")
        void commentaireConserve() {
            FinancialTransactionDTO dto = mouvement(TypeFinancialTransaction.SORTIE_CAISSE, 20_000);
            dto.setCommentaire("Versement banque");

            PaymentId id = service.create(dto);
            viderLeCache();

            assertThat(colonnes(id, "commentaire")[0]).isEqualTo("Versement banque");
        }
    }

    // ===== sens du mouvement =====

    @Nested
    @DisplayName("Sens du mouvement")
    class SensDuMouvement {

        @Test
        @DisplayName("une sortie de caisse et un règlement fournisseur sortent du tiroir")
        void sorties() {
            assertThat(estAuCredit(TypeFinancialTransaction.SORTIE_CAISSE)).isTrue();
            assertThat(estAuCredit(TypeFinancialTransaction.REGLMENT_FOURNISSEUR)).isTrue();
        }

        /**
         * Le défaut que ce test fixe : le sens était décidé par une liste de types écrite à la main,
         * où « Fonds de caisse » manquait. Le ticket Z ajoutait donc au tiroir un prélèvement qui en
         * était sorti — l'écart apparaissait au double du montant.
         */
        @Test
        @DisplayName("un fonds de caisse sort du tiroir lui aussi")
        void fondsDeCaisse() {
            assertThat(estAuCredit(TypeFinancialTransaction.FONDS_CAISSE)).isTrue();
        }

        @Test
        @DisplayName("une entrée de caisse et une caution entrent dans le tiroir")
        void entrees() {
            assertThat(estAuCredit(TypeFinancialTransaction.ENTREE_CAISSE)).isFalse();
            assertThat(estAuCredit(TypeFinancialTransaction.CAUTION)).isFalse();
        }

        private boolean estAuCredit(TypeFinancialTransaction type) {
            PaymentId id = service.create(mouvement(type, 10_000));
            viderLeCache();

            return (Boolean) colonnes(id, "credit")[0];
        }
    }

    // ===== relecture d'un mouvement =====

    @Nested
    @DisplayName("Relecture d'un mouvement")
    class RelectureDUnMouvement {

        /**
         * Le défaut que ce test fixe : le montant n'était jamais recopié dans le DTO — la ligne qui
         * l'affectait était restée commentée après la disparition de {@code getAmount()} de l'entité.
         * Tout mouvement relu par l'écran ou par l'API s'affichait donc à zéro.
         */
        @Test
        @DisplayName("le mouvement relu porte son montant")
        void montantRelu() {
            PaymentId id = service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            viderLeCache();

            assertThat(service.findById(id)).isPresent().get().extracting(FinancialTransactionDTO::getAmount).isEqualTo(50_000);
        }

        @Test
        @DisplayName("le mouvement relu porte son identifiant, son type et son sens")
        void identiteRelue() {
            PaymentId id = service.create(mouvement(TypeFinancialTransaction.SORTIE_CAISSE, 20_000));
            viderLeCache();

            FinancialTransactionDTO dto = service.findById(id).orElseThrow();

            assertThat(dto.getId()).isEqualTo(id.getId());
            assertThat(dto.getTypeFinancialTransaction()).isEqualTo(TypeFinancialTransaction.SORTIE_CAISSE);
            assertThat(dto.isCredit()).isTrue();
        }

        @Test
        @DisplayName("le mouvement relu porte son commentaire et son mode de règlement")
        void detailRelu() {
            FinancialTransactionDTO dto = mouvement(TypeFinancialTransaction.SORTIE_CAISSE, 20_000);
            dto.setCommentaire("Versement banque");
            PaymentId id = service.create(dto);
            viderLeCache();

            FinancialTransactionDTO relu = service.findById(id).orElseThrow();

            assertThat(relu.getCommentaire()).isEqualTo("Versement banque");
            assertThat(relu.getPaymentMode().getCode()).isEqualTo("CASH");
        }

        @Test
        @DisplayName("le mouvement relu nomme le caissier qui l'a saisi")
        void caissierRelu() {
            PaymentId id = service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            viderLeCache();

            assertThat(service.findById(id).orElseThrow().getUserFullName()).isNotBlank();
        }

        @Test
        @DisplayName("un identifiant inconnu ne rend rien")
        void identifiantInconnu() {
            assertThat(service.findById(new PaymentId(999_999L, aujourdHui()))).isEmpty();
        }
    }

    // ===== liste de suivi =====

    @Nested
    @DisplayName("Liste de suivi")
    class ListeDeSuivi {

        @Test
        @DisplayName("le mouvement enregistré apparaît avec son montant")
        void mouvementPresent() {
            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            viderLeCache();

            Page<FinancialTransactionDTO> page = service.findAll(filtre(null), PageRequest.of(0, 20));

            assertThat(page.getContent()).extracting(FinancialTransactionDTO::getAmount).contains(50_000);
        }

        @Test
        @DisplayName("le filtre par type ne retient que les mouvements concernés")
        void filtreParType() {
            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            service.create(mouvement(TypeFinancialTransaction.SORTIE_CAISSE, 20_000));
            viderLeCache();

            Page<FinancialTransactionDTO> sorties = service.findAll(filtre(TypeFinancialTransaction.SORTIE_CAISSE), PageRequest.of(0, 20));

            assertThat(sorties.getContent())
                .isNotEmpty()
                .allSatisfy(dto -> assertThat(dto.getTypeFinancialTransaction()).isEqualTo(TypeFinancialTransaction.SORTIE_CAISSE));
        }

        /** Une période sans mouvement rend une page vide, et non une erreur. */
        @Test
        @DisplayName("une période sans mouvement rend une page vide")
        void periodeSansMouvement() {
            LocalDate plusTard = LocalDate.now().plusMonths(1);

            Page<FinancialTransactionDTO> page = service.findAll(
                new FinancielTransactionFilterDTO(plusTard, plusTard.plusDays(1), null, null, null, null, null, null),
                PageRequest.of(0, 20)
            );

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        /** La liste est paginée : le total annoncé porte sur la période, non sur la page rendue. */
        @Test
        @DisplayName("le total annoncé dépasse la taille de la page")
        void totalHorsPage() {
            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 10_000));
            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 20_000));
            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 30_000));
            viderLeCache();

            Page<FinancialTransactionDTO> page = service.findAll(filtre(null), PageRequest.of(0, 2));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        }

        /** Ce filtre borne sur la date d'écriture du mouvement, non sur la date saisie par le caissier. */
        private FinancielTransactionFilterDTO filtre(TypeFinancialTransaction type) {
            return new FinancielTransactionFilterDTO(
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                null,
                null,
                type,
                null,
                null,
                null
            );
        }
    }

    // ===== rapprochement de caisse =====

    @Nested
    @DisplayName("Rapprochement de caisse")
    class RapprochementDeCaisse {

        @Test
        @DisplayName("les mouvements du jour se présentent avec leur montant et leur référence")
        void listeDesMouvements() {
            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            viderLeCache();

            Page<MvtCaisseDTO> page = service.findAll(filtreCaisse(), PageRequest.of(0, 20));

            assertThat(page.getContent()).isNotEmpty();
            MvtCaisseDTO mvt = page.getContent().getFirst();
            assertThat(mvt.getMontant()).isEqualTo(50_000);
            assertThat(mvt.getReference()).isEqualTo("MVT0001");
            assertThat(mvt.getDate()).isNotBlank();
            assertThat(mvt.getPaymentMode()).isEqualTo("CASH");
        }

        /**
         * Le défaut que ce test fixe : la mise en majuscule portait sur la chaîne vide qui séparait
         * l'initiale du point, et non sur l'initiale. Le nom du caissier s'imprimait « a. KONE ».
         */
        @Test
        @DisplayName("le caissier est nommé par son initiale en majuscule et son nom")
        void initialeDuCaissier() {
            utilisateur.setFirstName("aya");
            utilisateur.setLastName("KONE");
            em.flush();

            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            viderLeCache();

            assertThat(premierMouvement().getUserFullName()).isEqualTo("A. KONE");
        }

        @Test
        @DisplayName("les entrées et les sorties se totalisent séparément")
        void totauxSepares() {
            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            service.create(mouvement(TypeFinancialTransaction.SORTIE_CAISSE, 20_000));
            viderLeCache();

            MvtCaisseWrapper totaux = service.getMvtCaisseSum(filtreCaisse());

            assertThat(totaux.getCreditedAmount()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
            assertThat(totaux.getDebitedAmount()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
        }

        @Test
        @DisplayName("le rapprochement ventile par type et par mode de règlement")
        void ventilations() {
            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            viderLeCache();

            MvtCaisseWrapper totaux = service.getMvtCaisseSum(filtreCaisse());

            assertThat(totaux.getTypeTransactionAmounts()).extracting(tuple -> tuple.key()).contains("ENTREE_CAISSE");
            assertThat(totaux.getModesPaiementAmounts()).extracting(tuple -> tuple.key()).contains("CASH");
        }

        @Test
        @DisplayName("le filtre par type ne totalise que les mouvements retenus")
        void filtreParType() {
            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            service.create(mouvement(TypeFinancialTransaction.SORTIE_CAISSE, 20_000));
            viderLeCache();

            MvtCaisseWrapper totaux = service.getMvtCaisseSum(
                new TransactionFilterDTO(
                    aujourdHui(),
                    aujourdHui(),
                    null,
                    null,
                    Set.of(TypeFinancialTransaction.SORTIE_CAISSE),
                    Set.of(CategorieChiffreAffaire.CA),
                    null,
                    null,
                    null,
                    null
                )
            );

            assertThat(totaux.getCreditedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(totaux.getDebitedAmount()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
        }

        @Test
        @DisplayName("un mouvement d'un autre caissier est écarté par le filtre utilisateur")
        void filtreParCaissier() {
            service.create(mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000));
            viderLeCache();

            TransactionFilterDTO autreCaissier = new TransactionFilterDTO(
                aujourdHui(),
                aujourdHui(),
                999_999L,
                null,
                null,
                Set.of(CategorieChiffreAffaire.CA),
                null,
                null,
                null,
                null
            );

            assertThat(service.findAll(autreCaissier, PageRequest.of(0, 20)).getContent()).isEmpty();
        }

        @Test
        @DisplayName("une journée sans mouvement rend un rapprochement vide")
        void journeeSansMouvement() {
            LocalDate plusTard = aujourdHui().plusMonths(1);

            MvtCaisseWrapper totaux = service.getMvtCaisseSum(
                new TransactionFilterDTO(plusTard, plusTard.plusDays(1), null, null, null, null, null, null, null, null)
            );

            assertThat(totaux.getTypeTransactionAmounts()).isEmpty();
            assertThat(totaux.getCreditedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        private MvtCaisseDTO premierMouvement() {
            return service.findAll(filtreCaisse(), PageRequest.of(0, 20)).getContent().getFirst();
        }

        private TransactionFilterDTO filtreCaisse() {
            return new TransactionFilterDTO(
                aujourdHui(),
                aujourdHui(),
                null,
                null,
                null,
                Set.of(CategorieChiffreAffaire.CA),
                null,
                null,
                null,
                null
            );
        }
    }

    // ===== utilitaires =====

    private FinancialTransactionDTO mouvement(TypeFinancialTransaction type, int montant) {
        FinancialTransactionDTO dto = new FinancialTransactionDTO();
        dto.setAmount(montant);
        dto.setTypeTransaction(type);
        dto.setPaymentMode(modePaiement("CASH"));
        dto.setTransactionDate(aujourdHui());
        return dto;
    }

    private Object[] colonnes(PaymentId id, String... colonnes) {
        Object resultat = em
            .createNativeQuery("SELECT " + String.join(", ", colonnes) + " FROM payment_transaction WHERE id = :id")
            .setParameter("id", id.getId())
            .getSingleResult();
        return resultat instanceof Object[] ligne ? ligne : new Object[] { resultat };
    }

    /** Vide le contexte de persistance : la relecture qui suit vient bien de PostgreSQL. */
    private void viderLeCache() {
        em.flush();
        em.clear();
    }
}
