package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.enumeration.StatutPromotion;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    /**
     * Les promotions que le moteur peut appliquer à une vente : validées, visant le magasin de la
     * vente, en vigueur à sa date. Les produits sont chargés d'un coup : le moteur les parcourt tous.
     */
    @Query("""
            SELECT DISTINCT p FROM Promotion p
              JOIN p.magasins m
              LEFT JOIN FETCH p.produits pp
              LEFT JOIN FETCH pp.produit
             WHERE m.id = :magasinId
               AND p.statut = :statut
               AND p.dateDebut <= :date
               AND p.dateFin >= :date
            """)
    List<Promotion> findEnVigueur(
            @Param("magasinId") Integer magasinId,
            @Param("date") LocalDate date,
            @Param("statut") StatutPromotion statut);

    @Query("""
            SELECT DISTINCT p FROM Promotion p
              JOIN p.magasins m
             WHERE m.id = :magasinId
             ORDER BY p.dateDebut DESC, p.id DESC
            """)
    List<Promotion> findAllByMagasinId(@Param("magasinId") Integer magasinId);

    @Query("""
            SELECT p FROM Promotion p
              LEFT JOIN FETCH p.produits pp
              LEFT JOIN FETCH pp.produit
             WHERE p.id = :id
            """)
    Optional<Promotion> findOneWithProduits(@Param("id") Long id);

    /**
     * Unités déjà remisées par chaque promotion, sur les ventes clôturées et non annulées : le
     * compteur du plafond total. Une vente annulée est marquée {@code canceled} et sa contrepassation
     * a le statut {@code CANCELED} : ni l'une ni l'autre ne compte, l'annulation rend les unités. Un
     * panier en cours ne compte pas non plus : il ne réserve rien tant qu'il n'est pas clôturé.
     */
    @Query("""
            SELECT sl.promotionId AS promotionId, COALESCE(SUM(sl.quantitePromo), 0) AS unites
              FROM SalesLine sl
             WHERE sl.promotionId IN :ids
               AND sl.sales.statut = com.kobe.warehouse.domain.enumeration.SalesStatut.CLOSED
               AND sl.sales.canceled = false
             GROUP BY sl.promotionId
            """)
    List<UnitesRemisees> sommerUnitesRemisees(@Param("ids") Collection<Long> ids);

    /**
     * Les rayons des stockages du magasin qui rangent au moins un article actif. Le nom du stockage
     * départage deux rayons homonymes (rayon « Pansements » du stock principal et de la réserve).
     */
    @Query("""
            SELECT r.id AS id, r.libelle AS libelle, s.name AS stockage, COUNT(DISTINCT rp.produit.id) AS articles
              FROM RayonProduit rp
              JOIN rp.rayon r
              JOIN r.storage s
             WHERE s.magasin.id = :magasinId
               AND rp.produit.status = com.kobe.warehouse.domain.enumeration.Status.ENABLE
             GROUP BY r.id, r.libelle, s.name
             ORDER BY r.libelle, s.name
            """)
    List<OptionPerimetre> rayonsDuMagasin(@Param("magasinId") Integer magasinId);

    /** Les familles qui portent au moins un article actif ; une famille est commune à tous les magasins. */
    @Query("""
            SELECT f.id AS id, f.libelle AS libelle, '' AS stockage, COUNT(p.id) AS articles
              FROM Produit p
              JOIN p.famille f
             WHERE p.status = com.kobe.warehouse.domain.enumeration.Status.ENABLE
             GROUP BY f.id, f.libelle
             ORDER BY f.libelle
            """)
    List<OptionPerimetre> famillesActives();

    /** Les articles actifs d'un rayon, pourvu qu'il appartienne au magasin. */
    @Query("""
            SELECT DISTINCT rp.produit FROM RayonProduit rp
             WHERE rp.rayon.id = :rayonId
               AND rp.rayon.storage.magasin.id = :magasinId
               AND rp.produit.status = com.kobe.warehouse.domain.enumeration.Status.ENABLE
            """)
    List<Produit> produitsDuRayon(@Param("rayonId") Integer rayonId, @Param("magasinId") Integer magasinId);

    @Query("""
            SELECT p FROM Produit p
             WHERE p.famille.id = :familleId
               AND p.status = com.kobe.warehouse.domain.enumeration.Status.ENABLE
            """)
    List<Produit> produitsDeLaFamille(@Param("familleId") Integer familleId);

    /**
     * Les ventes d'une liste d'articles sur une période, article par article, et la part de ces
     * ventes qui porte la promotion. Ventes clôturées, non annulées, du chiffre d'affaires : une vente
     * dépôt (transfert) ne compte pas. La marge déduit le coût d'achat enregistré sur la ligne.
     */
    @Query("""
            SELECT sl.produit.id AS produitId,
                   COALESCE(SUM(sl.quantityRequested), 0) AS unites,
                   COALESCE(SUM(sl.salesAmount - sl.discountAmount), 0) AS chiffreAffaires,
                   COALESCE(SUM(sl.salesAmount - sl.discountAmount - sl.quantityRequested * sl.costAmount), 0) AS marge,
                   COALESCE(SUM(CASE WHEN sl.promotionId = :promotionId THEN sl.discountAmount ELSE 0 END), 0) AS remise,
                   COALESCE(SUM(CASE WHEN sl.promotionId = :promotionId THEN sl.quantitePromo ELSE 0 END), 0)
                       AS unitesRemisees
              FROM SalesLine sl
              JOIN sl.sales s
             WHERE sl.produit.id IN :produits
               AND s.magasin.id = :magasinId
               AND s.saleDate BETWEEN :debut AND :fin
               AND s.statut = com.kobe.warehouse.domain.enumeration.SalesStatut.CLOSED
               AND s.canceled = false
               AND s.categorieChiffreAffaire = com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire.CA
             GROUP BY sl.produit.id
            """)
    List<VentesArticle> ventesParArticle(
            @Param("promotionId") Long promotionId,
            @Param("produits") Collection<Integer> produits,
            @Param("magasinId") Integer magasinId,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    /** Tickets et clients identifiés distincts des ventes qui portent ces articles sur la période. */
    @Query("""
            SELECT COUNT(DISTINCT s.id) AS tickets, COUNT(DISTINCT c.id) AS clients
              FROM SalesLine sl
              JOIN sl.sales s
              LEFT JOIN s.customer c
             WHERE sl.produit.id IN :produits
               AND s.magasin.id = :magasinId
               AND s.saleDate BETWEEN :debut AND :fin
               AND s.statut = com.kobe.warehouse.domain.enumeration.SalesStatut.CLOSED
               AND s.canceled = false
               AND s.categorieChiffreAffaire = com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire.CA
            """)
    Frequentation frequentationDesArticles(
            @Param("produits") Collection<Integer> produits,
            @Param("magasinId") Integer magasinId,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    /** Tickets et clients identifiés distincts des ventes qui portent au moins une ligne de la promotion. */
    @Query("""
            SELECT COUNT(DISTINCT s.id) AS tickets, COUNT(DISTINCT c.id) AS clients
              FROM SalesLine sl
              JOIN sl.sales s
              LEFT JOIN s.customer c
             WHERE sl.promotionId = :promotionId
               AND s.magasin.id = :magasinId
               AND s.statut = com.kobe.warehouse.domain.enumeration.SalesStatut.CLOSED
               AND s.canceled = false
               AND s.categorieChiffreAffaire = com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire.CA
            """)
    Frequentation frequentationDeLaPromotion(
            @Param("promotionId") Long promotionId, @Param("magasinId") Integer magasinId);

    interface VentesArticle {
        Integer getProduitId();

        Long getUnites();

        Long getChiffreAffaires();

        Long getMarge();

        Long getRemise();

        Long getUnitesRemisees();
    }

    interface Frequentation {
        Long getTickets();

        Long getClients();
    }

    interface OptionPerimetre {
        Integer getId();

        String getLibelle();

        String getStockage();

        Long getArticles();
    }

    interface UnitesRemisees {
        Long getPromotionId();

        Long getUnites();
    }
}
