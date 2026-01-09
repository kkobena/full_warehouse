package com.kobe.warehouse.repository.projection;

import java.time.LocalDateTime;

/**
 * Spring Data projection interface for RepartitionStockProduit queries
 * Maps native query results directly to typed interface methods
 */
public interface RepartitionStockProduitProjection {

    Integer getId();

    LocalDateTime getCreatedAt();

    Integer getQtyMvt();

    Integer getSourceInitStock();

    Integer getSourceFinalStock();

    Integer getDestInitStock();

    Integer getDestFinalStock();

    Integer getTypeRepartition();

    // User information
    String getFirstName();

    String getLastName();

    // Product information
    String getProduitName();

    String getProduitCodeEanLabo();

    String getCodeCip();

    // Source stock produit (optional)
    Integer getSrcStockId();

    Integer getSrcStorageId();

    String getSrcStorageName();

    // Destination stock produit (required)
    Integer getDestStockId();

    Integer getDestStorageId();

    String getDestStorageName();
}
