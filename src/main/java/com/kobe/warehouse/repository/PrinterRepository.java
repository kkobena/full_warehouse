package com.kobe.warehouse.repository;


import com.kobe.warehouse.domain.Printer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrinterRepository extends JpaRepository<Printer, Long> {

    Optional<Printer> findByPosteName(String posteName);
}
