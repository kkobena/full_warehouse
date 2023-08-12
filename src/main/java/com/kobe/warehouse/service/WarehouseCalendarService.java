package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.WarehouseCalendar;
import com.kobe.warehouse.repository.WarehouseCalendarRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class WarehouseCalendarService {
    private final WarehouseCalendarRepository warehouseCalendarRepository;

    public WarehouseCalendarService(WarehouseCalendarRepository warehouseCalendarRepository) {
        this.warehouseCalendarRepository = warehouseCalendarRepository;
    }

    @Async
    public void initCalendar() {
        LocalDate now = LocalDate.now();
        Optional<WarehouseCalendar> optionalWarehouseCalendar =
            warehouseCalendarRepository.findById(now);
        if (optionalWarehouseCalendar.isEmpty()) {

            warehouseCalendarRepository.save(
                new WarehouseCalendar()
                    .setWorkDay(now)
                    .setWorkMonth(now.getMonthValue())
                    .setWorkYear(now.getYear()));
        }
    }
}
