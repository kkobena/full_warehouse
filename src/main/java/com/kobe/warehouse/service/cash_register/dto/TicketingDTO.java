package com.kobe.warehouse.service.cash_register.dto;

public record TicketingDTO(
    Long id,
    Long cashRegisterId,
    int numberOf10Thousand,
    int numberOf5Thousand,
    int numberOf2Thousand,
    int numberOf1Thousand,
    int numberOf500Hundred,
    int numberOf200Hundred,
    int numberOf100Hundred,
    int numberOf50,
    int numberOf25,
    int numberOf10,
    int numberOf5,
    int numberOf1,
    int otherAmount,
    long totalAmount) {}
