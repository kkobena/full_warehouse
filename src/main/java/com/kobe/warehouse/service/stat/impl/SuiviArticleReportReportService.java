package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.referential.magasin.MagasinService;
import com.kobe.warehouse.service.report.CommonReportService;
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
public class SuiviArticleReportReportService extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final MagasinService magasinService;

    private final Map<String, Object> variablesMap = new HashMap<>();

    private String templateFile;
    private List<ProduitAuditingState> items;

    public SuiviArticleReportReportService(
        SpringTemplateEngine templateEngine,
        FileStorageProperties fileStorageProperties,
        StorageService storageService,
        MagasinService magasinService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;

        this.storageService = storageService;
        this.magasinService = magasinService;
    }

    private String print(List<ProduitAuditingState> datas, String title) {
        Magasin magasin = storageService.getUser().getMagasin();
        this.items = datas;
        int itemSize = items.size();
        templateFile = Constant.SUIVI_ARTICLE_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.HAS_DEPOT, magasinService.hasDepot());
        getParameters().put(Constant.REPORT_TITLE, title);
        getParameters().put(Constant.ITEM_SIZE, itemSize);
        getParameters().put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
        getParameters().put(Constant.ITEMS, items);
        return super.printOneReceiptPage();
    }

    @Override
    protected List<ProduitAuditingState> getItems() {
        return items;
    }

    @Override
    protected int getMaxiRowCount() {
        return 0;
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
        return "suivi_mouvement_article";
    }

    public Resource exportToPdf(List<ProduitAuditingState> datas, String title) throws MalformedURLException {
        return this.getResource(print(datas, title));
    }
}
