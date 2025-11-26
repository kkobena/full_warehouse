package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ScheduledReportFrequency;
import com.kobe.warehouse.domain.enumeration.ScheduledReportType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity for scheduled automatic report generation
 */
@Entity
@Table(name = "scheduled_report")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ScheduledReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "report_name", nullable = false)
    private String reportName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ScheduledReportType reportType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false,length = 15)
    private ScheduledReportFrequency frequency;

    @Column(name = "execution_time")
    private LocalTime executionTime;

    @Column(name = "day_of_week")
    private Integer dayOfWeek; // 1-7 for weekly

    @Column(name = "day_of_month")
    private Integer dayOfMonth; // 1-31 for monthly

    @NotNull
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "email_recipients")
    private String emailRecipients; // Comma-separated emails

    @Column(name = "include_pdf", nullable = false)
    private boolean includePdf = true;

    @Column(name = "include_excel", nullable = false)
    private boolean includeExcel ;

    @Column(name = "last_execution")
    private LocalDateTime lastExecution;

    @Column(name = "next_execution")
    private LocalDateTime nextExecution;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private AppUser createdBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "filter_params", length = 2000)
    private Map<String, Object> filterParams; // JSON string for report-specific filters
    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public ScheduledReportType getReportType() {
        return reportType;
    }

    public void setReportType(ScheduledReportType reportType) {
        this.reportType = reportType;
    }

    public ScheduledReportFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(ScheduledReportFrequency frequency) {
        this.frequency = frequency;
    }

    public LocalTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalTime executionTime) {
        this.executionTime = executionTime;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }



    public String getEmailRecipients() {
        return emailRecipients;
    }

    public void setEmailRecipients(String emailRecipients) {
        this.emailRecipients = emailRecipients;
    }


    public void setIncludeExcel(Boolean includeExcel) {
        this.includeExcel = includeExcel;
    }

    public LocalDateTime getLastExecution() {
        return lastExecution;
    }

    public void setLastExecution(LocalDateTime lastExecution) {
        this.lastExecution = lastExecution;
    }

    public LocalDateTime getNextExecution() {
        return nextExecution;
    }

    public void setNextExecution(LocalDateTime nextExecution) {
        this.nextExecution = nextExecution;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, Object> getFilterParams() {
        return filterParams;
    }

    public void setFilterParams(Map<String, Object> filterParams) {
        this.filterParams = filterParams;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isIncludePdf() {
        return includePdf;
    }

    public void setIncludePdf(boolean includePdf) {
        this.includePdf = includePdf;
    }

    public boolean isIncludeExcel() {
        return includeExcel;
    }

    public void setIncludeExcel(boolean includeExcel) {
        this.includeExcel = includeExcel;
    }


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
