package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.VenteDepot;

public class DepotExtensionSaleDTO extends SaleDTO {

    public DepotExtensionSaleDTO() {
        super();
    }

    public DepotExtensionSaleDTO(VenteDepot sale) {
        this(sale, true);
    }

    /** @see SaleDTO#SaleDTO(com.kobe.warehouse.domain.Sales, boolean) */
    public DepotExtensionSaleDTO(VenteDepot sale, boolean withSalesLines) {
        super(sale, withSalesLines);
        this.magasin = new MagasinDTO(sale.getDepot());
    }
}
