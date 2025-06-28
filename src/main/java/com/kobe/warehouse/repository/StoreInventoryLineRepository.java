package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the StoreInventoryLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StoreInventoryLineRepository extends JpaRepository<StoreInventoryLine, Long> {
    List<StoreInventoryLine> findAllByStoreInventoryId(Long storeInventoryId);

    long countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(Long id);

    @Procedure("proc_close_inventory")
    int procCloseInventory(Integer inventoryId);

    @Query(
        value = "SELECT MAX(o.updated_at) AS updatedAt FROM store_inventory_line o JOIN warehouse.produit p ON o.produit_id = p.id JOIN store_inventory d ON o.store_inventory_id = d.id WHERE o.produit_id = ?1 AND d.statut =2",
        nativeQuery = true
    )
    LastDateProjection findLastUpdatedAtProduitId(Long produitId);

    @Query(value = "SELECT o FROM StoreInventoryLine o JOIN o.produit p JOIN p.fournisseurProduits fp WHERE fp.codeCip IN :codeCips")
    List<StoreInventoryLine> findAllByCodeCip(Set<String> codeCips);

    @Query(
        "SELECT DISTINCT rp.rayon FROM  StoreInventoryLine  s JOIN s.produit p JOIN p.rayonProduits rp WHERE s.storeInventory.id = ?1 ORDER BY rp.rayon.libelle"
    )
    List<Rayon> findAllRayons(Long storeInventoryId);

    @Query(
        "SELECT DISTINCT s FROM  StoreInventoryLine  s JOIN s.produit p JOIN p.rayonProduits rp WHERE s.storeInventory.id = :storeInventoryId AND rp.rayon.id=:rayonId ORDER BY rp.rayon.libelle"
    )
    List<StoreInventoryLine> findAllByStoreInventoryIdAndRayonId(
        @Param("storeInventoryId") Long storeInventoryId,
        @Param("rayonId") Long rayonId
    );
}
