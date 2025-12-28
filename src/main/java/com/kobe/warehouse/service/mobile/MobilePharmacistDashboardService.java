package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.mobile.ChartDataPointDTO;
import com.kobe.warehouse.service.dto.mobile.FournisseurAchatMobileDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePharmacistDashboardDTO;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienService;
import com.kobe.warehouse.service.financiel_transaction.dto.FournisseurAchat;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile pharmacist dashboard (Tableau Pharmacien).
 * Provides aggregated sales vs purchases data optimized for mobile display.
 */
@Service
@Transactional(readOnly = true)
public class MobilePharmacistDashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(MobilePharmacistDashboardService.class);
    private static final int TOP_FOURNISSEURS_LIMIT = 10;
    private static final Locale FR_LOCALE = Locale.FRENCH;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TableauPharmacienService tableauPharmacienService;

    public MobilePharmacistDashboardService(
        @Qualifier("tableauPharmacienServiceImpl") TableauPharmacienService tableauPharmacienService
    ) {
        this.tableauPharmacienService = tableauPharmacienService;
    }

    /**
     * Get pharmacist dashboard data for a given date range.
     *
     * @param fromDate Start date of the period
     * @param toDate End date of the period
     * @return Complete dashboard DTO with all sales/purchases data
     */
    public MobilePharmacistDashboardDTO getPharmacistDashboard(LocalDate fromDate, LocalDate toDate) {
        LOG.debug("Getting pharmacist dashboard for period: {} to {}", fromDate, toDate);

        // Build query parameters
        MvtParam mvtParam = buildMvtParam(fromDate, toDate, "daily");

        // Get tableau pharmacien data from existing service
        TableauPharmacienWrapper wrapper = tableauPharmacienService.getTableauPharmacien(mvtParam);

        // Build period label
        String periodLabel = buildPeriodLabel(fromDate, toDate);

        // Calculate margin
        long marge = wrapper.getMontantVenteNet() - wrapper.getMontantAchatNet();
        double margePercent = wrapper.getMontantVenteNet() > 0
            ? BigDecimal.valueOf(marge)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(wrapper.getMontantVenteNet()), 2, RoundingMode.HALF_UP)
                .doubleValue()
            : 0.0;

        // Calculate variations vs previous period
        Double ventesVariation = calculatePreviousPeriodVariation(fromDate, toDate, true);
        Double achatsVariation = calculatePreviousPeriodVariation(fromDate, toDate, false);

        // Build top suppliers list
        List<FournisseurAchatMobileDTO> topFournisseurs = buildTopFournisseurs(
            wrapper.getGroupAchats(),
            wrapper.getMontantAchatNet()
        );

        // Build chart data for daily sales vs purchases
        List<ChartDataPointDTO> chartVentesAchats = buildChartData(wrapper);

        return MobilePharmacistDashboardDTO.builder()
            .fromDate(fromDate)
            .toDate(toDate)
            .periodLabel(periodLabel)
            .montantVenteComptant(wrapper.getMontantVenteComptant())
            .montantVenteCredit(wrapper.getMontantVenteCredit())
            .montantVenteRemise(wrapper.getMontantVenteRemise())
            .montantVenteNet(wrapper.getMontantVenteNet())
            .montantVenteTtc(wrapper.getMontantVenteTtc())
            .montantVenteHt(wrapper.getMontantVenteHt())
            .montantVenteTaxe(wrapper.getMontantVenteTaxe())
            .transactionsCount((int) wrapper.getNumberCount())
            .montantAchatNet(wrapper.getMontantAchatNet())
            .montantAchatTtc(wrapper.getMontantAchatTtc())
            .montantAchatHt(wrapper.getMontantAchatHt())
            .montantAchatTaxe(wrapper.getMontantAchatTaxe())
            .montantAchatRemise(wrapper.getMontantAchatRemise())
            .montantAvoirFournisseur(wrapper.getMontantAvoirFournisseur())
            .ratioVenteAchat(wrapper.getRatioVenteAchat())
            .ratioAchatVente(wrapper.getRatioAchatVente())
            .marge(marge)
            .margePercent(margePercent)
            .ventesVariation(ventesVariation)
            .achatsVariation(achatsVariation)
            .topFournisseurs(topFournisseurs)
            .chartVentesAchats(chartVentesAchats)
            .build();
    }

    /**
     * Build MvtParam for the query.
     */
    private MvtParam buildMvtParam(LocalDate fromDate, LocalDate toDate, String groupBy) {
        MvtParam mvtParam = new MvtParam();
        mvtParam.setFromDate(fromDate);
        mvtParam.setToDate(toDate);
        mvtParam.setGroupeBy(groupBy);
        mvtParam.setStatuts(EnumSet.of(SalesStatut.CLOSED));
        mvtParam.setCategorieChiffreAffaires(
            EnumSet.of(
                CategorieChiffreAffaire.CA,
                CategorieChiffreAffaire.CA_DEPOT
            )
        );
        return mvtParam;
    }

    /**
     * Build a human-readable period label.
     */
    private String buildPeriodLabel(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.equals(toDate)) {
            // Single day
            return fromDate.format(DATE_FORMATTER);
        } else if (fromDate.getMonth().equals(toDate.getMonth()) && fromDate.getYear() == toDate.getYear()) {
            // Same month
            return String.format(
                "Du %d au %d %s %d",
                fromDate.getDayOfMonth(),
                toDate.getDayOfMonth(),
                toDate.getMonth().getDisplayName(TextStyle.FULL, FR_LOCALE),
                toDate.getYear()
            );
        } else {
            // Different months
            return String.format(
                "%s - %s",
                fromDate.format(DATE_FORMATTER),
                toDate.format(DATE_FORMATTER)
            );
        }
    }

    /**
     * Calculate variation compared to the same duration in the previous period.
     */
    private Double calculatePreviousPeriodVariation(LocalDate fromDate, LocalDate toDate, boolean forSales) {
        try {
            long currentPeriodDays = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
            LocalDate previousToDate = fromDate.minusDays(1);
            LocalDate previousFromDate = previousToDate.minusDays(currentPeriodDays - 1);

            MvtParam previousMvtParam = buildMvtParam(previousFromDate, previousToDate, "daily");
            TableauPharmacienWrapper previousWrapper = tableauPharmacienService.getTableauPharmacien(previousMvtParam);

            MvtParam currentMvtParam = buildMvtParam(fromDate, toDate, "daily");
            TableauPharmacienWrapper currentWrapper = tableauPharmacienService.getTableauPharmacien(currentMvtParam);

            long previousValue = forSales ? previousWrapper.getMontantVenteNet() : previousWrapper.getMontantAchatNet();
            long currentValue = forSales ? currentWrapper.getMontantVenteNet() : currentWrapper.getMontantAchatNet();

            if (previousValue == 0) {
                return currentValue > 0 ? 100.0 : 0.0;
            }

            return BigDecimal.valueOf(currentValue - previousValue)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previousValue), 2, RoundingMode.HALF_UP)
                .doubleValue();
        } catch (Exception e) {
            LOG.warn("Error calculating variation: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build top suppliers list sorted by purchase amount.
     */
    private List<FournisseurAchatMobileDTO> buildTopFournisseurs(
        List<FournisseurAchat> groupAchats,
        long totalAchats
    ) {
        if (groupAchats == null || groupAchats.isEmpty()) {
            return new ArrayList<>();
        }

        return groupAchats.stream()
            .sorted(Comparator.comparingLong((FournisseurAchat f) -> f.getAchat().getMontantNet()).reversed())
            .limit(TOP_FOURNISSEURS_LIMIT)
            .map(f -> FournisseurAchatMobileDTO.of(
                f.getId(),
                f.getLibelle(),
                f.getAchat().getMontantNet(),
                f.getAchat().getMontantTtc(),
                f.getAchat().getMontantHt(),
                f.getAchat().getMontantTaxe(),
                f.getAchat().getMontantRemise(),
                totalAchats
            ))
            .toList();
    }

    /**
     * Build chart data showing daily sales vs purchases trend.
     */
    private List<ChartDataPointDTO> buildChartData(TableauPharmacienWrapper wrapper) {
        List<ChartDataPointDTO> chartData = new ArrayList<>();

        if (wrapper.getTableauPharmaciens() == null || wrapper.getTableauPharmaciens().isEmpty()) {
            return chartData;
        }

        // Sort by date
        List<TableauPharmacienDTO> sortedData = wrapper.getTableauPharmaciens().stream()
            .sorted(Comparator.comparing(TableauPharmacienDTO::getMvtDate))
            .toList();

        for (TableauPharmacienDTO dto : sortedData) {
            String dateLabel = dto.getMvtDate().getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, FR_LOCALE);

            // Add sales point
            chartData.add(ChartDataPointDTO.sales(dateLabel, dto.getMontantNet()));

            // Add purchases point
            chartData.add(ChartDataPointDTO.purchases(dateLabel, dto.getMontantBonAchat()));
        }

        return chartData;
    }
}
