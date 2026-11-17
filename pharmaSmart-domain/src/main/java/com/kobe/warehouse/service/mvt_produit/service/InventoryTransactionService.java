package com.kobe.warehouse.service.mvt_produit.service;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.RepartitionStockProduit;
import com.kobe.warehouse.domain.RetourDepotItem;
import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingSum;
import com.kobe.warehouse.service.sale.dto.VenteDepotTransactionRecord;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryTransactionService {
    void save(Object entity); // Generic save method for all entities

    long quantitySold(Integer produitId);

    Optional<InventoryTransaction> findById(Long id);

    Page<InventoryTransactionDTO> getAllInventoryTransactions(
        Pageable pageable,
        Integer produitId,
        String startDate,
        String endDate,
        Integer type
    );

    LocalDateTime fetchLastDateByTypeAndProduitId(MouvementProduit type, Integer produitId);

    List<ProduitAuditingState> fetchProduitDailyTransaction(@Valid ProduitAuditingParam produitAuditingParam);

    List<ProduitAuditingSum> fetchProduitDailyTransactionSum(@Valid ProduitAuditingParam produitAuditingParam);

    void saveAll(List<OrderLine> entities);
    void saveVenteDepotExtensionInventoryTransactions(Magasin depot, List<VenteDepotTransactionRecord> venteDepotTransactionRecords);

    /**
     * Trace le débit du dépôt lors d'un retour vers l'officine.
     *
     * <p>Le retour déplace la marchandise entre deux magasins : {@code save(RetourDepotItem)} n'en
     * écrit que le crédit de l'officine, avec les stocks de l'officine. Sans cette seconde entrée,
     * le stock du dépôt baisse en base sans que le journal le dise, et {@code
     * fn_stock_quantites_at_time} — qui reconstitue par (produit, storage) — dérive d'autant.
     * Symétrique de {@link #saveVenteDepotExtensionInventoryTransactions}.
     */
    void saveRetourDepotExtensionInventoryTransaction(Magasin depot, RetourDepotItem retourDepotItem);

    /**
     * Crée deux entrées journal (MOUVEMENT_STOCK_OUT + MOUVEMENT_STOCK_IN)
     * pour un transfert inter-storage déjà persisté.
     * Doit être appelé après repartitionStockProduitRepository.save().
     */
    void saveRepartition(RepartitionStockProduit repartition);
}
