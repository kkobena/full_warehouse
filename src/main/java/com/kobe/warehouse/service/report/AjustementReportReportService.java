package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class AjustementReportReportService extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;

    private final Map<String, Object> variablesMap = new HashMap<>();
    private Ajust ajust;
    private String templateFile;

    public AjustementReportReportService(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        FileStorageProperties fileStorageProperties
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    @Override
    protected List<Ajustement> getItems() {
        return this.ajust.getAjustements();
    }

    @Override
    protected int getMaxiRowCount() {
        return Constant.COMMANDE_PAGE_SIZE;
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
        return "ajsutement";
    }

    public String print(Ajust ajust) {
        this.ajust = ajust;
        Magasin magasin = storageService.getUser().getMagasin();
        List<Ajustement> items = this.ajust.getAjustements();
        int itemSize = items.size();
        items.sort(Comparator.comparing(el -> el.getProduit().getFournisseurProduitPrincipal().getCodeCip()));
        templateFile = Constant.AJUSTEMENT_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.ENTITY, this.ajust);
        getParameters().put(Constant.ITEM_SIZE, itemSize);
        getParameters().put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
        if (itemSize > Constant.COMMANDE_PAGE_SIZE) {
            getParameters().put(Constant.ITEMS, items.subList(0, Constant.COMMANDE_PAGE_SIZE));
            getParameters().put(Constant.IS_LAST_PAGE, false);

            return super.printMultiplesReceiptPage();
        } else {
            getParameters().put(Constant.ITEMS, items);
            getParameters().put(Constant.IS_LAST_PAGE, true);
            getParameters().put(Constant.PAGE_COUNT, "1/1");
            return super.printOneReceiptPage();
        }
    }
}
