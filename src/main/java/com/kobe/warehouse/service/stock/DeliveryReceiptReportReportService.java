package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.DeliveryReceipt;
import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class   DeliveryReceiptReportReportService extends CommonReportService {

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;

    private final Map<String, Object> variablesMap = new HashMap<>();
    private String templateFile;

    private DeliveryReceipt deliveryReceipt;

    public DeliveryReceiptReportReportService(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        FileStorageProperties fileStorageProperties
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    @Override
    protected List<DeliveryReceiptItem> getItems() {
        return this.deliveryReceipt.getReceiptItems();
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
        return this.deliveryReceipt.getReceiptRefernce();
    }

    public String print(DeliveryReceipt deliveryReceipt) {
        this.deliveryReceipt = deliveryReceipt;
        Magasin magasin = storageService.getUser().getMagasin();
        List<DeliveryReceiptItem> receiptItems = this.deliveryReceipt.getReceiptItems();
        int itemSize = receiptItems.size();
        receiptItems.sort(Comparator.comparing(el -> el.getFournisseurProduit().getCodeCip()));
        templateFile = Constant.DELIVERY_TEMPLATE_FILE;
        getParameters().put(Constant.MAGASIN, magasin);
        getParameters().put(Constant.ENTITY, this.deliveryReceipt);
        getParameters().put(Constant.ITEM_SIZE, itemSize);
        getParameters().put(Constant.DEVISE, Constant.DEVISE_CONSTANT);
        getParameters().put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
        if (itemSize > Constant.COMMANDE_PAGE_SIZE) {
            getParameters().put(Constant.ITEMS, receiptItems.subList(0, Constant.COMMANDE_PAGE_SIZE));
            getParameters().put(Constant.IS_LAST_PAGE, false);

            return super.printMultiplesReceiptPage();
        } else {
            getParameters().put(Constant.ITEMS, receiptItems);
            getParameters().put(Constant.IS_LAST_PAGE, true);
            getParameters().put(Constant.PAGE_COUNT, "1/1");
            return super.printOneReceiptPage();
        }
    }
}
