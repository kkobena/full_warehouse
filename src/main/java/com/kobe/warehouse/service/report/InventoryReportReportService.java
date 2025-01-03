package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.InventoryExportWrapper;
import com.kobe.warehouse.service.dto.StoreInventoryGroupExport;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class InventoryReportReportService extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;

    private final Map<String, Object> variablesMap = new HashMap<>();

    private String templateFile;

    public InventoryReportReportService(
        FileStorageProperties fileStorageProperties,
        SpringTemplateEngine templateEngine,
        StorageService storageService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    public Resource printToPdf(InventoryExportWrapper wrapper) throws MalformedURLException {
        return super.getResource(print(wrapper));
    }

    private String print(InventoryExportWrapper wrapper) {
        var inventory = wrapper.getStoreInventory();
        Magasin magasin = storageService.getUser().getMagasin();
        List<StoreInventoryGroupExport> groupExports = wrapper.getInventoryGroups();
        this.templateFile = Constant.INVENTAIRE_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.ENTITY, inventory);
        getParameters().put(Constant.ITEMS, groupExports);
        getParameters().put(Constant.REPORT_SUMMARY, wrapper.getInventoryExportSummaries());
        getParameters().put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
        getParameters().put(Constant.REPORT_TITLE, "Inventaire du " + inventory.getCreatedAt().format(Constant.DATE_FORMATTER));
        return super.printOneReceiptPage();
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
