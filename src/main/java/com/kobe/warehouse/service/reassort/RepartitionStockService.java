package com.kobe.warehouse.service.reassort;

import com.kobe.warehouse.domain.LigneReassort;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.SuggestionReassort;
import com.kobe.warehouse.service.reassort.dto.RepartionQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartitionStockProduitDto;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RepartitionStockService {

    void process(SuggestionReassort suggestionReassort);

    void processReassortStockRayon(Set<LigneReassort> ligneReassorts);

    void process(List<RepartionQueryDto> datas);

    Page<RepartitionStockProduitDto> fetchRepartitionStockProduits(
        RepartionSearchQueryDto searchQueryDto, Pageable pageable);

    /*
    Lors de l'ajout de stock de reserve , on peut transférer transférer du stock du stock de rayon vers le stock de reserve
     */
    void transferStockBetweenStorages(StockProduit stockProduitDest);

    byte[] exportRepartitionStockProduits(RepartionSearchQueryDto searchQueryDto);

    /**
     * Transfert implicite atomique réserve → rayon, déclenché lors d'une vente urgente
     * ({@code forceStock=true} avec réserve disponible).
     *
     * <p>Crée un {@link com.kobe.warehouse.domain.RepartitionStockProduit} de type
     * {@link com.kobe.warehouse.domain.enumeration.TypeRepartition#AUTO} et journalise via
     * {@code InventoryTransactionService.saveRepartition()} : {@code MOUVEMENT_STOCK_OUT} sur la
     * réserve + {@code MOUVEMENT_STOCK_IN} sur le rayon.</p>
     *
     * <p>Doit être appelé dans la même transaction que la vente (atomicité garantie).</p>
     *
     * @param produitId        identifiant du produit
     * @param rayonStorageId   identifiant du storage rayon (destination)
     * @param reserveStorageId identifiant du storage réserve (source)
     * @param quantity         quantité à transférer (≤ stock réserve disponible)
     */
    void transfertImpliciteReserveVersRayon(
        Integer produitId, Integer rayonStorageId, Integer reserveStorageId, int quantity);
}
