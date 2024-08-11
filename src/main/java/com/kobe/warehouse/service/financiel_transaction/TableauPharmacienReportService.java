package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import com.kobe.warehouse.service.report.CommonService;
import com.kobe.warehouse.service.report.Constant;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class TableauPharmacienReportService extends CommonService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;

    private final Map<String, Object> variablesMap = new HashMap<>();

    private String templateFile;
    private List<TableauPharmacienDTO> items;

    public TableauPharmacienReportService(
        SpringTemplateEngine templateEngine,
        FileStorageProperties fileStorageProperties,
        StorageService storageService
    ) {
        super(fileStorageProperties);
        this.templateEngine = templateEngine;

        this.storageService = storageService;
    }

    private String print(
        TableauPharmacienWrapper tableauPharmacienWrapper,
        List<GroupeFournisseurDTO> groupeFournisseurs,
        ReportPeriode reportPeriode,
        String groupeBy
    ) {
        var colspan = 2 + groupeFournisseurs.size();
        this.items = tableauPharmacienWrapper.getTableauPharmaciens();
        Magasin magasin = storageService.getUser().getMagasin();
        int itemSize = items.size();
        templateFile = Constant.TABLEAU_PHARMACIEN_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.COLSPAN, colspan);
        getParameters().put(Constant.REPORT_TITLE, this.buildPeriode("Tableau de bord pharmacien ", reportPeriode));
        getParameters().put(Constant.ITEM_SIZE, itemSize);
        getParameters().put(Constant.REPORT_SUMMARY, tableauPharmacienWrapper);
        getParameters().put(Constant.TABLEAU_PHARMACIEN_GROUP_FOURNISSEUR, groupeFournisseurs);
        getParameters().put(Constant.TABLEAU_PHARMACIEN_GROUP_MONTH, "month".equals(groupeBy));
        getParameters().put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
        if (itemSize > Constant.PAGE_SIZE) {
            getParameters().put(Constant.ITEMS, items.subList(0, Constant.PAGE_SIZE));
            getParameters().put(Constant.IS_LAST_PAGE, false);
            return super.printMultiplesReceiptPage();
        } else {
            getParameters().put(Constant.ITEMS, items);
            getParameters().put(Constant.IS_LAST_PAGE, true);
            getParameters().put(Constant.PAGE_COUNT, "1/1");
            return super.printOneReceiptPage();
        }
    }

    @Override
    protected List<TableauPharmacienDTO> getItems() {
        return items;
    }

    @Override
    protected int getMaxiRowCount() {
        return Constant.PAGE_SIZE;
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

    @Override
    protected String getGenerateFileName() {
        return "tableau_pharmacien";
    }

    public Resource exportToPdf(
        TableauPharmacienWrapper tableauPharmacienWrapper,
        List<GroupeFournisseurDTO> groupeFournisseurs,
        ReportPeriode reportPeriode,
        String groupeBy
    ) throws MalformedURLException {
        return this.getResource(print(tableauPharmacienWrapper, groupeFournisseurs, reportPeriode, groupeBy));
    }
}
