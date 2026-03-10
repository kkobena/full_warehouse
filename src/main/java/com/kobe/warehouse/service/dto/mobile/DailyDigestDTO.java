package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for daily digest notification data.
 * Used for manager daily summary notifications.
 */
public class DailyDigestDTO {

    private Long totalCA;
    private Double variation;
    private Integer transactionCount;
    private Integer alertsCount;
    /** CA moyen journalier sur les 30 derniers jours — référence glissante. */
    private Long averageCA30j;
    /** Écart en % entre le CA du jour et la moyenne des 30 derniers jours. */
    private Double trendVs30j;
    private Integer customersCount;
    private Long averageBasket;

    public DailyDigestDTO() {}

    public DailyDigestDTO(
        Long totalCA,
        Double variation,
        Integer transactionCount,
        Integer alertsCount,
        Long averageCA30j,
        Double trendVs30j,
        Integer customersCount,
        Long averageBasket
    ) {
        this.totalCA = totalCA;
        this.variation = variation;
        this.transactionCount = transactionCount;
        this.alertsCount = alertsCount;
        this.averageCA30j = averageCA30j;
        this.trendVs30j = trendVs30j;
        this.customersCount = customersCount;
        this.averageBasket = averageBasket;
    }

    // Getters and Setters

    public Long getTotalCA() {
        return totalCA;
    }

    public void setTotalCA(Long totalCA) {
        this.totalCA = totalCA;
    }

    public Double getVariation() {
        return variation;
    }

    public void setVariation(Double variation) {
        this.variation = variation;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Integer getAlertsCount() {
        return alertsCount;
    }

    public void setAlertsCount(Integer alertsCount) {
        this.alertsCount = alertsCount;
    }

    public Long getAverageCA30j() {
        return averageCA30j;
    }

    public void setAverageCA30j(Long averageCA30j) {
        this.averageCA30j = averageCA30j;
    }

    public Double getTrendVs30j() {
        return trendVs30j;
    }

    public void setTrendVs30j(Double trendVs30j) {
        this.trendVs30j = trendVs30j;
    }

    public Integer getCustomersCount() {
        return customersCount;
    }

    public void setCustomersCount(Integer customersCount) {
        this.customersCount = customersCount;
    }

    public Long getAverageBasket() {
        return averageBasket;
    }

    public void setAverageBasket(Long averageBasket) {
        this.averageBasket = averageBasket;
    }

    @Override
    public String toString() {
        return "DailyDigestDTO{" +
            "totalCA=" + totalCA +
            ", variation=" + variation +
            ", transactionCount=" + transactionCount +
            ", alertsCount=" + alertsCount +
            ", averageCA30j=" + averageCA30j +
            ", trendVs30j=" + trendVs30j +
            ", customersCount=" + customersCount +
            ", averageBasket=" + averageBasket +
            '}';
    }
}
