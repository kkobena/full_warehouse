package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for individual user performance data.
 * Used for seller performance notifications and analytics.
 */
public class UserPerformanceDTO {

    private Long userId;
    private String userName;
    private Long totalCA;
    private Integer salesCount;
    private Long averageBasket;
    private Double variationVsPreviousDay;
    private Integer rank;
    private Integer totalSellers;
    private Long marginAmount;
    private Double marginPercent;

    public UserPerformanceDTO() {}

    public UserPerformanceDTO(
        Long userId,
        String userName,
        Long totalCA,
        Integer salesCount,
        Long averageBasket,
        Double variationVsPreviousDay
    ) {
        this.userId = userId;
        this.userName = userName;
        this.totalCA = totalCA;
        this.salesCount = salesCount;
        this.averageBasket = averageBasket;
        this.variationVsPreviousDay = variationVsPreviousDay;
    }

    // Getters and Setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getTotalCA() {
        return totalCA;
    }

    public void setTotalCA(Long totalCA) {
        this.totalCA = totalCA;
    }

    public Integer getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Integer salesCount) {
        this.salesCount = salesCount;
    }

    public Long getAverageBasket() {
        return averageBasket;
    }

    public void setAverageBasket(Long averageBasket) {
        this.averageBasket = averageBasket;
    }

    public Double getVariationVsPreviousDay() {
        return variationVsPreviousDay;
    }

    public void setVariationVsPreviousDay(Double variationVsPreviousDay) {
        this.variationVsPreviousDay = variationVsPreviousDay;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getTotalSellers() {
        return totalSellers;
    }

    public void setTotalSellers(Integer totalSellers) {
        this.totalSellers = totalSellers;
    }

    public Long getMarginAmount() {
        return marginAmount;
    }

    public void setMarginAmount(Long marginAmount) {
        this.marginAmount = marginAmount;
    }

    public Double getMarginPercent() {
        return marginPercent;
    }

    public void setMarginPercent(Double marginPercent) {
        this.marginPercent = marginPercent;
    }

    @Override
    public String toString() {
        return "UserPerformanceDTO{" +
            "userId=" + userId +
            ", userName='" + userName + '\'' +
            ", totalCA=" + totalCA +
            ", salesCount=" + salesCount +
            ", averageBasket=" + averageBasket +
            ", variationVsPreviousDay=" + variationVsPreviousDay +
            ", rank=" + rank +
            ", totalSellers=" + totalSellers +
            ", marginAmount=" + marginAmount +
            ", marginPercent=" + marginPercent +
            '}';
    }
}
