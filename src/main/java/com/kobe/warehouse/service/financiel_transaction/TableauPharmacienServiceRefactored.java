package com.kobe.warehouse.service.financiel_transaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.ReponseRetourBonItemRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.projection.ReponseRetourBonItemProjection;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.CommandeDataService;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.kobe.warehouse.service.financiel_transaction.TableauPharmacienConstants.GROUPING_MONTHLY;

/**
 * Refactored TableauPharmacien Service with improved maintainability
 * Delegates responsibilities to specialized components
 */
@Service
@Primary
public class TableauPharmacienServiceRefactored implements TableauPharmacienService {

    private static final Logger LOG = LoggerFactory.getLogger(TableauPharmacienServiceRefactored.class);

    // Data access
    private final SalesRepository salesRepository;
    private final CommandeDataService commandeDataService;
    private final ReponseRetourBonItemRepository reponseRetourBonItemRepository;

    // Configuration
    private final AppConfigurationService appConfigurationService;
    private final JsonMapper objectMapper;

    // Specialized services
    private final GroupeFournisseurManager groupeFournisseurManager;
    private final TableauPharmacienCalculator calculator;
    private final TableauPharmacienAggregator aggregator;
    private final TableauPharmacienExportService exportService;
    private final TableauPharmacienReportReportService reportService;

    public TableauPharmacienServiceRefactored(
        SalesRepository salesRepository,
        CommandeDataService commandeDataService,
        ReponseRetourBonItemRepository reponseRetourBonItemRepository,
        AppConfigurationService appConfigurationService,
        JsonMapper objectMapper,
        GroupeFournisseurManager groupeFournisseurManager,
        TableauPharmacienCalculator calculator,
        TableauPharmacienAggregator aggregator,
        TableauPharmacienExportService exportService,
        TableauPharmacienReportReportService reportService
    ) {
        this.salesRepository = salesRepository;
        this.commandeDataService = commandeDataService;
        this.reponseRetourBonItemRepository = reponseRetourBonItemRepository;
        this.appConfigurationService = appConfigurationService;
        this.objectMapper = objectMapper;
        this.groupeFournisseurManager = groupeFournisseurManager;
        this.calculator = calculator;
        this.aggregator = aggregator;
        this.exportService = exportService;
        this.reportService = reportService;
    }

    @Override
    public TableauPharmacienWrapper getTableauPharmacien(MvtParam mvtParam) {
        mvtParam.setExcludeFreeUnit(appConfigurationService.excludeFreeUnit());
        return computeTableauPharmacien(mvtParam);
    }

    @Override
    public Resource exportToPdf(MvtParam mvtParam) throws MalformedURLException {
        TableauPharmacienWrapper wrapper = getTableauPharmacien(mvtParam);
        List<GroupeFournisseurDTO> supplierGroups = fetchGroupGrossisteToDisplay();
        ReportPeriode periode = new ReportPeriode(mvtParam.getFromDate(), mvtParam.getToDate());

        return reportService.exportToPdf(wrapper, supplierGroups, periode, mvtParam.getGroupeBy());
    }

    @Override
    public Resource exportToExcel(MvtParam mvtParam) throws IOException {
        TableauPharmacienWrapper wrapper = getTableauPharmacien(mvtParam);
        List<GroupeFournisseurDTO> supplierGroups = fetchGroupGrossisteToDisplay();

        return exportService.exportToExcel(wrapper, supplierGroups);
    }

    @Override
    public List<GroupeFournisseurDTO> fetchGroupGrossisteToDisplay() {
        return groupeFournisseurManager.getDisplayedSupplierGroups();
    }

    // ===== Private computation methods =====

    /**
     * Main computation orchestrator
     * <p>
     * Flow:
     * 1. Fetch and process sales data
     * 2. Fetch purchases data
     * 3. Fetch supplier returns (avoirs)
     * 4. Aggregate purchases to wrapper
     * 5. Merge purchases with sales by date
     * 6. Merge supplier returns by date
     * 7. Create entries for avoir-only dates (no sales/purchases)
     * 8. Sort and set final data
     * 9. Calculate total avoirs
     * 10. Compute final aggregations and ratios
     */
    private TableauPharmacienWrapper computeTableauPharmacien(MvtParam mvtParam) {
        TableauPharmacienWrapper wrapper = new TableauPharmacienWrapper();
        Set<Integer> displayedGroupIds = groupeFournisseurManager.getDisplayedGroupIds();
        System.err.println("fetchSalesData called with mvtParam: " + mvtParam);
        // 1. Fetch and process sales data
        List<TableauPharmacienDTO> salesData = fetchAndProcessSalesData(mvtParam, wrapper);

        // 2. Fetch purchases data
        List<AchatDTO> purchasesData = fetchPurchasesData(mvtParam);

        // 3. Fetch supplier returns (avoirs)
        List<ReponseRetourBonItemProjection> supplerReturns = fetchSupplierReturns(mvtParam);

        // 4. Aggregate purchases to wrapper
        purchasesData.forEach(achat -> aggregator.aggregatePurchasesToWrapper(wrapper, achat));

        // 5. Merge purchases with sales by date
        List<TableauPharmacienDTO> mergedData = mergePurchasesWithSales(
            salesData,
            purchasesData,
            displayedGroupIds
        );

        // 6. Merge supplier returns (avoirs) by date into TableauPharmacienDTO
        Map<LocalDate, Long> unmatchedAvoirs = aggregator.mergeSupplierReturnsIntoTableau(mergedData, supplerReturns);

        // 7. Create entries for dates with avoirs but no sales/purchases
        List<TableauPharmacienDTO> avoirOnlyEntries = aggregator.createEntriesForAvoirsOnly(unmatchedAvoirs);
        mergedData.addAll(avoirOnlyEntries);

        // 8. Sort and set final data
        mergedData.sort(Comparator.comparing(TableauPharmacienDTO::getMvtDate));
        wrapper.setTableauPharmaciens(mergedData);

        // 9. Calculate total supplier returns from merged data
        long totalAvoirs = aggregator.calculateTotalSupplierReturns(mergedData);
        wrapper.setMontantAvoirFournisseur(totalAvoirs);

        // 10. Compute final aggregations and ratios
        computeFinalAggregations(wrapper);

        return wrapper;
    }

    /**
     * Fetch and process sales data
     */
    private List<TableauPharmacienDTO> fetchAndProcessSalesData(
        MvtParam mvtParam,
        TableauPharmacienWrapper wrapper
    ) {
        List<TableauPharmacienDTO> salesData = fetchSalesFromDatabase(mvtParam);

        // Process each sales entry
        salesData.forEach(dto -> {
            calculator.calculatePaymentTotals(dto);
            calculator.calculateNetAmount(dto);
            calculator.adjustCashAmountForUnitGratuite(dto);
            aggregator.aggregateSalesToWrapper(wrapper, dto);
        });

        return salesData;
    }

    /**
     * Fetch sales data from database
     */
    private List<TableauPharmacienDTO> fetchSalesFromDatabase(MvtParam mvtParam) {
        try {
            String jsonResult;
            String[] statuts = mvtParam.getStatuts().stream()
                .map(SalesStatut::name)
                .toArray(String[]::new);
            String[] categories = mvtParam.getCategorieChiffreAffaires().stream()
                .map(CategorieChiffreAffaire::name)
                .toArray(String[]::new);

            if (GROUPING_MONTHLY.equals(mvtParam.getGroupeBy())) {
                jsonResult = salesRepository.fetchTableauPharmacienReportMensuel(
                    mvtParam.getFromDate(),
                    mvtParam.getToDate(),
                    statuts,
                    categories,
                    mvtParam.isExcludeFreeUnit(),
                    BooleanUtils.toBoolean(mvtParam.getToIgnore())
                );
            } else {
                jsonResult = salesRepository.fetchTableauPharmacienReport(
                    mvtParam.getFromDate(),
                    mvtParam.getToDate(),
                    statuts,
                    categories,
                    mvtParam.isExcludeFreeUnit(),
                    BooleanUtils.toBoolean(mvtParam.getToIgnore())
                );
            }

            return objectMapper.readValue(jsonResult, new TypeReference<>() {
            });
        } catch (Exception e) {
            LOG.error("Error fetching sales data: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Fetch purchases data
     */
    private List<AchatDTO> fetchPurchasesData(MvtParam mvtParam) {
        List<AchatDTO> purchases = commandeDataService.fetchReportTableauPharmacienData(mvtParam);
        purchases.sort(Comparator.comparing(AchatDTO::getOrdreAffichage));
        return purchases;
    }

    /**
     * Fetch supplier returns (avoirs)
     */
    private List<ReponseRetourBonItemProjection> fetchSupplierReturns(MvtParam mvtParam) {
        if (GROUPING_MONTHLY.equals(mvtParam.getGroupeBy())) {
            return reponseRetourBonItemRepository.findByDateRangeGroupByMonth(
                mvtParam.getFromDate().atStartOfDay(),
                mvtParam.getToDate().atTime(LocalTime.MAX)
            );
        }
        return reponseRetourBonItemRepository.findByDateRange(
            mvtParam.getFromDate().atStartOfDay(),
            mvtParam.getToDate().atTime(LocalTime.MAX)
        );
    }

    /**
     * Merge purchases with sales by date
     */
    private List<TableauPharmacienDTO> mergePurchasesWithSales(
        List<TableauPharmacienDTO> salesData,
        List<AchatDTO> purchases,
        Set<Integer> displayedGroupIds
    ) {
        if (purchases.isEmpty()) {
            return salesData;
        }

        // Group purchases by date
        Map<LocalDate, List<AchatDTO>> purchasesByDate = purchases.stream()
            .collect(Collectors.groupingBy(AchatDTO::getMvtDate));

        // Merge with existing sales data
        aggregator.mergeAchatsIntoTableau(salesData, purchasesByDate, displayedGroupIds);

        // Create entries for dates with purchases but no sales
        List<TableauPharmacienDTO> purchaseOnlyEntries = aggregator.createEntriesForAchatsOnly(
            purchasesByDate,
            displayedGroupIds
        );
        salesData.addAll(purchaseOnlyEntries);

        return salesData;
    }

    /**
     * Compute final aggregations and ratios
     */
    private void computeFinalAggregations(TableauPharmacienWrapper wrapper) {
        // Aggregate all supplier purchases
        if (wrapper.getTableauPharmaciens() != null && !wrapper.getTableauPharmaciens().isEmpty()) {
            List<FournisseurAchat> aggregatedGroups =
                aggregator.aggregateFournisseurAchatsAcrossDays(wrapper.getTableauPharmaciens());
            wrapper.setGroupAchats(aggregatedGroups);

            // Compute total net purchase amount
            long totalNetPurchase = aggregatedGroups.stream()
                .mapToLong(f -> f.getAchat().getMontantNet())
                .sum();
            // Subtract montantAvoirFournisseur from montantAchatNet
            wrapper.setMontantAchatNet(totalNetPurchase - wrapper.getMontantAvoirFournisseur());

            // Map for frontend
            Map<Integer, Long> achatFournisseurs = aggregator.aggregateFournisseurAchatsByGroup(
                wrapper.getTableauPharmaciens().stream()
                    .flatMap(t -> t.getGroupAchats().stream())
                    .toList()
            );
            wrapper.setAchatFournisseurs(achatFournisseurs);
        }

        // Calculate ratios
        calculator.calculateRatioVenteAchat(wrapper);
        calculator.calculateRatioAchatVente(wrapper);
    }
}
