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
public class AjustementReportService extends CommonService {

  private final SpringTemplateEngine templateEngine;
  private final StorageService storageService;

  private final Map<String, Object> variablesMap = new HashMap<>();
  private Ajust ajust;
  private String templateFile;

  public AjustementReportService(
      SpringTemplateEngine templateEngine,
      StorageService storageService,
      FileStorageProperties fileStorageProperties) {
    super(fileStorageProperties);
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
    items.sort(
        Comparator.comparing(el -> el.getProduit().getFournisseurProduitPrincipal().getCodeCip()));
    templateFile = Constant.AJUSTEMENT_TEMPLATE_FILE;
    this.variablesMap.put(Constant.MAGASIN, magasin);
    this.variablesMap.put(Constant.ENTITY, this.ajust);
    this.variablesMap.put(Constant.ITEM_SIZE, itemSize);
    this.variablesMap.put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
    if (itemSize > Constant.COMMANDE_PAGE_SIZE) {
      this.variablesMap.put(Constant.ITEMS, items.subList(0, Constant.COMMANDE_PAGE_SIZE));
      this.variablesMap.put(Constant.IS_LAST_PAGE, false);

      return super.printMultiplesReceiptPage();
    } else {
      this.variablesMap.put(Constant.ITEMS, items);
      this.variablesMap.put(Constant.IS_LAST_PAGE, true);
      getParameters().put(Constant.PAGE_COUNT, "1/1");
      return super.printOneReceiptPage();
    }
  }
}
