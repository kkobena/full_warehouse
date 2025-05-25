package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.domain.enumeration.OrderStatut;

import java.time.LocalDate;

public class DeliveryReceiptFilterDTO {

    private LocalDate fromDate;
    private LocalDate toDate;
    private String search;
    private String searchByRef;
    private int start;
    private int limit;

    private Long fournisseurId;
    private Long userId;
    private boolean all;
    private OrderStatut statut ;

    public LocalDate getFromDate() {
        return fromDate;
    }

    public DeliveryReceiptFilterDTO setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public DeliveryReceiptFilterDTO setToDate(LocalDate toDate) {
        this.toDate = toDate;
        return this;
    }

    public String getSearch() {
        return search;
    }

    public DeliveryReceiptFilterDTO setSearch(String search) {
        this.search = search;
        return this;
    }

    public String getSearchByRef() {
        return searchByRef;
    }

    public DeliveryReceiptFilterDTO setSearchByRef(String searchByRef) {
        this.searchByRef = searchByRef;
        return this;
    }

    public int getStart() {
        return start;
    }

    public DeliveryReceiptFilterDTO setStart(int start) {
        this.start = start;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public DeliveryReceiptFilterDTO setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public DeliveryReceiptFilterDTO setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public DeliveryReceiptFilterDTO setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public boolean isAll() {
        return all;
    }

    public DeliveryReceiptFilterDTO setAll(boolean all) {
        this.all = all;
        return this;
    }

    public OrderStatut getStatut() {
        return statut;
    }

    public DeliveryReceiptFilterDTO setStatut(OrderStatut statut) {
        this.statut = statut;
        return this;
    }
}
