package com.kobe.warehouse.service.dto.ordonnance;

/**
 * Résultat du rattachement d'une vente : lignes de vente liées à une ligne prescrite, dont celles
 * liées par équivalence générique (un produit délivré différent du produit prescrit).
 */
public record AppariementDTO(OrdonnanceDTO ordonnance, int lignesLiees, int lignesGeneriques) {}
