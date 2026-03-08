package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.MvStockAlert;
import com.kobe.warehouse.domain.enumeration.StockAlertType;

import java.util.EnumSet;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


@Repository
public interface MvStockAlertRepository extends JpaRepository<MvStockAlert, Integer>, JpaSpecificationExecutor<MvStockAlert> {

    Page<MvStockAlert> findAllByAlertTypeIn(EnumSet<StockAlertType> alertTypes, Pageable pageable);

}
