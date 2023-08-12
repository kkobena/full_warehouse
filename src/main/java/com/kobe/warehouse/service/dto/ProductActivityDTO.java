package com.kobe.warehouse.service.dto;

import java.time.LocalDate;
import lombok.Getter;

@Getter
public class ProductActivityDTO {
  private LocalDate mvtDate;
  private Integer soldQuantity;
  private Integer canceledQuantity;
  private Integer ajustInQuantity;
  private Integer ajustOutQuantity;
  private Integer deconInQuantity;
  private Integer deconOutQuantity;
  private Integer inventoryQuantity;
  private Integer retourFourQuantity;
  private Integer receivedQuantity;
  private Integer currentStock;
  private Integer initSock;

    public ProductActivityDTO setMvtDate(LocalDate mvtDate) {
        this.mvtDate = mvtDate;
        return this;
    }

    public ProductActivityDTO setSoldQuantity(Integer soldQuantity) {
        this.soldQuantity = soldQuantity;
        return this;
    }

    public ProductActivityDTO setCanceledQuantity(Integer canceledQuantity) {
        this.canceledQuantity = canceledQuantity;
        return this;
    }

    public ProductActivityDTO setAjustInQuantity(Integer ajustInQuantity) {
        this.ajustInQuantity = ajustInQuantity;
        return this;
    }

    public ProductActivityDTO setAjustOutQuantity(Integer ajustOutQuantity) {
        this.ajustOutQuantity = ajustOutQuantity;
        return this;
    }

    public ProductActivityDTO setDeconInQuantity(Integer deconInQuantity) {
        this.deconInQuantity = deconInQuantity;
        return this;
    }

    public ProductActivityDTO setDeconOutQuantity(Integer deconOutQuantity) {
        this.deconOutQuantity = deconOutQuantity;
        return this;
    }

    public ProductActivityDTO setInventoryQuantity(Integer inventoryQuantity) {
        this.inventoryQuantity = inventoryQuantity;
        return this;
    }

    public ProductActivityDTO setRetourFourQuantity(Integer retourFourQuantity) {
        this.retourFourQuantity = retourFourQuantity;
        return this;
    }

    public ProductActivityDTO setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
        return this;
    }

    public ProductActivityDTO setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
        return this;
    }

    public ProductActivityDTO setInitSock(Integer initSock) {
        this.initSock = initSock;
        return this;
    }
}
