package com.kobe.warehouse.service.dto.builder;

import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import javax.persistence.Tuple;

public class StoreInventoryLineFilterBuilder {
  public static final String BASE_QUERY =
      """
SELECT p.id AS produitId,p.code_ean,
       p.libelle,fp.code_cip,
       a.quantity_on_hand,a.gap,a.updated_at,a.id As id,fp.prix_achat,fp.prix_uni,a.updated
FROM produit p JOIN (SELECT fp.code_cip,fp.produit_id,fp.prix_achat,fp.prix_uni FROM fournisseur_produit fp WHERE fp.principal ) AS fp ON p.id=fp.produit_id
JOIN store_inventory_line a ON p.id=a.produit_id {join_statement} WHERE a.store_inventory_id=?1 {join_statement_where} %s ORDER BY fp.code_cip

""";
  public static final String RAYON_STATEMENT = " join rayon_produit rp ON p.id = rp.produit_id ";

  public static final String RAYON_STATEMENT_WHERE = " AND rp.rayon_id =%d ";
  public static final String STOCKAGE_STATEMENT_WHERE =
      " AND rp.rayon_id IN(SELECT ry.id FROM rayon ry WHERE ry.storage_id =%d ) ";
  public static final String LIKE_STATEMENT_WHERE =
      " AND ( p.libelle LIKE '%s' or fp.code_cip LIKE '%s' or p.code_ean LIKE '%s' ) ";
  public static final String COUNT =
      """
SELECT COUNT(p.id)
FROM produit p JOIN (SELECT fp.code_cip,fp.produit_id,fp.prix_achat,fp.prix_uni FROM fournisseur_produit fp WHERE fp.principal ) AS fp ON p.id=fp.produit_id
JOIN store_inventory_line a ON p.id=a.produit_id {join_statement} WHERE a.store_inventory_id=?1 {join_statement_where} %s
""";
  public static final String SQL_ALL_INSERT_ALL =
      """
   INSERT INTO  store_inventory_line (produit_id,updated_at,updated,store_inventory_id) SELECT p.id,NOW() AS updatedAt
 ,false AS updated,%d AS storyId FROM produit p WHERE status=0
""";
  public static final String SQL_ALL_INSERT =
      """
  INSERT INTO  store_inventory_line (produit_id,updated_at,updated,store_inventory_id) SELECT p.id,NOW() AS updatedAt
   ,false AS updated,%d AS storyId FROM produit p,rayon_produit rp, rayon r, storage s WHERE  status=0 AND p.id=rp.produit_id AND r.id=rp.rayon_id AND s.id=r.storage_id
""";
  public static final String SUMMARY_SQL =
      """
SELECT SUM(i.quantity_on_hand*i.inventory_value_cost) as costValueAfter,SUM(i.quantity_on_hand*i.last_unit_price) as amountValueAfter
,SUM(i.quantity_init*i.inventory_value_cost) as costValueBegin,SUM(i.quantity_init*i.last_unit_price) as amountValueBegin
FROM store_inventory_line i where i.store_inventory_id=?1
""";
  public static final String PROC_CLOSE_INVENTORY = "proc_close_inventory";

  public static StoreInventoryLineRecord buildStoreInventoryLineRecordRecord(
      Tuple tuple, int currentStock) {

    if (Objects.isNull(tuple.get("produitId", BigInteger.class))) return null;
    boolean updated = tuple.get("updated", Boolean.class);

    return new StoreInventoryLineRecord(
        tuple.get("produitId", BigInteger.class).intValue(),
        tuple.get("code_cip", String.class),
        tuple.get("code_ean", String.class),
        tuple.get("libelle", String.class),
        tuple.get("id", BigInteger.class),
        tuple.get("gap", Integer.class),
        tuple.get("quantity_on_hand", Integer.class),
        currentStock,
        updated,
        tuple.get("prix_achat", Integer.class),
        tuple.get("prix_uni", Integer.class));
  }

  public static StoreInventorySummaryRecord buildSammary(Tuple tuple) {
    return new StoreInventorySummaryRecord(
        tuple.get("costValueBegin", BigDecimal.class),
        tuple.get("costValueAfter", BigDecimal.class),
        tuple.get("amountValueBegin", BigDecimal.class),
        tuple.get("amountValueAfter", BigDecimal.class));
  }
}
