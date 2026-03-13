package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.records.BatchSyncResultRecord;
import com.kobe.warehouse.service.stock.InventaireSyncService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class InventaireSyncServiceImpl implements InventaireSyncService {

    private final Logger log = LoggerFactory.getLogger(InventaireSyncServiceImpl.class);

    private final StoreInventoryLineRepository storeInventoryLineRepository;

    public InventaireSyncServiceImpl(StoreInventoryLineRepository storeInventoryLineRepository) {
        this.storeInventoryLineRepository = storeInventoryLineRepository;
    }

    @Override
    public BatchSyncResultRecord synchronize(List<StoreInventoryLineDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) {
            return new BatchSyncResultRecord(0, 0, List.of());
        }

        Set<Long> ids = dtos.stream()
            .map(StoreInventoryLineDTO::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // Chargement en masse : 1 requête au lieu de N getReferenceById
        Map<Long, StoreInventoryLine> lineMap = storeInventoryLineRepository.findAllById(ids)
            .stream()
            .collect(Collectors.toMap(StoreInventoryLine::getId, l -> l));

        List<Long> failedIds = new ArrayList<>();
        List<StoreInventoryLine> toSave = new ArrayList<>();

        for (StoreInventoryLineDTO dto : dtos) {
            if (dto.getId() == null) {
                continue;
            }
            StoreInventoryLine line = lineMap.get(dto.getId());
            if (line == null) {
                log.warn("StoreInventoryLine introuvable : id={}", dto.getId());
                failedIds.add(dto.getId());
                continue;
            }
            try {
                applyDto(dto, line);
                toSave.add(line);
            } catch (Exception e) {
                log.error("Erreur mise à jour ligne id={}", dto.getId(), e);
                failedIds.add(dto.getId());
            }
        }

        // Sauvegarde batch : 1 saveAll au lieu de N saveAndFlush
        storeInventoryLineRepository.saveAll(toSave);

        return new BatchSyncResultRecord(toSave.size(), failedIds.size(), failedIds);
    }

    private void applyDto(StoreInventoryLineDTO dto, StoreInventoryLine line) {
        Produit produit = line.getProduit();
        FournisseurProduit fp = produit.getFournisseurProduitPrincipal();
        line.setQuantitySold(0);
        line.setUpdated(true);
        line.setUpdatedAt(LocalDateTime.now());
        line.setInventoryValueCost(
            Objects.nonNull(fp) ? fp.getPrixAchat() : produit.getCostAmount()
        );
        line.setLastUnitPrice(
            Objects.nonNull(fp) ? fp.getPrixUni() : produit.getRegularUnitPrice()
        );
        line.setQuantityOnHand(dto.getQuantityOnHand());
        line.setQuantityInit(dto.getQuantityInit());
        line.setGap(line.getQuantityOnHand() - line.getQuantityInit());
    }
}
