package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.OrderStatut;


public class CommandeFilterDTO {
  private String search;
  private String searchCommande;
  private Long commandeId;
  private OrderStatut orderStatut;
  private FilterCommaneEnCours filterCommaneEnCours;
  private String typeSuggession;

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

    public Long getCommandeId() {
        return commandeId;
    }

    public CommandeFilterDTO setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
        return this;
    }

    public OrderStatut getOrderStatut() {
        return orderStatut;
    }

    public CommandeFilterDTO setOrderStatut(OrderStatut orderStatut) {
        this.orderStatut = orderStatut;
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

    public enum FilterCommaneEnCours {
    NOT_EQUAL,
    PROVISOL_CIP,
    ALL
  }
}
