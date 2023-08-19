package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class AchatRecordParamDTO extends VenteRecordParamDTO {
  @Builder.Default private Set<ReceiptStatut> receiptStatuts = Set.of(ReceiptStatut.CLOSE);

  private Long fournisseurId;
}
