package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.WarehouseCalendar;
import com.kobe.warehouse.repository.WarehouseCalendarRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WarehouseCalendarService {

    private final WarehouseCalendarRepository warehouseCalendarRepository;

    public WarehouseCalendarService(WarehouseCalendarRepository warehouseCalendarRepository) {
        this.warehouseCalendarRepository = warehouseCalendarRepository;
    }

    public WarehouseCalendar initCalendar() {
        LocalDate now = LocalDate.now();
        Optional<WarehouseCalendar> optionalWarehouseCalendar = warehouseCalendarRepository.findById(now);
        return optionalWarehouseCalendar.orElseGet(
            () ->
                warehouseCalendarRepository.save(
                    new WarehouseCalendar().setWorkDay(now).setWorkMonth(now.getMonthValue()).setWorkYear(now.getYear())
                )
        );
    }
}
