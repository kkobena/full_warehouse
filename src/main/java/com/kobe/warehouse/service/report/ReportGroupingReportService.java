package com.kobe.warehouse.service.report;

import com.kobe.warehouse.config.FileStorageProperties;
import com.lowagie.text.DocumentException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Component
public abstract class ReportGroupingReportService extends CommonReportService {

    protected ReportGroupingReportService(FileStorageProperties fileStorageProperties) {
        super(fileStorageProperties);
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
        } catch (IOException | DocumentException e) {
            log.error("printOneReceiptPage", e);
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
            int pageNumber = (int) Math.ceil(size / (double) maxiRowCount);
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
        } catch (IOException | DocumentException e) {
            log.error("printMultiplesReceiptPage", e);
        }
        return filePath;
    }
}
