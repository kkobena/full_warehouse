package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.WarehouseCalendar;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseCalendarRepository extends JpaRepository<WarehouseCalendar, LocalDate> {}
