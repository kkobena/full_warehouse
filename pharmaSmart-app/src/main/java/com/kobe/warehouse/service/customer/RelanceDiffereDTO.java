package com.kobe.warehouse.service.customer;

import java.time.LocalDateTime;

/** Une relance SMS envoyée à un client pour ses différés. */
public record RelanceDiffereDTO(LocalDateTime date, String telephone, int montant, String message, String envoyePar) {}
