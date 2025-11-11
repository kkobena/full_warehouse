package com.kobe.warehouse.domain;

import java.io.Serializable;

public record LotSold(int id, String numLot, int quantity) implements Serializable {}
