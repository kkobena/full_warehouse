package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PaymentMode;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for the PaymentMode entity. */
@SuppressWarnings("unused")
@Repository
public interface PaymentModeRepository extends JpaRepository<PaymentMode, String> {
    List<PaymentMode> findAllByEnableTrue();
}
