package com.kobe.warehouse.service.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kobe.warehouse.domain.enumeration.StorageType;
import java.util.List;
import java.util.Objects;
import org.springframework.util.CollectionUtils;

public record ProduitSearch(
    Integer id,
    Integer codecipprincipalid,
    String libelle,
    String codeeanlabo,
    Integer parentid,
    boolean deconditionnable,
    Integer itemqty,
    Integer vatrate,
    Integer regularunitprice,
    Integer costamount,
    List<ProduitFournisseurSearch> fournisseurs,

    List<ProduitRayonSearch> rayons,
    List<ProduitStockSearch> stocks
) {

    /**
     * Stock rayon (PRINCIPAL) uniquement : qty_stock + qty_ug. Cohérent avec
     * {@code findPointVenteStock} et la validation backend à la vente. Ne jamais sommer rayon +
     * réserve ici : le backend valide contre le rayon seul.
     */
    @JsonProperty("totalQuantity")
    public int totalQuantity() {
        if (CollectionUtils.isEmpty(stocks)) {
            return 0;
        }
        return stocks.stream()
            .filter(s -> s.storageType() == StorageType.PRINCIPAL)
            .mapToInt(s -> s.quantite() + s.qteUg())
            .sum();
    }

    @JsonProperty("fournisseurProduit")
    public ProduitFournisseurSearch fournisseurProduit() {
        return fournisseurs.stream().filter(e -> Objects.equals(codecipprincipalid, e.id()))
            .findFirst().orElse(fournisseurs.getFirst());
    }

    @JsonProperty("regularUnitPrice")
    public int regularUnitPrice() {
        return fournisseurProduit().prixUni();
    }

    @JsonProperty("codeCipPrincipalId")
    public Integer codeCipPrincipalId() {
        return codecipprincipalid;
    }

    @JsonProperty("itemQty")
    public Integer itemQty() {
        return itemqty;
    }

    @JsonProperty("vatRate")
    public Integer vatRate() {
        return vatrate;
    }

    @JsonProperty("codeEanLabo")
    public String codeEanLabo() {
        return codeeanlabo;
    }

    @JsonProperty("codeProduit")
    public String codeProduit() {
        var fournisseur = fournisseurProduit();
        if (fournisseur != null) {
            return fournisseur.codeCip();
        }
        return null;
    }

    @JsonProperty("parentId")
    public Integer parentId() {
        return parentid;
    }

    @JsonProperty("costAmount")
    public Integer costAmount() {
        return costamount;
    }

    @JsonProperty("baseRegularUnitPrice")
    public Integer baseRegularUnitPrice() {
        return regularunitprice;
    }

    /**
     * Alias de {@link #totalQuantity()} — stock rayon disponible à la vente. Exposé séparément pour
     * la lisibilité côté frontend (badge "dispo vente").
     */
    @JsonProperty("stockDisponibleEnVente")
    public int stockDisponibleEnVente() {
        return totalQuantity();
    }

    /**
     * Stock réserve (SAFETY_STOCK) : qty_stock + qty_ug. Informatif uniquement — la réserve n'est
     * pas directement vendable sans transfert rayon. Retourne 0 si aucun stock réserve n'existe
     * pour ce produit.
     */
    @JsonProperty("reserveQuantity")
    public int reserveQuantity() {
        if (CollectionUtils.isEmpty(stocks)) {
            return 0;
        }
        return stocks.stream()
            .filter(s -> s.storageType() == StorageType.SAFETY_STOCK)
            .mapToInt(s -> s.quantite() + s.qteUg())
            .sum();
    }
}
