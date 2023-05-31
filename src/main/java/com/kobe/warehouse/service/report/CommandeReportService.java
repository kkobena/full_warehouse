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
import com.kobe.warehouse.web.rest.errors.FileStorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.thymeleaf.spring5.SpringTemplateEngine;

@Service
public class CommandeReportService extends CommonService {

  private final SpringTemplateEngine templateEngine;
  private final StorageService storageService;
  private final Path fileStorageLocation;
  private final FileStorageProperties fileStorageProperties;
  private final ReportService reportService;
  private CommandeDTO commande;
  private String templateFile;
  private String contextAsString;
  private Map<String, Object> variablesMap = new HashMap<>();

  public CommandeReportService(
      SpringTemplateEngine templateEngine,
      StorageService storageService,
      FileStorageProperties fileStorageProperties,
      ReportService reportService) {
    this.templateEngine = templateEngine;
    this.storageService = storageService;
    this.fileStorageProperties = fileStorageProperties;
    this.reportService = reportService;
    this.fileStorageLocation =
        Paths.get(this.fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

    try {
      Files.createDirectories(this.fileStorageLocation);
    } catch (IOException ex) {
      throw new FileStorageException(
          "Could not create the directory where the uploaded files will be stored.", ex);
    }
  }

  public Map<String, Object> getVariablesMap() {
    return variablesMap;
  }

  public void setVariablesMap(Map<String, Object> variablesMap) {
    this.variablesMap = variablesMap;
  }

  public String getContextAsString() {
    return contextAsString;
  }

  public void setContextAsString(String contextAsString) {
    this.contextAsString = contextAsString;
  }

  public String getTemplateFile() {
    return templateFile;
  }

  public void setTemplateFile(String templateFile) {
    this.templateFile = templateFile;
  }

  public CommandeDTO getCommande() {
    return commande;
  }

  public void setCommande(CommandeDTO commande) {
    this.commande = commande;
  }

  public String printCommandeEnCours(CommandeDTO commande) {
    setCommande(commande);
    Magasin magasin = storageService.getUser().getMagasin();
    List<OrderLineDTO> orderLineDTOList = commande.getOrderLines();
    orderLineDTOList.sort(Comparator.comparing(OrderLineDTO::getProduitLibelle));
    setTemplateFile(Constant.COMMANDE_EN_COURS_TEMPLATE_FILE);
    this.variablesMap.put(Constant.MAGASIN, magasin);
    this.variablesMap.put(Constant.COMMANDE, commande);
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
    return this.getCommande().getOrderLines();
  }

  @Override
  protected String getDestFilePath() {
    return this.fileStorageLocation
        .resolve(
            getCommande().getOrderRefernce()
                + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss"))
                + ".pdf")
        .toFile()
        .getAbsolutePath();
  }

  @Override
  protected int getMaxiRowCount() {
    return Constant.COMMANDE_PAGE_SIZE;
  }

  @Override
  protected String getTemplateAsHtml() {
    return templateEngine.process(getTemplateFile(), super.getContextVariables());
  }

  @Override
  protected String getTemplateAsHtml(Context context) {
    this.getParameters().forEach(context::setVariable);
    return templateEngine.process(getTemplateFile(), context);
  }

  @Override
  protected Map<String, Object> getParameters() {
    return this.variablesMap;
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
        etiquettes.add(EtiquetteDTO.builder().print(false).order(index).build());
        index++;
      }

      for (DeliveryReceiptItem item : finalItems) {
        for (int i = 0; i < item.getQuantityReceived(); i++) {
          etiquettes.add(buildEtiquetteDTO(item, date, index, rasionSociale));
          index++;
        }
      }
      etiquettes.sort(Comparator.comparing(EtiquetteDTO::getOrder));
      return etiquettes;
    }
    for (DeliveryReceiptItem item : finalItems) {
      for (int i = 0; i < item.getQuantityReceived(); i++) {
        etiquettes.add(buildEtiquetteDTO(item, date, index, rasionSociale));
        index++;
      }
    }

    etiquettes.stream().sorted(Comparator.comparing(EtiquetteDTO::getLibelle));
    return etiquettes;
  }

  private EtiquetteDTO buildEtiquetteDTO(
      DeliveryReceiptItem item, String date, int order, String rasionSociale) {
    FournisseurProduit fournisseurProduit = item.getFournisseurProduit();

    return EtiquetteDTO.builder()
        .code(fournisseurProduit.getCodeCip())
        .prix(String.format("%s CFA", NumberUtil.formatLong(item.getRegularUnitPrice())))
        .print(true)
        .date(date)
        .order(order)
        .magasin(rasionSociale)
        .libelle(fournisseurProduit.getProduit().getLibelle())
        .build();
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
