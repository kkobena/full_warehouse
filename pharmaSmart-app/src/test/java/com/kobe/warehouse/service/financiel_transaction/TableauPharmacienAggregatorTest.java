package com.kobe.warehouse.service.financiel_transaction;

import static org.junit.jupiter.api.Assertions.*;

import com.kobe.warehouse.service.dto.projection.ReponseRetourBonItemProjection;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableauPharmacienAggregatorTest {

    private TableauPharmacienAggregator aggregator;
    private TableauPharmacienCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new TableauPharmacienCalculator();
        aggregator = new TableauPharmacienAggregator(calculator);
    }

    // ===== FournisseurAchat Aggregation Tests =====

    @Test
    void testAggregateFournisseurAchatsByGroup_normal() {
        List<FournisseurAchat> groupAchats = new ArrayList<>();
        groupAchats.add(createFournisseurAchat(1, "Supplier A", 1000L));
        groupAchats.add(createFournisseurAchat(1, "Supplier A", 500L));
        groupAchats.add(createFournisseurAchat(2, "Supplier B", 2000L));
        groupAchats.add(createFournisseurAchat(2, "Supplier B", 1500L));

        Map<Integer, Long> result = aggregator.aggregateFournisseurAchatsByGroup(groupAchats);

        assertEquals(2, result.size());
        assertEquals(1500L, result.get(1)); // 1000 + 500
        assertEquals(3500L, result.get(2)); // 2000 + 1500
    }

    @Test
    void testAggregateFournisseurAchatsByGroup_emptyList() {
        Map<Integer, Long> result = aggregator.aggregateFournisseurAchatsByGroup(new ArrayList<>());

        assertTrue(result.isEmpty());
    }

    @Test
    void testAggregateFournisseurAchatsByGroup_null() {
        Map<Integer, Long> result = aggregator.aggregateFournisseurAchatsByGroup(null);

        assertTrue(result.isEmpty());
    }

    // ===== Build FournisseurAchats for Day Tests =====

    @Test
    void testBuildFournisseurAchatsForDay_normal() {
        Set<Integer> displayedGroupIds = Set.of(1, 2, 3);
        List<AchatDTO> achats = new ArrayList<>();
        achats.add(createAchatDTO(1, "Supplier A", 1000L));
        achats.add(createAchatDTO(1, "Supplier A", 500L));
        achats.add(createAchatDTO(2, "Supplier B", 2000L));

        List<FournisseurAchat> result = aggregator.buildFournisseurAchatsForDay(achats, displayedGroupIds);

        assertEquals(2, result.size());

        FournisseurAchat supplierA = result.stream().filter(f -> f.getId() == 1).findFirst().orElseThrow();
        assertEquals(1500L, supplierA.getAchat().getMontantNet());

        FournisseurAchat supplierB = result.stream().filter(f -> f.getId() == 2).findFirst().orElseThrow();
        assertEquals(2000L, supplierB.getAchat().getMontantNet());
    }

    @Test
    void testBuildFournisseurAchatsForDay_groupAsOthers() {
        Set<Integer> displayedGroupIds = Set.of(1, 2); // Only 1 and 2 displayed
        List<AchatDTO> achats = new ArrayList<>();
        achats.add(createAchatDTO(1, "Supplier A", 1000L));
        achats.add(createAchatDTO(3, "Supplier C", 500L)); // Not in displayed
        achats.add(createAchatDTO(4, "Supplier D", 300L)); // Not in displayed

        List<FournisseurAchat> result = aggregator.buildFournisseurAchatsForDay(achats, displayedGroupIds);

        assertEquals(2, result.size());

        // Check "Others" group (-1)
        FournisseurAchat others = result.stream().filter(f -> f.getId() == -1).findFirst().orElseThrow();
        assertEquals(800L, others.getAchat().getMontantNet()); // 500 + 300
    }

    @Test
    void testBuildFournisseurAchatsForDay_emptyAchats() {
        Set<Integer> displayedGroupIds = Set.of(1, 2);

        List<FournisseurAchat> result = aggregator.buildFournisseurAchatsForDay(new ArrayList<>(), displayedGroupIds);

        assertTrue(result.isEmpty());
    }

    @Test
    void testBuildFournisseurAchatsForDay_nullAchats() {
        Set<Integer> displayedGroupIds = Set.of(1, 2);

        List<FournisseurAchat> result = aggregator.buildFournisseurAchatsForDay(null, displayedGroupIds);

        assertTrue(result.isEmpty());
    }

    // ===== Aggregate FournisseurAchats Across Days Tests =====

    @Test
    void testAggregateFournisseurAchatsAcrossDays_multipleDays() {
        List<TableauPharmacienDTO> tableauPharmaciens = new ArrayList<>();

        // Day 1
        TableauPharmacienDTO day1 = new TableauPharmacienDTO();
        List<FournisseurAchat> day1Achats = new ArrayList<>();
        day1Achats.add(createFournisseurAchat(1, "Supplier A", 1000L));
        day1Achats.add(createFournisseurAchat(2, "Supplier B", 2000L));
        day1.setGroupAchats(day1Achats);

        // Day 2
        TableauPharmacienDTO day2 = new TableauPharmacienDTO();
        List<FournisseurAchat> day2Achats = new ArrayList<>();
        day2Achats.add(createFournisseurAchat(1, "Supplier A", 500L));
        day2Achats.add(createFournisseurAchat(3, "Supplier C", 1500L));
        day2.setGroupAchats(day2Achats);

        tableauPharmaciens.add(day1);
        tableauPharmaciens.add(day2);

        List<FournisseurAchat> result = aggregator.aggregateFournisseurAchatsAcrossDays(tableauPharmaciens);

        assertEquals(3, result.size());

        FournisseurAchat supplierA = result.stream().filter(f -> f.getId() == 1).findFirst().orElseThrow();
        assertEquals(1500L, supplierA.getAchat().getMontantNet()); // 1000 + 500

        FournisseurAchat supplierB = result.stream().filter(f -> f.getId() == 2).findFirst().orElseThrow();
        assertEquals(2000L, supplierB.getAchat().getMontantNet());

        FournisseurAchat supplierC = result.stream().filter(f -> f.getId() == 3).findFirst().orElseThrow();
        assertEquals(1500L, supplierC.getAchat().getMontantNet());
    }

    @Test
    void testAggregateFournisseurAchatsAcrossDays_emptyList() {
        List<FournisseurAchat> result = aggregator.aggregateFournisseurAchatsAcrossDays(new ArrayList<>());

        assertTrue(result.isEmpty());
    }

    @Test
    void testAggregateFournisseurAchatsAcrossDays_null() {
        List<FournisseurAchat> result = aggregator.aggregateFournisseurAchatsAcrossDays(null);

        assertTrue(result.isEmpty());
    }

    // ===== Merge Achats Into Tableau Tests =====

    @Test
    void testMergeAchatsIntoTableau_matchingDates() {
        LocalDate date1 = LocalDate.of(2025, 1, 1);
        LocalDate date2 = LocalDate.of(2025, 1, 2);

        List<TableauPharmacienDTO> tableauPharmaciens = new ArrayList<>();
        TableauPharmacienDTO dto1 = new TableauPharmacienDTO();
        dto1.setMvtDate(date1);
        TableauPharmacienDTO dto2 = new TableauPharmacienDTO();
        dto2.setMvtDate(date2);
        tableauPharmaciens.add(dto1);
        tableauPharmaciens.add(dto2);

        Map<LocalDate, List<AchatDTO>> achatsByDate = new HashMap<>();
        achatsByDate.put(date1, List.of(createAchatDTO(1, "Supplier A", 1000L)));
        achatsByDate.put(date2, List.of(createAchatDTO(2, "Supplier B", 2000L)));

        Set<Integer> displayedGroupIds = Set.of(1, 2);

        aggregator.mergeAchatsIntoTableau(tableauPharmaciens, achatsByDate, displayedGroupIds);

        assertEquals(1000L, dto1.getMontantBonAchat());
        assertNotNull(dto1.getGroupAchats());
        assertEquals(1, dto1.getGroupAchats().size());

        assertEquals(2000L, dto2.getMontantBonAchat());
        assertNotNull(dto2.getGroupAchats());
        assertEquals(1, dto2.getGroupAchats().size());

        // achatsByDate should be empty after merge (all dates matched)
        assertTrue(achatsByDate.isEmpty());
    }

    // ===== Create Entries for Achats Only Tests =====

    @Test
    void testCreateEntriesForAchatsOnly_unmatchedDates() {
        LocalDate date1 = LocalDate.of(2025, 1, 1);
        LocalDate date2 = LocalDate.of(2025, 1, 2);

        Map<LocalDate, List<AchatDTO>> remainingAchats = new HashMap<>();
        remainingAchats.put(date1, List.of(createAchatDTO(1, "Supplier A", 1000L)));
        remainingAchats.put(date2, List.of(createAchatDTO(2, "Supplier B", 2000L)));

        Set<Integer> displayedGroupIds = Set.of(1, 2);

        List<TableauPharmacienDTO> result = aggregator.createEntriesForAchatsOnly(remainingAchats, displayedGroupIds);

        assertEquals(2, result.size());

        TableauPharmacienDTO dto1 = result.stream().filter(d -> d.getMvtDate().equals(date1)).findFirst().orElseThrow();
        assertEquals(1000L, dto1.getMontantBonAchat());

        TableauPharmacienDTO dto2 = result.stream().filter(d -> d.getMvtDate().equals(date2)).findFirst().orElseThrow();
        assertEquals(2000L, dto2.getMontantBonAchat());
    }

    // ===== Wrapper Aggregation Tests =====

    @Test
    void testAggregateSalesToWrapper_normal() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();

        // createSalesDTO params: credit, comptant, ht, ttc, taxe, remise, net, nombreVente
        TableauPharmacienDTO dto1 = createSalesDTO(500L, 800L, 1000L, 1200L, 100L, 50L, 1100L, 5);
        TableauPharmacienDTO dto2 = createSalesDTO(1000L, 1500L, 1000L, 2500L, 200L, 100L, 2300L, 3);

        aggregator.aggregateSalesToWrapper(wrapper, dto1);
        aggregator.aggregateSalesToWrapper(wrapper, dto2);

        assertEquals(1500L, wrapper.getMontantVenteCredit()); // 500 + 1000
        assertEquals(2300L, wrapper.getMontantVenteComptant()); // 800 + 1500
        assertEquals(2000L, wrapper.getMontantVenteHt()); // 1000 + 1000
        assertEquals(3700L, wrapper.getMontantVenteTtc()); // 1200 + 2500
        assertEquals(300L, wrapper.getMontantVenteTaxe()); // 100 + 200
        assertEquals(150L, wrapper.getMontantVenteRemise()); // 50 + 100
        assertEquals(3400L, wrapper.getMontantVenteNet()); // 1100 + 2300
        assertEquals(8, wrapper.getNumberCount()); // 5 + 3
    }

    @Test
    void testAggregatePurchasesToWrapper_normal() {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();

        AchatDTO achat1 = createFullAchatDTO(1000L, 1100L, 900L, 100L, 50L);
        AchatDTO achat2 = createFullAchatDTO(2000L, 2200L, 1800L, 200L, 100L);

        aggregator.aggregatePurchasesToWrapper(wrapper, achat1);
        aggregator.aggregatePurchasesToWrapper(wrapper, achat2);

        assertEquals(3300L, wrapper.getMontantAchatTtc()); // 1100 + 2200
        assertEquals(150L, wrapper.getMontantAchatRemise()); // 50 + 100
        assertEquals(3000L, wrapper.getMontantAchatNet()); // 1000 + 2000
        assertEquals(300L, wrapper.getMontantAchatTaxe()); // 100 + 200
        assertEquals(2700L, wrapper.getMontantAchatHt()); // 900 + 1800
    }

    // ===== Supplier Returns (Avoirs) Tests =====

    @Test
    void testMergeSupplierReturnsIntoTableau_matchingDates() {
        LocalDate date1 = LocalDate.of(2025, 1, 1);
        LocalDate date2 = LocalDate.of(2025, 1, 2);

        List<TableauPharmacienDTO> tableauPharmaciens = new ArrayList<>();
        TableauPharmacienDTO dto1 = new TableauPharmacienDTO();
        dto1.setMvtDate(date1);
        TableauPharmacienDTO dto2 = new TableauPharmacienDTO();
        dto2.setMvtDate(date2);
        tableauPharmaciens.add(dto1);
        tableauPharmaciens.add(dto2);

        List<ReponseRetourBonItemProjection> avoirs = new ArrayList<>();
        avoirs.add(createAvoirProjection(date1, 500L));
        avoirs.add(createAvoirProjection(date1, 300L)); // Same date
        avoirs.add(createAvoirProjection(date2, 1000L));

        Map<LocalDate, Long> unmatchedAvoirs = aggregator.mergeSupplierReturnsIntoTableau(tableauPharmaciens, avoirs);

        assertEquals(800L, dto1.getMontantAvoirFournisseur()); // 500 + 300
        assertEquals(1000L, dto2.getMontantAvoirFournisseur());
        assertTrue(unmatchedAvoirs.isEmpty()); // All matched
    }

    @Test
    void testMergeSupplierReturnsIntoTableau_unmatchedDates() {
        LocalDate date1 = LocalDate.of(2025, 1, 1);
        LocalDate date2 = LocalDate.of(2025, 1, 2);
        LocalDate date3 = LocalDate.of(2025, 1, 3);

        List<TableauPharmacienDTO> tableauPharmaciens = new ArrayList<>();
        TableauPharmacienDTO dto1 = new TableauPharmacienDTO();
        dto1.setMvtDate(date1);
        tableauPharmaciens.add(dto1);

        List<ReponseRetourBonItemProjection> avoirs = new ArrayList<>();
        avoirs.add(createAvoirProjection(date1, 500L));
        avoirs.add(createAvoirProjection(date2, 1000L)); // No matching DTO
        avoirs.add(createAvoirProjection(date3, 700L)); // No matching DTO

        Map<LocalDate, Long> unmatchedAvoirs = aggregator.mergeSupplierReturnsIntoTableau(tableauPharmaciens, avoirs);

        assertEquals(500L, dto1.getMontantAvoirFournisseur());
        assertEquals(2, unmatchedAvoirs.size());
        assertEquals(1000L, unmatchedAvoirs.get(date2));
        assertEquals(700L, unmatchedAvoirs.get(date3));
    }

    @Test
    void testMergeSupplierReturnsIntoTableau_emptyTableau() {
        LocalDate date1 = LocalDate.of(2025, 1, 1);

        List<TableauPharmacienDTO> tableauPharmaciens = new ArrayList<>();

        List<ReponseRetourBonItemProjection> avoirs = new ArrayList<>();
        avoirs.add(createAvoirProjection(date1, 500L));

        Map<LocalDate, Long> unmatchedAvoirs = aggregator.mergeSupplierReturnsIntoTableau(tableauPharmaciens, avoirs);

        assertEquals(1, unmatchedAvoirs.size());
        assertEquals(500L, unmatchedAvoirs.get(date1));
    }

    @Test
    void testMergeSupplierReturnsIntoTableau_emptyAvoirs() {
        LocalDate date1 = LocalDate.of(2025, 1, 1);

        List<TableauPharmacienDTO> tableauPharmaciens = new ArrayList<>();
        TableauPharmacienDTO dto1 = new TableauPharmacienDTO();
        dto1.setMvtDate(date1);
        tableauPharmaciens.add(dto1);

        List<ReponseRetourBonItemProjection> avoirs = new ArrayList<>();

        Map<LocalDate, Long> unmatchedAvoirs = aggregator.mergeSupplierReturnsIntoTableau(tableauPharmaciens, avoirs);

        assertTrue(unmatchedAvoirs.isEmpty());
        assertEquals(0L, dto1.getMontantAvoirFournisseur());
    }

    @Test
    void testMergeSupplierReturnsIntoTableau_nullInputs() {
        Map<LocalDate, Long> result1 = aggregator.mergeSupplierReturnsIntoTableau(null, null);
        assertTrue(result1.isEmpty());

        Map<LocalDate, Long> result2 = aggregator.mergeSupplierReturnsIntoTableau(new ArrayList<>(), null);
        assertTrue(result2.isEmpty());

        Map<LocalDate, Long> result3 = aggregator.mergeSupplierReturnsIntoTableau(null, new ArrayList<>());
        assertTrue(result3.isEmpty());
    }

    // ===== Create Entries for Avoirs Only Tests =====

    @Test
    void testCreateEntriesForAvoirsOnly_normal() {
        LocalDate date1 = LocalDate.of(2025, 1, 1);
        LocalDate date2 = LocalDate.of(2025, 1, 2);

        Map<LocalDate, Long> unmatchedAvoirs = new HashMap<>();
        unmatchedAvoirs.put(date1, 500L);
        unmatchedAvoirs.put(date2, 1000L);

        List<TableauPharmacienDTO> result = aggregator.createEntriesForAvoirsOnly(unmatchedAvoirs);

        assertEquals(2, result.size());

        TableauPharmacienDTO dto1 = result.stream().filter(d -> d.getMvtDate().equals(date1)).findFirst().orElseThrow();
        assertEquals(500L, dto1.getMontantAvoirFournisseur());

        TableauPharmacienDTO dto2 = result.stream().filter(d -> d.getMvtDate().equals(date2)).findFirst().orElseThrow();
        assertEquals(1000L, dto2.getMontantAvoirFournisseur());
    }

    @Test
    void testCreateEntriesForAvoirsOnly_emptyMap() {
        List<TableauPharmacienDTO> result = aggregator.createEntriesForAvoirsOnly(new HashMap<>());

        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateEntriesForAvoirsOnly_null() {
        List<TableauPharmacienDTO> result = aggregator.createEntriesForAvoirsOnly(null);

        assertTrue(result.isEmpty());
    }

    // ===== Calculate Total Supplier Returns Tests =====

    @Test
    void testCalculateTotalSupplierReturns_normal() {
        List<TableauPharmacienDTO> tableauPharmaciens = new ArrayList<>();

        TableauPharmacienDTO dto1 = new TableauPharmacienDTO();
        dto1.setMontantAvoirFournisseur(500L);

        TableauPharmacienDTO dto2 = new TableauPharmacienDTO();
        dto2.setMontantAvoirFournisseur(1000L);

        TableauPharmacienDTO dto3 = new TableauPharmacienDTO();
        dto3.setMontantAvoirFournisseur(300L);

        tableauPharmaciens.add(dto1);
        tableauPharmaciens.add(dto2);
        tableauPharmaciens.add(dto3);

        long total = aggregator.calculateTotalSupplierReturns(tableauPharmaciens);

        assertEquals(1800L, total); // 500 + 1000 + 300
    }

    @Test
    void testCalculateTotalSupplierReturns_emptyList() {
        long total = aggregator.calculateTotalSupplierReturns(new ArrayList<>());

        assertEquals(0L, total);
    }

    @Test
    void testCalculateTotalSupplierReturns_null() {
        long total = aggregator.calculateTotalSupplierReturns(null);

        assertEquals(0L, total);
    }

    // ===== Helper Methods =====

    private FournisseurAchat createFournisseurAchat(Integer id, String libelle, long montantNet) {
        FournisseurAchat fournisseurAchat = new FournisseurAchat();
        fournisseurAchat.setId(id);
        fournisseurAchat.setLibelle(libelle);
        AchatDTO achat = new AchatDTO();
        achat.setMontantNet(montantNet);
        fournisseurAchat.setAchat(achat);
        return fournisseurAchat;
    }

    private AchatDTO createAchatDTO(int groupeGrossisteId, String groupeGrossiste, long montantNet) {
        AchatDTO achat = new AchatDTO();
        achat.setGroupeGrossisteId(groupeGrossisteId);
        achat.setGroupeGrossiste(groupeGrossiste);
        achat.setMontantNet(montantNet);
        return achat;
    }

    private AchatDTO createFullAchatDTO(long net, long ttc, long ht, long taxe, long remise) {
        AchatDTO achat = new AchatDTO();
        achat.setMontantNet(net);
        achat.setMontantTtc(ttc);
        achat.setMontantHt(ht);
        achat.setMontantTaxe(taxe);
        achat.setMontantRemise(remise);
        return achat;
    }

    private TableauPharmacienDTO createSalesDTO(
        long credit,
        long comptant,
        long ht,
        long ttc,
        long taxe,
        long remise,
        long net,
        int nombreVente
    ) {
        TableauPharmacienDTO dto = new TableauPharmacienDTO();
        dto.setMontantCredit(credit);
        dto.setMontantComptant(comptant);
        dto.setMontantHt(ht);
        dto.setMontantTtc(ttc);
        dto.setMontantTaxe(taxe);
        dto.setMontantRemise(remise);
        dto.setMontantNet(net);
        dto.setNombreVente(nombreVente);
        return dto;
    }

    private ReponseRetourBonItemProjection createAvoirProjection(LocalDate date, Long valeurAchat) {
        return new ReponseRetourBonItemProjection() {
            @Override
            public Integer getAcceptedQty() {
                return null;
            }

            @Override
            public Integer getFournisseurId() {
                return null;
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
}
