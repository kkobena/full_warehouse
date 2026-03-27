package com.kobe.warehouse.service.dto;

import java.util.List;

/**
 * DTO générique pour transmettre une liste d'identifiants depuis le frontend.
 * Correspond au modèle TypeScript {@code Keys { ids: number[], all?: boolean }}.
 *
 * @param ids liste des identifiants concernés
 * @param all si {@code true}, l'opération s'applique à tous les éléments (non utilisé côté backend pour l'instant)
 */
public record Keys(List<Integer> ids, Boolean all) {

    /** Retourne les IDs sous forme de liste non nulle. */
    public List<Integer> safeIds() {
        return ids != null ? ids : List.of();
    }
}

