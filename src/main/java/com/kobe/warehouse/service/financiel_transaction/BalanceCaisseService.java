package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import java.time.LocalDate;
import java.util.Set;

public interface BalanceCaisseService {
  String SALE_QUERY =
      """
SELECT s.dtype AS typeSale,COUNT(s.id) as numberCount,SUM(s.discount_amount) as montantDiscount,
SUM(s.sales_amount) as montantTtc,SUM(p.paid_amount) as montantPaye, SUM(s.ht_amount) as montantHt,
SUM(s.net_amount) as  montantNet,p.payment_mode_code as modePaiement,pm.libelle as libelleModePaiement,SUM(s.tax_amount) as montantTaxe,
SUM(s.cost_amount) as montantAchat, SUM(s.marge) as montantMarge,SUM(s.rest_to_pay) as montantDiffere,SUM(s.amount_to_be_paid) as amountToBePaid,SUM(s.amount_to_be_taken_into_account) as amountToBeTakenIntoAccount,
SUM(s.montant_net_ug) as montantNetUg,SUM(s.montant_ttc_ug) as montantTtcUg, SUM(s.montant_tva_ug) as montantHtUg,SUM(s.part_tiers_payant) AS partTiersPayant ,SUM(s.part_assure) AS partAssure
FROM sales s LEFT JOIN  payment p  ON p.sales_id=s.id JOIN payment_mode pm ON pm.code=p.payment_mode_code
""";
  String SALE_QUERY_WHERE =
      """
 WHERE DATE(s.updated_at) BETWEEN :fromDate AND :toDate AND s.statut IN (:statuts) AND s.dtype IN (:typesVente) AND s.ca in (:ca)
""";
  String SALE_QUERY_GROUP_BY =
      """
 GROUP BY s.dtype, p.payment_mode_code
""";
  String MVT_QUERY =
      """
SELECT SUM(p.amount) as amount,p.payment_mode_code AS modePaiement,pm.libelle as libelleModePaiement,p.type_transaction as typeTransaction  FROM  payment_transaction p  join payment_mode pm ON  p.payment_mode_code = pm.code
WHERE DATE(p.created_at) BETWEEN :toDate AND :toDate AND p.categorie_ca in (:categorie) group by p.payment_mode_code,p.type_transaction
""";

  BalanceCaisseWrapper getBalanceCaisse(
      LocalDate fromDate,
      LocalDate toDate,
      Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      Set<SalesStatut> statuts,
      Set<TypeVente> typeVentes);
}
