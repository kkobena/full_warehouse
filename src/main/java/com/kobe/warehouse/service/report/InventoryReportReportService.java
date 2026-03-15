package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.InventoryExportWrapper;
import com.kobe.warehouse.service.dto.StoreInventoryGroupExport;
import com.kobe.warehouse.service.dto.StoreInventoryLotGroupExport;
import com.kobe.warehouse.service.stock.InventoryValuationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class InventoryReportReportService extends CommonReportService {

    private static final String LOT_TEMPLATE_FILE = "inventaire/main-lot";
    private static final String VALUATION_GLOBAL = "valuationGlobal";
    private static final String VALUATION_GROUPS = "valuationGroups";

    private final SpringTemplateEngine templateEngine;
    private final InventoryValuationService valuationService;

    private final Map<String, Object> variablesMap = new HashMap<>();

    private String templateFile;

    public InventoryReportReportService(
        FileStorageProperties fileStorageProperties,
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        InventoryValuationService valuationService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.valuationService = valuationService;
    }

    public byte[] printToPdf(InventoryExportWrapper wrapper) {
        return print(wrapper);
    }

    /**
     * Génère le PDF en mode gestion de lot.
     */
    public byte[] printLotToPdf(
        InventoryExportWrapper wrapper,
        List<StoreInventoryLotGroupExport> lotGroups
    ) {
        return printLot(wrapper, lotGroups);
    }

    private byte[] print(InventoryExportWrapper wrapper) {
        var inventory = wrapper.getStoreInventory();
        List<StoreInventoryGroupExport> groupExports = wrapper.getInventoryGroups();
        this.templateFile = Constant.INVENTAIRE_TEMPLATE_FILE;

        getParameters().put(Constant.ENTITY, inventory);
        getParameters().put(Constant.ITEMS, groupExports);
        getParameters().put(Constant.REPORT_SUMMARY, wrapper.getInventoryExportSummaries());
        getParameters().put(Constant.REPORT_TITLE,
            "Inventaire du " + inventory.getCreatedAt().format(Constant.DATE_FORMATTER));
        addValuationIfClosed(inventory.getId(), inventory.getStatut(), wrapper.getExportGroupBy().name());
        super.getCommonParameters();

        return super.exportReportToPdf();
    }

    private byte[] printLot(InventoryExportWrapper wrapper,
        List<StoreInventoryLotGroupExport> lotGroups) {
        var inventory = wrapper.getStoreInventory();
        this.templateFile = LOT_TEMPLATE_FILE;
        getParameters().put(Constant.ENTITY, inventory);
        getParameters().put(Constant.ITEMS, lotGroups);
        getParameters().put(Constant.REPORT_SUMMARY, wrapper.getInventoryExportSummaries());
        getParameters().put(Constant.REPORT_TITLE,
            "Inventaire (lots) du " + inventory.getCreatedAt().format(Constant.DATE_FORMATTER));
        addValuationIfClosed(inventory.getId(), inventory.getStatut(), wrapper.getExportGroupBy().name());
        super.getCommonParameters();

        return super.exportReportToPdf();
    }

    private void addValuationIfClosed(Long inventoryId, String statut, String groupBy) {
        if ("CLOSED".equals(statut)) {
            getParameters().put(VALUATION_GLOBAL, valuationService.getGlobalSummary(inventoryId));
            getParameters().put(VALUATION_GROUPS, valuationService.getSummaryByGroup(inventoryId, groupBy));
        }
    }

    @Override
    protected List<?> getItems() {
        return List.of();
    }

    @Override
    protected int getMaxiRowCount() {
        return 0;
    }

    @Override
    protected String getGenerateFileName() {
        return "inventaire";
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process(templateFile, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process(templateFile, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }
}
