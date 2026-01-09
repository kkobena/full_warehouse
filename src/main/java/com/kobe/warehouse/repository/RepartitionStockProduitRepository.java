package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RepartitionStockProduit;
import com.kobe.warehouse.domain.enumeration.TypeRepartition;
import com.kobe.warehouse.repository.projection.RepartitionStockProduitProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@SuppressWarnings("unused")
@Repository
public interface RepartitionStockProduitRepository extends JpaRepository<RepartitionStockProduit, Integer> {

    /**
     * Fetches repartition stock history with dynamic filters using Spring Data projection
     *
     * @param userId          filter by user ID (optional)
     * @param typeRepartition filter by repartition type (optional)
     * @param storageId       filter by storage ID (optional)
     * @param stockProduitId  filter by stock produit ID (optional)
     * @param searchTerm      search term for product name/code (optional)
     * @param dateDebut       start date filter (required)
     * @param dateFin         end date filter (required)
     * @param pageable        pagination information
     * @return page of projection results
     */
    @Query(value = """
        SELECT r.id AS id,
               r.created_at AS createdAt,
               r.qty_mvt AS qtyMvt,
               r.source_init_stock AS sourceInitStock,
               r.source_final_stock AS sourceFinalStock,
               r.dest_init_stock AS destInitStock,
               r.dest_final_stock AS destFinalStock,
               r.type_repartition AS typeRepartition,
               u.first_name AS firstName,
               u.last_name AS lastName,
               p.libelle AS produitName,
               p.code_ean_labo AS produitCodeEanLabo,
               fp.code_cip AS codeCip,
               src_sp.id AS srcStockId,
               src_sp.storage_id AS srcStorageId,
               src_s.name AS srcStorageName,
               dest_sp.id AS destStockId,
               dest_sp.storage_id AS destStorageId,
               dest_s.name AS destStorageName
        FROM repartition_stock_produit r
        INNER JOIN app_user u ON r.user_id = u.id
        INNER JOIN stock_produit dest_sp ON r.stock_produit_destination_id = dest_sp.id
        INNER JOIN produit p ON dest_sp.produit_id = p.id
        INNER JOIN storage dest_s ON dest_sp.storage_id = dest_s.id
        LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
        LEFT JOIN stock_produit src_sp ON r.stock_produit_source_id = src_sp.id
        LEFT JOIN storage src_s ON src_sp.storage_id = src_s.id
        WHERE CAST(r.created_at AS date) >= :dateDebut
            AND CAST(r.created_at AS date) <= :dateFin
            AND (:userId IS NULL OR r.user_id = :userId)
            AND (:typeRepartition IS NULL OR r.type_repartition = :typeRepartition)
            AND (:storageId IS NULL OR dest_sp.storage_id = :storageId OR src_sp.storage_id = :storageId)
            AND (:stockProduitId IS NULL OR dest_sp.id = :stockProduitId OR src_sp.id = :stockProduitId)
            AND (:searchTerm IS NULL OR :searchTerm = '' OR
                 UPPER(p.libelle) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR
                 UPPER(p.code_ean_labo) LIKE UPPER(CONCAT('%', :searchTerm, '%')))
        ORDER BY r.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(r.id)
        FROM repartition_stock_produit r
        INNER JOIN app_user u ON r.user_id = u.id
        INNER JOIN stock_produit dest_sp ON r.stock_produit_destination_id = dest_sp.id
        INNER JOIN produit p ON dest_sp.produit_id = p.id
        INNER JOIN storage dest_s ON dest_sp.storage_id = dest_s.id
        LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
        LEFT JOIN stock_produit src_sp ON r.stock_produit_source_id = src_sp.id
        LEFT JOIN storage src_s ON src_sp.storage_id = src_s.id
        WHERE CAST(r.created_at AS date) >= :dateDebut
            AND CAST(r.created_at AS date) <= :dateFin
            AND (:userId IS NULL OR r.user_id = :userId)
            AND (:typeRepartition IS NULL OR r.type_repartition = :typeRepartition)
            AND (:storageId IS NULL OR dest_sp.storage_id = :storageId OR src_sp.storage_id = :storageId)
            AND (:stockProduitId IS NULL OR dest_sp.id = :stockProduitId OR src_sp.id = :stockProduitId)
            AND (:searchTerm IS NULL OR :searchTerm = '' OR
                 UPPER(p.libelle) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR
                 UPPER(p.code_ean_labo) LIKE UPPER(CONCAT('%', :searchTerm, '%')))
        """,
        nativeQuery = true)
    Page<RepartitionStockProduitProjection> findRepartitionStockProduits(
        @Param("userId") Integer userId,
        @Param("typeRepartition") Integer typeRepartition,
        @Param("storageId") Integer storageId,
        @Param("stockProduitId") Integer stockProduitId,
        @Param("searchTerm") String searchTerm,
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin,
        Pageable pageable
    );
}
