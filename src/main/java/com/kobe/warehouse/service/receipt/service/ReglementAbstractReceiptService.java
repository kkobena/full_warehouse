package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public abstract class ReglementAbstractReceiptService extends AbstractJava2DReceiptPrinterService {

    protected static final String MONTANT_ATTENDU = "Montant attendu";
    protected static final String NOMBRE_DOSSIER = "Nombre de dossiers";
    private static final Logger LOG = LoggerFactory.getLogger(ReglementAbstractReceiptService.class);

    protected ReglementAbstractReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    protected abstract int drawCashInfo(Graphics2D graphics2D, int margin, int y, int lineHeight);

    protected abstract int drawSummary(Graphics2D graphics2D, int width, int margin, int y, int lineHeight);

    protected void print(String hostName) throws PrinterException {
        PrinterJob job = getPrinterJob(hostName);
        PageFormat pageFormat = job.defaultPage();
        int pageWidth = DEFAULT_WIDTH;
        int lineHeight = DEFAULT_LINE_HEIGHT;
        Paper paper = new Paper();
        paper.setSize(pageWidth, pageFormat.getImageableHeight());
        paper.setImageableArea(DEFAULT_MARGIN, lineHeight, pageWidth, pageWidth - lineHeight);
        pageFormat.setPaper(paper);
        job.setPrintable(this, pageFormat);
        try {
            job.setCopies(getNumberOfCopies());
            job.print();
        } catch (PrinterException e) {
            LOG.error("Error printing receipt: {}", e.getMessage());
        }
    }

    @Override
    protected int getNumberOfCopies() {
        return 1;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        int width = DEFAULT_WIDTH; // 80mm in pixels, à parametrer
        int margin = 0; // margin in pixels
        int lineHeight = DEFAULT_LINE_HEIGHT;
        int y = lineHeight;
        y = drawCompagnyInfo(graphics2D, margin, y);
        y = drawWelcomeMessage(graphics2D, margin, y);
        y = drawHeader(graphics2D, margin, y, lineHeight);
        y = drawLineSeparator(graphics2D, margin, y, width);

        y = drawSummary(graphics2D, width, margin, y, lineHeight);
        y = drawReglement(graphics2D, width, margin, y, lineHeight);
        y = drawCashInfo(graphics2D, margin, y, lineHeight);
        y = drawFooter(graphics2D, margin, y, lineHeight);
        y = drawLineSeparator(graphics2D, margin, y, width);
        y = drawDate(graphics2D, margin, y, lineHeight);
        drawThanksMessage(graphics2D, margin, y);

        return PAGE_EXISTS;
    }

    @Override
    protected java.util.List<? extends AbstractItem> getItems() {
        return List.of();
    }
}
