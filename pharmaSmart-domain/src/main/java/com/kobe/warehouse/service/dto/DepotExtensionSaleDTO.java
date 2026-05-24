package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.VenteDepot;

public class DepotExtensionSaleDTO extends SaleDTO {

    public DepotExtensionSaleDTO() {
        super();
    }

    public DepotExtensionSaleDTO(VenteDepot sale) {
        super(sale);
        this.magasin = new MagasinDTO(sale.getDepot());
    }
}
