package com.kobe.warehouse.domain.enumeration;

/** Pourquoi une ligne de vente a été forcée au-delà du stock. */
public enum MotifForcageStock {
    /** Rayon vide : le client paie et sera livré plus tard, l'écart part en avoir. */
    RUPTURE_AVOIR,
    /** Le rayon a le produit, la machine se trompe : le client repart servi, le stock est régularisé. */
    ECART_INVENTAIRE,
}
