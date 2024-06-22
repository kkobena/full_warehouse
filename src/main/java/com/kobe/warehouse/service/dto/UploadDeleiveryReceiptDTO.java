package com.kobe.warehouse.service.dto;

public class UploadDeleiveryReceiptDTO {

  private CommandeModel model;
  private Long fournisseurId;
  private DeliveryReceiptLiteDTO deliveryReceipt;

  public CommandeModel getModel() {
    return model;
  }

  public UploadDeleiveryReceiptDTO setModel(CommandeModel model) {
    this.model = model;
    return this;
  }

  public Long getFournisseurId() {
    return fournisseurId;
  }

  public UploadDeleiveryReceiptDTO setFournisseurId(Long fournisseurId) {
    this.fournisseurId = fournisseurId;
    return this;
  }

  public DeliveryReceiptLiteDTO getDeliveryReceipt() {
    return deliveryReceipt;
  }

  public UploadDeleiveryReceiptDTO setDeliveryReceipt(DeliveryReceiptLiteDTO deliveryReceipt) {
    this.deliveryReceipt = deliveryReceipt;
    return this;
  }
}
