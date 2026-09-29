package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ReinitialisationConsommation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReinitialisationConsommationRepository extends JpaRepository<ReinitialisationConsommation, Integer> {
    List<ReinitialisationConsommation> findAllByFactureIdAndFactureDate(Long factureId, LocalDate factureDate);
}
