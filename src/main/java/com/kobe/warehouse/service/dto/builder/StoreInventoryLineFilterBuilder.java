package com.kobe.warehouse.service.dto.builder;

import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import java.math.BigInteger;
import java.util.Objects;
import javax.persistence.Tuple;

public class StoreInventoryLineFilterBuilder {
  public static final String BASE_QUERY =
      """
SELECT p.id AS produitId,p.code_ean,
       p.libelle,fp.code_cip,
       a.quantity_on_hand,a.gap,a.updated_at,a.id As id,fp.prix_achat,fp.prix_uni
FROM produit p JOIN (SELECT fp.code_cip,fp.produit_id,fp.prix_achat,fp.prix_uni FROM fournisseur_produit fp WHERE fp.principal ) AS fp ON p.id=fp.produit_id
    LEFT JOIN (SELECT  a.quantity_on_hand,a.updated,a.quantity_init,a.gap,a.updated_at,a.id As id,a.produit_id FROM store_inventory_line a WHERE a.store_inventory_id=?1 ) AS a ON p.id=a.produit_id %s ORDER BY fp.code_cip

""";
  public static final String RAYON_STATEMENT = " JOIN rayon_produit rp on p.id = rp.produit_id ";
  public static final String STOCKAGE_STATEMENT =
      " JOIN rayon_produit rp on p.id = rp.produit_id JOIN rayon st ON rp.rayon_id =st.id ";
  public static final String RAYON_STATEMENT_WHERE = " rp.rayon_id =%d ";
  public static final String STOCKAGE_STATEMENT_WHERE = " st.storage_id IN(%s) ";
  public static final String LIKE_STATEMENT_WHERE =
      " ( p.libelle LIKE '%s' or fp.code_cip LIKE '%s' or p.code_ean LIKE '%s' ) ";

  public static final String COUNT =
      """
SELECT COUNT(p.id) FROM produit p JOIN (SELECT fp.code_cip,fp.produit_id FROM fournisseur_produit fp WHERE fp.principal ) AS fp ON p.id=fp.produit_id
 LEFT JOIN (SELECT  a.quantity_on_hand,a.updated,a.quantity_init,a.gap,a.updated_at,a.id As id,a.produit_id
FROM store_inventory_line a WHERE a.store_inventory_id=?1 ) AS a ON p.id=a.produit_id %s
""";

  public static StoreInventoryLineRecord buildStoreInventoryLineRecordRecord(
      Tuple tuple, int currentStock) {

    if (Objects.isNull(tuple.get("produitId", BigInteger.class))) return null;
    System.err.println(tuple.get("updated_at"));

    boolean updated = Objects.nonNull(tuple.get("id", BigInteger.class));
    return new StoreInventoryLineRecord(
        tuple.get("produitId", BigInteger.class).intValue(),
        tuple.get("code_cip", String.class),
        tuple.get("code_ean", String.class),
        tuple.get("libelle", String.class),
        tuple.get("id", BigInteger.class),
        tuple.get("gap", Integer.class),
        tuple.get("quantity_on_hand", Integer.class),
        currentStock,
        updated,  tuple.get("prix_achat", Integer.class),tuple.get("prix_uni", Integer.class));
  }
}
