package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DefaultPayment;
import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.service.dto.projection.MouvementCaisse;
import com.kobe.warehouse.service.dto.projection.MouvementCaisseGroupByMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface DefaultTransactionRepository extends JpaRepository<DefaultPayment, Long> {}
