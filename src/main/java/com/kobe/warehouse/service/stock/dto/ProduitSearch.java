package com.kobe.warehouse.service.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.CollectionUtils;

import java.util.List;

public record ProduitSearch(Long id,
                            Long codecipprincipalid,
                            String libelle,
                            String codeeanlabo,
                            Long parentid,
                            boolean deconditionnable,
                            Integer itemqty,
                            List<ProduitFournisseurSearch> fournisseurs,
                            List<ProduitRayonSearch> rayons,
                            List<ProduitStockSearch> stocks) {
    @JsonProperty("totalQuantity")
    public int totalQuantity() {
        return CollectionUtils.isEmpty(stocks) ? 0 : stocks.stream().mapToInt(ProduitStockSearch::quantite).sum();
    }

    @JsonProperty("fournisseurProduit")
    public ProduitFournisseurSearch fournisseurProduit() {
        return fournisseurs.stream().filter(e -> e.id().equals(codecipprincipalid)).findFirst().orElse(fournisseurs.getFirst());
    }

    @JsonProperty("regularUnitPrice")
    public int regularUnitPrice() {
        return fournisseurProduit().prixUni();
    }

    @JsonProperty("codeCipPrincipalId")
    public Long codeCipPrincipalId() {
        return codecipprincipalid;
    }

    @JsonProperty("itemQty")
    public Integer itemQty() {
        return itemqty;
    }

    @JsonProperty("codeEanLabo")
    public String codeEanLabo() {
        return codeeanlabo;
    }

    @JsonProperty("parentId")
    public Long parentId() {
        return parentid;
    }

}
