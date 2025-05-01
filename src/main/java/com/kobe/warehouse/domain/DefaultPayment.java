package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import java.io.Serial;
import java.io.Serializable;

/**
 * A DefaultPayment.
 */

@Entity
public class DefaultPayment extends PaymentTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
