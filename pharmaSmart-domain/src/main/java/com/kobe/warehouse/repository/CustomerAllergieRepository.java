package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CustomerAllergie;
import com.kobe.warehouse.domain.enumeration.StatutDci;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerAllergieRepository extends JpaRepository<CustomerAllergie, Integer> {
    List<CustomerAllergie> findAllByCustomerIdOrderByCreatedAtAsc(Integer customerId);

    /**
     * Allergies du client que le produit met en cause, avec la molécule du produit en cause.
     *
     * <p>Une association encore saisie comme une seule DCI (statut {@code COMPOSEE}, « A/B ») n'est
     * pas décomposée : on la rapproche alors par son libellé, sans quoi une allergie à l'amoxicilline
     * laisserait passer « AMOXICILLINE/ACIDE CLAVULANIQUE ».
     */
    @Query(
        """
            SELECT a, d.libelle FROM CustomerAllergie a JOIN a.dci ad, ProduitDci pd JOIN pd.dci d
            WHERE a.customerId = :customerId
              AND pd.produit.id = :produitId
              AND (d.id = ad.id OR (d.statut = :composee AND UPPER(d.libelle) LIKE CONCAT('%', UPPER(ad.libelle), '%')))
            """
    )
    List<Object[]> findAllergiesMisesEnCause(
        @Param("customerId") Integer customerId,
        @Param("produitId") Integer produitId,
        @Param("composee") StatutDci composee
    );
}
