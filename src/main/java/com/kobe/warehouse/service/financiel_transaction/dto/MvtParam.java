package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

public class MvtParam {

    private String groupeBy;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Set<CategorieChiffreAffaire> categorieChiffreAffaires;
    private Set<SalesStatut> statuts;
    private Set<TypeVente> typeVentes;
    private boolean excludeFreeUnit;
    private Boolean toIgnore;

    public Boolean getToIgnore() {
        return toIgnore;
    }

    public void setToIgnore(Boolean toIgnore) {
        this.toIgnore = toIgnore;
    }

    public MvtParam(
        LocalDate fromDate,
        LocalDate toDate,
        Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        Set<SalesStatut> statuts,
        Set<TypeVente> typeVentes,
        String groupeBy
    ) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.categorieChiffreAffaires = categorieChiffreAffaires;
        this.statuts = statuts;
        this.typeVentes = typeVentes;
        this.groupeBy = groupeBy;
    }

    public MvtParam() {}

    public boolean isExcludeFreeUnit() {
        return excludeFreeUnit;
    }

    public void setExcludeFreeUnit(boolean excludeFreeUnit) {
        this.excludeFreeUnit = excludeFreeUnit;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public MvtParam setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public MvtParam setToDate(LocalDate toDate) {
        this.toDate = toDate;
        return this;
    }

    public Set<CategorieChiffreAffaire> getCategorieChiffreAffaires() {
        return categorieChiffreAffaires;
    }

    public MvtParam setCategorieChiffreAffaires(Set<CategorieChiffreAffaire> categorieChiffreAffaires) {
        this.categorieChiffreAffaires = categorieChiffreAffaires;
        return this;
    }

    public Set<SalesStatut> getStatuts() {
        return statuts;
    }

    public MvtParam setStatuts(Set<SalesStatut> statuts) {
        this.statuts = statuts;
        return this;
    }

    public Set<TypeVente> getTypeVentes() {
        return typeVentes;
    }

    public MvtParam setTypeVentes(Set<TypeVente> typeVentes) {
        this.typeVentes = typeVentes;
        return this;
    }

    public String getGroupeBy() {
        return groupeBy;
    }

    public MvtParam setGroupeBy(String groupeBy) {
        this.groupeBy = groupeBy;
        return this;
    }

    public MvtParam build() {
        if (Objects.isNull(typeVentes) || typeVentes.isEmpty()) {
            typeVentes = Set.of(TypeVente.CASH_SALE, TypeVente.CREDIT_SALE, TypeVente.VENTES_DEPOT_AGREE);
        }
        if (Objects.isNull(statuts) || statuts.isEmpty()) {
            statuts = Set.of(SalesStatut.CLOSED, SalesStatut.CANCELED);
        }
        if (Objects.isNull(categorieChiffreAffaires) || categorieChiffreAffaires.isEmpty()) {
            categorieChiffreAffaires = Set.of(CategorieChiffreAffaire.CA);
        }
        if (Objects.isNull(fromDate)) {
            fromDate = LocalDate.now();
        }
        if (Objects.isNull(toDate)) {
            toDate = LocalDate.now();
        }

        return new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy);
    }

    @Override
    public String toString() {
        return "MvtParam{" + "toDate=" + toDate + ", fromDate=" + fromDate + ", groupeBy='" + groupeBy + '\'' + '}';
    }
}
