package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.repository.AppConfigurationRepository;
import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.errors.FileStorageException;
import com.kobe.warehouse.service.pdf.BarcodeImageReplacedElementFactory;
import com.kobe.warehouse.service.receipt.service.AssuranceSaleReceiptService;
import com.kobe.warehouse.service.receipt.service.CashSaleReceiptService;
import com.kobe.warehouse.service.sale.SaleDataService;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.openpdf.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
@Transactional(readOnly = true)
public class SaleReceiptService {

    private final Logger LOG = LoggerFactory.getLogger(SaleReceiptService.class);
    private final SpringTemplateEngine templateEngine;
    private final SaleDataService saleDataService;
    private final StorageService storageService;
    private final Path fileStorageLocation;
    private final AppConfigurationRepository appConfigurationRepository;
    private final AppConfigurationService appConfigurationService;
    private final PrinterRepository printerRepository;
    private String SIZE_VALUE = "80mm 80mm";
    private int maxiRowCount = 40;

    public SaleReceiptService(
        SpringTemplateEngine templateEngine,
        SaleDataService saleDataService,
        StorageService storageService,
        FileStorageProperties fileStorageProperties,
        AppConfigurationRepository appConfigurationRepository,
        AppConfigurationService appConfigurationService,
        PrinterRepository printerRepository
    ) {
        this.templateEngine = templateEngine;
        this.saleDataService = saleDataService;
        this.storageService = storageService;
        this.appConfigurationRepository = appConfigurationRepository;
        this.appConfigurationService = appConfigurationService;
        this.printerRepository = printerRepository;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    private Context getContext() {
        Locale locale = Locale.forLanguageTag("fr");
        return new Context(locale);
    }

    private String getCashSaleTemplate(
        Context context,
        CashSaleDTO sale,
        List<SaleLineDTO> saleLines,
        boolean isLastPage,
        String pageSize
    ) {
        if (!org.springframework.util.StringUtils.hasLength(pageSize)) {
            int itemSize = saleLines.size();
            if (itemSize < 5) {
                SIZE_VALUE = "80mm 90mm";
            } else if (itemSize >= 5 && itemSize <= 7) {
                SIZE_VALUE = "80mm 100mm";
            } else if (itemSize > 7 && itemSize <= 15) {
                SIZE_VALUE = "80mm 120mm";
            } else if (itemSize > 15 && itemSize <= 20) {
                SIZE_VALUE = "80mm 130mm";
            } else if (itemSize > 20 && itemSize <= 35) {
                SIZE_VALUE = "80mm 150mm";
            } else if (itemSize > 35 && itemSize <= 40) {
                SIZE_VALUE = "80mm 160mm";
            }
        } else {
            SIZE_VALUE = pageSize;
        }
        return getContext("receipt/cashSale", context, sale, saleLines, isLastPage);
    }

    private String getVOTemplate(
        Context context,
        ThirdPartySaleDTO sale,
        List<SaleLineDTO> saleLines,
        boolean isLastPage,
        String pageSize
    ) {
        if (StringUtils.isEmpty(pageSize)) {
            int itemSize = saleLines.size();
            if (itemSize < 5) {
                SIZE_VALUE = "80mm 90mm";
            } else if (itemSize >= 5 && itemSize <= 7) {
                SIZE_VALUE = "80mm 100mm";
            } else if (itemSize > 7 && itemSize <= 15) {
                SIZE_VALUE = "80mm 120mm";
            } else if (itemSize > 15 && itemSize <= 20) {
                SIZE_VALUE = "80mm 130mm";
            } else if (itemSize > 20 && itemSize <= 35) {
                SIZE_VALUE = "80mm 150mm";
            } else if (itemSize > 35 && itemSize <= 40) {
                SIZE_VALUE = "80mm 160mm";
            }
        } else {
            SIZE_VALUE = pageSize;
        }

        return getContext("receipt/vo", context, sale, saleLines, isLastPage);
    }

    private String getContext(String template, Context context, SaleDTO sale, List<SaleLineDTO> saleLines, boolean isLastPage) {
        context.setVariable(Constant.MAGASIN, storageService.getUser().getMagasin());
        context.setVariable(Constant.SALE, sale);
        context.setVariable(Constant.SIZE, SIZE_VALUE);
        context.setVariable(Constant.SALE_ITEMS, saleLines);
        context.setVariable(Constant.IS_LAST_PAGE, isLastPage);
        context.setVariable("bareCodeData", sale.getNumberTransaction());
        context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss")));
        return templateEngine.process(template, context);
    }

    private String lastPageSize(int itemSize) {
        if (itemSize > 15 && itemSize <= 20) {
            return "80mm 140mm";
        }
        if (itemSize > 20 && itemSize <= 25) {
            return "80mm 155mm";
        } else if (itemSize > 25 && itemSize <= 30) {
            return "80mm 160mm";
        } else if (itemSize > 30 && itemSize <= 35) {
            return "80mm 180mm";
        } else if (itemSize > 35 && itemSize <= 40) {
            return "80mm 200mm";
        } else {
            return null;
        }
    }

    public String printCashReceipt(SaleId id) {
        CashSaleDTO saleDTO = (CashSaleDTO) saleDataService.getOneSaleDTO(id);
        return buildReceiptFile(saleDTO);
    }

    private String buildReceiptFile(SaleDTO saleDTO) {
        List<SaleLineDTO> saleLines = new ArrayList<>(saleDTO.getSalesLines());
        int thatMaxiRowCount = getMaxiRowCount();
        Context context = getContext();
        int pageNumber = (int) Math.ceil(saleLines.size() / (double) thatMaxiRowCount);
        saleLines.sort(Comparator.comparing(SaleLineDTO::getProduitLibelle));
        String destFilePath =
            this.fileStorageLocation.resolve(
                    saleDTO.getNumberTransaction() +
                    "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) +
                    ".pdf"
                )
                .toFile()
                .getAbsolutePath();

        if (pageNumber == 1) {
            printOneReceiptPage(saleDTO, saleLines, destFilePath, lastPageSize(saleLines.size()), context);
        } else {
            printMultiplesReceiptPage(saleDTO, saleLines, destFilePath, pageNumber, thatMaxiRowCount, context);
        }
        return destFilePath;
    }

    private void printOneReceiptPage(SaleDTO saleDTO, List<SaleLineDTO> saleLines, String destFilePath, String pageSize, Context context) {
        try (OutputStream outputStream = new FileOutputStream(destFilePath)) {
            ITextRenderer renderer = new ITextRenderer();
            SharedContext sharedContext = renderer.getSharedContext();
            sharedContext.setReplacedElementFactory(
                new BarcodeImageReplacedElementFactory().setBarcodeData(saleDTO.getNumberTransaction())
            );
            sharedContext.getTextRenderer().setSmoothingThreshold(0);
            sharedContext.setPrint(true);
            if (saleDTO instanceof CashSaleDTO vno) {
                renderer.setDocumentFromString(getCashSaleTemplate(context, vno, saleLines, true, pageSize));
            } else if (saleDTO instanceof ThirdPartySaleDTO vo) {
                renderer.setDocumentFromString(getVOTemplate(context, vo, saleLines, true, pageSize));
            }

            renderer.layout();
            renderer.createPDF(outputStream);
        } catch (IOException | DocumentException e) {
            LOG.error("printOneReceiptPage ===>>", e);
        }
    }

    private void printMultiplesReceiptPage(
        SaleDTO saleDTO,
        List<SaleLineDTO> saleLines,
        String destFilePath,
        int pageCount,
        int maxiRowCount,
        Context context
    ) {
        try (OutputStream outputStream = new FileOutputStream(destFilePath)) {
            ITextRenderer renderer = new ITextRenderer();
            SharedContext sharedContext = renderer.getSharedContext();
            sharedContext.setReplacedElementFactory(
                new BarcodeImageReplacedElementFactory().setBarcodeData(saleDTO.getNumberTransaction())
            );
            sharedContext.getTextRenderer().setSmoothingThreshold(0);
            sharedContext.setPrint(true);
            if (saleDTO instanceof CashSaleDTO vno) {
                getCashSaleTemplate(context, vno, saleLines.subList(0, maxiRowCount), false, null);
            } else if (saleDTO instanceof ThirdPartySaleDTO vo) {
                getVOTemplate(context, vo, saleLines.subList(0, maxiRowCount), false, null);
            }
            renderer.layout();
            renderer.createPDF(outputStream, false);
            int firstPageRowCount = maxiRowCount;
            int size = saleLines.size();
            for (int i = 1; i < pageCount; i++) {
                int toIndex = maxiRowCount + firstPageRowCount;
                if (toIndex > size) {
                    toIndex = size;
                }
                List<SaleLineDTO> list = saleLines.subList(firstPageRowCount, toIndex);
                boolean isLastPage = toIndex == size;
                if (saleDTO instanceof CashSaleDTO vno) {
                    renderer.setDocumentFromString(
                        getCashSaleTemplate(context, vno, list, isLastPage, isLastPage ? lastPageSize(list.size()) : null)
                    );
                } else if (saleDTO instanceof ThirdPartySaleDTO vo) {
                    renderer.setDocumentFromString(
                        getVOTemplate(context, vo, list, isLastPage, isLastPage ? lastPageSize(list.size()) : null)
                    );
                }

                renderer.layout();
                renderer.writeNextDocument();
                firstPageRowCount += maxiRowCount;
            }
            renderer.finishPDF();
        } catch (IOException | DocumentException | IndexOutOfBoundsException e) {
            LOG.debug("printMultiplesReceiptPage ===>>", e);
        }
    }

    public int getMaxiRowCount() {
        this.appConfigurationRepository.findById(EntityConstant.RECEIPT_MAXI_ROW).ifPresent(appConfiguration ->
                this.maxiRowCount = Integer.parseInt(appConfiguration.getValue())
            );
        return this.maxiRowCount;
    }

    /**
     * Generate cash sale receipt as byte arrays for Tauri clients
     * <p>
     * This method creates a list of PNG images (one per page) as byte arrays
     * that can be sent to Tauri clients for printing on remote machines
     *
     * @param id the sale id and date
     * @return list of byte arrays representing receipt pages as PNG images
     * @throws IOException if image generation fails
     */
    public List<byte[]> generateTicketForTauri(SaleId id) throws IOException {
        CashSaleDTO saleDTO = (CashSaleDTO) saleDataService.getOneSaleDTO(id);
        CashSaleReceiptService receiptService = new CashSaleReceiptService(
            appConfigurationService,
            printerRepository
        );
        return receiptService.generateTicketForTauri(saleDTO);
    }

    /**
     * Generate assurance/third-party sale receipt as byte arrays for Tauri clients
     * <p>
     * This method creates a list of PNG images (one per page) as byte arrays
     * that can be sent to Tauri clients for printing on remote machines
     *
     * @param id the sale id and date
     * @return list of byte arrays representing receipt pages as PNG images
     * @throws IOException if image generation fails
     */
    public List<byte[]> generateVoTicketForTauri(SaleId id) throws IOException {
        ThirdPartySaleDTO saleDTO = (ThirdPartySaleDTO) saleDataService.getOneSaleDTO(id);
        AssuranceSaleReceiptService receiptService = new AssuranceSaleReceiptService(
            appConfigurationService,
            printerRepository
        );
        return receiptService.generateTicketForTauri(saleDTO);
    }

    public List<byte[]> generateTicketForTauri(SaleId id, boolean isEdit) throws IOException {
        SaleDTO saleDTO = saleDataService.getOneSaleDTO(id);
        if (saleDTO instanceof CashSaleDTO cashSaleDTO) {
            CashSaleReceiptService receiptService = new CashSaleReceiptService(
                appConfigurationService,
                printerRepository
            );
            return receiptService.generateTicketForTauri(cashSaleDTO);
        } else if (saleDTO instanceof ThirdPartySaleDTO thirdPartySaleDTO) {
            AssuranceSaleReceiptService receiptService = new AssuranceSaleReceiptService(
                appConfigurationService,
                printerRepository
            );
            return receiptService.generateTicketForTauri(thirdPartySaleDTO);
        }
        throw new IllegalArgumentException("Unsupported sale type: " + saleDTO.getClass().getName());
    }

    public byte[] generateEscPosReceipt(SaleId id, boolean isEdit) throws IOException {
        SaleDTO saleDTO = saleDataService.getOneSaleDTO(id);
        if (saleDTO instanceof CashSaleDTO cashSaleDTO) {
            CashSaleReceiptService receiptService = new CashSaleReceiptService(
                appConfigurationService,
                printerRepository
            );
            return receiptService.generateEscPosReceiptForTauri(cashSaleDTO, isEdit);
        } else if (saleDTO instanceof ThirdPartySaleDTO thirdPartySaleDTO) {
            AssuranceSaleReceiptService receiptService = new AssuranceSaleReceiptService(
                appConfigurationService,
                printerRepository
            );
            return receiptService.generateEscPosReceiptForTauri(thirdPartySaleDTO, isEdit);
        }
        throw new IllegalArgumentException("Unsupported sale type: " + saleDTO.getClass().getName());
    }
}
