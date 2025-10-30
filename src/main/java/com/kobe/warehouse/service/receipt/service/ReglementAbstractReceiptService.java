package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Abstract base class for payment receipt services (Invoice, Differe, etc.)
 * This class provides the Graphics2D-based printing implementation.
 * Subclasses must implement generateEscPosReceipt() for direct thermal printing.
 */

@Service
public abstract class ReglementAbstractReceiptService extends AbstractJava2DReceiptPrinterService {

    protected static final String MONTANT_ATTENDU = "MONTANT ATTENDU";
    protected static final String NOMBRE_DOSSIER = "NOMBRE DE DOSSIERS";

    protected ReglementAbstractReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }


    @Override
    protected int getNumberOfCopies() {
        return 1;
    }


    @Override
    protected List<? extends AbstractItem> getItems() {
        return List.of();
    }
}
