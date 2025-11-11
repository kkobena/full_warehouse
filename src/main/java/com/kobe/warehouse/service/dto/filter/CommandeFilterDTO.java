package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.FilterCommaneEnCours;
import com.kobe.warehouse.service.dto.Sort;

import java.time.LocalDate;
import java.util.Set;

public class CommandeFilterDTO {

    private String search;
    private String searchCommande;
    private Integer commandeId;
    private Set<OrderStatut> orderStatuts;
    private FilterCommaneEnCours filterCommaneEnCours;
    private String typeSuggession;
    private Sort orderBy;
    private LocalDate orderDate;

    public Sort getOrderBy() {
        return orderBy;
    }

    public CommandeFilterDTO setOrderBy(Sort orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public String getSearch() {
        return search;
    }

    public CommandeFilterDTO setSearch(String search) {
        this.search = search;
        return this;
    }

    public String getSearchCommande() {
        return searchCommande;
    }

    public CommandeFilterDTO setSearchCommande(String searchCommande) {
        this.searchCommande = searchCommande;
        return this;
    }

    public Integer getCommandeId() {
        return commandeId;
    }

    public CommandeFilterDTO setCommandeId(Integer commandeId) {
        this.commandeId = commandeId;
        return this;
    }

    public Set<OrderStatut> getOrderStatuts() {
        return orderStatuts;
    }

    public CommandeFilterDTO setOrderStatuts(Set<OrderStatut> orderStatuts) {
        this.orderStatuts = orderStatuts;
        return this;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public CommandeFilterDTO setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
        return this;
    }

    public FilterCommaneEnCours getFilterCommaneEnCours() {
        return filterCommaneEnCours;
    }

    public CommandeFilterDTO setFilterCommaneEnCours(FilterCommaneEnCours filterCommaneEnCours) {
        this.filterCommaneEnCours = filterCommaneEnCours;
        return this;
    }

    public String getTypeSuggession() {
        return typeSuggession;
    }

    public CommandeFilterDTO setTypeSuggession(String typeSuggession) {
        this.typeSuggession = typeSuggession;
        return this;
    }

    @Override
    public String toString() {
        String sb =
            "CommandeFilterDTO{" +
            "search='" +
            search +
            '\'' +
            ", searchCommande='" +
            searchCommande +
            '\'' +
            ", commandeId=" +
            commandeId +
            ", orderStatuts=" +
            orderStatuts +
            ", filterCommaneEnCours=" +
            filterCommaneEnCours +
            ", typeSuggession='" +
            typeSuggession +
            '\'' +
            ", orderBy=" +
            orderBy +
            '}';
        return sb;
    }
}
