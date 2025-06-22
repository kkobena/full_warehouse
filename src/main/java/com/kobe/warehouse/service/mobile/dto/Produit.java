package com.kobe.warehouse.service.mobile.dto;

import java.util.List;

public record Produit(long id,
                      String name,
                      String code,
                      int quantity,
                      String unitPrice,
                      String grossAmount,
                      List<ProduitCommde> commdes,
                      List<ProduitFournisseur> produitFournisseurs,
                      List<ProduitStock> produitStocks,
                      List<ProduitVendu> produitVendus) {
}
