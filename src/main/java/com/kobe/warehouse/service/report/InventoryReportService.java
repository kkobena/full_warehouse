package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.dto.InventoryExportSummary;
import com.kobe.warehouse.service.dto.StoreInventoryGroupExport;
import com.kobe.warehouse.service.dto.enumeration.InventoryExportSummaryEnum;
import com.kobe.warehouse.service.report.jasper.JasperReportService;
import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class InventoryReportService extends JasperReportService {

  public InventoryReportService(
      FileStorageProperties fileStorageProperties,
      MagasinRepository magasinRepository,
      UserRepository userRepository) {
    super(fileStorageProperties, magasinRepository, userRepository);
  }

  public Resource printToPdf(List<StoreInventoryGroupExport> datas) throws MalformedURLException {

    buildSummaries(datas);
    return this.printToPdf("warehouse_inventaire", datas, null);
  }

  private void updateSummary(
      InventoryExportSummary inventoryExportSummary, InventoryExportSummary exportSummary) {
    inventoryExportSummary.setValue(inventoryExportSummary.getValue() + exportSummary.getValue());
  }

  private void buildSummaries(List<StoreInventoryGroupExport> datas) {
    InventoryExportSummary achatAvant = new InventoryExportSummary();
    achatAvant.setName(InventoryExportSummaryEnum.ACHAT_AVANT);
    InventoryExportSummary achatApres = new InventoryExportSummary();
    achatApres.setName(InventoryExportSummaryEnum.ACHAT_APRES);

    InventoryExportSummary venteAvant = new InventoryExportSummary();
    venteAvant.setName(InventoryExportSummaryEnum.VENTE_AVANT);
    InventoryExportSummary venteApres = new InventoryExportSummary();
    venteApres.setName(InventoryExportSummaryEnum.VENTE_APRES);

    InventoryExportSummary achatEcart = new InventoryExportSummary();
    achatEcart.setName(InventoryExportSummaryEnum.ACHAT_ECART);
    InventoryExportSummary venteEcart = new InventoryExportSummary();
    venteEcart.setName(InventoryExportSummaryEnum.VENTE_ECART);
    for (StoreInventoryGroupExport export : datas) {

      List<InventoryExportSummary> totaux =
          Stream.of(export.getTotaux(), export.getTotauxEcart(), export.getTotauxVente())
              .flatMap(List::stream)
              .toList();

      for (InventoryExportSummary exportSummary : totaux) {

        switch (exportSummary.getName()) {
          case ACHAT_AVANT -> updateSummary(achatAvant, exportSummary);
          case ACHAT_APRES -> updateSummary(achatApres, exportSummary);
          case ACHAT_ECART -> updateSummary(achatEcart, exportSummary);
          case VENTE_APRES -> updateSummary(venteApres, exportSummary);
          case VENTE_AVANT -> updateSummary(venteAvant, exportSummary);
          case VENTE_ECART -> updateSummary(venteEcart, exportSummary);
        }
      }
    }
    List<InventoryExportSummary> inventoryExportSummaries =
        List.of(achatAvant, achatApres, venteAvant, venteApres, achatEcart, venteEcart);

    this.addParametter("SUMMARIES", inventoryExportSummaries);
  }
}
