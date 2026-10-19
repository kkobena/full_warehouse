package com.kobe.warehouse.repository.exports;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import java.util.EnumMap;
import java.util.Map;

/**
 * La requête de chaque export de données, en morceaux : les filtres choisis viennent s'ajouter aux conditions avant le tri.
 *
 * <p>Les alias sont posés une fois pour toutes et valent pour tous les exports — {@code fam}, {@code lab}, {@code four},
 * {@code forme}, {@code gam}, {@code mag}, {@code vend}, {@code caiss}, {@code util}, {@code pm} — afin qu'un prédicat de
 * {@link com.kobe.warehouse.domain.enumeration.FiltreExport} s'écrive une seule fois. Les jointures correspondantes sont donc
 * présentes dès qu'un export annonce le filtre, même s'il n'affiche pas la colonne ; toutes sont vers un seul élément, elles ne
 * multiplient pas les lignes.
 */
enum RequeteExport {
    VENTES(
        ExportDonnees.VENTES,
        "s",
        """
        SELECT s.saleDate, s.createdAt, s.numberTransaction, cast(s.natureVente AS String), cast(s.typePrescription AS String),
               concat(vend.firstName, ' ', vend.lastName), concat(caiss.firstName, ' ', caiss.lastName),
               s.salesAmount, s.htAmount, s.discountAmount, s.netAmount
        """,
        """
        FROM Sales s
        LEFT JOIN s.seller vend
        LEFT JOIN s.caissier caiss
        LEFT JOIN s.magasin mag
        """,
        "s.saleDate BETWEEN :du AND :au AND s.statut = :statut AND s.canceled = false AND s.categorieChiffreAffaire IN :categories",
        "ORDER BY s.saleDate, s.createdAt"
    ),
    LIGNES_VENTE(
        ExportDonnees.LIGNES_VENTE,
        "s",
        """
        SELECT s.saleDate, s.numberTransaction, fp.codeCip, p.libelle, fam.libelle,
               l.quantityRequested, l.quantitySold, l.regularUnitPrice, l.salesAmount, l.discountAmount, l.costAmount
        """,
        """
        FROM SalesLine l
        JOIN l.sales s
        JOIN l.produit p
        LEFT JOIN p.famille fam
        LEFT JOIN p.laboratoire lab
        LEFT JOIN p.fournisseurProduitPrincipal fp
        LEFT JOIN fp.fournisseur four
        LEFT JOIN s.seller vend
        LEFT JOIN s.magasin mag
        """,
        "s.saleDate BETWEEN :du AND :au AND s.statut = :statut AND s.canceled = false AND s.categorieChiffreAffaire IN :categories",
        "ORDER BY s.saleDate, s.numberTransaction"
    ),
    ENCAISSEMENTS(
        ExportDonnees.ENCAISSEMENTS,
        "",
        """
        SELECT t.transactionDate, t.createdAt, t.type, cast(t.typeFinancialTransaction AS String), pm.libelle, t.paidAmount,
               concat(caiss.firstName, ' ', caiss.lastName), t.transactionNumber
        """,
        """
        FROM PaymentTransaction t
        LEFT JOIN t.paymentMode pm
        LEFT JOIN t.cashRegister cr
        LEFT JOIN cr.user caiss
        """,
        "t.transactionDate BETWEEN :du AND :au",
        "ORDER BY t.transactionDate, t.createdAt"
    ),
    ACHATS(
        ExportDonnees.ACHATS,
        "",
        """
        SELECT c.receiptDate, c.receiptReference, four.libelle, fp.codeCip, p.libelle, ol.quantityReceived, ol.freeQty,
               ol.orderCostAmount, coalesce(ol.orderCostAmount, 0) * coalesce(ol.quantityReceived, 0)
        """,
        """
        FROM OrderLine ol
        JOIN ol.commande c
        LEFT JOIN c.fournisseur four
        JOIN ol.fournisseurProduit fp
        JOIN fp.produit p
        LEFT JOIN p.famille fam
        LEFT JOIN p.laboratoire lab
        """,
        "c.receiptDate BETWEEN :du AND :au AND c.orderStatus IN :statuts",
        "ORDER BY c.receiptDate, c.receiptReference"
    ),
    MOUVEMENTS_STOCK(
        ExportDonnees.MOUVEMENTS_STOCK,
        "",
        """
        SELECT t.transactionDate, t.createdAt, cast(t.mouvementType AS String), p.libelle, t.quantity, t.quantityBefor,
               t.quantityAfter, t.costAmount, t.regularUnitPrice, concat(util.firstName, ' ', util.lastName)
        """,
        """
        FROM InventoryTransaction t
        JOIN t.produit p
        LEFT JOIN t.user util
        LEFT JOIN p.famille fam
        LEFT JOIN p.laboratoire lab
        LEFT JOIN t.magasin mag
        """,
        "t.transactionDate BETWEEN :du AND :au",
        "ORDER BY t.createdAt"
    ),
    PRODUITS(
        ExportDonnees.PRODUITS,
        "",
        """
        SELECT fp.codeCip, p.libelle, fam.libelle, lab.libelle, tva.taux, p.regularUnitPrice, fp.prixAchat,
               (SELECT coalesce(sum(sp.qtyStock), 0) FROM StockProduit sp WHERE sp.produit = p)
        """,
        """
        FROM Produit p
        LEFT JOIN p.famille fam
        LEFT JOIN p.laboratoire lab
        LEFT JOIN p.tva tva
        LEFT JOIN p.forme forme
        LEFT JOIN p.gamme gam
        LEFT JOIN p.fournisseurProduitPrincipal fp
        LEFT JOIN fp.fournisseur four
        """,
        null,
        "ORDER BY p.libelle"
    ),
    CLIENTS(
        ExportDonnees.CLIENTS,
        "",
        "SELECT c.code, c.lastName, c.firstName, c.phone, c.email, c.datNaiss, c.sexe, c.createdAt",
        "FROM Customer c",
        null,
        "ORDER BY c.lastName, c.firstName"
    ),
    VENTES_JOUR_PRODUIT(
        ExportDonnees.VENTES_JOUR_PRODUIT,
        "l",
        """
        SELECT l.jour, fp.codeCip, p.libelle, fam.libelle, lab.libelle, four.libelle, forme.libelle, gam.libelle, tva.taux,
               cast(l.natureVente AS String), cast(l.typePrescription AS String), concat(vend.firstName, ' ', vend.lastName),
               mag.name, l.tauxRemise, cast(l.octroiRemise AS String), l.quantiteServie, l.montantTtc, l.montantHt, l.remise,
               l.coutHt, l.montantHt - l.coutHt
        """,
        """
        FROM PilotageVenteLigneJour l
        JOIN Produit p ON p.id = l.produitId
        LEFT JOIN p.famille fam
        LEFT JOIN p.laboratoire lab
        LEFT JOIN p.forme forme
        LEFT JOIN p.gamme gam
        LEFT JOIN p.tva tva
        LEFT JOIN p.fournisseurProduitPrincipal fp
        LEFT JOIN fp.fournisseur four
        LEFT JOIN AppUser vend ON vend.id = l.vendeurId
        LEFT JOIN Magasin mag ON mag.id = l.magasinId
        """,
        "l.jour BETWEEN :du AND :au AND l.categorieChiffreAffaire IN :categories",
        "ORDER BY l.jour, p.libelle"
    );

    private static final Map<ExportDonnees, RequeteExport> PAR_EXPORT = new EnumMap<>(ExportDonnees.class);

    static {
        for (RequeteExport requete : values()) {
            PAR_EXPORT.put(requete.export, requete);
        }
    }

    private final ExportDonnees export;
    private final String racineVente;
    private final String projection;
    private final String origine;
    private final String conditions;
    private final String tri;

    RequeteExport(ExportDonnees export, String racineVente, String projection, String origine, String conditions, String tri) {
        this.export = export;
        this.racineVente = racineVente;
        this.projection = projection;
        this.origine = origine;
        this.conditions = conditions;
        this.tri = tri;
    }

    static RequeteExport lirePour(ExportDonnees export) {
        return PAR_EXPORT.get(export);
    }

    /** Alias de l'entité qui porte la nature et le type de prescription de la vente ; vide pour un export qui n'en lit pas. */
    String getRacineVente() {
        return racineVente;
    }

    String getProjection() {
        return projection;
    }

    String getOrigine() {
        return origine;
    }

    /** Conditions de l'export lui-même, hors filtres choisis ; vide pour un export qui lit tout. */
    String getConditions() {
        return conditions;
    }

    String getTri() {
        return tri;
    }
}
