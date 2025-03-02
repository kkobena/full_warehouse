package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.dto.enumeration.StatGroupBy;
import java.time.LocalDate;

public class VenteRecordParamDTO {

    private LocalDate fromDate = LocalDate.now();
    private LocalDate toDate = LocalDate.now();
    private TypeVente typeVente;
    private boolean canceled;
    private boolean differeOnly;
    private StatGroupBy venteStatGroupBy = StatGroupBy.DAY;

    private CategorieChiffreAffaire categorieChiffreAffaire = CategorieChiffreAffaire.CA;

    private DashboardPeriode dashboardPeriode = DashboardPeriode.daily;
    private int start;
    private int limit = 10;

    public LocalDate getFromDate() {
        return fromDate;
    }

    public VenteRecordParamDTO setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public VenteRecordParamDTO setToDate(LocalDate toDate) {
        this.toDate = toDate;
        return this;
    }

    public TypeVente getTypeVente() {
        return typeVente;
    }

    public VenteRecordParamDTO setTypeVente(TypeVente typeVente) {
        this.typeVente = typeVente;
        return this;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public VenteRecordParamDTO setCanceled(boolean canceled) {
        this.canceled = canceled;
        return this;
    }

    public boolean isDiffereOnly() {
        return differeOnly;
    }

    public VenteRecordParamDTO setDiffereOnly(boolean differeOnly) {
        this.differeOnly = differeOnly;
        return this;
    }

    public StatGroupBy getVenteStatGroupBy() {
        return venteStatGroupBy;
    }

    public VenteRecordParamDTO setVenteStatGroupBy(StatGroupBy venteStatGroupBy) {
        this.venteStatGroupBy = venteStatGroupBy;
        return this;
    }

    public CategorieChiffreAffaire getCategorieChiffreAffaire() {
        return categorieChiffreAffaire;
    }

    public VenteRecordParamDTO setCategorieChiffreAffaire(CategorieChiffreAffaire categorieChiffreAffaire) {
        this.categorieChiffreAffaire = categorieChiffreAffaire;
        return this;
    }

    public DashboardPeriode getDashboardPeriode() {
        return dashboardPeriode;
    }

    public VenteRecordParamDTO setDashboardPeriode(DashboardPeriode dashboardPeriode) {
        this.dashboardPeriode = dashboardPeriode;
        return this;
    }

    public int getStart() {
        return start;
    }

    public VenteRecordParamDTO setStart(int start) {
        this.start = start;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public VenteRecordParamDTO setLimit(int limit) {
        this.limit = limit;
        return this;
    }
}
