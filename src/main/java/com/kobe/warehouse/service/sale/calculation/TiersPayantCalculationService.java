package com.kobe.warehouse.service.sale.calculation;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OptionPrixType;
import com.kobe.warehouse.service.sale.calculation.dto.CalculatedShare;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationInput;
import com.kobe.warehouse.service.sale.calculation.dto.CalculationResult;
import com.kobe.warehouse.service.sale.calculation.dto.Rate;
import com.kobe.warehouse.service.sale.calculation.dto.SaleItemInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantInput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantLineOutput;
import com.kobe.warehouse.service.sale.calculation.dto.TiersPayantPrixInput;
import com.kobe.warehouse.service.sale.calculation.dto.TvaRepartitionDto;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Optimized service for calculating third-party payment (Tiers Payant) amounts with VAT repartitions.
 *
 * <p>This is version 2 of the calculation service, implementing the following improvements:
 * <ul>
 *   <li>Fixed duplicate ceiling calculation bug in computePlafondClient</li>
 *   <li>Fixed incorrect totalAmountAssurance recalculation</li>
 *   <li>Added null safety checks for consumption values</li>
 *   <li>Consistent use of BigDecimal for all monetary calculations (no int/float conversions)</li>
 *   <li>Separated pure calculation from state modification</li>
 *   <li>Added VAT repartition tracking for each third-party line</li>
 *   <li>Comprehensive documentation</li>
 * </ul>
 *
 * <p><strong>Important:</strong> All monetary values use BigDecimal with HALF_UP rounding mode.
 * Final amounts are rounded to 0 decimal places (whole currency units).
 *
 *
 */
@Service
public class TiersPayantCalculationService {

    private static final int SCALE_MONEY = 0;
    private static final int SCALE_RATE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * Calculates third-party payment distribution for a sale.
     *
     * @param input calculation input containing sale items and third-party configurations
     * @return calculation result with amounts per third-party and per patient
     */
    public CalculationResult calculate(CalculationInput input) {
        if (CollectionUtils.isEmpty(input.getSaleItems())) {
            return null;
        }

        CalculationResult result = new CalculationResult();
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        Map<Integer, BigDecimal> tiersPayantAmounts = new HashMap<>();
        Map<Integer, Map<Integer, TvaRepartitionDto>> tiersPayantTvaRepartitions = new HashMap<>();

        // Process each sale item
        for (SaleItemInput saleItem : input.getSaleItems()) {
            CalculatedShare itemShare = calculateSaleItem(saleItem, input.getTiersPayants(), input.getNatureVente());
            totalDiscountAmount = totalDiscountAmount.add(saleItem.getDiscountAmount());

            // Aggregate amounts per third-party
            itemShare.getTiersPayants().forEach((clientTiersPayantId, amount) ->
                tiersPayantAmounts.merge(clientTiersPayantId, amount, BigDecimal::add)
            );

            // Aggregate TVA repartitions per third-party
            itemShare.getTvaRepartitionsByTiersPayant().forEach((clientTiersPayantId, repartitions) -> {
                Map<Integer, TvaRepartitionDto> tvaMap = tiersPayantTvaRepartitions
                    .computeIfAbsent(clientTiersPayantId, k -> new HashMap<>());

                for (TvaRepartitionDto repartition : repartitions) {
                    tvaMap.computeIfAbsent(repartition.getTva(), TvaRepartitionDto::new)
                        .add(repartition);
                }
            });

            result.getItemShares().add(itemShare);
        }

        result.setDiscountAmount(totalDiscountAmount.setScale(SCALE_MONEY, ROUNDING_MODE));
        result.setTotalSaleAmount(input.getTotalSalesAmount().setScale(SCALE_MONEY, ROUNDING_MODE));

        // Apply ceilings and create third-party line outputs
        BigDecimal totalTiersPayantAmount = BigDecimal.ZERO;
        List<TiersPayantLineOutput> lineOutputs = new ArrayList<>();
        StringBuilder warnings = new StringBuilder();

        for (TiersPayantInput tpInput : input.getTiersPayants()) {
            BigDecimal rawAmount = tiersPayantAmounts.getOrDefault(tpInput.getClientTiersPayantId(), BigDecimal.ZERO);
            rawAmount = rawAmount.setScale(SCALE_MONEY, ROUNDING_MODE);

            // Apply all ceilings (monthly consumption, daily client, etc.)
            CeilingApplicationResult ceilingResult = applyCeilings(rawAmount, tpInput);
            BigDecimal cappedAmount = ceilingResult.cappedAmount().setScale(SCALE_MONEY, ROUNDING_MODE);

            if (ceilingResult.wasCapped()) {
                warnings.append(String.format(
                    "Le montant remboursé pour le tiers payant %s a été plafonné à %s.%n",
                    tpInput.getTiersPayantFullName(),
                    cappedAmount
                ));
            }

            totalTiersPayantAmount = totalTiersPayantAmount.add(cappedAmount);

            // Create line output with TVA repartitions
            TiersPayantLineOutput lineOutput = new TiersPayantLineOutput();
            lineOutput.setClientTiersPayantId(tpInput.getClientTiersPayantId());
            lineOutput.setMontant(cappedAmount);
            lineOutput.setFinalTaux(calculateFinalTaux(cappedAmount, result.getTotalSaleAmount()));

            // Add TVA repartitions for this third-party
            Map<Integer, TvaRepartitionDto> tvaMap = tiersPayantTvaRepartitions.get(tpInput.getClientTiersPayantId());
            if (tvaMap != null) {
                lineOutput.setRepartitions(new ArrayList<>(tvaMap.values()));
            }

            lineOutputs.add(lineOutput);
        }

        result.setTotalTiersPayant(totalTiersPayantAmount.setScale(SCALE_MONEY, ROUNDING_MODE));
        result.setTiersPayantLines(lineOutputs);
        result.setWarningMessage(warnings.toString());

        // Calculate patient share (pure calculation, no state modification)
        BigDecimal patientShare = calculatePatientShare(
            result.getTotalSaleAmount(),
            totalTiersPayantAmount,
            result.getDiscountAmount(),
            input.getNatureVente()
        );
        result.setTotalPatientShare(patientShare);

        return result;
    }

    /**
     * Calculates the share for a single sale item, distributing amounts across third-parties.
     *
     * @param saleItem the sale item to calculate
     * @param tiersPayantInputs list of third-party configurations (will be sorted by priority)
     * @param natureVente the nature of the sale (ASSURANCE, CARNET, etc.)
     * @return calculated share with amounts per third-party and TVA repartitions
     */
    private CalculatedShare calculateSaleItem(
        SaleItemInput saleItem,
        List<TiersPayantInput> tiersPayantInputs,
        NatureVente natureVente
    ) {
        // Sort by priority (lower value = higher priority)
        // Create a mutable copy to avoid UnsupportedOperationException on immutable lists
        List<TiersPayantInput> sortedTiersPayants = new ArrayList<>(tiersPayantInputs);
        sortedTiersPayants.sort(Comparator.comparingInt(tp -> tp.getPriorite().getValue()));

        CalculatedShare itemShare = new CalculatedShare();
        itemShare.setPharmacyPrice(saleItem.getRegularUnitPrice());
        itemShare.setSaleLineId(saleItem.getSalesLineId());
        itemShare.setDiscountAmount(saleItem.getDiscountAmount());
        itemShare.setTotalSalesAmount(saleItem.getTotalSalesAmount());

        // Determine reference price (minimum non-percentage option price)
        Optional<BigDecimal> referencePriceOpt = saleItem.getPrixAssurances().stream()
            .filter(p -> p.getOptionPrixType() != OptionPrixType.POURCENTAGE)
            .map(p -> new BigDecimal(p.getPrice()))
            .min(BigDecimal::compareTo);

        boolean hasOptionPrix = !saleItem.getPrixAssurances().isEmpty();
        BigDecimal calculationBaseUni = referencePriceOpt.orElse(itemShare.getPharmacyPrice());
        BigDecimal calculationBase = calculationBaseUni.multiply(new BigDecimal(saleItem.getQuantity()));

        if (referencePriceOpt.isPresent()) {
            itemShare.setCalculationBasePrice(calculationBaseUni.intValue());
        }

        // Distribute amounts across third-parties
        BigDecimal totalPartTiersPayant = BigDecimal.ZERO;

        for (TiersPayantInput tiersPayantInput : sortedTiersPayants) {
            BigDecimal rate = BigDecimal.valueOf(tiersPayantInput.getTaux()).setScale(SCALE_RATE, ROUNDING_MODE);

            // Check for custom rate/price for this third-party
            if (hasOptionPrix) {
                Optional<TiersPayantPrixInput> customPriceOpt = saleItem.getPrixAssurances().stream()
                    .filter(p -> p.getCompteTiersPayantId().equals(tiersPayantInput.getClientTiersPayantId()))
                    .findFirst();

                if (customPriceOpt.isPresent() && customPriceOpt.get().getOptionPrixType() != OptionPrixType.REFERENCE) {
                    TiersPayantPrixInput customPrice = customPriceOpt.get();
                    rate = BigDecimal.valueOf(customPrice.getRate())
                        .divide(ONE_HUNDRED, SCALE_RATE, ROUNDING_MODE);
                    itemShare.getRates().add(new Rate(tiersPayantInput.getClientTiersPayantId(), rate.floatValue()));
                }
            }

            // Calculate remaining amount available for this third-party
            BigDecimal remainingAmount = calculationBase.subtract(totalPartTiersPayant);
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break; // Nothing left to distribute
            }

            BigDecimal actualShare;

            // Special handling for 100% rate with ASSURANCE nature (formula confort)
            if (rate.compareTo(BigDecimal.ONE) == 0 && natureVente == NatureVente.ASSURANCE) {
                // Use remaining from total sales amount instead of calculation base
                remainingAmount = saleItem.getTotalSalesAmount().subtract(totalPartTiersPayant);
                actualShare = remainingAmount.max(BigDecimal.ZERO);
            } else {
                actualShare = calculationBase.multiply(rate).min(remainingAmount);
            }

            totalPartTiersPayant = totalPartTiersPayant.add(actualShare);
            itemShare.getTiersPayants().put(tiersPayantInput.getClientTiersPayantId(), actualShare);

            // Calculate TVA repartition for this third-party share
            TvaRepartitionDto tvaRepartition = calculateTvaRepartition(
                actualShare,
                saleItem.getTvaRate()
            );

            itemShare.getTvaRepartitionsByTiersPayant()
                .computeIfAbsent(tiersPayantInput.getClientTiersPayantId(), k -> new ArrayList<>())
                .add(tvaRepartition);
        }

        itemShare.setTotalReimbursedAmount(totalPartTiersPayant);
        return itemShare;
    }

    /**
     * Calculates TVA repartition for a given amount.
     *
     * @param montantTtc the amount including VAT (TTC)
     * @param tvaRate the VAT rate in percentage (e.g., 5, 10, 20)
     * @return TVA repartition breakdown
     */
    private TvaRepartitionDto calculateTvaRepartition(BigDecimal montantTtc, int tvaRate) {
        TvaRepartitionDto repartition = new TvaRepartitionDto(tvaRate);

        if (tvaRate == 0) {
            repartition.setMontantTtc(montantTtc);
            repartition.setMontantNet(montantTtc);
            repartition.setMontantHt(montantTtc);
            repartition.setMontantTva(BigDecimal.ZERO);
            return repartition;
        }

        BigDecimal tvaRateBD = new BigDecimal(tvaRate).divide(ONE_HUNDRED, SCALE_RATE, ROUNDING_MODE);
        BigDecimal divisor = BigDecimal.ONE.add(tvaRateBD);

        BigDecimal montantHt = montantTtc.divide(divisor, SCALE_MONEY, ROUNDING_MODE);
        BigDecimal montantTva = montantTtc.subtract(montantHt);

        repartition.setMontantTtc(montantTtc);
        repartition.setMontantHt(montantHt);
        repartition.setMontantTva(montantTva);
        repartition.setMontantNet(montantTtc); // Net = TTC for display purposes

        return repartition;
    }

    /**
     * Applies all ceilings (plafonds) to a third-party amount.
     *
     * <p>Ceilings are applied in the following order:
     * <ol>
     *   <li>Monthly consumption ceiling (plafond de consommation mensuelle)</li>
     *   <li>Daily client ceiling (plafond journalier client)</li>
     * </ol>
     *
     * @param rawAmount the raw amount before ceilings
     * @param tp the third-party configuration with ceiling values
     * @return result containing capped amount and whether capping occurred
     */
    private CeilingApplicationResult applyCeilings(BigDecimal rawAmount, TiersPayantInput tp) {
        BigDecimal originalAmount = rawAmount;

        // Apply monthly consumption ceiling
        BigDecimal afterMonthlyCeiling = applyMonthlyCeiling(
            rawAmount,
            tp.getPlafondConso(),
            tp.getConsoMensuelle()
        );

        // Apply daily client ceiling
        BigDecimal finalAmount = applyDailyCeiling(
            afterMonthlyCeiling,
            tp.getPlafondJournalierClient()
        );

        boolean wasCapped = finalAmount.compareTo(originalAmount) != 0;
        return new CeilingApplicationResult(finalAmount, wasCapped);
    }

    /**
     * Applies monthly consumption ceiling.
     *
     * @param amount the amount to cap
     * @param plafond the monthly ceiling (null if no ceiling)
     * @param conso the current monthly consumption (must not be null if plafond is not null)
     * @return capped amount
     */
    private BigDecimal applyMonthlyCeiling(BigDecimal amount, BigDecimal plafond, BigDecimal conso) {
        if (plafond == null) {
            return amount;
        }

        // Null safety: if consumption is null, treat as zero
        BigDecimal currentConso = (conso != null) ? conso : BigDecimal.ZERO;

        // Already at or over ceiling
        if (currentConso.compareTo(plafond) >= 0) {
            return BigDecimal.ZERO;
        }

        // Calculate remaining available before ceiling
        BigDecimal remaining = plafond.subtract(currentConso);

        // Return minimum of requested amount and remaining
        return amount.min(remaining);
    }

    /**
     * Applies daily client ceiling.
     *
     * @param amount the amount to cap
     * @param plafondJournalier the daily ceiling (null if no ceiling)
     * @return capped amount
     */
    private BigDecimal applyDailyCeiling(BigDecimal amount, BigDecimal plafondJournalier) {
        if (plafondJournalier == null) {
            return amount;
        }
        return amount.min(plafondJournalier);
    }

    /**
     * Calculates the patient's share based on sale nature.
     *
     * <p>This is a pure calculation method with no side effects.
     *
     * @param totalSaleAmount total sale amount
     * @param totalTiersPayant total third-party amount
     * @param discountAmount discount amount
     * @param nature nature of the sale
     * @return patient's share (never negative)
     */
    private BigDecimal calculatePatientShare(
        BigDecimal totalSaleAmount,
        BigDecimal totalTiersPayant,
        BigDecimal discountAmount,
        NatureVente nature
    ) {
        return switch (nature) {
            case ASSURANCE -> {
                // Patient pays: total - third-party - discount
                BigDecimal patientPart = totalSaleAmount
                    .subtract(totalTiersPayant)
                    .subtract(discountAmount)
                    .setScale(SCALE_MONEY, ROUNDING_MODE);
                yield patientPart.max(BigDecimal.ZERO);
            }
            case CARNET -> {
                // For CARNET: discount applies to third-party amount
                // Net amount = total - discount
                // Patient pays: net - third-party
                BigDecimal netAmount = totalSaleAmount
                    .subtract(discountAmount)
                    .setScale(SCALE_MONEY, ROUNDING_MODE);
                BigDecimal patientPart = netAmount
                    .subtract(totalTiersPayant)
                    .setScale(SCALE_MONEY, ROUNDING_MODE);
                yield patientPart.max(BigDecimal.ZERO);
            }
            default -> totalSaleAmount.setScale(SCALE_MONEY, ROUNDING_MODE);
        };
    }

    /**
     * Calculates the final rate (taux) as a percentage.
     * Uses HALF_DOWN rounding to avoid systematic over-estimation.
     * Calculation: (actualShare * 100 / totalAmount) rounded to nearest integer
     *
     * Example:
     * - 1141 / 1755 * 100 = 65.0142... → 65 (HALF_DOWN)
     * - 263 / 1755 * 100 = 14.9857... → 15 (HALF_DOWN)
     * - 1525 / 3000 * 100 = 50.8333... → 51 (HALF_DOWN)
     *
     * @param actualShare the actual third-party share
     * @param totalAmount the total amount
     * @return rate as integer percentage (0-100)
     */
    private int calculateFinalTaux(BigDecimal actualShare, BigDecimal totalAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        // Calculate percentage with high precision (SCALE_RATE = 4 decimals)
        // then round to integer with HALF_DOWN
        return actualShare
            .multiply(ONE_HUNDRED)
            .divide(totalAmount, SCALE_RATE, RoundingMode.HALF_DOWN)
            .setScale(SCALE_MONEY, RoundingMode.HALF_DOWN)
            .intValue();
    }

    /**
     * Result of applying ceilings to an amount.
     *
     * @param cappedAmount the amount after applying all ceilings
     * @param wasCapped whether the amount was reduced by ceilings
     */
    private record CeilingApplicationResult(BigDecimal cappedAmount, boolean wasCapped) {}
}
