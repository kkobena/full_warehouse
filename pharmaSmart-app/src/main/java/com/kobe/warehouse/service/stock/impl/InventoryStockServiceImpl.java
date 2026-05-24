package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.stock.InventoryStockService;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryStockServiceImpl implements InventoryStockService {

    private final StockProduitRepository stockProduitRepository;

    public InventoryStockServiceImpl(StockProduitRepository stockProduitRepository) {
        this.stockProduitRepository = stockProduitRepository;
    }

    // ── Batch par storage précis (RAYON, STORAGE) ────────────────────────────

    @Override
    public Map<Integer, Integer> buildStockMapByStorage(Integer storageId, Set<Integer> produitIds) {
        if (isEmpty(produitIds)) return Map.of();
        return stockProduitRepository
            .findAllByStorageIdAndProduitIdIn(storageId, produitIds)
            .stream()
            .collect(Collectors.toMap(
                sp -> sp.getProduit().getId(),
                sp -> sp.getQtyStock() + (sp.getQtyUG() != null ? sp.getQtyUG() : 0),
                Integer::sum
            ));
    }

    // ── Batch agrégé par magasin (MAGASIN + types thématiques) ───────────────

    @Override
    public Map<Integer, Integer> buildStockMapByMagasin(Integer magasinId, Set<Integer> produitIds) {
        if (isEmpty(produitIds)) return Map.of();
        return stockProduitRepository
            .findAggregatedStockByMagasinIdAndProduitIdIn(magasinId, produitIds)
            .stream()
            .collect(Collectors.toMap(
                row -> (Integer) row[0],
                row -> row[1] != null ? ((Number) row[1]).intValue() : 0
            ));
    }

    // ── Unitaire par storage (fallback / compatibilité) ──────────────────────

    @Override
    public int getStockByStorage(Integer storageId, Integer produitId) {
        var sp = stockProduitRepository.findOneByProduitIdAndStockageId(produitId, storageId);
        if (sp == null) return 0;
        return sp.getQtyStock() + (sp.getQtyUG() != null ? sp.getQtyUG() : 0);
    }

    // ── Unitaire agrégé par magasin (rayon + réserve) ────────────────────────

    @Override
    public int getStockByMagasin(Integer magasinId, Integer produitId) {
        Integer total = stockProduitRepository
            .findTotalQuantityByMagasinIdIdAndProduitId(magasinId, produitId);
        return total != null ? total : 0;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }
}
