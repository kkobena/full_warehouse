package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.dto.enumeration.StatGroupBy;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
public class VenteRecordParamDTO {
  @Builder.Default private LocalDate fromDate = LocalDate.now();
  @Builder.Default private LocalDate toDate = LocalDate.now();
  private TypeVente typeVente;
  private boolean canceled;
  private boolean differeOnly;
  @Builder.Default private StatGroupBy venteStatGroupBy = StatGroupBy.DAY;

  @Builder.Default
  private CategorieChiffreAffaire categorieChiffreAffaire = CategorieChiffreAffaire.CA;

  @Builder.Default private DashboardPeriode dashboardPeriode = DashboardPeriode.daily;
  private int start;
  @Builder.Default private int limit = 10;
}
