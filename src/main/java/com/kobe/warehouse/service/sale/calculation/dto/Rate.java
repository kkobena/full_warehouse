package com.kobe.warehouse.service.sale.calculation.dto;

import java.io.Serializable;

public record Rate(long compteTiersPayantId, float rate) implements Serializable {}
