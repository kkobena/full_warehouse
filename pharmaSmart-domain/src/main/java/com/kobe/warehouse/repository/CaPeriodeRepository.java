package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalesLine;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Le chiffre d'affaires d'une période, dans ses deux lectures.
 *
 * <p>Sert la cascade d'assiette de la ponction : le pharmacien voit d'abord ce que la période a
 * réellement produit, puis ce qu'il en reste après les exclusions déjà appliquées. Deux méthodes
 * plutôt qu'un drapeau, parce que ce sont deux chiffres différents qui s'affichent côte à côte —
 * un paramètre booléen les aurait rendus interchangeables à la lecture du code appelant.
 *
 * <p>Le périmètre est celui de la ponction (D8) : ventes comptant closes, ni annulées, ni importées,
 * ni ignorées, et relevant du chiffre d'affaires.
 */
@Repository
public interface CaPeriodeRepository extends JpaRepository<SalesLine, SaleLineId> {
    /** Ce que la période a réellement encaissé, avant tout retraitement. */
    @Query(
        """
        select coalesce(sum(cast(sl.quantityRequested * sl.regularUnitPrice as Long)), 0L)
        from SalesLine sl
        join sl.sales s
        where type(s) = com.kobe.warehouse.domain.CashSale
          and s.saleDate between :dateDebut and :dateFin
          and s.statut = com.kobe.warehouse.domain.enumeration.SalesStatut.CLOSED
          and s.canceled = false
          and s.imported = false
          and s.toIgnore = false
          and s.categorieChiffreAffaire = com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire.CA
          and s.magasin.id = :magasinId
          and sl.toIgnore = false
        """
    )
    long caReel(
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin,
        @Param("magasinId") Integer magasinId
    );

    /** Ce qu'il en reste une fois retirées les unités gratuites, les rayons et les tiers-payants exclus. */
    @Query(
        """
        select coalesce(sum(cast(coalesce(sl.amountToBeTakenIntoAccount, 0) as Long)), 0L)
        from SalesLine sl
        join sl.sales s
        where type(s) = com.kobe.warehouse.domain.CashSale
          and s.saleDate between :dateDebut and :dateFin
          and s.statut = com.kobe.warehouse.domain.enumeration.SalesStatut.CLOSED
          and s.canceled = false
          and s.imported = false
          and s.toIgnore = false
          and s.categorieChiffreAffaire = com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire.CA
          and s.magasin.id = :magasinId
          and sl.toIgnore = false
        """
    )
    long caApresExclusions(
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin,
        @Param("magasinId") Integer magasinId
    );
}
