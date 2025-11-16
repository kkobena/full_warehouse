package com.kobe.warehouse.service.financiel_transaction;

import static com.kobe.warehouse.service.financiel_transaction.TableauPharmacienConstants.GROUPING_DAILY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.ReponseRetourBonItemRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.dto.projection.ReponseRetourBonItemProjection;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.CommandeDataService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TableauPharmacienServiceRefactoredTest {

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private CommandeDataService commandeDataService;

    @Mock
    private ReponseRetourBonItemRepository reponseRetourBonItemRepository;

    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private JsonMapper objectMapper;

    @Mock
    private GroupeFournisseurManager groupeFournisseurManager;

    @Mock
    private TableauPharmacienExportService exportService;

    @Mock
    private TableauPharmacienReportReportService reportService;

    // Use real instances for calculator and aggregator (simple utilities)
    private TableauPharmacienCalculator calculator;
    private TableauPharmacienAggregator aggregator;

    private TableauPharmacienServiceRefactored service;

    private MvtParam mvtParam;

    @BeforeEach
    void setUp() {
        // Create real instances for simple utilities
        calculator = new TableauPharmacienCalculator();
        aggregator = new TableauPharmacienAggregator(calculator);

        // Manually create service with all dependencies
        service = new TableauPharmacienServiceRefactored(
            salesRepository,
            commandeDataService,
            reponseRetourBonItemRepository,
            appConfigurationService,
            objectMapper,
            groupeFournisseurManager,
            calculator,
            aggregator,
            exportService,
            reportService
        );

        mvtParam = createMvtParam();
    }

    // ===== Integration Tests =====

    @Test
    void testGetTableauPharmacien_completFlow() throws Exception {
        // Setup test data
        LocalDate date1 = LocalDate.of(2025, 1, 1);
        LocalDate date2 = LocalDate.of(2025, 1, 2);
        LocalDate date3 = LocalDate.of(2025, 1, 3);

        // Mock sales data
        List<TableauPharmacienDTO> salesData = new ArrayList<>();
        TableauPharmacienDTO sales1 = createSalesDTO(date1, 10000L, 5000L, 5000L);
        TableauPharmacienDTO sales2 = createSalesDTO(date2, 15000L, 7000L, 8000L);
        salesData.add(sales1);
        salesData.add(sales2);

        String salesJson = "[]"; // Simplified
        when(salesRepository.fetchTableauPharmacienReport(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(salesJson);
        when(objectMapper.readValue(eq(salesJson), any(TypeReference.class))).thenReturn(salesData);

        // Mock purchases data
        List<AchatDTO> purchasesData = new ArrayList<>();
        purchasesData.add(createAchatDTO(date1, 1, "Supplier A", 4000L));
        purchasesData.add(createAchatDTO(date3, 2, "Supplier B", 3000L)); // Date with no sales
        when(commandeDataService.fetchReportTableauPharmacienData(any())).thenReturn(purchasesData);

        // Mock supplier returns
        List<ReponseRetourBonItemProjection> avoirs = new ArrayList<>();
        avoirs.add(createAvoirProjection(date1, 500L));
        avoirs.add(createAvoirProjection(date3, 300L)); // Date with no sales/purchases
        when(reponseRetourBonItemRepository.findByDateRange(any(), any())).thenReturn(avoirs);

        // Mock configuration
        when(appConfigurationService.excludeFreeUnit()).thenReturn(false);

        // Mock group manager
        Set<Integer> displayedGroupIds = Set.of(1, 2);
        when(groupeFournisseurManager.getDisplayedGroupIds()).thenReturn(displayedGroupIds);
        when(groupeFournisseurManager.getDisplayedSupplierGroups()).thenReturn(createSupplierGroups());

        // Execute
        TableauPharmacienWrapper result = service.getTableauPharmacien(mvtParam);

        // Verify
        assertNotNull(result);
    }

    @Test
    void testGetTableauPharmacien_onlySalesData() throws Exception {
        LocalDate date1 = LocalDate.of(2025, 1, 1);

        // Mock only sales data, no purchases or avoirs
        List<TableauPharmacienDTO> salesData = new ArrayList<>();
        salesData.add(createSalesDTO(date1, 10000L, 5000L, 5000L));

        String salesJson = "[]";
        when(salesRepository.fetchTableauPharmacienReport(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(salesJson);
        when(objectMapper.readValue(eq(salesJson), any(TypeReference.class))).thenReturn(salesData);

        when(commandeDataService.fetchReportTableauPharmacienData(any())).thenReturn(new ArrayList<>());
        when(reponseRetourBonItemRepository.findByDateRange(any(), any())).thenReturn(new ArrayList<>());

        when(appConfigurationService.excludeFreeUnit()).thenReturn(false);
        when(groupeFournisseurManager.getDisplayedGroupIds()).thenReturn(Set.of(1, 2));
        when(groupeFournisseurManager.getDisplayedSupplierGroups()).thenReturn(createSupplierGroups());

        TableauPharmacienWrapper result = service.getTableauPharmacien(mvtParam);

        assertNotNull(result);
    }

    @Test
    void testGetTableauPharmacien_onlyPurchasesData() throws Exception {
        LocalDate date1 = LocalDate.of(2025, 1, 1);

        // Mock only purchases data, no sales or avoirs
        when(salesRepository.fetchTableauPharmacienReport(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn("[]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(new ArrayList<>());

        List<AchatDTO> purchasesData = new ArrayList<>();
        purchasesData.add(createAchatDTO(date1, 1, "Supplier A", 4000L));
        when(commandeDataService.fetchReportTableauPharmacienData(any())).thenReturn(purchasesData);

        when(reponseRetourBonItemRepository.findByDateRange(any(), any())).thenReturn(new ArrayList<>());

        when(appConfigurationService.excludeFreeUnit()).thenReturn(false);
        when(groupeFournisseurManager.getDisplayedGroupIds()).thenReturn(Set.of(1, 2));
        when(groupeFournisseurManager.getDisplayedSupplierGroups()).thenReturn(createSupplierGroups());

        TableauPharmacienWrapper result = service.getTableauPharmacien(mvtParam);

        assertNotNull(result);
    }

    @Test
    void testGetTableauPharmacien_onlyAvoirsData() throws Exception {
        LocalDate date1 = LocalDate.of(2025, 1, 1);

        // Mock only avoirs data, no sales or purchases
        when(salesRepository.fetchTableauPharmacienReport(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn("[]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(new ArrayList<>());

        when(commandeDataService.fetchReportTableauPharmacienData(any())).thenReturn(new ArrayList<>());

        List<ReponseRetourBonItemProjection> avoirs = new ArrayList<>();
        avoirs.add(createAvoirProjection(date1, 500L));
        when(reponseRetourBonItemRepository.findByDateRange(any(), any())).thenReturn(avoirs);

        when(appConfigurationService.excludeFreeUnit()).thenReturn(false);
        when(groupeFournisseurManager.getDisplayedGroupIds()).thenReturn(Set.of(1, 2));
        when(groupeFournisseurManager.getDisplayedSupplierGroups()).thenReturn(createSupplierGroups());

        TableauPharmacienWrapper result = service.getTableauPharmacien(mvtParam);

        assertNotNull(result);
    }

    @Test
    void testGetTableauPharmacien_emptyData() throws Exception {
        // Mock all empty data
        when(salesRepository.fetchTableauPharmacienReport(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn("[]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(new ArrayList<>());

        when(commandeDataService.fetchReportTableauPharmacienData(any())).thenReturn(new ArrayList<>());
        when(reponseRetourBonItemRepository.findByDateRange(any(), any())).thenReturn(new ArrayList<>());

        when(appConfigurationService.excludeFreeUnit()).thenReturn(false);
        when(groupeFournisseurManager.getDisplayedGroupIds()).thenReturn(Set.of());
        when(groupeFournisseurManager.getDisplayedSupplierGroups()).thenReturn(new ArrayList<>());

        TableauPharmacienWrapper result = service.getTableauPharmacien(mvtParam);

        assertNotNull(result);
    }

    @Test
    void testGetTableauPharmacien_errorHandling() throws Exception {
        // Mock error in fetching sales data
        when(salesRepository.fetchTableauPharmacienReport(any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenThrow(
            new RuntimeException("Database error")
        );

        when(commandeDataService.fetchReportTableauPharmacienData(any())).thenReturn(new ArrayList<>());
        when(reponseRetourBonItemRepository.findByDateRange(any(), any())).thenReturn(new ArrayList<>());

        when(appConfigurationService.excludeFreeUnit()).thenReturn(false);
        when(groupeFournisseurManager.getDisplayedGroupIds()).thenReturn(Set.of(1, 2));
        when(groupeFournisseurManager.getDisplayedSupplierGroups()).thenReturn(createSupplierGroups());

        // Should handle error gracefully and return wrapper with empty sales data
        TableauPharmacienWrapper result = service.getTableauPharmacien(mvtParam);

        assertNotNull(result);
    }

    @Test
    void testGetTableauPharmacien_excludeFreeUnitTrue() throws Exception {
        when(salesRepository.fetchTableauPharmacienReport(any(), any(), any(), any(), eq(true), anyBoolean())).thenReturn("[]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(new ArrayList<>());

        when(commandeDataService.fetchReportTableauPharmacienData(any())).thenReturn(new ArrayList<>());
        when(reponseRetourBonItemRepository.findByDateRange(any(), any())).thenReturn(new ArrayList<>());

        when(appConfigurationService.excludeFreeUnit()).thenReturn(true);
        when(groupeFournisseurManager.getDisplayedGroupIds()).thenReturn(Set.of(1, 2));
        when(groupeFournisseurManager.getDisplayedSupplierGroups()).thenReturn(createSupplierGroups());

        TableauPharmacienWrapper result = service.getTableauPharmacien(mvtParam);

        assertNotNull(result);
        assertTrue(mvtParam.isExcludeFreeUnit());
    }

    @Test
    void testFetchGroupGrossisteToDisplay() {
        List<GroupeFournisseurDTO> expectedGroups = createSupplierGroups();
        when(groupeFournisseurManager.getDisplayedSupplierGroups()).thenReturn(expectedGroups);

        List<GroupeFournisseurDTO> result = service.fetchGroupGrossisteToDisplay();

        assertNotNull(result);
        assertEquals(expectedGroups.size(), result.size());
    }

    // ===== Helper Methods =====

    private MvtParam createMvtParam() {
        MvtParam param = new MvtParam();
        param.setFromDate(LocalDate.of(2025, 1, 1));
        param.setToDate(LocalDate.of(2025, 1, 31));
        param.setStatuts(Set.of(SalesStatut.CLOSED));
        param.setCategorieChiffreAffaires(Set.of(CategorieChiffreAffaire.CA));
        param.setGroupeBy(GROUPING_DAILY);
        param.setToIgnore(false);
        return param;
    }

    private TableauPharmacienDTO createSalesDTO(LocalDate date, long montantNet, long montantComptant, long montantCredit) {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setMvtDate(date);
        dto.setMontantNet(montantNet);
        dto.setMontantComptant(montantComptant);
        dto.setMontantCredit(montantCredit);
        dto.setMontantTtc(montantNet);
        dto.setMontantRemise(0L);
        dto.setMontantRemiseUg(0L);
        dto.setMontantTtcUg(0L);
        dto.setPayments(new ArrayList<>());
        return dto;
    }

    private AchatDTO createAchatDTO(LocalDate date, int groupeId, String groupeLibelle, long montantNet) {
        AchatDTO achat = new AchatDTO();
        achat.setMvtDate(date);
        achat.setGroupeGrossisteId(groupeId);
        achat.setGroupeGrossiste(groupeLibelle);
        achat.setMontantNet(montantNet);
        achat.setMontantTtc(montantNet);
        achat.setMontantHt(montantNet - 100L);
        achat.setMontantTaxe(100L);
        achat.setMontantRemise(0L);
        achat.setOrdreAffichage(groupeId);
        return achat;
    }

    private ReponseRetourBonItemProjection createAvoirProjection(LocalDate date, Long valeurAchat) {
        return new ReponseRetourBonItemProjection() {
            @Override
            public Integer getAcceptedQty() {
                return 10;
            }

            @Override
            public Integer getFournisseurId() {
                return 1;
            }

            @Override
            public Integer getValeurAchat() {
                return valeurAchat.intValue();
            }

            @Override
            public LocalDate getDateMtv() {
                return date;
            }
        };
    }

    private List<GroupeFournisseurDTO> createSupplierGroups() {
        List<GroupeFournisseurDTO> groups = new ArrayList<>();
        groups.add(createGroupDTO(1, "Supplier A", 10));
        groups.add(createGroupDTO(2, "Supplier B", 20));
        return groups;
    }

    private GroupeFournisseurDTO createGroupDTO(int id, String libelle, int ordre) {
        GroupeFournisseurDTO dto = new GroupeFournisseurDTO();
        dto.setId(id);
        dto.setLibelle(libelle);
        dto.setOdre(ordre);
        return dto;
    }
}
