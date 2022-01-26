package com.kobe.warehouse.service;

import com.kobe.warehouse.repository.RayonProduitRepository;
import com.kobe.warehouse.repository.RepartitionStockProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import org.springframework.stereotype.Service;

@Service
public class RepartitionStockService {
    private final RepartitionStockProduitRepository repartitionStockProduitRepository;
    private final StockProduitRepository stockProduitRepository;
    private final RayonProduitRepository rayonProduitRepository;

    public RepartitionStockService(RepartitionStockProduitRepository repartitionStockProduitRepository, StockProduitRepository stockProduitRepository, RayonProduitRepository rayonProduitRepository) {
        this.repartitionStockProduitRepository = repartitionStockProduitRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.rayonProduitRepository = rayonProduitRepository;
    }
}
