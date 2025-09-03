package com.kobe.warehouse.domain;

import java.io.Serializable;

public record LotSold(long id, String numLot, int quantity) implements Serializable {}
