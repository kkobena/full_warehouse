package com.kobe.warehouse.service.inventaire;

import com.kobe.warehouse.service.dto.CreateInventoryFromProduitIds;
import com.kobe.warehouse.service.dto.InventoryExportWrapper;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryGroupExport;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.StoreInventoryLotGroupExport;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryFilterRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import com.kobe.warehouse.service.errors.InventoryException;
import com.kobe.warehouse.service.mobile.dto.RayonRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface InventaireService {

    byte[] printToPdf(StoreInventoryExportRecord filterRecord);

    List<StoreInventoryGroupExport> getStoreInventoryToExport(
        StoreInventoryExportRecord filterRecord);

    void remove(Long id);

    StoreInventoryLineRecord updateQuantityOnHand(StoreInventoryLineDTO storeInventoryLineDTO);

    Optional<StoreInventoryDTO> getStoreInventory(Long id);

    Page<StoreInventoryDTO> storeInventoryList(
        StoreInventoryFilterRecord storeInventoryFilterRecord, Pageable pageable);

    Optional<StoreInventoryDTO> getProccessingStoreInventory(Long id);

    List<StoreInventoryDTO> fetchActifs();

    List<RayonRecord> fetchRayonsByStoreInventoryId(Long storeInventoryId);

    void importDetail(Long storeInventoryId, MultipartFile multipartFile);

    List<StoreInventoryLineDTO> getAllItems(Long storeInventoryId);

    List<StoreInventoryLineDTO> getItemsByRayonId(Long storeInventoryId, Long rayonId);

    void synchronizeStoreInventoryLine(List<StoreInventoryLineDTO> storeInventoryLines);

    InventoryExportWrapper exportInventory(StoreInventoryExportRecord inventoryExportRecord);

    List<StoreInventoryLotGroupExport> getLotGroupsForExport(Long inventoryId);

    int createInventoryFromFrom(CreateInventoryFromProduitIds createInventoryFromProduitIds)
        throws InventoryException;
}
