package com.kobe.warehouse.service.dto.produit;

import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record ProduitAuditingParam(
    @NotNull Integer produitId,
    LocalDate fromDate,
    LocalDate toDate,
    Integer magasinId,
    Integer storageId,
    // Types de mouvement à filtrer (null ou vide = tous)
    List<MouvementProduit> mouvementTypes
) {}
