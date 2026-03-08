package com.kobe.warehouse.domain.enumeration;

public enum StockAlertType{
    RUPTURE,    // Out of stock (quantity <= 0)
    ALERTE,     // Low stock (quantity < minimum threshold)
    PEREMPTION  // Near expiration (< 3 months)
}
