package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.OrderLineId;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class DeliveryReceiptItemLiteDTO {

    @NotNull
    private Long id;

    private Integer quantityUG;
    private Integer quantityReceived;

    private Integer quantityRequested;
    private Integer quantityReturned;
    private List<LotDTO> lots;
    private Integer quantityReceivedTmp;
    private Integer orderUnitPrice;
    private Integer tva;
    private Long tvaId;
    private LocalDate datePeremption;
    private LocalDate datePeremptionTmp;
    private OrderLineId orderLineId;

    public LocalDate getDatePeremptionTmp() {
        return datePeremptionTmp;
    }

    public DeliveryReceiptItemLiteDTO setDatePeremptionTmp(LocalDate datePeremptionTmp) {
        this.datePeremptionTmp = datePeremptionTmp;
        return this;
    }

    public @NotNull Long getId() {
        return id;
    }

    public DeliveryReceiptItemLiteDTO setId(@NotNull Long id) {
        this.id = id;
        return this;
    }

    public Integer getQuantityUG() {
        return quantityUG;
    }

    public DeliveryReceiptItemLiteDTO setQuantityUG(Integer quantityUG) {
        this.quantityUG = quantityUG;
        return this;
    }

    public Long getTvaId() {
        return tvaId;
    }

    public DeliveryReceiptItemLiteDTO setTvaId(Long tvaId) {
        this.tvaId = tvaId;
        return this;
    }

    public OrderLineId getOrderLineId() {
        return orderLineId;
    }

    public DeliveryReceiptItemLiteDTO setOrderLineId(OrderLineId orderLineId) {
        this.orderLineId = orderLineId;
        return this;
    }

    public Integer getQuantityReceived() {
        return quantityReceived;
    }

    public DeliveryReceiptItemLiteDTO setQuantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
        return this;
    }

    public Integer getQuantityRequested() {
        return quantityRequested;
    }

    public DeliveryReceiptItemLiteDTO setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        return this;
    }

    public Integer getQuantityReturned() {
        return quantityReturned;
    }

    public DeliveryReceiptItemLiteDTO setQuantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
        return this;
    }

    public List<LotDTO> getLots() {
        return lots;
    }

    public DeliveryReceiptItemLiteDTO setLots(List<LotDTO> lots) {
        this.lots = lots;
        return this;
    }

    public Integer getQuantityReceivedTmp() {
        return quantityReceivedTmp;
    }

    public DeliveryReceiptItemLiteDTO setQuantityReceivedTmp(Integer quantityReceivedTmp) {
        this.quantityReceivedTmp = quantityReceivedTmp;
        return this;
    }

    public LocalDate getDatePeremption() {
        return datePeremption;
    }

    public DeliveryReceiptItemLiteDTO setDatePeremption(LocalDate datePeremption) {
        this.datePeremption = datePeremption;
        return this;
    }

    public Integer getTva() {
        return tva;
    }

    public DeliveryReceiptItemLiteDTO setTva(Integer tva) {
        this.tva = tva;
        return this;
    }

    public Integer getOrderUnitPrice() {
        return orderUnitPrice;
    }

    public DeliveryReceiptItemLiteDTO setOrderUnitPrice(Integer orderUnitPrice) {
        this.orderUnitPrice = orderUnitPrice;
        return this;
    }
}
