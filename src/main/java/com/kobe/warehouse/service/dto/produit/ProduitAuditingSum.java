package com.kobe.warehouse.service.dto.produit;

import com.kobe.warehouse.domain.enumeration.MouvementProduit;

public record ProduitAuditingSum(MouvementProduit mouvementProduitType, Integer quantity) {}
