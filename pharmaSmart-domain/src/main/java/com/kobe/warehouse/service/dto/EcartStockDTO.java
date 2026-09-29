package com.kobe.warehouse.service.dto;

/**
 * Produit dont le stock négatif dépasse ce que doivent les avoirs ouverts : la part en trop est un
 * écart entre stock machine et stock physique, à régulariser par ajustement ou inventaire.
 *
 * @param stock        stock machine du magasin, UG comprises
 * @param quantiteDue  quantité encore due aux clients (avoirs ouverts)
 * @param ecart        unités sorties sans explication : {@code −(stock + quantiteDue)}, toujours &gt; 0
 */
public record EcartStockDTO(Integer produitId, String libelle, String codeCip, int stock, int quantiteDue, int ecart) {}
