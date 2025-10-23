package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.receipt.service.AssuranceSaleReceiptService;
import com.kobe.warehouse.service.receipt.service.CashSaleReceiptService;
import org.springframework.stereotype.Service;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.IOException;

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

    public byte[] generateEscPosReceipt(CashSaleDTO sale, boolean isEdit) throws IOException {
        return this.cashSaleReceiptService.generateEscPosReceiptForTauri(sale, isEdit);
    }

    public byte[] generateEscPosReceipt(ThirdPartySaleDTO thirdPartySale, boolean isEdit) throws IOException {
        return this.assuranceSaleReceiptService.generateEscPosReceiptForTauri(thirdPartySale, isEdit);
    }
}
