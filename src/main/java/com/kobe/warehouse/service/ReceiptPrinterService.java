package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Printer;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.service.report.SaleReceiptService;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.file.Paths;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.Orientation;
import org.apache.pdfbox.printing.PDFPageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReceiptPrinterService {
  private final SaleReceiptService saleReceiptService;
  private final StorageService storageService;
  private final Logger log = LoggerFactory.getLogger(ReceiptPrinterService.class);

  public ReceiptPrinterService(
      SaleReceiptService saleReceiptService, StorageService storageService) {
    this.saleReceiptService = saleReceiptService;
    this.storageService = storageService;
  }

  public void printCashSale(Long saleId) {
    Runnable runnableTask =
        () -> {
          try (PDDocument document =
              Loader.loadPDF(Paths.get(saleReceiptService.printCashReceipt(saleId)).toFile())) {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setPrintService(findPrintService());
            printerJob.setPageable(new PDFPageable(document, Orientation.AUTO));
            printerJob.print();
          } catch (IOException e) {
            log.debug("printCashSale : {}", e.getLocalizedMessage());
          } catch (PrinterException e) {
            log.debug("printCashSale : {}", e.getLocalizedMessage());
          }
        };
    runnableTask.run();
  }

  public void printVoSale(Long saleId) {
    Runnable runnableTask =
        () -> {
          try (PDDocument document =
              Loader.loadPDF(Paths.get(saleReceiptService.printVoReceipt(saleId)).toFile())) {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setPrintService(findPrintService());
            printerJob.setPageable(new PDFPageable(document, Orientation.AUTO));
            printerJob.print();
          } catch (IOException e) {
            log.debug("printVoSale : {}", e.getLocalizedMessage());
          } catch (PrinterException e) {
            log.debug("printVoSale : {}", e.getLocalizedMessage());
          }
        };
    runnableTask.run();
  }

  private PrintService findPrintService() {
    // findPrintService("\\\\192.168.1.104\\HP LaserJet P1007");
    User user = storageService.getUser();
    String printerName = null;
    Printer userPrinter = user.getPrinter();
    if (userPrinter != null) {
      printerName = userPrinter.getName();
    }

    if (StringUtils.isEmpty(printerName)) {
      return PrintServiceLookup.lookupDefaultPrintService();
    }
    PrintService[] printServices = PrinterJob.lookupPrintServices();
    for (PrintService printService : printServices) {
      if (printService.getName().equals(printerName)) {

        return printService;
      }
    }
    return PrintServiceLookup.lookupDefaultPrintService();
  }
}
