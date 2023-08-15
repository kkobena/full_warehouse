package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import java.time.LocalDate;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AchatRecordParamDTO {
  @Builder.Default private LocalDate fromDate = LocalDate.now();
  @Builder.Default private LocalDate toDate = LocalDate.now();

  @Builder.Default
  private Set<ReceiptStatut> receiptStatuts =
      Set.of(ReceiptStatut.PAID, ReceiptStatut.NOT_SOLD, ReceiptStatut.UNPAID);

  @Builder.Default private DashboardPeriode dashboardPeriode = DashboardPeriode.daily;
  private Long fournisseurId;
}
