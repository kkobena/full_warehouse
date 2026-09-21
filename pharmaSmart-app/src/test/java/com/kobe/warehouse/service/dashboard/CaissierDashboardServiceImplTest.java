package com.kobe.warehouse.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.PaymentGroup;
import com.kobe.warehouse.repository.CashRegisterItemRepository;
import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.dashboard.impl.CaissierDashboardServiceImpl;
import com.kobe.warehouse.service.dto.dashboard.CaissierDashboardDTO;
import com.kobe.warehouse.service.dto.dashboard.CaisseStatusDTO;
import com.kobe.warehouse.service.dto.dashboard.DiffereARelancerDTO;
import com.kobe.warehouse.service.dto.dashboard.EncaissementParModeDTO;
import com.kobe.warehouse.service.dto.dashboard.LivraisonAttendueDTO;
import com.kobe.warehouse.service.dto.dashboard.ResumeDifferesDTO;
import com.kobe.warehouse.service.dto.dashboard.SessionEncaissementsDTO;
import com.kobe.warehouse.service.dto.dashboard.VenteRecenteDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Le tableau de bord du préparateur lui montre sa caisse, et seulement la sienne. Les requêtes
 * sous-jacentes sont natives et rendent des {@code Object[]} anonymes : tout le sens — quelle
 * colonne est le fonds, laquelle l'heure d'ouverture, ce qui est encaissé et ce qui reste à
 * recouvrer — est reconstruit ici, en Java. C'est donc ici que ça peut se décaler, et c'est ce que
 * ces tests tiennent.
 *
 * <p>Quatre décisions y sont éprouvées. D'abord le <b>filtrage sur l'utilisateur connecté</b> :
 * sans login, aucune donnée ne doit fuir, pas même un zéro emprunté à la caisse d'un collègue.
 * Ensuite la <b>lecture de l'état de caisse</b>, où {@code OUVERTE} et {@code FERMEE} ne recouvrent
 * pas les quatre statuts de la base, et où l'heure de dernière fermeture ne s'affiche que caisse
 * close. Puis le <b>partage entre encaissé et à recouvrer</b> : un règlement en crédit ou par
 * carnet entre bien dans le total des transactions mais ne remplit pas le tiroir-caisse — les
 * confondre ferait accuser le préparateur d'un manquant. Enfin le <b>classement des différés</b>
 * par ancienneté, qui décide de l'ordre des relances du jour.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CaissierDashboardService — tableau de bord du préparateur")
class CaissierDashboardServiceImplTest {

    private static final String LOGIN = "prepa";
    private static final Integer CAISSE_ID = 42;

    @Mock
    private CashRegisterRepository cashRegisterRepository;

    @Mock
    private CashRegisterItemRepository cashRegisterItemRepository;

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private CommandeRepository commandeRepository;

    private CaissierDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CaissierDashboardServiceImpl(
            cashRegisterRepository,
            cashRegisterItemRepository,
            salesRepository,
            commandeRepository
        );
        connecte(LOGIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void connecte(String login) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }

    private static void deconnecte() {
        SecurityContextHolder.clearContext();
    }

    // ===== état de la caisse =====

    @Nested
    @DisplayName("État de la caisse")
    class EtatDeLaCaisse {

        @Test
        @DisplayName("caisse ouverte : les espèces théoriques sont le fonds plus les encaissements")
        void caisseOuverteAdditionneFondsEtEncaissements() {
            when(cashRegisterRepository.findCurrentByUserLogin(LOGIN)).thenReturn(
                List.<Object[]>of(ligneCaisse(50_000L, LocalDateTime.of(2026, 9, 18, 8, 5), CashRegisterStatut.OPEN, null))
            );
            when(cashRegisterItemRepository.sumEncaissementsEspeces(eq(CAISSE_ID), anySet(), anySet())).thenReturn(275_000L);

            CaisseStatusDTO resultat = service.getCaisseStatus();

            assertThat(resultat.fondOuverture()).isEqualTo(50_000L);
            assertThat(resultat.encaissementsEspeces()).isEqualTo(275_000L);
            assertThat(resultat.especesTheoriques()).isEqualTo(325_000L);
            assertThat(resultat.etat()).isEqualTo("OUVERTE");
            assertThat(resultat.heureOuverture()).isEqualTo("08:05");
        }

        /**
         * Une caisse encore ouverte n'a pas d'heure de fermeture à montrer : afficher celle de la
         * veille laisserait croire la session close.
         */
        @Test
        @DisplayName("caisse ouverte : aucune heure de fermeture n'est exposée")
        void caisseOuverteNExposePasDeFermeture() {
            when(cashRegisterRepository.findCurrentByUserLogin(LOGIN)).thenReturn(
                List.<Object[]>of(ligneCaisse(0L, LocalDateTime.now(), CashRegisterStatut.OPEN, LocalDateTime.now().minusDays(1)))
            );
            when(cashRegisterItemRepository.sumEncaissementsEspeces(anyInt(), anySet(), anySet())).thenReturn(0L);

            assertThat(service.getCaisseStatus().derniereFermeture()).isNull();
        }

        @Test
        @DisplayName("statut PENDING compte comme ouverte — la session n'est pas encore validée")
        void statutPendingResteOuverte() {
            when(cashRegisterRepository.findCurrentByUserLogin(LOGIN)).thenReturn(
                List.<Object[]>of(ligneCaisse(10_000L, LocalDateTime.of(2026, 9, 18, 9, 0), CashRegisterStatut.PENDING, null))
            );
            when(cashRegisterItemRepository.sumEncaissementsEspeces(anyInt(), anySet(), anySet())).thenReturn(0L);

            assertThat(service.getCaisseStatus().etat()).isEqualTo("OUVERTE");
        }

        @Test
        @DisplayName("caisse fermée dans la journée : l'heure de fermeture est exposée")
        void caisseFermeeExposeLHeureDeFermeture() {
            LocalDateTime fermeture = LocalDateTime.of(2026, 9, 18, 19, 30);
            when(cashRegisterRepository.findCurrentByUserLogin(LOGIN)).thenReturn(
                List.<Object[]>of(ligneCaisse(50_000L, LocalDateTime.of(2026, 9, 18, 8, 0), CashRegisterStatut.CLOSED, fermeture))
            );
            when(cashRegisterItemRepository.sumEncaissementsEspeces(anyInt(), anySet(), anySet())).thenReturn(100_000L);

            CaisseStatusDTO resultat = service.getCaisseStatus();

            assertThat(resultat.etat()).isEqualTo("FERMEE");
            assertThat(resultat.derniereFermeture()).isEqualTo(fermeture);
            assertThat(resultat.especesTheoriques()).isEqualTo(150_000L);
        }

        /**
         * Aucune ligne veut dire « aucune caisse ouverte aujourd'hui » : les montants retombent à
         * zéro, et la seule information utile devient la date de la dernière fermeture.
         */
        @Test
        @DisplayName("aucune caisse aujourd'hui : montants à zéro et dernière fermeture connue")
        void aucuneCaisseAujourdhuiRemonteLaDerniereFermeture() {
            LocalDateTime derniere = LocalDateTime.of(2026, 9, 17, 19, 0);
            when(cashRegisterRepository.findCurrentByUserLogin(LOGIN)).thenReturn(List.of());
            when(cashRegisterRepository.findLastClosedTimeByUserLogin(LOGIN)).thenReturn(List.of(derniere));

            CaisseStatusDTO resultat = service.getCaisseStatus();

            assertThat(resultat.etat()).isEqualTo("FERMEE");
            assertThat(resultat.fondOuverture()).isZero();
            assertThat(resultat.encaissementsEspeces()).isZero();
            assertThat(resultat.especesTheoriques()).isZero();
            assertThat(resultat.heureOuverture()).isNull();
            assertThat(resultat.derniereFermeture()).isEqualTo(derniere);
            verifyNoInteractions(cashRegisterItemRepository);
        }

        @Test
        @DisplayName("jamais de caisse fermée : la date reste nulle sans lever d'erreur")
        void jamaisDeCaisseFermee() {
            when(cashRegisterRepository.findCurrentByUserLogin(LOGIN)).thenReturn(List.of());
            when(cashRegisterRepository.findLastClosedTimeByUserLogin(LOGIN)).thenReturn(List.of());

            assertThat(service.getCaisseStatus().derniereFermeture()).isNull();
        }

        /**
         * {@code COALESCE} protège la requête, mais pas un jeu de résultats vide : la somme peut
         * revenir nulle et l'addition qui suit lèverait une {@code NullPointerException}.
         */
        @Test
        @DisplayName("une somme d'espèces nulle est traitée comme zéro")
        void sommeNulleTraiteeCommeZero() {
            when(cashRegisterRepository.findCurrentByUserLogin(LOGIN)).thenReturn(
                List.<Object[]>of(ligneCaisse(50_000L, LocalDateTime.of(2026, 9, 18, 8, 0), CashRegisterStatut.OPEN, null))
            );
            when(cashRegisterItemRepository.sumEncaissementsEspeces(anyInt(), anySet(), anySet())).thenReturn(null);

            CaisseStatusDTO resultat = service.getCaisseStatus();

            assertThat(resultat.encaissementsEspeces()).isZero();
            assertThat(resultat.especesTheoriques()).isEqualTo(50_000L);
        }

        @Test
        @DisplayName("sans utilisateur connecté, rien n'est lu en base")
        void sansLoginAucuneLecture() {
            deconnecte();

            CaisseStatusDTO resultat = service.getCaisseStatus();

            assertThat(resultat.etat()).isEqualTo("FERMEE");
            assertThat(resultat.especesTheoriques()).isZero();
            verifyNoInteractions(cashRegisterRepository, cashRegisterItemRepository);
        }

        private Object[] ligneCaisse(Long fonds, LocalDateTime ouverture, CashRegisterStatut statut, LocalDateTime fermeture) {
            return new Object[] { CAISSE_ID, fonds, ouverture, statut.name(), fermeture };
        }
    }

    // ===== encaissements de la session =====

    @Nested
    @DisplayName("Encaissements de la session")
    class EncaissementsDeLaSession {

        @BeforeEach
        void caisseOuverte() {
            lenient().when(cashRegisterRepository.findCurrentIdByUserLogin(LOGIN)).thenReturn(List.of(CAISSE_ID));
        }

        /**
         * Le partage est la raison d'être de l'écran : espèces, mobile, carte, chèque et virement
         * sont dans le tiroir ou sur le compte, le crédit non. Ranger un crédit dans l'encaissé
         * ferait apparaître un manquant à la fermeture.
         */
        @Test
        @DisplayName("sépare ce qui est encaissé de ce qui reste à recouvrer, selon le groupe de paiement")
        void separeEncaisseEtARecouvrer() {
            when(cashRegisterItemRepository.findEncaissementsParMode(eq(CAISSE_ID), anySet())).thenReturn(
                List.of(
                    mode("CASH", "ESPECE", PaymentGroup.CASH, 300_000L),
                    mode("OM", "ORANGE", PaymentGroup.MOBILE, 50_000L),
                    mode("CB", "CARTE BANCAIRE", PaymentGroup.CB, 25_000L),
                    mode("CREDIT", "CREDIT", PaymentGroup.CREDIT, 40_000L)
                )
            );
            when(salesRepository.findSalesEncaissementsForCaisse(CAISSE_ID)).thenReturn(List.of());

            SessionEncaissementsDTO resultat = service.getSessionEncaissements();

            assertThat(resultat.totalEncaisse()).isEqualTo(375_000L);
            assertThat(resultat.totalARecouvrer()).isEqualTo(40_000L);
        }

        @Test
        @DisplayName("une ligne par mode utilisé, dans l'ordre rendu par la requête")
        void uneLigneParMode() {
            when(cashRegisterItemRepository.findEncaissementsParMode(eq(CAISSE_ID), anySet())).thenReturn(
                List.of(mode("CASH", "ESPECE", PaymentGroup.CASH, 300_000L), mode("WAVE", "WAVE", PaymentGroup.MOBILE, 15_000L))
            );
            when(salesRepository.findSalesEncaissementsForCaisse(CAISSE_ID)).thenReturn(List.of());

            SessionEncaissementsDTO resultat = service.getSessionEncaissements();

            assertThat(resultat.lignes())
                .extracting(EncaissementParModeDTO::code, EncaissementParModeDTO::libelle, EncaissementParModeDTO::montant)
                .containsExactly(
                    tuple("CASH", "ESPECE", 300_000L),
                    tuple("WAVE", "WAVE", 15_000L)
                );
        }

        /**
         * Carnet et différé ne passent pas par {@code payment_transaction} : ils viennent des
         * ventes. Ils s'ajoutent donc au reste à recouvrer, et jamais à l'encaissé.
         */
        @Test
        @DisplayName("carnet et différé s'ajoutent au reste à recouvrer")
        void carnetEtDiffereGonflentLeARecouvrer() {
            when(cashRegisterItemRepository.findEncaissementsParMode(eq(CAISSE_ID), anySet())).thenReturn(
                List.of(mode("CASH", "ESPECE", PaymentGroup.CASH, 100_000L), mode("CREDIT", "CREDIT", PaymentGroup.CREDIT, 10_000L))
            );
            when(salesRepository.findSalesEncaissementsForCaisse(CAISSE_ID)).thenReturn(
                List.<Object[]>of(new Object[] { 30_000L, 20_000L, 17 })
            );

            SessionEncaissementsDTO resultat = service.getSessionEncaissements();

            assertThat(resultat.carnet()).isEqualTo(30_000L);
            assertThat(resultat.differe()).isEqualTo(20_000L);
            assertThat(resultat.totalEncaisse()).isEqualTo(100_000L);
            assertThat(resultat.totalARecouvrer()).isEqualTo(60_000L);
            assertThat(resultat.nombreTransactions()).isEqualTo(17);
        }

        /** Postgres rend les {@code SUM} en {@code numeric} : la conversion doit l'accepter. */
        @Test
        @DisplayName("accepte les sommes rendues en BigDecimal par Postgres")
        void accepteLesBigDecimal() {
            when(cashRegisterItemRepository.findEncaissementsParMode(eq(CAISSE_ID), anySet())).thenReturn(
                List.<Object[]>of(new Object[] { "CASH", "ESPECE", PaymentGroup.CASH.name(), BigDecimal.valueOf(123_456L) })
            );
            when(salesRepository.findSalesEncaissementsForCaisse(CAISSE_ID)).thenReturn(
                List.<Object[]>of(new Object[] { BigDecimal.valueOf(1_000L), BigDecimal.valueOf(2_000L), BigDecimal.valueOf(3L) })
            );

            SessionEncaissementsDTO resultat = service.getSessionEncaissements();

            assertThat(resultat.totalEncaisse()).isEqualTo(123_456L);
            assertThat(resultat.carnet()).isEqualTo(1_000L);
            assertThat(resultat.differe()).isEqualTo(2_000L);
            assertThat(resultat.nombreTransactions()).isEqualTo(3);
        }

        @Test
        @DisplayName("sans caisse ouverte, aucun encaissement n'est cherché")
        void sansCaisseAucuneLecture() {
            when(cashRegisterRepository.findCurrentIdByUserLogin(LOGIN)).thenReturn(List.of());

            SessionEncaissementsDTO resultat = service.getSessionEncaissements();

            assertThat(resultat.lignes()).isEmpty();
            assertThat(resultat.totalEncaisse()).isZero();
            assertThat(resultat.totalARecouvrer()).isZero();
            assertThat(resultat.nombreTransactions()).isZero();
            verifyNoInteractions(cashRegisterItemRepository);
            verify(salesRepository, never()).findSalesEncaissementsForCaisse(any());
        }

        @Test
        @DisplayName("sans utilisateur connecté, rien n'est lu en base")
        void sansLoginAucuneLecture() {
            deconnecte();

            assertThat(service.getSessionEncaissements().lignes()).isEmpty();
            verifyNoInteractions(cashRegisterRepository, cashRegisterItemRepository, salesRepository);
        }

        private Object[] mode(String code, String libelle, PaymentGroup groupe, long montant) {
            return new Object[] { code, libelle, groupe.name(), montant };
        }
    }

    // ===== différés à relancer =====

    @Nested
    @DisplayName("Différés à relancer")
    class DifferesARelancer {

        /**
         * Le niveau d'urgence est ce qui ordonne la journée de relances : échu du jour, retard
         * ordinaire, et au-delà d'une semaine un dossier qui ne se règlera plus tout seul.
         */
        @Test
        @DisplayName("classe chaque différé selon son ancienneté")
        void classeParAnciennete() {
            when(salesRepository.findDifferesARelancer()).thenReturn(
                List.of(
                    differe(1L, "Kouassi Yao", 10_000L, 0),
                    differe(2L, "Aya Traore", 5_000L, 3),
                    differe(3L, "Bamba Sekou", 25_000L, 7),
                    differe(4L, "Diaby Mariam", 40_000L, 8)
                )
            );

            ResumeDifferesDTO resultat = service.getDifferesRelance();

            assertThat(resultat.differes())
                .extracting(DiffereARelancerDTO::saleId, DiffereARelancerDTO::urgence)
                .containsExactly(
                    tuple(1L, "AUJOURD_HUI"),
                    tuple(2L, "RETARD"),
                    tuple(3L, "RETARD"),
                    tuple(4L, "CRITIQUE")
                );
        }

        @Test
        @DisplayName("ne compte comme échéance du jour que les différés sans retard")
        void compteLesEcheancesDuJour() {
            when(salesRepository.findDifferesARelancer()).thenReturn(
                List.of(differe(1L, "A", 1_000L, 0), differe(2L, "B", 2_000L, 0), differe(3L, "C", 3_000L, 5))
            );

            ResumeDifferesDTO resultat = service.getDifferesRelance();

            assertThat(resultat.nombreEcheancesAujourdhui()).isEqualTo(2);
            assertThat(resultat.montantTotalDu()).isEqualTo(6_000L);
        }

        @Test
        @DisplayName("reporte le téléphone et l'échéance, dont dépend la relance")
        void reporteTelephoneEtEcheance() {
            LocalDate echeance = LocalDate.of(2026, 9, 11);
            when(salesRepository.findDifferesARelancer()).thenReturn(
                List.<Object[]>of(new Object[] { 9L, "Kone Ali", "0102030405", 12_500L, echeance, 7 })
            );

            DiffereARelancerDTO differe = service.getDifferesRelance().differes().getFirst();

            assertThat(differe.clientNom()).isEqualTo("Kone Ali");
            assertThat(differe.clientTelephone()).isEqualTo("0102030405");
            assertThat(differe.dateEcheance()).isEqualTo(echeance);
            assertThat(differe.joursRetard()).isEqualTo(7);
            assertThat(differe.montantDu()).isEqualTo(12_500L);
        }

        @Test
        @DisplayName("aucun différé : résumé à zéro, liste vide")
        void aucunDiffere() {
            when(salesRepository.findDifferesARelancer()).thenReturn(List.of());

            ResumeDifferesDTO resultat = service.getDifferesRelance();

            assertThat(resultat.nombreEcheancesAujourdhui()).isZero();
            assertThat(resultat.montantTotalDu()).isZero();
            assertThat(resultat.differes()).isEmpty();
        }

        /** Les différés sont une donnée d'officine, pas une donnée de caisse : ils ne sont pas filtrés sur le login. */
        @Test
        @DisplayName("restent visibles même sans utilisateur connecté")
        void visiblesSansLogin() {
            deconnecte();
            when(salesRepository.findDifferesARelancer()).thenReturn(List.<Object[]>of(differe(1L, "A", 1_000L, 0)));

            assertThat(service.getDifferesRelance().differes()).hasSize(1);
        }

        private Object[] differe(long saleId, String nom, long montant, int joursRetard) {
            return new Object[] { saleId, nom, "0102030405", montant, LocalDate.now().minusDays(joursRetard), joursRetard };
        }
    }

    // ===== livraisons du jour =====

    @Nested
    @DisplayName("Livraisons du jour")
    class LivraisonsDuJour {

        @Test
        @DisplayName("reporte fournisseur et nombre de références de chaque commande attendue")
        void reporteLesLivraisons() {
            when(commandeRepository.findLivraisonsAttenduesAujourdhui(anySet())).thenReturn(
                List.<Object[]>of(new Object[] { 101, "LABOREX", 27 }, new Object[] { 102, "DPCI", 4 })
            );

            List<LivraisonAttendueDTO> resultat = service.getLivraisonsJour();

            assertThat(resultat)
                .extracting(
                    LivraisonAttendueDTO::commandeId,
                    LivraisonAttendueDTO::fournisseurNom,
                    LivraisonAttendueDTO::nombreReferences
                )
                .containsExactly(
                    tuple(101, "LABOREX", 27),
                    tuple(102, "DPCI", 4)
                );
        }

        /**
         * {@code COUNT} revient en {@code BigInteger} ou {@code Long} selon le pilote ; la
         * conversion doit couvrir les deux plutôt que de parier sur l'un.
         */
        @Test
        @DisplayName("accepte un compteur rendu en Long")
        void accepteUnCompteurLong() {
            when(commandeRepository.findLivraisonsAttenduesAujourdhui(anySet())).thenReturn(
                List.<Object[]>of(new Object[] { 101, "LABOREX", 12L })
            );

            assertThat(service.getLivraisonsJour().getFirst().nombreReferences()).isEqualTo(12);
        }

        @Test
        @DisplayName("aucune livraison attendue : liste vide")
        void aucuneLivraison() {
            when(commandeRepository.findLivraisonsAttenduesAujourdhui(anySet())).thenReturn(List.of());

            assertThat(service.getLivraisonsJour()).isEmpty();
        }
    }

    // ===== ventes récentes =====

    @Nested
    @DisplayName("Ventes récentes")
    class VentesRecentes {

        @Test
        @DisplayName("reporte reçu, montant, type et client de chaque vente")
        void reporteLesVentes() {
            LocalDateTime heure = LocalDateTime.of(2026, 9, 18, 11, 42);
            when(salesRepository.findVentesRecentesByCaissier(LOGIN)).thenReturn(
                List.<Object[]>of(new Object[] { 7L, "VNO000007", 18_500L, heure, "COMPTANT", "Kouassi Yao" })
            );

            VenteRecenteDTO vente = service.getVentesRecentes(null).getFirst();

            assertThat(vente.saleId()).isEqualTo(7L);
            assertThat(vente.numeroRecu()).isEqualTo("VNO000007");
            assertThat(vente.montant()).isEqualTo(18_500L);
            assertThat(vente.dateVente()).isEqualTo(heure);
            assertThat(vente.typeVente()).isEqualTo("COMPTANT");
            assertThat(vente.clientNom()).isEqualTo("Kouassi Yao");
        }

        /** Le mode de paiement n'est pas encore une donnée à part : il recopie le type de vente. */
        @Test
        @DisplayName("le mode de paiement affiché reprend le type de vente")
        void modePaiementReprendLeType() {
            when(salesRepository.findVentesRecentesByCaissier(LOGIN)).thenReturn(
                List.<Object[]>of(new Object[] { 7L, "VNO000007", 1_000L, LocalDateTime.now(), "DIFFERE", null })
            );

            VenteRecenteDTO vente = service.getVentesRecentes(null).getFirst();

            assertThat(vente.modePaiement()).isEqualTo("DIFFERE");
            assertThat(vente.clientNom()).isNull();
        }

        @Test
        @DisplayName("sans limite demandée, huit ventes au plus")
        void limiteParDefautAHuit() {
            when(salesRepository.findVentesRecentesByCaissier(LOGIN)).thenReturn(vingtVentes());

            assertThat(service.getVentesRecentes(null)).hasSize(8);
        }

        @Test
        @DisplayName("la limite demandée est respectée")
        void limiteDemandeeRespectee() {
            when(salesRepository.findVentesRecentesByCaissier(LOGIN)).thenReturn(vingtVentes());

            assertThat(service.getVentesRecentes(3)).hasSize(3);
        }

        /** Une limite nulle ou négative n'a pas de sens : elle retombe sur le défaut plutôt que de tout vider. */
        @Test
        @DisplayName("une limite nulle ou négative retombe sur le défaut")
        void limiteAberranteRetombeSurLeDefaut() {
            when(salesRepository.findVentesRecentesByCaissier(LOGIN)).thenReturn(vingtVentes());

            assertThat(service.getVentesRecentes(0)).hasSize(8);
            assertThat(service.getVentesRecentes(-5)).hasSize(8);
        }

        @Test
        @DisplayName("moins de ventes que la limite : tout est rendu")
        void moinsDeVentesQueLaLimite() {
            when(salesRepository.findVentesRecentesByCaissier(LOGIN)).thenReturn(vingtVentes().subList(0, 2));

            assertThat(service.getVentesRecentes(8)).hasSize(2);
        }

        @Test
        @DisplayName("sans utilisateur connecté, aucune vente n'est lue")
        void sansLoginAucuneLecture() {
            deconnecte();

            assertThat(service.getVentesRecentes(8)).isEmpty();
            verifyNoInteractions(salesRepository);
        }

        private List<Object[]> vingtVentes() {
            return IntStream.rangeClosed(1, 20)
                .<Object[]>mapToObj(i ->
                    new Object[] { (long) i, "VNO" + i, 1_000L * i, LocalDateTime.now().minusMinutes(i), "COMPTANT", null }
                )
                .toList();
        }
    }

    // ===== assemblage =====

    @Nested
    @DisplayName("Assemblage du tableau de bord")
    class Assemblage {

        /**
         * L'écran n'appelle qu'une fois le serveur : si un bloc manquait à l'assemblage, la tuile
         * correspondante resterait vide sans la moindre erreur.
         */
        @Test
        @DisplayName("un seul appel rend les cinq blocs de l'écran")
        void rendLesCinqBlocs() {
            when(cashRegisterRepository.findCurrentByUserLogin(LOGIN)).thenReturn(
                List.<Object[]>of(new Object[] { CAISSE_ID, 50_000L, LocalDateTime.of(2026, 9, 18, 8, 0), "OPEN", null })
            );
            when(cashRegisterItemRepository.sumEncaissementsEspeces(anyInt(), anySet(), anySet())).thenReturn(10_000L);
            when(cashRegisterRepository.findCurrentIdByUserLogin(LOGIN)).thenReturn(List.of(CAISSE_ID));
            when(cashRegisterItemRepository.findEncaissementsParMode(eq(CAISSE_ID), anySet())).thenReturn(
                List.<Object[]>of(new Object[] { "CASH", "ESPECE", PaymentGroup.CASH.name(), 10_000L })
            );
            when(salesRepository.findSalesEncaissementsForCaisse(CAISSE_ID)).thenReturn(
                List.<Object[]>of(new Object[] { 0L, 0L, 1 })
            );
            when(salesRepository.findDifferesARelancer()).thenReturn(
                List.<Object[]>of(new Object[] { 1L, "Kouassi", "0102030405", 5_000L, LocalDate.now(), 0 })
            );
            when(commandeRepository.findLivraisonsAttenduesAujourdhui(anySet())).thenReturn(
                List.<Object[]>of(new Object[] { 101, "LABOREX", 3 })
            );
            when(salesRepository.findVentesRecentesByCaissier(LOGIN)).thenReturn(
                List.<Object[]>of(new Object[] { 1L, "VNO1", 10_000L, LocalDateTime.now(), "COMPTANT", null })
            );

            CaissierDashboardDTO resultat = service.getDashboardData();

            assertThat(resultat.caisseStatus().especesTheoriques()).isEqualTo(60_000L);
            assertThat(resultat.sessionEncaissements().totalEncaisse()).isEqualTo(10_000L);
            assertThat(resultat.resumeDifferes().nombreEcheancesAujourdhui()).isEqualTo(1);
            assertThat(resultat.livraisonsAttendues()).hasSize(1);
            assertThat(resultat.ventesRecentes()).hasSize(1);
        }
    }
}
