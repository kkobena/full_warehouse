package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TransactionTypeAffichage;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BalanceCaisseServiceImpl implements BalanceCaisseService {
  private static final Logger log = LoggerFactory.getLogger(BalanceCaisseServiceImpl.class);
  private final EntityManager entityManager;

  public BalanceCaisseServiceImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public BalanceCaisseWrapper getBalanceCaisse(
      LocalDate fromDate,
      LocalDate toDate,
      Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      Set<SalesStatut> statuts,
      Set<TypeVente> typeVentes) {

    Pair pair =
        buildBalanceCaisses(
            getSales(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes));
    List<BalanceCaisseDTO> mvt = buildMvt(getMvt(fromDate, toDate, categorieChiffreAffaires));
    List<BalanceCaisseDTO> balanceCaisses = (List<BalanceCaisseDTO>) pair.key();
    BalanceCaisseWrapper balanceCaisseWrapper = new BalanceCaisseWrapper();
    balanceCaisseWrapper.setBalanceCaisses(balanceCaisses);
    BalanceCaisseDTO depot = (BalanceCaisseDTO) pair.value();
    if (Objects.nonNull(depot)) {
      balanceCaisseWrapper.setMontantDepot(depot.getMontantDepot());
    }

    return balanceCaisseWrapper;
  }

  private List<Tuple> getSales(
      LocalDate fromDate,
      LocalDate toDate,
      Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      Set<SalesStatut> statuts,
      Set<TypeVente> typeVentes) {
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
    try {
      return entityManager
          .createNativeQuery(SALE_QUERY + SALE_QUERY_WHERE + SALE_QUERY_GROUP_BY, Tuple.class)
          .setParameter("fromDate", fromDate)
          .setParameter("toDate", toDate)
          .setParameter(
              "statuts",
              statuts.stream().map(e -> "'" + e.name() + "'").collect(Collectors.joining(",")))
          .setParameter(
              "typesVente",
              typeVentes.stream()
                  .map(e -> "'" + e.getValue() + "'")
                  .collect(Collectors.joining(",")))
          .setParameter(
              "ca",
              categorieChiffreAffaires.stream()
                  .map(e -> "'" + e.name() + "'")
                  .collect(Collectors.joining(",")))
          .getResultList();
    } catch (Exception e) {
      log.error("Error getSales", e);
    }

    return Collections.emptyList();
  }

  private com.kobe.warehouse.service.dto.Pair buildBalanceCaisses(List<Tuple> tuples) {
    List<BalanceCaisseDTO> balanceCaisseDTOS = new ArrayList<>();
    List<BalanceCaisseDTO> balanceCaisses = new ArrayList<>();
    for (Tuple tuple : tuples) {
      BalanceCaisseDTO balanceCaisseDTO = new BalanceCaisseDTO();
      balanceCaisseDTO.setTypeVente(TypeVente.fromValue(tuple.get("typeSale", String.class)));
      balanceCaisseDTO.setCount(tuple.get("numberCount", BigInteger.class).bitCount());
      balanceCaisseDTO.setMontantDiscount(
          tuple.get("montantDiscount", BigDecimal.class).longValue());
      balanceCaisseDTO.setMontantTtc(tuple.get("montantTtc", BigDecimal.class).longValue());
      balanceCaisseDTO.setMontantPaye(tuple.get("montantPaye", BigDecimal.class).longValue());
      balanceCaisseDTO.setMontantHt(tuple.get("montantHt", BigDecimal.class).longValue());
      balanceCaisseDTO.setMontantNet(tuple.get("montantNet", BigDecimal.class).longValue());
      balanceCaisseDTO.setModePaiement(tuple.get("modePaiement", String.class));
      balanceCaisseDTO.setLibelleModePaiement(tuple.get("libelleModePaiement", String.class));
      balanceCaisseDTOS.add(balanceCaisseDTO);
    }
    BalanceCaisseDTO depot = null;
    BalanceCaisseDTO vno = null;
    BalanceCaisseDTO vo = null;
    long totalVente = 0L;
    for (Entry<TypeVente, List<BalanceCaisseDTO>> typeVenteListEntry :
        balanceCaisseDTOS.stream()
            .collect(Collectors.groupingBy(BalanceCaisseDTO::getTypeVente))
            .entrySet()) {

      switch (typeVenteListEntry.getKey()) {
        case CASH_SALE:
          if (vno == null) {
            vno = new BalanceCaisseDTO();
          }
          vno.setTypeVeTypeAffichage(TransactionTypeAffichage.VNO);
          vno.setTypeSale("VNO");
          for (BalanceCaisseDTO e : typeVenteListEntry.getValue()) {
            upadateBalance(vno, e);
          }
          break;
        case CREDIT_SALE, VENTES_DEPOT_AGREE:
          if (vo == null) {
            vo = new BalanceCaisseDTO();
          }
          vo.setTypeVeTypeAffichage(TransactionTypeAffichage.VO);
          vo.setTypeSale("VO");
          for (BalanceCaisseDTO e : typeVenteListEntry.getValue()) {
            upadateBalance(vo, e);
          }
          break;
        case VENTES_DEPOTS:
          if (depot == null) {
            depot = new BalanceCaisseDTO();
          }
          depot.setTypeVeTypeAffichage(TransactionTypeAffichage.VENTES_DEPOTS);
          depot.setTypeSale(TransactionTypeAffichage.VENTES_DEPOTS.getValue());
          for (BalanceCaisseDTO e : typeVenteListEntry.getValue()) {
            upadateBalance(depot, e);
          }
          break;
      }
      if (vno != null) {
        balanceCaisses.add(vno);
      }
      if (vo != null) {
        balanceCaisses.add(vo);
      }
    }

    return new com.kobe.warehouse.service.dto.Pair(balanceCaisses, depot);
  }

  private void upadateBalance(BalanceCaisseDTO b, BalanceCaisseDTO e) {
    b.setCount(b.getCount() + e.getCount());
    b.setMontantDiscount(b.getMontantDiscount() + e.getMontantDiscount());
    b.setMontantTtc(b.getMontantTtc() + e.getMontantTtc());
    b.setMontantPaye(b.getMontantPaye() + e.getMontantPaye());
    b.setMontantHt(b.getMontantHt() + e.getMontantHt());
    b.setMontantNet(b.getMontantNet() + e.getMontantNet());
    b.setMontantCash(b.getMontantPaye());
  }

  private List<Tuple> getMvt(
      LocalDate fromDate, LocalDate toDate, Set<CategorieChiffreAffaire> categorieChiffreAffaires) {
    if (Objects.isNull(categorieChiffreAffaires) || categorieChiffreAffaires.isEmpty()) {
      categorieChiffreAffaires = Set.of(CategorieChiffreAffaire.CA);
    } else {
      if (categorieChiffreAffaires.contains(CategorieChiffreAffaire.CALLEBASE)
          && categorieChiffreAffaires.size() > 1) {
        categorieChiffreAffaires = Set.of(CategorieChiffreAffaire.CALLEBASE);
      }
      if (categorieChiffreAffaires.contains(CategorieChiffreAffaire.TO_IGNORE)
          && categorieChiffreAffaires.size() > 1) {
        categorieChiffreAffaires = Set.of(CategorieChiffreAffaire.TO_IGNORE);
      }
    }
    if (Objects.isNull(fromDate)) {
      fromDate = LocalDate.now();
    }
    if (Objects.isNull(toDate)) {
      toDate = fromDate;
    }
    try {
      return entityManager
          .createNativeQuery(MVT_QUERY)
          .setParameter("fromDate", fromDate)
          .setParameter("toDate", toDate)
          .setParameter(
              "categorie",
              categorieChiffreAffaires.stream()
                  .map(e -> String.valueOf(e.ordinal()))
                  .collect(Collectors.joining(",")))
          .getResultList();
    } catch (Exception e) {
      log.error("Error getMvt", e);
    }
    return Collections.emptyList();
  }

  private List<BalanceCaisseDTO> buildMvt(List<Tuple> tuples) {
    List<BalanceCaisseDTO> balanceCaisseDTOS = new ArrayList<>();
    for (Tuple tuple : tuples) {
      BalanceCaisseDTO balanceCaisseDTO = new BalanceCaisseDTO();
      balanceCaisseDTO.setMontantTtc(tuple.get("amount", BigDecimal.class).longValue());
      balanceCaisseDTO.setModePaiement(tuple.get("modePaiement", String.class));
      TypeFinancialTransaction typeFinancialTransaction =
          TypeFinancialTransaction.values()[tuple.get("typeTransaction", Byte.class)];
      balanceCaisseDTO.setLibelleModePaiement(tuple.get("libelleModePaiement", String.class));
      balanceCaisseDTO.setTypeVeTypeAffichage(
          typeFinancialTransaction.getTransactionTypeAffichage());
      balanceCaisseDTOS.add(balanceCaisseDTO);
    }
    return balanceCaisseDTOS;
  }
}
