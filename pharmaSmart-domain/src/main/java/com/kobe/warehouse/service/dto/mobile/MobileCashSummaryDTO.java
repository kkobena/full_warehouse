package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO for mobile cash summary report (Ticket Z / Récapitulatif Caisse).
 *
 * @param fromDate       Start date of the period
 * @param toDate         End date of the period
 * @param fromTime       Start time (optional, for intra-day filtering)
 * @param toTime         End time (optional, for intra-day filtering)
 * @param periodLabel    Human-readable period label (e.g., "Aujourd'hui", "27/12/2024")
 * @param globalSummary  List of global summary items (totals across all cashiers)
 * @param cashierRecaps  List of per-cashier recaps
 * @param totalTtc       Total TTC calculated
 * @param totalEspeces   Total cash amount
 * @param totalCartes    Total card amount
 * @param totalMobileMoney Total mobile money amount
 * @param totalCheques   Total cheque amount
 * @param totalVirements Total wire transfer amount
 * @param totalCredit    Total credit sales
 * @param totalMobile    Total mobile payments (aggregated)
 * @param cashierCount   Number of cashiers with transactions
 */
public record MobileCashSummaryDTO(
    LocalDate fromDate,
    LocalDate toDate,
    LocalTime fromTime,
    LocalTime toTime,
    String periodLabel,
    List<SummaryItemDTO> globalSummary,
    List<CashierRecapDTO> cashierRecaps,
    long totalTtc,
    long totalEspeces,
    long totalCartes,
    long totalMobileMoney,
    long totalCheques,
    long totalVirements,
    long totalCredit,
    long totalMobile,
    int cashierCount
) {
    /**
     * Builder for creating MobileCashSummaryDTO instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate fromDate;
        private LocalDate toDate;
        private LocalTime fromTime;
        private LocalTime toTime;
        private String periodLabel;
        private List<SummaryItemDTO> globalSummary = List.of();
        private List<CashierRecapDTO> cashierRecaps = List.of();
        private long totalTtc;
        private long totalEspeces;
        private long totalCartes;
        private long totalMobileMoney;
        private long totalCheques;
        private long totalVirements;
        private long totalCredit;
        private long totalMobile;
        private int cashierCount;

        public Builder fromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder toDate(LocalDate toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder fromTime(LocalTime fromTime) {
            this.fromTime = fromTime;
            return this;
        }

        public Builder toTime(LocalTime toTime) {
            this.toTime = toTime;
            return this;
        }

        public Builder periodLabel(String periodLabel) {
            this.periodLabel = periodLabel;
            return this;
        }

        public Builder globalSummary(List<SummaryItemDTO> globalSummary) {
            this.globalSummary = globalSummary;
            return this;
        }

        public Builder cashierRecaps(List<CashierRecapDTO> cashierRecaps) {
            this.cashierRecaps = cashierRecaps;
            return this;
        }

        public Builder totalTtc(long totalTtc) {
            this.totalTtc = totalTtc;
            return this;
        }

        public Builder totalEspeces(long totalEspeces) {
            this.totalEspeces = totalEspeces;
            return this;
        }

        public Builder totalCartes(long totalCartes) {
            this.totalCartes = totalCartes;
            return this;
        }

        public Builder totalMobileMoney(long totalMobileMoney) {
            this.totalMobileMoney = totalMobileMoney;
            return this;
        }

        public Builder totalCheques(long totalCheques) {
            this.totalCheques = totalCheques;
            return this;
        }

        public Builder totalVirements(long totalVirements) {
            this.totalVirements = totalVirements;
            return this;
        }

        public Builder totalCredit(long totalCredit) {
            this.totalCredit = totalCredit;
            return this;
        }

        public Builder totalMobile(long totalMobile) {
            this.totalMobile = totalMobile;
            return this;
        }

        public Builder cashierCount(int cashierCount) {
            this.cashierCount = cashierCount;
            return this;
        }

        public MobileCashSummaryDTO build() {
            return new MobileCashSummaryDTO(
                fromDate, toDate, fromTime, toTime, periodLabel,
                globalSummary, cashierRecaps,
                totalTtc, totalEspeces, totalCartes, totalMobileMoney,
                totalCheques, totalVirements, totalCredit, totalMobile, cashierCount
            );
        }
    }
}
