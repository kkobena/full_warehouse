package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ProduitFavori;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProduitFavoriRepository extends JpaRepository<ProduitFavori, Integer> {
    List<ProduitFavori> findAllByMagasinIdOrderByOrdreAscIdAsc(Integer magasinId);

    @Query("select f.produit.id from ProduitFavori f where f.magasin.id = :magasinId order by f.ordre, f.id")
    List<Integer> findProduitIdsByMagasinId(@Param("magasinId") Integer magasinId);

    /**
     * Produits fréquents au comptoir (vue {@code mv_produits_frequents_comptoir}) qui ne sont pas déjà épinglés, du plus
     * fréquent au moins fréquent : {@code [produit_id, nb_ventes, qte_vendue]}.
     */
    @Query(
        value = """
        SELECT m.produit_id, m.nb_ventes, m.qte_vendue
        FROM mv_produits_frequents_comptoir m
        WHERE m.magasin_id = :magasinId
          AND NOT EXISTS (SELECT 1 FROM produit_favori f WHERE f.magasin_id = m.magasin_id AND f.produit_id = m.produit_id)
        ORDER BY m.rang
        LIMIT :limite
        """,
        nativeQuery = true
    )
    List<Object[]> findSuggestions(@Param("magasinId") Integer magasinId, @Param("limite") int limite);

    Optional<ProduitFavori> findByMagasinIdAndProduitId(Integer magasinId, Integer produitId);

    long countByMagasinId(Integer magasinId);

    @Query("select coalesce(max(f.ordre), 0) from ProduitFavori f where f.magasin.id = :magasinId")
    int maxOrdre(@Param("magasinId") Integer magasinId);
}
