package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DeliveryReceiptFilterDTO {

    private LocalDate fromDate;
    private LocalDate toDate;
    private String search;
    private int start;
    private int limit;

    private Long fournisseurId;
    private Long userId;
    private boolean all;
    @Default
    private ReceiptStatut statut = ReceiptStatut.ANY;
}
