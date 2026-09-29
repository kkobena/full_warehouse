package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RelanceDiffere;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelanceDiffereRepository extends JpaRepository<RelanceDiffere, Integer> {
    List<RelanceDiffere> findTop10ByCustomerIdOrderByCreatedAtDesc(Integer customerId);
}
