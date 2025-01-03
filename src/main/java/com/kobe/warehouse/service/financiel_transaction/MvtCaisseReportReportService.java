package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseWrapper;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class MvtCaisseReportReportService extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;

    private final Map<String, Object> variablesMap = new HashMap<>();

    private String templateFile;
    private List<MvtCaisseDTO> items;

    public MvtCaisseReportReportService(
        SpringTemplateEngine templateEngine,
        FileStorageProperties fileStorageProperties,
        StorageService storageService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;

        this.storageService = storageService;
    }

    private String print(ArrayList<MvtCaisseDTO> items, MvtCaisseWrapper mvtCaisseWrapper, ReportPeriode reportPeriode) {
        this.items = items;

        Magasin magasin = storageService.getUser().getMagasin();
        int itemSize = items.size();
        templateFile = Constant.MVT_CAISSE_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.REPORT_TITLE, this.buildPeriode("Mouvements de caisse", reportPeriode));
        getParameters().put(Constant.ITEM_SIZE, itemSize);
        getParameters().put(Constant.REPORT_SUMMARY, mvtCaisseWrapper);
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
    protected List<MvtCaisseDTO> getItems() {
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
        return "mvt-caisse";
    }

    public Resource exportToPdf(ArrayList<MvtCaisseDTO> items, MvtCaisseWrapper mvtCaisseWrapper, ReportPeriode reportPeriode)
        throws MalformedURLException {
        return this.getResource(print(items, mvtCaisseWrapper, reportPeriode));
    }
}
