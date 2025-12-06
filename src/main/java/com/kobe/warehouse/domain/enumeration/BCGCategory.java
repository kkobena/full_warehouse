package com.kobe.warehouse.domain.enumeration;

/**
 * BCG Matrix Category for product classification
 * Based on margin percentage and rotation rate
 */
public enum BCGCategory {
    /**
     * Stars: High margin (>= 20%) + High rotation (>= 6x/year)
     * Strategic products - invest to maintain position
     */
    STAR,

    /**
     * Cash Cows: High margin (>= 20%) + Low rotation (< 6x/year)
     * Profitable stable products - maintain and harvest
     */
    CASH_COW,

    /**
     * Question Marks: Low margin (< 20%) + High rotation (>= 6x/year)
     * High volume but low profitability - need strategy review
     */
    QUESTION_MARK,

    /**
     * Dogs: Low margin (< 20%) + Low rotation (< 6x/year)
     * Consider discontinuation or price adjustment
     */
    DOG,

    /**
     * Undefined: Products that don't fit into any category
     */
    UNDEFINED
}
