package com.kobe.warehouse.domain.enumeration;

/**
 * ABC Pareto Classification based on 80/20 rule
 * Based on cumulative revenue contribution
 */
public enum ClassePareto {
    /**
     * Class A: Products contributing to 80% of total revenue
     * Strategic products - high priority management
     */
    A,

    /**
     * Class B: Products contributing to next 15% of revenue (80-95%)
     * Important products - moderate priority
     */
    B,

    /**
     * Class C: Products contributing to remaining 5% of revenue (95-100%)
     * Low priority products - basic management
     */
    C
}
