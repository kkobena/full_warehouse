package com.kobe.warehouse.service.remise.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.SourceOptionsExport;
import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.repository.PromotionRepository;
import com.kobe.warehouse.repository.exports.ExportDonneesRepository;
import com.kobe.warehouse.repository.exports.OptionsFiltreExportRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.exports.OptionFiltreDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.financiel_transaction.integration.AbstractFinancialTransactionIntegrationTest;
import com.kobe.warehouse.service.portee.PorteeMagasinService;
import com.kobe.warehouse.service.remise.dto.JournalRemisesFiltre;
import com.kobe.warehouse.service.remise.dto.LigneJournalDTO;
import com.kobe.warehouse.service.remise.dto.NatureJournal;
import com.kobe.warehouse.service.remise.dto.OrigineRemise;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Le journal des remises lu sur PostgreSQL. Chaque essai date ses ventes d'un jour à lui, de l'an
 * dernier : la base d'essai est partagée, et le journal ne lit que sa période.
 */
@DisplayName("Journal des remises, promotions et UG — lu sur PostgreSQL")
class JournalRemisesIntegrationTest extends AbstractFinancialTransactionIntegrationTest {

    private static final AtomicInteger JOURS = new AtomicInteger();

    private JournalRemisesServiceImpl service;
    private LocalDate jour;

    @BeforeEach
    void cablerLeService() {
        // L'an dernier : la base d'essai n'a de partitions que de l'an dernier à l'an prochain.
        jour = LocalDate.of(LocalDate.now().getYear() - 1, 2, 1).plusDays(JOURS.incrementAndGet());
        PorteeMagasinService portee = mock(PorteeMagasinService.class);
        when(portee.resoudreLeMagasinDExploitation()).thenReturn(magasin);
        StorageService storage = mock(StorageService.class);
        when(storage.getUser()).thenReturn(utilisateur);
        JournalRemisesRequetes requetes = IntegrationPostgresDatabase.context()
                .getAutowireCapableBeanFactory()
                .createBean(JournalRemisesRequetes.class);
        service = new JournalRemisesServiceImpl(
                requetes,
                portee,
                IntegrationPostgresDatabase.bean(PromotionRepository.class),
                new JournalRemisesExports(
                        templateEngine(), storage, new ReportExcelExportService(), new CsvExportService()));
    }

    // ===== jeu d'essai =====

    private JournalRemisesFiltre duJour() {
        return JournalRemisesFiltre.periode(jour, jour);
    }

    private Promotion promotion(TypePromotion type, int valeur, int quantiteMin) {
        Promotion p = new Promotion()
                .setCode(unique("JR"))
                .setLibelle(unique("Promo journal"))
                .setType(type)
                .setValeur(valeur)
                .setQuantiteMin(quantiteMin)
                .setStatut(StatutPromotion.VALIDEE)
                .setDateDebut(jour.minusDays(1))
                .setDateFin(jour.plusDays(1));
        em.persist(p);
        em.flush();
        return p;
    }

    /** Une vente d'une ligne : la remise est d'abord celle de la grille, une promotion s'y ajoute. */
    private SalesLine vendre(Produit produit, int quantite, int remiseProduit, Promotion promotion, int partPromotion) {
        int brut = quantite * produit.getRegularUnitPrice();
        int remise = remiseProduit + partPromotion;
        CashSale vente = venteFermee(jour, brut, remise);
        SalesLine ligne = ligneDeVente(vente, produit, quantite, remise);
        if (promotion != null) {
            ligne.setPromotionId(promotion.getId());
            ligne.setPromotionLibelle(promotion.getLibelle());
            ligne.setQuantitePromo(quantite);
            ligne.setRemisePromotion(partPromotion);
        }
        em.flush();
        return ligne;
    }

    private SalesLine vendreAvecUg(Produit produit, int quantite, int ug, int remise) {
        CashSale vente = venteFermee(jour, quantite * produit.getRegularUnitPrice(), remise);
        SalesLine ligne = ligneDeVente(vente, produit, quantite, remise);
        ligne.setQuantityUg(ug);
        em.flush();
        return ligne;
    }

    private List<LigneJournalDTO> journal(NatureJournal nature) {
        return service.listerLignes(nature, duJour(), PageRequest.of(0, 50)).getContent();
    }

    // ===== essais =====

    @Nested
    @DisplayName("Répartition d'une remise")
    class Repartition {

        @Test
        @DisplayName("une ligne cumulée apparaît dans les deux journaux, chacun avec sa part")
        void ligneCumulee() {
            Promotion promo = promotion(TypePromotion.PROMO_POURCENTAGE, 10, 1);
            // 2 × 2 000 : 200 de grille, puis -10 % des 3 800 restants, 380.
            vendre(produit(unique("CREME"), 2_000, 18), 2, 200, promo, 380);

            LigneJournalDTO enPromotion = journal(NatureJournal.PROMOTIONS).getFirst();
            assertThat(enPromotion.partPromotion()).isEqualTo(380);
            assertThat(enPromotion.partRemiseProduit()).isEqualTo(200);
            assertThat(enPromotion.origine()).isEqualTo(OrigineRemise.CUMUL);
            assertThat(journal(NatureJournal.REMISES)).hasSize(1);

            SyntheseJournalDTO synthese = service.calculerSynthese(duJour());
            assertThat(synthese.promotions().remise()).isEqualTo(380);
            assertThat(synthese.remisesProduit().remise()).isEqualTo(200);
            assertThat(synthese.remiseTotale()).isEqualTo(580);
        }

        @Test
        @DisplayName("une ligne promue sans part enregistrée est imputée en entier à la promotion")
        void ligneAnterieureALaPart() {
            Promotion promo = promotion(TypePromotion.PROMO_POURCENTAGE, 10, 1);
            SalesLine ligne = vendre(produit(unique("SIROP"), 1_000, 18), 3, 0, promo, 300);
            ligne.setRemisePromotion(0);
            em.flush();

            assertThat(journal(NatureJournal.PROMOTIONS).getFirst().partPromotion())
                    .isEqualTo(300);
            assertThat(journal(NatureJournal.REMISES)).isEmpty();
        }

        @Test
        @DisplayName("le filtre d'origine sépare la remise produit seule du cumul")
        void filtreOrigine() {
            Promotion promo = promotion(TypePromotion.PROMO_POURCENTAGE, 10, 1);
            vendre(produit(unique("GEL"), 2_000, 18), 2, 200, promo, 380);
            vendre(produit(unique("BAUME"), 1_000, 18), 1, 50, null, 0);

            JournalRemisesFiltre seule = new JournalRemisesFiltre(
                    jour, jour, null, null, null, null, Set.of(OrigineRemise.REMISE_PRODUIT), null);
            assertThat(service.listerLignes(NatureJournal.REMISES, seule, PageRequest.of(0, 10))
                            .getContent())
                    .extracting(LigneJournalDTO::partRemiseProduit)
                    .containsExactly(50);
        }
    }

    @Nested
    @DisplayName("Offre N+M")
    class OffreNM {

        @Test
        @DisplayName("les unités offertes sont dans les promotions, pas dans les UG")
        void unitesOffertes() {
            // 2+1 : 6 unités à 1 000 forment deux groupes, deux unités offertes.
            Promotion offre = promotion(TypePromotion.PROMO_OFFRE_NM, 1, 2);
            vendre(produit(unique("VITAMINE"), 1_000, 0), 6, 0, offre, 2_000);

            SyntheseJournalDTO synthese = service.calculerSynthese(duJour());
            SyntheseJournalDTO.ParPromotion ligne = synthese.parPromotion().getFirst();
            assertThat(ligne.type()).isEqualTo(TypePromotion.PROMO_OFFRE_NM);
            assertThat(ligne.unitesRemisees()).isEqualTo(6);
            assertThat(ligne.unitesOffertes()).isEqualTo(2);
            assertThat(ligne.remise()).isEqualTo(2_000);
            assertThat(synthese.ug().unites()).isZero();
            assertThat(journal(NatureJournal.UG)).isEmpty();
        }
    }

    @Nested
    @DisplayName("UG vendues")
    class UgVendues {

        @Test
        @DisplayName("les UG sont valorisées, et leur marge ne compte pas leur coût")
        void margeDesUg() {
            // 10 unités à 1 180 TTC (18 %), dont 3 UG ; coût unitaire 708 (60 % du prix).
            vendreAvecUg(produit(unique("DOLIPRANE"), 1_180, 18), 10, 3, 0);

            LigneJournalDTO l = journal(NatureJournal.UG).getFirst();
            assertThat(l.quantiteUg()).isEqualTo(3);
            assertThat(l.valeurUg()).isEqualTo(3_540);
            assertThat(l.htNetUg()).isEqualTo(3_000);
            assertThat(l.margeUg()).isEqualTo(3_000);
            assertThat(l.coutEviteUg()).isEqualTo(3 * 708);
            // L'écart entre les deux marges est le coût des UG, compté à tort par les états.
            assertThat(l.margeReelle() - l.margeComptabilisee()).isEqualTo(3 * 708);

            SyntheseJournalDTO synthese = service.calculerSynthese(duJour());
            assertThat(synthese.ug().unites()).isEqualTo(3);
            assertThat(synthese.ug().marge()).isEqualTo(3_000);
            // Les UG ne sont pas une remise : le total des remises n'en dit rien.
            assertThat(synthese.remiseTotale()).isZero();
        }

        @Test
        @DisplayName("la remise d'une ligne touche ses UG au prorata des unités")
        void remiseSurLesUg() {
            vendreAvecUg(produit(unique("ASPIRINE"), 1_000, 0), 4, 1, 400);

            LigneJournalDTO l = journal(NatureJournal.UG).getFirst();
            assertThat(l.remiseUg()).isEqualTo(100);
            assertThat(l.htNetUg()).isEqualTo(900);
        }
    }

    @Nested
    @DisplayName("Périmètre")
    class Perimetre {

        @Test
        @DisplayName("une vente annulée ou une vente dépôt n'entre pas dans le journal")
        void venteAnnuleeEtDepotExclues() {
            SalesLine annulee = vendre(produit(unique("ANNULE"), 1_000, 0), 1, 100, null, 0);
            annulee.getSales().setCanceled(true);
            SalesLine depot = vendre(produit(unique("DEPOT"), 1_000, 0), 1, 100, null, 0);
            depot.getSales().setCategorieChiffreAffaire(CategorieChiffreAffaire.CA_DEPOT);
            em.flush();

            assertThat(journal(NatureJournal.REMISES)).isEmpty();
            assertThat(service.calculerSynthese(duJour()).remiseTotale()).isZero();
        }

        @Test
        @DisplayName("une ligne sans remise ni UG n'y figure pas")
        void ligneOrdinaireAbsente() {
            vendre(produit(unique("ORDINAIRE"), 1_000, 0), 1, 0, null, 0);

            assertThat(journal(NatureJournal.REMISES)).isEmpty();
            assertThat(journal(NatureJournal.PROMOTIONS)).isEmpty();
            assertThat(journal(NatureJournal.UG)).isEmpty();
        }

        @Test
        @DisplayName("la période est obligatoire et ne dépasse pas un an")
        void periode() {
            assertThatThrownBy(() -> service.calculerSynthese(JournalRemisesFiltre.periode(jour, jour.plusYears(2))))
                    .isInstanceOf(BadRequestAlertException.class);
            assertThatThrownBy(() -> service.calculerSynthese(JournalRemisesFiltre.periode(jour, jour.minusDays(1))))
                    .isInstanceOf(BadRequestAlertException.class);
        }
    }

    @Nested
    @DisplayName("Exports")
    class Exports {

        @Test
        @DisplayName("le CSV reprend la synthèse et les trois journaux, avec les totaux de l'écran")
        void csv() {
            Promotion promo = promotion(TypePromotion.PROMO_POURCENTAGE, 10, 1);
            vendre(produit(unique("CREME"), 2_000, 18), 2, 200, promo, 380);

            String csv = new String(service.exporter(FormatExport.CSV, null, duJour()), StandardCharsets.UTF_8);

            assertThat(csv).startsWith("﻿");
            assertThat(csv)
                    .contains(
                            "Remises accordées",
                            "Journal — Promotions",
                            "Journal — Remises produit",
                            "Journal — UG vendues");
            assertThat(csv).contains("Total;;;;;580;", "Généré le:");
        }

        @Test
        @DisplayName("le classeur a une feuille pour la synthèse et une par journal")
        void classeur() throws IOException {
            vendreAvecUg(produit(unique("DOLIPRANE"), 1_180, 18), 10, 3, 0);

            byte[] contenu = service.exporter(FormatExport.XLSX, null, duJour());

            try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(contenu))) {
                assertThat(classeur.getNumberOfSheets()).isEqualTo(4);
                assertThat(classeur.getSheetName(0)).isEqualTo("Synthèse");
                assertThat(classeur.getSheet("UG vendues")).isNotNull();
            }
        }

        @Test
        @DisplayName("le PDF se construit, même pour un seul journal vide")
        void pdf() {
            vendreAvecUg(produit(unique("DOLIPRANE"), 1_180, 18), 10, 3, 0);

            assertThat(service.exporter(FormatExport.PDF, null, duJour())).startsWith("%PDF".getBytes());
            assertThat(service.exporter(FormatExport.PDF, NatureJournal.PROMOTIONS, duJour()))
                    .startsWith("%PDF".getBytes());
        }

        @Test
        @DisplayName("le nom du fichier dit le journal et la période")
        void nomDeFichier() {
            assertThat(service.nommerFichier(
                            FormatExport.XLSX,
                            NatureJournal.UG,
                            JournalRemisesFiltre.periode(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))))
                    .isEqualTo("journal-ug-20261101-20261130.xlsx");
        }
    }

    @Nested
    @DisplayName("Filtres")
    class Filtres {

        private Produit creme;
        private Promotion promo;

        @BeforeEach
        void vendreUneLigneCumulee() {
            promo = promotion(TypePromotion.PROMO_POURCENTAGE, 10, 1);
            creme = produit(unique("CREME"), 2_000, 18);
            vendre(creme, 2, 200, promo, 380);
        }

        /** Les lignes lues et leur nombre : l'écart trahirait une jointure posée deux fois. */
        private List<LigneJournalDTO> lire(JournalRemisesFiltre filtre) {
            Page<LigneJournalDTO> page = service.listerLignes(NatureJournal.PROMOTIONS, filtre, PageRequest.of(0, 50));
            assertThat(page.getTotalElements()).isEqualTo(page.getContent().size());
            return page.getContent();
        }

        private JournalRemisesFiltre filtrer(
                Set<Integer> caissiers,
                Set<Integer> vendeurs,
                Set<NatureVente> natures,
                Set<Long> promotions,
                Set<Integer> familles) {
            return new JournalRemisesFiltre(jour, jour, caissiers, vendeurs, natures, promotions, null, familles);
        }

        @Test
        @DisplayName("chaque filtre retient la ligne qui lui correspond et écarte les autres")
        void chaqueFiltre() {
            Integer moi = utilisateur.getId();
            Integer famille = creme.getFamille().getId();

            assertThat(lire(filtrer(Set.of(moi), null, null, null, null))).hasSize(1);
            assertThat(lire(filtrer(Set.of(-1), null, null, null, null))).isEmpty();
            assertThat(lire(filtrer(null, Set.of(moi), null, null, null))).hasSize(1);
            assertThat(lire(filtrer(null, Set.of(-1), null, null, null))).isEmpty();
            assertThat(lire(filtrer(null, null, Set.of(NatureVente.COMPTANT), null, null)))
                    .hasSize(1);
            assertThat(lire(filtrer(null, null, Set.of(NatureVente.ASSURANCE), null, null)))
                    .isEmpty();
            assertThat(lire(filtrer(null, null, null, Set.of(promo.getId()), null)))
                    .hasSize(1);
            assertThat(lire(filtrer(null, null, null, Set.of(-1L), null))).isEmpty();
            assertThat(lire(filtrer(null, null, null, null, Set.of(famille)))).hasSize(1);
            assertThat(lire(filtrer(null, null, null, null, Set.of(-1)))).isEmpty();
        }

        @Test
        @DisplayName("les filtres se cumulent, et lisent les noms du caissier et du vendeur")
        void filtresCumules() {
            Integer moi = utilisateur.getId();
            List<LigneJournalDTO> lues = lire(filtrer(
                    Set.of(moi),
                    Set.of(moi),
                    Set.of(NatureVente.COMPTANT),
                    Set.of(promo.getId()),
                    Set.of(creme.getFamille().getId())));

            assertThat(lues).hasSize(1);
            LigneJournalDTO ligne = lues.getFirst();
            assertThat(ligne.caissier()).isNotBlank().isEqualTo(ligne.vendeur());
            assertThat(ligne.produit()).isEqualTo(creme.getLibelle());
            assertThat(ligne.partPromotion()).isEqualTo(380);
        }
    }

    @Nested
    @DisplayName("Menu Exports")
    class MenuExports {

        private List<Object[]> lignesRemisees(Map<FiltreExport, List<String>> filtres) {
            try (Stream<Object[]> lignes = IntegrationPostgresDatabase.bean(ExportDonneesRepository.class)
                    .lireLignes(ExportDonnees.LIGNES_REMISEES, jour, jour, filtres)) {
                return lignes.toList();
            }
        }

        @Test
        @DisplayName("l'export de données donne une ligne par ligne remisée ou porteuse d'UG, chaque part à sa place")
        void lignesRemisees() {
            Promotion promo = promotion(TypePromotion.PROMO_POURCENTAGE, 10, 1);
            vendre(produit(unique("CREME"), 2_000, 18), 2, 200, promo, 380);
            vendreAvecUg(produit(unique("UG"), 1_180, 18), 10, 3, 0);
            ligneDeVente(venteFermee(jour, 1_000, 0), produit(unique("SANS"), 1_000, 18), 1, 0);
            em.flush();

            List<Object[]> lues = lignesRemisees(Map.of());

            assertThat(lues).hasSize(2);
            Object[] cumulee =
                    lues.stream().filter(l -> l[10] != null).findFirst().orElseThrow();
            assertThat(cumulee)
                    .hasSize(ExportDonnees.LIGNES_REMISEES.getColonnes().size());
            assertThat(((Number) cumulee[9]).intValue()).as("part promotion").isEqualTo(380);
            assertThat(cumulee[10]).isEqualTo(promo.getLibelle());
            assertThat(((Number) cumulee[11]).intValue())
                    .as("part remise produit")
                    .isEqualTo(200);
            assertThat(((Number) cumulee[12]).intValue()).as("remise totale").isEqualTo(580);
            Object[] ug = lues.stream().filter(l -> l[10] == null).findFirst().orElseThrow();
            assertThat(((Number) ug[6]).intValue()).as("unités UG").isEqualTo(3);
            assertThat(((Number) ug[13]).intValue()).as("valeur des UG").isEqualTo(3_540);

            assertThat(lignesRemisees(Map.of(FiltreExport.PROMOTION, List.of(String.valueOf(promo.getId())))))
                    .hasSize(1);
        }

        @Test
        @DisplayName("le filtre Promotion propose les promotions appliquées, pas les brouillons")
        void optionsPromotion() {
            Promotion validee = promotion(TypePromotion.PROMO_POURCENTAGE, 10, 1);
            Promotion brouillon =
                    promotion(TypePromotion.PROMO_POURCENTAGE, 5, 1).setStatut(StatutPromotion.BROUILLON);
            em.flush();

            List<String> valeurs =
                    IntegrationPostgresDatabase.bean(OptionsFiltreExportRepository.class)
                            .lireOptions(SourceOptionsExport.PROMOTION)
                            .stream()
                            .map(OptionFiltreDTO::valeur)
                            .toList();

            assertThat(valeurs)
                    .contains(String.valueOf(validee.getId()))
                    .doesNotContain(String.valueOf(brouillon.getId()));
        }
    }

    @Nested
    @DisplayName("Menu")
    class Menu {

        @Test
        @DisplayName("le sous-menu suit « Ventes » et précède « Mouvements Caisse » dans la Gestion Courante")
        void placeDansLaGestionCourante() {
            @SuppressWarnings("unchecked")
            List<String> codes = em.createNativeQuery("""
                            SELECT n.code FROM nav_item n
                              JOIN nav_item g ON g.id = n.parent_id
                             WHERE g.code = 'gestion-courante'
                             ORDER BY n.ordre, n.id
                            """).getResultList();

            int ventes = codes.indexOf("ventes");
            assertThat(ventes).isNotNegative();
            assertThat(codes.get(ventes + 1)).isEqualTo("journal-remises");
            assertThat(codes.get(ventes + 2)).isEqualTo("mvt-caisse");
        }

        @Test
        @DisplayName("le journal a ses quatre onglets, et le module Promotions les siens")
        void onglets() {
            @SuppressWarnings("unchecked")
            List<String> codes = em.createNativeQuery("""
                            SELECT n.code FROM nav_item n
                              JOIN nav_item p ON p.id = n.parent_id
                             WHERE p.code IN ('journal-remises', 'promotions') AND n.target_type = 'SECTION'
                             ORDER BY p.code, n.ordre
                            """).getResultList();

            assertThat(codes)
                    .containsExactly(
                            "journal-remises.synthese",
                            "journal-remises.promotions",
                            "journal-remises.remises",
                            "journal-remises.ug",
                            "promotions.liste",
                            "promotions.journal");
        }
    }

    private static SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
