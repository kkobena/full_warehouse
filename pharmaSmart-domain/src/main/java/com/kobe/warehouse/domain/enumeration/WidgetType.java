package com.kobe.warehouse.domain.enumeration;

/**
 * Widget Type Enumeration
 */
public enum WidgetType {
    KPI_CARD,           // Simple KPI display (CA, nb ventes, etc.)
    LINE_CHART,         // Line chart (évolution CA, etc.)
    BAR_CHART,          // Bar chart (ventes par produit, etc.)
    PIE_CHART,          // Pie chart (répartition CA, etc.)
    TABLE,              // Data table
    TOP_PRODUCTS,       // Top 10 products widget
    STOCK_ALERTS,       // Stock alerts widget
    RECENT_SALES,       // Recent sales list
    PENDING_INVOICES,   // Pending invoices
    PERFORMANCE_GAUGE   // Gauge chart (taux de rotation, etc.)
}
