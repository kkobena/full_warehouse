package com.kobe.warehouse.service.dto.records;

import java.util.List;

public record BatchSyncResultRecord(
    int saved,
    int failed,
    List<Long> failedIds,
    /**
     * Lignes rejetées pour cause de comptage concurrent (version périmée).
     * À distinguer des échecs techniques : les rejouer telles quelles échouera
     * toujours — le client doit recharger la ligne et arbitrer.
     */
    List<Long> conflictedIds
) {

    /** Constructeur de compatibilité (sans détection de conflit). */
    public BatchSyncResultRecord(int saved, int failed, List<Long> failedIds) {
        this(saved, failed, failedIds, List.of());
    }
}
