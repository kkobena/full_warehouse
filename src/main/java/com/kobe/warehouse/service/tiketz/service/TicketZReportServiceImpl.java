package com.kobe.warehouse.service.tiketz.service;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.service.MailService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.report.CommonReportService;
import com.kobe.warehouse.service.report.Constant;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.ListUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class TicketZReportServiceImpl extends CommonReportService implements TicketZReportService {

    private final MailService mailService;
    private final SpringTemplateEngine templateEngine;
    private final Map<String, Object> variablesMap = new HashMap<>();

    public TicketZReportServiceImpl(
        FileStorageProperties fileStorageProperties,
        SpringTemplateEngine templateEngine,
        StorageService storageService,
        MailService mailService
    ) {
        super(fileStorageProperties, storageService);
        this.templateEngine = templateEngine;
        this.mailService = mailService;
    }

    @Override
    protected List<?> getItems() {
        return List.of();
    }

    @Override
    protected int getMaxiRowCount() {
        return 0;
    }

    @Override
    protected String getTemplateAsHtml() {
        return templateEngine.process(Constant.TICKET_Z_TEMPLATE_FILE, super.getContextVariables());
    }

    @Override
    protected String getTemplateAsHtml(Context context) {
        this.getParameters().forEach(context::setVariable);
        return templateEngine.process(Constant.TICKET_Z_TEMPLATE_FILE, context);
    }

    @Override
    protected Map<String, Object> getParameters() {
        return this.variablesMap;
    }

    @Override
    protected String getGenerateFileName() {
        return "ticket_z_";
    }

    @Override
    public ResponseEntity<byte[]> generatePdf(TicketZ ticket, Pair periode) {
        buildContext(ticket, periode);
        return super.genererPdf();
    }

    private void buildContext(TicketZ ticket, Pair periode) {
        var items = ListUtils.partition(ticket.datas(), 3);
        this.getParameters().put(Constant.ITEMS, items);
        this.getParameters().put(Constant.TABLE_WIDTH, 100 / items.size());
        this.getParameters().put(Constant.REPORT_SUMMARY, ticket.summaries());

        super.buildTitle("RECAPITULATIF DE CAISSE DU ", periode);
        super.getCommonParameters();
    }

    @Override
    public void sentToEmail(TicketZ ticket, Pair periode) {
        buildContext(ticket, periode);
        this.mailService.sendTicketZ(this.getTemplateAsHtml());
    }
}
