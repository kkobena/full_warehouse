package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.web.rest.errors.FileStorageException;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommandeReportService extends CommonService {
  private final SpringTemplateEngine templateEngine;
  private final StorageService storageService;
  private final Path fileStorageLocation;
  private final FileStorageProperties fileStorageProperties;
  private CommandeDTO commande;
  private String templateFile;
  private String contextAsString;
  private Map<String, Object> variablesMap = new HashMap<>();

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

  public void setTemplateFile(String templateFile) {
    this.templateFile = templateFile;
  }

  public String getTemplateFile() {
    return templateFile;
  }

  public CommandeDTO getCommande() {
    return commande;
  }

  public void setCommande(CommandeDTO commande) {
    this.commande = commande;
  }

  public CommandeReportService(
      SpringTemplateEngine templateEngine,
      StorageService storageService,
      FileStorageProperties fileStorageProperties) {
    this.templateEngine = templateEngine;
    this.storageService = storageService;
    this.fileStorageProperties = fileStorageProperties;
    this.fileStorageLocation =
        Paths.get(this.fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

    try {
      Files.createDirectories(this.fileStorageLocation);
    } catch (IOException ex) {
      throw new FileStorageException(
          "Could not create the directory where the uploaded files will be stored.", ex);
    }
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
        getParameters().put(Constant.PAGE_COUNT, "1/1" );
         return super.printOneReceiptPage();
    }

  }

  @Override
  protected List<?> getItems() {
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
        this.getParameters().forEach((k, v) -> context.setVariable(k, v));
        return templateEngine.process(getTemplateFile(),  context);
    }

    @Override
  protected Map<String, Object> getParameters() {
    return this.variablesMap;
  }
}
