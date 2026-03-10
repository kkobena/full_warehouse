package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.repository.MobileAlertRepository;
import com.kobe.warehouse.repository.MobileAlertRepository.ExpiryAlertProjection;
import com.kobe.warehouse.repository.MobileAlertRepository.OverdueInvoiceProjection;
import com.kobe.warehouse.repository.MobileAlertRepository.StockRuptureProjection;
import com.kobe.warehouse.service.dto.mobile.AlertSeverity;
import com.kobe.warehouse.service.dto.mobile.AlertType;
import com.kobe.warehouse.service.dto.mobile.MobileAlertDetailDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.MobileAlertDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile alerts and notifications.
 * Uses MobileAlertRepository for data access.
 */
@Service
@Transactional(readOnly = true)
public class MobileAlertService {


    private final MobileAlertRepository alertRepository;

    public MobileAlertService(MobileAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Get summary of all alert types for dashboard.
     *
     * @return List of alert summaries with counts
     */
    public List<MobileAlertDTO> getAlertsSummary() {
        List<MobileAlertDTO> alerts = new ArrayList<>();

        // 1. Stock ruptures (critical)
        int stockRuptureCount = alertRepository.getStockRuptureCount();
        if (stockRuptureCount > 0) {
            AlertType type = AlertType.STOCK_RUPTURE;
            AlertSeverity severity = AlertSeverity.CRITICAL;
            alerts.add(new MobileAlertDTO(
                type.getCode(),
                severity.getCode(),
                stockRuptureCount + " ruptures de stock",
                stockRuptureCount,
                type.getIcon(),
                severity.getColor()
            ));
        }

        // 2. Expiring products (< 30 days)
        int expiryCount = alertRepository.getExpiringProductsCount(30);
        if (expiryCount > 0) {
            AlertType type = AlertType.EXPIRY;
            AlertSeverity severity = AlertSeverity.WARNING;
            alerts.add(new MobileAlertDTO(
                type.getCode(),
                severity.getCode(),
                expiryCount + " peremptions < 30j",
                expiryCount,
                type.getIcon(),
                severity.getColor()
            ));
        }

        // 3. Cash discrepancy (today)
        long cashDiscrepancyAmount = alertRepository.getCashDiscrepancyAmount(LocalDate.now());
        if (cashDiscrepancyAmount > 0) {
            AlertType type = AlertType.CASH_DISCREPANCY;
            AlertSeverity severity = AlertSeverity.fromCriticalThreshold(cashDiscrepancyAmount > 10000);
            alerts.add(new MobileAlertDTO(
                type.getCode(),
                severity.getCode(),
                "Ecart caisse: " + formatAmount(cashDiscrepancyAmount) + " F",
                1,
                type.getIcon(),
                severity.getColor()
            ));
        }

        // 4. Overdue invoices (> 90 days)
        int overdueCount = alertRepository.getOverdueInvoicesCount(90);
        if (overdueCount > 0) {
            AlertType type = AlertType.INVOICE_OVERDUE;
            AlertSeverity severity = AlertSeverity.INFO;
            alerts.add(new MobileAlertDTO(
                type.getCode(),
                severity.getCode(),
                overdueCount + " factures impayees > 90j",
                overdueCount,
                type.getIcon(),
                severity.getColor()
            ));
        }

        return alerts;
    }

    /**
     * Get detailed list of alerts by type (without pagination).
     *
     * @param types List of alert types to filter (null for all)
     * @return List of detailed alerts
     */
    public List<MobileAlertDetailDTO> getAlerts(List<String> types) {
        return getAlerts(types, 0, Integer.MAX_VALUE);
    }

    /**
     * Get detailed list of alerts by type with pagination.
     *
     * @param types List of alert types to filter (null for all)
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of detailed alerts for the requested page
     */
    public List<MobileAlertDetailDTO> getAlerts(List<String> types, int page, int size) {
        List<MobileAlertDetailDTO> allAlerts = new ArrayList<>();

        if (types == null || types.isEmpty() || types.contains(AlertType.STOCK_RUPTURE.getCode())) {
            allAlerts.addAll(mapStockRuptureAlerts(alertRepository.getStockRuptureAlerts()));
        }

        if (types == null || types.isEmpty() || types.contains(AlertType.EXPIRY.getCode())) {
            allAlerts.addAll(mapExpiryAlerts(alertRepository.getExpiryAlerts(30)));
        }

        if (types == null || types.isEmpty() || types.contains(AlertType.INVOICE_OVERDUE.getCode())) {
            allAlerts.addAll(mapOverdueInvoiceAlerts(alertRepository.getOverdueInvoiceAlerts(90)));
        }

        // Apply pagination
        int fromIndex = page * size;
        if (fromIndex >= allAlerts.size()) {
            return new ArrayList<>();
        }
        int toIndex = Math.min(fromIndex + size, allAlerts.size());
        return allAlerts.subList(fromIndex, toIndex);
    }

    /**
     * Get total count of alerts by type.
     *
     * @param types List of alert types to filter (null for all)
     * @return Total number of alerts
     */
    public long getAlertsCount(List<String> types) {
        long count = 0;

        if (types == null || types.isEmpty() || types.contains(AlertType.STOCK_RUPTURE.getCode())) {
            count += alertRepository.getStockRuptureCount();
        }

        if (types == null || types.isEmpty() || types.contains(AlertType.EXPIRY.getCode())) {
            count += alertRepository.getExpiringProductsCount(30);
        }

        if (types == null || types.isEmpty() || types.contains(AlertType.INVOICE_OVERDUE.getCode())) {
            count += alertRepository.getOverdueInvoicesCount(90);
        }

        return count;
    }

    /**
     * Map stock rupture projections to alert DTOs.
     */
    private List<MobileAlertDetailDTO> mapStockRuptureAlerts(List<StockRuptureProjection> projections) {
        AlertType type = AlertType.STOCK_RUPTURE;
        AlertSeverity severity = AlertSeverity.CRITICAL;

        List<MobileAlertDetailDTO> alerts = new ArrayList<>();
        for (StockRuptureProjection p : projections) {
            alerts.add(MobileAlertDetailDTO.builder()
                .id(p.productId())
                .type(type.getCode())
                .severity(severity.getCode())
                .title(type.getLibelle())
                .message(p.productName() + " - Stock epuise")
                .icon(type.getIcon())
                .color(severity.getColor())
                .createdAt(LocalDateTime.now())
                .actionType("VIEW_PRODUCT")
                .actionData(Map.of("productId", p.productId()))
                .relatedEntityId(p.productId())
                .relatedEntityType("PRODUCT")
                .relatedEntityName(p.productName())
                .build());
        }

        return alerts;
    }

    /**
     * Map expiry projections to alert DTOs.
     */
    private List<MobileAlertDetailDTO> mapExpiryAlerts(List<ExpiryAlertProjection> projections) {
        AlertType type = AlertType.EXPIRY;

        List<MobileAlertDetailDTO> alerts = new ArrayList<>();
        for (ExpiryAlertProjection p : projections) {
            AlertSeverity severity = AlertSeverity.fromCriticalThreshold(p.daysUntilExpiry() <= 7);

            alerts.add(MobileAlertDetailDTO.builder()
                .id(p.lotId())
                .type(type.getCode())
                .severity(severity.getCode())
                .title(type.getLibelle())
                .message(p.productName() + " - Expire dans " + p.daysUntilExpiry() + " jours")
                .icon(type.getIcon())
                .color(severity.getColor())
                .createdAt(LocalDateTime.now())
                .actionType("VIEW_PRODUCT")
                .actionData(Map.of("productId", p.productId(), "lotId", p.lotId()))
                .relatedEntityId(p.productId())
                .relatedEntityType("PRODUCT")
                .relatedEntityName(p.productName())
                .build());
        }

        return alerts;
    }

    /**
     * Map overdue invoice projections to alert DTOs.
     */
    private List<MobileAlertDetailDTO> mapOverdueInvoiceAlerts(List<OverdueInvoiceProjection> projections) {
        AlertType type = AlertType.INVOICE_OVERDUE;

        List<MobileAlertDetailDTO> alerts = new ArrayList<>();
        for (OverdueInvoiceProjection p : projections) {
            long montantRestant = p.montantFacture() - p.montantRegle();
            AlertSeverity severity = AlertSeverity.fromCriticalThreshold(p.daysOverdue() > 180);

            alerts.add(MobileAlertDetailDTO.builder()
                .id(p.invoiceId())
                .type(type.getCode())
                .severity(severity.getCode())
                .title(type.getLibelle())
                .message(p.tiersPayantName() + " - " + formatAmount(montantRestant) + " F depuis " + p.daysOverdue() + "j")
                .icon(type.getIcon())
                .color(severity.getColor())
                .createdAt(LocalDateTime.now())
                .actionType("CALL_CLIENT")
                .actionData(Map.of("invoiceId", p.invoiceId(), "phone", p.telephone() != null ? p.telephone() : ""))
                .relatedEntityId(p.invoiceId())
                .relatedEntityType("INVOICE")
                .relatedEntityName(p.tiersPayantName())
                .build());
        }

        return alerts;
    }

    private String formatAmount(long amount) {
        return String.format("%,d", amount).replace(",", " ");
    }
}
