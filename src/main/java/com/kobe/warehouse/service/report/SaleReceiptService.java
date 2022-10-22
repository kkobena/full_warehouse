package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.repository.AppConfigurationRepository;
import com.kobe.warehouse.service.SaleDataService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.pdf.BarcodeImageReplacedElementFactory;
import com.kobe.warehouse.web.rest.errors.FileStorageException;
import com.lowagie.text.DocumentException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class SaleReceiptService {
  private final Logger LOG = LoggerFactory.getLogger(SaleReceiptService.class);
  private final SpringTemplateEngine templateEngine;
  private final SaleDataService saleDataService;
  private final StorageService storageService;
  private final Path fileStorageLocation;
  private final FileStorageProperties fileStorageProperties;
  private String SIZE_VALUE = "80mm 80mm";
  private final AppConfigurationRepository appConfigurationRepository;
  private int maxiRowCount = 40;

  public SaleReceiptService(
      SpringTemplateEngine templateEngine,
      SaleDataService saleDataService,
      StorageService storageService,
      FileStorageProperties fileStorageProperties,
      AppConfigurationRepository appConfigurationRepository) {
    this.templateEngine = templateEngine;
    this.saleDataService = saleDataService;
    this.storageService = storageService;
    this.appConfigurationRepository = appConfigurationRepository;
    this.fileStorageProperties = fileStorageProperties;
    this.fileStorageLocation =
        Paths.get(this.fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

    try {
      Files.createDirectories(this.fileStorageLocation);
    } catch (IOException ex) {
      throw new FileStorageException(
          "Could not create the directory where the uploaded files will be stored.", ex);
    }
  }

  private Context getContext() {
    Locale locale = Locale.forLanguageTag("fr");
    return new Context(locale);
  }

  private String getCashSaleTemplate(
      Context context,
      SaleDTO sale,
      List<SaleLineDTO> saleLines,
      boolean isLastPage,
      String pageSize) {
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
    return getContext("receipt/cashSale", context, sale, saleLines, isLastPage);
  }

  private String getVOTemplate(
      Context context,
      SaleDTO sale,
      List<SaleLineDTO> saleLines,
      boolean isLastPage,
      String pageSize) {
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

  private String getContext(
      String template,
      Context context,
      SaleDTO sale,
      List<SaleLineDTO> saleLines,
      boolean isLastPage) {
    context.setVariable(Constant.MAGASIN, storageService.getUser().getMagasin());
    context.setVariable(Constant.SALE, sale);
    context.setVariable(Constant.SIZE, SIZE_VALUE);
    context.setVariable(Constant.SALE_ITEMS, saleLines);
    context.setVariable(Constant.IS_LAST_PAGE, isLastPage);
    context.setVariable("bareCodeData", sale.getNumberTransaction());
    context.setVariable(
        "currentDate",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss")));
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
    } else return null;
  }

  public String printCashReceipt(Long id) {

    CashSaleDTO saleDTO = (CashSaleDTO) saleDataService.getOneSaleDTO(id);
    return buildReceiptFile(saleDTO);
  }

  private String buildReceiptFile(SaleDTO saleDTO) {
    List<SaleLineDTO> saleLines = saleDTO.getSalesLines();
    int thatMaxiRowCount = getMaxiRowCount();
    Context context = getContext();
    int pageNumber = (int) Math.ceil(saleLines.size() / Double.valueOf(thatMaxiRowCount));
    saleLines.sort(Comparator.comparing(SaleLineDTO::getProduitLibelle));
    String destFilePath =
        this.fileStorageLocation
            .resolve(
                saleDTO.getNumberTransaction()
                    + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss"))
                    + ".pdf")
            .toFile()
            .getAbsolutePath();
    if (pageNumber == 1) {

      printOneReceiptPage(
          saleDTO, saleLines, destFilePath, lastPageSize(saleLines.size()), context);
    } else {
      printMultiplesReceiptPage(
          saleDTO, saleLines, destFilePath, pageNumber, thatMaxiRowCount, context);
    }
    return destFilePath;
  }

  private void printOneReceiptPage(
      SaleDTO saleDTO,
      List<SaleLineDTO> saleLines,
      String destFilePath,
      String pageSize,
      Context context) {
    try (OutputStream outputStream = new FileOutputStream(destFilePath)) {
      ITextRenderer renderer = new ITextRenderer();
      SharedContext sharedContext = renderer.getSharedContext();
      sharedContext.setReplacedElementFactory(
          new BarcodeImageReplacedElementFactory().setBarcodeData(saleDTO.getNumberTransaction()));
      sharedContext.getTextRenderer().setSmoothingThreshold(0);
      sharedContext.setPrint(true);
      if (saleDTO instanceof CashSaleDTO) {
        renderer.setDocumentFromString(
            getCashSaleTemplate(context, saleDTO, saleLines, true, pageSize));
      } else if (saleDTO instanceof ThirdPartySaleDTO) {
        renderer.setDocumentFromString(getVOTemplate(context, saleDTO, saleLines, true, pageSize));
      }

      renderer.layout();
      renderer.createPDF(outputStream);
    } catch (FileNotFoundException e) {
      LOG.debug("printOneReceiptPage ===>>", e);
    } catch (IOException | DocumentException e) {
      LOG.debug("printOneReceiptPage ===>>", e);
    }
  }

  private void printMultiplesReceiptPage(
      SaleDTO saleDTO,
      List<SaleLineDTO> saleLines,
      String destFilePath,
      int pageCount,
      int maxiRowCount,
      Context context) {

    try (OutputStream outputStream = new FileOutputStream(destFilePath)) {
      ITextRenderer renderer = new ITextRenderer();
      SharedContext sharedContext = renderer.getSharedContext();
      sharedContext.setReplacedElementFactory(
          new BarcodeImageReplacedElementFactory().setBarcodeData(saleDTO.getNumberTransaction()));
      sharedContext.getTextRenderer().setSmoothingThreshold(0);
      sharedContext.setPrint(true);
      if (saleDTO instanceof CashSaleDTO) {
        getCashSaleTemplate(context, saleDTO, saleLines.subList(0, maxiRowCount), false, null);
      } else if (saleDTO instanceof ThirdPartySaleDTO) {
        getVOTemplate(context, saleDTO, saleLines.subList(0, maxiRowCount), false, null);
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
        if (saleDTO instanceof CashSaleDTO) {
          renderer.setDocumentFromString(
              getCashSaleTemplate(
                  context,
                  saleDTO,
                  list,
                  isLastPage,
                  isLastPage ? lastPageSize(list.size()) : null));
        } else if (saleDTO instanceof ThirdPartySaleDTO) {
          renderer.setDocumentFromString(
              getVOTemplate(
                  context,
                  saleDTO,
                  list,
                  isLastPage,
                  isLastPage ? lastPageSize(list.size()) : null));
        }

        renderer.layout();
        renderer.writeNextDocument();
        firstPageRowCount += maxiRowCount;
      }
      renderer.finishPDF();
    } catch (FileNotFoundException e) {
      LOG.debug("printMultiplesReceiptPage ===>>", e);
    } catch (IOException | DocumentException | IndexOutOfBoundsException e) {
      LOG.debug("printMultiplesReceiptPage ===>>", e);
    }
  }

  public int getMaxiRowCount() {
    this.appConfigurationRepository
        .findById(EntityConstant.RECEIPT_MAXI_ROW)
        .ifPresent(
            appConfiguration -> this.maxiRowCount = Integer.valueOf(appConfiguration.getValue()));
    return this.maxiRowCount;
  }

  public String printVoReceipt(Long id) {

    ThirdPartySaleDTO saleDTO = (ThirdPartySaleDTO) saleDataService.getOneSaleDTO(id);
    return buildReceiptFile(saleDTO);
  }
}
