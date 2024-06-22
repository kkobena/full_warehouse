package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class CommandeReportService extends CommonService {

  private final SpringTemplateEngine templateEngine;
  private final StorageService storageService;

  private final ReportService reportService;
  private final Map<String, Object> variablesMap = new HashMap<>();
  private CommandeDTO commande;
  private String templateFile;
  private String contextAsString;

  public CommandeReportService(
      SpringTemplateEngine templateEngine,
      StorageService storageService,
      FileStorageProperties fileStorageProperties,
      ReportService reportService) {
    super(fileStorageProperties);
    this.templateEngine = templateEngine;
    this.storageService = storageService;
    this.reportService = reportService;
  }

  public String getContextAsString() {
    return contextAsString;
  }

  public void setContextAsString(String contextAsString) {
    this.contextAsString = contextAsString;
  }

  public String printCommandeEnCours(CommandeDTO commande) {
    this.commande = commande;
    Magasin magasin = storageService.getUser().getMagasin();
    List<OrderLineDTO> orderLineDTOList = this.commande.getOrderLines();
    orderLineDTOList.sort(Comparator.comparing(OrderLineDTO::getProduitLibelle));
    this.templateFile = Constant.COMMANDE_EN_COURS_TEMPLATE_FILE;
    this.variablesMap.put(Constant.MAGASIN, magasin);
    this.variablesMap.put(Constant.COMMANDE, this.commande);
    this.variablesMap.put(Constant.ITEM_SIZE, orderLineDTOList.size());
    this.variablesMap.put(Constant.DEVISE, Constant.DEVISE_CONSTANT);
    this.variablesMap.put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
    if (orderLineDTOList.size() > Constant.COMMANDE_PAGE_SIZE) {
      this.variablesMap.put(
          Constant.COMMANDE_ITEMS, orderLineDTOList.subList(0, Constant.COMMANDE_PAGE_SIZE));
      this.variablesMap.put(Constant.IS_LAST_PAGE, false);

      return super.printMultiplesReceiptPage();
    } else {
      this.variablesMap.put(Constant.COMMANDE_ITEMS, orderLineDTOList);
      this.variablesMap.put(Constant.IS_LAST_PAGE, true);
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

  private List<EtiquetteDTO> buildEtiquettes(
      Set<DeliveryReceiptItem> receiptItems, int startAt, String rasionSociale) {
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    List<EtiquetteDTO> etiquettes = new ArrayList<>();
    var finalItems =
        receiptItems.stream()
            .filter(e -> StringUtils.isNotEmpty(e.getFournisseurProduit().getCodeCip()))
            .toList();
    int index = 1;
    if (startAt > 1) {
      for (int i = 1; i <= startAt; i++) {
        etiquettes.add(new EtiquetteDTO().setPrint(false).setOrder(index));
        index++;
      }

      return getEtiquetteDTOS(
          rasionSociale,
          date,
          etiquettes,
          finalItems,
          index,
          Comparator.comparing(EtiquetteDTO::getOrder));
    }
    return getEtiquetteDTOS(
        rasionSociale,
        date,
        etiquettes,
        finalItems,
        index,
        Comparator.comparing(EtiquetteDTO::getLibelle));
  }

  private List<EtiquetteDTO> getEtiquetteDTOS(
      String rasionSociale,
      String date,
      List<EtiquetteDTO> etiquettes,
      List<DeliveryReceiptItem> finalItems,
      int index,
      Comparator<EtiquetteDTO> comparing) {
    for (DeliveryReceiptItem item : finalItems) {
      for (int i = 0; i < item.getQuantityReceived(); i++) {
        etiquettes.add(buildEtiquetteDTO(item, date, index, rasionSociale));
        index++;
      }
    }

    etiquettes.sort(comparing);
    return etiquettes;
  }

  private EtiquetteDTO buildEtiquetteDTO(
      DeliveryReceiptItem item, String date, int order, String rasionSociale) {
    FournisseurProduit fournisseurProduit = item.getFournisseurProduit();

    return new EtiquetteDTO()
        .setCode(fournisseurProduit.getCodeCip())
        .setPrix(String.format("%s CFA", NumberUtil.formatToString(item.getRegularUnitPrice())))
        .setPrint(true)
        .setDate(date)
        .setOrder(order)
        .setMagasin(rasionSociale)
        .setLibelle(fournisseurProduit.getProduit().getLibelle());
  }

  public Resource printEtiquettes(Set<DeliveryReceiptItem> receiptItem, int startAt)
      throws IOException {
    Map<String, Object> parameters = reportService.buildMagasinInfo();

    return reportService.getResource(
        reportService.buildReportToPDF(
            parameters,
            "warehouse_etiqettes",
            buildEtiquettes(receiptItem, startAt, parameters.get("raisonSocial").toString())));
  }
}
