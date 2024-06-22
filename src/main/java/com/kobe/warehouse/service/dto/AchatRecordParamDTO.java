package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import java.util.Set;

public class AchatRecordParamDTO extends VenteRecordParamDTO {
  private Set<ReceiptStatut> receiptStatuts = Set.of(ReceiptStatut.CLOSE);

  private Long fournisseurId;

  public Set<ReceiptStatut> getReceiptStatuts() {
    return receiptStatuts;
  }

  public AchatRecordParamDTO setReceiptStatuts(Set<ReceiptStatut> receiptStatuts) {
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
