package com.kobe.warehouse.service.sale.calculation;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.SalesLine;

import java.util.List;
import java.util.Set;

public class SaleContext {
    private List<SalesLine> items;
    private Set<ClientTiersPayant> clientTiersPayants;

    public SaleContext(List<SalesLine> items, Set<ClientTiersPayant> clientTiersPayants) {
        this.items = items;
        this.clientTiersPayants = clientTiersPayants;
    }

    public List<SalesLine> getItems() {
        return items;
    }

    public void setItems(List<SalesLine> items) {
        this.items = items;
    }

    public Set<ClientTiersPayant> getClientTiersPayants() {
        return clientTiersPayants;
    }

    public void setClientTiersPayants(Set<ClientTiersPayant> clientTiersPayants) {
        this.clientTiersPayants = clientTiersPayants;
    }
}
