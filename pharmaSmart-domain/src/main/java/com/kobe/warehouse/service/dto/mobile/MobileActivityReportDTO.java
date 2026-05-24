package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for mobile activity report (Rapport d'Activité).
 * Consolidates all activity summary data in a single response.
 *
 * @param fromDate           Start date of the period
 * @param toDate             End date of the period
 * @param periodLabel        Human-readable period label
 * @param chiffreAffaire     Revenue summary (CA)
 * @param recettes           Receipts by payment mode
 * @param totalRecettes      Total of all receipts
 * @param mouvementsCaisse   Cash movements
 * @param totalEntrees       Total cash entries
 * @param totalSorties       Total cash exits
 * @param achatsFournisseurs Purchases by supplier group
 * @param totalAchats        Total purchases
 * @param tiersPayants       Third-party payer summary
 */
public record MobileActivityReportDTO(
    LocalDate fromDate,
    LocalDate toDate,
    String periodLabel,

    // Chiffre d'affaires
    ChiffreAffaireMobileDTO chiffreAffaire,

    // Recettes par mode de paiement
    List<RecetteMobileDTO> recettes,
    long totalRecettes,

    // Mouvements de caisse
    List<MouvementCaisseMobileDTO> mouvementsCaisse,
    long totalEntrees,
    long totalSorties,

    // Achats par groupe fournisseur
    List<GroupeFournisseurAchatMobileDTO> achatsFournisseurs,
    long totalAchats,

    // Tiers payants
    TiersPayantSummaryMobileDTO tiersPayants
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate fromDate;
        private LocalDate toDate;
        private String periodLabel;
        private ChiffreAffaireMobileDTO chiffreAffaire;
        private List<RecetteMobileDTO> recettes = List.of();
        private long totalRecettes;
        private List<MouvementCaisseMobileDTO> mouvementsCaisse = List.of();
        private long totalEntrees;
        private long totalSorties;
        private List<GroupeFournisseurAchatMobileDTO> achatsFournisseurs = List.of();
        private long totalAchats;
        private TiersPayantSummaryMobileDTO tiersPayants;

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

        public Builder chiffreAffaire(ChiffreAffaireMobileDTO chiffreAffaire) {
            this.chiffreAffaire = chiffreAffaire;
            return this;
        }

        public Builder recettes(List<RecetteMobileDTO> recettes) {
            this.recettes = recettes;
            return this;
        }

        public Builder totalRecettes(long totalRecettes) {
            this.totalRecettes = totalRecettes;
            return this;
        }

        public Builder mouvementsCaisse(List<MouvementCaisseMobileDTO> mouvementsCaisse) {
            this.mouvementsCaisse = mouvementsCaisse;
            return this;
        }

        public Builder totalEntrees(long totalEntrees) {
            this.totalEntrees = totalEntrees;
            return this;
        }

        public Builder totalSorties(long totalSorties) {
            this.totalSorties = totalSorties;
            return this;
        }

        public Builder achatsFournisseurs(List<GroupeFournisseurAchatMobileDTO> achatsFournisseurs) {
            this.achatsFournisseurs = achatsFournisseurs;
            return this;
        }

        public Builder totalAchats(long totalAchats) {
            this.totalAchats = totalAchats;
            return this;
        }

        public Builder tiersPayants(TiersPayantSummaryMobileDTO tiersPayants) {
            this.tiersPayants = tiersPayants;
            return this;
        }

        public MobileActivityReportDTO build() {
            return new MobileActivityReportDTO(
                fromDate, toDate, periodLabel, chiffreAffaire,
                recettes, totalRecettes,
                mouvementsCaisse, totalEntrees, totalSorties,
                achatsFournisseurs, totalAchats,
                tiersPayants
            );
        }
    }
}
