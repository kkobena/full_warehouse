package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class CommandeReportReportService extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;

    private final Map<String, Object> variablesMap = new HashMap<>();
    private CommandeDTO commande;
    private String templateFile;
    private String contextAsString;

    public CommandeReportReportService(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        FileStorageProperties fileStorageProperties
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    public String printCommandeEnCours(CommandeDTO commande) {
        this.commande = commande;
        Magasin magasin = storageService.getUser().getMagasin();
        List<OrderLineDTO> orderLineDTOList = this.commande.getOrderLines();
        orderLineDTOList.sort(Comparator.comparing(OrderLineDTO::getProduitLibelle));
        this.templateFile = Constant.COMMANDE_EN_COURS_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.COMMANDE, this.commande);
        getParameters().put(Constant.ITEM_SIZE, orderLineDTOList.size());
        getParameters().put(Constant.DEVISE, Constant.DEVISE_CONSTANT);
        getParameters().put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
        if (orderLineDTOList.size() > Constant.COMMANDE_PAGE_SIZE) {
            getParameters().put(Constant.COMMANDE_ITEMS, orderLineDTOList.subList(0, Constant.COMMANDE_PAGE_SIZE));
            getParameters().put(Constant.IS_LAST_PAGE, false);

            return super.printMultiplesReceiptPage();
        } else {
            getParameters().put(Constant.COMMANDE_ITEMS, orderLineDTOList);
            getParameters().put(Constant.IS_LAST_PAGE, true);
            getParameters().put(Constant.PAGE_COUNT, "1/1");
            return super.printOneReceiptPage();
        }
    }

    @Override
    protected List<OrderLineDTO> getItems() {
        return this.commande.getOrderLines();
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
        return this.commande.getOrderRefernce();
    }
}
