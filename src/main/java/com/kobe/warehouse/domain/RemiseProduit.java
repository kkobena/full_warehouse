package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import java.io.Serial;
import java.io.Serializable;

/**
 * A RemiseProduit.
 */
@Entity
public class RemiseProduit extends Remise implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
