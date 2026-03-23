package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.LotStockLocation;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.LotStockLocationRepository;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LotStockLocationServiceImpl implements LotStockLocationService {

    private final LotStockLocationRepository lotStockLocationRepository;
    private final LotRepository lotRepository;

    public LotStockLocationServiceImpl(LotStockLocationRepository repo, LotRepository lotRepository) {
        this.lotStockLocationRepository = repo;
        this.lotRepository = lotRepository;
    }

    @Override
    public void credit(Lot lot, Storage storage, int qtyDelta) {
        if (qtyDelta <= 0) return;
        lotStockLocationRepository.findByLotAndStorage(lot, storage).ifPresentOrElse(
            lsl -> {
                lsl.setQty(lsl.getQty() + qtyDelta);
                lotStockLocationRepository.save(lsl);
            },
            () -> lotStockLocationRepository.save(new LotStockLocation(lot, storage, qtyDelta))
        );
    }

    @Override
    public void debit(Lot lot, Storage storage, int qtyDelta) {
        if (qtyDelta <= 0) return;
        lotStockLocationRepository.findByLotAndStorage(lot, storage).ifPresent(lsl -> {
            int newQty = lsl.getQty() - qtyDelta;
            if (newQty <= 0) {
                lotStockLocationRepository.delete(lsl);
            } else {
                lsl.setQty(newQty);
                lotStockLocationRepository.save(lsl);
            }
        });
    }

    @Override
    public void creditFromSold(List<LotSold> lots, Storage storage) {
        if (lots == null || lots.isEmpty()) return;
        for (LotSold lotSold : lots) {
            if (lotSold.quantity() <= 0) continue;
            lotStockLocationRepository.findByLotIdAndStorageId(lotSold.id(), storage.getId())
                .ifPresentOrElse(
                    lsl -> {
                        lsl.setQty(lsl.getQty() + lotSold.quantity());
                        lotStockLocationRepository.save(lsl);
                    },
                    () -> {
                        // Le lot peut ne plus avoir d'entrée si épuisé — on recrée via proxy
                        Lot lotRef = new Lot();
                        lotRef.setId(lotSold.id());
                        lotStockLocationRepository.save(new LotStockLocation(lotRef, storage, lotSold.quantity()));
                    }
                );
        }
    }

    @Override
    public void creditLastLot(Produit produit, Storage storage, int qty) {
        if (qty <= 0) return;
        lotStockLocationRepository
            .findLastReceivedByStorageAndProduit(storage.getId(), produit.getId())
            .ifPresent(lsl -> {
                lsl.setQty(lsl.getQty() + qty);
                lotStockLocationRepository.save(lsl);
            });
    }

    @Override
    public void debitFefo(Produit produit, Storage storage, int qty) {
        if (qty <= 0) return;
        List<LotStockLocation> srcLots = lotStockLocationRepository.findFefoByStorageAndProduit(
            storage.getId(), produit.getId()
        );
        int remaining = qty;
        for (LotStockLocation lsl : srcLots) {
            if (remaining <= 0) break;
            int toRemove = Math.min(lsl.getQty(), remaining);
            lsl.setQty(lsl.getQty() - toRemove);
            lotStockLocationRepository.save(lsl);
            remaining -= toRemove;
        }
        srcLots.stream()
            .filter(lsl -> lsl.getQty() == 0)
            .map(lsl -> lsl.getLot().getId())
            .distinct()
            .forEach(lotStockLocationRepository::deleteZeroQtyByLot);
    }

    @Override
    public void transferFefo(Produit produit, Storage src, Storage dest, int qty) {
        if (qty <= 0) return;

        List<LotStockLocation> srcLots = lotStockLocationRepository.findFefoByStorageAndProduit(
            src.getId(), produit.getId()
        );

        int remaining = qty;
        for (LotStockLocation srcLsl : srcLots) {
            if (remaining <= 0) break;

            int toMove = Math.min(srcLsl.getQty(), remaining);

            // Décrémenter la source
            srcLsl.setQty(srcLsl.getQty() - toMove);
            lotStockLocationRepository.save(srcLsl);

            // Incrémenter la destination
            credit(srcLsl.getLot(), dest, toMove);

            remaining -= toMove;
        }

        // Nettoyage des entrées épuisées
        srcLots.stream()
            .filter(lsl -> lsl.getQty() == 0)
            .map(lsl -> lsl.getLot().getId())
            .distinct()
            .forEach(lotStockLocationRepository::deleteZeroQtyByLot);
    }
}
