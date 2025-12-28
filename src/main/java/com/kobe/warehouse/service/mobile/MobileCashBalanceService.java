package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;
import com.kobe.warehouse.service.dto.mobile.*;
import com.kobe.warehouse.service.dto.records.Tuple;
import com.kobe.warehouse.service.financiel_transaction.BalanceCaisseService;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for mobile cash balance report (Balance Caisse).
 * Transforms the existing BalanceCaisseService data into mobile-friendly format.
 */
@Service
@Transactional(readOnly = true)
public class MobileCashBalanceService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BalanceCaisseService balanceCaisseService;

    public MobileCashBalanceService(BalanceCaisseService balanceCaisseService) {
        this.balanceCaisseService = balanceCaisseService;
    }

    /**
     * Get cash balance report for the given date range.
     *
     * @param fromDate Start date
     * @param toDate   End date (defaults to fromDate if null)
     * @return Mobile cash balance DTO
     */
    public MobileCashBalanceDTO getCashBalance(LocalDate fromDate, LocalDate toDate) {
        if (toDate == null) {
            toDate = fromDate;
        }

        // Build params for the existing service
        MvtParam params = new MvtParam();
        params.setFromDate(fromDate);
        params.setToDate(toDate);
        params.setStatuts(Set.of(SalesStatut.CLOSED));

        // Get balance from existing service
        BalanceCaisseWrapper wrapper = balanceCaisseService.getBalanceCaisse(params.build());

        if (wrapper == null) {
            return MobileCashBalanceDTO.empty(fromDate, toDate, buildPeriodLabel(fromDate, toDate));
        }

        return buildCashBalance(fromDate, toDate, wrapper);
    }

    private MobileCashBalanceDTO buildCashBalance(
            LocalDate fromDate,
            LocalDate toDate,
            BalanceCaisseWrapper wrapper) {

        String periodLabel = buildPeriodLabel(fromDate, toDate);

        // Build payment breakdown for pie chart
        List<PaymentModeBreakdownDTO> paymentBreakdown = buildPaymentBreakdown(wrapper);

        // Build category balances
        List<CategoryBalanceDTO> categoryBalances = buildCategoryBalances(wrapper.getBalanceCaisses());

        // Build cash movements from mvtCaisses
        List<CashMovementDTO> cashMovements = buildCashMovements(wrapper.getMvtCaisses());

        return new MobileCashBalanceDTO(
            fromDate,
            toDate,
            periodLabel,
            (int) wrapper.getCount(),
            wrapper.getMontantTtc(),
            wrapper.getMontantHt(),
            wrapper.getMontantNet(),
            wrapper.getMontantDiscount(),
            wrapper.getMontantTaxe(),
            wrapper.getPanierMoyen(),
            wrapper.getMontantCash(),
            wrapper.getMontantCard(),
            wrapper.getMontantCheck(),
            wrapper.getMontantVirement(),
            wrapper.getMontantMobileMoney(),
            wrapper.getMontantCredit(),
            wrapper.getMontantDiffere(),
            wrapper.getPartTiersPayant(),
            wrapper.getMontantAchat(),
            wrapper.getMontantMarge(),
            wrapper.getRatioVenteAchat(),
            wrapper.getRatioAchatVente(),
            paymentBreakdown,
            categoryBalances,
            cashMovements
        );
    }

    private List<PaymentModeBreakdownDTO> buildPaymentBreakdown(BalanceCaisseWrapper wrapper) {
        List<PaymentModeBreakdownDTO> breakdown = new ArrayList<>();
        long total = wrapper.getMontantTtc();

        // Espèces
        if (wrapper.getMontantCash() > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "ESPECES",
                "Espèces",
                wrapper.getMontantCash(),
                calculatePercent(wrapper.getMontantCash(), total),
                PaymentModeBreakdownDTO.getColorForMode("ESPECES")
            ));
        }

        // Cartes
        if (wrapper.getMontantCard() > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "CARTE",
                "Cartes bancaires",
                wrapper.getMontantCard(),
                calculatePercent(wrapper.getMontantCard(), total),
                PaymentModeBreakdownDTO.getColorForMode("CARTE")
            ));
        }

        // Mobile Money
        if (wrapper.getMontantMobileMoney() > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "MOBILE_MONEY",
                "Mobile Money",
                wrapper.getMontantMobileMoney(),
                calculatePercent(wrapper.getMontantMobileMoney(), total),
                PaymentModeBreakdownDTO.getColorForMode("MOBILE_MONEY")
            ));
        }

        // Chèques
        if (wrapper.getMontantCheck() > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "CHEQUE",
                "Chèques",
                wrapper.getMontantCheck(),
                calculatePercent(wrapper.getMontantCheck(), total),
                PaymentModeBreakdownDTO.getColorForMode("CHEQUE")
            ));
        }

        // Virements
        if (wrapper.getMontantVirement() > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "VIREMENT",
                "Virements",
                wrapper.getMontantVirement(),
                calculatePercent(wrapper.getMontantVirement(), total),
                PaymentModeBreakdownDTO.getColorForMode("VIREMENT")
            ));
        }

        // Crédit
        if (wrapper.getMontantCredit() > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "CREDIT",
                "Crédit",
                wrapper.getMontantCredit(),
                calculatePercent(wrapper.getMontantCredit(), total),
                PaymentModeBreakdownDTO.getColorForMode("CREDIT")
            ));
        }

        // Différé
        if (wrapper.getMontantDiffere() > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "DIFFERE",
                "Différé",
                wrapper.getMontantDiffere(),
                calculatePercent(wrapper.getMontantDiffere(), total),
                PaymentModeBreakdownDTO.getColorForMode("DIFFERE")
            ));
        }

        // Tiers payant
        if (wrapper.getPartTiersPayant() > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "TIERS_PAYANT",
                "Tiers payant",
                wrapper.getPartTiersPayant(),
                calculatePercent(wrapper.getPartTiersPayant(), total),
                PaymentModeBreakdownDTO.getColorForMode("TIERS_PAYANT")
            ));
        }

        return breakdown;
    }

    private List<CategoryBalanceDTO> buildCategoryBalances(List<BalanceCaisseDTO> balanceCaisses) {
        if (balanceCaisses == null || balanceCaisses.isEmpty()) {
            return List.of();
        }

        return balanceCaisses.stream()
            .map(bc -> new CategoryBalanceDTO(
                bc.getTypeSale() != null ? bc.getTypeSale().getValue() : "AUTRE",
                getCategoryLabel(bc.getTypeSale()),
                bc.getCount() != null ? bc.getCount().intValue() : 0,
                bc.getMontantTtc(),
                bc.getMontantHt(),
                bc.getMontantNet(),
                bc.getMontantDiscount(),
                bc.getMontantTaxe(),
                bc.getMontantAchat(),
                bc.getMontantMarge(),
                bc.getPanierMoyen(),
                bc.getMontantCash(),
                bc.getMontantCard(),
                bc.getMontantCheck(),
                bc.getMontantVirement(),
                bc.getMontantMobileMoney(),
                bc.getMontantCredit(),
                bc.getMontantDiffere(),
                bc.getPartTiersPayant()
            ))
            .toList();
    }

    private List<CashMovementDTO> buildCashMovements(List<Tuple> mvtCaisses) {
        if (mvtCaisses == null || mvtCaisses.isEmpty()) {
            return List.of();
        }

        return mvtCaisses.stream()
            .map(mvt -> {
                long valueAsLong = convertToLong(mvt.value());
                return new CashMovementDTO(
                    0L, // ID not available from Tuple
                    mvt.libelle(),
                    Math.abs(valueAsLong),
                    valueAsLong >= 0 ? CashMovementDTO.TYPE_ENTREE : CashMovementDTO.TYPE_SORTIE,
                    null, // Date not available from Tuple
                    null
                );
            })
            .toList();
    }

    private long convertToLong(Object value) {
        if (value == null) return 0L;
        return switch (value) {
            case Long l -> l;
            case Integer i -> i.longValue();
            case Double d -> d.longValue();
            case BigDecimal bd -> bd.longValue();
            case Number n -> n.longValue();
            default -> 0L;
        };
    }

    private String getCategoryLabel(TypeVenteDTO typeSale) {
        if (typeSale == null) {
            return "Autre";
        }
        return switch (typeSale) {
            case ThirdPartySales -> "Vente Ordonnance";
            case CashSale -> "Vente Non Ordonnance";
            case VenteDepot -> "Ventes Dépôts";
        };
    }

    private double calculatePercent(long value, long total) {
        if (total == 0) return 0.0;
        return Math.round((value * 100.0 / total) * 10.0) / 10.0;
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
