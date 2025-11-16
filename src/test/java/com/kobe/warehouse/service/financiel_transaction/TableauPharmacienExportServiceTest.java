package com.kobe.warehouse.service.financiel_transaction;

import static org.junit.jupiter.api.Assertions.*;

import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.excel.ExcelExportService;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableauPharmacienExportServiceTest {

    @Mock
    private ExcelExportService excelExportService;

    private TableauPharmacienExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new TableauPharmacienExportService(excelExportService);
    }

    // ===== Export Tests =====

    @Test
    void testExportToExcel_normalData() throws Exception {
        TableauPharmacienWrapper wrapper = createWrapper();
        List<GroupeFournisseurDTO> supplierGroups = createSupplierGroups();

        // This test verifies that the method doesn't throw exceptions
        // Actual Excel generation is handled by excelExportService mock
        assertDoesNotThrow(() -> exportService.exportToExcel(wrapper, supplierGroups));
    }

    @Test
    void testExportToExcel_emptyWrapper() throws Exception {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        wrapper.setTableauPharmaciens(new ArrayList<>());
        List<GroupeFournisseurDTO> supplierGroups = createSupplierGroups();

        assertDoesNotThrow(() -> exportService.exportToExcel(wrapper, supplierGroups));
    }

    @Test
    void testExportToExcel_emptySupplierGroups() throws Exception {
        TableauPharmacienWrapper wrapper = createWrapper();
        List<GroupeFournisseurDTO> supplierGroups = new ArrayList<>();

        assertDoesNotThrow(() -> exportService.exportToExcel(wrapper, supplierGroups));
    }

    @Test
    void testExportToExcel_nullSupplierGroups() {
        TableauPharmacienWrapper wrapper = createWrapper();

        // Should throw NullPointerException when trying to process null supplier groups
        assertThrows(NullPointerException.class, () -> exportService.exportToExcel(wrapper, null));
    }

    @Test
    void testExportToExcel_nullWrapper() {
        List<GroupeFournisseurDTO> supplierGroups = createSupplierGroups();

        // Should throw NullPointerException when wrapper is null
        assertThrows(NullPointerException.class, () -> exportService.exportToExcel(null, supplierGroups));
    }

    @Test
    void testExportToExcel_multipleSupplierGroups() throws Exception {
        TableauPharmacienWrapper wrapper = createWrapperWithMultipleGroups();
        List<GroupeFournisseurDTO> supplierGroups = new ArrayList<>();
        supplierGroups.add(createGroupDTO(1, "Supplier A", 10));
        supplierGroups.add(createGroupDTO(2, "Supplier B", 20));
        supplierGroups.add(createGroupDTO(3, "Supplier C", 30));
        supplierGroups.add(createGroupDTO(-1, "Autres", 999));

        assertDoesNotThrow(() -> exportService.exportToExcel(wrapper, supplierGroups));
    }

    @Test
    void testExportToExcel_largeDataset() throws Exception {
        TableauPharmacienWrapper wrapper = createLargeWrapper(100);
        List<GroupeFournisseurDTO> supplierGroups = createSupplierGroups();

        assertDoesNotThrow(() -> exportService.exportToExcel(wrapper, supplierGroups));
    }

    // ===== Helper Methods =====

    private TableauPharmacienWrapper createWrapper() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        List<TableauPharmacienDTO> tableauPharmaciens = new ArrayList<>();

        TableauPharmacienDTO dto1 = createTableauDTO(LocalDate.of(2025, 1, 1), 10000L, 5000L, 5000L, 500L, 9500L, 5);
        dto1.setGroupAchats(createGroupAchats());

        TableauPharmacienDTO dto2 = createTableauDTO(LocalDate.of(2025, 1, 2), 15000L, 7000L, 8000L, 1000L, 14000L, 8);
        dto2.setGroupAchats(createGroupAchats());

        tableauPharmaciens.add(dto1);
        tableauPharmaciens.add(dto2);

        wrapper.setTableauPharmaciens(tableauPharmaciens);
        return wrapper;
    }

    private TableauPharmacienWrapper createWrapperWithMultipleGroups() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        List<TableauPharmacienDTO> tableauPharmaciens = new ArrayList<>();

        TableauPharmacienDTO dto = createTableauDTO(LocalDate.of(2025, 1, 1), 10000L, 5000L, 5000L, 500L, 9500L, 5);

        List<FournisseurAchat> groupAchats = new ArrayList<>();
        groupAchats.add(createFournisseurAchat(1, "Supplier A", 1000L));
        groupAchats.add(createFournisseurAchat(2, "Supplier B", 2000L));
        groupAchats.add(createFournisseurAchat(3, "Supplier C", 1500L));
        groupAchats.add(createFournisseurAchat(-1, "Autres", 500L));
        dto.setGroupAchats(groupAchats);

        tableauPharmaciens.add(dto);
        wrapper.setTableauPharmaciens(tableauPharmaciens);

        return wrapper;
    }

    private TableauPharmacienWrapper createLargeWrapper(int numDays) {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        List<TableauPharmacienDTO> tableauPharmaciens = new ArrayList<>();

        LocalDate startDate = LocalDate.of(2025, 1, 1);
        for (int i = 0; i < numDays; i++) {
            TableauPharmacienDTO dto = createTableauDTO(
                startDate.plusDays(i),
                10000L + (i * 100L),
                5000L,
                5000L + (i * 100L),
                500L,
                9500L + (i * 100L),
                5
            );
            dto.setGroupAchats(createGroupAchats());
            tableauPharmaciens.add(dto);
        }

        wrapper.setTableauPharmaciens(tableauPharmaciens);
        return wrapper;
    }

    private TableauPharmacienDTO createTableauDTO(
        LocalDate date,
        long montantNet,
        long montantComptant,
        long montantCredit,
        long montantRemise,
        long montantBonAchat,
        int nombreVente
    ) {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setMvtDate(date);
        dto.setMontantNet(montantNet);
        dto.setMontantComptant(montantComptant);
        dto.setMontantCredit(montantCredit);
        dto.setMontantRemise(montantRemise);
        dto.setMontantBonAchat(montantBonAchat);
        dto.setNombreVente(nombreVente);
        dto.setMontantAvoirFournisseur(0L);
        dto.setRatioVenteAchat(1.05f);
        dto.setRatioAchatVente(0.95f);
        return dto;
    }

    private List<FournisseurAchat> createGroupAchats() {
        List<FournisseurAchat> groupAchats = new ArrayList<>();
        groupAchats.add(createFournisseurAchat(1, "Supplier A", 3000L));
        groupAchats.add(createFournisseurAchat(2, "Supplier B", 2000L));
        return groupAchats;
    }

    private FournisseurAchat createFournisseurAchat(int id, String libelle, long montantNet) {
        FournisseurAchat fournisseurAchat = new FournisseurAchat();
        fournisseurAchat.setId(id);
        fournisseurAchat.setLibelle(libelle);

        AchatDTO achat = new AchatDTO();
        achat.setMontantNet(montantNet);
        achat.setMontantTtc(montantNet + 100L);
        fournisseurAchat.setAchat(achat);

        return fournisseurAchat;
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
