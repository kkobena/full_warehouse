package com.kobe.warehouse.repository;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import jakarta.persistence.QueryHint;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.Repository;

/**
 * Lectures en flux des exports de données ({@code ExportDonnees}) : une ligne = un tableau de valeurs, dans l'ordre des colonnes
 * déclarées par l'export. À consommer dans une transaction en lecture seule, flux fermé après usage.
 */
public interface ExportDonneesRepository extends Repository<Sales, SaleId> {
    String TAILLE_LOT = "500";

    @QueryHints({ @QueryHint(name = HINT_FETCH_SIZE, value = TAILLE_LOT), @QueryHint(name = HINT_READ_ONLY, value = "true") })
    @Query(
        """
        SELECT s.saleDate, s.createdAt, s.numberTransaction, cast(s.natureVente AS String), cast(s.typePrescription AS String),
               concat(v.firstName, ' ', v.lastName), concat(c.firstName, ' ', c.lastName),
               s.salesAmount, s.htAmount, s.discountAmount, s.netAmount
        FROM Sales s
        LEFT JOIN s.seller v
        LEFT JOIN s.caissier c
        WHERE s.saleDate BETWEEN :du AND :au
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
        ORDER BY s.saleDate, s.createdAt
        """
    )
    Stream<Object[]> lireVentes(LocalDate du, LocalDate au, SalesStatut statut, Set<CategorieChiffreAffaire> categories);

    @QueryHints({ @QueryHint(name = HINT_FETCH_SIZE, value = TAILLE_LOT), @QueryHint(name = HINT_READ_ONLY, value = "true") })
    @Query(
        """
        SELECT s.saleDate, s.numberTransaction, fp.codeCip, p.libelle, f.libelle,
               l.quantityRequested, l.quantitySold, l.regularUnitPrice, l.salesAmount, l.discountAmount, l.costAmount
        FROM SalesLine l
        JOIN l.sales s
        JOIN l.produit p
        LEFT JOIN p.famille f
        LEFT JOIN p.fournisseurProduitPrincipal fp
        WHERE s.saleDate BETWEEN :du AND :au
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
        ORDER BY s.saleDate, s.numberTransaction
        """
    )
    Stream<Object[]> lireLignesVente(LocalDate du, LocalDate au, SalesStatut statut, Set<CategorieChiffreAffaire> categories);

    @QueryHints({ @QueryHint(name = HINT_FETCH_SIZE, value = TAILLE_LOT), @QueryHint(name = HINT_READ_ONLY, value = "true") })
    @Query(
        """
        SELECT t.transactionDate, t.createdAt, t.type, cast(t.typeFinancialTransaction AS String), m.libelle, t.paidAmount,
               concat(u.firstName, ' ', u.lastName), t.transactionNumber
        FROM PaymentTransaction t
        LEFT JOIN t.paymentMode m
        LEFT JOIN t.cashRegister cr
        LEFT JOIN cr.user u
        WHERE t.transactionDate BETWEEN :du AND :au
        ORDER BY t.transactionDate, t.createdAt
        """
    )
    Stream<Object[]> lireEncaissements(LocalDate du, LocalDate au);

    @QueryHints({ @QueryHint(name = HINT_FETCH_SIZE, value = TAILLE_LOT), @QueryHint(name = HINT_READ_ONLY, value = "true") })
    @Query(
        """
        SELECT c.receiptDate, c.receiptReference, f.libelle, fp.codeCip, p.libelle, ol.quantityReceived, ol.freeQty, ol.orderCostAmount,
               coalesce(ol.orderCostAmount, 0) * coalesce(ol.quantityReceived, 0)
        FROM OrderLine ol
        JOIN ol.commande c
        LEFT JOIN c.fournisseur f
        JOIN ol.fournisseurProduit fp
        JOIN fp.produit p
        WHERE c.receiptDate BETWEEN :du AND :au
          AND c.orderStatus IN :statuts
        ORDER BY c.receiptDate, c.receiptReference
        """
    )
    Stream<Object[]> lireAchats(LocalDate du, LocalDate au, Collection<OrderStatut> statuts);

    @QueryHints({ @QueryHint(name = HINT_FETCH_SIZE, value = TAILLE_LOT), @QueryHint(name = HINT_READ_ONLY, value = "true") })
    @Query(
        """
        SELECT t.transactionDate, t.createdAt, cast(t.mouvementType AS String), p.libelle, t.quantity, t.quantityBefor, t.quantityAfter,
               t.costAmount, t.regularUnitPrice, concat(u.firstName, ' ', u.lastName)
        FROM InventoryTransaction t
        JOIN t.produit p
        LEFT JOIN t.user u
        WHERE t.transactionDate BETWEEN :du AND :au
        ORDER BY t.createdAt
        """
    )
    Stream<Object[]> lireMouvementsStock(LocalDate du, LocalDate au);

    @QueryHints({ @QueryHint(name = HINT_FETCH_SIZE, value = TAILLE_LOT), @QueryHint(name = HINT_READ_ONLY, value = "true") })
    @Query(
        """
        SELECT fp.codeCip, p.libelle, f.libelle, lab.libelle, tva.taux, p.regularUnitPrice, fp.prixAchat,
               (SELECT coalesce(sum(sp.qtyStock), 0) FROM StockProduit sp WHERE sp.produit = p)
        FROM Produit p
        LEFT JOIN p.famille f
        LEFT JOIN p.laboratoire lab
        LEFT JOIN p.tva tva
        LEFT JOIN p.fournisseurProduitPrincipal fp
        ORDER BY p.libelle
        """
    )
    Stream<Object[]> lireProduits();

    @QueryHints({ @QueryHint(name = HINT_FETCH_SIZE, value = TAILLE_LOT), @QueryHint(name = HINT_READ_ONLY, value = "true") })
    @Query(
        """
        SELECT c.code, c.lastName, c.firstName, c.phone, c.email, c.datNaiss, c.sexe, c.createdAt
        FROM Customer c
        ORDER BY c.lastName, c.firstName
        """
    )
    Stream<Object[]> lireClients();

    @QueryHints({ @QueryHint(name = HINT_FETCH_SIZE, value = TAILLE_LOT), @QueryHint(name = HINT_READ_ONLY, value = "true") })
    @Query(
        """
        SELECT l.jour, fpp.codeCip, p.libelle, f.libelle, lab.libelle, fr.libelle, fo.libelle, g.libelle, tva.taux,
               cast(l.natureVente AS String), cast(l.typePrescription AS String), concat(u.firstName, ' ', u.lastName), m.name,
               l.tauxRemise, cast(l.octroiRemise AS String), l.quantiteServie, l.montantTtc, l.montantHt, l.remise, l.coutHt,
               l.montantHt - l.coutHt
        FROM PilotageVenteLigneJour l
        JOIN Produit p ON p.id = l.produitId
        LEFT JOIN p.famille f
        LEFT JOIN p.laboratoire lab
        LEFT JOIN p.forme fo
        LEFT JOIN p.gamme g
        LEFT JOIN p.tva tva
        LEFT JOIN p.fournisseurProduitPrincipal fpp
        LEFT JOIN fpp.fournisseur fr
        LEFT JOIN AppUser u ON u.id = l.vendeurId
        LEFT JOIN Magasin m ON m.id = l.magasinId
        WHERE l.jour BETWEEN :du AND :au
          AND l.categorieChiffreAffaire IN :categories
        ORDER BY l.jour, p.libelle
        """
    )
    Stream<Object[]> lireVentesJourProduit(LocalDate du, LocalDate au, Set<CategorieChiffreAffaire> categories);
}
