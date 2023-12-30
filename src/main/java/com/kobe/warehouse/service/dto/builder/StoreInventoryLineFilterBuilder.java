package com.kobe.warehouse.service.dto.builder;

import com.kobe.warehouse.service.dto.StoreInventoryLineExport;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.util.Objects;

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
 ,false AS updated,%d AS storyId FROM produit p WHERE status=0 {famille_close}
""";
  public static final String SQL_ALL_INSERT =
      """
  INSERT INTO  store_inventory_line (produit_id,updated_at,updated,store_inventory_id) SELECT p.id,NOW() AS updatedAt
   ,false AS updated,%d AS storyId FROM produit p,rayon_produit rp, rayon r, storage s WHERE  status=0 AND p.id=rp.produit_id AND r.id=rp.rayon_id AND s.id=r.storage_id
""";
  public static final String SUMMARY_SQL =
      """
SELECT SUM(i.quantity_on_hand*i.inventory_value_cost) as costValueAfter,SUM(i.quantity_on_hand*i.last_unit_price) as amountValueAfter
,SUM(i.quantity_init*i.inventory_value_cost) as costValueBegin,SUM(i.quantity_init*i.last_unit_price) as amountValueBegin,SUM(i.gap*i.inventory_value_cost) as gapCost,SUM(i.gap*i.last_unit_price) as gapAmount
 FROM store_inventory_line i where i.store_inventory_id=?1
""";
  public static final String EXPORT_QUERY =
      """
SELECT r.id as rayon_id,s.id AS storage_id, fm.code AS famillyCode, fm.libelle AS famillyLibelle,fm.id AS famillyId,  a.gap,r.code AS code_rayon,  a.inventory_value_cost,a.quantity_init,a.quantity_on_hand,a.last_unit_price,p.libelle AS produit_libelle,p.code_ean,r.libelle AS rayon_libelle,s.name AS storage_name,fp.code_cip AS produit_code_cip,fp.prix_uni ,fp.prix_achat  FROM store_inventory_line a JOIN warehouse.produit p on p.id = a.produit_id
    JOIN warehouse.fournisseur_produit fp  ON p.id = fp.produit_id JOIN warehouse.famille_produit fm ON fm.id=p.famille_id
    LEFT JOIN warehouse.rayon_produit rp on p.id = rp.produit_id LEFT JOIN warehouse.rayon r on rp.rayon_id = r.id LEFT JOIN warehouse.storage s  ON r.storage_id = s.id
WHERE  a.store_inventory_id=?1 AND fP.principal %s ORDER BY {order_by} fp.code_cip
""";
  public static final String EXPORT_RAYON_CLOSE_QUERY = " AND r.id=%d ";
  public static final String EXPORT_STORAGE_CLOSE_QUERY = " AND s.id=%d ";

  public static StoreInventoryLineRecord buildStoreInventoryLineRecordRecord(
      Tuple tuple, int currentStock) {

    if (Objects.isNull(tuple.get("produitId", Long.class))) return null;
    boolean updated = tuple.get("updated", Boolean.class);

    return new StoreInventoryLineRecord(
        tuple.get("produitId", Long.class).intValue(),
        tuple.get("code_cip", String.class),
        tuple.get("code_ean", String.class),
        tuple.get("libelle", String.class),
        tuple.get("id", Long.class),
        tuple.get("gap", Integer.class),
        tuple.get("quantity_on_hand", Integer.class),
        currentStock,
        updated,
        tuple.get("prix_achat", Integer.class),
        tuple.get("prix_uni", Integer.class));
  }

  public static StoreInventoryLineExport buildStoreInventoryLineExportRecord(Tuple tuple) {

    return new StoreInventoryLineExport(
        tuple.get("gap", Integer.class),
        tuple.get("inventory_value_cost", Integer.class),
        tuple.get("quantity_init", Integer.class),
        tuple.get("quantity_on_hand", Integer.class),
        tuple.get("produit_code_cip", String.class),
        tuple.get("code_ean", String.class),
        tuple.get("produit_libelle", String.class),
        tuple.get("rayon_libelle", String.class),
        tuple.get("storage_name", String.class),
        tuple.get("prix_uni", Integer.class),
        tuple.get("prix_achat", Integer.class),
        tuple.get("last_unit_price", Integer.class),
        tuple.get("rayon_id", Long.class),
        tuple.get("storage_id", Long.class),
        tuple.get("code_rayon", String.class),
        tuple.get("famillyCode", String.class),
        tuple.get("famillyLibelle", String.class),
        tuple.get("famillyId", Long.class));
  }

  public static StoreInventorySummaryRecord buildSammary(Tuple tuple) {
    return new StoreInventorySummaryRecord(
        tuple.get("costValueBegin", BigDecimal.class),
        tuple.get("costValueAfter", BigDecimal.class),
        tuple.get("amountValueBegin", BigDecimal.class),
        tuple.get("amountValueAfter", BigDecimal.class),
        tuple.get("gapCost", BigDecimal.class),
        tuple.get("gapAmount", BigDecimal.class));
  }
}
