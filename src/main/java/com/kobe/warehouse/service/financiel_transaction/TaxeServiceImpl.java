package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TaxeServiceImpl implements TaxeService {
  private static final Logger log = LoggerFactory.getLogger(TaxeServiceImpl.class);
  private static final String SELECT_TAXE =
      """
SELECT %s  sl.tax_value as codeTva ,SUM(sl.tax_amount) as montantTaxe,
SUM(sl.sales_amount) as montantTtc, SUM(sl.cost_amount) as montantAchat,SUM(sl.ht_amount) as montantHt,SUM(sl.discount_amount) as montantRemise,
SUM(sl.net_amount) as montantNet,SUM(sl.montant_tva_ug) as montantTvaUg,SUM(sl.discount_amount_ug) as montantRemiseUg,SUM(sl.amount_to_be_taken_into_account) as amountToBeTakenIntoAccount,
SUM(sl.quantity_ug*sl.regular_unit_price) as montantTtcUg FROM sales_line sl JOIN sales s ON s.id=sl.sales_id

""";
  private static final String DATE_COLUMN = " DATE_FORMAT(s.updated_at, '%Y-%m-%d') AS mvtDate,";
  private static final String GROUP_BY_DATE = " group by sl.tax_value, mvtDate ORDER BY mvtDate";
  private static final String GROUP_BY_TVA_CODE = " group by sl.tax_value ORDER BY sl.tax_value";
  private static final String WHERE_CLAUSE =
      " WHERE DATE(s.updated_at) BETWEEN :fromDate AND :toDate AND s.statut IN (:statuts) AND s.dtype IN (:typesVente) AND s.ca in (:ca) AND sl.to_ignore=:ignoreSomeTaxe";
  private final EntityManager entityManager;

  public TaxeServiceImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public TaxeWrapperDTO fetchTaxe(
      LocalDate fromDate,
      LocalDate toDate,
      Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      Set<SalesStatut> statuts,
      Set<TypeVente> typeVentes,
      String groupeBy,
      boolean ignoreSomeTaxe) {
    TaxeWrapperDTO taxeWrapperDTO = new TaxeWrapperDTO();

    buildFromTuple(
        fetchTaxe(
            groupeBy,
            fromDate,
            toDate,
            statuts,
            typeVentes,
            categorieChiffreAffaires,
            ignoreSomeTaxe),
        "daily".equals(groupeBy),
        taxeWrapperDTO);
    return taxeWrapperDTO;
  }

  private String buildQuery(String groupeBy) {
    if (StringUtils.hasText(groupeBy) && "daily".equals(groupeBy)) {
      return String.format(SELECT_TAXE, DATE_COLUMN) + WHERE_CLAUSE + GROUP_BY_DATE;
    }
    return String.format(SELECT_TAXE, "") + WHERE_CLAUSE + GROUP_BY_TVA_CODE;
  }

  private List<Tuple> fetchTaxe(
      String groupeBy,
      LocalDate fromDate,
      LocalDate toDate,
      Set<SalesStatut> statuts,
      Set<TypeVente> typeVentes,
      Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      boolean ignoreSomeTaxe) {
    if (Objects.isNull(typeVentes) || typeVentes.isEmpty()) {
      typeVentes = Set.of(TypeVente.CASH_SALE, TypeVente.CREDIT_SALE, TypeVente.VENTES_DEPOT_AGREE);
    }
    if (Objects.isNull(statuts) || statuts.isEmpty()) {
      statuts = Set.of(SalesStatut.CLOSED);
    }
    if (Objects.isNull(categorieChiffreAffaires) || categorieChiffreAffaires.isEmpty()) {
      categorieChiffreAffaires = Set.of(CategorieChiffreAffaire.CA);
    }
    if (Objects.isNull(fromDate)) {
      fromDate = LocalDate.now();
    }
    if (Objects.isNull(toDate)) {
      toDate = fromDate;
    }

    return entityManager
        .createNativeQuery(buildQuery(groupeBy), Tuple.class)
        .setParameter("fromDate", fromDate)
        .setParameter("toDate", toDate)
        .setParameter(
            "statuts",
            statuts.stream().map(e -> "'" + e.name() + "'").collect(Collectors.joining(",")))
        .setParameter(
            "typesVente",
            typeVentes.stream().map(e -> "'" + e.getValue() + "'").collect(Collectors.joining(",")))
        .setParameter(
            "ca",
            categorieChiffreAffaires.stream()
                .map(e -> "'" + e.name() + "'")
                .collect(Collectors.joining(",")))
        .setParameter("ignoreSomeTaxe", ignoreSomeTaxe)
        .getResultList();
  }

  private List<TaxeDTO> buildFromTuple(
      List<Tuple> tuples, boolean groupByDate, TaxeWrapperDTO taxeWrapperDTO) {
    taxeWrapperDTO.setGroupDate(groupByDate);
    return tuples.stream()
        .map(
            t -> {
              TaxeDTO taxeDTO = new TaxeDTO();
              if (groupByDate) {
                taxeDTO.setMvtDate(LocalDate.parse(t.get("mvtDate", String.class)));
              }
              taxeDTO.setCodeTva(t.get("codeTva", Integer.class));
              taxeDTO.setMontantTaxe(t.get("montantTaxe", BigDecimal.class).longValue());
              taxeDTO.setMontantTtc(t.get("montantTtc", BigDecimal.class).longValue());
              taxeDTO.setMontantAchat(t.get("montantAchat", BigDecimal.class).longValue());
              taxeDTO.setMontantHt(t.get("montantHt", BigDecimal.class).longValue());
              taxeDTO.setMontantRemise(t.get("montantRemise", BigDecimal.class).longValue());
              taxeDTO.setMontantNet(t.get("montantNet", BigDecimal.class).longValue());
              taxeDTO.setMontantTvaUg(t.get("montantTvaUg", BigDecimal.class).longValue());
              taxeDTO.setMontantRemiseUg(t.get("montantRemiseUg", BigDecimal.class).longValue());
              taxeDTO.setAmountToBeTakenIntoAccount(
                  t.get("amountToBeTakenIntoAccount", BigDecimal.class).longValue());
              taxeDTO.setMontantTtcUg(t.get("montantTtcUg", BigDecimal.class).longValue());
              updateTaxeWrapper(taxeWrapperDTO, taxeDTO);
              return taxeDTO;
            })
        .toList();
  }

  private void updateTaxeWrapper(TaxeWrapperDTO taxeWrapper, TaxeDTO taxe) {
    taxeWrapper.setMontantHt(taxeWrapper.getMontantHt() + taxe.getMontantHt());
    taxeWrapper.setMontantTaxe(taxeWrapper.getMontantTaxe() + taxe.getMontantTaxe());
    taxeWrapper.setMontantTtc(taxeWrapper.getMontantTtc() + taxe.getMontantTtc());
    taxeWrapper.setMontantNet(taxeWrapper.getMontantNet() + taxe.getMontantNet());
    taxeWrapper.setMontantRemise(taxeWrapper.getMontantRemise() + taxe.getMontantRemise());
    taxeWrapper.setMontantAchat(taxeWrapper.getMontantAchat() + taxe.getMontantAchat());
    taxeWrapper.setMontantRemiseUg(taxeWrapper.getMontantRemiseUg() + taxe.getMontantRemiseUg());
    taxeWrapper.setMontantTvaUg(taxeWrapper.getMontantTvaUg() + taxe.getMontantTvaUg());
    taxeWrapper.setAmountToBeTakenIntoAccount(
        taxeWrapper.getAmountToBeTakenIntoAccount() + taxe.getAmountToBeTakenIntoAccount());
    taxeWrapper.setMontantTtcUg(taxeWrapper.getMontantTtcUg() + taxe.getMontantTtcUg());
  }
}
