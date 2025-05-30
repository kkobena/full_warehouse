package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Printer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrinterRepository extends JpaRepository<Printer, Long> {
    Optional<Printer> findByPosteName(String posteName);
}
