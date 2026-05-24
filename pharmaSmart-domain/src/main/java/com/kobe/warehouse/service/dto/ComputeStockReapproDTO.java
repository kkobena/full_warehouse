package com.kobe.warehouse.service.dto;

public class ComputeStockReapproDTO {

    private final long id;
    private final int qtySold;
    private final int itemQtySold;
    private final int itemQty;

    public ComputeStockReapproDTO(long id, int itemQty, int qtySold, int itemQtySold) {
        this.id = id;
        this.qtySold = qtySold;
        this.itemQtySold = itemQtySold;
        this.itemQty = itemQty;
    }

    public long getId() {
        return id;
    }

    public int getQtySold() {
        return qtySold;
    }

    public int getItemQtySold() {
        return itemQtySold;
    }

    public int getItemQty() {
        return itemQty;
    }
}
