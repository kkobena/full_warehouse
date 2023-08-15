package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import java.time.LocalDate;
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
public class VenteRecordParamDTO {
  @Builder.Default private LocalDate fromDate = LocalDate.now();
  @Builder.Default private LocalDate toDate = LocalDate.now();
  private TypeVente typeVente;
  private boolean canceled;
  private boolean differeOnly;

  @Builder.Default
  private CategorieChiffreAffaire categorieChiffreAffaire = CategorieChiffreAffaire.CA;

  @Builder.Default private DashboardPeriode dashboardPeriode = DashboardPeriode.daily;
}
