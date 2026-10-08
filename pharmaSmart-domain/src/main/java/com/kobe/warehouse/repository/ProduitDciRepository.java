package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ProduitDci;
import com.kobe.warehouse.service.dto.controle.MoleculeProduitDTO;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProduitDciRepository extends JpaRepository<ProduitDci, Integer> {
    /** DCI du catalogue portées par ces produits. */
    @Query("select pd from ProduitDci pd join fetch pd.dci where pd.produit.id in :produitIds")
    List<ProduitDci> findAvecDci(@Param("produitIds") Collection<Integer> produitIds);

    /** Molécules du référentiel des produits donnés, par la DCI que porte le produit (seules les DCI reliées au référentiel). */
    @Query(
        "select new com.kobe.warehouse.service.dto.controle.MoleculeProduitDTO(pd.produit.id, rd.id, rd.libelle) " +
        "from ProduitDci pd, RefDci rd where rd.dci = pd.dci and pd.produit.id in :produitIds"
    )
    List<MoleculeProduitDTO> findMolecules(@Param("produitIds") Collection<Integer> produitIds);
}
