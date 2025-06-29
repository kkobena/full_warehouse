package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.InventoryExportWrapper;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryGroupExport;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.builder.StoreInventoryLineFilterBuilder;
import com.kobe.warehouse.service.dto.enumeration.StoreInventoryLineEnum;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryFilterRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.ItemsCountRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import com.kobe.warehouse.service.errors.InventoryException;
import com.kobe.warehouse.service.mobile.dto.RayonRecord;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface InventaireService {
    Page<StoreInventoryLineRecord> getInventoryItems(StoreInventoryLineFilterRecord storeInventoryLineFilterRecord, Pageable pageable);

    Resource printToPdf(StoreInventoryExportRecord filterRecord) throws MalformedURLException;

    ItemsCountRecord close(Long id) throws InventoryException;

    List<StoreInventoryGroupExport> getStoreInventoryToExport(StoreInventoryExportRecord filterRecord);

    Page<StoreInventoryLineRecord> getAllByInventory(StoreInventoryLineFilterRecord storeInventoryLineFilterRecord, Pageable pageable);

    void remove(Long id);

    StoreInventoryLineRecord updateQuantityOnHand(StoreInventoryLineDTO storeInventoryLineDTO);

    Optional<StoreInventoryDTO> getStoreInventory(Long id);

    StoreInventoryDTO create(StoreInventoryRecord storeInventoryRecord);

    Page<StoreInventoryDTO> storeInventoryList(StoreInventoryFilterRecord storeInventoryFilterRecord, Pageable pageable);

    Optional<StoreInventoryDTO> getProccessingStoreInventory(Long id);

    List<StoreInventoryDTO> fetchActifs();

    List<RayonRecord> fetchRayonsByStoreInventoryId(Long storeInventoryId);

    void importDetail(Long storeInventoryId, MultipartFile multipartFile);

    List<StoreInventoryLineDTO> getAllItems(Long storeInventoryId);

    List<StoreInventoryLineDTO> getItemsByRayonId(Long storeInventoryId, Long rayonId);

    void synchronizeStoreInventoryLine(
        List<StoreInventoryLineDTO> storeInventoryLines
    );

    default String buildBaseQuery(String baseQuery, StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
        if (Objects.nonNull(storeInventoryLineFilterRecord.storageId()) || Objects.nonNull(storeInventoryLineFilterRecord.rayonId())) {
            if (Objects.nonNull(storeInventoryLineFilterRecord.rayonId())) {
                return baseQuery
                    .replace("{join_statement}", StoreInventoryLineFilterBuilder.RAYON_STATEMENT)
                    .replace(
                        "{join_statement_where}",
                        String.format(StoreInventoryLineFilterBuilder.RAYON_STATEMENT_WHERE, storeInventoryLineFilterRecord.rayonId())
                    );
            } else {
                return baseQuery
                    .replace("{join_statement}", StoreInventoryLineFilterBuilder.RAYON_STATEMENT)
                    .replace(
                        "{join_statement_where}",
                        String.format(StoreInventoryLineFilterBuilder.STOCKAGE_STATEMENT_WHERE, storeInventoryLineFilterRecord.storageId())
                    );
            }
        } else {
            return baseQuery.replace("{join_statement}", "").replace("{join_statement_where}", "");
        }
    }

    default String buildFetchDetailQuery(String baseQuery, StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
        String query = buildBaseQuery(baseQuery, storeInventoryLineFilterRecord);
        String q = buildFilter(storeInventoryLineFilterRecord.selectedFilter());
        if (StringUtils.hasLength(storeInventoryLineFilterRecord.search())) {
            return String.format(query, buildSearchSection(storeInventoryLineFilterRecord) + q);
        } else if (StringUtils.hasLength(q)) {
            return String.format(query, q);
        }
        return String.format(query, " ");
    }

    default String buildFetchDetailQuery(StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
        return buildFetchDetailQuery(StoreInventoryLineFilterBuilder.BASE_QUERY, storeInventoryLineFilterRecord);
    }

    default String buildFetchDetailQueryCount(StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
        return buildFetchDetailQuery(StoreInventoryLineFilterBuilder.COUNT, storeInventoryLineFilterRecord);
    }

    default String buildSearchSection(StoreInventoryLineFilterRecord storeInventoryLineFilterRecord) {
        String search = storeInventoryLineFilterRecord.search() + "%";
        return String.format(StoreInventoryLineFilterBuilder.LIKE_STATEMENT_WHERE, search, search, search);
    }

    default String buildFilter(StoreInventoryLineEnum storeInventoryLineEnum) {
        return switch (storeInventoryLineEnum) {
            case NONE -> "";
            case NOT_UPDATED -> " AND a.updated IS false ";
            case UPDATED -> " AND a.updated ";
            case GAP -> " AND a.updated  AND  a.quantity_on_hand <> a.quantity_init ";
            case GAP_NEGATIF -> " AND a.updated  AND  a.quantity_on_hand < a.quantity_init ";
            case GAP_POSITIF -> " AND a.updated  AND  a.quantity_on_hand >= a.quantity_init ";
        };
    }

    default String buildExportQuery(StoreInventoryExportRecord inventoryExportRecord) {
        StoreInventoryLineFilterRecord storeInventoryLineFilterRecord = inventoryExportRecord.filterRecord();
        String whereClose = "";
        if (Objects.nonNull(storeInventoryLineFilterRecord.rayonId())) {
            whereClose = whereClose.concat(
                String.format(StoreInventoryLineFilterBuilder.EXPORT_RAYON_CLOSE_QUERY, storeInventoryLineFilterRecord.rayonId())
            );
        }
        if (Objects.nonNull(storeInventoryLineFilterRecord.storageId())) {
            whereClose = whereClose.concat(
                String.format(StoreInventoryLineFilterBuilder.EXPORT_STORAGE_CLOSE_QUERY, storeInventoryLineFilterRecord.storageId())
            );
        }

        whereClose = String.format(
            StoreInventoryLineFilterBuilder.EXPORT_QUERY,
            whereClose.concat(buildFilter(storeInventoryLineFilterRecord.selectedFilter()))
        );

        return switch (inventoryExportRecord.exportGroupBy()) {
            case RAYON -> whereClose.replace("{order_by}", "storage_name, rayon_libelle,");
            case FAMILLY -> whereClose.replace("{order_by}", "storage_name, fm.code,fm.libelle,");
            case STORAGE -> whereClose.replace("{order_by}", "storage_name,");
            case NONE -> whereClose.replace("{order_by}", "");
        };
    }

    InventoryExportWrapper exportInventory(StoreInventoryExportRecord inventoryExportRecord);
}
