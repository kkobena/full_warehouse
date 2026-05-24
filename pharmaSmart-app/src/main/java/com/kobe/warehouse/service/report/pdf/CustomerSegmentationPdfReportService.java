package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.report.CustomerSegmentationDTO;
import com.kobe.warehouse.service.report.CustomerSegmentationReportService;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerSegmentationPdfReportService extends AbstractStatistiqueReportService {
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();
    private final CustomerSegmentationReportService customerSegmentationReportService;

    public CustomerSegmentationPdfReportService(FileStorageProperties fileStorageProperties, StorageService storageService, SpringTemplateEngine templateEngine, CustomerSegmentationReportService customerSegmentationReportService) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.customerSegmentationReportService = customerSegmentationReportService;
    }


    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process("reports/customer-segmentation/main", super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process("reports/customer-segmentation/main", context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "rapport_segmentation_client";
    }

    public byte[] export() {

        List<CustomerSegmentationDTO> segmentations = customerSegmentationReportService.getAllCustomerSegmentation();
        Map<CustomerSegmentationDTO.CustomerClassification, Long> counts = customerSegmentationReportService.getCustomerCountByClassification();


        this.getParameters().put("segmentations", segmentations);
        this.getParameters().put(
            "championCount",
            counts.getOrDefault(CustomerSegmentationDTO.CustomerClassification.CHAMPION, 0L)
        );
        this.getParameters().put("loyalCount", counts.getOrDefault(CustomerSegmentationDTO.CustomerClassification.LOYAL, 0L));
        this.getParameters().put(
            "bigSpenderCount",
            counts.getOrDefault(CustomerSegmentationDTO.CustomerClassification.BIG_SPENDER, 0L)
        );
        this.getParameters().put(
            "activeCount",
            counts.getOrDefault(CustomerSegmentationDTO.CustomerClassification.ACTIVE, 0L)
        );
        this.getParameters().put(
            "atRiskCount",
            counts.getOrDefault(CustomerSegmentationDTO.CustomerClassification.AT_RISK, 0L)
        );
        this.getParameters().put(
            "needAttentionCount",
            counts.getOrDefault(CustomerSegmentationDTO.CustomerClassification.NEED_ATTENTION, 0L)
        );
        this.getParameters().put(
            "inactiveCount",
            counts.getOrDefault(CustomerSegmentationDTO.CustomerClassification.INACTIVE, 0L)
        );
        this.getParameters().put("reportTitle", "Rapport de Segmentation Client (RFM)");
        this.getParameters().put("page_count", "1/1");


        super.getCommonParameters();
        return super.exportReportToPdf();
    }
}
