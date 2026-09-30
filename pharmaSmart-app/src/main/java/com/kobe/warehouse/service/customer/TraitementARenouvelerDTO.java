package com.kobe.warehouse.service.customer;

/** Patient à relancer pour un traitement chronique arrivé à échéance. */
public record TraitementARenouvelerDTO(Integer customerId, String customerCode, String customerNom, String telephone, TraitementChroniqueDTO traitement) {}
