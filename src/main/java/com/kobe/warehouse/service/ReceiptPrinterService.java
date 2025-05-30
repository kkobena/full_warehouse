package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.service.receipt.service.AssuranceSaleReceiptService;
import com.kobe.warehouse.service.receipt.service.CashSaleReceiptService;
import com.kobe.warehouse.service.report.SaleReceiptService;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReceiptPrinterService {

    private final SaleReceiptService saleReceiptService;
    private final StorageService storageService;
    private final Logger log = LoggerFactory.getLogger(ReceiptPrinterService.class);
    private final CashSaleReceiptService cashSaleReceiptService;
    private final AssuranceSaleReceiptService assuranceSaleReceiptService;

    public ReceiptPrinterService(
        SaleReceiptService saleReceiptService,
        StorageService storageService,
        CashSaleReceiptService cashSaleReceiptService,
        AssuranceSaleReceiptService assuranceSaleReceiptService
    ) {
        this.saleReceiptService = saleReceiptService;
        this.storageService = storageService;
        this.cashSaleReceiptService = cashSaleReceiptService;
        this.assuranceSaleReceiptService = assuranceSaleReceiptService;
    }

    public void printCashSale(Long saleId, boolean isEdit) {
        this.cashSaleReceiptService.printReceipt(null, saleId, isEdit);
        /* Runnable runnableTask = () -> {
            try (PDDocument document = Loader.loadPDF(Paths.get(saleReceiptService.printCashReceipt(saleId)).toFile())) {
                PrinterJob printerJob = PrinterJob.getPrinterJob();
                printerJob.setPrintService(findPrintService());
                printerJob.setPageable(new PDFPageable(document, Orientation.AUTO));
                printerJob.print();
            } catch (IOException | PrinterException e) {
                log.error("printCashSale : {}", e.getLocalizedMessage());
            }
        };
        runnableTask.run();*/
    }

    public void printVoSale(Long saleId, boolean isEdit) {
        this.assuranceSaleReceiptService.printReceipt(null, saleId, isEdit);
        /*   Runnable runnableTask = () -> {
            try (PDDocument document = Loader.loadPDF(Paths.get(saleReceiptService.printVoReceipt(saleId)).toFile())) {
                PrinterJob printerJob = PrinterJob.getPrinterJob();
                printerJob.setPrintService(findPrintService());
                printerJob.setPageable(new PDFPageable(document, Orientation.AUTO));
                printerJob.print();
            } catch (IOException | PrinterException e) {
                log.debug("printVoSale : {}", e.getLocalizedMessage());
            }
        };
        runnableTask.run();*/
    }

    private PrintService findPrintService() {
        // findPrintService("\\\\192.168.1.104\\HP LaserJet P1007");
        User user = storageService.getUser();
        String printerName = null;

        return PrintServiceLookup.lookupDefaultPrintService();
    }

    /*
    common size
  80*80
  58
  Width: 2.25 inches (57 mm), 3 inches (76 mm)
  Width: 2.25 inches
     */

    public void printCashSale2(Long saleId, boolean isEdit) {
        this.assuranceSaleReceiptService.printReceipt(null, saleId, isEdit);
    }
}
