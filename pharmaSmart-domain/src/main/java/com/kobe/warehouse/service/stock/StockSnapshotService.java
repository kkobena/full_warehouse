package com.kobe.warehouse.service.stock;

public interface StockSnapshotService {

    /**
     * Crée un snapshot de stock pour tous les produits d'un magasin donné.
     * Idempotent : utilise DATE_TRUNC('day') pour éviter les doublons quotidiens.
     */
    void createDailySnapshot(Integer magasinId);

    /**
     * Crée un snapshot pour tous les magasins.
     * Appelé par le scheduler nocturne.
     */
    void createDailySnapshotForAll();
}
