package com.kobe.warehouse.service.financiel_transaction.dto;

public record PaymentDTO(String code, String libelle, Long paidAmount, Long realAmount) {}
