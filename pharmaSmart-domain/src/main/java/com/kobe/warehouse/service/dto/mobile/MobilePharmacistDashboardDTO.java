package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for mobile pharmacist dashboard (Tableau Pharmacien).
 * Provides a comprehensive view of sales vs purchases for a given period.
 */
public record MobilePharmacistDashboardDTO(
    // Period
    LocalDate fromDate,
    LocalDate toDate,
    String periodLabel,

    // Sales (Ventes)
    long montantVenteComptant,
    long montantVenteCredit,
    long montantVenteRemise,
    long montantVenteNet,
    long montantVenteTtc,
    long montantVenteHt,
    long montantVenteTaxe,
    int transactionsCount,

    // Purchases (Achats)
    long montantAchatNet,
    long montantAchatTtc,
    long montantAchatHt,
    long montantAchatTaxe,
    long montantAchatRemise,
    long montantAvoirFournisseur,

    // Ratios
    double ratioVenteAchat,
    double ratioAchatVente,

    // Calculated margin
    long marge,
    double margePercent,

    // Variations vs previous period
    Double ventesVariation,
    Double achatsVariation,

    // Top suppliers (top 10)
    List<FournisseurAchatMobileDTO> topFournisseurs,

    // Chart data for sales vs purchases
    List<ChartDataPointDTO> chartVentesAchats
) {
    /**
     * Creates a builder for constructing the DTO.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate fromDate;
        private LocalDate toDate;
        private String periodLabel;
        private long montantVenteComptant;
        private long montantVenteCredit;
        private long montantVenteRemise;
        private long montantVenteNet;
        private long montantVenteTtc;
        private long montantVenteHt;
        private long montantVenteTaxe;
        private int transactionsCount;
        private long montantAchatNet;
        private long montantAchatTtc;
        private long montantAchatHt;
        private long montantAchatTaxe;
        private long montantAchatRemise;
        private long montantAvoirFournisseur;
        private double ratioVenteAchat;
        private double ratioAchatVente;
        private long marge;
        private double margePercent;
        private Double ventesVariation;
        private Double achatsVariation;
        private List<FournisseurAchatMobileDTO> topFournisseurs;
        private List<ChartDataPointDTO> chartVentesAchats;

        public Builder fromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder toDate(LocalDate toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder periodLabel(String periodLabel) {
            this.periodLabel = periodLabel;
            return this;
        }

        public Builder montantVenteComptant(long montantVenteComptant) {
            this.montantVenteComptant = montantVenteComptant;
            return this;
        }

        public Builder montantVenteCredit(long montantVenteCredit) {
            this.montantVenteCredit = montantVenteCredit;
            return this;
        }

        public Builder montantVenteRemise(long montantVenteRemise) {
            this.montantVenteRemise = montantVenteRemise;
            return this;
        }

        public Builder montantVenteNet(long montantVenteNet) {
            this.montantVenteNet = montantVenteNet;
            return this;
        }

        public Builder montantVenteTtc(long montantVenteTtc) {
            this.montantVenteTtc = montantVenteTtc;
            return this;
        }

        public Builder montantVenteHt(long montantVenteHt) {
            this.montantVenteHt = montantVenteHt;
            return this;
        }

        public Builder montantVenteTaxe(long montantVenteTaxe) {
            this.montantVenteTaxe = montantVenteTaxe;
            return this;
        }

        public Builder transactionsCount(int transactionsCount) {
            this.transactionsCount = transactionsCount;
            return this;
        }

        public Builder montantAchatNet(long montantAchatNet) {
            this.montantAchatNet = montantAchatNet;
            return this;
        }

        public Builder montantAchatTtc(long montantAchatTtc) {
            this.montantAchatTtc = montantAchatTtc;
            return this;
        }

        public Builder montantAchatHt(long montantAchatHt) {
            this.montantAchatHt = montantAchatHt;
            return this;
        }

        public Builder montantAchatTaxe(long montantAchatTaxe) {
            this.montantAchatTaxe = montantAchatTaxe;
            return this;
        }

        public Builder montantAchatRemise(long montantAchatRemise) {
            this.montantAchatRemise = montantAchatRemise;
            return this;
        }

        public Builder montantAvoirFournisseur(long montantAvoirFournisseur) {
            this.montantAvoirFournisseur = montantAvoirFournisseur;
            return this;
        }

        public Builder ratioVenteAchat(double ratioVenteAchat) {
            this.ratioVenteAchat = ratioVenteAchat;
            return this;
        }

        public Builder ratioAchatVente(double ratioAchatVente) {
            this.ratioAchatVente = ratioAchatVente;
            return this;
        }

        public Builder marge(long marge) {
            this.marge = marge;
            return this;
        }

        public Builder margePercent(double margePercent) {
            this.margePercent = margePercent;
            return this;
        }

        public Builder ventesVariation(Double ventesVariation) {
            this.ventesVariation = ventesVariation;
            return this;
        }

        public Builder achatsVariation(Double achatsVariation) {
            this.achatsVariation = achatsVariation;
            return this;
        }

        public Builder topFournisseurs(List<FournisseurAchatMobileDTO> topFournisseurs) {
            this.topFournisseurs = topFournisseurs;
            return this;
        }

        public Builder chartVentesAchats(List<ChartDataPointDTO> chartVentesAchats) {
            this.chartVentesAchats = chartVentesAchats;
            return this;
        }

        public MobilePharmacistDashboardDTO build() {
            return new MobilePharmacistDashboardDTO(
                fromDate,
                toDate,
                periodLabel,
                montantVenteComptant,
                montantVenteCredit,
                montantVenteRemise,
                montantVenteNet,
                montantVenteTtc,
                montantVenteHt,
                montantVenteTaxe,
                transactionsCount,
                montantAchatNet,
                montantAchatTtc,
                montantAchatHt,
                montantAchatTaxe,
                montantAchatRemise,
                montantAvoirFournisseur,
                ratioVenteAchat,
                ratioAchatVente,
                marge,
                margePercent,
                ventesVariation,
                achatsVariation,
                topFournisseurs,
                chartVentesAchats
            );
        }
    }
}
