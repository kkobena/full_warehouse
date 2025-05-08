package com.kobe.warehouse.service.stock.daily.service;


import com.kobe.warehouse.service.stock.daily.dto.StockRotation;

import java.util.List;

public interface DailyStockService {


    /**
     * Generate daily stock for all products
     * This method is used to generate daily stock for all products in the system.
     */
    void generateDailyStockAllProducts();

List<StockRotation> computeStockRotation(Long produitId);


}
