package com.kobe.warehouse.service.dto;

public class ProduitRecordParamDTO extends VenteRecordParamDTO {

    private OrderBy order = OrderBy.QUANTITY_SOLD;
    private String search;
    private Long produitId;

    public OrderBy getOrder() {
        return order;
    }

    public ProduitRecordParamDTO setOrder(OrderBy order) {
        this.order = order;
        return this;
    }

    public String getSearch() {
        return search;
    }

    public ProduitRecordParamDTO setSearch(String search) {
        this.search = search;
        return this;
    }

    public Long getProduitId() {
        return produitId;
    }

    public ProduitRecordParamDTO setProduitId(Long produitId) {
        this.produitId = produitId;
        return this;
    }
}
