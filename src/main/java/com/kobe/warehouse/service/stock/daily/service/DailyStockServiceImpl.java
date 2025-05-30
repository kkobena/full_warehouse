package com.kobe.warehouse.service.stock.daily.service;

import com.kobe.warehouse.repository.DailyStockRepository;
import com.kobe.warehouse.service.stock.daily.dto.StockRotation;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DailyStockServiceImpl implements DailyStockService {

    private final DailyStockRepository dailyStockRepository;

    public DailyStockServiceImpl(DailyStockRepository dailyStockRepository) {
        this.dailyStockRepository = dailyStockRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        this.generateDailyStockAllProducts();
    }

    @Override
    @Transactional
    public void generateDailyStockAllProducts() {
        this.dailyStockRepository.updateDailyStock();
    }
}
