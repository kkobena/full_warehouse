package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Abstract base class for payment receipt services (Invoice, Differe, etc.)
 * This class provides the Graphics2D-based printing implementation.
 * Subclasses must implement generateEscPosReceipt() for direct thermal printing.
 */

@Service
public abstract class ReglementAbstractReceiptService extends AbstractJava2DReceiptPrinterService {

    protected static final String MONTANT_ATTENDU = "MONTANT ATTENDU";
    protected static final String NOMBRE_DOSSIER = "NOMBRE DE DOSSIERS";

    protected ReglementAbstractReceiptService(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
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
