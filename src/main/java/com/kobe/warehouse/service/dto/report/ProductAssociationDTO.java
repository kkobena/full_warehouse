package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Product Association for Market Basket Analysis
 * Represents products frequently bought together
 */
public record ProductAssociationDTO(
    Long productAId,
    String productAName,
    String productACodeCip,
    Long productBId,
    String productBName,
    String productBCodeCip,
    Long transactionsWithBoth,
    Long transactionsWithA,
    Long transactionsWithB,
    BigDecimal support,      // % of transactions containing both products
    BigDecimal confidence,   // % of transactions with A that also have B
    BigDecimal lift          // Likelihood that B is bought when A is bought (lift > 1 = positive correlation)
) {
    /**
     * Creates a ProductAssociationDTO with calculated metrics
     */
    public static ProductAssociationDTO create(
        Long productAId,
        String productAName,
        String productACodeCip,
        Long productBId,
        String productBName,
        String productBCodeCip,
        Long transactionsWithBoth,
        Long transactionsWithA,
        Long transactionsWithB,
        Long totalTransactions
    ) {
        BigDecimal support = BigDecimal.valueOf(transactionsWithBoth)
            .divide(BigDecimal.valueOf(totalTransactions), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        BigDecimal confidence = BigDecimal.valueOf(transactionsWithBoth).divide(BigDecimal.valueOf(transactionsWithA), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        BigDecimal expectedProbability = BigDecimal.valueOf(transactionsWithB)
            .divide(BigDecimal.valueOf(totalTransactions), 4, RoundingMode.HALF_UP);

        BigDecimal actualProbability = BigDecimal.valueOf(transactionsWithBoth)
            .divide(BigDecimal.valueOf(transactionsWithA), 4, RoundingMode.HALF_UP);

        BigDecimal lift = actualProbability.divide(expectedProbability, 4, RoundingMode.HALF_UP);

        return new ProductAssociationDTO(
            productAId,
            productAName,
            productACodeCip,
            productBId,
            productBName,
            productBCodeCip,
            transactionsWithBoth,
            transactionsWithA,
            transactionsWithB,
            support,
            confidence,
            lift
        );
    }
}
