package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.builder.StoreInventoryLineFilterBuilder;
import com.kobe.warehouse.service.dto.enumeration.StoreInventoryLineEnum;
import com.kobe.warehouse.service.dto.filter.StoreInventoryFilterRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public interface InventaireService {

  void close(Long id);

  List<StoreInventoryLineDTO> storeInventoryList(Long storeInventoryId);

  Page<StoreInventoryLineRecord> getAllByInventory(
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord, Pageable pageable);

  void remove(Long id);

    StoreInventoryLineRecord updateQuantityOnHand(StoreInventoryLineDTO storeInventoryLineDTO);

  Optional<StoreInventoryDTO> getStoreInventory(Long id);

  StoreInventoryDTO create(StoreInventoryRecord storeInventoryRecord);

  Page<StoreInventoryDTO> storeInventoryList(
      StoreInventoryFilterRecord storeInventoryFilterRecord, Pageable pageable);

  Optional<StoreInventoryDTO> getProccessingStoreInventory(Long id);

  default String buildFetchDetailQuery(
      String baseQery,
      StoreInventory storeInventory,
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
    if (storeInventory.getInventoryCategory() == InventoryCategory.STORAGE || !CollectionUtils.isEmpty(storeInventoryLineFilterRecord.storageIds())) {
      return String.format(
          baseQery,
          String.format(
              "%s %S",
              buildFetchDetailStorageQuery(storeInventory, storeInventoryLineFilterRecord),
              buildFilter(storeInventoryLineFilterRecord.selectedFilter())));
    } else if (storeInventory.getInventoryCategory() == InventoryCategory.RAYON || Objects.nonNull(storeInventoryLineFilterRecord.rayonId())) {
      return String.format(
          baseQery,
          String.format(
              "%s %S",
              buildFetchDetailRayonQuery(storeInventory, storeInventoryLineFilterRecord),
              buildFilter(storeInventoryLineFilterRecord.selectedFilter())));
    }

    String q = buildFilter(storeInventoryLineFilterRecord.selectedFilter());
    if (StringUtils.hasLength(storeInventoryLineFilterRecord.search())) {

      return String.format(
          baseQery, " WHERE " + buildSearchSection(storeInventoryLineFilterRecord) + q);
    }
    if (StringUtils.hasLength(q)) {
      return String.format(
          baseQery, " WHERE " + org.apache.commons.lang3.StringUtils.removeStart(q, "AND"));
    }
    return String.format(baseQery, " ");
  }

  default String buildFetchDetailQuery(
      StoreInventory storeInventory,
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
    return buildFetchDetailQuery(
        StoreInventoryLineFilterBuilder.BASE_QUERY, storeInventory, storeInventoryLineFilterRecord);
  }

  default String buildFetchDetailQueryCount(
      StoreInventory storeInventory,
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
    return buildFetchDetailQuery(
        StoreInventoryLineFilterBuilder.COUNT, storeInventory, storeInventoryLineFilterRecord);
  }

  default String buildSearchSection(StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
    String search = storeInventoryLineFilterRecord.search() + "%";
    return String.format(
        StoreInventoryLineFilterBuilder.LIKE_STATEMENT_WHERE, search, search, search);
  }

  default String buildRayonWhereClose(
      StoreInventory storeInventory,
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
    if (storeInventory.getInventoryCategory() == InventoryCategory.RAYON) {
      return String.format(
          StoreInventoryLineFilterBuilder.RAYON_STATEMENT_WHERE, storeInventory.getRayon().getId());
    }
    return String.format(
        StoreInventoryLineFilterBuilder.RAYON_STATEMENT_WHERE,
        storeInventoryLineFilterRecord.rayonId());
  }

  default String buildStorageWhereClose(
      StoreInventory storeInventory,
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
    if (storeInventory.getInventoryCategory() == InventoryCategory.STORAGE) {
      return String.format(
          StoreInventoryLineFilterBuilder.STOCKAGE_STATEMENT_WHERE,
          storeInventory.getStorage().getId());
    }
    if (CollectionUtils.isEmpty(storeInventoryLineFilterRecord.storageIds())) return "";
    return String.format(
        StoreInventoryLineFilterBuilder.STOCKAGE_STATEMENT_WHERE,
        storeInventoryLineFilterRecord.storageIds().stream()
            .map(e -> e.toString())
            .collect(Collectors.joining(",")));
  }

  default String buildFetchDetailStorageQuery(
      StoreInventory storeInventory,
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {

    if (Objects.nonNull(storeInventoryLineFilterRecord.rayonId())) {
      if (StringUtils.hasLength(storeInventoryLineFilterRecord.search())) {
        return String.format(
            "%s WHERE %s AND %s",
            StoreInventoryLineFilterBuilder.STOCKAGE_STATEMENT,
            buildSearchSection(storeInventoryLineFilterRecord),
            buildRayonWhereClose(storeInventory, storeInventoryLineFilterRecord));
      } else {
        return String.format(
            "%s WHERE %s ",
            StoreInventoryLineFilterBuilder.STOCKAGE_STATEMENT,
            buildRayonWhereClose(storeInventory, storeInventoryLineFilterRecord));
      }
    } else {
      if (!CollectionUtils.isEmpty(storeInventoryLineFilterRecord.storageIds())
          || storeInventory.getInventoryCategory() == InventoryCategory.STORAGE) {
        if (StringUtils.hasLength(storeInventoryLineFilterRecord.search())) {
          return String.format(
              "%s WHERE %s AND %s",
              StoreInventoryLineFilterBuilder.STOCKAGE_STATEMENT,
              buildSearchSection(storeInventoryLineFilterRecord),
              buildStorageWhereClose(storeInventory, storeInventoryLineFilterRecord));
        }
        return String.format(
            "%s WHERE %s ",
            StoreInventoryLineFilterBuilder.STOCKAGE_STATEMENT,
            buildStorageWhereClose(storeInventory, storeInventoryLineFilterRecord));
      }
    }
    return String.format(
        "%s WHERE %s ",
        StoreInventoryLineFilterBuilder.STOCKAGE_STATEMENT,
        buildStorageWhereClose(storeInventory, storeInventoryLineFilterRecord));
  }

  default String buildFetchDetailRayonQuery(
      StoreInventory storeInventory,
      StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {

    if (StringUtils.hasLength(storeInventoryLineFilterRecord.search())) {
      return String.format(
          "%s WHERE %s AND %s",
          StoreInventoryLineFilterBuilder.RAYON_STATEMENT,
          buildSearchSection(storeInventoryLineFilterRecord),
          buildRayonWhereClose(storeInventory, storeInventoryLineFilterRecord));
    }
    return String.format(
        "%s WHERE %s ",
        StoreInventoryLineFilterBuilder.RAYON_STATEMENT,
        buildRayonWhereClose(storeInventory, storeInventoryLineFilterRecord));
  }

  default String buildFilter(StoreInventoryLineEnum storeInventoryLineEnum) {
    return switch (storeInventoryLineEnum) {
      case NONE -> "";
      case NOT_UPDATED -> " AND a.produit_id IS NULL ";
      case UPDATED -> " AND a.produit_id IS NOT NULL ";
      case GAP -> " AND a.produit_id IS NOT NULL AND  a.quantity_on_hand <> a.quantity_init ";
      case GAP_NEGATIF -> " AND a.produit_id IS NOT NULL AND  a.quantity_on_hand < a.quantity_init ";
      case GAP_POSITIF -> " AND a.produit_id IS NOT NULL AND  a.quantity_on_hand >= a.quantity_init ";
    };
  }
}
