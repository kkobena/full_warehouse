package com.kobe.warehouse.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.InventoryExportWrapper;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryGroupExport;
import com.kobe.warehouse.service.dto.StoreInventoryLotGroupExport;
import com.kobe.warehouse.service.dto.enumeration.StoreInventoryExportGroupBy;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import com.kobe.warehouse.service.inventaire.InventoryValuationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * L'état d'inventaire est le document que l'officine archive après un comptage. Il existe en deux
 * formes — par produit, ou détaillé par lot quand la pharmacie suit ses lots — et porte, une fois
 * l'inventaire clos, la valorisation qui chiffre l'écart constaté.
 *
 * <p>C'est cette valorisation qui demande de l'attention : elle n'a de sens que sur un inventaire
 * <b>clos</b>. La calculer sur un comptage en cours reviendrait à chiffrer un écart contre un stock
 * qu'on est encore en train de compter — un chiffre faux dans un document archivé.
 */
@DisplayName("InventoryReportReportService — état d'inventaire")
class InventoryReportReportServiceTest {

    /** Gabarit minimal : Flying Saucer rend réellement, il lui faut du XHTML valide. */
    private static final String HTML = "<html><head><title>Inventaire</title></head><body><p>inventaire</p></body></html>";

    private static final long INVENTAIRE = 42L;

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
    private final StorageService storageService = mock(StorageService.class);
    private final FileStorageProperties fileStorageProperties = mock(FileStorageProperties.class);
    private final InventoryValuationService valuationService = mock(InventoryValuationService.class);

    private final InventoryReportReportService service = new InventoryReportReportService(
        fileStorageProperties,
        templateEngine,
        storageService,
        valuationService
    );

    @BeforeEach
    void officine() {
        Magasin magasin = new Magasin();
        magasin.setId(1);
        magasin.setFullName("PHARMACIE DU PLATEAU");
        magasin.setRegistre("RC-001");
        AppUser utilisateur = new AppUser();
        utilisateur.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(utilisateur);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML);
        when(valuationService.getGlobalSummary(anyLong())).thenReturn(valorisation());
        when(valuationService.getSummaryByGroup(anyLong(), anyString())).thenReturn(List.of());
    }

    // ===== état par produit =====

    @Nested
    @DisplayName("État par produit")
    class EtatParProduit {

        @Test
        @DisplayName("l'état produit un PDF")
        void produitUnPdf() {
            byte[] pdf = service.printToPdf(inventaire("PENDING"));

            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        }

        @Test
        @DisplayName("le document porte l'inventaire, ses groupes et sa synthèse")
        void variablesDuGabarit() {
            InventoryExportWrapper wrapper = inventaire("PENDING");

            service.printToPdf(wrapper);

            assertThat(service.getParameters())
                .containsEntry(Constant.ENTITY, wrapper.getStoreInventory())
                .containsEntry(Constant.ITEMS, wrapper.getInventoryGroups())
                .containsEntry(Constant.REPORT_SUMMARY, wrapper.getInventoryExportSummaries());
        }

        /** Le titre datant l'inventaire, c'est lui qui distingue deux comptages du même rayon. */
        @Test
        @DisplayName("le titre date l'inventaire au jour près")
        void titreDate() {
            service.printToPdf(inventaire("PENDING"));

            assertThat(service.getParameters()).containsEntry(Constant.REPORT_TITLE, "Inventaire du 15/03/2026");
        }

        @Test
        @DisplayName("l'état se rend depuis le gabarit d'inventaire")
        void gabaritUtilise() {
            service.printToPdf(inventaire("PENDING"));

            assertThat(gabaritRendu()).isEqualTo(Constant.INVENTAIRE_TEMPLATE_FILE);
        }
    }

    // ===== état par lot =====

    @Nested
    @DisplayName("État détaillé par lot")
    class EtatParLot {

        @Test
        @DisplayName("l'état par lot porte les groupes de lots et son propre titre")
        void groupesDeLots() {
            List<StoreInventoryLotGroupExport> lots = List.of(new StoreInventoryLotGroupExport("CIP0001", "DOLIPRANE"));

            service.printLotToPdf(inventaire("PENDING"), lots);

            assertThat(service.getParameters()).containsEntry(Constant.ITEMS, lots);
            assertThat(service.getParameters()).containsEntry(Constant.REPORT_TITLE, "Inventaire (lots) du 15/03/2026");
        }

        @Test
        @DisplayName("l'état par lot se rend depuis son propre gabarit")
        void gabaritDedie() {
            service.printLotToPdf(inventaire("PENDING"), List.of());

            assertThat(gabaritRendu()).isEqualTo("inventaire/main-lot");
        }
    }

    // ===== valorisation =====

    @Nested
    @DisplayName("Valorisation")
    class Valorisation {

        /**
         * Chiffrer l'écart d'un comptage encore ouvert reviendrait à comparer un stock à lui-même en
         * cours de modification : la valorisation attend la clôture.
         */
        @Test
        @DisplayName("un inventaire en cours n'est pas valorisé")
        void inventaireEnCours() {
            service.printToPdf(inventaire("PENDING"));

            verify(valuationService, never()).getGlobalSummary(anyLong());
            assertThat(service.getParameters()).doesNotContainKey("valuationGlobal");
        }

        @Test
        @DisplayName("un inventaire clos porte sa valorisation globale et sa ventilation")
        void inventaireClos() {
            service.printToPdf(inventaire("CLOSED"));

            verify(valuationService).getGlobalSummary(INVENTAIRE);
            assertThat(service.getParameters()).containsKey("valuationGlobal").containsKey("valuationGroups");
        }

        /** La ventilation suit le regroupement demandé pour l'export : rayon, famille ou emplacement. */
        @Test
        @DisplayName("la ventilation suit le regroupement demandé pour l'export")
        void ventilationSuitLeRegroupement() {
            service.printToPdf(inventaire("CLOSED", StoreInventoryExportGroupBy.STORAGE));

            verify(valuationService).getSummaryByGroup(INVENTAIRE, "STORAGE");
        }

        @Test
        @DisplayName("l'état par lot d'un inventaire clos est valorisé aussi")
        void etatParLotValorise() {
            service.printLotToPdf(inventaire("CLOSED"), List.of());

            verify(valuationService).getGlobalSummary(INVENTAIRE);
            assertThat(service.getParameters()).containsKey("valuationGlobal");
        }
    }

    // ===== utilitaires =====

    private String gabaritRendu() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(templateEngine, atLeastOnce()).process(captor.capture(), any(Context.class));
        return captor.getValue();
    }

    private static StoreInventorySummaryRecord valorisation() {
        return new StoreInventorySummaryRecord(
            BigDecimal.valueOf(1_000_000),
            BigDecimal.valueOf(980_000),
            BigDecimal.valueOf(1_500_000),
            BigDecimal.valueOf(1_470_000),
            BigDecimal.valueOf(-20_000),
            BigDecimal.valueOf(-30_000)
        );
    }

    private static InventoryExportWrapper inventaire(String statut) {
        return inventaire(statut, StoreInventoryExportGroupBy.RAYON);
    }

    private static InventoryExportWrapper inventaire(String statut, StoreInventoryExportGroupBy regroupement) {
        StoreInventoryDTO inventaire = new StoreInventoryDTO();
        inventaire.setId(INVENTAIRE);
        inventaire.setStatut(statut);
        inventaire.setCreatedAt(LocalDateTime.of(2026, 3, 15, 9, 30));

        InventoryExportWrapper wrapper = new InventoryExportWrapper();
        wrapper.setStoreInventory(inventaire);
        wrapper.setInventoryGroups(List.of(new StoreInventoryGroupExport()));
        wrapper.setExportGroupBy(regroupement);
        return wrapper;
    }
}
