package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CaPonctionDetail;
import com.kobe.warehouse.domain.CaPonctionDetailId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaPonctionDetailRepository extends JpaRepository<CaPonctionDetail, CaPonctionDetailId> {
    List<CaPonctionDetail> findByPonctionIdOrderByRang(Integer ponctionId);
}
