package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StoreInventoryLine;
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

    void deleteAllByStoreInventoryId(Long storeInventoryId);

    @Procedure(name = "StoreInventoryLine.proc_close_inventory")
    int procCloseInventory(@Param("store_inventory_id") Integer inventoryId);

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
