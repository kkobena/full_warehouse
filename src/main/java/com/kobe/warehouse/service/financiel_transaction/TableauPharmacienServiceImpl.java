package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.financiel_transaction.dto.AchatDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TableauPharmacienServiceImpl implements TableauPharmacienService {
  private static final Logger log = LoggerFactory.getLogger(TableauPharmacienServiceImpl.class);
  private final EntityManager entityManager;

  public TableauPharmacienServiceImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public TableauPharmacienWrapper getTableauPharmacien(
      LocalDate fromDate,
      LocalDate toDate,
      Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      Set<SalesStatut> statuts,
      Set<TypeVente> typeVentes,
      String groupeBy) {
    return computeData(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy);
  }

  private List<Tuple> executeQuery(
      LocalDate fromDate,
      LocalDate toDate,
      Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      Set<SalesStatut> statuts,
      Set<TypeVente> typeVentes,
      String groupeBy) {
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
        .getResultList();
  }

  private List<Tuple> executeAchatQuery(LocalDate fromDate, LocalDate toDate, String groupeBy) {
    if (Objects.isNull(fromDate)) {
      fromDate = LocalDate.now();
    }
    if (Objects.isNull(toDate)) {
      toDate = fromDate;
    }
    return entityManager
        .createNativeQuery(buildAchatQuery(groupeBy), Tuple.class)
        .setParameter("fromDate", fromDate)
        .setParameter("toDate", toDate)
        .getResultList();
  }

  private List<AchatDTO> buildAchatsFromTuple(
      List<Tuple> tuples, TableauPharmacienWrapper tableauPharmacienWrapper) {
    return tuples.stream()
        .map(
            t -> {
              AchatDTO achatDTO = new AchatDTO();
              achatDTO
                  .setMontantNet(t.get("montantNet", BigDecimal.class).longValue())
                  .setMontantTtc(t.get("montantTtc", BigDecimal.class).longValue())
                  .setMontantHt(t.get("montantHt", BigDecimal.class).longValue())
                  .setMontantTaxe(t.get("montantTaxe", BigDecimal.class).longValue())
                  .setMontantRemise(t.get("montantRemise", BigDecimal.class).longValue())
                  .setGroupeGrossiste(t.get("groupeGrossiste", String.class))
                  .setGroupeGrossisteId(t.get("groupeGrossisteId", Long.class))
                  .setOrdreAffichage(t.get("ordreAffichage", Integer.class))
                  .setMvtDate(LocalDate.parse(t.get("mvtDate", String.class)));
              updateTableauPharmacienWrapper(tableauPharmacienWrapper, achatDTO);
              return achatDTO;
            })
        .toList();
  }

  private void updateTableauPharmacienWrapper(
      TableauPharmacienWrapper tableauPharmacienWrapper, AchatDTO achatDTO) {
    tableauPharmacienWrapper.setMontantAchatTtc(
        tableauPharmacienWrapper.getMontantAchatTtc() + achatDTO.getMontantTtc());
    tableauPharmacienWrapper.setMontantAchatRemise(
        tableauPharmacienWrapper.getMontantAchatRemise() + achatDTO.getMontantRemise());
    tableauPharmacienWrapper.setMontantAchatNet(
        tableauPharmacienWrapper.getMontantAchatNet() + achatDTO.getMontantNet());
    tableauPharmacienWrapper.setMontantAchatTaxe(
        tableauPharmacienWrapper.getMontantAchatTaxe() + achatDTO.getMontantTaxe());
    tableauPharmacienWrapper.setMontantAchatHt(
        tableauPharmacienWrapper.getMontantAchatHt() + achatDTO.getMontantHt());
  }

  private List<TableauPharmacienDTO> buildTableauPharmacienFromTuple(
      List<Tuple> tuples, TableauPharmacienWrapper tableauPharmacienWrapper) {
    return tuples.stream()
        .map(
            t -> {
              TableauPharmacienDTO tableauPharmacienDTO = new TableauPharmacienDTO();
              long montantCredit = t.get("montantCredit", BigDecimal.class).longValue();
              montantCredit += t.get("montantDiffere", BigDecimal.class).longValue();

              tableauPharmacienDTO
                  .setAmountToBePaid(t.get("amountToBePaid", BigDecimal.class).longValue())
                  .setMontantNetUg(t.get("montantNetUg", BigDecimal.class).longValue())
                  .setMontantTtcUg(t.get("montantTtcUg", BigDecimal.class).longValue())
                  .setMontantHtUg(t.get("montantHtUg", BigDecimal.class).longValue())
                  .setNombreVente(t.get("numberCount", BigInteger.class).intValue())
                  .setAmountToBeTakenIntoAccount(
                      t.get("amountToBeTakenIntoAccount", BigDecimal.class).longValue())
                  .setMontantAchat(t.get("montantAchat", BigDecimal.class).longValue())
                  .setMontantCredit(montantCredit)
                  .setPartAssure(t.get("partAssure", BigDecimal.class).longValue())
                  .setMvtDate(LocalDate.parse(t.get("mvtDate", String.class)))
                  .setMontantComptant(t.get("montantPaye", BigDecimal.class).longValue())
                  .setMontantTtc(t.get("montantTtc", BigDecimal.class).longValue())
                  .setMontantHt(t.get("montantHt", BigDecimal.class).longValue())
                  .setMontantTaxe(t.get("montantTaxe", BigDecimal.class).longValue())
                  .setMontantRemise(t.get("montantDiscount", BigDecimal.class).longValue())
                  .setMontantNet(t.get("montantNet", BigDecimal.class).longValue());
              updateTableauPharmacienWrapper(tableauPharmacienWrapper, tableauPharmacienDTO);
              return tableauPharmacienDTO;
            })
        .toList();
  }

  private void updateTableauPharmacienWrapper(
      TableauPharmacienWrapper tableauPharmacienWrapper,
      TableauPharmacienDTO tableauPharmacienDTO) {
    tableauPharmacienWrapper.setMontantVenteCredit(
        tableauPharmacienWrapper.getMontantVenteCredit() + tableauPharmacienDTO.getMontantCredit());
    tableauPharmacienWrapper.setMontantVenteComptant(
        tableauPharmacienWrapper.getMontantVenteComptant()
            + tableauPharmacienDTO.getMontantComptant());
    tableauPharmacienWrapper.setMontantVenteHt(
        tableauPharmacienWrapper.getMontantVenteHt() + tableauPharmacienDTO.getMontantHt());
    tableauPharmacienWrapper.setMontantVenteTtc(
        tableauPharmacienWrapper.getMontantVenteTtc() + tableauPharmacienDTO.getMontantTtc());
    tableauPharmacienWrapper.setMontantVenteTaxe(
        tableauPharmacienWrapper.getMontantVenteTaxe() + tableauPharmacienDTO.getMontantTaxe());
    tableauPharmacienWrapper.setMontantVenteRemise(
        tableauPharmacienWrapper.getMontantVenteRemise() + tableauPharmacienDTO.getMontantRemise());
    tableauPharmacienWrapper.setMontantVenteNet(
        tableauPharmacienWrapper.getMontantVenteNet() + tableauPharmacienDTO.getMontantNet());
    tableauPharmacienWrapper.setNumberCount(
        tableauPharmacienWrapper.getNumberCount() + tableauPharmacienDTO.getNombreVente());
  }

  private List<TableauPharmacienDTO> addAchatsToTableauPharmacien(
      List<TableauPharmacienDTO> tableauPharmaciens, List<AchatDTO> achats) {
    Map<LocalDate, List<AchatDTO>> map =
        achats.stream().collect(Collectors.groupingBy(AchatDTO::getMvtDate));
    if (map.isEmpty()) {
      return tableauPharmaciens;
    }
    if (tableauPharmaciens.isEmpty()) {
      updateTableauPharmaciens(map, tableauPharmaciens);
      return tableauPharmaciens;
    }
    for (TableauPharmacienDTO tableauPharmacien : tableauPharmaciens) {
      List<AchatDTO> achatDTOS = map.remove(tableauPharmacien.getMvtDate());
      if (Objects.nonNull(achatDTOS)) {
        tableauPharmacien.getAchats().put(tableauPharmacien.getMvtDate(), achatDTOS);
      }
      if (!map.isEmpty()) {
        updateTableauPharmaciens(map, tableauPharmaciens);
      }
    }

    return tableauPharmaciens;
  }

  private void updateTableauPharmaciens(
      Map<LocalDate, List<AchatDTO>> map, List<TableauPharmacienDTO> tableauPharmaciens) {
    map.forEach(
        (k, v) -> {
          TableauPharmacienDTO tableauPharmacienDTO = new TableauPharmacienDTO();
          tableauPharmacienDTO.setMvtDate(k).getAchats().put(k, v);
          tableauPharmaciens.add(tableauPharmacienDTO);
        });
  }

  private TableauPharmacienWrapper computeData(
      LocalDate fromDate,
      LocalDate toDate,
      Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      Set<SalesStatut> statuts,
      Set<TypeVente> typeVentes,
      String groupeBy) {
    TableauPharmacienWrapper tableauPharmacienWrapper = new TableauPharmacienWrapper();
    List<Tuple> tuples =
        executeQuery(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy);
    List<TableauPharmacienDTO> tableauPharmaciens =
        buildTableauPharmacienFromTuple(tuples, tableauPharmacienWrapper);
    List<Tuple> achatTuples = executeAchatQuery(fromDate, toDate, groupeBy);
    List<AchatDTO> achats = buildAchatsFromTuple(achatTuples, tableauPharmacienWrapper);
    achats.sort(Comparator.comparing(AchatDTO::getOrdreAffichage));
    List<TableauPharmacienDTO> result = addAchatsToTableauPharmacien(tableauPharmaciens, achats);
    computeRatioVenteAchat(tableauPharmacienWrapper);
    computeRatioAchatVente(tableauPharmacienWrapper);
    return tableauPharmacienWrapper.setTableauPharmaciens(result);
  }

  private void computeRatioVenteAchat(TableauPharmacienWrapper tableauPharmacienWrapper) {
    try {
      tableauPharmacienWrapper.setRatioVenteAchat(
          BigDecimal.valueOf(tableauPharmacienWrapper.getMontantVenteNet())
              .divide(
                  BigDecimal.valueOf(tableauPharmacienWrapper.getMontantAchatNet()),
                  2,
                  RoundingMode.FLOOR)
              .floatValue());
    } catch (Exception e) {
      log.warn("Error", e.getMessage());
    }
  }

  private void computeRatioAchatVente(TableauPharmacienWrapper tableauPharmacienWrapper) {
    try {
      tableauPharmacienWrapper.setRatioAchatVente(
          BigDecimal.valueOf(tableauPharmacienWrapper.getMontantAchatNet())
              .divide(
                  BigDecimal.valueOf(tableauPharmacienWrapper.getMontantVenteNet()),
                  2,
                  RoundingMode.FLOOR)
              .floatValue());
    } catch (Exception e) {
      log.warn("Error", e.getMessage());
    }
  }
}
