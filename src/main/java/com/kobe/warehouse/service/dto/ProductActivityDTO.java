package com.kobe.warehouse.service.dto;

import java.time.LocalDate;

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

  public LocalDate getMvtDate() {
    return mvtDate;
  }

  public ProductActivityDTO setMvtDate(LocalDate mvtDate) {
    this.mvtDate = mvtDate;
    return this;
  }

  public Integer getSoldQuantity() {
    return soldQuantity;
  }

  public ProductActivityDTO setSoldQuantity(Integer soldQuantity) {
    this.soldQuantity = soldQuantity;
    return this;
  }

  public Integer getCanceledQuantity() {
    return canceledQuantity;
  }

  public ProductActivityDTO setCanceledQuantity(Integer canceledQuantity) {
    this.canceledQuantity = canceledQuantity;
    return this;
  }

  public Integer getAjustInQuantity() {
    return ajustInQuantity;
  }

  public ProductActivityDTO setAjustInQuantity(Integer ajustInQuantity) {
    this.ajustInQuantity = ajustInQuantity;
    return this;
  }

  public Integer getAjustOutQuantity() {
    return ajustOutQuantity;
  }

  public ProductActivityDTO setAjustOutQuantity(Integer ajustOutQuantity) {
    this.ajustOutQuantity = ajustOutQuantity;
    return this;
  }

  public Integer getDeconInQuantity() {
    return deconInQuantity;
  }

  public ProductActivityDTO setDeconInQuantity(Integer deconInQuantity) {
    this.deconInQuantity = deconInQuantity;
    return this;
  }

  public Integer getDeconOutQuantity() {
    return deconOutQuantity;
  }

  public ProductActivityDTO setDeconOutQuantity(Integer deconOutQuantity) {
    this.deconOutQuantity = deconOutQuantity;
    return this;
  }

  public Integer getInventoryQuantity() {
    return inventoryQuantity;
  }

  public ProductActivityDTO setInventoryQuantity(Integer inventoryQuantity) {
    this.inventoryQuantity = inventoryQuantity;
    return this;
  }

  public Integer getRetourFourQuantity() {
    return retourFourQuantity;
  }

  public ProductActivityDTO setRetourFourQuantity(Integer retourFourQuantity) {
    this.retourFourQuantity = retourFourQuantity;
    return this;
  }

  public Integer getReceivedQuantity() {
    return receivedQuantity;
  }

  public ProductActivityDTO setReceivedQuantity(Integer receivedQuantity) {
    this.receivedQuantity = receivedQuantity;
    return this;
  }

  public Integer getCurrentStock() {
    return currentStock;
  }

  public ProductActivityDTO setCurrentStock(Integer currentStock) {
    this.currentStock = currentStock;
    return this;
  }

  public Integer getInitSock() {
    return initSock;
  }

  public ProductActivityDTO setInitSock(Integer initSock) {
    this.initSock = initSock;
    return this;
  }
}
