package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.service.dto.mobile.CashierRecapDTO;
import com.kobe.warehouse.service.dto.mobile.MobileCashSummaryDTO;
import com.kobe.warehouse.service.dto.mobile.SummaryItemDTO;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZData;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;
import com.kobe.warehouse.service.tiketz.dto.TicketZRecap;
import com.kobe.warehouse.service.tiketz.service.TicketZService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Service for mobile cash summary (Ticket Z / Récapitulatif Caisse).
 */
@Service
@Transactional(readOnly = true)
public class MobileCashSummaryService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Keys for extracting totals from labels
    private static final String KEY_ESPECES = "ESPÈCES";
    private static final String KEY_CASH = "CASH";
    private static final String KEY_CB = "CB";
    private static final String KEY_CARTES = "CARTES";
    private static final String KEY_CHEQUE = "CHÈQUE";
    private static final String KEY_CH = "CH";
    private static final String KEY_VIREMENT = "VIREMENT";
    private static final String KEY_CREDIT = "CRÉDIT";
    private static final String KEY_MOBILE = "MOBILE";
    private static final String KEY_OM = "OM";
    private static final String KEY_MTN = "MTN";
    private static final String KEY_MOOV = "MOOV";
    private static final String KEY_WAVE = "WAVE";
    private static final String KEY_TOTAL = "TOTAL";

    private final TicketZService ticketZService;

    public MobileCashSummaryService(TicketZService ticketZService) {
        this.ticketZService = ticketZService;
    }

    /**
     * Get cash summary for the given date range.
     *
     * @param fromDate Start date
     * @param toDate   End date (defaults to fromDate if null)
     * @param fromTime Start time (optional)
     * @param toTime   End time (optional)
     * @param userIds  Filter by specific users (optional)
     * @param onlyVente If true, only include sales payments
     * @return Mobile cash summary DTO
     */
    public MobileCashSummaryDTO getCashSummary(
            LocalDate fromDate,
            LocalDate toDate,
            LocalTime fromTime,
            LocalTime toTime,
            Set<Integer> userIds,
            boolean onlyVente) {

        if (toDate == null) {
            toDate = fromDate;
        }

        // Create TicketZ param
        TicketZParam param = new TicketZParam(
            userIds != null ? userIds : Set.of(),
            onlyVente,
            fromDate,
            toDate,
            fromTime,
            toTime
        );

        // Get TicketZ data from existing service
        TicketZ ticketZ = ticketZService.getTicketZ(param);

        // Transform to mobile format
        return transformToMobileFormat(ticketZ, fromDate, toDate, fromTime, toTime);
    }

    private MobileCashSummaryDTO transformToMobileFormat(
            TicketZ ticketZ,
            LocalDate fromDate,
            LocalDate toDate,
            LocalTime fromTime,
            LocalTime toTime) {

        // Convert global summary
        List<SummaryItemDTO> globalSummary = transformSummaryItems(ticketZ.summaries());

        // Convert cashier recaps
        List<CashierRecapDTO> cashierRecaps = ticketZ.datas().stream()
            .map(this::transformCashierRecap)
            .toList();

        // Calculate totals
        Totals totals = calculateTotals(ticketZ);

        // Build period label
        String periodLabel = buildPeriodLabel(fromDate, toDate, fromTime, toTime);

        return MobileCashSummaryDTO.builder()
            .fromDate(fromDate)
            .toDate(toDate)
            .fromTime(fromTime)
            .toTime(toTime)
            .periodLabel(periodLabel)
            .globalSummary(globalSummary)
            .cashierRecaps(cashierRecaps)
            .totalTtc(totals.totalTtc)
            .totalEspeces(totals.especes)
            .totalCartes(totals.cartes)
            .totalMobileMoney(totals.mobileMoney)
            .totalCheques(totals.cheques)
            .totalVirements(totals.virements)
            .totalCredit(totals.credit)
            .totalMobile(totals.totalMobile)
            .cashierCount(cashierRecaps.size())
            .build();
    }

    private List<SummaryItemDTO> transformSummaryItems(List<TicketZData> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
            .map(data -> new SummaryItemDTO(
                data.libelle(),
                data.value(),
                data.secondValue()
            ))
            .toList();
    }

    private CashierRecapDTO transformCashierRecap(TicketZRecap recap) {
        List<SummaryItemDTO> details = transformSummaryItems(recap.datas());
        List<SummaryItemDTO> summary = transformSummaryItems(recap.summary());

        return CashierRecapDTO.of(
            recap.userId(),
            recap.userName(),
            details,
            summary
        );
    }

    private Totals calculateTotals(TicketZ ticketZ) {
        Totals totals = new Totals();

        // Calculate from global summary first
        for (TicketZData data : ticketZ.summaries()) {
            categorizeAmount(data.libelle().toUpperCase(), data.value(), totals);
        }

        // If no global summary, sum from individual cashier data
        if (ticketZ.summaries().isEmpty() && !ticketZ.datas().isEmpty()) {
            for (TicketZRecap recap : ticketZ.datas()) {
                for (TicketZData data : recap.datas()) {
                    categorizeAmount(data.libelle().toUpperCase(), data.value(), totals);
                }
            }
        }

        // Calculate total TTC
        totals.totalTtc = totals.especes + totals.cartes + totals.cheques +
                          totals.virements + totals.mobileMoney + totals.credit;

        return totals;
    }

    private void categorizeAmount(String label, long value, Totals totals) {
        if (label.contains(KEY_ESPECES) || label.contains(KEY_CASH)) {
            totals.especes += value;
        } else if (label.contains(KEY_CB) || label.contains(KEY_CARTES)) {
            totals.cartes += value;
        } else if (label.contains(KEY_CHEQUE) || label.equals(KEY_CH)) {
            totals.cheques += value;
        } else if (label.contains(KEY_VIREMENT)) {
            totals.virements += value;
        } else if (label.contains(KEY_CREDIT)) {
            totals.credit += value;
        } else if (label.contains(KEY_OM) || label.contains(KEY_MTN) ||
                   label.contains(KEY_MOOV) || label.contains(KEY_WAVE)) {
            totals.mobileMoney += value;
        } else if (label.contains(KEY_MOBILE) && label.contains(KEY_TOTAL)) {
            totals.totalMobile = value;
        }
    }

    private String buildPeriodLabel(LocalDate fromDate, LocalDate toDate, LocalTime fromTime, LocalTime toTime) {
        StringBuilder sb = new StringBuilder();
        LocalDate today = LocalDate.now();

        if (fromDate.equals(toDate)) {
            if (fromDate.equals(today)) {
                sb.append("Aujourd'hui");
            } else if (fromDate.equals(today.minusDays(1))) {
                sb.append("Hier");
            } else {
                sb.append(fromDate.format(DATE_FORMATTER));
            }
        } else {
            sb.append(fromDate.format(DATE_FORMATTER));
            sb.append(" - ");
            sb.append(toDate.format(DATE_FORMATTER));
        }

        // Add time range if not full day
        if (fromTime != null && toTime != null) {
            boolean isFullDay = fromTime.equals(LocalTime.MIN) &&
                               (toTime.equals(LocalTime.MAX) || toTime.equals(LocalTime.of(23, 59, 59)));
            if (!isFullDay) {
                sb.append(" (");
                sb.append(fromTime.format(TIME_FORMATTER));
                sb.append(" - ");
                sb.append(toTime.format(TIME_FORMATTER));
                sb.append(")");
            }
        }

        return sb.toString();
    }

    /**
     * Internal class to accumulate totals
     */
    private static class Totals {
        long totalTtc = 0;
        long especes = 0;
        long cartes = 0;
        long mobileMoney = 0;
        long cheques = 0;
        long virements = 0;
        long credit = 0;
        long totalMobile = 0;
    }
}
