package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.FileStorageException;
import com.kobe.warehouse.service.report.Constant;
import com.lowagie.text.DocumentException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.Orientation;
import org.apache.pdfbox.printing.PDFPageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public abstract class AbstractReceiptServiceImpl implements ReceiptService {

    protected final Path fileStorageLocation;
    private final Logger LOG = LoggerFactory.getLogger(AbstractReceiptServiceImpl.class);
    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    protected String SIZE_VALUE = "80mm 70mm";

    protected AbstractReceiptServiceImpl(
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        FileStorageProperties fileStorageProperties
    ) {
        this.templateEngine = templateEngine;
        this.storageService = storageService;

        fileStorageLocation = Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    protected abstract Map<String, Object> getParameters();

    protected abstract String getTemplate();

    protected abstract String getPath();

    private Context getContext() {
        Locale locale = Locale.forLanguageTag("fr");
        return new Context(locale);
    }

    protected Context getCommonParameters() {
        Context context = getContext();
        context.setVariable(Constant.MAGASIN, storageService.getUser().getMagasin());
        context.setVariable(Constant.SIZE, SIZE_VALUE);
        context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss")));
        return context;
    }

    private ITextRenderer getITextRenderer() {
        return new ITextRenderer();
    }

    protected String getContent() {
        Context context = this.getCommonParameters();
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process(this.getTemplate(), context);
    }

    private String createPdf() {
        var path = this.getPath();
        try (OutputStream outputStream = new FileOutputStream(path)) {
            ITextRenderer renderer = getITextRenderer();
            SharedContext sharedContext = renderer.getSharedContext();
            sharedContext.getTextRenderer().setSmoothingThreshold(0);
            sharedContext.setPrint(true);
            renderer.setDocumentFromString(this.getContent());
            renderer.layout();
            renderer.createPDF(outputStream);
        } catch (IOException | DocumentException e) {
            LOG.error("createPdf", e);
        }
        return path;
    }

    @Override
    public void print() {
        Runnable runnableTask = () -> {
            try (PDDocument document = Loader.loadPDF(Paths.get(createPdf()).toFile())) {
                PrinterJob printerJob = PrinterJob.getPrinterJob();
                printerJob.setPrintService(findPrintService());
                printerJob.setPageable(new PDFPageable(document, Orientation.AUTO));
                printerJob.print();
            } catch (IOException | PrinterException e) {
                LOG.error("print Receipt : ", e);
            }
        };
        runnableTask.run();
    }

    private PrintService findPrintService() {
        // findPrintService("\\\\192.168.1.104\\HP LaserJet P1007");

        return PrintServiceLookup.lookupDefaultPrintService();
    }
}
