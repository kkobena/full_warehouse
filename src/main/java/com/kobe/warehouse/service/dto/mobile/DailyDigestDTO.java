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
    private Long targetCA;
    private Double targetProgress;
    private Integer customersCount;
    private Long averageBasket;

    public DailyDigestDTO() {}

    public DailyDigestDTO(
        Long totalCA,
        Double variation,
        Integer transactionCount,
        Integer alertsCount,
        Long targetCA,
        Double targetProgress,
        Integer customersCount,
        Long averageBasket
    ) {
        this.totalCA = totalCA;
        this.variation = variation;
        this.transactionCount = transactionCount;
        this.alertsCount = alertsCount;
        this.targetCA = targetCA;
        this.targetProgress = targetProgress;
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

    public Long getTargetCA() {
        return targetCA;
    }

    public void setTargetCA(Long targetCA) {
        this.targetCA = targetCA;
    }

    public Double getTargetProgress() {
        return targetProgress;
    }

    public void setTargetProgress(Double targetProgress) {
        this.targetProgress = targetProgress;
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
            ", targetCA=" + targetCA +
            ", targetProgress=" + targetProgress +
            ", customersCount=" + customersCount +
            ", averageBasket=" + averageBasket +
            '}';
    }
}
