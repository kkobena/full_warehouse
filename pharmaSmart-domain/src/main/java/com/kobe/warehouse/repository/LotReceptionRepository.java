package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.LotReception;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LotReceptionRepository extends JpaRepository<LotReception, Integer> {

    List<LotReception> findByLotIdOrderByCreatedAtAsc(Integer lotId);
}
