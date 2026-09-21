package com.kobe.warehouse.service.activity_summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.PaymentTransactionRepository;
import com.kobe.warehouse.repository.SalePaymentRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.TiersPayantService;
import com.kobe.warehouse.service.dto.ChiffreAffaireDTO;
import com.kobe.warehouse.service.dto.projection.ChiffreAffaireAchat;
import com.kobe.warehouse.service.dto.projection.MouvementCaisse;
import com.kobe.warehouse.service.dto.projection.ReglementTiersPayants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Le résumé d'activité est l'état que le titulaire consulte pour boucler sa journée : chiffre
 * d'affaires, ventilation des recettes, mouvements de caisse, achats fournisseurs et tiers payants.
 *
 * <p>Deux de ses sections ne sont pas calculées en Java mais par des <b>fonctions stockées</b> rendant
 * du JSON, relu ici avec Jackson. Le service enveloppe chaque lecture dans un {@code catch} qui rend
 * {@code null} : une officine dont la fonction échouerait verrait un état à zéro, sans erreur. C'est
 * une décision de robustesse défendable, mais elle mérite d'être fixée pour ce qu'elle est — un état
 * vide ne doit pas pouvoir venir d'ailleurs que d'une absence de données.
 */
@DisplayName("ActivitySummaryService — résumé d'activité")
class ActivitySummaryServiceImplTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 31);

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final TiersPayantService payantService = mock(TiersPayantService.class);
    private final InvoicePaymentRepository invoicePaymentRepository = mock(InvoicePaymentRepository.class);
    private final CommandeRepository commandeRepository = mock(CommandeRepository.class);
    private final PaymentTransactionRepository paymentTransactionRepository = mock(PaymentTransactionRepository.class);
    private final SalePaymentRepository paymentRepository = mock(SalePaymentRepository.class);
    private final SalesRepository salesRepository = mock(SalesRepository.class);
    private final ActivitySummaryReportService reportService = mock(ActivitySummaryReportService.class);

    private final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final ActivitySummaryService service = new ActivitySummaryServiceImpl(
        payantService,
        invoicePaymentRepository,
        commandeRepository,
        paymentTransactionRepository,
        paymentRepository,
        salesRepository,
        reportService,
        objectMapper
    );

    @BeforeEach
    void journeeVide() {
        when(salesRepository.getChiffreAffaire(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn("null");
        when(commandeRepository.fetchAchats(any(), any(), any(OrderStatut.class))).thenReturn(null);
        when(paymentTransactionRepository.findMouvementsCaisse(any(), any())).thenReturn(List.of());
        when(paymentRepository.findRecettes(any(), any())).thenReturn(List.of());
        when(invoicePaymentRepository.findReglementTierspayant(any(), any(), any(), anyInt(), anyInt())).thenReturn("null");
        when(payantService.fetchAchatTiersPayant(any(), any(), any(), any())).thenReturn(Page.empty());
    }

    // ===== chiffre d'affaires =====

    @Nested
    @DisplayName("Chiffre d'affaires")
    class ChiffreDAffaires {

        @Test
        @DisplayName("les montants de la fonction stockée sont repris et les dérivés calculés")
        void montantsEtDerives() {
            donneChiffreAffaire(
                """
                {"montantTtc":1000000,"montantHt":900000,"montantRemise":50000,"montantDiffere":20000,
                 "montantTp":80000,"montantAchat":600000,"montantRemiseUg":0,"payments":[]}
                """
            );

            var ca = service.getChiffreAffaire(DEBUT, FIN).chiffreAffaire();

            assertThat(ca.montantTtc()).isEqualByComparingTo("1000000");
            assertThat(ca.montantHt()).isEqualByComparingTo("900000");
            assertThat(ca.montantRemise()).isEqualByComparingTo("50000");
            // La TVA est l'écart entre le TTC et le HT, le net le TTC diminué de la remise.
            assertThat(ca.montantTva()).isEqualByComparingTo("100000");
            assertThat(ca.montantNet()).isEqualByComparingTo("950000");
            // Le crédit réunit le tiers payant et le différé ; la marge est le HT moins l'achat.
            assertThat(ca.montantCredit()).isEqualByComparingTo("100000");
            assertThat(ca.marge()).isEqualByComparingTo("300000");
        }

        /**
         * Les espèces se distinguent du reste : c'est ce qui doit se retrouver dans le tiroir, et donc
         * la seule ligne que le titulaire peut recompter à la main.
         */
        @Test
        @DisplayName("les espèces se distinguent des autres modes de règlement")
        void especesDistinguees() {
            donneChiffreAffaire(
                """
                {"montantTtc":0,"montantHt":0,"montantRemise":0,"montantDiffere":0,"montantTp":0,
                 "montantAchat":0,"montantRemiseUg":0,
                 "payments":[
                   {"realAmount":600000,"paidAmount":600000,"code":"CASH","libelle":"Espèces"},
                   {"realAmount":250000,"paidAmount":250000,"code":"CB","libelle":"Carte"},
                   {"realAmount":150000,"paidAmount":150000,"code":"OM","libelle":"Orange Money"}
                 ]}
                """
            );

            var ca = service.getChiffreAffaire(DEBUT, FIN).chiffreAffaire();

            assertThat(ca.montantEspece()).isEqualByComparingTo("600000");
            assertThat(ca.montantAutreModePaiement()).isEqualByComparingTo("400000");
            assertThat(ca.montantRegle()).isEqualByComparingTo("1000000");
        }

        @Test
        @DisplayName("le total réglé est la somme de toutes les recettes")
        void totalRegle() {
            donneChiffreAffaire(
                """
                {"montantTtc":0,"montantHt":0,"montantRemise":0,"montantDiffere":0,"montantTp":0,
                 "montantAchat":0,"montantRemiseUg":0,
                 "payments":[{"realAmount":300000,"paidAmount":300000,"code":"CASH","libelle":"Espèces"}]}
                """
            );

            ChiffreAffaireDTO resultat = service.getChiffreAffaire(DEBUT, FIN);

            assertThat(resultat.recettes()).hasSize(1);
            assertThat(resultat.chiffreAffaire().montantRegle()).isEqualByComparingTo("300000");
        }

        /**
         * La lecture du JSON est protégée par un {@code catch} : une fonction stockée en échec rend un
         * état à zéro plutôt qu'une erreur. Il faut que ce zéro soit complet et non un objet à trous.
         */
        @Test
        @DisplayName("une fonction stockée illisible rend un état à zéro, sans erreur")
        void fonctionIllisible() {
            when(salesRepository.getChiffreAffaire(any(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn("ceci n'est pas du JSON");

            var ca = service.getChiffreAffaire(DEBUT, FIN).chiffreAffaire();

            assertThat(ca.montantTtc()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(ca.montantRegle()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(ca.marge()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("une journée sans vente rend un état à zéro et aucune recette")
        void journeeSansVente() {
            ChiffreAffaireDTO resultat = service.getChiffreAffaire(DEBUT, FIN);

            assertThat(resultat.recettes()).isEmpty();
            assertThat(resultat.chiffreAffaire().montantTtc()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("les achats et les mouvements de caisse accompagnent le chiffre d'affaires")
        void achatsEtMouvements() {
            ChiffreAffaireAchat achats = mock(ChiffreAffaireAchat.class);
            when(commandeRepository.fetchAchats(DEBUT, FIN, OrderStatut.CLOSED)).thenReturn(achats);
            MouvementCaisse mouvement = mock(MouvementCaisse.class);
            when(paymentTransactionRepository.findMouvementsCaisse(DEBUT, FIN)).thenReturn(List.of(mouvement));

            ChiffreAffaireDTO resultat = service.getChiffreAffaire(DEBUT, FIN);

            assertThat(resultat.achats()).isSameAs(achats);
            assertThat(resultat.mouvementCaisses()).containsExactly(mouvement);
        }
    }

    // ===== périmètres délégués =====

    @Nested
    @DisplayName("Périmètres délégués")
    class PerimetresDelegues {

        /** Seules les commandes clôturées sont des achats : une réception en cours n'est pas un achat. */
        @Test
        @DisplayName("les achats fournisseurs ne retiennent que les commandes clôturées")
        void achatsCloturesSeulement() {
            service.fetchAchats(DEBUT, FIN, PageRequest.of(0, 20));

            verify(commandeRepository).fetchAchats(DEBUT, FIN, OrderStatut.CLOSED, PageRequest.of(0, 20));
        }

        @Test
        @DisplayName("les recettes sont demandées sur la période")
        void recettesSurLaPeriode() {
            service.findRecettes(DEBUT, FIN);

            verify(paymentRepository).findRecettes(DEBUT, FIN);
        }

        @Test
        @DisplayName("les mouvements de caisse sont demandés sur la période")
        void mouvementsSurLaPeriode() {
            service.findMouvementsCaisse(DEBUT, FIN);

            verify(paymentTransactionRepository).findMouvementsCaisse(DEBUT, FIN);
        }

        @Test
        @DisplayName("les bons de tiers payant sont délégués avec la recherche et la pagination")
        void bonsDelegues() {
            service.fetchAchatTiersPayant(DEBUT, FIN, "CNAM", PageRequest.of(1, 10));

            verify(payantService).fetchAchatTiersPayant(DEBUT, FIN, "CNAM", PageRequest.of(1, 10));
        }
    }

    // ===== règlements tiers payant =====

    @Nested
    @DisplayName("Règlements tiers payant")
    class ReglementsTiersPayant {

        /**
         * Le défaut que ce test fixe : la page était construite puis abandonnée sans être rendue. La
         * section « Règlements tiers payant » du résumé restait donc vide en toutes circonstances, à
         * l'écran comme au PDF, alors que les règlements existaient bien en base.
         */
        @Test
        @DisplayName("les règlements lus sont effectivement rendus")
        void reglementsRendus() {
            donneReglements(
                """
                {"totalElements":2,"content":[
                  {"libelle":"CNAM","type":"ASSURANCE","numFacture":"F-001","montantReglement":300000,"montantFacture":500000},
                  {"libelle":"MUGEF","type":"CARNET","numFacture":"F-002","montantReglement":100000,"montantFacture":100000}
                ]}
                """
            );

            Page<ReglementTiersPayants> page = service.findReglementTierspayant(DEBUT, FIN, null, PageRequest.of(0, 20));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent())
                .extracting(ReglementTiersPayants::libelle)
                .containsExactly("CNAM", "MUGEF");
        }

        @Test
        @DisplayName("chaque règlement porte son payeur, sa facture et ses montants")
        void contenuDuReglement() {
            donneReglements(
                """
                {"totalElements":1,"content":[
                  {"libelle":"CNAM","type":"ASSURANCE","numFacture":"F-001","montantReglement":300000,"montantFacture":500000}
                ]}
                """
            );

            ReglementTiersPayants reglement = service
                .findReglementTierspayant(DEBUT, FIN, null, PageRequest.of(0, 20))
                .getContent()
                .getFirst();

            assertThat(reglement.libelle()).isEqualTo("CNAM");
            assertThat(reglement.type()).isEqualTo(TiersPayantCategorie.ASSURANCE);
            assertThat(reglement.numFacture()).isEqualTo("F-001");
            assertThat(reglement.montantReglement()).isEqualTo(300_000L);
            assertThat(reglement.montantFacture()).isEqualTo(500_000L);
            assertThat(reglement.montantRestant()).isEqualTo(200_000L);
        }

        @Test
        @DisplayName("le nombre total de règlements accompagne la page")
        void totalAccompagneLaPage() {
            donneReglements(
                """
                {"totalElements":37,"content":[
                  {"libelle":"CNAM","type":"ASSURANCE","numFacture":"F-001","montantReglement":1,"montantFacture":2}
                ]}
                """
            );

            Page<ReglementTiersPayants> page = service.findReglementTierspayant(DEBUT, FIN, null, PageRequest.of(0, 20));

            assertThat(page.getTotalElements()).isEqualTo(37L);
        }

        @Test
        @DisplayName("la pagination demandée est transmise en décalage et en taille")
        void paginationTransmise() {
            service.findReglementTierspayant(DEBUT, FIN, null, PageRequest.of(2, 15));

            verify(invoicePaymentRepository).findReglementTierspayant(eq(DEBUT), eq(FIN), isNull(), eq(30), eq(15));
        }

        /** Sans pagination, la requête ne borne rien : le PDF reprend l'intégralité des règlements. */
        @Test
        @DisplayName("sans pagination, la requête n'est pas bornée")
        void sansPagination() {
            service.findReglementTierspayant(DEBUT, FIN, null, Pageable.unpaged());

            verify(invoicePaymentRepository).findReglementTierspayant(eq(DEBUT), eq(FIN), isNull(), eq(0), eq(Integer.MAX_VALUE));
        }

        /** Une recherche vide n'est pas une recherche : elle ne doit pas filtrer. */
        @Test
        @DisplayName("une recherche vide ne filtre pas")
        void rechercheVide() {
            service.findReglementTierspayant(DEBUT, FIN, "", PageRequest.of(0, 20));

            verify(invoicePaymentRepository).findReglementTierspayant(eq(DEBUT), eq(FIN), isNull(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("une recherche renseignée est transmise")
        void rechercheRenseignee() {
            service.findReglementTierspayant(DEBUT, FIN, "CNAM", PageRequest.of(0, 20));

            verify(invoicePaymentRepository).findReglementTierspayant(eq(DEBUT), eq(FIN), eq("CNAM"), anyInt(), anyInt());
        }

        @Test
        @DisplayName("aucun règlement rend une page vide")
        void aucunReglement() {
            donneReglements("""
                {"totalElements":0,"content":[]}
                """);

            assertThat(service.findReglementTierspayant(DEBUT, FIN, null, PageRequest.of(0, 20)).getContent()).isEmpty();
        }

        @Test
        @DisplayName("une fonction stockée illisible rend une page vide, sans erreur")
        void fonctionIllisible() {
            when(invoicePaymentRepository.findReglementTierspayant(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn("ceci n'est pas du JSON");

            assertThat(service.findReglementTierspayant(DEBUT, FIN, null, PageRequest.of(0, 20)).getContent()).isEmpty();
        }
    }

    // ===== impression =====

    @Nested
    @DisplayName("Impression")
    class Impression {

        @Test
        @DisplayName("le PDF rassemble les cinq sections du résumé et sa période")
        void assemblageDuPdf() {
            donneReglements(
                """
                {"totalElements":1,"content":[
                  {"libelle":"CNAM","type":"ASSURANCE","numFacture":"F-001","montantReglement":1,"montantFacture":2}
                ]}
                """
            );
            when(commandeRepository.fetchAchats(any(), any(), any(OrderStatut.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
            when(reportService.printToPdfBytes(any())).thenReturn("%PDF-".getBytes());

            byte[] pdf = service.printToPdf(DEBUT, FIN, null, null);

            assertThat(pdf).isNotEmpty();
            verify(reportService).printToPdfBytes(
                org.mockito.ArgumentMatchers.argThat(record -> {
                    assertThat(record.reglementTiersPayants()).hasSize(1);
                    assertThat(record.periode()).contains("au");
                    return true;
                })
            );
        }
    }

    // ===== utilitaires =====

    private void donneChiffreAffaire(String json) {
        when(salesRepository.getChiffreAffaire(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(json);
    }

    private void donneReglements(String json) {
        when(invoicePaymentRepository.findReglementTierspayant(any(), any(), any(), anyInt(), anyInt())).thenReturn(json);
    }
}
