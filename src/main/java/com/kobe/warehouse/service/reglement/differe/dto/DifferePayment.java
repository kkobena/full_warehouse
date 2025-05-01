package com.kobe.warehouse.service.reglement.differe.dto;

import java.time.LocalDateTime;

public interface DifferePayment {
    LocalDateTime getMvtDate();

    String getReference();

    int getAmount();
}
