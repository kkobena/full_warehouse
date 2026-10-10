package com.kobe.warehouse.service.dto;

import java.util.ArrayList;
import java.util.List;

public class CommandeResponseDTO {

    private int totalItemCount;
    private int succesCount;
    private int failureCount;
    private String reference;
    private List<OrderItem> items = new ArrayList<>();
    private DeliveryReceiptLiteDTO entity;
    /** Lignes non prises en compte, au format du grossiste ; absent s'il n'y en a pas. */
    private byte[] ruptureCsv;

    public CommandeResponseDTO() {}

    public byte[] getRuptureCsv() {
        return ruptureCsv;
    }

    public CommandeResponseDTO setRuptureCsv(byte[] ruptureCsv) {
        this.ruptureCsv = ruptureCsv;
        return this;
    }

    public DeliveryReceiptLiteDTO getEntity() {
        return entity;
    }

    public CommandeResponseDTO setEntity(DeliveryReceiptLiteDTO entity) {
        this.entity = entity;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public CommandeResponseDTO setReference(String reference) {
        this.reference = reference;
        return this;
    }

    public int getTotalItemCount() {
        return totalItemCount;
    }

    public CommandeResponseDTO setTotalItemCount(int totalItemCount) {
        this.totalItemCount = totalItemCount;
        return this;
    }

    public int getSuccesCount() {
        return succesCount;
    }

    public CommandeResponseDTO setSuccesCount(int succesCount) {
        this.succesCount = succesCount;
        return this;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public CommandeResponseDTO setFailureCount(int failureCount) {
        this.failureCount = failureCount;
        return this;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public CommandeResponseDTO setItems(List<OrderItem> items) {
        this.items = items;
        return this;
    }
}
