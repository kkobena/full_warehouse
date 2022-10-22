package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.SaleDataService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SaleDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class SaleInvoiceService {
  private final SpringTemplateEngine templateEngine;
  private final SaleDataService saleDataService;
  private final ReportService reportService;
  private static final String MAGASIN = "magasin";
  private static final String SALE = "sale";
  private final StorageService storageService;

  public SaleInvoiceService(
      SpringTemplateEngine templateEngine,
      SaleDataService saleDataService,
      ReportService reportService,
      StorageService storageService) {
    this.templateEngine = templateEngine;
    this.saleDataService = saleDataService;
    this.reportService = reportService;
    this.storageService = storageService;
  }

  private String getTemplateInvoiceContext(SaleDTO sale) {
    Context context = getContext();
    context.setVariable(MAGASIN, storageService.getUser().getMagasin());
    context.setVariable(SALE, sale);
    String content = templateEngine.process("facture/saleInvoice", context);

    return content;
  }

  public String printInvoice(Long saleId) {
    return reportService.buildInvoiceToPDF(
        "invoice", getTemplateInvoiceContext(saleDataService.findOne(saleId)));
  }

  private Context getContext() {
    Locale locale = Locale.forLanguageTag("fr");
    return new Context(locale);
  }
}
