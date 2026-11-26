package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.SalesLine;

public record VenteDepotTransactionRecord(int quantityBefore, int quantityAfter, SalesLine salesLine) {}
