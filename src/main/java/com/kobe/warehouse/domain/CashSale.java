package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import java.io.Serial;
import java.io.Serializable;

@Entity
public class CashSale extends Sales implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
