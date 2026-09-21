package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.CategorieTransaction;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;
import com.kobe.warehouse.service.dto.mobile.CashMovementDTO;
import com.kobe.warehouse.service.dto.mobile.CategoryBalanceDTO;
import com.kobe.warehouse.service.dto.mobile.MobileCashBalanceDTO;
import com.kobe.warehouse.service.dto.mobile.PaymentModeBreakdownDTO;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for mobile cash balance report (Balance Caisse).
 * Transforms the existing BalanceCaisseService data into mobile-friendly format.
 */
@Service
@Transactional(readOnly = true)
public class MobileCashBalanceService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Libellés d'affichage des types de transaction que le domaine classe en sortie de caisse. */
    private static final Set<String> CLES_SORTIE = Arrays.stream(TypeFinancialTransaction.values())
        .filter(type -> type.getCategorieTransaction() == CategorieTransaction.SORTIE_CAISSE)
        .map(type -> type.getTransactionTypeAffichage().name())
        .collect(Collectors.toUnmodifiableSet());

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
                "Règlement mobile",
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

        long credit = wrapper.getMontantCredit() + wrapper.getMontantDiffere() + wrapper.getPartTiersPayant();
        /*if (wrapper.getMontantCredit() > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "CREDIT",
                "Crédit",
                wrapper.getMontantCredit(),
                calculatePercent(wrapper.getMontantCredit(), total),
                PaymentModeBreakdownDTO.getColorForMode("CREDIT")
            ));
        }*/

        // Différé
       /* if (wrapper.getMontantDiffere() > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "DIFFERE",
                "Différé",
                wrapper.getMontantDiffere(),
                calculatePercent(wrapper.getMontantDiffere(), total),
                PaymentModeBreakdownDTO.getColorForMode("DIFFERE")
            ));
        }*/

        // Tiers payant
        if (credit > 0) {
            breakdown.add(new PaymentModeBreakdownDTO(
                "CREDIT",
                "Crédit",
                credit,
                calculatePercent(credit, total),
                PaymentModeBreakdownDTO.getColorForMode("CREDIT")
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
                    isSortie(mvt.key(), valueAsLong) ? CashMovementDTO.TYPE_SORTIE : CashMovementDTO.TYPE_ENTREE,
                    null, // Date not available from Tuple
                    null
                );
            })
            .toList();
    }

    /**
     * Le sens du mouvement se lit sur la clé, non sur le signe : les montants remontés sont des
     * sommes de règlements, toujours positives, sortie de caisse comprise. Le sens est celui que
     * le domaine attribue au type de transaction — la même autorité que le rapport d'activité.
     */
    private boolean isSortie(String key, long value) {
        return CLES_SORTIE.contains(key) || value < 0;
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
            case ThirdPartySales -> "VO";
            case CashSale -> "VNO";
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
