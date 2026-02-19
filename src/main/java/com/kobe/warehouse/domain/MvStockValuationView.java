package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entité en lecture seule mappée sur la vue matérialisée mv_stock_valuation.
 * IMPORTANT: Cette entité est IMMUTABLE (lecture seule).
 */
@Entity
@Table(name = "mv_stock_valuation")
public class MvStockValuationView extends StockValuationView {
}
