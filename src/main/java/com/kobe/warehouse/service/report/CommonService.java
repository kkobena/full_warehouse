package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.web.rest.errors.FileStorageException;
import com.lowagie.text.DocumentException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Component
public abstract class CommonService {
  protected final Logger log = LoggerFactory.getLogger(CommonService.class);
  private final FileStorageProperties fileStorageProperties;

  protected CommonService(FileStorageProperties fileStorageProperties) {
    this.fileStorageProperties = fileStorageProperties;
  }

  protected abstract List<?> getItems();

  protected String getDestFilePath() {
    Path fileStorageLocation =
        Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

    try {
      Files.createDirectories(fileStorageLocation);
    } catch (IOException ex) {
      throw new FileStorageException(
          "Could not create the directory where the uploaded files will be stored.", ex);
    }

    return fileStorageLocation
        .resolve(
            this.getGenerateFileName()
                + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss"))
                + ".pdf")
        .toFile()
        .getAbsolutePath();
  }

  protected abstract int getMaxiRowCount();

  protected abstract String getTemplateAsHtml();

  protected abstract String getTemplateAsHtml(Context context);

  protected abstract Map<String, Object> getParameters();

  protected abstract String getGenerateFileName();

  protected Context getContextVariables() {
    Context context = getContext();
    this.getParameters().forEach(context::setVariable);
    return context;
  }

  public ITextRenderer getITextRenderer() {
    return new ITextRenderer();
  }

  public SharedContext getSharedContext(ITextRenderer renderer) {
    return renderer.getSharedContext();
  }

  public String printOneReceiptPage() {
    String filePath = getDestFilePath();
    try (OutputStream outputStream = new FileOutputStream(filePath)) {
      ITextRenderer renderer = this.getITextRenderer();
      SharedContext sharedContext = this.getSharedContext(renderer);
      sharedContext.setPrint(true);
      renderer.setDocumentFromString(this.getTemplateAsHtml());
      renderer.layout();
      renderer.createPDF(outputStream);
    } catch (FileNotFoundException e) {
      log.debug("printOneReceiptPage", e);
    } catch (IOException | DocumentException e) {
      log.debug("printOneReceiptPage", e);
    }
    return filePath;
  }

  public String printMultiplesReceiptPage() {
    String filePath = getDestFilePath();

    try (OutputStream outputStream = new FileOutputStream(filePath)) {
      ITextRenderer renderer = this.getITextRenderer();
      SharedContext sharedContext = this.getSharedContext(renderer);
      Context context = getContext();
      sharedContext.setPrint(true);

      int maxiRowCount = this.getMaxiRowCount();
      int firstPageRowCount = maxiRowCount;
      List<?> items = this.getItems();
      int size = items.size();
      int pageNumber = (int) Math.ceil(size / Double.valueOf(maxiRowCount));
      getParameters().put(Constant.PAGE_COUNT, "1/" + pageNumber);
      renderer.setDocumentFromString(this.getTemplateAsHtml(context));
      renderer.layout();
      renderer.createPDF(outputStream, false);

      for (int i = 1; i < pageNumber; i++) {
        int toIndex = maxiRowCount + firstPageRowCount;
        if (toIndex > size) {
          toIndex = size;
        }
        List<?> list = items.subList(firstPageRowCount, toIndex);
        boolean isLastPage = toIndex == size;
        getParameters().put(Constant.IS_LAST_PAGE, isLastPage);
        getParameters().put(Constant.COMMANDE_ITEMS, list);
        getParameters().put(Constant.ITEM_SIZE, size);
        getParameters().put(Constant.PAGE_COUNT, (i + 1) + "/" + pageNumber);
        renderer.setDocumentFromString(this.getTemplateAsHtml(context));

        renderer.layout();
        renderer.writeNextDocument();
        firstPageRowCount += maxiRowCount;
      }
      renderer.finishPDF();

    } catch (FileNotFoundException e) {
      log.debug("printMultiplesReceiptPage", e);
    } catch (IOException | DocumentException e) {
      log.debug("printMultiplesReceiptPage", e);
    }
    return filePath;
  }

  public StringBuilder builderFooter(Magasin magasin) {
    StringBuilder builder = new StringBuilder();
    if (StringUtils.isNotEmpty(magasin.getRegistre())) {
      builder.append("RC N° ").append(magasin.getRegistre());
    }
    if (StringUtils.isNotEmpty(magasin.getCompteContribuable())) {
      builder.append(" - CC N° ").append(magasin.getCompteContribuable());
    }
    if (StringUtils.isNotEmpty(magasin.getNumComptable())) {
      builder.append(" - CPT N°: ").append(magasin.getNumComptable());
    }
    if (StringUtils.isNotEmpty(magasin.getPhone())) {
      builder.append("- Tel: ").append(magasin.getPhone());
    }
    if (StringUtils.isNotEmpty(magasin.getAddress())) {
      builder.append("- Adr: ").append(magasin.getAddress());
    }
    return builder;
  }

  protected Context getContext() {
    Locale locale = Locale.forLanguageTag("fr");
    return new Context(locale);
  }
}
