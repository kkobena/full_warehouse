package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Printer;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.service.report.SaleReceiptService;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.Orientation;
import org.apache.pdfbox.printing.PDFPageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.file.Paths;

import static org.apache.pdfbox.pdmodel.PDDocument.load;

@Service
public class ReceiptPrinterService {
  private final SaleReceiptService saleReceiptService;
  private final StorageService storageService;

  public ReceiptPrinterService(
      SaleReceiptService saleReceiptService, StorageService storageService) {
    this.saleReceiptService = saleReceiptService;
    this.storageService = storageService;
  }


  public void printCashSale(Long saleId) {
    Runnable runnableTask =
        () -> {
          try (PDDocument document =
              load(Paths.get(saleReceiptService.printCashReceipt(saleId)).toFile())) {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setPrintService(findPrintService());
            printerJob.setPageable(new PDFPageable(document, Orientation.AUTO));
            printerJob.print();
          } catch (IOException e) {
            e.printStackTrace();
          } catch (PrinterException e) {
            e.printStackTrace();
          }
        };
    runnableTask.run();
  }

    public void printVoSale(Long saleId) {
        Runnable runnableTask =
            () -> {
                try (PDDocument document =
                         load(Paths.get(saleReceiptService.printVoReceipt(saleId)).toFile())) {
                    PrinterJob printerJob = PrinterJob.getPrinterJob();
                    printerJob.setPrintService(findPrintService());
                    printerJob.setPageable(new PDFPageable(document, Orientation.AUTO));
                    printerJob.print();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (PrinterException e) {
                    e.printStackTrace();
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
      System.out.print(printService.getName());
      if (printService.getName().equals(printerName)) {

        return printService;
      }
    }
    return PrintServiceLookup.lookupDefaultPrintService();
  }
}
