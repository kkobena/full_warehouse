package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.service.dto.mobile.CashierRecapDTO;
import com.kobe.warehouse.service.dto.mobile.MobileCashSummaryDTO;
import com.kobe.warehouse.service.dto.mobile.SummaryItemDTO;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZData;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;
import com.kobe.warehouse.service.tiketz.dto.TicketZRecap;
import com.kobe.warehouse.service.tiketz.service.TicketZService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Le récapitulatif de caisse mobile ne calcule rien lui-même : il redemande le ticket Z au service
 * qui le produit déjà pour le poste, puis le retaille au format attendu par l'application.
 *
 * <p>Tout l'enjeu tient donc dans la ventilation. Le ticket Z livre deux natures de lignes : celles
 * qui portent un {@link ModePaimentCode}, ventilées par groupe de règlement, et celles qui n'en
 * portent pas — le crédit et le « Total Mobile » — reconnues au seul libellé. Une ligne mal
 * reconnue ne provoque aucune erreur : elle disparaît simplement du total, et le pharmacien lit un
 * chiffre d'affaires amputé sans rien pour l'alerter. Ces tests fixent chaque libellé réellement
 * émis par {@code TicketZServiceImpl} sur la case où il doit atterrir.
 */
@DisplayName("MobileCashSummaryService — récapitulatif de caisse")
class MobileCashSummaryServiceTest {

    /** Libellés tels que {@code TicketZServiceImpl} les émet — ce sont eux qu'il faut reconnaître. */
    private static final String LIBELLE_CREDIT = "Crédit(vno/vo)";

    private static final String LIBELLE_TOTAL_MOBILE = "Total Mobile";

    // Doublure créée à l'initialisation du champ, et non dans un @BeforeEach : le délai de quinze
    // secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final TicketZService ticketZService = mock(TicketZService.class);

    private final MobileCashSummaryService service = new MobileCashSummaryService(ticketZService);

    // ===== interrogation du ticket Z =====

    @Nested
    @DisplayName("Périmètre interrogé")
    class PerimetreInterroge {

        @Test
        @DisplayName("une date de fin absente ramène la période à la seule date de début")
        void dateDeFinAbsente() {
            donneTicketZ(new TicketZ(List.of(), List.of()));

            MobileCashSummaryDTO resultat = service.getCashSummary(
                LocalDate.of(2026, 3, 10),
                null,
                null,
                null,
                null,
                false
            );

            assertThat(parametreTransmis().toDate()).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(resultat.toDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        }

        @Test
        @DisplayName("aucun caissier demandé interroge l'ensemble des caissiers")
        void aucunCaissierDemande() {
            donneTicketZ(new TicketZ(List.of(), List.of()));

            service.getCashSummary(LocalDate.of(2026, 3, 10), null, null, null, null, false);

            assertThat(parametreTransmis().usersId()).isEmpty();
        }

        @Test
        @DisplayName("les caissiers demandés et le filtre « ventes seules » sont transmis tels quels")
        void filtresTransmis() {
            donneTicketZ(new TicketZ(List.of(), List.of()));

            service.getCashSummary(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12), null, null, Set.of(4, 7), true);

            TicketZParam param = parametreTransmis();
            assertThat(param.usersId()).containsExactlyInAnyOrder(4, 7);
            assertThat(param.onlyVente()).isTrue();
            assertThat(param.fromDate()).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(param.toDate()).isEqualTo(LocalDate.of(2026, 3, 12));
        }

        @Test
        @DisplayName("les bornes horaires demandées sont transmises")
        void bornesHorairesTransmises() {
            donneTicketZ(new TicketZ(List.of(), List.of()));

            service.getCashSummary(
                LocalDate.of(2026, 3, 10),
                null,
                LocalTime.of(8, 0),
                LocalTime.of(12, 30),
                null,
                false
            );

            TicketZParam param = parametreTransmis();
            assertThat(param.fromTime()).isEqualTo(LocalTime.of(8, 0));
            assertThat(param.toTime()).isEqualTo(LocalTime.of(12, 30));
        }
    }

    // ===== ventilation =====

    @Nested
    @DisplayName("Ventilation par mode de règlement")
    class VentilationParMode {

        @Test
        @DisplayName("chaque mode alimente le total de son groupe de règlement")
        void modesVersLeurGroupe() {
            donneTicketZ(
                new TicketZ(
                    List.of(
                        ligne("Espèces", 300_000L, ModePaimentCode.CASH),
                        ligne("Carte bancaire", 120_000L, ModePaimentCode.CB),
                        ligne("Chèque", 45_000L, ModePaimentCode.CH),
                        ligne("Virement", 80_000L, ModePaimentCode.VIREMENT)
                    ),
                    List.of()
                )
            );

            MobileCashSummaryDTO resultat = recapitulatif();

            assertThat(resultat.totalEspeces()).isEqualTo(300_000L);
            assertThat(resultat.totalCartes()).isEqualTo(120_000L);
            assertThat(resultat.totalCheques()).isEqualTo(45_000L);
            assertThat(resultat.totalVirements()).isEqualTo(80_000L);
        }

        @Test
        @DisplayName("les quatre opérateurs mobiles se cumulent sur une seule ligne")
        void operateursMobilesCumules() {
            donneTicketZ(
                new TicketZ(
                    List.of(
                        ligne("Orange Money", 10_000L, ModePaimentCode.OM),
                        ligne("MTN Money", 20_000L, ModePaimentCode.MTN),
                        ligne("Moov Money", 30_000L, ModePaimentCode.MOOV),
                        ligne("Wave", 40_000L, ModePaimentCode.WAVE)
                    ),
                    List.of()
                )
            );

            assertThat(recapitulatif().totalMobileMoney()).isEqualTo(100_000L);
        }

        /**
         * Le crédit n'est pas un mode de règlement : la ligne que le ticket Z émet ne porte aucun
         * code, et seul son libellé — accentué — permet de la reconnaître.
         */
        @Test
        @DisplayName("la ligne de crédit est reconnue à son libellé")
        void creditReconnuAuLibelle() {
            donneTicketZ(new TicketZ(List.of(ligneSansCode(LIBELLE_CREDIT, 75_000L)), List.of()));

            assertThat(recapitulatif().totalCredit()).isEqualTo(75_000L);
        }

        /**
         * Le ticket Z ajoute une ligne « Total Mobile » qui récapitule les opérateurs déjà listés
         * un à un. Elle doit être reportée à part : l'additionner au mobile money compterait deux
         * fois les mêmes encaissements.
         */
        @Test
        @DisplayName("la ligne « Total Mobile » est reportée à part, sans doubler le mobile money")
        void totalMobileNeDoublePas() {
            donneTicketZ(
                new TicketZ(
                    List.of(
                        ligne("Orange Money", 10_000L, ModePaimentCode.OM),
                        ligne("Wave", 40_000L, ModePaimentCode.WAVE),
                        ligneSansCode(LIBELLE_TOTAL_MOBILE, 50_000L)
                    ),
                    List.of()
                )
            );

            MobileCashSummaryDTO resultat = recapitulatif();

            assertThat(resultat.totalMobileMoney()).isEqualTo(50_000L);
            assertThat(resultat.totalMobile()).isEqualTo(50_000L);
            assertThat(resultat.totalTtc()).isEqualTo(50_000L);
        }

        @Test
        @DisplayName("le total TTC additionne les six natures d'encaissement")
        void totalTtc() {
            donneTicketZ(
                new TicketZ(
                    List.of(
                        ligne("Espèces", 1L, ModePaimentCode.CASH),
                        ligne("Carte bancaire", 10L, ModePaimentCode.CB),
                        ligne("Chèque", 100L, ModePaimentCode.CH),
                        ligne("Virement", 1_000L, ModePaimentCode.VIREMENT),
                        ligne("Wave", 10_000L, ModePaimentCode.WAVE),
                        ligneSansCode(LIBELLE_CREDIT, 100_000L)
                    ),
                    List.of()
                )
            );

            assertThat(recapitulatif().totalTtc()).isEqualTo(111_111L);
        }

        @Test
        @DisplayName("un montant négatif — remboursement, annulation — se soustrait du total")
        void montantNegatif() {
            donneTicketZ(
                new TicketZ(
                    List.of(ligne("Espèces", 300_000L, ModePaimentCode.CASH), ligne("Espèces", -50_000L, ModePaimentCode.CASH)),
                    List.of()
                )
            );

            assertThat(recapitulatif().totalEspeces()).isEqualTo(250_000L);
        }

        @Test
        @DisplayName("une journée sans mouvement rend des totaux nuls et aucun caissier")
        void journeeSansMouvement() {
            donneTicketZ(new TicketZ(List.of(), List.of()));

            MobileCashSummaryDTO resultat = recapitulatif();

            assertThat(resultat.totalTtc()).isZero();
            assertThat(resultat.totalEspeces()).isZero();
            assertThat(resultat.totalCredit()).isZero();
            assertThat(resultat.globalSummary()).isEmpty();
            assertThat(resultat.cashierRecaps()).isEmpty();
            assertThat(resultat.cashierCount()).isZero();
        }
    }

    // ===== repli par caissier =====

    @Nested
    @DisplayName("Repli sur le détail par caissier")
    class RepliParCaissier {

        /**
         * Le ticket Z ne construit son récapitulatif global que sous condition. Quand il ne le
         * fournit pas, les totaux se reconstituent en parcourant le détail de chaque caissier.
         */
        @Test
        @DisplayName("sans récapitulatif global, les totaux se reconstituent caissier par caissier")
        void repliSurLeDetail() {
            donneTicketZ(
                new TicketZ(
                    List.of(),
                    List.of(
                        caissier(1L, "Jean Dupont", List.of(ligne("Espèces", 200_000L, ModePaimentCode.CASH))),
                        caissier(
                            2L,
                            "Awa Koné",
                            List.of(ligne("Carte bancaire", 60_000L, ModePaimentCode.CB), ligneSansCode(LIBELLE_CREDIT, 15_000L))
                        )
                    )
                )
            );

            MobileCashSummaryDTO resultat = recapitulatif();

            assertThat(resultat.totalEspeces()).isEqualTo(200_000L);
            assertThat(resultat.totalCartes()).isEqualTo(60_000L);
            assertThat(resultat.totalCredit()).isEqualTo(15_000L);
            assertThat(resultat.totalTtc()).isEqualTo(275_000L);
        }

        /**
         * Garde-fou : le récapitulatif global et le détail par caissier portent les mêmes
         * encaissements. Dès que le premier est présent, le second ne doit plus être additionné,
         * sous peine de doubler la recette de la journée.
         */
        @Test
        @DisplayName("le détail par caissier ne s'ajoute pas au récapitulatif global")
        void pasDeDoubleComptage() {
            donneTicketZ(
                new TicketZ(
                    List.of(ligne("Espèces", 200_000L, ModePaimentCode.CASH)),
                    List.of(caissier(1L, "Jean Dupont", List.of(ligne("Espèces", 200_000L, ModePaimentCode.CASH))))
                )
            );

            assertThat(recapitulatif().totalEspeces()).isEqualTo(200_000L);
        }
    }

    // ===== restitution =====

    @Nested
    @DisplayName("Restitution des lignes")
    class RestitutionDesLignes {

        @Test
        @DisplayName("une ligne portant un mode est indexée sur le code de ce mode")
        void cleSurLeCode() {
            donneTicketZ(new TicketZ(List.of(ligne("Carte bancaire", 60_000L, ModePaimentCode.CB)), List.of()));

            SummaryItemDTO item = recapitulatif().globalSummary().getFirst();

            assertThat(item.key()).isEqualTo("CB");
            assertThat(item.libelle()).isEqualTo("Carte bancaire");
            assertThat(item.value()).isEqualTo(60_000L);
            assertThat(item.type()).isEqualTo(SummaryItemDTO.TYPE_AMOUNT);
        }

        @Test
        @DisplayName("à défaut de mode, la clé se déduit du libellé")
        void cleDeduiteDuLibelle() {
            donneTicketZ(new TicketZ(List.of(ligneSansCode(LIBELLE_TOTAL_MOBILE, 50_000L)), List.of()));

            assertThat(recapitulatif().globalSummary().getFirst().key()).isEqualTo("TOTAL_MOBILE");
        }

        @Test
        @DisplayName("le montant réel est conservé à côté du montant arrondi")
        void montantReelConserve() {
            donneTicketZ(new TicketZ(List.of(new TicketZData("Espèces", 300_000L, 299_995L, 0, ModePaimentCode.CASH)), List.of()));

            SummaryItemDTO item = recapitulatif().globalSummary().getFirst();

            assertThat(item.value()).isEqualTo(300_000L);
            assertThat(item.secondValue()).isEqualTo(299_995L);
        }

        @Test
        @DisplayName("chaque caissier est restitué avec son détail et son sous-total")
        void detailEtSousTotalParCaissier() {
            donneTicketZ(
                new TicketZ(
                    List.of(),
                    List.of(
                        new TicketZRecap(
                            1L,
                            "Jean Dupont",
                            List.of(ligne("Orange Money", 10_000L, ModePaimentCode.OM), ligne("Wave", 40_000L, ModePaimentCode.WAVE)),
                            List.of(ligneSansCode(LIBELLE_TOTAL_MOBILE, 50_000L))
                        )
                    )
                )
            );

            CashierRecapDTO recap = recapitulatif().cashierRecaps().getFirst();

            assertThat(recap.details()).extracting(SummaryItemDTO::key).containsExactly("OM", "WAVE");
            assertThat(recap.summary()).extracting(SummaryItemDTO::key).containsExactly("TOTAL_MOBILE");
        }

        @Test
        @DisplayName("les initiales du caissier sont dérivées de son nom")
        void initialesDuCaissier() {
            donneTicketZ(
                new TicketZ(
                    List.of(),
                    List.of(caissier(1L, "Jean Dupont", List.of()), caissier(2L, "Awa", List.of()))
                )
            );

            assertThat(recapitulatif().cashierRecaps())
                .extracting(CashierRecapDTO::userInitials)
                .containsExactly("J.D", "AW");
        }

        @Test
        @DisplayName("le nombre de caissiers suit le nombre de récapitulatifs")
        void nombreDeCaissiers() {
            donneTicketZ(
                new TicketZ(
                    List.of(),
                    List.of(caissier(1L, "Jean Dupont", List.of()), caissier(2L, "Awa Koné", List.of()))
                )
            );

            assertThat(recapitulatif().cashierCount()).isEqualTo(2);
        }
    }

    // ===== libellé de période =====

    @Nested
    @DisplayName("Libellé de période")
    class LibelleDePeriode {

        @Test
        @DisplayName("la journée en cours se nomme « Aujourd'hui »")
        void aujourdHui() {
            assertThat(libellePour(LocalDate.now(), null, null, null)).isEqualTo("Aujourd'hui");
        }

        @Test
        @DisplayName("la veille se nomme « Hier »")
        void hier() {
            assertThat(libellePour(LocalDate.now().minusDays(1), null, null, null)).isEqualTo("Hier");
        }

        @Test
        @DisplayName("toute autre journée s'affiche en clair")
        void autreJournee() {
            assertThat(libellePour(LocalDate.of(2026, 3, 10), null, null, null)).isEqualTo("10/03/2026");
        }

        @Test
        @DisplayName("une période encadrée affiche ses deux bornes")
        void periodeEncadree() {
            assertThat(libellePour(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12), null, null))
                .isEqualTo("10/03/2026 - 12/03/2026");
        }

        @Test
        @DisplayName("des bornes horaires partielles s'ajoutent entre parenthèses")
        void bornesPartielles() {
            assertThat(libellePour(LocalDate.of(2026, 3, 10), null, LocalTime.of(8, 0), LocalTime.of(12, 30)))
                .isEqualTo("10/03/2026 (08:00 - 12:30)");
        }

        /** Une journée entière n'a pas à porter d'horaire : la mention n'apprendrait rien. */
        @Test
        @DisplayName("une journée entière n'affiche pas d'horaire")
        void journeeEntiere() {
            assertThat(libellePour(LocalDate.of(2026, 3, 10), null, LocalTime.MIN, LocalTime.MAX)).isEqualTo("10/03/2026");
        }

        @Test
        @DisplayName("la borne de fin à 23:59:59 vaut aussi journée entière")
        void finDeJournee() {
            assertThat(libellePour(LocalDate.of(2026, 3, 10), null, LocalTime.MIN, LocalTime.of(23, 59, 59)))
                .isEqualTo("10/03/2026");
        }

        @Test
        @DisplayName("une seule borne horaire ne suffit pas à afficher une plage")
        void borneIsolee() {
            assertThat(libellePour(LocalDate.of(2026, 3, 10), null, LocalTime.of(8, 0), null)).isEqualTo("10/03/2026");
        }

        private String libellePour(LocalDate debut, LocalDate fin, LocalTime heureDebut, LocalTime heureFin) {
            donneTicketZ(new TicketZ(List.of(), List.of()));
            return service.getCashSummary(debut, fin, heureDebut, heureFin, null, false).periodLabel();
        }
    }

    // ===== utilitaires =====

    private void donneTicketZ(TicketZ ticketZ) {
        when(ticketZService.getTicketZ(any())).thenReturn(ticketZ);
    }

    private MobileCashSummaryDTO recapitulatif() {
        return service.getCashSummary(LocalDate.of(2026, 3, 10), null, null, null, null, false);
    }

    private TicketZParam parametreTransmis() {
        ArgumentCaptor<TicketZParam> captor = ArgumentCaptor.forClass(TicketZParam.class);
        verify(ticketZService).getTicketZ(captor.capture());
        return captor.getValue();
    }

    private static TicketZData ligne(String libelle, long montant, ModePaimentCode code) {
        return new TicketZData(libelle, montant, montant, code.getSortOrder(), code);
    }

    private static TicketZData ligneSansCode(String libelle, long montant) {
        return new TicketZData(libelle, montant, montant, 100, null);
    }

    private static TicketZRecap caissier(long id, String nom, List<TicketZData> lignes) {
        return new TicketZRecap(id, nom, lignes, List.of());
    }
}
