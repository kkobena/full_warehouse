package com.kobe.warehouse.service.report.produit;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.report.jasper.JasperReportService;
import java.net.MalformedURLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ProduitAuditingReportSeviceImpl extends JasperReportService
    implements ProduitAuditingReportSevice {

  public ProduitAuditingReportSeviceImpl(
      FileStorageProperties fileStorageProperties,
      MagasinRepository magasinRepository,
      UserRepository userRepository) {
    super(fileStorageProperties, magasinRepository, userRepository);
  }

  @Override
  public Resource printToPdf(
      List<ProduitAuditingState> datas,
      FournisseurProduit fournisseurProduit,
      ReportPeriode reportPeriode)
      throws MalformedURLException {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    this.addParametter(
        "report_title",
        String.format(
            "SUIVI PRODUIT %s [%s] du %s au %s ",
            fournisseurProduit.getProduit().getLibelle(),
            fournisseurProduit.getCodeCip(),
            reportPeriode.from().format(dateTimeFormatter),
            reportPeriode.to().format(dateTimeFormatter)));
    return this.printToPdf("warehouse_stock_transaction", datas, null);
  }
}
