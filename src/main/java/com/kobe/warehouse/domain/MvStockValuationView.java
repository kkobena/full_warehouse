package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Entité en lecture seule mappée sur la vue matérialisée mv_stock_valuation.
 * IMPORTANT: Cette entité est IMMUTABLE (lecture seule).
 */
@Entity
@Table(name = "mv_stock_valuation")
@Immutable // Hibernate: empêche INSERT/UPDATE/DELETE
public class MvStockValuationView extends StockValuationView {
}
