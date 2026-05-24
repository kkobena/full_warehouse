package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.mobile.MobileTvaReportDTO;
import com.kobe.warehouse.service.dto.mobile.TvaChartDataDTO;
import com.kobe.warehouse.service.dto.mobile.TvaRateBreakdownDTO;
import com.kobe.warehouse.service.financiel_transaction.TaxeService;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for mobile TVA report.
 * Transforms the existing TaxeService data into mobile-friendly format.
 */
@Service
@Transactional(readOnly = true)
public class MobileTvaReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TaxeService taxeService;

    public MobileTvaReportService(TaxeService taxeService) {
        this.taxeService = taxeService;
    }

    /**
     * Get TVA report for the given date range.
     *
     * @param fromDate Start date
     * @param toDate   End date (defaults to fromDate if null)
     * @param groupByDate Whether to group by date
     * @return Mobile TVA report DTO
     */
    public MobileTvaReportDTO getTvaReport(LocalDate fromDate, LocalDate toDate, boolean groupByDate) {
        if (toDate == null) {
            toDate = fromDate;
        }

        // Build params for the existing service
        MvtParam params = new MvtParam();
        params.setFromDate(fromDate);
        params.setToDate(toDate);
        params.setCategorieChiffreAffaires(Set.of(CategorieChiffreAffaire.CA, CategorieChiffreAffaire.CA_DEPOT));
        if (groupByDate) {
            params.setGroupeBy("daily");
        }

        // Get data from existing service
        TaxeWrapperDTO wrapper = taxeService.fetchTaxe(params, false);

        if (wrapper == null || wrapper.getTaxes().isEmpty()) {
            return MobileTvaReportDTO.empty(fromDate, toDate, buildPeriodLabel(fromDate, toDate));
        }

        return buildTvaReport(fromDate, toDate, wrapper, groupByDate);
    }

    private MobileTvaReportDTO buildTvaReport(
            LocalDate fromDate,
            LocalDate toDate,
            TaxeWrapperDTO wrapper,
            boolean groupByDate) {

        String periodLabel = buildPeriodLabel(fromDate, toDate);

        // Build breakdown by TVA rate
        List<TvaRateBreakdownDTO> breakdown = buildTvaBreakdown(wrapper.getTaxes(), groupByDate);

        // Build chart data
        List<TvaChartDataDTO> chartData = buildChartData(breakdown, wrapper.getMontantTtc());

        return new MobileTvaReportDTO(
            fromDate,
            toDate,
            periodLabel,
            wrapper.getMontantHt(),
            wrapper.getMontantTaxe(),
            wrapper.getMontantTtc(),
            wrapper.getMontantNet(),
            wrapper.getMontantRemise(),
            wrapper.getMontantAchat(),
            wrapper.getMontantTvaUg(),
            wrapper.getMontantTtcUg(),
            wrapper.getMontantRemiseUg(),
            wrapper.getAmountToBeTakenIntoAccount(),
            breakdown,
            chartData
        );
    }

    private List<TvaRateBreakdownDTO> buildTvaBreakdown(List<TaxeDTO> taxes, boolean groupByDate) {
        if (groupByDate) {
            // Group by date and code
            return taxes.stream()
                .sorted(Comparator.comparing(TaxeDTO::getMvtDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(TaxeDTO::getCodeTva, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(t -> new TvaRateBreakdownDTO(
                    t.getCodeTva() != null ? t.getCodeTva() : 0,
                    TvaRateBreakdownDTO.getRateName(t.getCodeTva() != null ? t.getCodeTva() : 0),
                    t.getMontantHt() != null ? t.getMontantHt() : 0,
                    t.getMontantTaxe() != null ? t.getMontantTaxe() : 0,
                    t.getMontantTtc() != null ? t.getMontantTtc() : 0,
                    t.getMontantAchat() != null ? t.getMontantAchat() : 0,
                    t.getAmountToBeTakenIntoAccount() != null ? t.getAmountToBeTakenIntoAccount() : 0,
                    t.getMvtDate()
                ))
                .toList();
        } else {
            // Group by code only
            Map<Integer, List<TaxeDTO>> grouped = taxes.stream()
                .collect(Collectors.groupingBy(t -> t.getCodeTva() != null ? t.getCodeTva() : 0));

            return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    int code = entry.getKey();
                    List<TaxeDTO> items = entry.getValue();

                    long totalHt = items.stream().mapToLong(t -> t.getMontantHt() != null ? t.getMontantHt() : 0).sum();
                    long totalTva = items.stream().mapToLong(t -> t.getMontantTaxe() != null ? t.getMontantTaxe() : 0).sum();
                    long totalTtc = items.stream().mapToLong(t -> t.getMontantTtc() != null ? t.getMontantTtc() : 0).sum();
                    long totalAchat = items.stream().mapToLong(t -> t.getMontantAchat() != null ? t.getMontantAchat() : 0).sum();
                    long totalToAccount = items.stream().mapToLong(t -> t.getAmountToBeTakenIntoAccount() != null ? t.getAmountToBeTakenIntoAccount() : 0).sum();

                    return new TvaRateBreakdownDTO(
                        code,
                        TvaRateBreakdownDTO.getRateName(code),
                        totalHt,
                        totalTva,
                        totalTtc,
                        totalAchat,
                        totalToAccount,
                        null
                    );
                })
                .toList();
        }
    }

    private List<TvaChartDataDTO> buildChartData(List<TvaRateBreakdownDTO> breakdown, long totalTtc) {
        // Group by rate for chart (ignoring dates)
        Map<Integer, Long> grouped = breakdown.stream()
            .collect(Collectors.groupingBy(
                TvaRateBreakdownDTO::codeTva,
                Collectors.summingLong(TvaRateBreakdownDTO::montantTtc)
            ));

        List<TvaChartDataDTO> chartData = new ArrayList<>();
        int colorIndex = 0;

        for (Map.Entry<Integer, Long> entry : grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList()) {

            double percent = totalTtc > 0
                ? Math.round((entry.getValue() * 100.0 / totalTtc) * 10.0) / 10.0
                : 0.0;

            chartData.add(new TvaChartDataDTO(
                entry.getKey() + "%",
                entry.getValue(),
                percent,
                TvaChartDataDTO.getColor(colorIndex++)
            ));
        }

        return chartData;
    }

    private String buildPeriodLabel(LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now();

        if (fromDate.equals(toDate)) {
            if (fromDate.equals(today)) {
                return "Aujourd'hui";
            } else if (fromDate.equals(today.minusDays(1))) {
                return "Hier";
            } else {
                return fromDate.format(DATE_FORMATTER);
            }
        } else {
            return fromDate.format(DATE_FORMATTER) + " - " + toDate.format(DATE_FORMATTER);
        }
    }
}
