package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.DeliveryReceipt;
import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
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
import org.thymeleaf.spring5.SpringTemplateEngine;

@Service
public class DeliveryReceiptReportService extends CommonService {
  private final SpringTemplateEngine templateEngine;
  private final StorageService storageService;

  private final ReportService reportService;
  private final Map<String, Object> variablesMap = new HashMap<>();
  private String templateFile;

  private DeliveryReceipt deliveryReceipt;

  public DeliveryReceiptReportService(
      SpringTemplateEngine templateEngine,
      StorageService storageService,
      FileStorageProperties fileStorageProperties,
      ReportService reportService) {
    super(fileStorageProperties);
    this.templateEngine = templateEngine;
    this.storageService = storageService;
    this.reportService = reportService;
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
    int itemSize=receiptItems.size();
    receiptItems.sort(Comparator.comparing(el -> el.getFournisseurProduit().getCodeCip()));
    templateFile = Constant.DELIVERY_TEMPLATE_FILE;
    this.variablesMap.put(Constant.MAGASIN, magasin);
    this.variablesMap.put(Constant.ENTITY, this.deliveryReceipt);
    this.variablesMap.put(Constant.ITEM_SIZE, itemSize);
    this.variablesMap.put(Constant.DEVISE, Constant.DEVISE_CONSTANT);
    this.variablesMap.put(Constant.FOOTER, "\"" + super.builderFooter(magasin) + "\"");
    if (itemSize > Constant.COMMANDE_PAGE_SIZE) {
      this.variablesMap.put(
          Constant.ITEMS, receiptItems.subList(0, Constant.COMMANDE_PAGE_SIZE));
      this.variablesMap.put(Constant.IS_LAST_PAGE, false);

      return super.printMultiplesReceiptPage();
    } else {
      this.variablesMap.put(Constant.ITEMS, receiptItems);
      this.variablesMap.put(Constant.IS_LAST_PAGE, true);
      getParameters().put(Constant.PAGE_COUNT, "1/1");
      return super.printOneReceiptPage();
    }
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

        return EtiquetteDTO.builder()
            .code(fournisseurProduit.getCodeCip())
            .prix(String.format("%s CFA", NumberUtil.formatToString(item.getRegularUnitPrice())))
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
