package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.receipt.service.AssuranceSaleReceiptService;
import com.kobe.warehouse.service.receipt.service.CashSaleReceiptService;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import org.springframework.stereotype.Service;

@Service
public class ReceiptPrinterService {

    private final StorageService storageService;
    private final CashSaleReceiptService cashSaleReceiptService;
    private final AssuranceSaleReceiptService assuranceSaleReceiptService;

    public ReceiptPrinterService(
        StorageService storageService,
        CashSaleReceiptService cashSaleReceiptService,
        AssuranceSaleReceiptService assuranceSaleReceiptService
    ) {
        this.storageService = storageService;
        this.cashSaleReceiptService = cashSaleReceiptService;
        this.assuranceSaleReceiptService = assuranceSaleReceiptService;
    }

    public void printCashSale(CashSaleDTO sale, boolean isEdit) {
        this.cashSaleReceiptService.printReceipt(null, sale, isEdit);
    }

    public void printVoSale(ThirdPartySaleDTO thirdPartySale, boolean isEdit) {
        this.assuranceSaleReceiptService.printReceipt(null, thirdPartySale, isEdit);
    }

    private PrintService findPrintService() {
        // findPrintService("\\\\192.168.1.104\\HP LaserJet P1007");
        AppUser user = storageService.getUser();
        String printerName = null;

        return PrintServiceLookup.lookupDefaultPrintService();
    }
}
