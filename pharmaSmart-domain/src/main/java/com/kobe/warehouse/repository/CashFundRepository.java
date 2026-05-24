package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashFundRepository extends JpaRepository<CashFund, Integer> {
    CashFund findOneByCashRegisterIdAndStatut(Integer cashRegisterId, CashRegisterStatut statut);
}
