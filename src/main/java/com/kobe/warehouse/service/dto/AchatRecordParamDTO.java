package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.OrderStatut;
import java.util.Set;

public class AchatRecordParamDTO extends VenteRecordParamDTO {

    private Set<OrderStatut> receiptStatuts = Set.of(OrderStatut.CLOSED);

    private Long fournisseurId;

    public Set<OrderStatut> getReceiptStatuts() {
        return receiptStatuts;
    }

    public AchatRecordParamDTO setReceiptStatuts(Set<OrderStatut> receiptStatuts) {
        this.receiptStatuts = receiptStatuts;
        return this;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public AchatRecordParamDTO setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
        return this;
    }
}
