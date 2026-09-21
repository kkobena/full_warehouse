package com.kobe.warehouse.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.activity_summary.ActivitySummaryService;
import com.kobe.warehouse.service.dto.ChiffreAffaireDTO;
import com.kobe.warehouse.service.dto.mobile.AchatTiersPayantMobileDTO;
import com.kobe.warehouse.service.dto.mobile.ChiffreAffaireMobileDTO;
import com.kobe.warehouse.service.dto.mobile.GroupeFournisseurAchatMobileDTO;
import com.kobe.warehouse.service.dto.mobile.MobileActivityReportDTO;
import com.kobe.warehouse.service.dto.mobile.MouvementCaisseMobileDTO;
import com.kobe.warehouse.service.dto.mobile.RecetteMobileDTO;
import com.kobe.warehouse.service.dto.mobile.ReglementTiersPayantMobileDTO;
import com.kobe.warehouse.service.dto.mobile.TiersPayantSummaryMobileDTO;
import com.kobe.warehouse.service.dto.projection.AchatTiersPayant;
import com.kobe.warehouse.service.dto.projection.GroupeFournisseurAchat;
import com.kobe.warehouse.service.dto.projection.MouvementCaisse;
import com.kobe.warehouse.service.dto.projection.Recette;
import com.kobe.warehouse.service.dto.projection.ReglementTiersPayants;
import com.kobe.warehouse.service.dto.records.ChiffreAffaireRecord;
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
 * Le rapport d'activité mobile rassemble en un seul appel six interrogations distinctes — chiffre
 * d'affaires, recettes par mode, mouvements de caisse, achats fournisseurs, règlements et bons de
 * tiers payant — et les remet dans un format que l'application affiche sans retraitement.
 *
 * <p>La difficulté est là : le service ne calcule presque rien, il recopie. Une valeur recopiée dans
 * le mauvais champ ne provoque aucune erreur et ne se voit qu'à l'écran — c'est exactement ce qui
 * arrivait aux recettes, dont le code du mode de règlement et son libellé étaient permutés.
 */
@DisplayName("MobileActivityReportService — rapport d'activité")
class MobileActivityReportServiceTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 3, 10);

    // Doublure créée à l'initialisation du champ, et non dans un @BeforeEach : le délai de quinze
    // secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final ActivitySummaryService activitySummaryService = mock(ActivitySummaryService.class);

    private final MobileActivityReportService service = new MobileActivityReportService(activitySummaryService);

    @BeforeEach
    void perimetreVide() {
        when(activitySummaryService.getChiffreAffaire(any(), any())).thenReturn(null);
        when(activitySummaryService.findRecettes(any(), any())).thenReturn(List.of());
        when(activitySummaryService.findMouvementsCaisse(any(), any())).thenReturn(List.of());
        when(activitySummaryService.fetchAchats(any(), any(), any())).thenReturn(page(List.of()));
        when(activitySummaryService.findReglementTierspayant(any(), any(), any(), any())).thenReturn(page(List.of()));
        when(activitySummaryService.fetchAchatTiersPayant(any(), any(), any(), any())).thenReturn(page(List.of()));
    }

    // ===== périmètre =====

    @Nested
    @DisplayName("Périmètre interrogé")
    class PerimetreInterroge {

        @Test
        @DisplayName("une date de fin absente ramène la période à la seule date de début")
        void dateDeFinAbsente() {
            MobileActivityReportDTO resultat = service.getActivityReport(JOUR, null);

            assertThat(resultat.toDate()).isEqualTo(JOUR);
            verify(activitySummaryService).findRecettes(JOUR, JOUR);
        }

        /**
         * Le rapport tient sur un écran de téléphone : les listes longues sont tronquées à la
         * source, sans quoi l'appel ramènerait des milliers de lignes que personne ne fait défiler.
         */
        @Test
        @DisplayName("les listes détaillées sont bornées aux vingt premières lignes")
        void listesBornees() {
            service.getActivityReport(JOUR, JOUR);

            verify(activitySummaryService).fetchAchats(JOUR, JOUR, PageRequest.of(0, 20));
            verify(activitySummaryService).findReglementTierspayant(eq(JOUR), eq(JOUR), isNull(), eq(PageRequest.of(0, 20)));
            verify(activitySummaryService).fetchAchatTiersPayant(eq(JOUR), eq(JOUR), isNull(), eq(PageRequest.of(0, 20)));
        }

        @Test
        @DisplayName("une période sans activité rend un rapport vide plutôt qu'aucun rapport")
        void periodeSansActivite() {
            MobileActivityReportDTO resultat = service.getActivityReport(JOUR, JOUR);

            assertThat(resultat.chiffreAffaire()).isEqualTo(ChiffreAffaireMobileDTO.empty());
            assertThat(resultat.recettes()).isEmpty();
            assertThat(resultat.totalRecettes()).isZero();
            assertThat(resultat.mouvementsCaisse()).isEmpty();
            assertThat(resultat.achatsFournisseurs()).isEmpty();
            assertThat(resultat.tiersPayants()).isEqualTo(TiersPayantSummaryMobileDTO.empty());
        }
    }

    // ===== chiffre d'affaires =====

    @Nested
    @DisplayName("Chiffre d'affaires")
    class ChiffreDAffaires {

        @Test
        @DisplayName("chaque montant est repris dans le champ qui le désigne")
        void montantsRepris() {
            donneChiffreAffaire(
                new ChiffreAffaireRecord(
                    BigDecimal.valueOf(1_000_000),
                    BigDecimal.valueOf(100_000),
                    BigDecimal.valueOf(900_000),
                    BigDecimal.valueOf(50_000),
                    BigDecimal.valueOf(950_000),
                    BigDecimal.valueOf(600_000),
                    BigDecimal.valueOf(120_000),
                    BigDecimal.valueOf(880_000),
                    BigDecimal.valueOf(280_000),
                    BigDecimal.valueOf(400_000)
                )
            );

            ChiffreAffaireMobileDTO ca = service.getActivityReport(JOUR, JOUR).chiffreAffaire();

            assertThat(ca.montantTtc()).isEqualTo(1_000_000L);
            assertThat(ca.montantTva()).isEqualTo(100_000L);
            assertThat(ca.montantHt()).isEqualTo(900_000L);
            assertThat(ca.montantRemise()).isEqualTo(50_000L);
            assertThat(ca.montantNet()).isEqualTo(950_000L);
            assertThat(ca.montantEspece()).isEqualTo(600_000L);
            assertThat(ca.montantCredit()).isEqualTo(120_000L);
            assertThat(ca.montantRegle()).isEqualTo(880_000L);
            assertThat(ca.montantAutreMode()).isEqualTo(280_000L);
            assertThat(ca.marge()).isEqualTo(400_000L);
        }

        @Test
        @DisplayName("le taux de marge se calcule sur le TTC, arrondi au dixième")
        void tauxDeMarge() {
            donneChiffreAffaire(chiffreAffaire(1_000_000L, 333_333L));

            assertThat(service.getActivityReport(JOUR, JOUR).chiffreAffaire().margePercent()).isEqualTo(33.3);
        }

        /** Sans chiffre d'affaires, pas de division : le taux vaut zéro. */
        @Test
        @DisplayName("un chiffre d'affaires nul rend un taux de marge nul")
        void chiffreDAffairesNul() {
            donneChiffreAffaire(chiffreAffaire(0L, 0L));

            assertThat(service.getActivityReport(JOUR, JOUR).chiffreAffaire().margePercent()).isZero();
        }

        @Test
        @DisplayName("un chiffre d'affaires absent du calcul rend un bloc vide")
        void chiffreAffaireAbsent() {
            when(activitySummaryService.getChiffreAffaire(any(), any()))
                .thenReturn(new ChiffreAffaireDTO(List.of(), null, null, List.of()));

            assertThat(service.getActivityReport(JOUR, JOUR).chiffreAffaire()).isEqualTo(ChiffreAffaireMobileDTO.empty());
        }

        @Test
        @DisplayName("un montant non renseigné vaut zéro")
        void montantAbsent() {
            donneChiffreAffaire(new ChiffreAffaireRecord(null, null, null, null, null, null, null, null, null, null));

            assertThat(service.getActivityReport(JOUR, JOUR).chiffreAffaire()).isEqualTo(ChiffreAffaireMobileDTO.empty());
        }
    }

    // ===== recettes =====

    @Nested
    @DisplayName("Recettes par mode de règlement")
    class Recettes {

        /**
         * Le défaut que ce test fixe : le code du mode et son libellé étaient permutés à la
         * construction. L'application affichait « CASH » là où elle devait lire « Espèces », et tout
         * aiguillage sur le code tombait sur un libellé.
         */
        @Test
        @DisplayName("le code et le libellé du mode ne sont pas permutés")
        void codeEtLibelle() {
            donneRecettes(new Recette(300_000L, 300_000L, "CASH", "Espèces"));

            RecetteMobileDTO recette = service.getActivityReport(JOUR, JOUR).recettes().getFirst();

            assertThat(recette.code()).isEqualTo("CASH");
            assertThat(recette.libelle()).isEqualTo("Espèces");
        }

        @Test
        @DisplayName("la couleur du graphique suit le mode de règlement")
        void couleurParMode() {
            donneRecettes(
                new Recette(1L, 1L, "CASH", "Espèces"),
                new Recette(1L, 1L, "CB", "Carte bancaire"),
                new Recette(1L, 1L, "WAVE", "Wave"),
                new Recette(1L, 1L, "INCONNU", "Autre")
            );

            assertThat(service.getActivityReport(JOUR, JOUR).recettes())
                .extracting(RecetteMobileDTO::color)
                .containsExactly("#28A745", "#007BFF", "#FFC107", "#6C757D");
        }

        /** C'est le montant réellement encaissé qui compte, non le montant facturé. */
        @Test
        @DisplayName("le montant retenu est celui réellement encaissé")
        void montantReellementEncaisse() {
            donneRecettes(new Recette(295_000L, 300_000L, "CASH", "Espèces"));

            assertThat(service.getActivityReport(JOUR, JOUR).recettes().getFirst().montant()).isEqualTo(295_000L);
        }

        @Test
        @DisplayName("chaque mode porte sa part du total encaissé")
        void partDuTotal() {
            donneRecettes(new Recette(750_000L, 750_000L, "CASH", "Espèces"), new Recette(250_000L, 250_000L, "CB", "Carte"));

            MobileActivityReportDTO resultat = service.getActivityReport(JOUR, JOUR);

            assertThat(resultat.recettes()).extracting(RecetteMobileDTO::percent).containsExactly(75.0, 25.0);
            assertThat(resultat.totalRecettes()).isEqualTo(1_000_000L);
        }

        @Test
        @DisplayName("un total nul rend des parts nulles")
        void totalNul() {
            donneRecettes(new Recette(0L, 0L, "CASH", "Espèces"));

            assertThat(service.getActivityReport(JOUR, JOUR).recettes().getFirst().percent()).isZero();
        }
    }

    // ===== mouvements de caisse =====

    @Nested
    @DisplayName("Mouvements de caisse")
    class MouvementsDeCaisse {

        @Test
        @DisplayName("le sens du mouvement suit la catégorie que le domaine lui donne")
        void sensDuMouvement() {
            donneMouvements(
                mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000L),
                mouvement(TypeFinancialTransaction.SORTIE_CAISSE, 20_000L),
                mouvement(TypeFinancialTransaction.REGLMENT_FOURNISSEUR, 400_000L),
                mouvement(TypeFinancialTransaction.FONDS_CAISSE, 30_000L),
                mouvement(TypeFinancialTransaction.CASH_SALE, 900_000L),
                mouvement(TypeFinancialTransaction.CAUTION, 10_000L)
            );

            assertThat(service.getActivityReport(JOUR, JOUR).mouvementsCaisse())
                .extracting(MouvementCaisseMobileDTO::type)
                .containsExactly("ENTREE", "SORTIE", "SORTIE", "SORTIE", "ENTREE", "ENTREE");
        }

        @Test
        @DisplayName("entrées et sorties sont totalisées séparément")
        void totauxSepares() {
            donneMouvements(
                mouvement(TypeFinancialTransaction.ENTREE_CAISSE, 50_000L),
                mouvement(TypeFinancialTransaction.CAUTION, 10_000L),
                mouvement(TypeFinancialTransaction.SORTIE_CAISSE, 20_000L),
                mouvement(TypeFinancialTransaction.REGLMENT_FOURNISSEUR, 400_000L)
            );

            MobileActivityReportDTO resultat = service.getActivityReport(JOUR, JOUR);

            assertThat(resultat.totalEntrees()).isEqualTo(60_000L);
            assertThat(resultat.totalSorties()).isEqualTo(420_000L);
        }

        @Test
        @DisplayName("le mouvement porte le libellé court, fait pour les colonnes étroites")
        void libelleCourt() {
            donneMouvements(mouvement(TypeFinancialTransaction.REGLMENT_FOURNISSEUR, 400_000L));

            assertThat(service.getActivityReport(JOUR, JOUR).mouvementsCaisse().getFirst().libelle())
                .isEqualTo("Rgt fournisseur");
        }

        @Test
        @DisplayName("un montant non renseigné vaut zéro")
        void montantAbsent() {
            MouvementCaisse mvt = mock(MouvementCaisse.class);
            when(mvt.getType()).thenReturn(TypeFinancialTransaction.ENTREE_CAISSE);
            when(mvt.getMontant()).thenReturn(null);
            when(mvt.getLibelle()).thenReturn("Entrée");
            donneMouvements(mvt);

            assertThat(service.getActivityReport(JOUR, JOUR).mouvementsCaisse().getFirst().montant()).isZero();
        }
    }

    // ===== achats fournisseurs =====

    @Nested
    @DisplayName("Achats fournisseurs")
    class AchatsFournisseurs {

        @Test
        @DisplayName("chaque groupe porte ses montants et sa part des achats")
        void montantsEtPart() {
            donneAchats(achat("LABOREX", 800_000L, 80_000L, 720_000L), achat("COPHARMED", 200_000L, 20_000L, 180_000L));

            MobileActivityReportDTO resultat = service.getActivityReport(JOUR, JOUR);

            assertThat(resultat.achatsFournisseurs())
                .extracting(GroupeFournisseurAchatMobileDTO::libelle, GroupeFournisseurAchatMobileDTO::montantTtc)
                .containsExactly(org.assertj.core.api.Assertions.tuple("LABOREX", 800_000L), org.assertj.core.api.Assertions.tuple("COPHARMED", 200_000L));
            assertThat(resultat.achatsFournisseurs())
                .extracting(GroupeFournisseurAchatMobileDTO::percentTotal)
                .containsExactly(80.0, 20.0);
            assertThat(resultat.totalAchats()).isEqualTo(1_000_000L);
        }

        @Test
        @DisplayName("la TVA et le HT du groupe sont repris distinctement")
        void tvaEtHt() {
            donneAchats(achat("LABOREX", 800_000L, 80_000L, 720_000L));

            GroupeFournisseurAchatMobileDTO groupe = service.getActivityReport(JOUR, JOUR).achatsFournisseurs().getFirst();

            assertThat(groupe.montantTva()).isEqualTo(80_000L);
            assertThat(groupe.montantHt()).isEqualTo(720_000L);
        }

        @Test
        @DisplayName("un total nul rend des parts nulles")
        void totalNul() {
            donneAchats(achat("LABOREX", 0L, 0L, 0L));

            assertThat(service.getActivityReport(JOUR, JOUR).achatsFournisseurs().getFirst().percentTotal()).isZero();
        }
    }

    // ===== tiers payants =====

    @Nested
    @DisplayName("Tiers payants")
    class TiersPayants {

        @Test
        @DisplayName("un règlement affiche son reste à recouvrer")
        void resteARecouvrer() {
            donneReglements(new ReglementTiersPayants("CNAM", TiersPayantCategorie.ASSURANCE, "F-2026-001", 300_000L, 500_000L));

            ReglementTiersPayantMobileDTO reglement = service.getActivityReport(JOUR, JOUR).tiersPayants().reglements().getFirst();

            assertThat(reglement.libelle()).isEqualTo("CNAM");
            assertThat(reglement.categorie()).isEqualTo("ASSURANCE");
            assertThat(reglement.numFacture()).isEqualTo("F-2026-001");
            assertThat(reglement.montantFacture()).isEqualTo(500_000L);
            assertThat(reglement.montantReglement()).isEqualTo(300_000L);
            assertThat(reglement.montantRestant()).isEqualTo(200_000L);
        }

        @Test
        @DisplayName("les règlements sont totalisés, reste à recouvrer compris")
        void totauxDesReglements() {
            donneReglements(
                new ReglementTiersPayants("CNAM", TiersPayantCategorie.ASSURANCE, "F1", 300_000L, 500_000L),
                new ReglementTiersPayants("MUGEF", TiersPayantCategorie.ASSURANCE, "F2", 100_000L, 250_000L)
            );

            TiersPayantSummaryMobileDTO tiersPayants = service.getActivityReport(JOUR, JOUR).tiersPayants();

            assertThat(tiersPayants.totalFacture()).isEqualTo(750_000L);
            assertThat(tiersPayants.totalRegle()).isEqualTo(400_000L);
            assertThat(tiersPayants.totalRestant()).isEqualTo(350_000L);
        }

        /** Une facture non encore réglée arrive sans montant de règlement. */
        @Test
        @DisplayName("un montant de règlement absent vaut zéro")
        void reglementAbsent() {
            donneReglements(new ReglementTiersPayants("CNAM", TiersPayantCategorie.ASSURANCE, "F1", null, 500_000L));

            ReglementTiersPayantMobileDTO reglement = service.getActivityReport(JOUR, JOUR).tiersPayants().reglements().getFirst();

            assertThat(reglement.montantReglement()).isZero();
            assertThat(reglement.montantRestant()).isEqualTo(500_000L);
        }

        @Test
        @DisplayName("une catégorie absente laisse le champ vide sans interrompre le rapport")
        void categorieAbsente() {
            donneReglements(new ReglementTiersPayants("CNAM", null, "F1", 0L, 0L));

            assertThat(service.getActivityReport(JOUR, JOUR).tiersPayants().reglements().getFirst().categorie()).isEmpty();
        }

        @Test
        @DisplayName("les bons de tiers payant sont repris avec leurs compteurs")
        void bonsEtCompteurs() {
            donneAchatsTiersPayant(new AchatTiersPayant("CNAM", TiersPayantCategorie.ASSURANCE, 12L, 8L, 450_000));

            AchatTiersPayantMobileDTO achat = service.getActivityReport(JOUR, JOUR).tiersPayants().achats().getFirst();

            assertThat(achat.libelle()).isEqualTo("CNAM");
            assertThat(achat.categorie()).isEqualTo("ASSURANCE");
            assertThat(achat.bonsCount()).isEqualTo(12);
            assertThat(achat.montant()).isEqualTo(450_000L);
            assertThat(achat.clientCount()).isEqualTo(8);
        }

        @Test
        @DisplayName("les bons sont totalisés, en nombre comme en montant")
        void totauxDesBons() {
            donneAchatsTiersPayant(
                new AchatTiersPayant("CNAM", TiersPayantCategorie.ASSURANCE, 12L, 8L, 450_000),
                new AchatTiersPayant("MUGEF", TiersPayantCategorie.CARNET, 3L, 2L, 50_000)
            );

            TiersPayantSummaryMobileDTO tiersPayants = service.getActivityReport(JOUR, JOUR).tiersPayants();

            assertThat(tiersPayants.totalBons()).isEqualTo(15);
            assertThat(tiersPayants.totalMontantAchats()).isEqualTo(500_000L);
            assertThat(tiersPayants.totalClients()).isEqualTo(10);
        }

        @Test
        @DisplayName("des compteurs absents valent zéro")
        void compteursAbsents() {
            donneAchatsTiersPayant(new AchatTiersPayant("CNAM", TiersPayantCategorie.ASSURANCE, null, null, null));

            AchatTiersPayantMobileDTO achat = service.getActivityReport(JOUR, JOUR).tiersPayants().achats().getFirst();

            assertThat(achat.bonsCount()).isZero();
            assertThat(achat.montant()).isZero();
            assertThat(achat.clientCount()).isZero();
        }
    }

    // ===== libellé de période =====

    @Nested
    @DisplayName("Libellé de période")
    class LibelleDePeriode {

        @Test
        @DisplayName("la journée en cours se nomme « Aujourd'hui »")
        void aujourdHui() {
            assertThat(service.getActivityReport(LocalDate.now(), null).periodLabel()).isEqualTo("Aujourd'hui");
        }

        @Test
        @DisplayName("la veille se nomme « Hier »")
        void hier() {
            assertThat(service.getActivityReport(LocalDate.now().minusDays(1), null).periodLabel()).isEqualTo("Hier");
        }

        @Test
        @DisplayName("toute autre journée s'affiche en clair")
        void autreJournee() {
            assertThat(service.getActivityReport(JOUR, null).periodLabel()).isEqualTo("10/03/2026");
        }

        @Test
        @DisplayName("une période encadrée affiche ses deux bornes")
        void periodeEncadree() {
            assertThat(service.getActivityReport(JOUR, LocalDate.of(2026, 3, 12)).periodLabel())
                .isEqualTo("10/03/2026 - 12/03/2026");
        }
    }

    // ===== utilitaires =====

    private void donneChiffreAffaire(ChiffreAffaireRecord record) {
        when(activitySummaryService.getChiffreAffaire(any(), any()))
            .thenReturn(new ChiffreAffaireDTO(List.of(), record, null, List.of()));
    }

    private void donneRecettes(Recette... recettes) {
        when(activitySummaryService.findRecettes(any(), any())).thenReturn(List.of(recettes));
    }

    private void donneAchats(GroupeFournisseurAchat... achats) {
        when(activitySummaryService.fetchAchats(any(), any(), any())).thenReturn(page(List.of(achats)));
    }

    private void donneMouvements(MouvementCaisse... mouvements) {
        when(activitySummaryService.findMouvementsCaisse(any(), any())).thenReturn(List.of(mouvements));
    }

    private void donneReglements(ReglementTiersPayants... reglements) {
        when(activitySummaryService.findReglementTierspayant(any(), any(), any(), any()))
            .thenReturn(page(List.of(reglements)));
    }

    private void donneAchatsTiersPayant(AchatTiersPayant... achats) {
        when(activitySummaryService.fetchAchatTiersPayant(any(), any(), any(), any())).thenReturn(page(List.of(achats)));
    }

    private static ChiffreAffaireRecord chiffreAffaire(long ttc, long marge) {
        return new ChiffreAffaireRecord(
            BigDecimal.valueOf(ttc),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            BigDecimal.valueOf(marge)
        );
    }

    private static MouvementCaisse mouvement(TypeFinancialTransaction type, long montant) {
        MouvementCaisse mvt = mock(MouvementCaisse.class);
        when(mvt.getType()).thenReturn(type);
        when(mvt.getMontant()).thenReturn(BigDecimal.valueOf(montant));
        when(mvt.getLibelle()).thenReturn(type.getTransactionTypeAffichage().getValueCourt());
        return mvt;
    }

    private static GroupeFournisseurAchat achat(String libelle, long ttc, long tva, long ht) {
        GroupeFournisseurAchat groupe = mock(GroupeFournisseurAchat.class);
        when(groupe.getLibelle()).thenReturn(libelle);
        when(groupe.getMontantTtc()).thenReturn(BigDecimal.valueOf(ttc));
        when(groupe.getMontantTva()).thenReturn(BigDecimal.valueOf(tva));
        when(groupe.getMontantHt()).thenReturn(BigDecimal.valueOf(ht));
        return groupe;
    }

    private static <T> Page<T> page(List<T> contenu) {
        Pageable pageable = PageRequest.of(0, 20);
        return new PageImpl<>(contenu, pageable, contenu.size());
    }
}
