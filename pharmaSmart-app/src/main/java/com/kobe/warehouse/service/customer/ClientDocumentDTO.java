package com.kobe.warehouse.service.customer;

/** Le client tel qu'il figure en tête d'un document. */
public record ClientDocumentDTO(Integer id, String code, String nom, String telephone) {}
