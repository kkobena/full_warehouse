package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.domain.ScheduledReport;
import com.kobe.warehouse.domain.enumeration.ScheduledReportFrequency;
import com.kobe.warehouse.repository.ScheduledReportRepository;
import com.kobe.warehouse.service.MailService;
import com.kobe.warehouse.service.report.TiersPayantReportService;
import com.kobe.warehouse.service.report.pdf.ComparativePdfReportService;
import com.kobe.warehouse.service.report.pdf.DashboardCAPdfExportService;
import com.kobe.warehouse.service.report.pdf.StockAlertPdfReportService;
import com.kobe.warehouse.service.report.pdf.TiersPayantPdfReportService;
import jakarta.mail.MessagingException;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

/**
 * Service for executing scheduled reports
 */
@Service
@Transactional
public class ScheduledReportService {

    private final Logger log = LoggerFactory.getLogger(ScheduledReportService.class);

    private final ScheduledReportRepository scheduledReportRepository;
    private final MailService mailService;
    private final DashboardCAPdfExportService dashboardCAPdfExportService;
    private final StockAlertPdfReportService stockAlertPdfReportService;
    private final TiersPayantPdfReportService tiersPayantPdfReportService;
    private final ComparativePdfReportService comparativePdfReportService;

    public ScheduledReportService(
        ScheduledReportRepository scheduledReportRepository,
        MailService mailService,
        DashboardCAPdfExportService dashboardCAPdfExportService, StockAlertPdfReportService stockAlertPdfReportService,
        TiersPayantPdfReportService tiersPayantPdfReportService,
        ComparativePdfReportService comparativePdfReportService
    ) {
        this.scheduledReportRepository = scheduledReportRepository;
        this.mailService = mailService;
        this.dashboardCAPdfExportService = dashboardCAPdfExportService;
        this.stockAlertPdfReportService = stockAlertPdfReportService;
        this.tiersPayantPdfReportService = tiersPayantPdfReportService;
        this.comparativePdfReportService = comparativePdfReportService;
    }

    /**
     * Execute scheduled reports every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at :00
    public void executeScheduledReports() {
        log.info("Checking for due scheduled reports...");
        LocalDateTime now = LocalDateTime.now();

        List<ScheduledReport> dueReports = scheduledReportRepository.findDueReports(now);
        log.info("Found {} due reports", dueReports.size());

        for (ScheduledReport report : dueReports) {
            try {
                executeReport(report);
                updateNextExecution(report);
                scheduledReportRepository.save(report);
            } catch (Exception e) {
                log.error("Error executing scheduled report {}: {}", report.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Execute a single report
     */
    public void executeReport(ScheduledReport scheduledReport) throws Exception {
        log.info("Executing scheduled report: {} ({})", scheduledReport.getReportName(), scheduledReport.getReportType());

        byte[] pdfData = null;
        byte[] excelData = null;

        // Get report date range (last period based on frequency)
        LocalDate[] dateRange = getReportDateRange(scheduledReport.getFrequency());
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];

        // Generate report based on type
        switch (scheduledReport.getReportType()) {
            case DASHBOARD_CA:
                if (scheduledReport.isIncludePdf()) {
                    pdfData = dashboardCAPdfExportService.export(startDate, endDate);
                }
                break;

            case STOCK_ALERTS:
                if (scheduledReport.isIncludePdf()) {
                    pdfData = stockAlertPdfReportService.export(null);
                }
                break;

            case TIERS_PAYANT_CREANCES:
                if (scheduledReport.isIncludePdf()) {
                    pdfData = tiersPayantPdfReportService.export();
                }
                break;

            case COMPARATIVE_ANALYSIS:
                if (scheduledReport.isIncludePdf()) {
                    pdfData = comparativePdfReportService.export("MONTHLY", LocalDate.now().getYear());
                }
                break;

            default:
                log.warn("Report type {} not yet implemented for scheduling", scheduledReport.getReportType());
                return;
        }

        // Send email with attachments
        sendReportEmail(scheduledReport, pdfData, excelData, startDate, endDate);

        // Update last execution
        scheduledReport.setLastExecution(LocalDateTime.now());
    }

    /**
     * Send report via email
     */
    private void sendReportEmail(
        ScheduledReport scheduledReport,
        byte[] pdfData,
        byte[] excelData,
        LocalDate startDate,
        LocalDate endDate
    ) throws MessagingException {
        Set<String> recipients = scheduledReport.getEmailRecipients();

        String subject = String.format(
            "Rapport Automatique: %s (%s - %s)",
            scheduledReport.getReportName(),
            startDate,
            endDate
        );

        String content = String.format(
            "<p>Bonjour,</p>" +
                "<p>Veuillez trouver ci-joint le rapport automatique <strong>%s</strong>.</p>" +
                "<p>Période: Du %s au %s</p>" +
                "<p>Fréquence: %s</p>" +
                "<br/>" +
                "<p><em>Ce rapport a été généré automatiquement par Pharma-Smart.</em></p>",
            scheduledReport.getReportName(),
            startDate,
            endDate,
            scheduledReport.getFrequency()
        );

        for (String recipient : recipients) {
            try {
                // Create email with attachments


                // Add PDF attachment if present
                if (pdfData != null) {
                    ByteArrayDataSource pdfSource = new ByteArrayDataSource(pdfData, "application/pdf");
                    String pdfFilename = String.format(
                        "%s_%s_%s.pdf",
                        scheduledReport.getReportType(),
                        startDate,
                        endDate
                    );
                    // Note: MailService needs to be enhanced to support attachments
                    // For now, this is a placeholder
                }
                if (excelData != null) {
                    ByteArrayDataSource excelSource = new ByteArrayDataSource(pdfData, "application/pdf");
                    String pdfFilename = String.format(
                        "%s_%s_%s.pdf",
                        scheduledReport.getReportType(),
                        startDate,
                        endDate
                    );
                    // Note: MailService needs to be enhanced to support attachments
                    // For now, this is a placeholder
                }
                mailService.sendEmail(recipient.trim(), subject, content, false, true);
                log.info("Report email sent to {}", recipient);
            } catch (Exception e) {
                log.error("Error sending report email to {}: {}", recipient, e.getMessage());
            }
        }
    }

    /**
     * Calculate next execution time based on frequency
     */
    private void updateNextExecution(ScheduledReport report) {
        LocalDateTime nextExecution;
        LocalTime executionTime = report.getExecutionTime() != null ? report.getExecutionTime() : LocalTime.of(8, 0);

        switch (report.getFrequency()) {
            case DAILY:
                nextExecution = LocalDateTime.now().plusDays(1).with(executionTime);
                break;

            case WEEKLY:
                DayOfWeek targetDay = DayOfWeek.of(report.getDayOfWeek() != null ? report.getDayOfWeek() : 1);
                nextExecution = LocalDateTime
                    .now()
                    .with(TemporalAdjusters.next(targetDay))
                    .with(executionTime);
                break;

            case MONTHLY:
                int dayOfMonth = report.getDayOfMonth() != null ? report.getDayOfMonth() : 1;
                nextExecution = LocalDateTime.now().plusMonths(1).withDayOfMonth(dayOfMonth).with(executionTime);
                break;

            default:
                nextExecution = LocalDateTime.now().plusDays(1).with(executionTime);
        }

        report.setNextExecution(nextExecution);
    }

    /**
     * Get date range for report based on frequency
     */
    private LocalDate[] getReportDateRange(ScheduledReportFrequency frequency) {
        LocalDate endDate = LocalDate.now().minusDays(1); // Yesterday
        LocalDate startDate;

        switch (frequency) {
            case DAILY:
                startDate = endDate; // Same day
                break;

            case WEEKLY:
                startDate = endDate.minusDays(7);
                break;

            case MONTHLY:
                startDate = endDate.minusMonths(1);
                break;

            default:
                startDate = endDate.minusDays(7);
        }

        return new LocalDate[]{startDate, endDate};
    }

    /**
     * Create a new scheduled report
     */
    public ScheduledReport createScheduledReport(ScheduledReport scheduledReport) {
        updateNextExecution(scheduledReport);
        return scheduledReportRepository.save(scheduledReport);
    }

    /**
     * Update a scheduled report
     */
    public ScheduledReport updateScheduledReport(ScheduledReport scheduledReport) {
        updateNextExecution(scheduledReport);
        return scheduledReportRepository.save(scheduledReport);
    }

    /**
     * Delete a scheduled report
     */
    public void deleteScheduledReport(Integer id) {
        scheduledReportRepository.deleteById(id);
    }

    /**
     * Get all scheduled reports
     */
    @Transactional(readOnly = true)
    public List<ScheduledReport> getAllScheduledReports() {
        return scheduledReportRepository.findAll();
    }

    /**
     * Get all active scheduled reports
     */
    @Transactional(readOnly = true)
    public List<ScheduledReport> getActiveScheduledReports() {
        return scheduledReportRepository.findByActiveTrue();
    }
}
